# Graph Report - Project-B  (2026-11-26)

## Corpus Check
- 66 files · ~41,064 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 15 file(s) not represented in the graph (top: .xml 8, .properties 2, (none) 1)

## Summary
- 1215 nodes · 3135 edges · 92 communities (34 shown, 58 thin omitted)
- Extraction: 95% EXTRACTED · 5% INFERRED · 0% AMBIGUOUS · INFERRED: 167 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `2f0c13b8`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- AndroidIntegration
- TazerViewModel
- TaskRepository
- handler.ts
- SchedulerTest
- TaskRepositoryTest
- TaskScreens.kt
- TazerDao
- TazerApp.kt
- Spesifikasi scheduler dan timer
- Supabase Auth
- Notification
- FocusRuntime
- Job
- ByokVault.kt
- IntegrationError
- WorkspaceScreen
- WorkspaceScreen
- AndroidIntegrationTest.kt
- FocusNotificationTest.kt
- SettingsScreen
- TazerState
- Uri
- FocusNotifications
- compilerOptions
- Smart Shift
- AuthFlow
- ScheduleScreens.kt
- CalendarScreens.kt
- FocusReceiver.kt
- 20261125000000_entitlement.sql
- Focus timer state machine
- Autentikasi dan endpoint HTTP
- Rencana eksekusi Tazer
- FocusStartTest.kt
- Rencana eksekusi Tazer
- fail
- IntegrationHttp.kt
- SessionVault
- HTTP error envelope
- MainActivity.kt
- HTTP error envelope
- instant
- tazer-v1.sql
- Subscription
- .readLink()
- AuthPkce.kt
- FocusNotificationTest.kt

## God Nodes (most connected - your core abstractions)
1. `TazerViewModel` - 69 edges
2. `TaskRepository` - 60 edges
3. `WorkSession` - 53 edges
4. `FocusRuntime` - 48 edges
5. `FocusMoment` - 43 edges
6. `AndroidIntegration` - 41 edges
7. `fail()` - 38 edges
8. `TaskRepositoryTest` - 35 edges
9. `TazerDao` - 32 edges
10. `Task` - 29 edges

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
- **Agenda pada kalender lokal** — docs_data_and_api_contract_calendarevent, docs_data_and_api_contract_worksession, docs_data_and_api_contract_fixedschedule, docs_data_and_api_contract_task [EXTRACTED 1.00]
- **Transactional work history and replanning** — agents_room_repository, docs_data_and_api_contract_worksession, docs_data_and_api_contract_task, docs_scheduler_spec_smart_shift, docs_scheduler_spec_dynamic_reschedule, docs_scheduler_spec_weighted_time_allocation [EXTRACTED 1.00]

## Communities (92 total, 58 thin omitted)

### Community 0 - "AndroidIntegration"
Cohesion: 0.10
Nodes (8): FixedScheduleKind, BREAK, ROUTINE, WorkSession, FocusChange, TaskRepository, FocusClock, FocusMoment

### Community 1 - "TazerViewModel"
Cohesion: 0.06
Nodes (16): CalendarDao, AnalysisSource, AI, OFFLINE, CalendarEvent, FixedSchedule, Task, TaskStatus (+8 more)

### Community 2 - "TaskRepository"
Cohesion: 0.06
Nodes (51): Analysis, analysisOutput(), analysisSchema, integerBetween(), isUtcTimestamp(), object(), TaskInput, taskInput() (+43 more)

### Community 3 - "handler.ts"
Cohesion: 0.06
Nodes (17): ByokProvider, GEMINI, GROQ, SubscriptionTier, BYOK, FREE, PRO, TaskEditor() (+9 more)

### Community 4 - "SchedulerTest"
Cohesion: 0.10
Nodes (14): continuousWorkMs(), focusDurationMs(), remainingAfterInterruption(), DeadlineRisk, PlannedWork, SchedulePlan, Scheduler, Option (+6 more)

### Community 8 - "TazerApp.kt"
Cohesion: 0.19
Nodes (3): AndroidIntegration, Session, Account

### Community 9 - "Spesifikasi scheduler dan timer"
Cohesion: 0.12
Nodes (13): dayName(), FixedEditor(), ProfileForm(), ScheduleTag(), SettingsScreen(), TimeField(), TimelineEntry(), TimelineRow (+5 more)

