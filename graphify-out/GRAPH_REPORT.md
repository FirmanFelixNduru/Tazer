# Graph Report - Project-B  (2026-11-26)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 1826 nodes · 5017 edges · 119 communities (54 shown, 65 thin omitted)
- Extraction: 94% EXTRACTED · 6% INFERRED · 0% AMBIGUOUS · INFERRED: 311 edges (avg confidence: 0.86)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `b08173ab`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- SchedulerTest
- handler.ts
- TazerViewModel
- TaskRepository
- CloudSync
- WeeklyStatistics.kt
- ReleaseUpdateSetting.kt
- FocusRuntime
- TaskRepositoryTest
- Spesifikasi scheduler dan timer
- ScheduleScreens.kt
- SettingsScreen
- ReleaseUiTest.kt
- ReleaseUiTest
- FocusService.kt
- FocusWidgetTest.kt
- FocusEvent
- WidgetContent.kt
- ByokVault.kt
- IcsParser.kt
- TaskRepositoryTest.kt
- TazerDao
- MainActivity
- TazerApp.kt
- AndroidIntegrationTest
- TaskScreens.kt
- compilerOptions
- Supabase Auth
- FocusStartTest
- IntegrationError
- WorkSession
- uuid
- FocusDnd.kt
- PendingAuth
- TazerViewModel.kt
- DateTimeButtons.kt
- Signed APK Release
- IntegrationModels.kt
- StatisticsScreen.kt
- parseReleaseUpdate
- FocusScreens.kt
- Claude 5.5 model mapping
- WorkSession
- OfflineAnalyzerTest
- AccountScreens.kt
- FocusNotifications
- Rencana eksekusi Tazer
- .change
- package.json
- CloudSyncTest.kt
- CalendarDao
- AccountUid
- Focus timer state machine
- tazer-v3.sql
- TazerGlyph
- compilerOptions
- FocusChange
- SyncJournal.kt
- IcsParserTest
- 20261125000000_entitlement.sql
- tazer-v1.sql
- SubtaskRepository
- FocusAlarms
- IcsImportTest.kt
- public.tazer_sync_pull
- .focusAction
- Autentikasi dan endpoint HTTP
- FocusNotificationSoundTest.kt
- HTTP error envelope
- AnalysisSource
- FixedScheduleKind
- WorkspaceScreen
- .analyze
- Per-account focus preset controls planning and breaks

## God Nodes (most connected - your core abstractions)
1. `TazerViewModel` - 85 edges
2. `TaskRepository` - 74 edges
3. `FocusRuntime` - 69 edges
4. `FocusMoment` - 62 edges
5. `AndroidIntegration` - 45 edges
6. `fail()` - 43 edges
7. `TaskRepositoryTest` - 42 edges
8. `WorkSession` - 40 edges
9. `TazerDatabase` - 35 edges
10. `ReleaseUiTest` - 34 edges

## Surprising Connections (you probably didn't know these)
- `Slot kosong dan batas sesi` --references--> `FixedSchedule`  [INFERRED]
  docs/SCHEDULER_SPEC.md → android/app/src/main/java/com/tazer/taskorganizer/data/Entities.kt
- `Personal pace median from completed work` --references--> `PaceCalibration`  [INFERRED]
  docs/SCHEDULER_SPEC.md → android/app/src/main/java/com/tazer/taskorganizer/domain/PaceCalibration.kt
- `Optional analyzer subtasks negotiation` --references--> `AndroidIntegration`  [INFERRED]
  docs/DATA_AND_API_CONTRACT.md → android/app/src/main/java/com/tazer/taskorganizer/integration/AndroidIntegration.kt
- `Per-account focus preset controls planning and breaks` --references--> `FocusPreset`  [INFERRED]
  docs/SCHEDULER_SPEC.md → android/app/src/main/java/com/tazer/taskorganizer/domain/FocusPreset.kt
