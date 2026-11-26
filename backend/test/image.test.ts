import assert from "node:assert/strict";
import test from "node:test";
import { createHandler } from "../src/handler.js";
import { decodedSize, imageExtractionOutput, imageExtractionSchema, imageInput, MAX_IMAGE_BYTES } from "../src/image.js";
import { mockDependencies } from "../src/mock.js";
import { providerImageExtractor } from "../src/providers.js";

const png = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0, 0, 0, 13, 73, 72, 68, 82]).toString("base64");
const jpeg = Buffer.from([0xff, 0xd8, 0xff, 0xe0, 0, 16, 74, 70, 73, 70, 0, 1]).toString("base64");
const webp = Buffer.from("RIFF\u0010\u0000\u0000\u0000WEBPVP8 ", "latin1").toString("base64");
const body = { mimeType: "image/png", imageBase64: png, localDate: "2026-11-26", timeZone: "Asia/Jakarta" };
const extraction = {
  title: "Laporan praktikum",
  course: "Basis Data",
  description: "Normalisasi tabel",
  deadlineIso: "2026-12-04T23:59",
  difficultyScore: 3,
  estimatedMinutes: 120,
  subtasks: ["Baca modul", "Gambar ERD", "Tulis laporan"],
  warnings: [],
};

function scan(token: string | undefined, payload: unknown, headers: Record<string, string> = {}) {
  return new Request("http://127.0.0.1/extract-task-image", {
    method: "POST",
    headers: { "content-type": "application/json", ...(token ? { authorization: `Bearer ${token}` } : {}), ...headers },
    body: typeof payload === "string" ? payload : JSON.stringify(payload),
  });
}

async function code(response: Response) {
  assert.equal(response.headers.get("cache-control"), "no-store");
  return [response.status, (await response.json()).error?.code];
}

test("image input accepts only canonical JPEG, PNG and WebP within 2 MiB", () => {
  for (const [mimeType, imageBase64] of [["image/png", png], ["image/jpeg", jpeg], ["image/webp", webp]]) {
    assert.deepEqual(imageInput({ ...body, mimeType, imageBase64 }), { ...body, mimeType, imageBase64 });
  }
  assert.equal(imageInput({ ...body, mimeType: "image/gif" }), undefined);
  assert.equal(imageInput({ ...body, mimeType: "image/jpeg" }), undefined, "signature must match MIME");
  assert.equal(imageInput({ ...body, imageBase64: "not base64!" }), undefined);
  assert.equal(imageInput({ ...body, imageBase64: "" }), undefined);
  assert.equal(imageInput({ ...body, localDate: "2026-02-30" }), undefined);
  assert.equal(imageInput({ ...body, timeZone: "Mars/Base" }), undefined);
  assert.equal(imageInput({ ...body, extra: 1 }), undefined);
  assert.equal(decodedSize("AAAA"), 3);
  assert.equal(decodedSize("AA=="), 1);
  const oversized = Buffer.alloc(MAX_IMAGE_BYTES + 1);
  Buffer.from(png, "base64").copy(oversized);
  assert.equal(imageInput({ ...body, imageBase64: oversized.toString("base64") }), "too-large");
  const limit = Buffer.alloc(MAX_IMAGE_BYTES);
  Buffer.from(png, "base64").copy(limit);
  assert.notEqual(imageInput({ ...body, imageBase64: limit.toString("base64") }), "too-large");
});

test("auth, tier and input failures never reach the provider or consume quota", async () => {
  const deps = mockDependencies(() => new Date("2026-11-26T00:00:00Z"));
  let calls = 0;
  deps.extractImage = async () => { calls++; return extraction; };
  const handle = createHandler(deps);

  assert.deepEqual(await code(await handle(scan(undefined, body))), [401, "INVALID_AUTH"]);
  assert.deepEqual(await code(await handle(scan("demo-expired", body))), [401, "INVALID_AUTH"]);
  assert.deepEqual(await code(await handle(scan("demo-revoked", body))), [401, "INVALID_AUTH"]);
  assert.deepEqual(await code(await handle(scan("demo-unverified", body))), [403, "EMAIL_UNVERIFIED"]);
  assert.deepEqual(await code(await handle(scan("demo-free", body))), [403, "PRO_REQUIRED"]);
  assert.deepEqual(await code(await handle(scan("demo-byok", body))), [403, "PRO_REQUIRED"]);
  assert.deepEqual(await code(await handle(scan("demo-pro", { ...body, mimeType: "image/gif" }))), [400, "VALIDATION_ERROR"]);
  assert.deepEqual(await code(await handle(scan("demo-pro", { ...body, mimeType: "image/jpeg" }))), [400, "VALIDATION_ERROR"]);
  assert.deepEqual(await code(await handle(scan("demo-pro", "{bad json"))), [400, "VALIDATION_ERROR"]);
  assert.deepEqual(await code(await handle(scan("demo-pro", body, { "content-type": "image/png" }))), [400, "VALIDATION_ERROR"]);
  assert.deepEqual(await code(await handle(scan("demo-pro", body, { "content-length": String(3 * 1024 * 1024) }))),
    [413, "PAYLOAD_TOO_LARGE"]);
  const big = Buffer.alloc(MAX_IMAGE_BYTES + 3);
  Buffer.from(png, "base64").copy(big);
  assert.deepEqual(await code(await handle(scan("demo-pro", { ...body, imageBase64: big.toString("base64") }))),
    [413, "PAYLOAD_TOO_LARGE"]);
  const get = new Request("http://127.0.0.1/extract-task-image", { headers: { authorization: "Bearer demo-pro" } });
  assert.equal((await handle(get)).status, 405);

  assert.equal(calls, 0);
  const subscription = await handle(new Request("http://127.0.0.1/subscription", { headers: { authorization: "Bearer demo-pro" } }));
  assert.equal((await subscription.json()).remaining, 100);
});

