# Graph Report - Project-B  (2026-11-26)

## Corpus Check
- 114 files · ~77,718 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 24 file(s) not represented in the graph (top: .xml 17, .properties 2, (none) 1)

## Summary
- 1982 nodes · 5569 edges · 137 communities (63 shown, 74 thin omitted)
- Extraction: 94% EXTRACTED · 6% INFERRED · 0% AMBIGUOUS · INFERRED: 352 edges (avg confidence: 0.86)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `c1c83b16`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- SchedulerTest
- handler.ts
- TazerViewModel
- TaskRepository
- CloudSync
- WeeklyStatistics.kt
- ScanImageButton.kt
- FocusRuntime
- TaskRepositoryTest
- Spesifikasi scheduler dan timer
- ScheduleScreens.kt
- StatisticsScreen.kt
- ReleaseUiTest
- FocusService.kt
- SyncConflictTest.kt
- FocusEvent
- WidgetContent.kt
- ByokVault.kt
- IcsParser.kt
- TazerDao
- .enter
- TazerApp.kt
- AndroidIntegrationTest
- TaskScreens.kt
- Claude 5.5 model mapping
- Supabase Auth
- DailyBriefingAlarms.kt
- IntegrationError
- WorkSession
- .analyze
- FocusDnd.kt
- PendingAuth
- TazerViewModel.kt
- DateTimeButtons.kt
- Signed APK Release
- IntegrationModels.kt
- DailyBriefingAlarms.kt
- parseReleaseUpdate
- FocusScreens.kt
- Claude 5.5 model mapping
- WorkSession
- OfflineAnalyzerTest
- ReleaseUpdateSetting.kt
- FocusNotifications
- .change
- package.json
- CloudSyncTest.kt
- CalendarDao
- AccountUid
- Focus timer state machine
- tazer-v3.sql
- TazerGlyph
- compilerOptions
- ThemePreference.kt
- TazerDatabase
- IcsParserTest
- 20261125000000_entitlement.sql
- tazer-v1.sql
- SubtaskRepository
- FocusAlarms
- SubtaskControls.kt
- public.tazer_sync_pull
- .focusAction
- Autentikasi dan endpoint HTTP
- FocusNotificationSoundTest.kt
- HTTP error envelope
- ImageScan.kt
- image.ts
- TazerState
- Spesifikasi scheduler dan timer
- DailyBriefingTest
- WorkSessionState
- node.ts
- analysis.ts
- MainActivity.kt
- Per-account focus preset controls planning and breaks
- FocusRuntime.kt
- AndroidIntegrationTest.kt
- WidgetContent.kt
- providers.ts
- CalendarScreens.kt
- supabase.ts
- focusDndAction
- FocusDndTest.kt
- FocusEventKind
- adapters.test.ts
- CalendarRepository

## God Nodes (most connected - your core abstractions)
1. `TazerViewModel` - 88 edges
2. `TaskRepository` - 75 edges
3. `FocusRuntime` - 70 edges
4. `FocusMoment` - 62 edges
5. `fail()` - 51 edges
6. `AndroidIntegration` - 49 edges
7. `ReleaseUiTest` - 43 edges
8. `TaskRepositoryTest` - 42 edges
9. `WorkSession` - 40 edges
10. `TazerDatabase` - 35 edges

## Surprising Connections (you probably didn't know these)
- `Slot kosong dan batas sesi` --references--> `FixedSchedule`  [INFERRED]
  docs/SCHEDULER_SPEC.md → android/app/src/main/java/com/tazer/taskorganizer/data/Entities.kt
- `Optional analyzer subtasks negotiation` --references--> `AndroidIntegration`  [INFERRED]
  docs/DATA_AND_API_CONTRACT.md → android/app/src/main/java/com/tazer/taskorganizer/integration/AndroidIntegration.kt
- `Per-account focus preset controls planning and breaks` --references--> `FocusPreset`  [INFERRED]
  docs/SCHEDULER_SPEC.md → android/app/src/main/java/com/tazer/taskorganizer/domain/FocusPreset.kt
- `Per-account focus preset controls planning and breaks` --references--> `Scheduler`  [INFERRED]
  docs/SCHEDULER_SPEC.md → android/app/src/main/java/com/tazer/taskorganizer/domain/Scheduler.kt
- `OpenDocument iCalendar import and Room journal` --references--> `IcsParser`  [INFERRED]
  docs/DATA_AND_API_CONTRACT.md → android/app/src/main/java/com/tazer/taskorganizer/domain/IcsParser.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Transactional work history and replanning** — agents_room_repository, docs_data_and_api_contract_worksession, docs_data_and_api_contract_task, docs_scheduler_spec_smart_shift, docs_scheduler_spec_dynamic_reschedule, docs_scheduler_spec_weighted_time_allocation [EXTRACTED 1.00]
