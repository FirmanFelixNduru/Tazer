# Graph Report - Project-B  (2026-11-27)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 2388 nodes · 7041 edges · 161 communities (71 shown, 90 thin omitted)
- Extraction: 92% EXTRACTED · 8% INFERRED · 0% AMBIGUOUS · INFERRED: 546 edges (avg confidence: 0.87)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `9d9c8d5d`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- NetworkAvailability.kt
- handler.ts
- DailyBriefingAlarms.kt
- adapters.test.ts
- WorkSessionState
- WeeklyStatistics.kt
- DailyBriefingTest
- FocusRuntime
- TaskRepositoryTest
- Spesifikasi scheduler dan timer
- CalendarScreen
- TaskScreens.kt
- ReleaseUiTest
- FocusService.kt
- SchedulerTest
- FocusEvent
- WidgetContent.kt
- TazerGlyph
- IcsParser
- FocusNotifications.kt
- TazerDao
- neu
- TazerApp.kt
- parseJson
- Row
- MainActivity.kt
- Supabase Auth
- TaskRepository.kt
- IntegrationError
- WorkSession
- SyncJournal.kt
- focusDndAction
- PendingAuth
- TazerViewModel.kt
- DateTimeButtons.kt
- Signed APK Release
- fail
- TasksScreen
- ReleaseUpdateSetting.kt
- ImageScanTest
- DATA_AND_API_CONTRACT.md
- WorkSession
- ReleaseUiTest.kt
- SchedulePages.kt
- Claude 5.5 model mapping
- TaskPackage
- TazerState
- package.json
- CourseColors
- IntegrationException
- ByokVault.kt
- Focus timer state machine
- tazer-v3.sql
- CloudSync
- compilerOptions
- .requireUid
- CloudSyncTest.kt
- FocusPreset
- 20261125000000_entitlement.sql
- tazer-v1.sql
- FocusNotifications
- ScheduleDraft
- TaskSearch.kt
- public.tazer_sync_pull
- TazerViewModel
- Autentikasi dan endpoint HTTP
- TaskRepository
- HTTP error envelope
- ImageScan.kt
- image.ts
- .analyze
- OfflineAnalyzerTest
- FocusAlarms
- ScanImageButton.kt
- .get
- Spesifikasi scheduler dan timer
- IcsImportTest.kt
- instant
- StatisticsScreen
- node.ts
- TazerBackup.kt
- CalendarRepository
- FocusScreen
- SubtaskRepository
- parseReleaseUpdate
- AndroidIntegrationTest.kt
- WidgetContent.kt
- providers.ts
- SettingsScreen
- supabase.ts
- ScheduleScreens.kt
- Per-account focus preset controls planning and breaks
- SessionVault
- .focusAction
- FocusRuntime.kt
- .enter
- WidgetContent.kt
- .change

## God Nodes (most connected - your core abstractions)
1. `TazerViewModel` - 100 edges
2. `TaskRepository` - 80 edges
3. `FocusRuntime` - 70 edges
4. `ReleaseUiTest` - 63 edges
5. `FocusMoment` - 62 edges
6. `WorkSession` - 55 edges
7. `AndroidIntegration` - 54 edges
8. `fail()` - 54 edges
9. `TaskRepositoryTest` - 42 edges
10. `TazerGlyph` - 36 edges

## Surprising Connections (you probably didn't know these)
- `Slot kosong dan batas sesi` --references--> `FixedSchedule`  [INFERRED]
  docs/SCHEDULER_SPEC.md → android/app/src/main/java/com/tazer/taskorganizer/data/Entities.kt
- `Personal pace median from completed work` --references--> `PaceCalibration`  [INFERRED]
  docs/SCHEDULER_SPEC.md → android/app/src/main/java/com/tazer/taskorganizer/domain/PaceCalibration.kt
- `Offline queue acknowledgement and cursor` --references--> `CloudSync`  [INFERRED]
  docs/DATA_AND_API_CONTRACT.md → android/app/src/main/java/com/tazer/taskorganizer/data/CloudSync.kt
