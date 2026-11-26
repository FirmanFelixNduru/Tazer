import assert from "node:assert/strict";
import { once } from "node:events";
import { request } from "node:http";
import type { OutgoingHttpHeaders, Server } from "node:http";
import test from "node:test";
import type { TestContext } from "node:test";
import { createNodeServer, loopback } from "../src/node.js";

const task = { title: "Implementasi sorting", course: "Algoritma", description: "", deadlineAt: "2026-12-02T10:00:00Z" };

async function listen(t: TestContext, env: Parameters<typeof createNodeServer>[0]) {
  const server = createNodeServer(env);
  server.listen(0, loopback);
  await once(server, "listening");
  t.after(() => new Promise<void>((resolve, reject) => {
    server.close(error => error ? reject(error) : resolve());
  }));
  const address = server.address();
  assert.ok(address && typeof address === "object");
  return { server, origin: `http://${loopback}:${address.port}` };
}

function rawRequest(server: Server, path: string, headers: OutgoingHttpHeaders, chunks?: string[]) {
  const address = server.address();
  assert.ok(address && typeof address === "object");
  return new Promise<{ status: number | undefined; body: unknown }>((resolve, reject) => {
    const req = request({ hostname: loopback, port: address.port, path, headers, method: chunks ? "POST" : "GET" }, res => {
      res.setEncoding("utf8");
      let body = "";
      res.on("data", chunk => { body += chunk; });
      res.on("error", reject);
      res.on("end", () => {
        try {
          resolve({ status: res.statusCode, body: JSON.parse(body) });
        } catch (error) {
          reject(error);
        }
      });
    });
    req.on("error", reject);
    for (const chunk of chunks ?? []) req.write(chunk);
    req.end();
  });
}

test("Node bridge serves all three endpoints over real loopback HTTP", async t => {
  const { origin } = await listen(t, { TAZER_MODE: "mock" });
  const headers = { Authorization: "Bearer demo-pro" };
  const session = await fetch(origin + "/auth/session", { headers });
  assert.equal(session.status, 200);
  assert.deepEqual(await session.json(), { uid: "demo-pro" });
  const analysis = await fetch(origin + "/analyze-task", {
    method: "POST",
    headers: { ...headers, "Content-Type": "application/json" },
    body: JSON.stringify(task),
  });
  assert.equal(analysis.status, 200);
  assert.equal(analysis.headers.get("cache-control"), "no-store");
  assert.deepEqual(await analysis.json(), { difficultyScore: 4, estimatedMinutes: 150 });
  const sub = await fetch(origin + "/subscription", { headers });
  assert.equal(sub.status, 200);
  const usage = await sub.json();
  assert.equal(usage.tier, "PRO");
  assert.equal(usage.remaining, 99);
});

test("Node bridge bounds chunked input and refuses mock traffic with a foreign Host or Origin", async t => {
  const { server } = await listen(t, { TAZER_MODE: "mock" });
  const auth = { Authorization: "Bearer demo-pro" };
  const large = await rawRequest(server, "/analyze-task", {
    ...auth, "Content-Type": "application/json", "Transfer-Encoding": "chunked",
  }, [" ".repeat(16_385), " ".repeat(16_385)]);
  assert.equal(large.status, 413);
  assert.deepEqual(large.body, { error: { code: "PAYLOAD_TOO_LARGE", message: "Ukuran input analisis melebihi 32 KiB." } });
  const foreignHost = await rawRequest(server, "/auth/session", { ...auth, Host: "external.example.invalid" });
  const foreignOrigin = await rawRequest(server, "/auth/session", { ...auth, Origin: "https://external.example.invalid" });
  assert.equal(foreignHost.status, 403);
  assert.equal(foreignOrigin.status, 403);
});

test("Node mock requires explicit development configuration", async t => {
  assert.throws(() => createNodeServer({ TAZER_MODE: "mock", NODE_ENV: "production" }), /Mock mode/);
  assert.throws(() => createNodeServer({ TAZER_MODE: "unknown" }), /TAZER_MODE/);
  const { origin } = await listen(t, {});
  const response = await fetch(origin + "/auth/session", { headers: { Authorization: "Bearer demo-pro" } });
  assert.equal(response.status, 503);
  assert.equal((await response.json()).error.code, "VERIFICATION_UNAVAILABLE");
});