- **Agenda pada kalender lokal** — docs_data_and_api_contract_calendarevent, docs_data_and_api_contract_worksession, docs_data_and_api_contract_fixedschedule, docs_data_and_api_contract_task [EXTRACTED 1.00]
- **Persistensi status, progres, dan sisa waktu saat jeda** — docs_scheduler_spec_paused, docs_scheduler_spec_jeda_dan_lanjutkan, docs_data_and_api_contract_worksession_pausedremainingms, docs_data_and_api_contract_worksession_pausedfromstate, docs_data_and_api_contract_worksession_workbeforesegmentms, docs_data_and_api_contract_worksession_continuousworkbeforesegmentms [INFERRED 0.95]

## Communities (137 total, 74 thin omitted)

### Community 1 - "handler.ts"
Cohesion: 0.13
Nodes (17): TaskInput, createHandler(), Dependencies, error(), errors, json(), Readiness, readJson() (+9 more)

### Community 3 - "TaskRepository"
Cohesion: 0.06
Nodes (21): FixedScheduleKind, BREAK, ROUTINE, FocusChange, TaskRepository, continuousWorkMs(), focusAvailableMs(), FocusClock (+13 more)

### Community 4 - "CloudSync"
Cohesion: 0.10
Nodes (13): CloudSync, Change, SyncResult, SyncKind, EVENT, FIXED, PROFILE, SESSION (+5 more)

### Community 5 - "WeeklyStatistics.kt"
Cohesion: 0.12
Nodes (17): PaceCalibration, PaceSample, civilDate(), CourseFocus, DailyFocus, EstimateAccuracy, onTimeStreak(), StatisticsSession (+9 more)

### Community 6 - "ScanImageButton.kt"
Cohesion: 0.17
Nodes (5): captureUri(), scanFailureMessage(), ScanImageButton(), ScanReview(), TaskEditor()

### Community 7 - "FocusRuntime"
Cohesion: 0.12
Nodes (4): FocusAlarm, FocusRuntime, FocusSettings, FocusPermissionsDialog()

### Community 9 - "Spesifikasi scheduler dan timer"
Cohesion: 0.18
Nodes (6): AndroidIntegration, Session, Tokens, Account, fail(), string()

### Community 10 - "ScheduleScreens.kt"
Cohesion: 0.11
Nodes (13): DateSpinner(), dayName(), FixedEditor(), ProfileForm(), ScheduleTag(), SettingsScreen(), TimeField(), TimelineEntry() (+5 more)

### Community 12 - "StatisticsScreen.kt"
Cohesion: 0.15
Nodes (7): DailyFocusChart(), duration(), Statistic(), StatisticsHeading(), StatisticsScreen(), toMinutes(), localDate()

### Community 17 - "WidgetContent.kt"
Cohesion: 0.14
Nodes (8): TazerThemeMode, DARK, LIGHT, SYSTEM, ThemePreference, FocusWidget, Portable WorkSession history and local monotonic timer, Home screen widget follows selected account and focus state

### Community 19 - "IcsParser.kt"
Cohesion: 0.10
Nodes (15): IcsImportTest, deleteEvent(), IcsImportCounts, IcsImportPreview, observeEvents(), previewIcs(), IcsCalendar, IcsItem (+7 more)

### Community 23 - "TazerApp.kt"
Cohesion: 0.11
Nodes (5): FloatingTimerDeck(), TazerApp(), TazerIcon(), TimerIconButton(), WorkspaceScreen()

### Community 24 - "AndroidIntegrationTest"
Cohesion: 0.17
Nodes (13): analyzeGemini(), analyzeGroq(), analyzeWithProvider(), geminiText(), integerSchema(), schema(), validateByok(), backendJson() (+5 more)

### Community 25 - "TaskScreens.kt"
Cohesion: 0.11
Nodes (5): formatDate(), schedulingTask(), TaskAction(), TasksScreen(), TaskTag()

### Community 26 - "Claude 5.5 model mapping"
Cohesion: 0.14
Nodes (20): Android :app, APK Debug, Claude 5.5 model mapping, com.tazer.taskorganizer, Compose UI and ViewModel, Graphify, Impeccable, Room repository (+12 more)

### Community 27 - "Supabase Auth"
Cohesion: 0.09
Nodes (36): Android data layer, Backend, Kotlin domain, Server secret store, account_entitlements, analysis_usage, Analyzer input validation, Android Keystore AES-GCM (+28 more)