### Community 10 - "Supabase Auth"
Cohesion: 0.11
Nodes (7): AgendaDot(), AgendaItem, CalendarScreen(), CalendarWeek(), EventDate(), EventEditor(), MonthPicker()

### Community 11 - "Notification"
Cohesion: 0.10
Nodes (27): Android :app, Android data layer, Android timer layer, APK Debug, com.tazer.taskorganizer, Compose UI and ViewModel, Impeccable, Kotlin domain (+19 more)

### Community 16 - "IntegrationError"
Cohesion: 0.19
Nodes (6): FocusEvent, FocusLink, FocusLinkKind, ACTIVE, EXTEND, REMINDER

### Community 17 - "WorkspaceScreen"
Cohesion: 0.14
Nodes (22): Backend, Server secret store, Analyzer input validation, Entitlement akun dan kuota bulanan, Gemini API Free, GET /auth/session, GET /subscription, Groq Free (+14 more)

### Community 18 - "WorkspaceScreen"
Cohesion: 0.22
Nodes (20): CalendarEvent, CalendarEvent(ownerUid,startEpochDay), CalendarEvent.startEpochDay dan endEpochDay, CalendarEvent.startMinute, FixedSchedule, Kalender lokal, Migrasi Room v1 ke v2, Room schema (+12 more)

### Community 19 - "AndroidIntegrationTest.kt"
Cohesion: 0.14
Nodes (15): Aturan global, Batas modul dan struktur target, Graphify, Handoff `i-have-adhd`, i-have-adhd handoff, Pemeriksaan sebelum handoff, Ponytail, Tazer (Task Organizer) (+7 more)

### Community 21 - "SettingsScreen"
Cohesion: 0.18
Nodes (14): DeferFocusDialog(), ExtensionDialog(), focusClock(), FocusPermissionsDialog(), FocusScreen(), isOvertime(), formatDate(), schedulingTask() (+6 more)

### Community 22 - "TazerState"
Cohesion: 0.12
Nodes (17): IntegrationError, AI_UNAVAILABLE, AUTH_REJECTED, BYOK_KEY_MISSING, BYOK_REQUIRED, BYOK_STORAGE_UNAVAILABLE, EMAIL_UNVERIFIED, INVALID_AUTH (+9 more)

### Community 26 - "compilerOptions"
Cohesion: 0.14
Nodes (15): devDependencies, @types/node@^22.0.0, typescript@~5.9.3, engines, node, name, private, scripts (+7 more)

### Community 27 - "Smart Shift"
Cohesion: 0.14
Nodes (16): account_entitlements, analysis_usage, Android Keystore AES-GCM, Atomic quota RPC, BYOK, Kuota bulanan UTC, PostgreSQL, PRO tier (+8 more)

### Community 30 - "CalendarScreens.kt"
Cohesion: 0.20
Nodes (13): Tokens, analysis(), AuthCallback, PasswordRecovery, SignedIn, fail(), integer(), IntegrationException (+5 more)

### Community 31 - "FocusReceiver.kt"
Cohesion: 0.31
Nodes (3): ByokVault, CredentialCipher, AccountUid

### Community 32 - "20261125000000_entitlement.sql"
Cohesion: 0.29
Nodes (15): Belum Selesai and Selesai tabs, OVERDUE display status, Task.status, CANCELLED session, DONE task, Durasi fokus penuh, EXTENDED session, Focus timer state machine (+7 more)

### Community 33 - "Focus timer state machine"
Cohesion: 0.33
Nodes (3): OfflineAnalysis, OfflineAnalyzer, OfflineAnalyzerTest

### Community 34 - "Autentikasi dan endpoint HTTP"
Cohesion: 0.14
Nodes (13): compilerOptions, lib, module, noUncheckedIndexedAccess, noUnusedLocals, noUnusedParameters, outDir, rootDir (+5 more)

### Community 35 - "Rencana eksekusi Tazer"
Cohesion: 0.18
Nodes (13): APK sideload, GitHub Releases gratis, Node.js + TypeScript, Rp 0 free tier, Signed APK Release, Cloudflare Workers Free, Web Fetch API, apksigner verify (+5 more)

