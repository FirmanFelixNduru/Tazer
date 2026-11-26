import assert from "node:assert/strict";
import test from "node:test";
import { createHandler } from "../src/handler.js";
import { mockDependencies } from "../src/mock.js";
import type { TaskInput } from "../src/analysis.js";

const input = {
  title: "  Implementasi sorting  ",
  course: "Algoritma",
  description: "",
  deadlineAt: "2020-02-29T10:00:00Z",
};

function request(path: string, token?: string, body?: string) {
  const headers = new Headers();
  if (token) headers.set("authorization", `Bearer ${token}`);
  if (body !== undefined) headers.set("content-type", "application/json");
  return new Request(`http://127.0.0.1${path}`, {
    method: body === undefined ? "GET" : "POST",
    headers,
    body,
  });
}

async function checkError(response: Response, status: number, code: string) {
  assert.equal(response.status, status);
  assert.equal(response.headers.get("cache-control"), "no-store");
  assert.equal((await response.json()).error.code, code);
}

test("session returns the verified UID and rejects invalid, expired, revoked and unverified tokens", async () => {
  const handle = createHandler(mockDependencies());
  const session = await handle(request("/auth/session", "demo-pro"));
  assert.equal(session.status, 200);
  assert.equal(session.headers.get("cache-control"), "no-store");
  assert.deepEqual(await session.json(), { uid: "demo-pro" });

  for (const token of [undefined, "unknown", "demo-expired", "demo-revoked"]) {
    const response = await handle(request("/auth/session", token));
    assert.equal(response.headers.get("www-authenticate"), "Bearer");
    await checkError(response, 401, "INVALID_AUTH");
  }
  const duplicate = request("/auth/session", "demo-pro");
  duplicate.headers.append("authorization", "Bearer demo-free");
  await checkError(await handle(duplicate), 401, "INVALID_AUTH");
  await checkError(await handle(request("/auth/session", "demo-unverified")), 403, "EMAIL_UNVERIFIED");
});

test("tier, session, auth outage and entitlement outage reject analysis before the provider", async () => {
  const deps = mockDependencies(() => new Date("2026-11-25T00:00:00Z"));
  const verify = deps.verify;
  const entitlement = deps.entitlement;
  let analyzed = 0;
  deps.verify = async token => {
    if (token === "auth-down") throw new Error("private auth error");
    return verify(token);
  };
  deps.entitlement = async (uid, consume) => {
    if (uid === "demo-pro") throw new Error("private database error");
    return entitlement(uid, consume);
  };
  deps.analyze = async () => { analyzed++; };
  const handle = createHandler(deps);

  for (const [token, status, code] of [
    ["demo-free", 403, "PRO_REQUIRED"],
    ["demo-byok", 403, "PRO_REQUIRED"],
    ["demo-unverified", 403, "EMAIL_UNVERIFIED"],
    ["demo-expired", 401, "INVALID_AUTH"],
    ["demo-revoked", 401, "INVALID_AUTH"],
    ["wrong", 401, "INVALID_AUTH"],
    ["auth-down", 503, "VERIFICATION_UNAVAILABLE"],
    ["demo-pro", 503, "VERIFICATION_UNAVAILABLE"],
  ] as const) {
    await checkError(await handle(request("/analyze-task", token, JSON.stringify(input))), status, code);
  }
  assert.equal(analyzed, 0);
  for (const tier of ["FREE", "BYOK"]) {
    const response = await handle(request("/subscription", `demo-${tier.toLowerCase()}`));
    assert.deepEqual(await response.json(), { tier, monthlyLimit: 0, remaining: 0, resetsAt: "2026-12-01T00:00:00Z" });
  }
});

test("analysis validates JSON shape, dimensions and date before charging", async () => {
  const deps = mockDependencies();
  const received: TaskInput[] = [];
  deps.analyze = async task => {
    received.push(task);
    return { difficultyScore: 4, estimatedMinutes: 150 };
  };
  const handle = createHandler(deps);
  const analyze = (body: string) => handle(request("/analyze-task", "demo-pro", body));
  for (const value of [
    null, [], { ...input, uid: "other-user" }, { ...input, description: null },
    { ...input, title: "  " }, { ...input, course: "a".repeat(121) },
    { ...input, description: "b".repeat(4001) },
    { ...input, deadlineAt: "2025-02-29T10:00:00Z" },
    { ...input, deadlineAt: "2020-02-29T10:00:00.000+07:00" },
    { ...input, deadlineAt: "2020-02-29T24:00:00Z" },
  ]) {
    await checkError(await analyze(JSON.stringify(value)), 400, "VALIDATION_ERROR");
  }
  await checkError(await analyze("{"), 400, "VALIDATION_ERROR");
  const badMime = request("/analyze-task", "demo-pro", JSON.stringify(input));
  badMime.headers.set("content-type", "text/plain");
  await checkError(await handle(badMime), 400, "VALIDATION_ERROR");
  await checkError(await analyze(" ".repeat(32 * 1024 + 1)), 413, "PAYLOAD_TOO_LARGE");
  assert.equal(received.length, 0);
  assert.equal((await (await handle(request("/subscription", "demo-pro"))).json()).remaining, 100);

  const body = JSON.stringify(input);
  const response = await analyze(body.padEnd(32 * 1024));
  assert.deepEqual(await response.json(), { difficultyScore: 4, estimatedMinutes: 150 });
  assert.deepEqual(received, [{ ...input, title: input.title.trim() }]);
});

