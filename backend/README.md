# Tazer backend

Node.js 22.9+ and npm. Run these commands from `backend/`.

```powershell
npm ci
npm test
npm run build
$env:TAZER_MODE = 'mock'
npm start
```

In a second PowerShell terminal, run the smoke requests.

```powershell
$base = 'http://127.0.0.1:8787'
$headers = @{ Authorization = 'Bearer demo-pro' }
Invoke-RestMethod "$base/auth/session" -Headers $headers
Invoke-RestMethod "$base/subscription" -Headers $headers
$task = @{
  title = 'Implementasi sorting'
  course = 'Algoritma'
  description = 'Coding kelompok dan presentasi hasil'
  deadlineAt = '2026-12-02T10:00:00Z'
} | ConvertTo-Json
Invoke-RestMethod "$base/analyze-task" -Method Post -Headers $headers -ContentType 'application/json' -Body $task
Invoke-RestMethod "$base/subscription" -Headers $headers
```

A fresh process gives `demo-pro` 20 monthly slots. The sample analysis returns `{"difficultyScore":4,"estimatedMinutes":150}` and leaves 19 slots. `demo-free` and `demo-byok` return zero server slots and reject analysis with `PRO_REQUIRED`. `demo-unverified` returns `EMAIL_UNVERIFIED`. `demo-expired`, `demo-revoked`, and unknown tokens return `INVALID_AUTH`. Node listens on IPv4 loopback and accepts mock requests addressed to `127.0.0.1` on its listening port. Restarting Node resets mock usage.

## Supabase and providers

1. Apply [the migration](supabase/migrations/20261125000000_entitlement.sql) to a Supabase Free project. It gives the REST RPC to `service_role` and defaults users without an entitlement row to `FREE`.
2. In the Supabase SQL editor, grant a verified test user `PRO` by adding an `account_entitlements` row with that user's Auth UUID. The primary key is `user_id`. Change the row to `FREE` or `BYOK` to revoke access to server analysis.
3. Copy `.env.example` to `.env` and fill in the Supabase URL and keys on the server. Remove the mock setting from the Node terminal with `Remove-Item Env:TAZER_MODE -ErrorAction SilentlyContinue`, then run `npm start`. Use a real Supabase access token for the smoke requests.
4. Configure a Gemini AI Studio free-tier key and approved model. Configure Groq Free only if fallback is desired. Select a Groq model that supports strict JSON Schema. Keep billing disabled. The server sends the validated task to Gemini, then to Groq if the first attempt fails and Groq is configured. A valid PRO request consumes one slot even if both attempts fail. [Gemini pricing](https://ai.google.dev/gemini-api/docs/pricing) · [Groq structured output](https://console.groq.com/docs/structured-outputs) · [Groq limits](https://console.groq.com/docs/rate-limits)
5. For Cloudflare Workers Free staging, use the included Wrangler configuration and configure the same Supabase and provider bindings in the Worker, using secrets for server and API keys. The Worker always uses the real adapter. Run the smoke requests over its HTTPS staging URL with a staging token.

When verification is unavailable, the API returns `503 VERIFICATION_UNAVAILABLE`. A valid PRO analysis with an unavailable provider returns `502 AI_UNAVAILABLE` so Android can offer offline analysis. An exhausted account gets `429 QUOTA_EXHAUSTED` with `Retry-After` in seconds. The API returns JSON error envelopes and sets `Cache-Control: no-store`.
