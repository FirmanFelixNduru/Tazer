# Graph Report - Project-B  (2026-11-26)

## Corpus Check
- 87 files · ~58,756 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 23 file(s) not represented in the graph (top: .xml 16, .properties 2, (none) 1)
- WARNING: 23 of 111 files seen (21%) have no supported extractor and are missing from this graph entirely. It may look complete while representing only part of the codebase.

## Summary
- 1601 nodes · 4270 edges · 124 communities (42 shown, 82 thin omitted)
- Extraction: 94% EXTRACTED · 6% INFERRED · 0% AMBIGUOUS · INFERRED: 252 edges (avg confidence: 0.86)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `0d230ed5`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- SchedulerTest
- TazerDao
- TazerThemeMode
- SchedulerTest
- TazerViewModel
- CloudSync
- TaskScreens.kt
- TazerDao
- TaskRepository
- Spesifikasi scheduler dan timer
- Supabase Auth
- CalendarScreen
- Task
- ReleaseUiTest
- ThemeSetting
- FocusEvent
- FocusScreens.kt
- WorkspaceScreen
- FocusAlarms
- TazerViewModel.kt
- IntegrationHttp.kt
- SettingsScreen
- TazerState
- MainActivity.kt
- FocusNotifications
- DateTimeButtons.kt
- compilerOptions
- TazerDatabase
- WidgetContent.kt
- FocusScreen
- CalendarRepository
- FocusReceiver.kt
- MainActivity.kt
- SubtaskControls.kt
- Autentikasi dan endpoint HTTP
- AndroidIntegrationTest
- FocusEventKind
- Rencana eksekusi Tazer
- OfflineAnalyzerTest
- FocusStartTest.kt
- HTTP error envelope
- CloudSync
- assertfalse
- CalendarScreens.kt
- FocusEvent
- Subscription
- FocusWidget
- Intent
- CalendarRepository
- build.gradle.kts
- FocusWidgetTest.kt
- SubtaskRepository
- CalendarDao
- AndroidJUnit4
- Before
- RunWith
- CalendarDao
- FocusRuntime.kt
- UserProfile
- FocusNotifications

## God Nodes (most connected - your core abstractions)
1. `TazerViewModel` - 80 edges
2. `TaskRepository` - 75 edges
3. `WorkSession` - 65 edges
4. `FocusMoment` - 62 edges
5. `FocusRuntime` - 56 edges
6. `AndroidIntegration` - 44 edges
7. `TaskRepositoryTest` - 41 edges
8. `fail()` - 40 edges
9. `TazerDatabase` - 34 edges
10. `TazerDao` - 32 edges

## Surprising Connections (you probably didn't know these)
- `Slot kosong dan batas sesi` --references--> `FixedSchedule`  [INFERRED]
  docs/SCHEDULER_SPEC.md → android/app/src/main/java/com/tazer/taskorganizer/data/Entities.kt
- `Optional analyzer subtasks negotiation` --references--> `AndroidIntegration`  [INFERRED]
  docs/DATA_AND_API_CONTRACT.md → android/app/src/main/java/com/tazer/taskorganizer/integration/AndroidIntegration.kt
- `Portable WorkSession history and local monotonic timer` --references--> `FocusRuntime`  [INFERRED]
  docs/DATA_AND_API_CONTRACT.md → android/app/src/main/java/com/tazer/taskorganizer/timer/FocusRuntime.kt
- `Personal pace median from completed work` --references--> `PaceCalibration`  [INFERRED]
  docs/SCHEDULER_SPEC.md → android/app/src/main/java/com/tazer/taskorganizer/domain/PaceCalibration.kt
- `Cloud Sync Lamport version and writer tie breaker` --references--> `SyncJournal`  [INFERRED]
  docs/DATA_AND_API_CONTRACT.md → android/app/src/main/java/com/tazer/taskorganizer/data/SyncJournal.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Transactional work history and replanning** — agents_room_repository, docs_data_and_api_contract_worksession, docs_data_and_api_contract_task, docs_scheduler_spec_smart_shift, docs_scheduler_spec_dynamic_reschedule, docs_scheduler_spec_weighted_time_allocation [EXTRACTED 1.00]
