# Graph Report - Project-B  (2026-11-25)

## Corpus Check
- Corpus is ~34,424 words - fits in a single context window. You may not need a graph.

## Summary
- 1050 nodes · 2747 edges · 56 communities (26 shown, 30 thin omitted)
- Extraction: 95% EXTRACTED · 5% INFERRED · 0% AMBIGUOUS · INFERRED: 141 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- AndroidIntegration
- TazerViewModel
- TaskRepository
- handler.ts
- SchedulerTest
- TaskRepositoryTest
- TazerDao
- Spesifikasi scheduler dan timer
- Supabase Auth
- FocusNotifications
- FocusRuntime
- TazerViewModel.kt
- Aturan global
- FocusEvent
- IntegrationError
- FocusService.kt
- MainActivity.kt
- AndroidIntegrationTest.kt
- package.json
- FocusAlarms
- Task
- OfflineAnalysis
- compilerOptions
- Smart Shift
- AuthFlow
- Signed APK Release
- FocusReceiver.kt
- 20261125000000_entitlement.sql
- Focus timer state machine
- TazerDatabase
- AuthPkce.kt
- Rencana eksekusi Tazer
- Cloudflare Workers Free
- WorkSessionState
- SessionVault
- FocusEventKind
- Atomic quota RPC
- HTTP error envelope
- .onStartAlarm()
- FocusLinkKind
- .create()

## God Nodes (most connected - your core abstractions)
1. `TazerViewModel` - 64 edges
2. `TaskRepository` - 56 edges
3. `WorkSession` - 48 edges
4. `FocusRuntime` - 45 edges
5. `AndroidIntegration` - 41 edges
6. `fail()` - 38 edges
7. `FocusMoment` - 36 edges
8. `TaskRepositoryTest` - 35 edges
9. `TazerDao` - 32 edges
10. `SchedulerTest` - 28 edges

## Surprising Connections (you probably didn't know these)
- `Slot kosong dan batas sesi` --references--> `FixedSchedule`  [INFERRED]
  docs/SCHEDULER_SPEC.md → android/app/src/main/java/com/tazer/taskorganizer/data/Entities.kt
- `Room` --references--> `WorkSession`  [INFERRED]
  docs/DATA_AND_API_CONTRACT.md → android/app/src/main/java/com/tazer/taskorganizer/data/Entities.kt
- `Room` --references--> `FixedSchedule`  [INFERRED]
  docs/DATA_AND_API_CONTRACT.md → android/app/src/main/java/com/tazer/taskorganizer/data/Entities.kt
- `Room` --references--> `Task`  [INFERRED]
  docs/DATA_AND_API_CONTRACT.md → android/app/src/main/java/com/tazer/taskorganizer/data/Entities.kt
- `Room` --references--> `UserProfile`  [INFERRED]
  docs/DATA_AND_API_CONTRACT.md → android/app/src/main/java/com/tazer/taskorganizer/data/Entities.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Transactional work history and replanning** — agents_room_repository, docs_data_and_api_contract_worksession, docs_data_and_api_contract_task, docs_scheduler_spec_smart_shift, docs_scheduler_spec_dynamic_reschedule, docs_scheduler_spec_weighted_time_allocation [EXTRACTED 1.00]

## Communities (56 total, 30 thin omitted)

### Community 0 - "AndroidIntegration"
Cohesion: 0.06
Nodes (40): AndroidIntegration, Session, Tokens, analyzeGemini(), analyzeGroq(), analyzeWithProvider(), integerSchema(), schema() (+32 more)

### Community 1 - "TazerViewModel"
Cohesion: 0.06
Nodes (30): AccountSettings(), ByokForm(), LoginScreen(), SecretField(), DeferFocusDialog(), ExtensionDialog(), FocusPermissionsDialog(), FocusScreen() (+22 more)