- `Per-account focus preset controls planning and breaks` --references--> `Scheduler`  [INFERRED]
  docs/SCHEDULER_SPEC.md → android/app/src/main/java/com/tazer/taskorganizer/domain/Scheduler.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Transactional work history and replanning** — agents_room_repository, docs_data_and_api_contract_worksession, docs_data_and_api_contract_task, docs_scheduler_spec_smart_shift, docs_scheduler_spec_dynamic_reschedule, docs_scheduler_spec_weighted_time_allocation [EXTRACTED 1.00]
- **Agenda pada kalender lokal** — docs_data_and_api_contract_calendarevent, docs_data_and_api_contract_worksession, docs_data_and_api_contract_fixedschedule, docs_data_and_api_contract_task [EXTRACTED 1.00]
- **Persistensi status, progres, dan sisa waktu saat jeda** — docs_scheduler_spec_paused, docs_scheduler_spec_jeda_dan_lanjutkan, docs_data_and_api_contract_worksession_pausedremainingms, docs_data_and_api_contract_worksession_pausedfromstate, docs_data_and_api_contract_worksession_workbeforesegmentms, docs_data_and_api_contract_worksession_continuousworkbeforesegmentms [INFERRED 0.95]

## Communities (119 total, 65 thin omitted)

### Community 0 - "SchedulerTest"
Cohesion: 0.08
Nodes (21): continuousWorkMs(), focusAvailableMs(), FocusClock, focusDurationMs(), remainingAfterInterruption(), FocusPreset, FocusPresetMode, CUSTOM (+13 more)

### Community 1 - "handler.ts"
Cohesion: 0.06
Nodes (56): Analysis, analysisOutput(), analysisSchema, analysisSubtasks(), integerBetween(), isUtcTimestamp(), object(), TaskInput (+48 more)

### Community 2 - "TazerViewModel"
Cohesion: 0.14
Nodes (3): IntegrationException, rejectsSession(), TazerViewModel

### Community 4 - "CloudSync"
Cohesion: 0.08
Nodes (18): CloudSync, Change, SyncResult, SyncKind, EVENT, FIXED, PROFILE, SESSION (+10 more)

### Community 5 - "WeeklyStatistics.kt"
Cohesion: 0.12
Nodes (15): PaceCalibration, PaceSample, civilDate(), CourseFocus, DailyFocus, EstimateAccuracy, onTimeStreak(), StatisticsSession (+7 more)

### Community 6 - "ReleaseUpdateSetting.kt"
Cohesion: 0.09
Nodes (3): isDark(), ThemeSetting(), ThemeSystemBars()

### Community 7 - "FocusRuntime"
Cohesion: 0.16
Nodes (3): FocusAlarm, FocusRuntime, FocusSettings

### Community 9 - "Spesifikasi scheduler dan timer"
Cohesion: 0.21
Nodes (5): AndroidIntegration, Session, Tokens, fail(), json()

### Community 10 - "ScheduleScreens.kt"
Cohesion: 0.07
Nodes (9): AgendaItem, AgendaPill(), CalendarScreen(), CalendarWeek(), EventDate(), EventEditor(), MonthPicker(), DateSpinner() (+1 more)

### Community 11 - "SettingsScreen"
Cohesion: 0.25
Nodes (11): dayName(), FixedEditor(), ProfileForm(), ScheduleTag(), SettingsScreen(), TimeField(), TimelineEntry(), TimelineRow (+3 more)

### Community 12 - "ReleaseUiTest.kt"
Cohesion: 0.08
Nodes (9): FocusNotificationTest, WorkSessionState, CANCELLED, DONE, EXTENDED, OVERTIME, PAUSED, PENDING (+1 more)

### Community 16 - "FocusEvent"
Cohesion: 0.16
Nodes (9): FocusEvent, FocusEventKind, ACTIVE, CUTOFF, DONE, EXTEND, REMINDER, START_ALARM (+1 more)

### Community 17 - "WidgetContent.kt"
Cohesion: 0.08
Nodes (17): FocusWidgetTest, FocusLinkKind, ACTIVE, EXTEND, REMINDER, TazerThemeMode, DARK, LIGHT (+9 more)

