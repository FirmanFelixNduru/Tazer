# Graph Report - Project-B  (2026-11-26)

## Corpus Check
- 105 files · ~68,870 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 23 file(s) not represented in the graph (top: .xml 16, .properties 2, (none) 1)

## Summary
- 1821 nodes · 5003 edges · 115 communities (53 shown, 62 thin omitted)
- Extraction: 94% EXTRACTED · 6% INFERRED · 0% AMBIGUOUS · INFERRED: 310 edges (avg confidence: 0.86)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- SchedulerTest
- handler.ts
- TazerViewModel
- TaskRepository
- CloudSync
- StatisticsScreen
- FocusRuntime
- TaskRepositoryTest
- AndroidIntegration
- CalendarScreens.kt
- ScheduleScreens.kt
- FocusNotifications
- ReleaseUiTest
- FocusRuntime.kt
- FocusEvent
- FocusWidget.kt
- ByokVault.kt
- IcsParser
- TaskRepositoryTest.kt
- TazerDao
- MainActivity
- WidgetContent.kt
- IntegrationHttp.kt
- ImportIcsButton.kt
- Spesifikasi scheduler dan timer
- Supabase Auth
- IntegrationError
- Room schema
- FocusDnd.kt
- .get
- PendingAuth
- TazerViewModel.kt
- DateTimeButtons.kt
- Signed APK Release
- IntegrationModels.kt
- CalendarRepository.kt
- Uri
- FocusScreens.kt
- Aturan global
- WorkSession
- OfflineAnalyzerTest
- TaskScreens.kt
- ThemePreference.kt
- Rencana eksekusi Tazer
- SubtaskControls.kt
- package.json
- AndroidIntegrationTest
- CalendarDao
- AccountUid
- Focus timer state machine
- tazer-v3.sql
- TazerGlyph
- compilerOptions
- WorkSession
- SubtaskRepository
- IcsParserTest
- 20261125000000_entitlement.sql
- tazer-v1.sql
- SubtaskRepository.kt
- focusDndAction
- IcsImportTest.kt
- public.tazer_sync_pull
- WorkSessionState
- Autentikasi dan endpoint HTTP
- FocusNotificationTest.kt
- HTTP error envelope
- FocusPresetMode
- TaskStatus

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
- `Optional analyzer subtasks negotiation` --references--> `AndroidIntegration`  [INFERRED]
  docs/DATA_AND_API_CONTRACT.md → android/app/src/main/java/com/tazer/taskorganizer/integration/AndroidIntegration.kt
- `Slot kosong dan batas sesi` --references--> `FixedSchedule`  [INFERRED]
  docs/SCHEDULER_SPEC.md → android/app/src/main/java/com/tazer/taskorganizer/data/Entities.kt
- `Personal pace median from completed work` --references--> `PaceCalibration`  [INFERRED]
  docs/SCHEDULER_SPEC.md → android/app/src/main/java/com/tazer/taskorganizer/domain/PaceCalibration.kt
- `Supabase Cloud Sync push and pull RPC` --references--> `AndroidIntegration`  [INFERRED]
  docs/DATA_AND_API_CONTRACT.md → android/app/src/main/java/com/tazer/taskorganizer/integration/AndroidIntegration.kt
- `Home screen widget follows selected account and focus state` --references--> `FocusWidget`  [INFERRED]
  docs/SCHEDULER_SPEC.md → android/app/src/main/java/com/tazer/taskorganizer/widget/FocusWidget.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Transactional work history and replanning** — agents_room_repository, docs_data_and_api_contract_worksession, docs_data_and_api_contract_task, docs_scheduler_spec_smart_shift, docs_scheduler_spec_dynamic_reschedule, docs_scheduler_spec_weighted_time_allocation [EXTRACTED 1.00]
- **Agenda pada kalender lokal** — docs_data_and_api_contract_calendarevent, docs_data_and_api_contract_worksession, docs_data_and_api_contract_fixedschedule, docs_data_and_api_contract_task [EXTRACTED 1.00]
- **Persistensi status, progres, dan sisa waktu saat jeda** — docs_scheduler_spec_paused, docs_scheduler_spec_jeda_dan_lanjutkan, docs_data_and_api_contract_worksession_pausedremainingms, docs_data_and_api_contract_worksession_pausedfromstate, docs_data_and_api_contract_worksession_workbeforesegmentms, docs_data_and_api_contract_worksession_continuousworkbeforesegmentms [INFERRED 0.95]