- `Optional analyzer subtasks negotiation` --references--> `AndroidIntegration`  [INFERRED]
  docs/DATA_AND_API_CONTRACT.md → android/app/src/main/java/com/tazer/taskorganizer/integration/AndroidIntegration.kt
- `Task checklist: Subtask with composite owner foreign key` --references--> `SubtaskRepository`  [INFERRED]
  docs/DATA_AND_API_CONTRACT.md → android/app/src/main/java/com/tazer/taskorganizer/data/SubtaskRepository.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Transactional work history and replanning** — agents_room_repository, docs_data_and_api_contract_worksession, docs_data_and_api_contract_task, docs_scheduler_spec_smart_shift, docs_scheduler_spec_dynamic_reschedule, docs_scheduler_spec_weighted_time_allocation [EXTRACTED 1.00]
- **Agenda pada kalender lokal** — docs_data_and_api_contract_calendarevent, docs_data_and_api_contract_worksession, docs_data_and_api_contract_fixedschedule, docs_data_and_api_contract_task [EXTRACTED 1.00]
- **Persistensi status, progres, dan sisa waktu saat jeda** — docs_scheduler_spec_paused, docs_scheduler_spec_jeda_dan_lanjutkan, docs_data_and_api_contract_worksession_pausedremainingms, docs_data_and_api_contract_worksession_pausedfromstate, docs_data_and_api_contract_worksession_workbeforesegmentms, docs_data_and_api_contract_worksession_continuousworkbeforesegmentms [INFERRED 0.95]

## Communities (161 total, 90 thin omitted)

### Community 1 - "handler.ts"
Cohesion: 0.13
Nodes (17): TaskInput, createHandler(), Dependencies, error(), errors, json(), Readiness, readJson() (+9 more)

### Community 2 - "DailyBriefingAlarms.kt"
Cohesion: 0.16
Nodes (12): BriefingKind, EVENING, MORNING, BriefingMessage, BriefingSession, BriefingSettings, BriefingTask, morning() (+4 more)

### Community 3 - "adapters.test.ts"
Cohesion: 0.22
Nodes (7): analysisSchema, auth, entitlement, env, input, result, suggested

### Community 4 - "WorkSessionState"
Cohesion: 0.16
Nodes (9): ActivityMonitor, WorkSessionState, CANCELLED, DONE, EXTENDED, OVERTIME, PAUSED, PENDING (+1 more)

### Community 5 - "WeeklyStatistics.kt"
Cohesion: 0.12
Nodes (15): PaceCalibration, PaceSample, civilDate(), CourseFocus, DailyFocus, EstimateAccuracy, onTimeStreak(), StatisticsSession (+7 more)

### Community 7 - "FocusRuntime"
Cohesion: 0.13
Nodes (3): FocusRuntime, FocusSettings, FocusPermissionsDialog()

### Community 9 - "Spesifikasi scheduler dan timer"
Cohesion: 0.13
Nodes (10): AndroidIntegration, Session, Tokens, scanBody(), Account, AccountUid, AuthCallback, PasswordRecovery (+2 more)

### Community 10 - "CalendarScreen"
Cohesion: 0.16
Nodes (14): AgendaItem, AgendaPill(), CalendarScreen(), CalendarWeek(), MonthPicker(), color(), CourseChip(), CourseColorSetting() (+6 more)

### Community 16 - "FocusEvent"
Cohesion: 0.17
Nodes (8): FocusEvent, FocusEventKind, ACTIVE, CUTOFF, DONE, EXTEND, REMINDER, START_ALARM

### Community 18 - "TazerGlyph"
Cohesion: 0.06
Nodes (31): TazerGlyph, Add, ArrowRight, Back, Calendar, Camera, Check, CsvFile (+23 more)

### Community 19 - "IcsParser"
Cohesion: 0.06
Nodes (18): deleteEvent(), observeEvents(), previewIcs(), IcsCalendar, IcsItem, Event, WeeklyBlock, IcsParser (+10 more)

### Community 20 - "FocusNotifications.kt"
Cohesion: 0.09
Nodes (3): FocusDndTest, FocusNotificationSoundTest, FocusDnd