### Community 18 - "ByokVault.kt"
Cohesion: 0.10
Nodes (3): isAuthCallback(), credentialStorage(), SessionVault

### Community 19 - "IcsParser.kt"
Cohesion: 0.09
Nodes (13): deleteEvent(), IcsImportPreview, observeEvents(), previewIcs(), IcsCalendar, IcsItem, Event, WeeklyBlock (+5 more)

### Community 22 - "MainActivity"
Cohesion: 0.10
Nodes (4): networkAvailability(), NetworkCallback, MainActivity, Factory

### Community 23 - "TazerApp.kt"
Cohesion: 0.10
Nodes (3): FloatingTimerDeck(), TazerIcon(), TimerIconButton()

### Community 24 - "AndroidIntegrationTest"
Cohesion: 0.12
Nodes (14): AndroidIntegrationTest, analyzeGemini(), analyzeGroq(), analyzeWithProvider(), integerSchema(), schema(), validateByok(), backendJson() (+6 more)

### Community 25 - "TaskScreens.kt"
Cohesion: 0.08
Nodes (3): ImportIcsButton(), formatDate(), schedulingTask()

### Community 26 - "compilerOptions"
Cohesion: 0.17
Nodes (16): Android data layer, Android timer layer, Kotlin domain, Room repository, Analisis offline, Analyzer offline, Android timer dan pemulihan, Priority Ranking Engine (+8 more)

### Community 27 - "Supabase Auth"
Cohesion: 0.14
Nodes (22): Backend, Server secret store, Analyzer input validation, Entitlement akun dan kuota bulanan, Gemini API Free, GET /auth/session, GET /subscription, Groq Free (+14 more)

### Community 29 - "IntegrationError"
Cohesion: 0.10
Nodes (20): IntegrationError, AI_UNAVAILABLE, AUTH_REJECTED, BROWSER_UNAVAILABLE, BYOK_KEY_MISSING, BYOK_REQUIRED, BYOK_STORAGE_UNAVAILABLE, EMAIL_UNVERIFIED (+12 more)

### Community 30 - "WorkSession"
Cohesion: 0.14
Nodes (31): CalendarEvent, CalendarEvent(ownerUid,startEpochDay), CalendarEvent.startEpochDay dan endEpochDay, CalendarEvent.startMinute, FixedSchedule, Kalender lokal, Migrasi Room v1 ke v2, Migrasi Room v2 ke v3 (+23 more)

### Community 32 - "FocusDnd.kt"
Cohesion: 0.14
Nodes (7): FocusDnd, FocusDndAction, focusDndAction(), INSTALL, KEEP, WITHDRAW, FocusDndStateTest

### Community 33 - "PendingAuth"
Cohesion: 0.18
Nodes (8): AuthFlow, GOOGLE, RECOVERY, SIGN_UP, AuthPkce, PendingAuth, AuthPkceTest, Google OAuth browser PKCE with state and cancellation

### Community 34 - "TazerViewModel.kt"
Cohesion: 0.13
Nodes (8): SubtaskDraft, ChecklistItem, checklistItem(), TaskEditor(), asDraft(), ChecklistChange, OpenDocument iCalendar import and Room journal, Repeat-safe iCalendar import identity

### Community 35 - "DateTimeButtons.kt"
Cohesion: 0.14
Nodes (5): NumberWheel(), PickerDateButton(), PickerTimeButton(), platformTheme(), TimeSpinner()

### Community 36 - "Signed APK Release"
Cohesion: 0.18
Nodes (13): APK sideload, GitHub Releases gratis, Node.js + TypeScript, Rp 0 free tier, Signed APK Release, Cloudflare Workers Free, Web Fetch API, apksigner verify (+5 more)

### Community 37 - "IntegrationModels.kt"
Cohesion: 0.13
Nodes (18): Account, analysis(), analysisSubtasks(), AuthCallback, PasswordRecovery, SignedIn, ByokProvider, GEMINI (+10 more)