## Communities (115 total, 62 thin omitted)

### Community 0 - "SchedulerTest"
Cohesion: 0.10
Nodes (16): continuousWorkMs(), focusAvailableMs(), focusDurationMs(), remainingAfterInterruption(), FocusPreset, DeadlineRisk, PlannedWork, SchedulePlan (+8 more)

### Community 1 - "handler.ts"
Cohesion: 0.06
Nodes (56): Analysis, analysisOutput(), analysisSchema, analysisSubtasks(), integerBetween(), isUtcTimestamp(), object(), TaskInput (+48 more)

### Community 2 - "TazerViewModel"
Cohesion: 0.06
Nodes (14): AccountSettings(), ByokForm(), LoginScreen(), SecretField(), AnalyzerMode, BYOK, CLOUD, OFFLINE (+6 more)

### Community 3 - "TaskRepository"
Cohesion: 0.09
Nodes (7): FixedScheduleKind, BREAK, ROUTINE, FocusChange, TaskRepository, FocusClock, FocusMoment

### Community 4 - "CloudSync"
Cohesion: 0.06
Nodes (25): CloudSyncTest, Remote, CloudSync, Change, SyncResult, SyncCursor, SyncDevice, SyncEntry (+17 more)

### Community 5 - "StatisticsScreen"
Cohesion: 0.09
Nodes (25): PaceCalibration, PaceSample, civilDate(), CourseFocus, DailyFocus, EstimateAccuracy, onTimeStreak(), StatisticsSession (+17 more)

### Community 9 - "AndroidIntegration"
Cohesion: 0.21
Nodes (6): AndroidIntegration, Session, Tokens, fail(), json(), string()

### Community 10 - "CalendarScreens.kt"
Cohesion: 0.10
Nodes (12): AgendaItem, AgendaPill(), CalendarScreen(), CalendarWeek(), EventDate(), EventEditor(), MonthPicker(), FloatingTimerDeck() (+4 more)

### Community 11 - "ScheduleScreens.kt"
Cohesion: 0.09
Nodes (13): DateSpinner(), dayName(), FixedEditor(), ProfileForm(), ScheduleTag(), SettingsScreen(), TimeField(), TimelineEntry() (+5 more)

### Community 16 - "FocusEvent"
Cohesion: 0.12
Nodes (10): FocusAlarm, FocusAlarms, FocusEvent, FocusEventKind, ACTIVE, CUTOFF, DONE, EXTEND (+2 more)

### Community 17 - "FocusWidget.kt"
Cohesion: 0.15
Nodes (6): TazerThemeMode, DARK, LIGHT, SYSTEM, ThemePreference, FocusWidget

### Community 18 - "ByokVault.kt"
Cohesion: 0.10
Nodes (3): isAuthCallback(), credentialStorage(), SessionVault

### Community 19 - "IcsParser"
Cohesion: 0.19
Nodes (11): IcsCalendar, IcsItem, Event, WeeklyBlock, IcsParser, EventLength, EventTime, Property (+3 more)

### Community 22 - "MainActivity"
Cohesion: 0.10
Nodes (4): networkAvailability(), NetworkCallback, MainActivity, Factory

### Community 23 - "WidgetContent.kt"
Cohesion: 0.17
Nodes (12): FocusWidgetTest, FocusLink, FocusLinkKind, ACTIVE, EXTEND, REMINDER, activeWidget(), link() (+4 more)

### Community 24 - "IntegrationHttp.kt"
Cohesion: 0.17
Nodes (12): analyzeGemini(), analyzeGroq(), analyzeWithProvider(), integerSchema(), schema(), validateByok(), backendJson(), call() (+4 more)

### Community 26 - "Spesifikasi scheduler dan timer"
Cohesion: 0.11
Nodes (23): Android :app, Android data layer, Android timer layer, APK Debug, com.tazer.taskorganizer, Compose UI and ViewModel, Impeccable, Kotlin domain (+15 more)

### Community 27 - "Supabase Auth"
Cohesion: 0.14
Nodes (22): Backend, Server secret store, Analyzer input validation, Entitlement akun dan kuota bulanan, Gemini API Free, GET /auth/session, GET /subscription, Groq Free (+14 more)