### Community 21 - "TazerDao"
Cohesion: 0.08
Nodes (5): CalendarDao, AnalysisSource, AI, OFFLINE, TazerDao

### Community 22 - "neu"
Cohesion: 0.17
Nodes (22): AmbientOrbs(), LoginScreen(), LogoBadge(), NeuField(), NeuIconButton(), SignInButton(), SignUpButton(), Neu (+14 more)

### Community 23 - "TazerApp.kt"
Cohesion: 0.09
Nodes (5): Pemetaan layar, Polish v1.9.1, Remember Me, Spesifikasi UI neumorphic biru-putih (v1.9.0), Token

### Community 24 - "parseJson"
Cohesion: 0.30
Nodes (5): backendJson(), call(), HttpResponse, parseJson(), parseJsonValue()

### Community 25 - "Row"
Cohesion: 0.29
Nodes (9): BriefingRow(), DailyBriefingSetting(), FilterButton(), FilterPill(), SearchPill(), SheetHeading(), TaskFilterBar(), TaskFilterSheet() (+1 more)

### Community 27 - "Supabase Auth"
Cohesion: 0.09
Nodes (33): Backend, Server secret store, account_entitlements, analysis_usage, Analyzer input validation, Android Keystore AES-GCM, Atomic quota RPC, BYOK (+25 more)

### Community 28 - "TaskRepository.kt"
Cohesion: 0.19
Nodes (7): continuousWorkMs(), focusAvailableMs(), FocusClock, focusDurationMs(), remainingAfterInterruption(), ScheduleRule, FocusTest

### Community 29 - "IntegrationError"
Cohesion: 0.09
Nodes (23): IntegrationError, AI_UNAVAILABLE, AUTH_REJECTED, BROWSER_UNAVAILABLE, BYOK_GEMINI_REQUIRED, BYOK_KEY_MISSING, BYOK_REQUIRED, BYOK_STORAGE_UNAVAILABLE (+15 more)

### Community 30 - "WorkSession"
Cohesion: 0.14
Nodes (31): CalendarEvent, CalendarEvent(ownerUid,startEpochDay), CalendarEvent.startEpochDay dan endEpochDay, CalendarEvent.startMinute, FixedSchedule, Kalender lokal, Migrasi Room v1 ke v2, Migrasi Room v2 ke v3 (+23 more)

### Community 31 - "SyncJournal.kt"
Cohesion: 0.15
Nodes (14): SyncCursor, SyncDevice, SyncEntry, SyncJournal, Optional analyzer subtasks negotiation, Cloud Sync Lamport version and writer tie breaker, Supabase Cloud Sync push and pull RPC, Portable WorkSession history and local monotonic timer (+6 more)

### Community 32 - "focusDndAction"
Cohesion: 0.27
Nodes (6): FocusDndAction, focusDndAction(), INSTALL, KEEP, WITHDRAW, FocusDndStateTest

### Community 33 - "PendingAuth"
Cohesion: 0.11
Nodes (9): AuthFlow, GOOGLE, RECOVERY, SIGN_UP, AuthPkce, isAuthCallback(), PendingAuth, AuthPkceTest (+1 more)

### Community 34 - "TazerViewModel.kt"
Cohesion: 0.14
Nodes (8): SubtaskDraft, ChecklistItem, checklistItem(), AnalysisSuggestion, asDraft(), ChecklistChange, ScanSuggestion, TaskInput

### Community 36 - "Signed APK Release"
Cohesion: 0.11
Nodes (20): APK sideload, GitHub Releases gratis, Node.js + TypeScript, Rp 0 free tier, Signed APK Release, Cloudflare Workers Free, Web Fetch API, apksigner verify (+12 more)

### Community 37 - "fail"
Cohesion: 0.13
Nodes (27): analyzeGemini(), analyzeGroq(), analyzeWithProvider(), geminiText(), integerSchema(), schema(), validateByok(), ByokCredential (+19 more)

### Community 38 - "TasksScreen"
Cohesion: 0.10
Nodes (22): NumberWheel(), PickerButton(), PickerDateButton(), PickerTimeButton(), platformTheme(), TimeSpinner(), DeferFocusDialog(), PrimaryButton() (+14 more)