- **Agenda pada kalender lokal** — docs_data_and_api_contract_calendarevent, docs_data_and_api_contract_worksession, docs_data_and_api_contract_fixedschedule, docs_data_and_api_contract_task [EXTRACTED 1.00]
- **Persistensi status, progres, dan sisa waktu saat jeda** — docs_scheduler_spec_paused, docs_scheduler_spec_jeda_dan_lanjutkan, docs_data_and_api_contract_worksession_pausedremainingms, docs_data_and_api_contract_worksession_pausedfromstate, docs_data_and_api_contract_worksession_workbeforesegmentms, docs_data_and_api_contract_worksession_continuousworkbeforesegmentms [INFERRED 0.95]

## Communities (124 total, 82 thin omitted)

### Community 1 - "TazerDao"
Cohesion: 0.06
Nodes (55): Analysis, analysisOutput(), analysisSchema, analysisSubtasks(), integerBetween(), isUtcTimestamp(), object(), TaskInput (+47 more)

### Community 2 - "TazerThemeMode"
Cohesion: 0.33
Nodes (5): TazerThemeMode, DARK, LIGHT, SYSTEM, ThemePreference

### Community 4 - "TazerViewModel"
Cohesion: 0.08
Nodes (12): AccountSettings(), ByokForm(), LoginScreen(), SecretField(), AnalyzerMode, BYOK, CLOUD, OFFLINE (+4 more)

### Community 5 - "CloudSync"
Cohesion: 0.06
Nodes (24): CloudSync, Change, SyncResult, SyncKind, EVENT, FIXED, PROFILE, SESSION (+16 more)

### Community 7 - "TazerDao"
Cohesion: 0.09
Nodes (13): DateSpinner(), dayName(), FixedEditor(), ProfileForm(), ScheduleTag(), SettingsScreen(), TimeField(), TimelineEntry() (+5 more)

### Community 8 - "TaskRepository"
Cohesion: 0.06
Nodes (19): WorkSession, FocusChange, TaskRepository, continuousWorkMs(), focusAvailableMs(), FocusClock, focusDurationMs(), FocusMoment (+11 more)

### Community 9 - "Spesifikasi scheduler dan timer"
Cohesion: 0.19
Nodes (3): AndroidIntegration, Session, Account

### Community 10 - "Supabase Auth"
Cohesion: 0.12
Nodes (36): CalendarEvent, CalendarEvent(ownerUid,startEpochDay), CalendarEvent.startEpochDay dan endEpochDay, CalendarEvent.startMinute, FixedSchedule, Kalender lokal, Migrasi Room v1 ke v2, Migrasi Room v2 ke v3 (+28 more)

### Community 11 - "CalendarScreen"
Cohesion: 0.09
Nodes (33): Backend, Server secret store, account_entitlements, analysis_usage, Analyzer input validation, Android Keystore AES-GCM, Atomic quota RPC, BYOK (+25 more)

### Community 12 - "Task"
Cohesion: 0.09
Nodes (4): networkAvailability(), NetworkCallback, MainActivity, Factory

### Community 13 - "ReleaseUiTest"
Cohesion: 0.11
Nodes (5): FloatingTimerDeck(), TazerApp(), TazerIcon(), TimerIconButton(), WorkspaceScreen()

### Community 14 - "ThemeSetting"
Cohesion: 0.40
Nodes (3): isDark(), ThemeSetting(), ThemeSystemBars()

### Community 17 - "WorkspaceScreen"
Cohesion: 0.12
Nodes (14): AndroidIntegrationTest, analyzeGemini(), analyzeGroq(), analyzeWithProvider(), integerSchema(), schema(), validateByok(), backendJson() (+6 more)

### Community 19 - "TazerViewModel.kt"
Cohesion: 0.12
Nodes (5): NumberWheel(), PickerDateButton(), PickerTimeButton(), platformTheme(), TimeSpinner()

### Community 20 - "IntegrationHttp.kt"
Cohesion: 0.19
Nodes (14): CalendarEvent, FixedSchedule, FixedScheduleKind, BREAK, ROUTINE, Subtask, Task, TaskStatus (+6 more)

### Community 22 - "TazerState"
Cohesion: 0.12
Nodes (11): AnalysisSource, AI, OFFLINE, SubtaskDraft, ChecklistItem, checklistItem(), TaskEditor(), AnalysisSuggestion (+3 more)