### Community 29 - "IntegrationError"
Cohesion: 0.09
Nodes (21): IntegrationError, AI_UNAVAILABLE, AUTH_REJECTED, BROWSER_UNAVAILABLE, BYOK_KEY_MISSING, BYOK_REQUIRED, BYOK_STORAGE_UNAVAILABLE, EMAIL_UNVERIFIED (+13 more)

### Community 30 - "Room schema"
Cohesion: 0.18
Nodes (22): CalendarEvent, CalendarEvent(ownerUid,startEpochDay), CalendarEvent.startEpochDay dan endEpochDay, CalendarEvent.startMinute, FixedSchedule, Kalender lokal, Migrasi Room v1 ke v2, Room schema (+14 more)

### Community 33 - "PendingAuth"
Cohesion: 0.18
Nodes (8): AuthFlow, GOOGLE, RECOVERY, SIGN_UP, AuthPkce, PendingAuth, AuthPkceTest, Google OAuth browser PKCE with state and cancellation

### Community 34 - "TazerViewModel.kt"
Cohesion: 0.12
Nodes (9): AnalysisSource, AI, OFFLINE, SubtaskDraft, ChecklistItem, checklistItem(), AnalysisSuggestion, asDraft() (+1 more)

### Community 35 - "DateTimeButtons.kt"
Cohesion: 0.14
Nodes (5): NumberWheel(), PickerDateButton(), PickerTimeButton(), platformTheme(), TimeSpinner()

### Community 36 - "Signed APK Release"
Cohesion: 0.12
Nodes (19): APK sideload, GitHub Releases gratis, Node.js + TypeScript, Rp 0 free tier, Signed APK Release, Cloudflare Workers Free, Web Fetch API, AlarmManager.setExactAndAllowWhileIdle (+11 more)

### Community 37 - "IntegrationModels.kt"
Cohesion: 0.16
Nodes (15): Account, analysis(), analysisSubtasks(), AuthCallback, PasswordRecovery, SignedIn, integer(), Subscription (+7 more)

### Community 38 - "CalendarRepository.kt"
Cohesion: 0.14
Nodes (4): deleteEvent(), IcsImportPreview, observeEvents(), previewIcs()

### Community 39 - "Uri"
Cohesion: 0.17
Nodes (6): parseReleaseUpdate(), ReleaseUpdate, SemVer, ReleaseUpdateSetting(), ReleaseUpdatesTest, GitHub Releases SemVer checks and signed APK distribution

### Community 40 - "FocusScreens.kt"
Cohesion: 0.22
Nodes (9): DeferFocusDialog(), ExtensionDialog(), focusClock(), FocusPermissionsDialog(), FocusRhythm(), FocusScreen(), formatTimer(), isOvertime() (+1 more)

### Community 41 - "Aturan global"
Cohesion: 0.16
Nodes (12): Aturan global, Batas modul dan struktur target, Graphify, Handoff `i-have-adhd`, i-have-adhd handoff, Pemeriksaan sebelum handoff, Ponytail, Tazer (Task Organizer) (+4 more)

### Community 42 - "WorkSession"
Cohesion: 0.34
Nodes (8): CalendarEvent, FixedSchedule, Subtask, Task, UserProfile, WorkSession, TazerDatabase, Room

### Community 43 - "OfflineAnalyzerTest"
Cohesion: 0.29
Nodes (3): OfflineAnalysis, OfflineAnalyzer, OfflineAnalyzerTest

### Community 44 - "TaskScreens.kt"
Cohesion: 0.19
Nodes (6): formatDate(), schedulingTask(), TaskAction(), TaskEditor(), TasksScreen(), TaskTag()

### Community 45 - "ThemePreference.kt"
Cohesion: 0.14
Nodes (3): isDark(), ThemeSetting(), ThemeSystemBars()

### Community 46 - "Rencana eksekusi Tazer"
Cohesion: 0.13
Nodes (17): account_entitlements, analysis_usage, Android Keystore AES-GCM, Atomic quota RPC, BYOK, FREE tier, Kuota bulanan UTC, PostgreSQL (+9 more)

### Community 47 - "SubtaskControls.kt"
Cohesion: 0.17
Nodes (3): SubtaskEditor(), SubtaskProgress(), SubtaskTitleEditor()

### Community 48 - "package.json"
Cohesion: 0.14
Nodes (15): devDependencies, @types/node@^22.0.0, typescript@~5.9.3, engines, node, name, private, scripts (+7 more)

