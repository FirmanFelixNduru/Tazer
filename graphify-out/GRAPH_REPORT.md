# Graph Report - Project-B  (2026-11-26)

## Corpus Check
- 71 files · ~46,256 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 15 file(s) not represented in the graph (top: .xml 8, .properties 2, (none) 1)

## Summary
- 1337 nodes · 3515 edges · 99 communities (35 shown, 64 thin omitted)
- Extraction: 95% EXTRACTED · 5% INFERRED · 0% AMBIGUOUS · INFERRED: 190 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `fbef7b5f`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- AndroidIntegration
- TazerDao
- TaskRepository
- SchedulerTest
- SchedulerTest
- FocusScreens.kt
- TaskScreens.kt
- TazerDao
- TaskScreens.kt
- Spesifikasi scheduler dan timer
- Supabase Auth
- Notification
- FocusRuntime
- ByokVault.kt
- FocusEvent
- FocusScreen
- WorkspaceScreen
- WorkspaceScreen
- FocusNotificationTest.kt
- TaskScreens.kt
- SettingsScreen
- TazerState
- MainActivity.kt
- FocusNotifications
- DateTimeButtons.kt
- compilerOptions
- fail
- FocusStartTest.kt
- ScheduleScreens.kt
- TazerGlyph
- FocusReceiver.kt
- MainActivity
- TazerState
- Autentikasi dan endpoint HTTP
- Rencana eksekusi Tazer
- Task
- Rencana eksekusi Tazer
- fail
- SessionVault
- HTTP error envelope
- HTTP error envelope
- tazer-v1.sql
- Subscription
- Intent
- CalendarRepository
- build.gradle.kts
- AuthPkce.kt
- DateTimeButtons.kt
- CalendarDao

## God Nodes (most connected - your core abstractions)
1. `TazerViewModel` - 71 edges
2. `TaskRepository` - 67 edges
3. `WorkSession` - 58 edges
4. `FocusMoment` - 57 edges
5. `FocusRuntime` - 50 edges
6. `TaskRepositoryTest` - 41 edges
7. `AndroidIntegration` - 41 edges
8. `fail()` - 38 edges
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
- **Agenda pada kalender lokal** — docs_data_and_api_contract_calendarevent, docs_data_and_api_contract_worksession, docs_data_and_api_contract_fixedschedule, docs_data_and_api_contract_task [EXTRACTED 1.00]
- **Persistensi status, progres, dan sisa waktu saat jeda** — docs_scheduler_spec_paused, docs_scheduler_spec_jeda_dan_lanjutkan, docs_data_and_api_contract_worksession_pausedremainingms, docs_data_and_api_contract_worksession_pausedfromstate, docs_data_and_api_contract_worksession_workbeforesegmentms, docs_data_and_api_contract_worksession_continuousworkbeforesegmentms [INFERRED 0.95]
- **Transactional work history and replanning** — agents_room_repository, docs_data_and_api_contract_worksession, docs_data_and_api_contract_task, docs_scheduler_spec_smart_shift, docs_scheduler_spec_dynamic_reschedule, docs_scheduler_spec_weighted_time_allocation [EXTRACTED 1.00]

## Communities (99 total, 64 thin omitted)

### Community 0 - "AndroidIntegration"
Cohesion: 0.07
Nodes (21): AnalysisSource, AI, OFFLINE, CalendarEvent, FixedSchedule, FixedScheduleKind, BREAK, ROUTINE (+13 more)

### Community 1 - "TazerDao"
Cohesion: 0.06
Nodes (51): Analysis, analysisOutput(), analysisSchema, integerBetween(), isUtcTimestamp(), object(), TaskInput, taskInput() (+43 more)

### Community 2 - "TaskRepository"
Cohesion: 0.08
Nodes (9): TaskEditor(), AnalyzerMode, BYOK, CLOUD, OFFLINE, FocusTarget, rejectsSession(), TaskInput (+1 more)

### Community 3 - "SchedulerTest"
Cohesion: 0.08
Nodes (16): continuousWorkMs(), focusAvailableMs(), FocusClock, focusDurationMs(), remainingAfterInterruption(), DeadlineRisk, PlannedWork, SchedulePlan (+8 more)

### Community 5 - "FocusScreens.kt"
Cohesion: 0.12
Nodes (36): CalendarEvent, CalendarEvent(ownerUid,startEpochDay), CalendarEvent.startEpochDay dan endEpochDay, CalendarEvent.startMinute, FixedSchedule, Kalender lokal, Migrasi Room v1 ke v2, Migrasi Room v2 ke v3 (+28 more)

