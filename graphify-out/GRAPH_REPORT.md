# Graph Report - Project-B  (2026-11-26)

## Corpus Check
- 98 files · ~67,050 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 23 file(s) not represented in the graph (top: .xml 16, .properties 2, (none) 1)

## Summary
- 1776 nodes · 4872 edges · 117 communities (49 shown, 68 thin omitted)
- Extraction: 94% EXTRACTED · 6% INFERRED · 0% AMBIGUOUS · INFERRED: 288 edges (avg confidence: 0.86)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `c5786ebd`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- SchedulerTest
- TazerDao
- TazerViewModel
- SchedulerTest
- SchedulerTest
- CloudSync
- TaskScreens.kt
- TazerDao
- Spesifikasi scheduler dan timer
- Supabase Auth
- CalendarScreen
- Task
- ReleaseUiTest
- FocusRuntime.kt
- FocusEvent
- FocusScreens.kt
- WorkspaceScreen
- FocusAlarms
- TazerViewModel.kt
- IntegrationHttp.kt
- FocusDnd.kt
- TazerState
- MainActivity.kt
- FocusNotifications
- TaskScreens.kt
- compilerOptions
- ReleaseUiTest.kt
- FocusScreen
- .queuesOfflineChangesAcrossCloseAndReplaysSixEntityTypesAndTombstones
- WidgetContent.kt
- MainActivity.kt
- FocusNotifications
- Autentikasi dan endpoint HTTP
- AndroidIntegrationTest
- FocusEventKind
- WorkspaceScreen
- FocusScreens.kt
- FocusStartTest.kt
- HTTP error envelope
- CloudSync
- FocusNotificationTest.kt
- ThemePreference.kt
- FocusEvent
- Subscription
- .enter
- focusDndAction
- CalendarRepository
- FocusWidgetTest.kt
- SubtaskRepository
- CalendarDao
- AndroidJUnit4
- Before
- TazerState
- CalendarDao
- FocusDndTest.kt
- WeeklyStatistics.kt
- FocusDnd.kt
- WorkSessionState
- FocusNotifications
- FocusStartTest
- SharedPreferences
- Task
- UserProfile
- TazerState
- IcsImportTest.kt
- Test
- FocusEventKind

## God Nodes (most connected - your core abstractions)
1. `TazerViewModel` - 84 edges
2. `TaskRepository` - 74 edges
3. `FocusRuntime` - 69 edges
4. `FocusMoment` - 62 edges
5. `AndroidIntegration` - 44 edges
6. `TaskRepositoryTest` - 42 edges
7. `WorkSession` - 40 edges
8. `fail()` - 40 edges
9. `TazerDatabase` - 35 edges
10. `ReleaseUiTest` - 32 edges

## Surprising Connections (you probably didn't know these)
- `Optional analyzer subtasks negotiation` --references--> `AndroidIntegration`  [INFERRED]
  docs/DATA_AND_API_CONTRACT.md → android/app/src/main/java/com/tazer/taskorganizer/integration/AndroidIntegration.kt
- `Offline queue acknowledgement and cursor` --references--> `CloudSync`  [INFERRED]
  docs/DATA_AND_API_CONTRACT.md → android/app/src/main/java/com/tazer/taskorganizer/data/CloudSync.kt
- `Slot kosong dan batas sesi` --references--> `FixedSchedule`  [INFERRED]
  docs/SCHEDULER_SPEC.md → android/app/src/main/java/com/tazer/taskorganizer/data/Entities.kt
- `Personal pace median from completed work` --references--> `PaceCalibration`  [INFERRED]
  docs/SCHEDULER_SPEC.md → android/app/src/main/java/com/tazer/taskorganizer/domain/PaceCalibration.kt
- `OpenDocument iCalendar import and Room journal` --references--> `IcsParser`  [INFERRED]
  docs/DATA_AND_API_CONTRACT.md → android/app/src/main/java/com/tazer/taskorganizer/domain/IcsParser.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Transactional work history and replanning** — agents_room_repository, docs_data_and_api_contract_worksession, docs_data_and_api_contract_task, docs_scheduler_spec_smart_shift, docs_scheduler_spec_dynamic_reschedule, docs_scheduler_spec_weighted_time_allocation [EXTRACTED 1.00]
- **Agenda pada kalender lokal** — docs_data_and_api_contract_calendarevent, docs_data_and_api_contract_worksession, docs_data_and_api_contract_fixedschedule, docs_data_and_api_contract_task [EXTRACTED 1.00]
- **Persistensi status, progres, dan sisa waktu saat jeda** — docs_scheduler_spec_paused, docs_scheduler_spec_jeda_dan_lanjutkan, docs_data_and_api_contract_worksession_pausedremainingms, docs_data_and_api_contract_worksession_pausedfromstate, docs_data_and_api_contract_worksession_workbeforesegmentms, docs_data_and_api_contract_worksession_continuousworkbeforesegmentms [INFERRED 0.95]