### Community 36 - "FocusStartTest.kt"
Cohesion: 0.32
Nodes (9): analyzeGemini(), analyzeGroq(), analyzeWithProvider(), integerSchema(), schema(), validateByok(), ByokCredential, ByokSettings (+1 more)

### Community 37 - "Rencana eksekusi Tazer"
Cohesion: 0.29
Nodes (11): `FixedSchedule`, IF, index_FixedSchedule_ownerUid_dayOfWeek, index_Task_id_ownerUid, index_Task_ownerUid_status_completedAt, index_WorkSession_ownerUid_state_plannedStartAt, index_WorkSession_taskId_ownerUid, room_master_table (+3 more)

### Community 38 - "fail"
Cohesion: 0.27
Nodes (4): backendJson(), call(), HttpResponse, parseJson()

### Community 40 - "SessionVault"
Cohesion: 0.18
Nodes (11): TazerGlyph, Add, Calendar, Down, Edit, Next, Previous, Settings (+3 more)

### Community 41 - "HTTP error envelope"
Cohesion: 0.25
Nodes (7): Android client, Supabase and providers, Tazer backend, public.account_entitlements, public.analysis_usage, public.tazer_entitlement(), tazer-db

### Community 43 - "HTTP error envelope"
Cohesion: 0.22
Nodes (9): Analyzer, Autentikasi dan endpoint HTTP, Envelope error, GEMINI_MODEL, GET /health, Health readiness (ready), Kontrak data dan API, SUPABASE_PUBLISHABLE_KEY (+1 more)

### Community 44 - "instant"
Cohesion: 0.29
Nodes (7): WorkSessionState, CANCELLED, DONE, EXTENDED, OVERTIME, PENDING, RUNNING

### Community 45 - "tazer-v1.sql"
Cohesion: 0.29
Nodes (7): FocusEventKind, ACTIVE, CUTOFF, DONE, EXTEND, REMINDER, START_ALARM

### Community 46 - "Subscription"
Cohesion: 0.54
Nodes (5): AccountSettings(), ByokForm(), LoginScreen(), SecretField(), TazerState

### Community 47 - ".readLink()"
Cohesion: 0.29
Nodes (7): AI_UNAVAILABLE, EMAIL_UNVERIFIED, HTTP error envelope, INVALID_AUTH, PRO_REQUIRED, QUOTA_EXHAUSTED, VERIFICATION_UNAVAILABLE

### Community 51 - "AuthPkce.kt"
Cohesion: 0.19
Nodes (7): AuthFlow, GOOGLE, RECOVERY, SIGN_UP, AuthPkce, isAuthCallback(), PendingAuth

## Knowledge Gaps
- **92 isolated node(s):** `PasswordRecovery`, `AI`, `OFFLINE`, `BREAK`, `ROUTINE` (+87 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 300 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **58 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `FixedSchedule` connect `TazerViewModel` to `AndroidIntegration`, `handler.ts`, `TazerDao`, `Spesifikasi scheduler dan timer`, `Notification`?**
  _High betweenness centrality (0.142) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `WorkSession` (e.g. with `.migratesExportedV1AndPreservesExistingRows()` and `Room`) actually correct?**
  _`WorkSession` has 2 INFERRED edges - model-reasoned connections that need verification._
- **What connects `PasswordRecovery`, `AI`, `OFFLINE` to the rest of the system?**
  _92 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `AndroidIntegration` be split into smaller, more focused modules?**
  _Cohesion score 0.10382513661202186 - nodes in this community are weakly interconnected._
- **Why does `Spesifikasi scheduler dan timer` connect `Notification` to `20261125000000_entitlement.sql`, `WorkspaceScreen`, `AndroidIntegrationTest.kt`?**
  _High betweenness centrality (0.132) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `FocusRuntime` (e.g. with `FocusAlarms` and `FocusNotifications`) actually correct?**
  _`FocusRuntime` has 2 INFERRED edges - model-reasoned connections that need verification._
- **Should `TazerViewModel` be split into smaller, more focused modules?**
  _Cohesion score 0.057297297297297295 - nodes in this community are weakly interconnected._