### Community 2 - "TaskRepository"
Cohesion: 0.08
Nodes (13): FixedScheduleKind, BREAK, ROUTINE, WorkSession, FocusChange, TaskRepository, continuousWorkMs(), FocusClock (+5 more)

### Community 3 - "handler.ts"
Cohesion: 0.07
Nodes (46): Analysis, analysisOutput(), analysisSchema, integerBetween(), isUtcTimestamp(), object(), TaskInput, taskInput() (+38 more)

### Community 4 - "SchedulerTest"
Cohesion: 0.14
Nodes (9): DeadlineRisk, PlannedWork, SchedulePlan, Scheduler, Option, Work, SchedulingTask, TimeBlock (+1 more)

### Community 9 - "Spesifikasi scheduler dan timer"
Cohesion: 0.11
Nodes (25): Android :app, Android data layer, Android timer layer, APK Debug, com.tazer.taskorganizer, Compose UI and ViewModel, Kotlin domain, Room repository (+17 more)

### Community 10 - "Supabase Auth"
Cohesion: 0.14
Nodes (22): Backend, Server secret store, Analyzer input validation, Entitlement akun dan kuota bulanan, Gemini API Free, GET /auth/session, GET /subscription, Groq Free (+14 more)

### Community 13 - "TazerViewModel.kt"
Cohesion: 0.12
Nodes (9): AnalysisSource, AI, OFFLINE, AnalysisSuggestion, AnalyzerMode, BYOK, CLOUD, OFFLINE (+1 more)

### Community 14 - "Aturan global"
Cohesion: 0.15
Nodes (13): Aturan global, Batas modul dan struktur target, Graphify, Handoff `i-have-adhd`, i-have-adhd handoff, Pemeriksaan sebelum handoff, Ponytail, Tazer (Task Organizer) (+5 more)

### Community 16 - "IntegrationError"
Cohesion: 0.12
Nodes (17): IntegrationError, AI_UNAVAILABLE, AUTH_REJECTED, BYOK_KEY_MISSING, BYOK_REQUIRED, BYOK_STORAGE_UNAVAILABLE, EMAIL_UNVERIFIED, INVALID_AUTH (+9 more)

### Community 20 - "package.json"
Cohesion: 0.14
Nodes (15): devDependencies, @types/node@^22.0.0, typescript@~5.9.3, engines, node, name, private, scripts (+7 more)

### Community 23 - "Task"
Cohesion: 0.29
Nodes (9): FixedSchedule, Task, TaskStatus, DONE, IN_PROGRESS, PENDING, UserProfile, Workspace (+1 more)

### Community 24 - "OfflineAnalysis"
Cohesion: 0.37
Nodes (3): OfflineAnalysis, OfflineAnalyzer, OfflineAnalyzerTest

### Community 26 - "compilerOptions"
Cohesion: 0.14
Nodes (13): compilerOptions, lib, module, noUncheckedIndexedAccess, noUnusedLocals, noUnusedParameters, outDir, rootDir (+5 more)

### Community 27 - "Smart Shift"
Cohesion: 0.34
Nodes (14): FixedSchedule, Room schema, Task, UserProfile, WorkSession, WorkSession.state, boundedShares, Callback revision check (+6 more)

### Community 28 - "AuthFlow"
Cohesion: 0.27
Nodes (6): AuthFlow, GOOGLE, RECOVERY, SIGN_UP, AuthPkce, PendingAuth

### Community 30 - "Signed APK Release"
Cohesion: 0.18
Nodes (12): GitHub Releases gratis, Signed APK Release, AlarmManager.setExactAndAllowWhileIdle, Doze, apksigner verify, GitHub Releases, Keystore Tazer, GPT-6.2 Astra escalation (+4 more)

### Community 32 - "20261125000000_entitlement.sql"
Cohesion: 0.25
Nodes (7): Android client, Supabase and providers, Tazer backend, public.account_entitlements, public.analysis_usage, public.tazer_entitlement(), tazer-db

