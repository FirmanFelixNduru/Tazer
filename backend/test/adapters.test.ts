import assert from "node:assert/strict";
import test from "node:test";
import { analysisSchema } from "../src/analysis.js";
import { createHandler } from "../src/handler.js";
import { providerAnalyzer, providerImageExtractor, providerScheduleExtractor } from "../src/providers.js";
import { supabase } from "../src/supabase.js";
import worker from "../src/worker.js";

const uid = "854359ab-dcb7-4b61-8a94-4a56e153bca1";
const auth = { id: uid, email: "demo@example.invalid", email_confirmed_at: "2026-11-01T01:00:00+00:00" };
const entitlement = { tier: "PRO", monthly_limit: 100, remaining: 99, resets_at: "2026-12-01T00:00:00Z", granted: true };
const input = { title: "Sorting", course: "Algorithms", description: "", deadlineAt: "2026-11-03T00:00:00Z" };
const result = { difficultyScore: 4, estimatedMinutes: 150 };
const suggested = { ...result, subtasks: ["Pick a sorting algorithm", "Implement it", "Test duplicate values"] };
const env = {
  SUPABASE_URL: "https://project.example.invalid",
  SUPABASE_PUBLISHABLE_KEY: "sb_publishable_test",
  SUPABASE_SECRET_KEY: "sb_secret_test",
  GEMINI_MODEL: "gemini-3.5-flash-lite",
  GEMINI_API_KEY: "test-gemini",
  GROQ_MODEL: "test/model",
  GROQ_API_KEY: "test-groq",
};

function analyzeRequest() {
  return new Request("https://backend.example.invalid/analyze-task", {
    method: "POST",
    body: JSON.stringify(input),
    headers: { Authorization: "Bearer auth-token", "Content-Type": "application/json", "X-Tazer-Analysis-Subtasks": "1" },
  });
}

test("Supabase auth then atomic quota, then Gemini and Groq fallback with one charge", async () => {
  const hits: string[] = [];
  const fetcher: typeof fetch = async (resource, init) => {
    const url = new URL(String(resource));
    const headers = new Headers(init?.headers);
    hits.push(url.pathname);
    assert.equal(init?.redirect, "manual");
    assert.ok(init?.signal);
    if (url.pathname === "/auth/v1/user") {
      assert.equal(headers.get("apikey"), env.SUPABASE_PUBLISHABLE_KEY);
      assert.equal(headers.get("authorization"), "Bearer auth-token");
      return Response.json(auth);
    }
    const body = JSON.parse(String(init?.body));
    if (url.pathname === "/rest/v1/rpc/tazer_entitlement") {
      assert.equal(headers.get("apikey"), env.SUPABASE_SECRET_KEY);
      assert.equal(headers.get("authorization"), null);
      assert.deepEqual(body, { p_user_id: uid, p_consume: true });
      return Response.json([entitlement]);
    }
    assert.equal(headers.get("apikey"), null);
    if (url.hostname === "generativelanguage.googleapis.com") {
      assert.equal(headers.get("x-goog-api-key"), env.GEMINI_API_KEY);
      assert.equal(headers.get("authorization"), null);
      assert.deepEqual(body.generationConfig.responseJsonSchema, analysisSchema);
      assert.equal(body.generationConfig.responseMimeType, "application/json");
      assert.deepEqual(JSON.parse(body.contents[0].parts[0].text), input);
      return Response.json({
        candidates: [{ finishReason: "STOP", content: { parts: [{ text: '{"difficultyScore":3.5,"estimatedMinutes":150}' }] } }],
      });
    }
    assert.equal(url.hostname, "api.groq.com");
    assert.equal(headers.get("authorization"), `Bearer ${env.GROQ_API_KEY}`);
    assert.equal(body.model, env.GROQ_MODEL);
    assert.equal(body.response_format.type, "json_schema");
    assert.equal(body.response_format.json_schema.strict, true);
    assert.deepEqual(body.response_format.json_schema.schema, analysisSchema);
    assert.deepEqual(JSON.parse(body.messages[1].content), input);
    return Response.json({ choices: [{ finish_reason: "stop", message: { content: JSON.stringify(suggested), refusal: null } }] });
  };
  const handle = createHandler({ ...supabase(env, fetcher), analyze: providerAnalyzer(env, fetcher), extractImage: providerImageExtractor(env, fetcher), extractSchedule: providerScheduleExtractor(env, fetcher) });
  const response = await handle(analyzeRequest());
  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), suggested);
  assert.deepEqual(hits, [
    "/auth/v1/user",
    "/rest/v1/rpc/tazer_entitlement",
    "/v1beta/models/gemini-3.5-flash-lite:generateContent",
    "/openai/v1/chat/completions",
  ]);
});

