import { createHandler } from "./handler.js";
import { providerAnalyzer } from "./providers.js";
import type { ProviderEnv } from "./providers.js";
import { supabase } from "./supabase.js";
import type { SupabaseEnv } from "./supabase.js";

export type Env = SupabaseEnv & ProviderEnv;

export function realHandler(env: Env) {
  return createHandler({ ...supabase(env), analyze: providerAnalyzer(env) });
}