### Community 6 - "TaskScreens.kt"
Cohesion: 0.19
Nodes (3): AndroidIntegration, Session, Account

### Community 7 - "TazerDao"
Cohesion: 0.09
Nodes (33): Backend, Server secret store, account_entitlements, analysis_usage, Analyzer input validation, Android Keystore AES-GCM, Atomic quota RPC, BYOK (+25 more)

### Community 8 - "TaskScreens.kt"
Cohesion: 0.08
Nodes (5): formatTimer(), timerText(), isDark(), ThemeSetting(), ThemeSystemBars()

### Community 9 - "Spesifikasi scheduler dan timer"
Cohesion: 0.11
Nodes (12): dayName(), FixedEditor(), ProfileForm(), ScheduleTag(), SettingsScreen(), TimeField(), TimelineEntry(), TimelineRow (+4 more)

### Community 11 - "Notification"
Cohesion: 0.10
Nodes (9): AgendaItem, AgendaPill(), CalendarScreen(), CalendarWeek(), EventDate(), EventEditor(), MonthPicker(), formatTime() (+1 more)

### Community 13 - "FocusRuntime"
Cohesion: 0.19
Nodes (6): ReleaseUiTest, TazerThemeMode, DARK, LIGHT, SYSTEM, ThemePreference

### Community 16 - "FocusScreen"
Cohesion: 0.15
Nodes (14): DeferFocusDialog(), ExtensionDialog(), focusClock(), FocusPermissionsDialog(), FocusScreen(), isOvertime(), formatDate(), schedulingTask() (+6 more)

### Community 18 - "WorkspaceScreen"
Cohesion: 0.19
Nodes (6): FocusEvent, FocusLink, FocusLinkKind, ACTIVE, EXTEND, REMINDER

### Community 20 - "TaskScreens.kt"
Cohesion: 0.17
Nodes (5): AndroidIntegrationTest, backendJson(), call(), HttpResponse, parseJson()

### Community 21 - "SettingsScreen"
Cohesion: 0.13
Nodes (16): ByokCredential, AuthCallback, PasswordRecovery, SignedIn, ByokProvider, GEMINI, GROQ, ByokSettings (+8 more)

### Community 22 - "TazerState"
Cohesion: 0.13
Nodes (22): Android :app, Android data layer, Android timer layer, APK Debug, com.tazer.taskorganizer, Compose UI and ViewModel, Kotlin domain, Room repository (+14 more)

### Community 24 - "FocusNotifications"
Cohesion: 0.12
Nodes (17): Aturan global, Batas modul dan struktur target, Graphify, Handoff `i-have-adhd`, i-have-adhd handoff, Impeccable, Pemeriksaan sebelum handoff, Ponytail (+9 more)

### Community 25 - "DateTimeButtons.kt"
Cohesion: 0.13
Nodes (5): NumberWheel(), PickerDateButton(), PickerTimeButton(), platformTheme(), TimeSpinner()

### Community 26 - "compilerOptions"
Cohesion: 0.12
Nodes (19): APK sideload, GitHub Releases gratis, Node.js + TypeScript, Rp 0 free tier, Signed APK Release, Cloudflare Workers Free, Web Fetch API, apksigner verify (+11 more)

### Community 27 - "fail"
Cohesion: 0.25
Nodes (13): Tokens, analyzeGemini(), analyzeGroq(), analyzeWithProvider(), integerSchema(), schema(), validateByok(), analysis() (+5 more)

### Community 28 - "FocusStartTest.kt"
Cohesion: 0.19
Nodes (7): AuthFlow, GOOGLE, RECOVERY, SIGN_UP, AuthPkce, isAuthCallback(), PendingAuth

### Community 30 - "TazerGlyph"
Cohesion: 0.18
Nodes (11): TazerGlyph, Add, Calendar, Down, Edit, Next, Previous, Settings (+3 more)

### Community 31 - "FocusReceiver.kt"
Cohesion: 0.31
Nodes (3): ByokVault, CredentialCipher, AccountUid

### Community 33 - "TazerState"
Cohesion: 0.44
Nodes (5): AccountSettings(), ByokForm(), LoginScreen(), SecretField(), TazerState