test("Supabase rejects bad, expired, revoked, or unverified sessions and fails closed on backend errors", async () => {
  const cases: [number, object, number, string][] = [
    [302, {}, 503, "VERIFICATION_UNAVAILABLE"],
    [401, { code: "bad_jwt" }, 401, "INVALID_AUTH"],
    [400, { error_code: "bad_jwt", msg: "JWT expired" }, 401, "INVALID_AUTH"],
    [403, { code: 403, error_code: "bad_jwt" }, 401, "INVALID_AUTH"],
    [403, { code: "session_not_found" }, 401, "INVALID_AUTH"],
    [200, { ...auth, email_confirmed_at: null, confirmed_at: auth.email_confirmed_at }, 403, "EMAIL_UNVERIFIED"],
    [200, { ...auth, id: "malformed-uid" }, 503, "VERIFICATION_UNAVAILABLE"],
    [401, { message: "Invalid API key" }, 503, "VERIFICATION_UNAVAILABLE"],
    [503, { message: "Auth offline" }, 503, "VERIFICATION_UNAVAILABLE"],
  ];
  for (const [upstreamStatus, body, status, code] of cases) {
    let calls = 0;
    const fetcher: typeof fetch = async () => {
      calls++;
      return Response.json(body, { status: upstreamStatus });
    };
    const handle = createHandler({ ...supabase(env, fetcher), analyze: providerAnalyzer(env, fetcher), extractImage: providerImageExtractor(env, fetcher), extractSchedule: providerScheduleExtractor(env, fetcher) });
    const response = await handle(analyzeRequest());
    assert.equal(response.status, status);
    assert.equal((await response.json()).error.code, code);
    assert.equal(calls, 1);
  }
});

test("malformed or unavailable quota RPC never invokes providers", async () => {
  for (const reply of [
    Response.json({ error: "database detail" }, { status: 503 }),
    Response.json([]),
    Response.json([{ ...entitlement, tier: "BYOK" }]),
    Response.json([{ ...entitlement, granted: false }]),
    Response.json([{ ...entitlement, resets_at: "tomorrow" }]),
  ]) {
    let calls = 0;
    const fetcher: typeof fetch = async () => ++calls === 1 ? Response.json(auth) : reply;
    const handle = createHandler({ ...supabase(env, fetcher), analyze: providerAnalyzer(env, fetcher), extractImage: providerImageExtractor(env, fetcher), extractSchedule: providerScheduleExtractor(env, fetcher) });
    const response = await handle(analyzeRequest());
    assert.equal(response.status, 503);
    assert.deepEqual(await response.json(), {
      error: { code: "VERIFICATION_UNAVAILABLE", message: "Verifikasi tidak tersedia." },
    });
    assert.equal(calls, 2);
  }
});

test("Gemini valid structured output takes precedence and skips thought parts", async () => {
  let calls = 0;
  const prompts: string[] = [];
  const fetcher: typeof fetch = async (_url, init) => {
    calls++;
    prompts.push(JSON.parse(String(init?.body)).systemInstruction.parts[0].text);
    const text = JSON.stringify(suggested);
    return Response.json({ candidates: [{
      finishReason: "STOP",
      content: { parts: [{ text: "private reasoning", thought: true }, { text: text.slice(0, 20) }, { text: text.slice(20) }] },
    }] });
  };
  assert.deepEqual(await providerAnalyzer(env, fetcher)(input), suggested);
  assert.equal(calls, 1);
  assert.match(prompts[0]!, /3 to 5 concrete.*120 characters/);
  assert.ok(analysisSchema.required.includes("subtasks"));
  assert.equal(analysisSchema.properties.subtasks.minItems, 3);
  assert.equal(analysisSchema.properties.subtasks.maxItems, 5);
});

test("blocked or incomplete Gemini responses fall back to a legacy Groq result", async () => {
  const content = { parts: [{ text: JSON.stringify(suggested) }] };
  for (const payload of [
    { promptFeedback: { blockReason: "SAFETY" } },
    { candidates: [{ finishReason: "MAX_TOKENS", content }] },
    { candidates: [{ finishReason: "STOP", content: { parts: [{ text: 12 }] } }] },
  ]) {
    let calls = 0;
    const fetcher: typeof fetch = async () => ++calls === 1
      ? Response.json(payload)
      : Response.json({ choices: [{ finish_reason: "stop", message: { content: JSON.stringify(result) } }] });
    assert.deepEqual(await providerAnalyzer(env, fetcher)(input), result);
    assert.equal(calls, 2);
  }
});