### Community 39 - "ReleaseUpdateSetting.kt"
Cohesion: 0.12
Nodes (3): ExportIcsButton(), readDocument(), writeDocument()

### Community 41 - "DATA_AND_API_CONTRACT.md"
Cohesion: 0.09
Nodes (27): Aturan global, Batas modul dan struktur target, Handoff `i-have-adhd`, i-have-adhd handoff, Pemeriksaan sebelum handoff, Ponytail, Tazer (Task Organizer), Tim Orkestra Claude 5.5 (+19 more)

### Community 42 - "WorkSession"
Cohesion: 0.22
Nodes (15): IcsImportPreview, CalendarEvent, FixedSchedule, Subtask, Task, TaskStatus, DONE, IN_PROGRESS (+7 more)

### Community 43 - "ReleaseUiTest.kt"
Cohesion: 0.08
Nodes (4): CalendarRepositoryTest, FocusNotificationTest, versionWins(), SyncConflictTest

### Community 45 - "Claude 5.5 model mapping"
Cohesion: 0.16
Nodes (18): Android :app, APK Debug, Claude 5.5 model mapping, com.tazer.taskorganizer, Compose UI and ViewModel, Graphify, Impeccable, Verifikasi render Android Compose (+10 more)

### Community 47 - "TazerState"
Cohesion: 0.32
Nodes (4): AccountSettings(), ByokForm(), SecretField(), TazerState

### Community 48 - "package.json"
Cohesion: 0.14
Nodes (15): devDependencies, @types/node@^22.0.0, typescript@~5.9.3, engines, node, name, private, scripts (+7 more)

### Community 49 - "CourseColors"
Cohesion: 0.13
Nodes (6): CourseColorStore, CourseColors, CourseSwatch, CourseTone, NeumorphicTokensTest, Kontras WCAG

### Community 51 - "ByokVault.kt"
Cohesion: 0.15
Nodes (3): ByokVault, CredentialCipher, credentialStorage()

### Community 52 - "Focus timer state machine"
Cohesion: 0.22
Nodes (20): Belum Selesai and Selesai tabs, OVERDUE display status, Task.status, CANCELLED session, DONE task, Durasi fokus penuh, elapsedRealtime, EXTENDED session (+12 more)

### Community 53 - "tazer-v3.sql"
Cohesion: 0.25
Nodes (13): `CalendarEvent`, `FixedSchedule`, IF, index_CalendarEvent_ownerUid_startEpochDay, index_FixedSchedule_ownerUid_dayOfWeek, index_Task_id_ownerUid, index_Task_ownerUid_status_completedAt, index_WorkSession_ownerUid_state_plannedStartAt (+5 more)

### Community 54 - "CloudSync"
Cohesion: 0.12
Nodes (10): CloudSync, Change, SyncResult, SyncKind, EVENT, FIXED, PROFILE, SESSION (+2 more)

### Community 55 - "compilerOptions"
Cohesion: 0.14
Nodes (13): compilerOptions, lib, module, noUncheckedIndexedAccess, noUnusedLocals, noUnusedParameters, outDir, rootDir (+5 more)

### Community 56 - ".requireUid"
Cohesion: 0.12
Nodes (3): FixedScheduleKind, BREAK, ROUTINE

### Community 57 - "CloudSyncTest.kt"
Cohesion: 0.16
Nodes (3): CloudSyncTest, Remote, Callback

### Community 58 - "FocusPreset"
Cohesion: 0.26
Nodes (7): FocusPreset, Scheduler, Option, Work, SchedulingTask, TimeBlock, schedulingTask()

### Community 59 - "20261125000000_entitlement.sql"
Cohesion: 0.24
Nodes (8): Android client, Supabase and providers, Tazer backend, public.account_entitlements, public.analysis_usage, public.tazer_entitlement(), public.tazer_entitlement(), tazer-db