### Community 23 - "MainActivity.kt"
Cohesion: 0.14
Nodes (7): SyncCursor, SyncDevice, SyncEntry, SyncJournal, TazerDatabase, Callback, Room v4 migration and sync journal

### Community 24 - "FocusNotifications"
Cohesion: 0.13
Nodes (22): Android :app, Android data layer, Android timer layer, APK Debug, com.tazer.taskorganizer, Compose UI and ViewModel, Kotlin domain, Room repository (+14 more)

### Community 25 - "DateTimeButtons.kt"
Cohesion: 0.14
Nodes (4): formatDate(), TaskAction(), TasksScreen(), TaskTag()

### Community 27 - "TazerDatabase"
Cohesion: 0.12
Nodes (17): Aturan global, Batas modul dan struktur target, Graphify, Handoff `i-have-adhd`, i-have-adhd handoff, Impeccable, Pemeriksaan sebelum handoff, Ponytail (+9 more)

### Community 28 - "WidgetContent.kt"
Cohesion: 0.25
Nodes (11): FocusLink, FocusLinkKind, ACTIVE, EXTEND, REMINDER, activeWidget(), link(), loadWidgetSnapshot() (+3 more)

### Community 29 - "FocusScreen"
Cohesion: 0.17
Nodes (12): DeferFocusDialog(), ExtensionDialog(), focusClock(), FocusPermissionsDialog(), FocusScreen(), formatTimer(), isOvertime(), timerText() (+4 more)

### Community 30 - "CalendarRepository"
Cohesion: 0.23
Nodes (3): CloudSyncTest, Remote, CalendarRepository

### Community 33 - "SubtaskControls.kt"
Cohesion: 0.12
Nodes (19): APK sideload, GitHub Releases gratis, Node.js + TypeScript, Rp 0 free tier, Signed APK Release, Cloudflare Workers Free, Web Fetch API, apksigner verify (+11 more)

### Community 34 - "Autentikasi dan endpoint HTTP"
Cohesion: 0.15
Nodes (18): Tokens, analysis(), analysisSubtasks(), AuthCallback, PasswordRecovery, SignedIn, fail(), integer() (+10 more)

### Community 35 - "AndroidIntegrationTest"
Cohesion: 0.19
Nodes (7): AuthFlow, GOOGLE, RECOVERY, SIGN_UP, AuthPkce, isAuthCallback(), PendingAuth

### Community 36 - "FocusEventKind"
Cohesion: 0.29
Nodes (7): FocusEventKind, ACTIVE, CUTOFF, DONE, EXTEND, REMINDER, START_ALARM

### Community 37 - "Rencana eksekusi Tazer"
Cohesion: 0.20
Nodes (8): ByokCredential, ByokVault, CredentialCipher, AccountUid, ByokProvider, GEMINI, GROQ, ByokSettings

### Community 38 - "OfflineAnalyzerTest"
Cohesion: 0.12
Nodes (17): IntegrationError, AI_UNAVAILABLE, AUTH_REJECTED, BYOK_KEY_MISSING, BYOK_REQUIRED, BYOK_STORAGE_UNAVAILABLE, EMAIL_UNVERIFIED, INVALID_AUTH (+9 more)

### Community 41 - "HTTP error envelope"
Cohesion: 0.33
Nodes (3): OfflineAnalysis, OfflineAnalyzer, OfflineAnalyzerTest

### Community 42 - "CloudSync"
Cohesion: 0.14
Nodes (15): devDependencies, @types/node@^22.0.0, typescript@~5.9.3, engines, node, name, private, scripts (+7 more)

### Community 44 - "CalendarScreens.kt"
Cohesion: 0.11
Nodes (7): AgendaItem, AgendaPill(), CalendarScreen(), CalendarWeek(), EventDate(), EventEditor(), MonthPicker()

### Community 46 - "Subscription"
Cohesion: 0.25
Nodes (13): `CalendarEvent`, `FixedSchedule`, IF, index_CalendarEvent_ownerUid_startEpochDay, index_FixedSchedule_ownerUid_dayOfWeek, index_Task_id_ownerUid, index_Task_ownerUid_status_completedAt, index_WorkSession_ownerUid_state_plannedStartAt (+5 more)