test("Gemini keeps valid estimates with legacy or malformed optional suggestions", async () => {
  const cases = [result, { ...result, subtasks: ["Implement it", "   ", 42] }];
  for (const output of cases) {
    let calls = 0;
    const fetcher: typeof fetch = async () => {
      calls++;
      return Response.json({ candidates: [{
        finishReason: "STOP", content: { parts: [{ text: JSON.stringify(output) }] },
      }] });
    };
    assert.deepEqual(await providerAnalyzer(env, fetcher)(input), result);
    assert.equal(calls, 1);
  }
});

test("Gemini failure can fall back, and Groq refusals and malformed output are rejected", async () => {
  for (const message of [
    { content: JSON.stringify(result), refusal: "refused" },
    { content: "not JSON" },
    { content: '{"difficultyScore":2,"estimatedMinutes":0}' },
  ]) {
    let calls = 0;
    const fetcher: typeof fetch = async () => {
      if (++calls === 1) throw new DOMException("timeout", "TimeoutError");
      return Response.json({ choices: [{ finish_reason: "stop", message }] });
    };
    assert.equal(await providerAnalyzer(env, fetcher)(input), undefined);
    assert.equal(calls, 2);
  }
});

test("Workers entry point fails closed even when presented a local demo token", async () => {
  const response = await worker.fetch(new Request("https://backend.example.invalid/auth/session", {
    headers: { Authorization: "Bearer demo-pro" },
  }), {});
  assert.equal(response.status, 503);
  assert.equal((await response.json()).error.code, "VERIFICATION_UNAVAILABLE");
});

test("public Worker health reports only configuration booleans and makes no upstream requests", async t => {
  const fetcher = t.mock.method(globalThis, "fetch", async () => {
    throw new Error("Unexpected upstream request");
  });
  const request = new Request("https://backend.example.invalid/health");
  const { GROQ_MODEL: _groqModel, GROQ_API_KEY: _groqKey, ...requiredEnv } = env;
  const configured = { SUPABASE_URL: true, SUPABASE_PUBLISHABLE_KEY: true, GEMINI_MODEL: true };
  const response = await worker.fetch(request, { ...requiredEnv, GEMINI_MODEL: "gemini-3.5-flash-lite" });
  assert.equal(response.status, 200);
  assert.equal(response.headers.get("Cache-Control"), "no-store");
  assert.deepEqual(await response.json(), { ready: true, configured });

  for (const key of Object.keys(configured) as Array<keyof typeof configured>) {
    const missing = await worker.fetch(request, { ...requiredEnv, [key]: undefined });
    assert.equal(missing.status, 503, key);
    assert.deepEqual(await missing.json(), { ready: false, configured: { ...configured, [key]: false } });
  }
  for (const key of ["SUPABASE_SECRET_KEY", "GEMINI_API_KEY"]) {
    const missing = await worker.fetch(request, { ...env, [key]: "  " });
    assert.equal(missing.status, 503, key);
    assert.deepEqual(await missing.json(), { ready: false, configured });
  }
  const invalid = await worker.fetch(request, {
    ...requiredEnv,
    SUPABASE_URL: "http://project.example.invalid",
    SUPABASE_PUBLISHABLE_KEY: "sb_publishable_",
    GEMINI_MODEL: "models/gemini-test",
  });
  assert.equal(invalid.status, 503);
  assert.deepEqual(await invalid.json(), {
    ready: false,
    configured: { SUPABASE_URL: false, SUPABASE_PUBLISHABLE_KEY: false, GEMINI_MODEL: false },
  });

  const wrongMethod = await worker.fetch(new Request(request.url, { method: "POST" }), env);
  assert.equal(wrongMethod.status, 405);
  assert.equal(wrongMethod.headers.get("Allow"), "GET");
  assert.equal(fetcher.mock.callCount(), 0);
});

test("Worker endpoints still require a Bearer token whether or not the Worker is ready", async t => {
  const fetcher = t.mock.method(globalThis, "fetch", async () => {
    throw new Error("Unexpected upstream request");
  });
  for (const config of [env, {}]) {
    for (const [method, path] of [["GET", "/auth/session"], ["GET", "/subscription"], ["POST", "/analyze-task"], ["POST", "/extract-task-image"], ["POST", "/extract-schedule-image"]]) {
      const response = await worker.fetch(new Request(`https://backend.example.invalid${path}`, { method }), config);
      assert.equal(response.status, 401, path);
      assert.equal(response.headers.get("WWW-Authenticate"), "Bearer");
      assert.equal((await response.json()).error.code, "INVALID_AUTH");
    }
  }
  assert.equal(fetcher.mock.callCount(), 0);
});
