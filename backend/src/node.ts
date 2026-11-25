import { createServer } from "node:http";
import type { IncomingMessage } from "node:http";
import { realHandler } from "./config.js";
import type { Env } from "./config.js";
import { createHandler } from "./handler.js";
import { mockDependencies } from "./mock.js";

export const loopback = "127.0.0.1";

interface NodeEnv extends Env {
  TAZER_MODE?: string;
  NODE_ENV?: string;
}

export function createNodeServer(env: NodeEnv) {
  const mock = env.TAZER_MODE === "mock";
  if (env.TAZER_MODE && !mock && env.TAZER_MODE !== "supabase") {
    throw new Error("Unsupported TAZER_MODE");
  }
  if (mock && env.NODE_ENV === "production") throw new Error("Mock mode requires local development");
  const handler = mock ? createHandler(mockDependencies()) : realHandler(env);

  return createServer({ requestTimeout: 30_000, headersTimeout: 10_000 }, async (incoming, outgoing) => {
    const { localAddress, localPort, remoteAddress } = incoming.socket;
    const origin = `http://${loopback}:${localPort}`;
    const target = incoming.url;
    const method = incoming.method;
    let response: Response;
    try {
      if (
        mock && (
          localAddress !== loopback || remoteAddress !== loopback ||
          incoming.headers.host !== `${loopback}:${localPort}` ||
          (incoming.headers.origin && incoming.headers.origin !== origin)
        )
      ) {
        response = bridgeError(403, "REQUEST_FORBIDDEN", "Akses mock dibatasi ke loopback.");
      } else if (!target?.startsWith("/") || target.startsWith("//") || !method) {
        response = bridgeError(400, "VALIDATION_ERROR", "Request HTTP tidak valid.");
      } else {
        const headers = new Headers();
        for (let i = 0; i < incoming.rawHeaders.length; i += 2) {
          headers.append(incoming.rawHeaders[i]!, incoming.rawHeaders[i + 1]!);
        }
        const init: RequestInit & { duplex?: "half" } = { method, headers };
        if (method === "POST" && (headers.has("content-length") || headers.has("transfer-encoding"))) {
          init.body = requestBody(incoming);
          init.duplex = "half";
        }
        response = await handler(new Request(origin + target, init));
      }
    } catch {
      response = bridgeError(503, "VERIFICATION_UNAVAILABLE", "Verifikasi tidak tersedia.");
    }
    outgoing.statusCode = response.status;
    response.headers.forEach((value, name) => outgoing.setHeader(name, value));
    outgoing.end(method === "HEAD" ? undefined : Buffer.from(await response.arrayBuffer()));
  });
}

function requestBody(incoming: IncomingMessage): ReadableStream<Uint8Array> {
  const chunks = incoming.iterator({ destroyOnReturn: false });
  return new ReadableStream({
    async pull(controller) {
      const chunk = await chunks.next();
      if (chunk.done) controller.close();
      else controller.enqueue(chunk.value);
    },
    async cancel() {
      // Let Node drain any remaining request data after sending the error response.
      await chunks.return?.();
    },
  }, { highWaterMark: 0 });
}

function bridgeError(status: number, code: string, message: string) {
  return Response.json({ error: { code, message } }, { status, headers: { "Cache-Control": "no-store" } });
}