### Community 49 - "AndroidIntegrationTest"
Cohesion: 0.24
Nodes (6): AndroidIntegrationTest, ByokCredential, ByokProvider, GEMINI, GROQ, ByokSettings

### Community 51 - "AccountUid"
Cohesion: 0.31
Nodes (3): ByokVault, CredentialCipher, AccountUid

### Community 52 - "Focus timer state machine"
Cohesion: 0.30
Nodes (15): Belum Selesai and Selesai tabs, OVERDUE display status, Task.status, CANCELLED session, DONE task, EXTENDED session, Focus timer state machine, Hentikan sesi dijeda (+7 more)

### Community 53 - "tazer-v3.sql"
Cohesion: 0.25
Nodes (13): `CalendarEvent`, `FixedSchedule`, IF, index_CalendarEvent_ownerUid_startEpochDay, index_FixedSchedule_ownerUid_dayOfWeek, index_Task_id_ownerUid, index_Task_ownerUid_status_completedAt, index_WorkSession_ownerUid_state_plannedStartAt (+5 more)

### Community 54 - "TazerGlyph"
Cohesion: 0.14
Nodes (14): TazerGlyph, Add, Calendar, Check, Delete, Down, Edit, Next (+6 more)

### Community 55 - "compilerOptions"
Cohesion: 0.14
Nodes (13): compilerOptions, lib, module, noUncheckedIndexedAccess, noUnusedLocals, noUnusedParameters, outDir, rootDir (+5 more)

### Community 56 - "WorkSession"
Cohesion: 0.23
Nodes (14): Migrasi Room v2 ke v3, Room v3, WorkSession, WorkSession.continuousWorkBeforeSegmentMs, WorkSession.lastObservedAt, WorkSession.pausedFromState, WorkSession.pausedRemainingMs, WorkSession.state (+6 more)

### Community 57 - "SubtaskRepository"
Cohesion: 0.24
Nodes (4): SubtaskRepository, Optional analyzer subtasks negotiation, Room v4 migration and sync journal, Task checklist: Subtask with composite owner foreign key

### Community 59 - "20261125000000_entitlement.sql"
Cohesion: 0.24
Nodes (8): Android client, Supabase and providers, Tazer backend, public.account_entitlements, public.analysis_usage, public.tazer_entitlement(), public.tazer_entitlement(), tazer-db

### Community 60 - "tazer-v1.sql"
Cohesion: 0.29
Nodes (11): `FixedSchedule`, IF, index_FixedSchedule_ownerUid_dayOfWeek, index_Task_id_ownerUid, index_Task_ownerUid_status_completedAt, index_WorkSession_ownerUid_state_plannedStartAt, index_WorkSession_taskId_ownerUid, room_master_table (+3 more)

### Community 62 - "focusDndAction"
Cohesion: 0.27
Nodes (6): FocusDndAction, focusDndAction(), INSTALL, KEEP, WITHDRAW, FocusDndStateTest

### Community 64 - "public.tazer_sync_pull"
Cohesion: 0.22
Nodes (5): public, public.%I, public.tazer_sync_pull(), public.tazer_sync_push(), auth.users

### Community 65 - "WorkSessionState"
Cohesion: 0.22
Nodes (8): WorkSessionState, CANCELLED, DONE, EXTENDED, OVERTIME, PAUSED, PENDING, RUNNING

### Community 66 - "Autentikasi dan endpoint HTTP"
Cohesion: 0.22
Nodes (9): Analyzer, Autentikasi dan endpoint HTTP, Envelope error, GEMINI_MODEL, GET /health, Health readiness (ready), Kontrak data dan API, SUPABASE_PUBLISHABLE_KEY (+1 more)

### Community 68 - "HTTP error envelope"
Cohesion: 0.29
Nodes (7): AI_UNAVAILABLE, EMAIL_UNVERIFIED, HTTP error envelope, INVALID_AUTH, PRO_REQUIRED, QUOTA_EXHAUSTED, VERIFICATION_UNAVAILABLE

### Community 69 - "FocusPresetMode"
Cohesion: 0.40
Nodes (4): FocusPresetMode, CUSTOM, DEEP_WORK, POMODORO

### Community 70 - "TaskStatus"
Cohesion: 0.50
Nodes (4): TaskStatus, DONE, IN_PROGRESS, PENDING

## Knowledge Gaps
- **114 isolated node(s):** `PasswordRecovery`, `BREAK`, `ROUTINE`, `BYOK`, `CLOUD` (+109 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 392 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **62 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.