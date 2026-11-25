# Graph Report - Project-B  (2026-11-26)

## Corpus Check
- 58 files · ~34,224 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 1066 nodes · 2776 edges · 52 communities (29 shown, 23 thin omitted)
- Extraction: 95% EXTRACTED · 5% INFERRED · 0% AMBIGUOUS · INFERRED: 141 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `02dc697b`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- AndroidIntegration
- TaskRepository
- handler.ts
- TazerDao
- TazerViewModel
- SchedulerTest
- TaskRepositoryTest
- FocusRuntime
- Spesifikasi scheduler dan timer
- FocusNotifications
- Supabase Auth
- FocusService.kt
- Aturan global
- FocusScreens.kt
- FocusEvent
- IntegrationError
- MainActivity.kt
- FocusAlarms
- package.json
- AndroidIntegrationTest.kt
- AuthFlow
- Signed APK Release
- OfflineAnalysis
- compilerOptions
- Smart Shift
- SettingsScreen
- WorkspaceScreen
- 20261125000000_entitlement.sql
- Focus timer state machine
- FocusReceiver.kt
- Autentikasi dan endpoint HTTP
- Rencana eksekusi Tazer
- TazerState
- WorkSessionState
- SessionVault
- FocusEventKind
- Atomic quota RPC
- HTTP error envelope
- FocusLinkKind
- .create

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

## Communities (52 total, 23 thin omitted)

### Community 0 - "AndroidIntegration"
Cohesion: 0.06
Nodes (33): AndroidIntegration, Session, Tokens, analyzeGemini(), analyzeGroq(), analyzeWithProvider(), integerSchema(), schema() (+25 more)

### Community 1 - "TaskRepository"
Cohesion: 0.08
Nodes (10): WorkSession, FocusChange, TaskRepository, continuousWorkMs(), FocusClock, focusDurationMs(), FocusMoment, remainingAfterInterruption() (+2 more)

### Community 2 - "handler.ts"
Cohesion: 0.06
Nodes (51): Analysis, analysisOutput(), analysisSchema, integerBetween(), isUtcTimestamp(), object(), TaskInput, taskInput() (+43 more)

### Community 3 - "TazerDao"
Cohesion: 0.06
Nodes (17): AnalysisSource, AI, OFFLINE, FixedSchedule, FixedScheduleKind, BREAK, ROUTINE, Task (+9 more)

### Community 4 - "TazerViewModel"
Cohesion: 0.06
Nodes (16): ByokProvider, GEMINI, GROQ, SubscriptionTier, BYOK, FREE, PRO, AnalysisSuggestion (+8 more)

### Community 5 - "SchedulerTest"
Cohesion: 0.14
Nodes (9): DeadlineRisk, PlannedWork, SchedulePlan, Scheduler, Option, Work, SchedulingTask, TimeBlock (+1 more)

### Community 10 - "Spesifikasi scheduler dan timer"
Cohesion: 0.11
Nodes (25): Android :app, Android data layer, Android timer layer, APK Debug, com.tazer.taskorganizer, Compose UI and ViewModel, Kotlin domain, Room repository (+17 more)

### Community 12 - "Supabase Auth"
Cohesion: 0.14
Nodes (22): Backend, Server secret store, Analyzer input validation, Entitlement akun dan kuota bulanan, Gemini API Free, GET /auth/session, GET /subscription, Groq Free (+14 more)

### Community 16 - "Aturan global"
Cohesion: 0.15
Nodes (14): Aturan global, Batas modul dan struktur target, Graphify, Handoff `i-have-adhd`, i-have-adhd handoff, Pemeriksaan sebelum handoff, Ponytail, Tazer (Task Organizer) (+6 more)

### Community 17 - "FocusScreens.kt"
Cohesion: 0.18
Nodes (5): DeferFocusDialog(), ExtensionDialog(), FocusPermissionsDialog(), FocusScreen(), formatTimer()

### Community 19 - "IntegrationError"
Cohesion: 0.12
Nodes (17): IntegrationError, AI_UNAVAILABLE, AUTH_REJECTED, BYOK_KEY_MISSING, BYOK_REQUIRED, BYOK_STORAGE_UNAVAILABLE, EMAIL_UNVERIFIED, INVALID_AUTH (+9 more)

### Community 22 - "package.json"
Cohesion: 0.14
Nodes (15): devDependencies, @types/node@^22.0.0, typescript@~5.9.3, engines, node, name, private, scripts (+7 more)

### Community 24 - "AuthFlow"
Cohesion: 0.23
Nodes (7): AuthFlow, GOOGLE, RECOVERY, SIGN_UP, AuthPkce, isAuthCallback(), PendingAuth

### Community 25 - "Signed APK Release"
Cohesion: 0.16
Nodes (14): APK sideload, GitHub Releases gratis, Node.js + TypeScript, Rp 0 free tier, Signed APK Release, Cloudflare Workers Free, Web Fetch API, apksigner verify (+6 more)

### Community 26 - "OfflineAnalysis"
Cohesion: 0.37
Nodes (3): OfflineAnalysis, OfflineAnalyzer, OfflineAnalyzerTest