test("local mock sends optional suggestions only to clients that request them", async () => {
  const handle = createHandler(mockDependencies());
  const estimates = { difficultyScore: 4, estimatedMinutes: 150 };
  for (const capability of [undefined, "0", "1"]) {
    const req = request("/analyze-task", "demo-pro", JSON.stringify(input));
    if (capability) req.headers.set("X-Tazer-Analysis-Subtasks", capability);
    const response = await handle(req);
    assert.equal(response.status, 200);
    assert.deepEqual(await response.json(), capability === "1" ? {
      ...estimates,
      subtasks: [
        "Catat hasil yang diminta pada instruksi tugas",
        "Buat draf untuk setiap bagian tugas",
        "Periksa draf dengan kriteria penilaian",
      ],
    } : estimates);
  }
});

test("opted-in analysis trims valid suggestions and omits an invalid list", async () => {
  const deps = mockDependencies();
  const handle = createHandler(deps);
  const estimates = { difficultyScore: 4, estimatedMinutes: 150 };
  const exactLength = "Test ".padEnd(119, "a") + "🚀";
  const valid = ["  Define the inputs  ", "Write a sample", "Run tests", "Check edge cases", exactLength];
  const legacy = Symbol("missing subtasks");
  for (const subtasks of [
    valid, legacy, null, "Run tests", valid.slice(0, 2), [...valid, "Publish the result"],
    ["Write code", "  \n\t ", "Run tests"], ["Write code", 42, "Run tests"],
    ["Write code", "Run tests", exactLength + "a"],
  ]) {
    deps.analyze = async () => subtasks === legacy ? estimates : { ...estimates, subtasks };
    const req = request("/analyze-task", "demo-pro", JSON.stringify(input));
    req.headers.set("X-Tazer-Analysis-Subtasks", "1");
    const response = await handle(req);
    assert.equal(response.status, 200);
    assert.deepEqual(await response.json(), subtasks === valid
      ? { ...estimates, subtasks: valid.map(item => item.trim()) }
      : estimates);
  }
});

test("parallel requests take exactly 100 slots and the UTC rollover resets the quota", async () => {
  let now = "2026-11-30T23:59:59Z";
  const deps = mockDependencies(() => new Date(now));
  let analyzed = 0;
  deps.analyze = async () => {
    analyzed++;
    return { difficultyScore: 4, estimatedMinutes: 150 };
  };
  const handle = createHandler(deps);
  const analyze = () => handle(request("/analyze-task", "demo-pro", JSON.stringify(input)));
  const responses = await Promise.all(Array.from({ length: 108 }, analyze));
  assert.equal(analyzed, 100);
  assert.equal(responses.filter(response => response.status === 200).length, 100);
  for (const response of responses.filter(response => response.status !== 200)) {
    assert.equal(response.headers.get("retry-after"), "1");
    await checkError(response, 429, "QUOTA_EXHAUSTED");
  }
  const sub = () => handle(request("/subscription", "demo-pro"));
  assert.deepEqual(await (await sub()).json(), {
    tier: "PRO", monthlyLimit: 100, remaining: 0, resetsAt: "2026-12-01T00:00:00Z",
  });
  now = "2026-12-01T00:00:00Z";
  assert.deepEqual(await (await sub()).json(), {
    tier: "PRO", monthlyLimit: 100, remaining: 100, resetsAt: "2027-01-01T00:00:00Z",
  });
  assert.equal((await analyze()).status, 200);
});

test("invalid provider results and exceptions consume the slot and return a generic 502", async () => {
  const deps = mockDependencies();
  const outputs = [
    { difficultyScore: "1", estimatedMinutes: 5 },
    { difficultyScore: 6, estimatedMinutes: 5 },
    { difficultyScore: 4, estimatedMinutes: 10081 },
    { difficultyScore: 4, estimatedMinutes: 1.5 },
    { difficultyScore: 4, estimatedMinutes: 5, extra: true },
  ];
  deps.analyze = async () => {
    if (outputs.length) return outputs.shift();
    throw new Error("private provider failure");
  };
  const handle = createHandler(deps);
  for (let attempt = 0; attempt < 6; attempt++) {
    const response = await handle(request("/analyze-task", "demo-pro", JSON.stringify(input)));
    await checkError(response, 502, "AI_UNAVAILABLE");
  }
  assert.equal((await (await handle(request("/subscription", "demo-pro"))).json()).remaining, 94);
});
