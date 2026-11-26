# Graph Report - Project-B  (2026-11-26)

## Corpus Check
- 128 files · ~87,055 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 24 file(s) not represented in the graph (top: .xml 17, .properties 2, (none) 1)

## Summary
- 2158 nodes · 6210 edges · 138 communities (64 shown, 74 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 414 edges (avg confidence: 0.86)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `c2506518`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- SchedulerTest
- handler.ts
- FocusPreset
- TaskRepository
- CloudSync
- WeeklyStatistics.kt
- TaskScreens.kt
- FocusRuntime
- TaskRepositoryTest
- Spesifikasi scheduler dan timer
- CalendarScreen
- .requireUid
- FocusScreens.kt
- ReleaseUiTest
- FocusService.kt
- CalendarRepositoryTest.kt
- FocusEvent
- WidgetContent.kt
- ByokVault.kt
- IcsParser
- instant
- TazerDao
- FocusClock
- AndroidIntegrationTest
- TasksScreen
- Spesifikasi scheduler dan timer
- Supabase Auth
- DailyBriefing.kt
- IntegrationError
- WorkSession
- .analyze
- FocusDnd.kt
- PendingAuth
- TazerViewModel.kt
- Signed APK Release
- IntegrationModels.kt
- FocusReceiver.kt
- parseReleaseUpdate
- FocusScreens.kt
- DATA_AND_API_CONTRACT.md
- WorkSession
- OfflineAnalyzerTest
- CourseChip
- FocusNotifications
- TazerBackup.kt
- .change
- package.json
- .queuesOfflineChangesAcrossCloseAndReplaysSixEntityTypesAndTombstones
- CourseColorUi.kt
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
- TaskRepositoryTest.kt
- public.tazer_sync_pull
- TazerViewModel
- Autentikasi dan endpoint HTTP
- .get
- HTTP error envelope
- ImageScan.kt
- image.ts
- TazerState
- TaskFilterBar.kt
- focusDndAction
- FocusWidgetTest.kt
- CalendarDao
- node.ts
- FocusAlarms
- Supabase Cloud Sync push and pull RPC
- Per-account focus preset controls planning and breaks
- ReleaseUiTest.kt
- FocusNotificationSoundTest.kt
- AndroidIntegrationTest.kt
- WidgetContent.kt
- providers.ts
- supabase.ts
- FocusDndTest.kt
- adapters.test.ts

## God Nodes (most connected - your core abstractions)
1. `TazerViewModel` - 100 edges
2. `TaskRepository` - 79 edges
3. `FocusRuntime` - 70 edges
4. `FocusMoment` - 62 edges
5. `ReleaseUiTest` - 51 edges
6. `fail()` - 51 edges
7. `AndroidIntegration` - 49 edges
8. `TaskRepositoryTest` - 42 edges
9. `WorkSession` - 40 edges
10. `TazerDatabase` - 36 edges

## Surprising Connections (you probably didn't know these)
- `Offline queue acknowledgement and cursor` --references--> `CloudSync`  [INFERRED]
  docs/DATA_AND_API_CONTRACT.md → android/app/src/main/java/com/tazer/taskorganizer/data/CloudSync.kt
- `Slot kosong dan batas sesi` --references--> `FixedSchedule`  [INFERRED]
  docs/SCHEDULER_SPEC.md → android/app/src/main/java/com/tazer/taskorganizer/data/Entities.kt
- `Personal pace median from completed work` --references--> `PaceCalibration`  [INFERRED]
  docs/SCHEDULER_SPEC.md → android/app/src/main/java/com/tazer/taskorganizer/domain/PaceCalibration.kt
- `Optional analyzer subtasks negotiation` --references--> `AndroidIntegration`  [INFERRED]
  docs/DATA_AND_API_CONTRACT.md → android/app/src/main/java/com/tazer/taskorganizer/integration/AndroidIntegration.kt
- `Home screen widget follows selected account and focus state` --references--> `FocusWidget`  [INFERRED]
  docs/SCHEDULER_SPEC.md → android/app/src/main/java/com/tazer/taskorganizer/widget/FocusWidget.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Transactional work history and replanning** — agents_room_repository, docs_data_and_api_contract_worksession, docs_data_and_api_contract_task, docs_scheduler_spec_smart_shift, docs_scheduler_spec_dynamic_reschedule, docs_scheduler_spec_weighted_time_allocation [EXTRACTED 1.00]
- **Agenda pada kalender lokal** — docs_data_and_api_contract_calendarevent, docs_data_and_api_contract_worksession, docs_data_and_api_contract_fixedschedule, docs_data_and_api_contract_task [EXTRACTED 1.00]
- **Persistensi status, progres, dan sisa waktu saat jeda** — docs_scheduler_spec_paused, docs_scheduler_spec_jeda_dan_lanjutkan, docs_data_and_api_contract_worksession_pausedremainingms, docs_data_and_api_contract_worksession_pausedfromstate, docs_data_and_api_contract_worksession_workbeforesegmentms, docs_data_and_api_contract_worksession_continuousworkbeforesegmentms [INFERRED 0.95]

## Communities (138 total, 74 thin omitted)

### Community 0 - "SchedulerTest"
Cohesion: 0.24
Nodes (3): DeadlineRisk, PlannedWork, SchedulerTest

### Community 1 - "handler.ts"
Cohesion: 0.14
Nodes (16): TaskInput, createHandler(), Dependencies, error(), errors, json(), Readiness, readJson() (+8 more)

### Community 2 - "FocusPreset"
Cohesion: 0.22
Nodes (10): continuousWorkMs(), focusAvailableMs(), FocusPreset, Scheduler, Option, Work, ScheduleRule, SchedulingTask (+2 more)

### Community 3 - "TaskRepository"
Cohesion: 0.20
Nodes (4): FocusChange, TaskRepository, FocusMoment, SchedulePlan

### Community 4 - "CloudSync"
Cohesion: 0.11
Nodes (10): CloudSync, Change, SyncResult, SyncKind, EVENT, FIXED, PROFILE, SESSION (+2 more)

### Community 5 - "WeeklyStatistics.kt"
Cohesion: 0.10
Nodes (21): PaceCalibration, PaceSample, civilDate(), CourseFocus, DailyFocus, EstimateAccuracy, onTimeStreak(), StatisticsSession (+13 more)

### Community 6 - "TaskScreens.kt"
Cohesion: 0.09
Nodes (3): ExportIcsButton(), readDocument(), writeDocument()

### Community 7 - "FocusRuntime"
Cohesion: 0.13
Nodes (3): FocusRuntime, FocusSettings, FocusPermissionsDialog()

### Community 8 - "TaskRepositoryTest"
Cohesion: 0.11
Nodes (5): TaskRepositoryTest, FocusPresetMode, CUSTOM, DEEP_WORK, POMODORO

### Community 9 - "Spesifikasi scheduler dan timer"
Cohesion: 0.18
Nodes (5): AndroidIntegration, Session, Tokens, Account, fail()

### Community 10 - "CalendarScreen"
Cohesion: 0.10
Nodes (27): AgendaItem, CalendarScreen(), CalendarWeek(), EventDate(), EventEditor(), MonthPicker(), ImportIcsButton(), DateSpinner() (+19 more)

### Community 11 - ".requireUid"
Cohesion: 0.11
Nodes (3): FixedScheduleKind, BREAK, ROUTINE

### Community 12 - "FocusScreens.kt"
Cohesion: 0.19
Nodes (11): NumberWheel(), PickerDateButton(), PickerTimeButton(), platformTheme(), TimeSpinner(), DeferFocusDialog(), ExtensionDialog(), focusClock() (+3 more)

### Community 13 - "ReleaseUiTest"
Cohesion: 0.17
Nodes (3): ReleaseUiTest, ActivityMonitor, ThemePreference

### Community 16 - "FocusEvent"
Cohesion: 0.13
Nodes (13): FocusEvent, FocusEventKind, ACTIVE, CUTOFF, DONE, EXTEND, REMINDER, START_ALARM (+5 more)

### Community 19 - "IcsParser"
Cohesion: 0.06
Nodes (18): deleteEvent(), IcsImportPreview, observeEvents(), previewIcs(), IcsCalendar, IcsItem, Event, WeeklyBlock (+10 more)

### Community 22 - "FocusClock"
Cohesion: 0.20
Nodes (4): FocusClock, focusDurationMs(), remainingAfterInterruption(), FocusTest

### Community 24 - "AndroidIntegrationTest"
Cohesion: 0.17
Nodes (13): analyzeGemini(), analyzeGroq(), analyzeWithProvider(), geminiText(), integerSchema(), schema(), validateByok(), backendJson() (+5 more)

### Community 25 - "TasksScreen"
Cohesion: 0.17
Nodes (11): scanFailureMessage(), ScanImageButton(), ScanReview(), SubtaskEditor(), SubtaskProgress(), SubtaskTitleEditor(), formatDate(), TaskAction() (+3 more)

### Community 26 - "Spesifikasi scheduler dan timer"
Cohesion: 0.10
Nodes (27): Android data layer, Android timer layer, APK Debug, Claude 5.5 model mapping, Compose UI and ViewModel, Impeccable, Kotlin domain, Room repository (+19 more)

### Community 27 - "Supabase Auth"
Cohesion: 0.09
Nodes (33): Backend, Server secret store, account_entitlements, analysis_usage, Analyzer input validation, Android Keystore AES-GCM, Atomic quota RPC, BYOK (+25 more)

### Community 28 - "DailyBriefing.kt"
Cohesion: 0.11
Nodes (15): BriefingKind, EVENING, MORNING, BriefingMessage, BriefingSession, BriefingSettings, BriefingTask, morning() (+7 more)

### Community 29 - "IntegrationError"
Cohesion: 0.08
Nodes (24): IntegrationError, AI_UNAVAILABLE, AUTH_REJECTED, BROWSER_UNAVAILABLE, BYOK_GEMINI_REQUIRED, BYOK_KEY_MISSING, BYOK_REQUIRED, BYOK_STORAGE_UNAVAILABLE (+16 more)

### Community 30 - "WorkSession"
Cohesion: 0.14
Nodes (31): CalendarEvent, CalendarEvent(ownerUid,startEpochDay), CalendarEvent.startEpochDay dan endEpochDay, CalendarEvent.startMinute, FixedSchedule, Kalender lokal, Migrasi Room v1 ke v2, Migrasi Room v2 ke v3 (+23 more)

### Community 31 - ".analyze"
Cohesion: 0.33
Nodes (4): AnalyzerMode, BYOK, CLOUD, OFFLINE

### Community 33 - "PendingAuth"
Cohesion: 0.14
Nodes (9): AuthFlow, GOOGLE, RECOVERY, SIGN_UP, AuthPkce, isAuthCallback(), PendingAuth, AuthPkceTest (+1 more)

### Community 34 - "TazerViewModel.kt"
Cohesion: 0.10
Nodes (10): SubtaskDraft, ChecklistItem, checklistItem(), AnalysisSuggestion, asDraft(), ChecklistChange, ScanSuggestion, TaskInput (+2 more)

### Community 36 - "Signed APK Release"
Cohesion: 0.11
Nodes (20): APK sideload, GitHub Releases gratis, Node.js + TypeScript, Rp 0 free tier, Signed APK Release, Cloudflare Workers Free, Web Fetch API, apksigner verify (+12 more)

### Community 37 - "IntegrationModels.kt"
Cohesion: 0.12
Nodes (19): analysis(), analysisSubtasks(), AuthCallback, PasswordRecovery, SignedIn, ByokProvider, GEMINI, GROQ (+11 more)

### Community 39 - "parseReleaseUpdate"
Cohesion: 0.06
Nodes (20): CourseColors, CourseSwatch, CourseTone, NoteBlock, Bullet, Heading, Paragraph, NoteSpan (+12 more)

### Community 40 - "FocusScreens.kt"
Cohesion: 0.12
Nodes (3): formatTimer(), timerText(), ImportPackageButton()

### Community 41 - "DATA_AND_API_CONTRACT.md"
Cohesion: 0.09
Nodes (25): Android :app, Aturan global, Batas modul dan struktur target, com.tazer.taskorganizer, Graphify, Handoff `i-have-adhd`, i-have-adhd handoff, Pemeriksaan sebelum handoff (+17 more)

### Community 42 - "WorkSession"
Cohesion: 0.21
Nodes (13): AnalysisSource, AI, OFFLINE, CalendarEvent, FixedSchedule, Subtask, Task, TaskStatus (+5 more)

### Community 43 - "OfflineAnalyzerTest"
Cohesion: 0.33
Nodes (3): OfflineAnalysis, OfflineAnalyzer, OfflineAnalyzerTest

### Community 44 - "CourseChip"
Cohesion: 0.28
Nodes (7): AgendaPill(), color(), CourseChip(), CourseColorSetting(), courseContainer(), CourseDot(), CoursePalette

### Community 46 - "TazerBackup.kt"
Cohesion: 0.06
Nodes (12): CourseColorStore, TaskPackage, TaskPackages, BackupCounts, BackupData, BackupPreview, BackupRepository, Plan (+4 more)

### Community 48 - "package.json"
Cohesion: 0.14
Nodes (15): devDependencies, @types/node@^22.0.0, typescript@~5.9.3, engines, node, name, private, scripts (+7 more)

### Community 49 - ".queuesOfflineChangesAcrossCloseAndReplaysSixEntityTypesAndTombstones"
Cohesion: 0.36
Nodes (3): CloudSyncTest, Remote, versionWins()

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
Cohesion: 0.11
Nodes (18): TazerGlyph, Add, Calendar, Camera, Check, Delete, Down, Edit (+10 more)

### Community 55 - "compilerOptions"
Cohesion: 0.14
Nodes (13): compilerOptions, lib, module, noUncheckedIndexedAccess, noUnusedLocals, noUnusedParameters, outDir, rootDir (+5 more)

### Community 56 - "ThemePreference.kt"
Cohesion: 0.16
Nodes (3): isDark(), ThemeSetting(), ThemeSystemBars()

### Community 57 - "TazerDatabase"
Cohesion: 0.12
Nodes (11): SyncCursor, SyncDevice, SyncEntry, SyncJournal, TazerDatabase, Callback, Optional analyzer subtasks negotiation, Cloud Sync Lamport version and writer tie breaker (+3 more)

### Community 59 - "20261125000000_entitlement.sql"
Cohesion: 0.24
Nodes (8): Android client, Supabase and providers, Tazer backend, public.account_entitlements, public.analysis_usage, public.tazer_entitlement(), public.tazer_entitlement(), tazer-db

### Community 60 - "tazer-v1.sql"
Cohesion: 0.29
Nodes (11): `FixedSchedule`, IF, index_FixedSchedule_ownerUid_dayOfWeek, index_Task_id_ownerUid, index_Task_ownerUid_status_completedAt, index_WorkSession_ownerUid_state_plannedStartAt, index_WorkSession_taskId_ownerUid, room_master_table (+3 more)

### Community 64 - "public.tazer_sync_pull"
Cohesion: 0.22
Nodes (5): public, public.%I, public.tazer_sync_pull(), public.tazer_sync_push(), auth.users

### Community 65 - "TazerViewModel"
Cohesion: 0.10
Nodes (3): FocusTarget, rejectsSession(), TazerViewModel

### Community 66 - "Autentikasi dan endpoint HTTP"
Cohesion: 0.22
Nodes (9): Analyzer, Autentikasi dan endpoint HTTP, Envelope error, GEMINI_MODEL, GET /health, Health readiness (ready), Kontrak data dan API, SUPABASE_PUBLISHABLE_KEY (+1 more)

### Community 68 - "HTTP error envelope"
Cohesion: 0.29
Nodes (7): AI_UNAVAILABLE, EMAIL_UNVERIFIED, HTTP error envelope, INVALID_AUTH, PRO_REQUIRED, QUOTA_EXHAUSTED, VERIFICATION_UNAVAILABLE

### Community 69 - "ImageScan.kt"
Cohesion: 0.20
Nodes (11): compressForScan(), extractWithGemini(), ImageExtraction, imageExtraction(), scanBody(), scanInstructions(), scanSampleSize(), scanSchema() (+3 more)

### Community 70 - "image.ts"
Cohesion: 0.12
Nodes (20): calendarDate(), decodedSize(), ImageExtraction, imageExtractionOutput(), imageInput(), ImageMimeType, imageMimeTypes, localDateTime() (+12 more)

### Community 73 - "TazerState"
Cohesion: 0.38
Nodes (5): AccountSettings(), ByokForm(), LoginScreen(), SecretField(), TazerState

### Community 76 - "TaskFilterBar.kt"
Cohesion: 0.16
Nodes (4): TaskQuery, FilteredEmpty(), TaskFilterBar(), Pencarian dan warna mata kuliah

### Community 92 - "focusDndAction"
Cohesion: 0.27
Nodes (6): FocusDndAction, focusDndAction(), INSTALL, KEEP, WITHDRAW, FocusDndStateTest

### Community 105 - "node.ts"
Cohesion: 0.18
Nodes (10): Env, mockDependencies(), bridgeError(), createNodeServer(), loopback, NodeEnv, requestBody(), listen() (+2 more)

### Community 110 - "Supabase Cloud Sync push and pull RPC"
Cohesion: 0.50
Nodes (5): Supabase Cloud Sync push and pull RPC, Portable WorkSession history and local monotonic timer, Release 1.3.0 verification gate, Personal pace median from completed work, Home screen widget follows selected account and focus state

### Community 113 - "Per-account focus preset controls planning and breaks"
Cohesion: 1.00
Nodes (3): Scoped DND rule and rollback on leaving a countdown, Release 1.4.0 gate and signed upgrade, Per-account focus preset controls planning and breaks

### Community 116 - "FocusNotificationSoundTest.kt"
Cohesion: 0.15
Nodes (9): FocusNotificationSoundTest, WorkSessionState, CANCELLED, DONE, EXTENDED, OVERTIME, PAUSED, PENDING (+1 more)

### Community 119 - "WidgetContent.kt"
Cohesion: 0.16
Nodes (11): FocusWidgetTest, TazerThemeMode, DARK, LIGHT, SYSTEM, activeWidget(), link(), loadWidgetSnapshot() (+3 more)

### Community 120 - "providers.ts"
Cohesion: 0.21
Nodes (15): Analysis, imageExtractionSchema, imageInstructions(), geminiConfig(), geminiOutput(), geminiSubtaskSchema, groqOutput(), legacySchema (+7 more)

### Community 122 - "supabase.ts"
Cohesion: 0.23
Nodes (14): analysisOutput(), analysisSubtasks(), integerBetween(), isUtcTimestamp(), object(), taskInput(), realHandler(), httpsOrigin() (+6 more)

### Community 126 - "adapters.test.ts"
Cohesion: 0.22
Nodes (7): analysisSchema, auth, entitlement, env, input, result, suggested

## Knowledge Gaps
- **124 isolated node(s):** `PasswordRecovery`, `CANCELLED`, `DONE`, `EXTENDED`, `OVERTIME` (+119 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 429 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **74 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `POST /extract-task-image` connect `image.ts` to `handler.ts`, `Signed APK Release`, `ImageScan.kt`, `DATA_AND_API_CONTRACT.md`?**
  _High betweenness centrality (0.100) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `TaskRepository` (e.g. with `.acknowledgementCannotClearAnEditMadeDuringARequest()` and `.queuesOfflineChangesAcrossCloseAndReplaysSixEntityTypesAndTombstones()`) actually correct?**
  _`TaskRepository` has 2 INFERRED edges - model-reasoned connections that need verification._
- **What connects `PasswordRecovery`, `CANCELLED`, `DONE` to the rest of the system?**
  _124 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `handler.ts` be split into smaller, more focused modules?**
  _Cohesion score 0.14 - nodes in this community are weakly interconnected._
- **Why does `FocusRuntime` connect `FocusRuntime` to `TaskRepository`, `CalendarScreen`, `FocusScreens.kt`, `ReleaseUiTest`, `FocusService.kt`, `WidgetContent.kt`, `TazerApp.kt`, `FocusDnd.kt`, `TazerViewModel.kt`, `FocusReceiver.kt`, `FocusScreens.kt`, `FocusNotifications`, `SubtaskControls.kt`, `TazerViewModel`, `.get`, `FocusWidgetTest.kt`, `FocusAlarms`, `Supabase Cloud Sync push and pull RPC`, `Per-account focus preset controls planning and breaks`, `ReleaseUiTest.kt`, `FocusRuntime.kt`?**
  _High betweenness centrality (0.076) - this node is a cross-community bridge._
- **Are the 7 inferred relationships involving `FocusRuntime` (e.g. with `FocusAlarms` and `FocusDnd`) actually correct?**
  _`FocusRuntime` has 7 INFERRED edges - model-reasoned connections that need verification._
- **Should `CloudSync` be split into smaller, more focused modules?**
  _Cohesion score 0.11051693404634581 - nodes in this community are weakly interconnected._