### Community 27 - "compilerOptions"
Cohesion: 0.14
Nodes (13): compilerOptions, lib, module, noUncheckedIndexedAccess, noUnusedLocals, noUnusedParameters, outDir, rootDir (+5 more)

### Community 28 - "Smart Shift"
Cohesion: 0.34
Nodes (14): FixedSchedule, Room schema, Task, UserProfile, WorkSession, WorkSession.state, boundedShares, Callback revision check (+6 more)

### Community 29 - "SettingsScreen"
Cohesion: 0.28
Nodes (11): dayName(), FixedEditor(), ProfileForm(), SettingsScreen(), TimeField(), TimelineRow, TimelineScreen(), formatMinute() (+3 more)

### Community 30 - "WorkspaceScreen"
Cohesion: 0.24
Nodes (7): formatDate(), schedulingTask(), TaskEditor(), TasksScreen(), SectionIcon(), TazerApp(), WorkspaceScreen()

### Community 31 - "20261125000000_entitlement.sql"
Cohesion: 0.25
Nodes (7): Android client, Supabase and providers, Tazer backend, public.account_entitlements, public.analysis_usage, public.tazer_entitlement(), tazer-db

### Community 32 - "Focus timer state machine"
Cohesion: 0.36
Nodes (11): Belum Selesai and Selesai tabs, OVERDUE display status, Task.status, CANCELLED session, DONE task, EXTENDED session, Focus timer state machine, OVERTIME (+3 more)

### Community 34 - "Autentikasi dan endpoint HTTP"
Cohesion: 0.22
Nodes (9): Analyzer, Autentikasi dan endpoint HTTP, Envelope error, GEMINI_MODEL, GET /health, Health readiness (ready), Kontrak data dan API, SUPABASE_PUBLISHABLE_KEY (+1 more)

### Community 35 - "Rencana eksekusi Tazer"
Cohesion: 0.22
Nodes (9): Android Keystore AES-GCM, BYOK, Awalan system prompt untuk semua peran, Eskalasi ke GPT-6.2 Astra, Rencana eksekusi Tazer, Template handoff, Urutan handoff Wave 1, Wave 0 Blueprint (+1 more)

### Community 36 - "TazerState"
Cohesion: 0.54
Nodes (5): AccountSettings(), ByokForm(), LoginScreen(), SecretField(), TazerState

### Community 37 - "WorkSessionState"
Cohesion: 0.29
Nodes (7): WorkSessionState, CANCELLED, DONE, EXTENDED, OVERTIME, PENDING, RUNNING

### Community 39 - "FocusEventKind"
Cohesion: 0.29
Nodes (7): FocusEventKind, ACTIVE, CUTOFF, DONE, EXTEND, REMINDER, START_ALARM

### Community 40 - "Atomic quota RPC"
Cohesion: 0.38
Nodes (7): account_entitlements, analysis_usage, Atomic quota RPC, Kuota bulanan UTC, PostgreSQL, PRO tier, SubscriptionTier

### Community 41 - "HTTP error envelope"
Cohesion: 0.29
Nodes (7): AI_UNAVAILABLE, EMAIL_UNVERIFIED, HTTP error envelope, INVALID_AUTH, PRO_REQUIRED, QUOTA_EXHAUSTED, VERIFICATION_UNAVAILABLE

### Community 42 - "FocusLinkKind"
Cohesion: 0.50
Nodes (4): FocusLinkKind, ACTIVE, EXTEND, REMINDER

## Knowledge Gaps
- **79 isolated node(s):** `PasswordRecovery`, `GEMINI`, `GROQ`, `BYOK`, `FREE` (+74 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 240 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **23 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `FixedSchedule` connect `TazerDao` to `TaskRepository`, `TazerViewModel`, `TaskRepositoryTest`, `TaskScreens.kt`, `Spesifikasi scheduler dan timer`, `SettingsScreen`?**
  _High betweenness centrality (0.136) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `FocusRuntime` (e.g. with `FocusAlarms` and `FocusNotifications`) actually correct?**
  _`FocusRuntime` has 2 INFERRED edges - model-reasoned connections that need verification._
- **What connects `PasswordRecovery`, `GEMINI`, `GROQ` to the rest of the system?**
  _79 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `AndroidIntegration` be split into smaller, more focused modules?**
  _Cohesion score 0.06405375139977604 - nodes in this community are weakly interconnected._
- **Why does `WorkSession` connect `TaskRepository` to `TazerDao`, `TazerViewModel`, `WorkSessionState`, `TaskRepositoryTest`, `TaskScreens.kt`, `FocusRuntime`, `FocusNotifications`, `FocusRuntime.kt`, `FocusScreens.kt`, `FocusEvent`, `SettingsScreen`?**
  _High betweenness centrality (0.118) - this node is a cross-community bridge._
- **Are the 4 inferred relationships involving `AndroidIntegration` (e.g. with `.configuredLoopbackEnforcesTiersAndChargesOneProRequest()` and `AuthPkce`) actually correct?**
  _`AndroidIntegration` has 4 INFERRED edges - model-reasoned connections that need verification._
- **Should `TaskRepository` be split into smaller, more focused modules?**
  _Cohesion score 0.07911392405063292 - nodes in this community are weakly interconnected._