### Community 60 - "tazer-v1.sql"
Cohesion: 0.29
Nodes (11): `FixedSchedule`, IF, index_FixedSchedule_ownerUid_dayOfWeek, index_Task_id_ownerUid, index_Task_ownerUid_status_completedAt, index_WorkSession_ownerUid_state_plannedStartAt, index_WorkSession_taskId_ownerUid, room_master_table (+3 more)

### Community 62 - "ScheduleDraft"
Cohesion: 0.07
Nodes (14): XlsxReader, PreviewRow, ScheduleDraft, SchedulePreview, ScheduleTable, Columns, TableImport, extractScheduleWithGemini() (+6 more)

### Community 63 - "TaskSearch.kt"
Cohesion: 0.09
Nodes (20): DeadlineRange, ALL, OVERDUE, THIS_WEEK, THREE_DAYS, TODAY, DurationRange, ALL (+12 more)

### Community 64 - "public.tazer_sync_pull"
Cohesion: 0.22
Nodes (5): public, public.%I, public.tazer_sync_pull(), public.tazer_sync_push(), auth.users

### Community 66 - "Autentikasi dan endpoint HTTP"
Cohesion: 0.22
Nodes (9): Analyzer, Autentikasi dan endpoint HTTP, Envelope error, GEMINI_MODEL, GET /health, Health readiness (ready), Kontrak data dan API, SUPABASE_PUBLISHABLE_KEY (+1 more)

### Community 67 - "TaskRepository"
Cohesion: 0.20
Nodes (4): WorkSession, FocusChange, TaskRepository, FocusMoment

### Community 68 - "HTTP error envelope"
Cohesion: 0.29
Nodes (7): AI_UNAVAILABLE, EMAIL_UNVERIFIED, HTTP error envelope, INVALID_AUTH, PRO_REQUIRED, QUOTA_EXHAUSTED, VERIFICATION_UNAVAILABLE

### Community 69 - "ImageScan.kt"
Cohesion: 0.20
Nodes (10): compressForScan(), extractWithGemini(), ImageExtraction, imageExtraction(), scanInstructions(), scanSampleSize(), scanSchema(), scanTargetSize() (+2 more)

### Community 70 - "image.ts"
Cohesion: 0.10
Nodes (26): integerBetween(), calendarDate(), clock(), decodedSize(), ImageExtraction, imageExtractionOutput(), imageExtractionSchema, imageInput() (+18 more)

### Community 73 - ".analyze"
Cohesion: 0.33
Nodes (4): AnalyzerMode, BYOK, CLOUD, OFFLINE

### Community 76 - "OfflineAnalyzerTest"
Cohesion: 0.33
Nodes (3): OfflineAnalysis, OfflineAnalyzer, OfflineAnalyzerTest

### Community 92 - "Spesifikasi scheduler dan timer"
Cohesion: 0.19
Nodes (13): Android data layer, Android timer layer, Kotlin domain, Room repository, Analisis offline, Analyzer offline, Android timer dan pemulihan, Priority Ranking Engine (+5 more)

### Community 102 - "StatisticsScreen"
Cohesion: 0.46
Nodes (6): DailyFocusChart(), duration(), Statistic(), StatisticsHeading(), StatisticsScreen(), toMinutes()

### Community 105 - "node.ts"
Cohesion: 0.13
Nodes (13): Env, scheduleExtractionSchema, mockDependencies(), bridgeError(), createNodeServer(), loopback, NodeEnv, requestBody() (+5 more)

### Community 108 - "TazerBackup.kt"
Cohesion: 0.17
Nodes (4): BackupCounts, BackupPreview, BackupRepository, TazerBackup

### Community 114 - "FocusScreen"
Cohesion: 0.27
Nodes (6): ExtensionDialog(), focusClock(), FocusRhythm(), FocusScreen(), isOvertime(), formatDate()

### Community 116 - "parseReleaseUpdate"
Cohesion: 0.12
Nodes (14): NoteBlock, Bullet, Heading, Paragraph, NoteSpan, ReleaseNotes, parseReleaseUpdate(), ReleaseUpdate (+6 more)

### Community 119 - "WidgetContent.kt"
Cohesion: 0.12
Nodes (5): FocusWidgetTest, TazerThemeMode, DARK, LIGHT, SYSTEM