## Communities (117 total, 68 thin omitted)

### Community 0 - "SchedulerTest"
Cohesion: 0.09
Nodes (8): FixedScheduleKind, BREAK, ROUTINE, FocusChange, TaskRepository, FocusClock, FocusMoment, isOvertime()

### Community 1 - "TazerDao"
Cohesion: 0.06
Nodes (55): Analysis, analysisOutput(), analysisSchema, analysisSubtasks(), integerBetween(), isUtcTimestamp(), object(), TaskInput (+47 more)

### Community 3 - "SchedulerTest"
Cohesion: 0.07
Nodes (3): formatTimer(), timerText(), ImportIcsButton()

### Community 4 - "SchedulerTest"
Cohesion: 0.10
Nodes (17): continuousWorkMs(), focusAvailableMs(), focusDurationMs(), remainingAfterInterruption(), FocusPreset, DeadlineRisk, PlannedWork, SchedulePlan (+9 more)

### Community 5 - "CloudSync"
Cohesion: 0.07
Nodes (7): FocusAlarm, FocusRuntime, FocusSettings, FocusService, Scoped DND rule and rollback on leaving a countdown, Release 1.4.0 gate and signed upgrade, Per-account focus preset controls planning and breaks

### Community 6 - "TaskScreens.kt"
Cohesion: 0.13
Nodes (15): PaceCalibration, PaceSample, civilDate(), CourseFocus, DailyFocus, EstimateAccuracy, onTimeStreak(), StatisticsSession (+7 more)

### Community 9 - "Spesifikasi scheduler dan timer"
Cohesion: 0.12
Nodes (36): CalendarEvent, CalendarEvent(ownerUid,startEpochDay), CalendarEvent.startEpochDay dan endEpochDay, CalendarEvent.startMinute, FixedSchedule, Kalender lokal, Migrasi Room v1 ke v2, Migrasi Room v2 ke v3 (+28 more)

### Community 10 - "Supabase Auth"
Cohesion: 0.23
Nodes (9): IcsCalendar, IcsItem, Event, WeeklyBlock, IcsParser, EventLength, EventTime, Property (+1 more)

### Community 11 - "CalendarScreen"
Cohesion: 0.11
Nodes (9): DateSpinner(), dayName(), FixedEditor(), ProfileForm(), ScheduleTag(), SettingsScreen(), TimeField(), formatMinute() (+1 more)

### Community 12 - "Task"
Cohesion: 0.09
Nodes (33): Backend, Server secret store, account_entitlements, analysis_usage, Analyzer input validation, Android Keystore AES-GCM, Atomic quota RPC, BYOK (+25 more)

### Community 14 - "FocusRuntime.kt"
Cohesion: 0.10
Nodes (4): FocusPresetMode, CUSTOM, DEEP_WORK, POMODORO

### Community 16 - "FocusScreens.kt"
Cohesion: 0.11
Nodes (6): AgendaItem, AgendaPill(), CalendarWeek(), EventDate(), EventEditor(), MonthPicker()

### Community 19 - "TazerViewModel.kt"
Cohesion: 0.13
Nodes (14): AndroidIntegrationTest, analyzeGemini(), analyzeGroq(), analyzeWithProvider(), integerSchema(), schema(), validateByok(), backendJson() (+6 more)

### Community 20 - "IntegrationHttp.kt"
Cohesion: 0.14
Nodes (19): Tokens, Account, analysis(), analysisSubtasks(), AuthCallback, PasswordRecovery, SignedIn, fail() (+11 more)

### Community 22 - "TazerState"
Cohesion: 0.14
Nodes (5): DailyFocusChart(), duration(), Statistic(), StatisticsHeading(), toMinutes()

### Community 23 - "MainActivity.kt"
Cohesion: 0.13
Nodes (22): Android :app, Android data layer, Android timer layer, APK Debug, com.tazer.taskorganizer, Compose UI and ViewModel, Kotlin domain, Room repository (+14 more)

### Community 25 - "TaskScreens.kt"
Cohesion: 0.13
Nodes (5): NumberWheel(), PickerDateButton(), PickerTimeButton(), platformTheme(), TimeSpinner()

### Community 26 - "compilerOptions"
Cohesion: 0.12
Nodes (17): Aturan global, Batas modul dan struktur target, Graphify, Handoff `i-have-adhd`, i-have-adhd handoff, Impeccable, Pemeriksaan sebelum handoff, Ponytail (+9 more)