### Community 33 - "Focus timer state machine"
Cohesion: 0.36
Nodes (11): Belum Selesai and Selesai tabs, OVERDUE display status, Task.status, CANCELLED session, DONE task, EXTENDED session, Focus timer state machine, OVERTIME (+3 more)

### Community 37 - "Rencana eksekusi Tazer"
Cohesion: 0.22
Nodes (9): Android Keystore AES-GCM, BYOK, Awalan system prompt untuk semua peran, Eskalasi ke GPT-6.2 Astra, Rencana eksekusi Tazer, Template handoff, Urutan handoff Wave 1, Wave 0 Blueprint (+1 more)

### Community 38 - "Cloudflare Workers Free"
Cohesion: 0.33
Nodes (7): APK sideload, Node.js + TypeScript, Rp 0 free tier, Cloudflare Workers Free, Web Fetch API, Worker secrets, Wrangler

### Community 39 - "WorkSessionState"
Cohesion: 0.29
Nodes (7): WorkSessionState, CANCELLED, DONE, EXTENDED, OVERTIME, PENDING, RUNNING

### Community 41 - "FocusEventKind"
Cohesion: 0.29
Nodes (7): FocusEventKind, ACTIVE, CUTOFF, DONE, EXTEND, REMINDER, START_ALARM

### Community 42 - "Atomic quota RPC"
Cohesion: 0.38
Nodes (7): account_entitlements, analysis_usage, Atomic quota RPC, Kuota bulanan UTC, PostgreSQL, PRO tier, SubscriptionTier

### Community 43 - "HTTP error envelope"
Cohesion: 0.29
Nodes (7): AI_UNAVAILABLE, EMAIL_UNVERIFIED, HTTP error envelope, INVALID_AUTH, PRO_REQUIRED, QUOTA_EXHAUSTED, VERIFICATION_UNAVAILABLE

### Community 46 - "FocusLinkKind"
Cohesion: 0.50
Nodes (4): FocusLinkKind, ACTIVE, EXTEND, REMINDER

## Knowledge Gaps
- **76 isolated node(s):** `PasswordRecovery`, `GEMINI`, `GROQ`, `BYOK`, `FREE` (+71 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 233 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **30 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `FixedSchedule` connect `Task` to `TazerViewModel`, `TaskRepository`, `TazerDatabase`, `TazerDao.kt`, `TaskRepositoryTest`, `TaskScreens.kt`, `TazerDao`, `Spesifikasi scheduler dan timer`, `TazerViewModel.kt`?**
  _High betweenness centrality (0.160) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `FocusRuntime` (e.g. with `FocusAlarms` and `FocusNotifications`) actually correct?**
  _`FocusRuntime` has 2 INFERRED edges - model-reasoned connections that need verification._
- **What connects `PasswordRecovery`, `GEMINI`, `GROQ` to the rest of the system?**
  _76 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `AndroidIntegration` be split into smaller, more focused modules?**
  _Cohesion score 0.05691985532076908 - nodes in this community are weakly interconnected._
- **Why does `Spesifikasi scheduler dan timer` connect `Spesifikasi scheduler dan timer` to `Focus timer state machine`, `Smart Shift`, `Aturan global`, `Signed APK Release`?**
  _High betweenness centrality (0.132) - this node is a cross-community bridge._
- **Are the 4 inferred relationships involving `AndroidIntegration` (e.g. with `.configuredLoopbackEnforcesTiersAndChargesOneProRequest()` and `AuthPkce`) actually correct?**
  _`AndroidIntegration` has 4 INFERRED edges - model-reasoned connections that need verification._
- **Should `TazerViewModel` be split into smaller, more focused modules?**
  _Cohesion score 0.05789009697325889 - nodes in this community are weakly interconnected._
> Penggunaan token ekstraksi checklist tidak tersedia dari runtime. Angka nol adalah nilai default.