### Community 28 - "DailyBriefingAlarms.kt"
Cohesion: 0.16
Nodes (11): BriefingKind, EVENING, MORNING, BriefingMessage, BriefingSession, BriefingSettings, BriefingTask, morning() (+3 more)

### Community 29 - "IntegrationError"
Cohesion: 0.09
Nodes (23): IntegrationError, AI_UNAVAILABLE, AUTH_REJECTED, BROWSER_UNAVAILABLE, BYOK_GEMINI_REQUIRED, BYOK_KEY_MISSING, BYOK_REQUIRED, BYOK_STORAGE_UNAVAILABLE (+15 more)

### Community 30 - "WorkSession"
Cohesion: 0.14
Nodes (31): CalendarEvent, CalendarEvent(ownerUid,startEpochDay), CalendarEvent.startEpochDay dan endEpochDay, CalendarEvent.startMinute, FixedSchedule, Kalender lokal, Migrasi Room v1 ke v2, Migrasi Room v2 ke v3 (+23 more)

### Community 31 - ".analyze"
Cohesion: 0.22
Nodes (4): AnalyzerMode, BYOK, CLOUD, OFFLINE

### Community 33 - "PendingAuth"
Cohesion: 0.14
Nodes (9): AuthFlow, GOOGLE, RECOVERY, SIGN_UP, AuthPkce, isAuthCallback(), PendingAuth, AuthPkceTest (+1 more)

### Community 34 - "TazerViewModel.kt"
Cohesion: 0.11
Nodes (9): AnalysisSource, AI, OFFLINE, SubtaskDraft, IntegrationException, AnalysisSuggestion, asDraft(), ChecklistChange (+1 more)

### Community 35 - "DateTimeButtons.kt"
Cohesion: 0.14
Nodes (5): NumberWheel(), PickerDateButton(), PickerTimeButton(), platformTheme(), TimeSpinner()

### Community 36 - "Signed APK Release"
Cohesion: 0.11
Nodes (20): APK sideload, GitHub Releases gratis, Node.js + TypeScript, Rp 0 free tier, Signed APK Release, Cloudflare Workers Free, Web Fetch API, apksigner verify (+12 more)

### Community 37 - "IntegrationModels.kt"
Cohesion: 0.13
Nodes (18): analysis(), analysisSubtasks(), AuthCallback, PasswordRecovery, SignedIn, ByokProvider, GEMINI, GROQ (+10 more)

### Community 39 - "parseReleaseUpdate"
Cohesion: 0.18
Nodes (6): parseReleaseUpdate(), ReleaseUpdate, SemVer, ReleaseUpdateSetting(), ReleaseUpdatesTest, GitHub Releases SemVer checks and signed APK distribution

### Community 40 - "FocusScreens.kt"
Cohesion: 0.23
Nodes (8): DeferFocusDialog(), ExtensionDialog(), focusClock(), FocusRhythm(), FocusScreen(), formatTimer(), isOvertime(), timerText()

### Community 41 - "Claude 5.5 model mapping"
Cohesion: 0.10
Nodes (20): Aturan global, Batas modul dan struktur target, Handoff `i-have-adhd`, i-have-adhd handoff, Pemeriksaan sebelum handoff, Ponytail, Tazer (Task Organizer), Tim Orkestra Claude 5.5 (+12 more)

### Community 42 - "WorkSession"
Cohesion: 0.20
Nodes (12): CalendarRepositoryTest, CalendarEvent, FixedSchedule, Subtask, Task, TaskStatus, DONE, IN_PROGRESS (+4 more)

### Community 43 - "OfflineAnalyzerTest"
Cohesion: 0.33
Nodes (3): OfflineAnalysis, OfflineAnalyzer, OfflineAnalyzerTest

### Community 44 - "ReleaseUpdateSetting.kt"
Cohesion: 0.09
Nodes (3): BriefingRow(), DailyBriefingSetting(), ImportIcsButton()

### Community 48 - "package.json"
Cohesion: 0.14
Nodes (15): devDependencies, @types/node@^22.0.0, typescript@~5.9.3, engines, node, name, private, scripts (+7 more)

### Community 49 - "CloudSyncTest.kt"
Cohesion: 0.28
Nodes (3): CloudSyncTest, Remote, versionWins()

### Community 51 - "AccountUid"
Cohesion: 0.25
Nodes (5): ByokCredential, ByokVault, CredentialCipher, AccountUid, ByokSettings