### Community 34 - "Autentikasi dan endpoint HTTP"
Cohesion: 0.11
Nodes (18): IntegrationError, AI_UNAVAILABLE, AUTH_REJECTED, BYOK_KEY_MISSING, BYOK_REQUIRED, BYOK_STORAGE_UNAVAILABLE, EMAIL_UNVERIFIED, INVALID_AUTH (+10 more)

### Community 37 - "Rencana eksekusi Tazer"
Cohesion: 0.14
Nodes (15): devDependencies, @types/node@^22.0.0, typescript@~5.9.3, engines, node, name, private, scripts (+7 more)

### Community 38 - "fail"
Cohesion: 0.33
Nodes (3): OfflineAnalysis, OfflineAnalyzer, OfflineAnalyzerTest

### Community 40 - "SessionVault"
Cohesion: 0.14
Nodes (13): compilerOptions, lib, module, noUncheckedIndexedAccess, noUnusedLocals, noUnusedParameters, outDir, rootDir (+5 more)

### Community 41 - "HTTP error envelope"
Cohesion: 0.31
Nodes (14): Belum Selesai and Selesai tabs, OVERDUE display status, Task.status, WorkSession.state, CANCELLED session, DONE task, EXTENDED session, Focus timer state machine (+6 more)

### Community 43 - "HTTP error envelope"
Cohesion: 0.29
Nodes (11): `FixedSchedule`, IF, index_FixedSchedule_ownerUid_dayOfWeek, index_Task_id_ownerUid, index_Task_ownerUid_status_completedAt, index_WorkSession_ownerUid_state_plannedStartAt, index_WorkSession_taskId_ownerUid, room_master_table (+3 more)

### Community 48 - "Intent"
Cohesion: 0.25
Nodes (7): Android client, Supabase and providers, Tazer backend, public.account_entitlements, public.analysis_usage, public.tazer_entitlement(), tazer-db

### Community 50 - "build.gradle.kts"
Cohesion: 0.22
Nodes (9): Analyzer, Autentikasi dan endpoint HTTP, Envelope error, GEMINI_MODEL, GET /health, Health readiness (ready), Kontrak data dan API, SUPABASE_PUBLISHABLE_KEY (+1 more)

### Community 51 - "AuthPkce.kt"
Cohesion: 0.25
Nodes (8): WorkSessionState, CANCELLED, DONE, EXTENDED, OVERTIME, PAUSED, PENDING, RUNNING

### Community 52 - "DateTimeButtons.kt"
Cohesion: 0.29
Nodes (7): FocusEventKind, ACTIVE, CUTOFF, DONE, EXTEND, REMINDER, START_ALARM

### Community 53 - "CalendarDao"
Cohesion: 0.29
Nodes (7): AI_UNAVAILABLE, EMAIL_UNVERIFIED, HTTP error envelope, INVALID_AUTH, PRO_REQUIRED, QUOTA_EXHAUSTED, VERIFICATION_UNAVAILABLE

## Knowledge Gaps
- **95 isolated node(s):** `PasswordRecovery`, `Add`, `Calendar`, `Down`, `Edit` (+90 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 336 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **64 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `FixedSchedule` connect `AndroidIntegration` to `TaskRepository`, `SchedulerTest`, `Spesifikasi scheduler dan timer`, `tazer-v1.sql`, `FocusEvent`, `TazerState`?**
  _High betweenness centrality (0.119) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `WorkSession` (e.g. with `.migratesExportedV1AndPreservesExistingRows()` and `Room`) actually correct?**
  _`WorkSession` has 2 INFERRED edges - model-reasoned connections that need verification._
- **What connects `PasswordRecovery`, `Add`, `Calendar` to the rest of the system?**
  _95 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `AndroidIntegration` be split into smaller, more focused modules?**
  _Cohesion score 0.07116104868913857 - nodes in this community are weakly interconnected._
- **Why does `Spesifikasi scheduler dan timer` connect `TazerState` to `FocusNotifications`, `HTTP error envelope`, `FocusScreens.kt`?**
  _High betweenness centrality (0.108) - this node is a cross-community bridge._
- **Are the 3 inferred relationships involving `FocusMoment` (e.g. with `.pausedWorkIsFrozenAndResumeAddsOnlyTheNewSegment()` and `.restartUsesCheckpointAndOutOfOrderSamplesDoNotEraseIt()`) actually correct?**
  _`FocusMoment` has 3 INFERRED edges - model-reasoned connections that need verification._
- **Should `TazerDao` be split into smaller, more focused modules?**
  _Cohesion score 0.061754385964912284 - nodes in this community are weakly interconnected._