### Community 48 - "Intent"
Cohesion: 0.14
Nodes (13): compilerOptions, lib, module, noUncheckedIndexedAccess, noUnusedLocals, noUnusedParameters, outDir, rootDir (+5 more)

### Community 49 - "CalendarRepository"
Cohesion: 0.31
Nodes (14): Belum Selesai and Selesai tabs, OVERDUE display status, Task.status, WorkSession.state, CANCELLED session, DONE task, EXTENDED session, Focus timer state machine (+6 more)

### Community 50 - "build.gradle.kts"
Cohesion: 0.25
Nodes (8): WorkSessionState, CANCELLED, DONE, EXTENDED, OVERTIME, PAUSED, PENDING, RUNNING

### Community 52 - "SubtaskRepository"
Cohesion: 0.15
Nodes (13): TazerGlyph, Add, Calendar, Check, Delete, Down, Edit, Next (+5 more)

### Community 53 - "CalendarDao"
Cohesion: 0.29
Nodes (11): `FixedSchedule`, IF, index_FixedSchedule_ownerUid_dayOfWeek, index_Task_id_ownerUid, index_Task_ownerUid_status_completedAt, index_WorkSession_ownerUid_state_plannedStartAt, index_WorkSession_taskId_ownerUid, room_master_table (+3 more)

### Community 54 - "AndroidJUnit4"
Cohesion: 0.22
Nodes (5): public, public.%I, public.tazer_sync_pull(), public.tazer_sync_push(), auth.users

### Community 55 - "Before"
Cohesion: 0.25
Nodes (7): Android client, Supabase and providers, Tazer backend, public.account_entitlements, public.analysis_usage, public.tazer_entitlement(), tazer-db

### Community 56 - "RunWith"
Cohesion: 0.22
Nodes (9): Analyzer, Autentikasi dan endpoint HTTP, Envelope error, GEMINI_MODEL, GET /health, Health readiness (ready), Kontrak data dan API, SUPABASE_PUBLISHABLE_KEY (+1 more)

### Community 61 - "UserProfile"
Cohesion: 0.29
Nodes (7): AI_UNAVAILABLE, EMAIL_UNVERIFIED, HTTP error envelope, INVALID_AUTH, PRO_REQUIRED, QUOTA_EXHAUSTED, VERIFICATION_UNAVAILABLE

## Knowledge Gaps
- **103 isolated node(s):** `PasswordRecovery`, `DARK`, `LIGHT`, `SYSTEM`, `BREAK` (+98 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 389 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **82 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `FixedSchedule` connect `IntegrationHttp.kt` to `SchedulerTest`, `TazerViewModel`, `TazerDao`, `TaskRepository`, `FocusScreens.kt`, `TazerState`, `MainActivity.kt`, `FocusNotifications`?**
  _High betweenness centrality (0.127) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `TaskRepository` (e.g. with `.acknowledgementCannotClearAnEditMadeDuringARequest()` and `.queuesOfflineChangesAcrossCloseAndReplaysSixEntityTypesAndTombstones()`) actually correct?**
  _`TaskRepository` has 2 INFERRED edges - model-reasoned connections that need verification._
- **What connects `PasswordRecovery`, `DARK`, `LIGHT` to the rest of the system?**
  _103 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `TazerDao` be split into smaller, more focused modules?**
  _Cohesion score 0.05727848101265823 - nodes in this community are weakly interconnected._
- **Why does `Spesifikasi scheduler dan timer` connect `FocusNotifications` to `CalendarRepository`, `Supabase Auth`, `TazerDatabase`?**
  _High betweenness centrality (0.116) - this node is a cross-community bridge._
- **Are the 3 inferred relationships involving `WorkSession` (e.g. with `.migratesExportedV1AndPreservesExistingRows()` and `.queuesOfflineChangesAcrossCloseAndReplaysSixEntityTypesAndTombstones()`) actually correct?**
  _`WorkSession` has 3 INFERRED edges - model-reasoned connections that need verification._
- **Should `TazerViewModel` be split into smaller, more focused modules?**
  _Cohesion score 0.07526881720430108 - nodes in this community are weakly interconnected._