### Community 52 - "Focus timer state machine"
Cohesion: 0.30
Nodes (15): Belum Selesai and Selesai tabs, OVERDUE display status, Task.status, CANCELLED session, DONE task, EXTENDED session, Focus timer state machine, Hentikan sesi dijeda (+7 more)

### Community 53 - "tazer-v3.sql"
Cohesion: 0.25
Nodes (13): `CalendarEvent`, `FixedSchedule`, IF, index_CalendarEvent_ownerUid_startEpochDay, index_FixedSchedule_ownerUid_dayOfWeek, index_Task_id_ownerUid, index_Task_ownerUid_status_completedAt, index_WorkSession_ownerUid_state_plannedStartAt (+5 more)

### Community 54 - "TazerGlyph"
Cohesion: 0.12
Nodes (16): TazerGlyph, Add, Calendar, Camera, Check, Delete, Down, Edit (+8 more)

### Community 55 - "compilerOptions"
Cohesion: 0.14
Nodes (13): compilerOptions, lib, module, noUncheckedIndexedAccess, noUnusedLocals, noUnusedParameters, outDir, rootDir (+5 more)

### Community 56 - "ThemePreference.kt"
Cohesion: 0.14
Nodes (3): isDark(), ThemeSetting(), ThemeSystemBars()

### Community 57 - "TazerDatabase"
Cohesion: 0.13
Nodes (9): SyncCursor, SyncDevice, SyncEntry, SyncJournal, TazerDatabase, Callback, Optional analyzer subtasks negotiation, Room v4 migration and sync journal (+1 more)

### Community 59 - "20261125000000_entitlement.sql"
Cohesion: 0.24
Nodes (8): Android client, Supabase and providers, Tazer backend, public.account_entitlements, public.analysis_usage, public.tazer_entitlement(), public.tazer_entitlement(), tazer-db

### Community 60 - "tazer-v1.sql"
Cohesion: 0.29
Nodes (11): `FixedSchedule`, IF, index_FixedSchedule_ownerUid_dayOfWeek, index_Task_id_ownerUid, index_Task_ownerUid_status_completedAt, index_WorkSession_ownerUid_state_plannedStartAt, index_WorkSession_taskId_ownerUid, room_master_table (+3 more)

### Community 63 - "SubtaskControls.kt"
Cohesion: 0.18
Nodes (5): ChecklistItem, checklistItem(), SubtaskEditor(), SubtaskProgress(), SubtaskTitleEditor()

### Community 64 - "public.tazer_sync_pull"
Cohesion: 0.22
Nodes (5): public, public.%I, public.tazer_sync_pull(), public.tazer_sync_push(), auth.users

### Community 66 - "Autentikasi dan endpoint HTTP"
Cohesion: 0.25
Nodes (8): Analyzer, Autentikasi dan endpoint HTTP, Envelope error, GEMINI_MODEL, GET /health, Health readiness (ready), SUPABASE_PUBLISHABLE_KEY, SUPABASE_URL

### Community 68 - "HTTP error envelope"
Cohesion: 0.29
Nodes (7): AI_UNAVAILABLE, EMAIL_UNVERIFIED, HTTP error envelope, INVALID_AUTH, PRO_REQUIRED, QUOTA_EXHAUSTED, VERIFICATION_UNAVAILABLE

### Community 69 - "ImageScan.kt"
Cohesion: 0.12
Nodes (12): compressForScan(), extractWithGemini(), ImageExtraction, imageExtraction(), scanBody(), scanInstructions(), scanSampleSize(), scanSchema() (+4 more)

### Community 70 - "image.ts"
Cohesion: 0.13
Nodes (19): calendarDate(), decodedSize(), ImageExtraction, imageExtractionOutput(), imageExtractionSchema, imageInput(), ImageMimeType, imageMimeTypes (+11 more)

### Community 73 - "TazerState"
Cohesion: 0.44
Nodes (5): AccountSettings(), ByokForm(), LoginScreen(), SecretField(), TazerState

### Community 76 - "Spesifikasi scheduler dan timer"
Cohesion: 0.20
Nodes (12): Android timer layer, Analisis offline, Android timer dan pemulihan, Durasi fokus penuh, elapsedRealtime, ForegroundService, Jeda dan lanjutkan, Pemulihan timer setelah reboot (+4 more)

### Community 100 - "WorkSessionState"
Cohesion: 0.25
Nodes (8): WorkSessionState, CANCELLED, DONE, EXTENDED, OVERTIME, PAUSED, PENDING, RUNNING

