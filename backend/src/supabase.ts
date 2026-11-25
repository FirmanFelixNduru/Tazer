import { integerBetween, isUtcTimestamp, object } from "./analysis.js";
import type { Dependencies, Tier } from "./handler.js";

export interface SupabaseEnv {
  SUPABASE_URL?: string;
  SUPABASE_PUBLISHABLE_KEY?: string;
  SUPABASE_SECRET_KEY?: string;
}

const uuid = /^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$/i;
const invalidAuthCodes = new Set([
  "bad_jwt", "no_authorization", "session_not_found", "session_expired", "user_not_found",
  "user_banned", "invalid_jwt",
]);

export function supabaseConfig(env: SupabaseEnv) {
  return {
    origin: httpsOrigin(env.SUPABASE_URL),
    publishableKey: supabaseKey(env.SUPABASE_PUBLISHABLE_KEY, "sb_publishable_"),
    secretKey: supabaseKey(env.SUPABASE_SECRET_KEY, "sb_secret_"),
  };
}

export function supabase(env: SupabaseEnv, fetcher: typeof fetch = (input, init) => globalThis.fetch(input, init)): Pick<Dependencies, "verify" | "entitlement"> {
  const { origin, publishableKey, secretKey } = supabaseConfig(env);

  return {
    async verify(token) {
      if (!origin || !publishableKey) throw new Error("Auth unavailable");
      const response = await fetcher(new URL("/auth/v1/user", origin).href, {
        headers: { apikey: publishableKey, Authorization: `Bearer ${token}` },
        redirect: "manual",
        signal: AbortSignal.timeout(5000),
      });
      if (!response.ok) {
        if (response.status === 400 || response.status === 401 || response.status === 403) {
          const error = object(await response.json().catch(() => undefined));
          const code = typeof error?.error_code === "string" ? error.error_code : error?.code;
          if (code === "email_not_confirmed") return "unverified";
          if (typeof code === "string" && invalidAuthCodes.has(code)) return "invalid";
          const badKey = code === "invalid_api_key" ||
            (typeof error?.message === "string" && /api[ -]?key/i.test(error.message));
          if (!badKey && response.status !== 400) return "invalid";
        }
        throw new Error("Auth unavailable");
      }
      const user = object(await response.json());
      if (typeof user?.id !== "string" || !uuid.test(user.id)) throw new Error("Invalid Auth response");
      if (
        typeof user.email !== "string" || user.email.trim() === "" ||
        typeof user.email_confirmed_at !== "string" || !Number.isFinite(Date.parse(user.email_confirmed_at))
      ) return "unverified";
      return { uid: user.id };
    },

    async entitlement(uid, consume) {
      if (!origin || !secretKey) throw new Error("Database unavailable");
      const response = await fetcher(new URL("/rest/v1/rpc/tazer_entitlement", origin).href, {
        method: "POST",
        headers: { apikey: secretKey, "Content-Type": "application/json" },
        body: JSON.stringify({ p_user_id: uid, p_consume: consume }),
        redirect: "manual",
        signal: AbortSignal.timeout(5000),
      });
      if (!response.ok) throw new Error("Database unavailable");
      const rows: unknown = await response.json();
      const row = Array.isArray(rows) && rows.length === 1 ? object(rows[0]) : undefined;
      if (!row) throw new Error("Invalid entitlement response");
      const { tier, monthly_limit: monthlyLimit, remaining, resets_at: resetsAt, granted } = row;
      if (
        !isTier(tier) || !integerBetween(monthlyLimit, 0, 2147483647) ||
        !integerBetween(remaining, 0, monthlyLimit) ||
        !isUtcTimestamp(resetsAt) || typeof granted !== "boolean" ||
        (tier === "PRO" ? monthlyLimit === 0 : monthlyLimit !== 0) ||
        (granted && (tier !== "PRO" || !consume || remaining === monthlyLimit)) ||
        (consume && tier === "PRO" && !granted && remaining !== 0)
      ) throw new Error("Invalid entitlement response");
      return { tier, monthlyLimit, remaining, resetsAt, granted };
    },
  };
}

function isTier(value: unknown): value is Tier {
  return value === "FREE" || value === "PRO" || value === "BYOK";
}

function supabaseKey(value: string | undefined, prefix: string): string | undefined {
  if (typeof value !== "string") return;
  const key = value.trim();
  if (key.startsWith(prefix) && key.length > prefix.length) return key;
}

function httpsOrigin(value: string | undefined): URL | undefined {
  if (typeof value !== "string") return;
  try {
    const url = new URL(value);
    if (url.protocol === "https:" && url.href === `${url.origin}/`) return url;
  } catch {
    return undefined;
  }
}
