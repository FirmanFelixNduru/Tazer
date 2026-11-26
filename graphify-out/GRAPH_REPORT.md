# Graph Report - Project-B  (2026-11-26)

## Corpus Check
- 135 files · ~97,541 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 24 file(s) not represented in the graph (top: .xml 17, .properties 2, (none) 1)

## Summary
- 2256 nodes · 6568 edges · 158 communities (60 shown, 98 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 439 edges (avg confidence: 0.86)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `d790c412`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Spesifikasi scheduler dan timer
- handler.ts
- Rencana eksekusi Tazer
- TaskRepository
- SchedulePages.kt
- WeeklyStatistics.kt
- FocusRuntime
- TaskRepositoryTest
- Spesifikasi scheduler dan timer
- TazerState
- FocusScreens.kt
- ReleaseUiTest
- FocusService.kt
- StatisticsScreen.kt
- FocusEvent
- WidgetContent.kt
- SessionVault
- IcsParser
- WorkSessionState
- TazerDao
- SubtaskDraft
- TazerApp.kt
- AndroidIntegrationTest
- ScanImageButton.kt
- Supabase Auth
- FocusRuntime.kt
- IntegrationError
- WorkSession
- FocusWidgetTest
- FocusDnd.kt
- PendingAuth
- TazerViewModel.kt
- DailyBriefingSetting
- Signed APK Release
- IntegrationModels.kt
- XlsxAndScheduleScanTest.kt
- parseReleaseUpdate
- ScheduleTable
- DATA_AND_API_CONTRACT.md
- WorkSession
- OfflineAnalyzerTest
- CourseColorUi.kt
- FocusNotifications
- BackupAndPackageTest
- package.json
- ScheduleImportTest
- ScheduleDraft
- AccountUid
- Focus timer state machine
- tazer-v3.sql
- TazerGlyph
- compilerOptions
- ThemeSetting
- TazerDatabase
- IcsParserTest
- 20261125000000_entitlement.sql
- tazer-v1.sql
- SubtaskRepository
- XlsxReader.kt
- NetworkAvailability.kt
- public.tazer_sync_pull
- TazerViewModel
- Autentikasi dan endpoint HTTP
- FocusStartTest.kt
- HTTP error envelope
- ImageScan.kt
- image.ts
- FocusDndTest.kt
- Uri
- ScheduleScan.kt
- schedule.test.ts
- SyncConflictTest.kt
- node.ts
- build
- ReleaseUiTest.kt
- MainActivity.kt
- AndroidIntegrationTest.kt
- WidgetContent.kt
- providers.ts
- ScheduleScreens.kt
- supabase.ts
- MainActivity.kt

## God Nodes (most connected - your core abstractions)
1. `TazerViewModel` - 99 edges
2. `TaskRepository` - 80 edges
3. `FocusRuntime` - 70 edges
4. `WorkSession` - 68 edges
5. `FocusMoment` - 62 edges
6. `ReleaseUiTest` - 56 edges
7. `fail()` - 54 edges
8. `AndroidIntegration` - 52 edges
9. `TaskRepositoryTest` - 42 edges
10. `TazerDatabase` - 36 edges

## Surprising Connections (you probably didn't know these)
- `Slot kosong dan batas sesi` --references--> `FixedSchedule`  [INFERRED]
  docs/SCHEDULER_SPEC.md → android/app/src/main/java/com/tazer/taskorganizer/data/Entities.kt
- `Personal pace median from completed work` --references--> `PaceCalibration`  [INFERRED]
  docs/SCHEDULER_SPEC.md → android/app/src/main/java/com/tazer/taskorganizer/domain/PaceCalibration.kt
- `Optional analyzer subtasks negotiation` --references--> `AndroidIntegration`  [INFERRED]
  docs/DATA_AND_API_CONTRACT.md → android/app/src/main/java/com/tazer/taskorganizer/integration/AndroidIntegration.kt
- `Per-account focus preset controls planning and breaks` --references--> `FocusPreset`  [INFERRED]
  docs/SCHEDULER_SPEC.md → android/app/src/main/java/com/tazer/taskorganizer/domain/FocusPreset.kt
- `Home screen widget follows selected account and focus state` --references--> `FocusWidget`  [INFERRED]
  docs/SCHEDULER_SPEC.md → android/app/src/main/java/com/tazer/taskorganizer/widget/FocusWidget.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Transactional work history and replanning** — agents_room_repository, docs_data_and_api_contract_worksession, docs_data_and_api_contract_task, docs_scheduler_spec_smart_shift, docs_scheduler_spec_dynamic_reschedule, docs_scheduler_spec_weighted_time_allocation [EXTRACTED 1.00]
- **Agenda pada kalender lokal** — docs_data_and_api_contract_calendarevent, docs_data_and_api_contract_worksession, docs_data_and_api_contract_fixedschedule, docs_data_and_api_contract_task [EXTRACTED 1.00]
- **Persistensi status, progres, dan sisa waktu saat jeda** — docs_scheduler_spec_paused, docs_scheduler_spec_jeda_dan_lanjutkan, docs_data_and_api_contract_worksession_pausedremainingms, docs_data_and_api_contract_worksession_pausedfromstate, docs_data_and_api_contract_worksession_workbeforesegmentms, docs_data_and_api_contract_worksession_continuousworkbeforesegmentms [INFERRED 0.95]

## Communities (158 total, 98 thin omitted)

### Community 0 - "Spesifikasi scheduler dan timer"
Cohesion: 0.09
Nodes (31): Android :app, Android data layer, Android timer layer, APK Debug, Claude 5.5 model mapping, com.tazer.taskorganizer, Compose UI and ViewModel, Graphify (+23 more)

### Community 1 - "handler.ts"
Cohesion: 0.11
Nodes (19): TaskInput, createHandler(), Dependencies, error(), errors, json(), Readiness, readJson() (+11 more)

### Community 2 - "Rencana eksekusi Tazer"
Cohesion: 0.13
Nodes (17): account_entitlements, analysis_usage, Android Keystore AES-GCM, Atomic quota RPC, BYOK, FREE tier, Kuota bulanan UTC, PostgreSQL (+9 more)

### Community 3 - "TaskRepository"
Cohesion: 0.05
Nodes (21): FocusChange, TaskRepository, continuousWorkMs(), focusAvailableMs(), FocusClock, focusDurationMs(), FocusMoment, remainingAfterInterruption() (+13 more)

### Community 4 - "SchedulePages.kt"
Cohesion: 0.10
Nodes (4): ScheduleAddButton(), formatDate(), TaskAction(), TaskTag()

### Community 5 - "WeeklyStatistics.kt"
Cohesion: 0.13
Nodes (14): PaceCalibration, PaceSample, civilDate(), CourseFocus, DailyFocus, EstimateAccuracy, onTimeStreak(), StatisticsSession (+6 more)

### Community 7 - "FocusRuntime"
Cohesion: 0.09
Nodes (7): FocusAlarm, FocusRuntime, FocusSettings, FocusPermissionsDialog(), Scoped DND rule and rollback on leaving a countdown, Release 1.4.0 gate and signed upgrade, Per-account focus preset controls planning and breaks

### Community 9 - "Spesifikasi scheduler dan timer"
Cohesion: 0.18
Nodes (6): AndroidIntegration, Session, Tokens, Account, fail(), string()

### Community 10 - "TazerState"
Cohesion: 0.44
Nodes (5): AccountSettings(), ByokForm(), LoginScreen(), SecretField(), TazerState

### Community 12 - "FocusScreens.kt"
Cohesion: 0.27
Nodes (8): DeferFocusDialog(), ExtensionDialog(), focusClock(), FocusRhythm(), FocusScreen(), formatTimer(), isOvertime(), timerText()

### Community 13 - "ReleaseUiTest"
Cohesion: 0.08
Nodes (14): ReleaseUiTest, BriefingKind, EVENING, MORNING, BriefingMessage, BriefingSession, BriefingSettings, BriefingTask (+6 more)

### Community 15 - "StatisticsScreen.kt"
Cohesion: 0.17
Nodes (7): DailyFocusChart(), duration(), Statistic(), StatisticsHeading(), StatisticsScreen(), toMinutes(), localDate()

### Community 16 - "FocusEvent"
Cohesion: 0.15
Nodes (9): FocusNotificationTest, FocusEvent, FocusEventKind, ACTIVE, CUTOFF, DONE, EXTEND, REMINDER (+1 more)

### Community 19 - "IcsParser"
Cohesion: 0.08
Nodes (19): IcsImportTest, deleteEvent(), IcsImportCounts, observeEvents(), previewIcs(), IcsCalendar, IcsItem, Event (+11 more)

### Community 20 - "WorkSessionState"
Cohesion: 0.14
Nodes (9): FocusNotificationSoundTest, WorkSessionState, CANCELLED, DONE, EXTENDED, OVERTIME, PAUSED, PENDING (+1 more)

### Community 22 - "SubtaskDraft"
Cohesion: 0.18
Nodes (8): SubtaskDraft, TaskPackage, TaskPackages, ChecklistItem, checklistItem(), asDraft(), ChecklistChange, TaskInput

### Community 23 - "TazerApp.kt"
Cohesion: 0.11
Nodes (5): FloatingTimerDeck(), TazerApp(), TazerIcon(), TimerIconButton(), WorkspaceScreen()

### Community 24 - "AndroidIntegrationTest"
Cohesion: 0.19
Nodes (13): analyzeGemini(), analyzeGroq(), analyzeWithProvider(), geminiText(), integerSchema(), schema(), validateByok(), backendJson() (+5 more)

### Community 25 - "ScanImageButton.kt"
Cohesion: 0.12
Nodes (11): captureUri(), ImageSource, rememberImageSource(), scanFailureMessage(), ScanImageButton(), ScanReview(), SubtaskEditor(), SubtaskProgress() (+3 more)

### Community 27 - "Supabase Auth"
Cohesion: 0.14
Nodes (22): Backend, Server secret store, Analyzer input validation, Entitlement akun dan kuota bulanan, Gemini API Free, GET /auth/session, GET /subscription, Groq Free (+14 more)

### Community 29 - "IntegrationError"
Cohesion: 0.09
Nodes (23): IntegrationError, AI_UNAVAILABLE, AUTH_REJECTED, BROWSER_UNAVAILABLE, BYOK_GEMINI_REQUIRED, BYOK_KEY_MISSING, BYOK_REQUIRED, BYOK_STORAGE_UNAVAILABLE (+15 more)

### Community 30 - "WorkSession"
Cohesion: 0.14
Nodes (31): CalendarEvent, CalendarEvent(ownerUid,startEpochDay), CalendarEvent.startEpochDay dan endEpochDay, CalendarEvent.startMinute, FixedSchedule, Kalender lokal, Migrasi Room v1 ke v2, Migrasi Room v2 ke v3 (+23 more)

### Community 32 - "FocusDnd.kt"
Cohesion: 0.14
Nodes (7): FocusDnd, FocusDndAction, focusDndAction(), INSTALL, KEEP, WITHDRAW, FocusDndStateTest

### Community 33 - "PendingAuth"
Cohesion: 0.14
Nodes (9): AuthFlow, GOOGLE, RECOVERY, SIGN_UP, AuthPkce, isAuthCallback(), PendingAuth, AuthPkceTest (+1 more)

### Community 34 - "TazerViewModel.kt"
Cohesion: 0.08
Nodes (15): AnalysisSource, AI, OFFLINE, TaskStatus, DONE, IN_PROGRESS, PENDING, IntegrationException (+7 more)

### Community 35 - "DailyBriefingSetting"
Cohesion: 0.29
Nodes (8): BriefingRow(), DailyBriefingSetting(), NumberWheel(), PickerDateButton(), PickerTimeButton(), platformTheme(), TimeSpinner(), formatMinute()

### Community 36 - "Signed APK Release"
Cohesion: 0.16
Nodes (14): APK sideload, GitHub Releases gratis, Node.js + TypeScript, Rp 0 free tier, Signed APK Release, Cloudflare Workers Free, Web Fetch API, apksigner verify (+6 more)

### Community 37 - "IntegrationModels.kt"
Cohesion: 0.13
Nodes (17): analysis(), analysisSubtasks(), AuthCallback, PasswordRecovery, SignedIn, ByokProvider, GEMINI, GROQ (+9 more)

### Community 39 - "parseReleaseUpdate"
Cohesion: 0.05
Nodes (24): CourseColorStore, CourseColors, CourseSwatch, CourseTone, NoteBlock, Bullet, Heading, Paragraph (+16 more)

### Community 41 - "DATA_AND_API_CONTRACT.md"
Cohesion: 0.09
Nodes (25): Aturan global, Batas modul dan struktur target, Handoff `i-have-adhd`, i-have-adhd handoff, Pemeriksaan sebelum handoff, Ponytail, Tazer (Task Organizer), Tim Orkestra Claude 5.5 (+17 more)

### Community 42 - "WorkSession"
Cohesion: 0.17
Nodes (15): CalendarEvent, FixedSchedule, FixedScheduleKind, BREAK, ROUTINE, Subtask, Task, UserProfile (+7 more)

### Community 43 - "OfflineAnalyzerTest"
Cohesion: 0.33
Nodes (3): OfflineAnalysis, OfflineAnalyzer, OfflineAnalyzerTest

### Community 44 - "CourseColorUi.kt"
Cohesion: 0.33
Nodes (7): color(), CourseChip(), CourseColorSetting(), courseContainer(), CourseDot(), CoursePalette, paletteContainer()

### Community 48 - "package.json"
Cohesion: 0.14
Nodes (15): devDependencies, @types/node@^22.0.0, typescript@~5.9.3, engines, node, name, private, scripts (+7 more)

### Community 50 - "ScheduleDraft"
Cohesion: 0.50
Nodes (4): PreviewRow, ScheduleDraft, SchedulePreview, TableImport

### Community 51 - "AccountUid"
Cohesion: 0.13
Nodes (6): ByokCredential, ByokVault, CredentialCipher, credentialStorage(), AccountUid, ByokSettings

### Community 52 - "Focus timer state machine"
Cohesion: 0.22
Nodes (20): Belum Selesai and Selesai tabs, OVERDUE display status, Task.status, CANCELLED session, DONE task, Durasi fokus penuh, elapsedRealtime, EXTENDED session (+12 more)

### Community 53 - "tazer-v3.sql"
Cohesion: 0.25
Nodes (13): `CalendarEvent`, `FixedSchedule`, IF, index_CalendarEvent_ownerUid_startEpochDay, index_FixedSchedule_ownerUid_dayOfWeek, index_Task_id_ownerUid, index_Task_ownerUid_status_completedAt, index_WorkSession_ownerUid_state_plannedStartAt (+5 more)

### Community 54 - "TazerGlyph"
Cohesion: 0.10
Nodes (20): TazerGlyph, Add, Calendar, Camera, Check, CsvFile, Delete, Down (+12 more)

### Community 55 - "compilerOptions"
Cohesion: 0.14
Nodes (13): compilerOptions, lib, module, noUncheckedIndexedAccess, noUnusedLocals, noUnusedParameters, outDir, rootDir (+5 more)

### Community 56 - "ThemeSetting"
Cohesion: 0.40
Nodes (3): isDark(), ThemeSetting(), ThemeSystemBars()

### Community 57 - "TazerDatabase"
Cohesion: 0.06
Nodes (28): CloudSyncTest, Remote, CloudSync, Change, SyncResult, SyncCursor, SyncDevice, SyncEntry (+20 more)

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
Cohesion: 0.09
Nodes (4): IcsImportPreview, FocusTarget, rejectsSession(), TazerViewModel

### Community 66 - "Autentikasi dan endpoint HTTP"
Cohesion: 0.22
Nodes (9): Analyzer, Autentikasi dan endpoint HTTP, Envelope error, GEMINI_MODEL, GET /health, Health readiness (ready), Kontrak data dan API, SUPABASE_PUBLISHABLE_KEY (+1 more)

### Community 68 - "HTTP error envelope"
Cohesion: 0.29
Nodes (7): AI_UNAVAILABLE, EMAIL_UNVERIFIED, HTTP error envelope, INVALID_AUTH, PRO_REQUIRED, QUOTA_EXHAUSTED, VERIFICATION_UNAVAILABLE

### Community 69 - "ImageScan.kt"
Cohesion: 0.13
Nodes (13): compressForScan(), extractWithGemini(), geminiImageJson(), ImageExtraction, imageExtraction(), scanBody(), scanInstructions(), scanSampleSize() (+5 more)

### Community 70 - "image.ts"
Cohesion: 0.11
Nodes (24): calendarDate(), clock(), decodedSize(), ImageExtraction, imageExtractionOutput(), imageExtractionSchema, imageInput(), ImageMimeType (+16 more)

### Community 76 - "Uri"
Cohesion: 0.33
Nodes (3): ExportIcsButton(), readDocument(), writeDocument()

### Community 92 - "ScheduleScan.kt"
Cohesion: 0.62
Nodes (5): extractScheduleWithGemini(), scheduleInstructions(), ScheduleScan, scheduleScan(), scheduleSchema()

### Community 93 - "schedule.test.ts"
Cohesion: 0.29
Nodes (4): MAX_IMAGE_BYTES, scheduleExtractionSchema, body, jpeg

### Community 105 - "node.ts"
Cohesion: 0.22
Nodes (8): mockDependencies(), bridgeError(), createNodeServer(), loopback, requestBody(), listen(), task, 100 monthly PRO analysis slots by account

### Community 114 - "ReleaseUiTest.kt"
Cohesion: 0.12
Nodes (5): ActivityMonitor, FocusPresetMode, CUSTOM, DEEP_WORK, POMODORO

### Community 119 - "WidgetContent.kt"
Cohesion: 0.17
Nodes (15): FocusLink, FocusLinkKind, ACTIVE, EXTEND, REMINDER, TazerThemeMode, DARK, LIGHT (+7 more)

### Community 120 - "providers.ts"
Cohesion: 0.12
Nodes (28): analysisSchema, Env, realHandler(), imageInstructions(), scheduleInstructions(), NodeEnv, geminiConfig(), geminiImage() (+20 more)

### Community 121 - "ScheduleScreens.kt"
Cohesion: 0.08
Nodes (16): AgendaItem, AgendaPill(), CalendarScreen(), CalendarWeek(), MonthPicker(), DateSpinner(), ProfileForm(), ScheduleTag() (+8 more)

### Community 122 - "supabase.ts"
Cohesion: 0.27
Nodes (12): Analysis, analysisOutput(), analysisSubtasks(), integerBetween(), isUtcTimestamp(), object(), taskInput(), httpsOrigin() (+4 more)

### Community 140 - "MainActivity.kt"
Cohesion: 0.12
Nodes (5): BackupCounts, BackupPreview, BackupRepository, OpenDocument iCalendar import and Room journal, Repeat-safe iCalendar import identity

## Knowledge Gaps
- **126 isolated node(s):** `PasswordRecovery`, `CANCELLED`, `DONE`, `EXTENDED`, `OVERTIME` (+121 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 457 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **98 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Impor jadwal tetap` connect `DATA_AND_API_CONTRACT.md` to `zoneid`, `SchedulePages.kt`, `image.ts`, `ScheduleDraft`, `ScheduleScan.kt`, `XlsxReader.kt`?**
  _High betweenness centrality (0.078) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `TaskRepository` (e.g. with `.acknowledgementCannotClearAnEditMadeDuringARequest()` and `.queuesOfflineChangesAcrossCloseAndReplaysSixEntityTypesAndTombstones()`) actually correct?**
  _`TaskRepository` has 2 INFERRED edges - model-reasoned connections that need verification._
- **What connects `PasswordRecovery`, `CANCELLED`, `DONE` to the rest of the system?**
  _126 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Spesifikasi scheduler dan timer` be split into smaller, more focused modules?**
  _Cohesion score 0.09247311827956989 - nodes in this community are weakly interconnected._
- **Why does `WorkSession` connect `WorkSession` to `ScheduleScreens.kt`, `TazerViewModel.kt`, `FocusStartTest.kt`, `zoneid`, `TaskRepository`, `TazerViewModel`, `FocusScreens.kt`, `ReleaseUiTest`, `FocusNotifications`, `FocusEvent`, `ReleaseUiTest.kt`, `WorkSessionState`, `TazerDao`, `WidgetContent.kt`, `TazerApp.kt`, `TazerDatabase`, `FocusRuntime.kt`, `FocusWidgetTest`?**
  _High betweenness centrality (0.069) - this node is a cross-community bridge._
- **Are the 7 inferred relationships involving `FocusRuntime` (e.g. with `FocusAlarms` and `FocusDnd`) actually correct?**
  _`FocusRuntime` has 7 INFERRED edges - model-reasoned connections that need verification._
- **Should `handler.ts` be split into smaller, more focused modules?**
  _Cohesion score 0.11264367816091954 - nodes in this community are weakly interconnected._