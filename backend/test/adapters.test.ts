import assert from "node:assert/strict";
import test from "node:test";
import { analysisSchema } from "../src/analysis.js";
import { createHandler } from "../src/handler.js";
import { providerAnalyzer } from "../src/providers.js";
import { supabase } from "../src/supabase.js";
import worker from "../src/worker.js";

const uid = "854359ab-dcb7-4b61-8a94-4a56e153bca1";
const auth = { id: uid, email: "demo@example.invalid", email_confirmed_at: "2026-11-01T01:00:00+00:00" };
const entitlement = { tier: "PRO", monthly_limit: 20, remaining: 19, resets_at: "2026-12-01T00:00:00Z", granted: true };
const input = { title: "Sorting", course: "Algorithms", description: "", deadlineAt: "2026-11-03T00:00:00Z" };
const result = { difficultyScore: 4, estimatedMinutes: 150 };
const env = {
  SUPABASE_URL: "https://project.example.invalid",
  SUPABASE_PUBLISHABLE_KEY: "sb_publishable_test",
  SUPABASE_SECRET_KEY: "sb_secret_test",
  GEMINI_MODEL: "gemini-test",
  GEMINI_API_KEY: "test-gemini",
  GROQ_MODEL: "test/model",
  GROQ_API_KEY: "test-groq",
};

function analyzeRequest() {
  return new Request("https://backend.example.invalid/analyze-task", {
    method: "POST",
    body: JSON.stringify(input),
    headers: { Authorization: "Bearer auth-token", "Content-Type": "application/json" },
  });
}

test("Supabase auth then atomic quota, then Gemini and Groq fallback with one charge", async () => {
  const hits: string[] = [];
  const fetcher: typeof fetch = async (resource, init) => {
    const url = new URL(String(resource));
    const headers = new Headers(init?.headers);
    hits.push(url.pathname);
    assert.equal(init?.redirect, "error");
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
    return Response.json({ choices: [{ finish_reason: "stop", message: { content: JSON.stringify(result) } }] });
  };
  const handle = createHandler({ ...supabase(env, fetcher), analyze: providerAnalyzer(env, fetcher) });
  const response = await handle(analyzeRequest());
  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), result);
  assert.deepEqual(hits, [
    "/auth/v1/user",
    "/rest/v1/rpc/tazer_entitlement",
    "/v1beta/models/gemini-test:generateContent",
    "/openai/v1/chat/completions",
  ]);
});

test("Supabase rejects bad, expired, revoked, or unverified sessions and fails closed on backend errors", async () => {
  const cases: [number, object, number, string][] = [
    [401, { code: "bad_jwt" }, 401, "INVALID_AUTH"],
    [400, { error_code: "bad_jwt", msg: "JWT expired" }, 401, "INVALID_AUTH"],
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
    const handle = createHandler({ ...supabase(env, fetcher), analyze: providerAnalyzer(env, fetcher) });
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
    const handle = createHandler({ ...supabase(env, fetcher), analyze: providerAnalyzer(env, fetcher) });
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
  const fetcher: typeof fetch = async () => {
    calls++;
    return Response.json({ candidates: [{
      finishReason: "STOP",
      content: { parts: [{ text: "private reasoning", thought: true }, { text: JSON.stringify(result) }] },
    }] });
  };
  assert.deepEqual(await providerAnalyzer(env, fetcher)(input), result);
  assert.equal(calls, 1);
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