### Community 38 - "StatisticsScreen.kt"
Cohesion: 0.16
Nodes (5): DailyFocusChart(), duration(), Statistic(), StatisticsHeading(), toMinutes()

### Community 39 - "parseReleaseUpdate"
Cohesion: 0.18
Nodes (6): parseReleaseUpdate(), ReleaseUpdate, SemVer, ReleaseUpdateSetting(), ReleaseUpdatesTest, GitHub Releases SemVer checks and signed APK distribution

### Community 40 - "FocusScreens.kt"
Cohesion: 0.22
Nodes (9): DeferFocusDialog(), ExtensionDialog(), focusClock(), FocusPermissionsDialog(), FocusRhythm(), FocusScreen(), formatTimer(), isOvertime() (+1 more)

### Community 41 - "Claude 5.5 model mapping"
Cohesion: 0.08
Nodes (29): Android :app, APK Debug, Aturan global, Batas modul dan struktur target, Claude 5.5 model mapping, com.tazer.taskorganizer, Compose UI and ViewModel, Graphify (+21 more)

### Community 42 - "WorkSession"
Cohesion: 0.23
Nodes (12): CalendarEvent, FixedSchedule, Subtask, Task, TaskStatus, DONE, IN_PROGRESS, PENDING (+4 more)

### Community 43 - "OfflineAnalyzerTest"
Cohesion: 0.29
Nodes (3): OfflineAnalysis, OfflineAnalyzer, OfflineAnalyzerTest

### Community 44 - "AccountScreens.kt"
Cohesion: 0.10
Nodes (3): SubtaskEditor(), SubtaskProgress(), SubtaskTitleEditor()

### Community 46 - "Rencana eksekusi Tazer"
Cohesion: 0.13
Nodes (17): account_entitlements, analysis_usage, Android Keystore AES-GCM, Atomic quota RPC, BYOK, FREE tier, Kuota bulanan UTC, PostgreSQL (+9 more)

### Community 48 - "package.json"
Cohesion: 0.14
Nodes (15): devDependencies, @types/node@^22.0.0, typescript@~5.9.3, engines, node, name, private, scripts (+7 more)

### Community 51 - "AccountUid"
Cohesion: 0.25
Nodes (5): ByokCredential, ByokVault, CredentialCipher, AccountUid, ByokSettings

### Community 52 - "Focus timer state machine"
Cohesion: 0.22
Nodes (20): Belum Selesai and Selesai tabs, OVERDUE display status, Task.status, CANCELLED session, DONE task, Durasi fokus penuh, elapsedRealtime, EXTENDED session (+12 more)

### Community 53 - "tazer-v3.sql"
Cohesion: 0.25
Nodes (13): `CalendarEvent`, `FixedSchedule`, IF, index_CalendarEvent_ownerUid_startEpochDay, index_FixedSchedule_ownerUid_dayOfWeek, index_Task_id_ownerUid, index_Task_ownerUid_status_completedAt, index_WorkSession_ownerUid_state_plannedStartAt (+5 more)

### Community 54 - "TazerGlyph"
Cohesion: 0.14
Nodes (14): TazerGlyph, Add, Calendar, Check, Delete, Down, Edit, Next (+6 more)

### Community 55 - "compilerOptions"
Cohesion: 0.14
Nodes (13): compilerOptions, lib, module, noUncheckedIndexedAccess, noUnusedLocals, noUnusedParameters, outDir, rootDir (+5 more)

### Community 57 - "SyncJournal.kt"
Cohesion: 0.26
Nodes (7): SyncCursor, SyncDevice, SyncEntry, SyncJournal, Optional analyzer subtasks negotiation, Room v4 migration and sync journal, Task checklist: Subtask with composite owner foreign key

### Community 59 - "20261125000000_entitlement.sql"
Cohesion: 0.24
Nodes (8): Android client, Supabase and providers, Tazer backend, public.account_entitlements, public.analysis_usage, public.tazer_entitlement(), public.tazer_entitlement(), tazer-db