### Community 105 - "node.ts"
Cohesion: 0.18
Nodes (10): Env, mockDependencies(), bridgeError(), createNodeServer(), loopback, NodeEnv, requestBody(), listen() (+2 more)

### Community 108 - "analysis.ts"
Cohesion: 0.43
Nodes (7): Analysis, analysisOutput(), analysisSubtasks(), integerBetween(), isUtcTimestamp(), object(), taskInput()

### Community 115 - "MainActivity.kt"
Cohesion: 0.09
Nodes (4): networkAvailability(), NetworkCallback, MainActivity, Factory

### Community 116 - "Per-account focus preset controls planning and breaks"
Cohesion: 1.00
Nodes (3): Scoped DND rule and rollback on leaving a countdown, Release 1.4.0 gate and signed upgrade, Per-account focus preset controls planning and breaks

### Community 117 - "FocusRuntime.kt"
Cohesion: 0.12
Nodes (4): FocusPresetMode, CUSTOM, DEEP_WORK, POMODORO

### Community 119 - "WidgetContent.kt"
Cohesion: 0.15
Nodes (12): FocusWidgetTest, FocusLink, FocusLinkKind, ACTIVE, EXTEND, REMINDER, activeWidget(), link() (+4 more)

### Community 120 - "providers.ts"
Cohesion: 0.27
Nodes (12): imageInstructions(), geminiConfig(), geminiOutput(), geminiSubtaskSchema, groqOutput(), legacySchema, providerAnalyzer(), providerImageExtractor() (+4 more)

### Community 121 - "CalendarScreens.kt"
Cohesion: 0.12
Nodes (7): AgendaItem, AgendaPill(), CalendarScreen(), CalendarWeek(), EventDate(), EventEditor(), MonthPicker()

### Community 122 - "supabase.ts"
Cohesion: 0.28
Nodes (9): realHandler(), ProviderEnv, httpsOrigin(), invalidAuthCodes, supabase(), supabaseConfig(), SupabaseEnv, supabaseKey() (+1 more)

### Community 123 - "focusDndAction"
Cohesion: 0.27
Nodes (6): FocusDndAction, focusDndAction(), INSTALL, KEEP, WITHDRAW, FocusDndStateTest

### Community 125 - "FocusEventKind"
Cohesion: 0.29
Nodes (7): FocusEventKind, ACTIVE, CUTOFF, DONE, EXTEND, REMINDER, START_ALARM

### Community 126 - "adapters.test.ts"
Cohesion: 0.22
Nodes (7): analysisSchema, auth, entitlement, env, input, result, suggested

## Knowledge Gaps
- **122 isolated node(s):** `PasswordRecovery`, `ACTIVE`, `EXTEND`, `REMINDER`, `INSTALL` (+117 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 412 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **74 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `POST /extract-task-image` connect `Claude 5.5 model mapping` to `handler.ts`, `Signed APK Release`, `ImageScan.kt`, `image.ts`?**
  _High betweenness centrality (0.097) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `TaskRepository` (e.g. with `.acknowledgementCannotClearAnEditMadeDuringARequest()` and `.queuesOfflineChangesAcrossCloseAndReplaysSixEntityTypesAndTombstones()`) actually correct?**
  _`TaskRepository` has 2 INFERRED edges - model-reasoned connections that need verification._
- **What connects `PasswordRecovery`, `ACTIVE`, `EXTEND` to the rest of the system?**
  _122 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `handler.ts` be split into smaller, more focused modules?**
  _Cohesion score 0.13230769230769232 - nodes in this community are weakly interconnected._
- **Why does `FocusRuntime` connect `FocusRuntime` to `FocusDnd.kt`, `TazerViewModel.kt`, `TaskRepository`, `TazerViewModel`, `DailyBriefingAlarms.kt`, `FocusScreens.kt`, `ReleaseUiTest.kt`, `ReleaseUpdateSetting.kt`, `ReleaseUiTest`, `FocusNotifications`, `FocusService.kt`, `WidgetContent.kt`, `MainActivity.kt`, `Per-account focus preset controls planning and breaks`, `FocusRuntime.kt`, `TazerApp.kt`, `FocusAlarms`, `Intent`?**
  _High betweenness centrality (0.087) - this node is a cross-community bridge._
- **Are the 7 inferred relationships involving `FocusRuntime` (e.g. with `FocusAlarms` and `FocusDnd`) actually correct?**
  _`FocusRuntime` has 7 INFERRED edges - model-reasoned connections that need verification._
- **Should `TaskRepository` be split into smaller, more focused modules?**
  _Cohesion score 0.06071323312702623 - nodes in this community are weakly interconnected._