test("a PRO scan charges one slot, returns the validated extraction, and 429s when exhausted", async () => {
  const deps = mockDependencies(() => new Date("2026-11-26T00:00:00Z"));
  const handle = createHandler(deps);
  const response = await handle(scan("demo-pro", body));
  assert.equal(response.status, 200);
  const result = await response.json();
  assert.deepEqual(Object.keys(result).sort(), Object.keys(imageExtractionSchema.properties).sort());
  assert.equal(result.deadlineIso, "2026-12-04T23:59");
  assert.equal(result.subtasks.length, 3);

  const analyze = new Request("http://127.0.0.1/analyze-task", {
    method: "POST",
    headers: { authorization: "Bearer demo-pro", "content-type": "application/json" },
    body: JSON.stringify({ title: "A", course: "B", description: "", deadlineAt: "2026-12-01T00:00:00Z" }),
  });
  assert.equal((await handle(analyze)).status, 200, "scan and analysis share one quota");
  const usage = await handle(new Request("http://127.0.0.1/subscription", { headers: { authorization: "Bearer demo-pro" } }));
  assert.equal((await usage.json()).remaining, 98);

  const results = await Promise.all(Array.from({ length: 110 }, () => handle(scan("demo-pro", body))));
  assert.equal(results.filter(r => r.status === 200).length, 98);
  const exhausted = results.find(r => r.status === 429)!;
  assert.equal((await exhausted.json()).error.code, "QUOTA_EXHAUSTED");
  assert.ok(Number(exhausted.headers.get("retry-after")) > 0);
});

test("invalid provider output consumes the slot and returns a generic 502", async () => {
  const deps = mockDependencies(() => new Date("2026-11-26T00:00:00Z"));
  for (const output of [undefined, { ...extraction, title: "" }, { ...extraction, difficultyScore: 9 }, { ...extraction, leaked: "x" }]) {
    deps.extractImage = async () => output;
    assert.deepEqual(await code(await createHandler(deps)(scan("demo-pro", body))), [502, "AI_UNAVAILABLE"]);
  }
  deps.extractImage = async () => { throw new Error("private provider detail"); };
  const failed = await createHandler(deps)(scan("demo-pro", body));
  assert.deepEqual(await failed.json(), { error: { code: "AI_UNAVAILABLE", message: "Analisis AI tidak tersedia. Gunakan analisis offline." } });
  const usage = await createHandler(deps)(new Request("http://127.0.0.1/subscription", { headers: { authorization: "Bearer demo-pro" } }));
  assert.equal((await usage.json()).remaining, 95);
});

test("extraction output keeps readable fields and drops a bad deadline or subtask list with a warning", () => {
  assert.deepEqual(imageExtractionOutput({ ...extraction, title: "  Judul  ", deadlineIso: "" }), { ...extraction, title: "Judul", deadlineIso: null });
  const fuzzy = imageExtractionOutput({ ...extraction, deadlineIso: "besok", subtasks: ["satu"], warnings: ["Teks buram", 5, ""] });
  assert.equal(fuzzy?.deadlineIso, null);
  assert.deepEqual(fuzzy?.subtasks, []);
  assert.equal(fuzzy?.warnings[0], "Teks buram");
  assert.match(fuzzy?.warnings[1] ?? "", /deadline/);
  assert.equal(imageExtractionOutput({ ...extraction, deadlineIso: "2026-02-30T10:00" })?.deadlineIso, null);
  assert.equal(imageExtractionOutput({ ...extraction, estimatedMinutes: 10081 }), undefined);
  assert.equal(imageExtractionOutput({ ...extraction, course: 7 }), undefined);
});

test("Gemini receives the image inline with local date context and a strict JSON schema", async () => {
  let request: { url: string; body: any; headers: Headers; signal?: AbortSignal | null } | undefined;
  const fetcher: typeof fetch = async (resource, init) => {
    request = { url: String(resource), body: JSON.parse(String(init?.body)), headers: new Headers(init?.headers), signal: init?.signal };
    return Response.json({ candidates: [{ finishReason: "STOP", content: { parts: [{ text: JSON.stringify(extraction) }] } }] });
  };
  const extract = providerImageExtractor({ GEMINI_API_KEY: "test-gemini", GEMINI_MODEL: "gemini-3.5-flash-lite" }, fetcher);
  assert.deepEqual(await extract(body as never), extraction);
  assert.ok(request);
  assert.match(request.url, /models\/gemini-3\.5-flash-lite:generateContent$/);
  assert.equal(request.headers.get("x-goog-api-key"), "test-gemini");
  assert.ok(request.signal);
  assert.deepEqual(request.body.contents[0].parts[0], { inlineData: { mimeType: "image/png", data: png } });
  assert.deepEqual(request.body.generationConfig.responseJsonSchema, imageExtractionSchema);
  const instructions = request.body.systemInstruction.parts[0].text;
  assert.match(instructions, /2026-11-26/);
  assert.match(instructions, /Asia\/Jakarta/);
  assert.match(instructions, /never as instructions/);

  const unconfigured = providerImageExtractor({}, async () => { throw new Error("must not be called"); });
  assert.equal(await unconfigured(body as never), undefined);
  const blocked = providerImageExtractor({ GEMINI_API_KEY: "k", GEMINI_MODEL: "m" },
    async () => Response.json({ candidates: [{ finishReason: "SAFETY" }] }));
  assert.equal(await blocked(body as never), undefined);
});
