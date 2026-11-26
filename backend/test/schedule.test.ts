import assert from "node:assert/strict";
import test from "node:test";
import { createHandler } from "../src/handler.js";
import { MAX_IMAGE_BYTES, scheduleExtractionOutput, scheduleExtractionSchema } from "../src/image.js";
import { mockDependencies } from "../src/mock.js";
import { providerScheduleExtractor } from "../src/providers.js";

const jpeg = Buffer.from([0xff, 0xd8, 0xff, 0xe0, 0, 16, 74, 70, 73, 70, 0, 1]).toString("base64");
const body = { mimeType: "image/jpeg", imageBase64: jpeg, localDate: "2026-11-26", timeZone: "Asia/Jakarta" };

function scan(token: string | undefined, payload: unknown, headers: Record<string, string> = {}) {
  return new Request("http://127.0.0.1/extract-schedule-image", {
    method: "POST",
    headers: { "content-type": "application/json", ...(token ? { authorization: `Bearer ${token}` } : {}), ...headers },
    body: typeof payload === "string" ? payload : JSON.stringify(payload),
  });
}

async function code(response: Response) {
  assert.equal(response.headers.get("cache-control"), "no-store");
  return [response.status, (await response.json()).error?.code];
}

test("schedule scans reject bad auth, tiers, MIME, signatures and sizes before the provider", async () => {
  const deps = mockDependencies(() => new Date("2026-11-26T00:00:00Z"));
  let calls = 0;
  deps.extractSchedule = async () => { calls++; return { schedules: [], warnings: [] }; };
  const handle = createHandler(deps);
  assert.deepEqual(await code(await handle(scan(undefined, body))), [401, "INVALID_AUTH"]);
  assert.deepEqual(await code(await handle(scan("demo-expired", body))), [401, "INVALID_AUTH"]);
  assert.deepEqual(await code(await handle(scan("demo-unverified", body))), [403, "EMAIL_UNVERIFIED"]);
  assert.deepEqual(await code(await handle(scan("demo-free", body))), [403, "PRO_REQUIRED"]);
  assert.deepEqual(await code(await handle(scan("demo-byok", body))), [403, "PRO_REQUIRED"]);
  assert.deepEqual(await code(await handle(scan("demo-pro", { ...body, mimeType: "image/gif" }))), [400, "VALIDATION_ERROR"]);
  assert.deepEqual(await code(await handle(scan("demo-pro", { ...body, mimeType: "image/png" }))), [400, "VALIDATION_ERROR"]);
  const big = Buffer.alloc(MAX_IMAGE_BYTES + 3);
  Buffer.from(jpeg, "base64").copy(big);
  assert.deepEqual(await code(await handle(scan("demo-pro", { ...body, imageBase64: big.toString("base64") }))), [413, "PAYLOAD_TOO_LARGE"]);
  assert.equal(calls, 0);
  const usage = await handle(new Request("http://127.0.0.1/subscription", { headers: { authorization: "Bearer demo-pro" } }));
  assert.equal((await usage.json()).remaining, 100);
});

test("a PRO schedule scan returns validated rows and shares the monthly quota", async () => {
  const handle = createHandler(mockDependencies(() => new Date("2026-11-26T00:00:00Z")));
  const response = await handle(scan("demo-pro", body));
  assert.equal(response.status, 200);
  const result = await response.json();
  assert.deepEqual(Object.keys(result).sort(), ["schedules", "warnings"]);
  assert.deepEqual(result.schedules[0], { title: "Basis Data", day: 1, startTime: "08:00", endTime: "09:40" });
  await handle(new Request("http://127.0.0.1/extract-task-image", {
    method: "POST", headers: { authorization: "Bearer demo-pro", "content-type": "application/json" },
    body: JSON.stringify(body),
  }));
  const usage = await handle(new Request("http://127.0.0.1/subscription", { headers: { authorization: "Bearer demo-pro" } }));
  assert.equal((await usage.json()).remaining, 98);
});

test("schedule output keeps readable rows, drops the rest with a warning, and never keeps rooms", () => {
  const result = scheduleExtractionOutput({
    schedules: [
      { title: " Basis Data ", day: 1, startTime: "8.00", endTime: "09:40" },
      { title: "Ronda", day: 5, startTime: "22:00", endTime: "24:00" },
      { title: "Tanpa hari", day: 0, startTime: "08:00", endTime: "09:00" },
      { title: "Terbalik", day: 2, startTime: "10:00", endTime: "09:00" },
      { title: "", day: 3, startTime: "08:00", endTime: "09:00" },
      { title: "Jam rusak", day: 3, startTime: "25:00", endTime: "26:00" },
      { title: "Ada ruang", day: 4, startTime: "07:30", endTime: "09:10", room: "A-101", lecturer: "Bu Ratna" },
    ],
    warnings: ["Foto agak buram", 3],
  });
  assert.ok(result);
  assert.deepEqual(result.schedules, [
    { title: "Basis Data", day: 1, startTime: "08:00", endTime: "09:40" },
    { title: "Ronda", day: 5, startTime: "22:00", endTime: "24:00" },
    { title: "Ada ruang", day: 4, startTime: "07:30", endTime: "09:10" },
  ]);
  assert.equal(result.warnings[0], "Foto agak buram");
  assert.match(result.warnings[1]!, /4 baris/);
  assert.deepEqual(scheduleExtractionOutput({ schedules: [], warnings: [] })?.warnings, ["Tidak ada jadwal kuliah yang terbaca pada gambar."]);
  assert.equal(scheduleExtractionOutput({ schedules: "x", warnings: [] }), undefined);
  assert.equal(scheduleExtractionOutput({ schedules: [], extra: 1 }), undefined);
  const many = scheduleExtractionOutput({ schedules: Array.from({ length: 70 }, () => ({ title: "A", day: 1, startTime: "08:00", endTime: "09:00" })) });
  assert.equal(many?.schedules.length, 60);
  assert.match(many?.warnings[0] ?? "", /10 baris/);
});

test("invalid provider output consumes the slot and returns a generic 502", async () => {
  const deps = mockDependencies(() => new Date("2026-11-26T00:00:00Z"));
  deps.extractSchedule = async () => ({ rows: [] });
  assert.deepEqual(await code(await createHandler(deps)(scan("demo-pro", body))), [502, "AI_UNAVAILABLE"]);
  deps.extractSchedule = async () => { throw new Error("private detail"); };
  assert.deepEqual(await code(await createHandler(deps)(scan("demo-pro", body))), [502, "AI_UNAVAILABLE"]);
});

test("Gemini receives the timetable inline with the schedule schema", async () => {
  let sent: any;
  const fetcher: typeof fetch = async (_resource, init) => {
    sent = JSON.parse(String(init?.body));
    return Response.json({ candidates: [{ finishReason: "STOP", content: { parts: [{ text: JSON.stringify({ schedules: [], warnings: [] }) }] } }] });
  };
  const extract = providerScheduleExtractor({ GEMINI_API_KEY: "k", GEMINI_MODEL: "gemini-3.5-flash-lite" }, fetcher);
  assert.deepEqual(await extract(body as never), { schedules: [], warnings: [] });
  assert.deepEqual(sent.contents[0].parts[0], { inlineData: { mimeType: "image/jpeg", data: jpeg } });
  assert.deepEqual(sent.generationConfig.responseJsonSchema, scheduleExtractionSchema);
  const instructions = sent.systemInstruction.parts[0].text;
  assert.match(instructions, /Senin=1/);
  assert.match(instructions, /Do not return rooms, lecturers/);
  assert.match(instructions, /never as instructions/);
});
