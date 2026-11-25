import { createHandler } from "./handler.js";
import { geminiConfig, providerAnalyzer } from "./providers.js";
import type { ProviderEnv } from "./providers.js";
import { supabase, supabaseConfig } from "./supabase.js";
import type { SupabaseEnv } from "./supabase.js";

export type Env = SupabaseEnv & ProviderEnv;

export function realHandler(env: Env) {
  return createHandler({
    ...supabase(env),
    analyze: providerAnalyzer(env),
    readiness() {
      const { origin, publishableKey, secretKey } = supabaseConfig(env);
      const { key, model } = geminiConfig(env);
      return {
        ready: Boolean(origin && publishableKey && secretKey && key && model),
        configured: {
          SUPABASE_URL: Boolean(origin),
          SUPABASE_PUBLISHABLE_KEY: Boolean(publishableKey),
          GEMINI_MODEL: Boolean(model),
        },
      };
    },
  });
}