### Community 60 - "tazer-v1.sql"
Cohesion: 0.29
Nodes (11): `FixedSchedule`, IF, index_FixedSchedule_ownerUid_dayOfWeek, index_Task_id_ownerUid, index_Task_ownerUid_status_completedAt, index_WorkSession_ownerUid_state_plannedStartAt, index_WorkSession_taskId_ownerUid, room_master_table (+3 more)

### Community 64 - "public.tazer_sync_pull"
Cohesion: 0.22
Nodes (5): public, public.%I, public.tazer_sync_pull(), public.tazer_sync_push(), auth.users

### Community 66 - "Autentikasi dan endpoint HTTP"
Cohesion: 0.22
Nodes (9): Analyzer, Autentikasi dan endpoint HTTP, Envelope error, GEMINI_MODEL, GET /health, Health readiness (ready), Kontrak data dan API, SUPABASE_PUBLISHABLE_KEY (+1 more)

### Community 68 - "HTTP error envelope"
Cohesion: 0.29
Nodes (7): AI_UNAVAILABLE, EMAIL_UNVERIFIED, HTTP error envelope, INVALID_AUTH, PRO_REQUIRED, QUOTA_EXHAUSTED, VERIFICATION_UNAVAILABLE

### Community 69 - "AnalysisSource"
Cohesion: 0.50
Nodes (4): AnalysisSource, AI, OFFLINE, AnalysisSuggestion

### Community 70 - "FixedScheduleKind"
Cohesion: 0.67
Nodes (3): FixedScheduleKind, BREAK, ROUTINE

### Community 73 - "WorkspaceScreen"
Cohesion: 0.17
Nodes (13): AccountSettings(), ByokForm(), LoginScreen(), SecretField(), StatisticsScreen(), TaskAction(), TasksScreen(), TaskTag() (+5 more)

### Community 115 - ".analyze"
Cohesion: 0.33
Nodes (4): AnalyzerMode, BYOK, CLOUD, OFFLINE

### Community 116 - "Per-account focus preset controls planning and breaks"
Cohesion: 1.00
Nodes (3): Scoped DND rule and rollback on leaving a countdown, Release 1.4.0 gate and signed upgrade, Per-account focus preset controls planning and breaks

## Knowledge Gaps
- **115 isolated node(s):** `PasswordRecovery`, `BYOK`, `CLOUD`, `OFFLINE`, `ACTIVE` (+110 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 393 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **65 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `FixedSchedule` connect `WorkSession` to `TazerViewModel.kt`, `FixedScheduleKind`, `ScheduleScreens.kt`, `CalendarDao`, `TazerDao`, `compilerOptions`?**
  _High betweenness centrality (0.134) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `TaskRepository` (e.g. with `.acknowledgementCannotClearAnEditMadeDuringARequest()` and `.queuesOfflineChangesAcrossCloseAndReplaysSixEntityTypesAndTombstones()`) actually correct?**
  _`TaskRepository` has 2 INFERRED edges - model-reasoned connections that need verification._
- **What connects `PasswordRecovery`, `BYOK`, `CLOUD` to the rest of the system?**
  _115 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `SchedulerTest` be split into smaller, more focused modules?**
  _Cohesion score 0.07984442919240449 - nodes in this community are weakly interconnected._
- **Why does `Spesifikasi scheduler dan timer` connect `compilerOptions` to `Claude 5.5 model mapping`, `Focus timer state machine`, `WorkSession`?**
  _High betweenness centrality (0.109) - this node is a cross-community bridge._
- **Are the 7 inferred relationships involving `FocusRuntime` (e.g. with `FocusAlarms` and `FocusDnd`) actually correct?**
  _`FocusRuntime` has 7 INFERRED edges - model-reasoned connections that need verification._
- **Should `handler.ts` be split into smaller, more focused modules?**
  _Cohesion score 0.05617283950617284 - nodes in this community are weakly interconnected._