### Community 28 - "ReleaseUiTest.kt"
Cohesion: 0.20
Nodes (8): ByokCredential, ByokVault, CredentialCipher, AccountUid, ByokProvider, GEMINI, GROQ, ByokSettings

### Community 29 - "FocusScreen"
Cohesion: 0.12
Nodes (19): APK sideload, GitHub Releases gratis, Node.js + TypeScript, Rp 0 free tier, Signed APK Release, Cloudflare Workers Free, Web Fetch API, apksigner verify (+11 more)

### Community 30 - ".queuesOfflineChangesAcrossCloseAndReplaysSixEntityTypesAndTombstones"
Cohesion: 0.22
Nodes (10): SubtaskEditor(), SubtaskProgress(), SubtaskTitleEditor(), formatDate(), TaskAction(), TaskEditor(), TasksScreen(), TaskTag() (+2 more)

### Community 31 - "WidgetContent.kt"
Cohesion: 0.08
Nodes (18): FocusWidgetTest, FocusLink, FocusLinkKind, ACTIVE, EXTEND, REMINDER, TazerThemeMode, DARK (+10 more)

### Community 32 - "MainActivity.kt"
Cohesion: 0.11
Nodes (12): AnalysisSource, AI, OFFLINE, SubtaskDraft, ChecklistItem, checklistItem(), AnalysisSuggestion, AnalyzerMode (+4 more)

### Community 33 - "FocusNotifications"
Cohesion: 0.06
Nodes (12): FocusNotificationTest, FocusAlarms, FocusEvent, FocusEventKind, ACTIVE, CUTOFF, DONE, EXTEND (+4 more)

### Community 35 - "AndroidIntegrationTest"
Cohesion: 0.24
Nodes (4): CloudSync, Change, SyncResult, versionWins()

### Community 36 - "FocusEventKind"
Cohesion: 0.12
Nodes (17): IntegrationError, AI_UNAVAILABLE, AUTH_REJECTED, BYOK_KEY_MISSING, BYOK_REQUIRED, BYOK_STORAGE_UNAVAILABLE, EMAIL_UNVERIFIED, INVALID_AUTH (+9 more)

### Community 38 - "WorkspaceScreen"
Cohesion: 0.16
Nodes (14): CalendarScreen(), FocusPermissionsDialog(), TimelineEntry(), TimelineRow, TimelineScreen(), StatisticsScreen(), formatTime(), FloatingTimerDeck() (+6 more)

### Community 39 - "FocusScreens.kt"
Cohesion: 0.20
Nodes (12): CalendarRepositoryTest, CalendarEvent, FixedSchedule, Subtask, Task, TaskStatus, DONE, IN_PROGRESS (+4 more)

### Community 40 - "FocusStartTest.kt"
Cohesion: 0.33
Nodes (3): OfflineAnalysis, OfflineAnalyzer, OfflineAnalyzerTest

### Community 41 - "HTTP error envelope"
Cohesion: 0.14
Nodes (15): devDependencies, @types/node@^22.0.0, typescript@~5.9.3, engines, node, name, private, scripts (+7 more)

### Community 44 - "ThemePreference.kt"
Cohesion: 0.13
Nodes (3): isDark(), ThemeSetting(), ThemeSystemBars()

### Community 46 - "Subscription"
Cohesion: 0.19
Nodes (7): AuthFlow, GOOGLE, RECOVERY, SIGN_UP, AuthPkce, isAuthCallback(), PendingAuth

### Community 48 - "focusDndAction"
Cohesion: 0.27
Nodes (6): FocusDndAction, focusDndAction(), INSTALL, KEEP, WITHDRAW, FocusDndStateTest

### Community 49 - "CalendarRepository"
Cohesion: 0.25
Nodes (13): `CalendarEvent`, `FixedSchedule`, IF, index_CalendarEvent_ownerUid_startEpochDay, index_FixedSchedule_ownerUid_dayOfWeek, index_Task_id_ownerUid, index_Task_ownerUid_status_completedAt, index_WorkSession_ownerUid_state_plannedStartAt (+5 more)

### Community 51 - "FocusWidgetTest.kt"
Cohesion: 0.14
Nodes (14): TazerGlyph, Add, Calendar, Check, Delete, Down, Edit, Next (+6 more)

### Community 52 - "SubtaskRepository"
Cohesion: 0.14
Nodes (13): compilerOptions, lib, module, noUncheckedIndexedAccess, noUnusedLocals, noUnusedParameters, outDir, rootDir (+5 more)

### Community 53 - "CalendarDao"
Cohesion: 0.31
Nodes (14): Belum Selesai and Selesai tabs, OVERDUE display status, Task.status, WorkSession.state, CANCELLED session, DONE task, EXTENDED session, Focus timer state machine (+6 more)

