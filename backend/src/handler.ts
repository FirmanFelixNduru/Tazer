import { analysisOutput, taskInput } from "./analysis.js";
import type { TaskInput } from "./analysis.js";

export type Tier = "FREE" | "PRO" | "BYOK";

export interface Subscription {
  tier: Tier;
  monthlyLimit: number;
  remaining: number;
  resetsAt: string;
}

export type Session = { uid: string } | "invalid" | "unverified";

interface Readiness {
  ready: boolean;
  configured: {
    SUPABASE_URL: boolean;
    SUPABASE_PUBLISHABLE_KEY: boolean;
    GEMINI_MODEL: boolean;
  };
}

export interface Dependencies {
  verify(token: string): Promise<Session>;
  entitlement(uid: string, consume: boolean): Promise<Subscription & { granted: boolean }>;
  analyze(input: TaskInput, includeSubtasks: boolean): Promise<unknown>;
  now?: () => Date;
  readiness?: () => Readiness;
}

const MAX_BODY_BYTES = 32 * 1024;

const errors = {
  VALIDATION_ERROR: [400, "Input analisis tidak valid."],
  PAYLOAD_TOO_LARGE: [413, "Ukuran input analisis melebihi 32 KiB."],
  INVALID_AUTH: [401, "Sesi tidak valid."],
  EMAIL_UNVERIFIED: [403, "Verifikasi email diperlukan."],
  PRO_REQUIRED: [403, "Analisis melalui server memerlukan PRO."],
  QUOTA_EXHAUSTED: [429, "Kuota analisis bulanan habis."],
  AI_UNAVAILABLE: [502, "Analisis AI tidak tersedia. Gunakan analisis offline."],
  VERIFICATION_UNAVAILABLE: [503, "Verifikasi tidak tersedia."],
  NOT_FOUND: [404, "Endpoint tidak ditemukan."],
  METHOD_NOT_ALLOWED: [405, "Metode HTTP tidak didukung."],
} as const;

function json(value: unknown, status = 200, headers?: Record<string, string>) {
  return Response.json(value, { status, headers: { "Cache-Control": "no-store", ...headers } });
}

function error(code: keyof typeof errors, headers?: Record<string, string>) {
  const [status, message] = errors[code];
  return json({ error: { code, message } }, status, headers);
}

async function readTask(request: Request): Promise<TaskInput | undefined | "too-large"> {
  if (Number(request.headers.get("content-length")) > MAX_BODY_BYTES) return "too-large";
  const mime = request.headers.get("content-type")?.split(";")[0]?.trim().toLowerCase();
  if (mime !== "application/json" || !request.body) return;

  const reader = request.body.getReader();
  const decoder = new TextDecoder("utf-8", { fatal: true });
  let size = 0;
  let text = "";
  try {
    while (true) {
      const chunk = await reader.read();
      if (chunk.done) break;
      size += chunk.value.byteLength;
      if (size > MAX_BODY_BYTES) {
        void reader.cancel().catch(() => {});
        return "too-large";
      }
      text += decoder.decode(chunk.value, { stream: true });
    }
    return taskInput(JSON.parse(text + decoder.decode()));
  } catch {
    return undefined;
  } finally {
    reader.releaseLock();
  }
}

export function createHandler(deps: Dependencies) {
  return async (request: Request): Promise<Response> => {
    const path = new URL(request.url).pathname;
    if (path === "/health" && deps.readiness) {
      if (request.method !== "GET") return error("METHOD_NOT_ALLOWED", { Allow: "GET" });
      const health = deps.readiness();
      return json(health, health.ready ? 200 : 503);
    }
    const method = path === "/analyze-task" ? "POST" : "GET";
    if (path !== "/auth/session" && path !== "/subscription" && path !== "/analyze-task") {
      return error("NOT_FOUND");
    }
    if (request.method !== method) return error("METHOD_NOT_ALLOWED", { Allow: method });

    const token = /^Bearer ([A-Za-z0-9._~+/-]+=*)$/i.exec(request.headers.get("authorization") ?? "")?.[1];
    if (!token) return error("INVALID_AUTH", { "WWW-Authenticate": "Bearer" });

    let session: Session;
    try {
      session = await deps.verify(token);
    } catch {
      return error("VERIFICATION_UNAVAILABLE");
    }
    if (session === "invalid") return error("INVALID_AUTH", { "WWW-Authenticate": "Bearer" });
    if (session === "unverified") return error("EMAIL_UNVERIFIED");
    if (path === "/auth/session") return json({ uid: session.uid });

    const consuming = path === "/analyze-task";
    const input = consuming ? await readTask(request) : undefined;
    if (input === "too-large") return error("PAYLOAD_TOO_LARGE");
    if (consuming && !input) return error("VALIDATION_ERROR");

    let entitlement: Awaited<ReturnType<Dependencies["entitlement"]>>;
    try {
      entitlement = await deps.entitlement(session.uid, consuming);
    } catch {
      return error("VERIFICATION_UNAVAILABLE");
    }
    const { tier, monthlyLimit, remaining, resetsAt, granted } = entitlement;
    if (!input) return json({ tier, monthlyLimit, remaining, resetsAt });
    if (tier !== "PRO") return error("PRO_REQUIRED");
    if (!granted) {
      const now = deps.now?.() ?? new Date();
      const seconds = Math.max(1, Math.ceil((Date.parse(resetsAt) - now.getTime()) / 1000));
      return error("QUOTA_EXHAUSTED", { "Retry-After": String(seconds) });
    }

    try {
      const includeSubtasks = request.headers.get("X-Tazer-Analysis-Subtasks") === "1";
      const result = analysisOutput(await deps.analyze(input, includeSubtasks));
      if (result) {
        // Older Android builds require exactly two response fields.
        if (includeSubtasks) return json(result);
        const { difficultyScore, estimatedMinutes } = result;
        return json({ difficultyScore, estimatedMinutes });
      }
    } catch {
      // The provider failure is reported without upstream data or credentials.
    }
    return error("AI_UNAVAILABLE");
  };
}