### Community 120 - "providers.ts"
Cohesion: 0.20
Nodes (18): realHandler(), imageInstructions(), scheduleInstructions(), geminiConfig(), geminiImage(), geminiOutput(), geminiSubtaskSchema, groqOutput() (+10 more)

### Community 121 - "SettingsScreen"
Cohesion: 0.16
Nodes (13): DateSpinner(), ProfileForm(), ScheduleTag(), SettingsCard(), SettingsScreen(), TimeField(), TimelineEntry(), TimelineRow (+5 more)

### Community 122 - "supabase.ts"
Cohesion: 0.24
Nodes (12): Analysis, analysisOutput(), analysisSubtasks(), isUtcTimestamp(), object(), taskInput(), httpsOrigin(), invalidAuthCodes (+4 more)

### Community 123 - "ScheduleScreens.kt"
Cohesion: 0.07
Nodes (3): Navigasi jadwal dan acara, 3. Perubahan navigasi, Gate rilis 1.9.0

### Community 124 - "Per-account focus preset controls planning and breaks"
Cohesion: 1.00
Nodes (3): Scoped DND rule and rollback on leaving a countdown, Release 1.4.0 gate and signed upgrade, Per-account focus preset controls planning and breaks

### Community 126 - "SessionVault"
Cohesion: 0.13
Nodes (14): SessionVault, 1. Login (mengikuti `login-neumorphic-blue-reference.jpg`), 2. Seluruh aplikasi (mengikuti `app-ui-neumorphic-mobile-reference.jpg`, warna diganti biru-putih), 4. Saran per peran, 5. Filter tugas v1.9.1 (mengikuti `task-filter-dropdown-reference.jpg`), 6. Gerbang persetujuan, Aksesibilitas (wajib, `impeccable` craft floor), Aset (+6 more)

### Community 134 - "FocusRuntime.kt"
Cohesion: 0.09
Nodes (5): FocusPresetMode, CUSTOM, DEEP_WORK, POMODORO, FocusReceiver

### Community 156 - "WidgetContent.kt"
Cohesion: 0.17
Nodes (11): FocusLink, FocusLinkKind, ACTIVE, EXTEND, REMINDER, activeWidget(), link(), loadWidgetSnapshot() (+3 more)

## Knowledge Gaps
- **150 isolated node(s):** `PasswordRecovery`, `DARK`, `LIGHT`, `SYSTEM`, `CUSTOM` (+145 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 492 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **90 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Impor jadwal tetap` connect `DATA_AND_API_CONTRACT.md` to `image.ts`, `IcsParser`, `SchedulePages.kt`, `ScheduleDraft`?**
  _High betweenness centrality (0.085) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `TaskRepository` (e.g. with `.acknowledgementCannotClearAnEditMadeDuringARequest()` and `.queuesOfflineChangesAcrossCloseAndReplaysSixEntityTypesAndTombstones()`) actually correct?**
  _`TaskRepository` has 2 INFERRED edges - model-reasoned connections that need verification._
- **What connects `PasswordRecovery`, `DARK`, `LIGHT` to the rest of the system?**
  _150 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `handler.ts` be split into smaller, more focused modules?**
  _Cohesion score 0.13105413105413105 - nodes in this community are weakly interconnected._
- **Why does `TaskRepository` connect `TaskRepository` to `TazerViewModel`, `TazerViewModel.kt`, `FocusRuntime.kt`, `FocusRuntime`, `TaskRepositoryTest`, `WorkSession`, `ReleaseUiTest.kt`, `ReleaseUiTest`, `.get`, `WidgetContent.kt`, `.requireUid`, `CloudSyncTest.kt`, `TaskRepository.kt`?**
  _High betweenness centrality (0.062) - this node is a cross-community bridge._
- **Are the 7 inferred relationships involving `FocusRuntime` (e.g. with `FocusAlarms` and `FocusDnd`) actually correct?**
  _`FocusRuntime` has 7 INFERRED edges - model-reasoned connections that need verification._
- **Should `WeeklyStatistics.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.1207897793263647 - nodes in this community are weakly interconnected._