### Community 55 - "Before"
Cohesion: 0.29
Nodes (11): `FixedSchedule`, IF, index_FixedSchedule_ownerUid_dayOfWeek, index_Task_id_ownerUid, index_Task_ownerUid_status_completedAt, index_WorkSession_ownerUid_state_plannedStartAt, index_WorkSession_taskId_ownerUid, room_master_table (+3 more)

### Community 56 - "TazerState"
Cohesion: 0.44
Nodes (5): AccountSettings(), ByokForm(), LoginScreen(), SecretField(), TazerState

### Community 57 - "CalendarDao"
Cohesion: 0.24
Nodes (4): deleteEvent(), IcsImportPreview, observeEvents(), previewIcs()

### Community 58 - "FocusDndTest.kt"
Cohesion: 0.13
Nodes (9): SyncCursor, SyncDevice, SyncEntry, SyncJournal, TazerDatabase, Callback, Optional analyzer subtasks negotiation, Room v4 migration and sync journal (+1 more)

### Community 60 - "FocusDnd.kt"
Cohesion: 0.26
Nodes (6): DeferFocusDialog(), ExtensionDialog(), focusClock(), FocusRhythm(), FocusScreen(), FocusTarget

### Community 61 - "WorkSessionState"
Cohesion: 0.09
Nodes (9): FocusNotificationSoundTest, WorkSessionState, CANCELLED, DONE, EXTENDED, OVERTIME, PAUSED, PENDING (+1 more)

### Community 62 - "FocusNotifications"
Cohesion: 0.22
Nodes (5): public, public.%I, public.tazer_sync_pull(), public.tazer_sync_push(), auth.users

### Community 63 - "FocusStartTest"
Cohesion: 0.25
Nodes (7): Android client, Supabase and providers, Tazer backend, public.account_entitlements, public.analysis_usage, public.tazer_entitlement(), tazer-db

### Community 69 - "SharedPreferences"
Cohesion: 0.22
Nodes (9): Analyzer, Autentikasi dan endpoint HTTP, Envelope error, GEMINI_MODEL, GET /health, Health readiness (ready), Kontrak data dan API, SUPABASE_PUBLISHABLE_KEY (+1 more)

### Community 71 - "Task"
Cohesion: 0.22
Nodes (7): SyncKind, EVENT, FIXED, PROFILE, SESSION, SUBTASK, TASK

### Community 74 - "UserProfile"
Cohesion: 0.29
Nodes (7): AI_UNAVAILABLE, EMAIL_UNVERIFIED, HTTP error envelope, INVALID_AUTH, PRO_REQUIRED, QUOTA_EXHAUSTED, VERIFICATION_UNAVAILABLE

### Community 75 - "TazerState"
Cohesion: 0.33
Nodes (7): Cloud Sync Lamport version and writer tie breaker, Supabase Cloud Sync push and pull RPC, Portable WorkSession history and local monotonic timer, Release 1.3.0 verification gate, Offline queue acknowledgement and cursor, Personal pace median from completed work, Home screen widget follows selected account and focus state

## Knowledge Gaps
- **110 isolated node(s):** `PasswordRecovery`, `BREAK`, `ROUTINE`, `CUSTOM`, `DEEP_WORK` (+105 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 391 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **68 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `FixedSchedule` connect `FocusScreens.kt` to `SchedulerTest`, `MainActivity.kt`, `CalendarScreen`, `FocusEvent`, `WorkspaceScreen`, `MainActivity.kt`, `FocusDndTest.kt`?**
  _High betweenness centrality (0.129) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `TaskRepository` (e.g. with `.acknowledgementCannotClearAnEditMadeDuringARequest()` and `.queuesOfflineChangesAcrossCloseAndReplaysSixEntityTypesAndTombstones()`) actually correct?**
  _`TaskRepository` has 2 INFERRED edges - model-reasoned connections that need verification._
- **What connects `PasswordRecovery`, `BREAK`, `ROUTINE` to the rest of the system?**
  _110 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `SchedulerTest` be split into smaller, more focused modules?**
  _Cohesion score 0.09175050301810865 - nodes in this community are weakly interconnected._
- **Why does `Spesifikasi scheduler dan timer` connect `MainActivity.kt` to `Spesifikasi scheduler dan timer`, `compilerOptions`, `CalendarDao`?**
  _High betweenness centrality (0.122) - this node is a cross-community bridge._
- **Are the 7 inferred relationships involving `FocusRuntime` (e.g. with `FocusAlarms` and `FocusDnd`) actually correct?**
  _`FocusRuntime` has 7 INFERRED edges - model-reasoned connections that need verification._
- **Should `TazerDao` be split into smaller, more focused modules?**
  _Cohesion score 0.05727848101265823 - nodes in this community are weakly interconnected._