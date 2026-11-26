# Graph Report - Project-B  (2026-11-26)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 2269 nodes · 6590 edges · 164 communities (66 shown, 98 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 449 edges (avg confidence: 0.86)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `b09c5a14`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Spesifikasi scheduler dan timer
- handler.ts
- DailyBriefing.kt
- TaskRepository
- AccountScreens.kt
- WeeklyStatistics.kt
- ReleaseUpdateSetting.kt
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
- TazerGlyph
- IcsParser
- WorkSessionState
- TazerDao
- TaskPackage
- TazerApp.kt
- parseJson
- ScanImageButton.kt
- Claude 5.5 model mapping
- Supabase Auth
- CloudSyncTest.kt
- IntegrationError
- WorkSession
- TazerDatabase
- FocusDnd.kt
- PendingAuth
- TazerViewModel.kt
- DateTimeButtons.kt
- Signed APK Release
- fail
- XlsxAndScheduleScanTest.kt
- CourseColorStore
- ScheduleTable
- DATA_AND_API_CONTRACT.md
- WorkSession
- assertequals
- SchedulePages.kt
- FocusNotifications
- TazerBackup.kt
- package.json
- ScheduleImportTest
- ScheduleDraft
- CredentialCipher
- Focus timer state machine
- tazer-v3.sql
- FocusRuntime.kt
- compilerOptions
- .factor
- TazerDatabase
- instant
- 20261125000000_entitlement.sql
- tazer-v1.sql
- SubtaskRepository
- XlsxReader.kt
- ReleaseUiTest.kt
- public.tazer_sync_pull
- TazerViewModel
- Autentikasi dan endpoint HTTP
- CalendarRepositoryTest.kt
- HTTP error envelope
- ImageScan.kt
- image.ts
- FocusDndTest.kt
- NetworkAvailability.kt
- ScheduleScan.kt
- .enter
- TaskScreens.kt
- Rencana eksekusi Tazer
- node.ts
- build
- FocusPresetMode
- DailyBriefingTest
- SessionVault
- IntegrationException
- AndroidIntegrationTest.kt
- WidgetContent.kt
- providers.ts
- ScheduleScreens.kt
- supabase.ts
- WidgetContent.kt
- FocusStartTest
- adapters.test.ts
- .analyze
- AuthCallback

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
- `Offline queue acknowledgement and cursor` --references--> `CloudSync`  [INFERRED]
  docs/DATA_AND_API_CONTRACT.md → android/app/src/main/java/com/tazer/taskorganizer/data/CloudSync.kt
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

## Communities (164 total, 98 thin omitted)

### Community 0 - "Spesifikasi scheduler dan timer"
Cohesion: 0.18
Nodes (14): Android data layer, Android timer layer, Kotlin domain, Room repository, FREE tier, Analisis offline, Analyzer offline, Android timer dan pemulihan (+6 more)

### Community 1 - "handler.ts"
Cohesion: 0.13
Nodes (17): TaskInput, createHandler(), Dependencies, error(), errors, json(), Readiness, readJson() (+9 more)

### Community 2 - "DailyBriefing.kt"
Cohesion: 0.20
Nodes (9): BriefingKind, EVENING, MORNING, BriefingMessage, BriefingSession, BriefingTask, morning(), nextTrigger() (+1 more)

### Community 3 - "TaskRepository"
Cohesion: 0.05
Nodes (21): FocusChange, TaskRepository, continuousWorkMs(), focusAvailableMs(), FocusClock, focusDurationMs(), FocusMoment, remainingAfterInterruption() (+13 more)

### Community 4 - "AccountScreens.kt"
Cohesion: 0.11
Nodes (4): TaskQuery, FilteredEmpty(), TaskFilterBar(), ImportPackageButton()

### Community 5 - "WeeklyStatistics.kt"
Cohesion: 0.23
Nodes (10): civilDate(), CourseFocus, DailyFocus, EstimateAccuracy, onTimeStreak(), StatisticsSession, StatisticsTask, WeeklyStatistics (+2 more)

### Community 6 - "ReleaseUpdateSetting.kt"
Cohesion: 0.07
Nodes (8): BriefingRow(), DailyBriefingSetting(), ExportIcsButton(), readDocument(), writeDocument(), isDark(), ThemeSetting(), ThemeSystemBars()

### Community 7 - "FocusRuntime"
Cohesion: 0.11
Nodes (8): FocusAlarm, FocusRuntime, FocusSettings, FocusPermissionsDialog(), Scoped DND rule and rollback on leaving a countdown, Release 1.4.0 gate and signed upgrade, Per-account focus preset controls planning and breaks, Seven civil days weekly statistics and on-time streak

### Community 9 - "Spesifikasi scheduler dan timer"
Cohesion: 0.14
Nodes (7): AndroidIntegration, Session, Tokens, Account, AccountUid, string(), TaskAnalysisInput

### Community 10 - "TazerState"
Cohesion: 0.44
Nodes (5): AccountSettings(), ByokForm(), LoginScreen(), SecretField(), TazerState

### Community 12 - "FocusScreens.kt"
Cohesion: 0.12
Nodes (11): DeferFocusDialog(), ExtensionDialog(), focusClock(), FocusRhythm(), FocusScreen(), formatTimer(), isOvertime(), timerText() (+3 more)

### Community 13 - "ReleaseUiTest"
Cohesion: 0.15
Nodes (4): ReleaseUiTest, BriefingSettings, BriefingPreferences, ThemePreference

### Community 15 - "StatisticsScreen.kt"
Cohesion: 0.21
Nodes (6): DailyFocusChart(), duration(), Statistic(), StatisticsHeading(), StatisticsScreen(), toMinutes()

### Community 16 - "FocusEvent"
Cohesion: 0.14
Nodes (9): FocusNotificationTest, FocusEvent, FocusEventKind, ACTIVE, CUTOFF, DONE, EXTEND, REMINDER (+1 more)

### Community 18 - "TazerGlyph"
Cohesion: 0.10
Nodes (20): TazerGlyph, Add, Calendar, Camera, Check, CsvFile, Delete, Down (+12 more)

### Community 19 - "IcsParser"
Cohesion: 0.09
Nodes (18): deleteEvent(), IcsImportPreview, observeEvents(), previewIcs(), IcsCalendar, IcsItem, Event, WeeklyBlock (+10 more)

### Community 20 - "WorkSessionState"
Cohesion: 0.13
Nodes (9): FocusNotificationSoundTest, WorkSessionState, CANCELLED, DONE, EXTENDED, OVERTIME, PAUSED, PENDING (+1 more)

### Community 23 - "TazerApp.kt"
Cohesion: 0.11
Nodes (5): FloatingTimerDeck(), TazerApp(), TazerIcon(), TimerIconButton(), WorkspaceScreen()

### Community 24 - "parseJson"
Cohesion: 0.30
Nodes (5): backendJson(), call(), HttpResponse, parseJson(), parseJsonValue()

### Community 25 - "ScanImageButton.kt"
Cohesion: 0.13
Nodes (7): captureUri(), ImageSource, rememberImageSource(), scanFailureMessage(), ScanImageButton(), ScanReview(), TaskEditor()

### Community 26 - "Claude 5.5 model mapping"
Cohesion: 0.17
Nodes (18): Android :app, APK Debug, Claude 5.5 model mapping, com.tazer.taskorganizer, Compose UI and ViewModel, Graphify, Impeccable, Verifikasi render Android Compose (+10 more)

### Community 27 - "Supabase Auth"
Cohesion: 0.11
Nodes (29): Backend, Server secret store, account_entitlements, analysis_usage, Analyzer input validation, Atomic quota RPC, Entitlement akun dan kuota bulanan, Gemini API Free (+21 more)

### Community 29 - "IntegrationError"
Cohesion: 0.09
Nodes (23): IntegrationError, AI_UNAVAILABLE, AUTH_REJECTED, BROWSER_UNAVAILABLE, BYOK_GEMINI_REQUIRED, BYOK_KEY_MISSING, BYOK_REQUIRED, BYOK_STORAGE_UNAVAILABLE (+15 more)

### Community 30 - "WorkSession"
Cohesion: 0.14
Nodes (31): CalendarEvent, CalendarEvent(ownerUid,startEpochDay), CalendarEvent.startEpochDay dan endEpochDay, CalendarEvent.startMinute, FixedSchedule, Kalender lokal, Migrasi Room v1 ke v2, Migrasi Room v2 ke v3 (+23 more)

### Community 31 - "TazerDatabase"
Cohesion: 0.10
Nodes (16): SyncCursor, SyncDevice, SyncEntry, SyncJournal, TazerDatabase, Callback, Optional analyzer subtasks negotiation, Cloud Sync Lamport version and writer tie breaker (+8 more)

### Community 32 - "FocusDnd.kt"
Cohesion: 0.15
Nodes (7): FocusDnd, FocusDndAction, focusDndAction(), INSTALL, KEEP, WITHDRAW, FocusDndStateTest

### Community 33 - "PendingAuth"
Cohesion: 0.08
Nodes (9): AuthFlow, GOOGLE, RECOVERY, SIGN_UP, AuthPkce, isAuthCallback(), PendingAuth, AuthPkceTest (+1 more)

### Community 34 - "TazerViewModel.kt"
Cohesion: 0.13
Nodes (11): AnalysisSource, AI, OFFLINE, SubtaskDraft, ChecklistItem, checklistItem(), AnalysisSuggestion, asDraft() (+3 more)

### Community 35 - "DateTimeButtons.kt"
Cohesion: 0.13
Nodes (6): NumberWheel(), PickerDateButton(), PickerTimeButton(), platformTheme(), TimeSpinner(), formatMinute()

### Community 36 - "Signed APK Release"
Cohesion: 0.16
Nodes (14): APK sideload, GitHub Releases gratis, Node.js + TypeScript, Rp 0 free tier, Signed APK Release, Cloudflare Workers Free, Web Fetch API, apksigner verify (+6 more)

### Community 37 - "fail"
Cohesion: 0.14
Nodes (26): analyzeGemini(), analyzeGroq(), analyzeWithProvider(), geminiText(), integerSchema(), schema(), validateByok(), ByokCredential (+18 more)

### Community 39 - "CourseColorStore"
Cohesion: 0.06
Nodes (19): CourseColorStore, CourseColors, CourseSwatch, CourseTone, NoteBlock, Bullet, Heading, Paragraph (+11 more)

### Community 41 - "DATA_AND_API_CONTRACT.md"
Cohesion: 0.09
Nodes (24): Aturan global, Batas modul dan struktur target, Handoff `i-have-adhd`, i-have-adhd handoff, Pemeriksaan sebelum handoff, Ponytail, Tazer (Task Organizer), Tim Orkestra Claude 5.5 (+16 more)

### Community 42 - "WorkSession"
Cohesion: 0.16
Nodes (18): CalendarEvent, FixedSchedule, FixedScheduleKind, BREAK, ROUTINE, Subtask, Task, TaskStatus (+10 more)

### Community 43 - "assertequals"
Cohesion: 0.08
Nodes (7): IcsImportTest, IcsImportCounts, OfflineAnalysis, OfflineAnalyzer, SyncConflictTest, IcsParserTest, OfflineAnalyzerTest

### Community 44 - "SchedulePages.kt"
Cohesion: 0.10
Nodes (11): AgendaPill(), color(), CourseChip(), CourseColorSetting(), courseContainer(), CourseDot(), CoursePalette, paletteContainer() (+3 more)

### Community 46 - "TazerBackup.kt"
Cohesion: 0.20
Nodes (4): BackupCounts, BackupPreview, BackupRepository, TazerBackup

### Community 48 - "package.json"
Cohesion: 0.14
Nodes (15): devDependencies, @types/node@^22.0.0, typescript@~5.9.3, engines, node, name, private, scripts (+7 more)

### Community 50 - "ScheduleDraft"
Cohesion: 0.42
Nodes (4): PreviewRow, ScheduleDraft, SchedulePreview, TableImport

### Community 52 - "Focus timer state machine"
Cohesion: 0.22
Nodes (20): Belum Selesai and Selesai tabs, OVERDUE display status, Task.status, CANCELLED session, DONE task, Durasi fokus penuh, elapsedRealtime, EXTENDED session (+12 more)

### Community 53 - "tazer-v3.sql"
Cohesion: 0.25
Nodes (13): `CalendarEvent`, `FixedSchedule`, IF, index_CalendarEvent_ownerUid_startEpochDay, index_FixedSchedule_ownerUid_dayOfWeek, index_Task_id_ownerUid, index_Task_ownerUid_status_completedAt, index_WorkSession_ownerUid_state_plannedStartAt (+5 more)

### Community 55 - "compilerOptions"
Cohesion: 0.14
Nodes (13): compilerOptions, lib, module, noUncheckedIndexedAccess, noUnusedLocals, noUnusedParameters, outDir, rootDir (+5 more)

### Community 56 - ".factor"
Cohesion: 0.33
Nodes (3): PaceCalibration, PaceSample, PaceCalibrationTest

### Community 57 - "TazerDatabase"
Cohesion: 0.11
Nodes (11): CloudSync, Change, SyncResult, SyncKind, EVENT, FIXED, PROFILE, SESSION (+3 more)

### Community 59 - "20261125000000_entitlement.sql"
Cohesion: 0.24
Nodes (8): Android client, Supabase and providers, Tazer backend, public.account_entitlements, public.analysis_usage, public.tazer_entitlement(), public.tazer_entitlement(), tazer-db

### Community 60 - "tazer-v1.sql"
Cohesion: 0.29
Nodes (11): `FixedSchedule`, IF, index_FixedSchedule_ownerUid_dayOfWeek, index_Task_id_ownerUid, index_Task_ownerUid_status_completedAt, index_WorkSession_ownerUid_state_plannedStartAt, index_WorkSession_taskId_ownerUid, room_master_table (+3 more)

### Community 63 - "ReleaseUiTest.kt"
Cohesion: 0.08
Nodes (5): ActivityMonitor, MainActivity, Factory, OpenDocument iCalendar import and Room journal, Repeat-safe iCalendar import identity

### Community 64 - "public.tazer_sync_pull"
Cohesion: 0.22
Nodes (5): public, public.%I, public.tazer_sync_pull(), public.tazer_sync_push(), auth.users

### Community 66 - "Autentikasi dan endpoint HTTP"
Cohesion: 0.22
Nodes (9): Analyzer, Autentikasi dan endpoint HTTP, Envelope error, GEMINI_MODEL, GET /health, Health readiness (ready), Kontrak data dan API, SUPABASE_PUBLISHABLE_KEY (+1 more)

### Community 68 - "HTTP error envelope"
Cohesion: 0.29
Nodes (7): AI_UNAVAILABLE, EMAIL_UNVERIFIED, HTTP error envelope, INVALID_AUTH, PRO_REQUIRED, QUOTA_EXHAUSTED, VERIFICATION_UNAVAILABLE

### Community 69 - "ImageScan.kt"
Cohesion: 0.19
Nodes (11): compressForScan(), extractWithGemini(), ImageExtraction, imageExtraction(), scanBody(), scanInstructions(), scanSampleSize(), scanSchema() (+3 more)

### Community 70 - "image.ts"
Cohesion: 0.10
Nodes (26): integerBetween(), calendarDate(), clock(), decodedSize(), ImageExtraction, imageExtractionOutput(), imageExtractionSchema, imageInput() (+18 more)

### Community 92 - "ScheduleScan.kt"
Cohesion: 0.42
Nodes (7): extractScheduleWithGemini(), scheduleInstructions(), ScheduleScan, scheduleScan(), scheduleSchema(), Impor jadwal tetap, Gate rilis 1.8.0

### Community 100 - "TaskScreens.kt"
Cohesion: 0.09
Nodes (6): TaskSearch, formatDate(), TaskAction(), TasksScreen(), TaskTag(), Pencarian dan warna mata kuliah

### Community 102 - "Rencana eksekusi Tazer"
Cohesion: 0.22
Nodes (9): Android Keystore AES-GCM, BYOK, Awalan system prompt untuk semua peran, Eskalasi ke Claude Opus 5.5, Rencana eksekusi Tazer, Template handoff, Urutan handoff Wave 1, Wave 0 Blueprint (+1 more)

### Community 105 - "node.ts"
Cohesion: 0.13
Nodes (13): Env, scheduleExtractionSchema, mockDependencies(), bridgeError(), createNodeServer(), loopback, NodeEnv, requestBody() (+5 more)

### Community 114 - "FocusPresetMode"
Cohesion: 0.40
Nodes (4): FocusPresetMode, CUSTOM, DEEP_WORK, POMODORO

### Community 116 - "SessionVault"
Cohesion: 0.14
Nodes (12): credentialStorage(), SessionVault, 1. Login (mengikuti `login-neumorphic-blue-reference.jpg`), 2. Seluruh aplikasi (mengikuti `app-ui-neumorphic-mobile-reference.jpg`, warna diganti biru-putih), 4. Saran per peran, 5. Gerbang persetujuan, Aksesibilitas (wajib, `impeccable` craft floor), Aset (+4 more)

### Community 119 - "WidgetContent.kt"
Cohesion: 0.12
Nodes (5): FocusWidgetTest, TazerThemeMode, DARK, LIGHT, SYSTEM

### Community 120 - "providers.ts"
Cohesion: 0.20
Nodes (18): realHandler(), imageInstructions(), scheduleInstructions(), geminiConfig(), geminiImage(), geminiOutput(), geminiSubtaskSchema, groqOutput() (+10 more)

### Community 121 - "ScheduleScreens.kt"
Cohesion: 0.07
Nodes (16): AgendaItem, CalendarScreen(), CalendarWeek(), MonthPicker(), DateSpinner(), ProfileForm(), ScheduleTag(), SettingsScreen() (+8 more)

### Community 122 - "supabase.ts"
Cohesion: 0.24
Nodes (12): Analysis, analysisOutput(), analysisSubtasks(), isUtcTimestamp(), object(), taskInput(), httpsOrigin(), invalidAuthCodes (+4 more)

### Community 123 - "WidgetContent.kt"
Cohesion: 0.17
Nodes (11): FocusLink, FocusLinkKind, ACTIVE, EXTEND, REMINDER, activeWidget(), link(), loadWidgetSnapshot() (+3 more)

### Community 126 - "adapters.test.ts"
Cohesion: 0.22
Nodes (7): analysisSchema, auth, entitlement, env, input, result, suggested

### Community 131 - ".analyze"
Cohesion: 0.33
Nodes (4): AnalyzerMode, BYOK, CLOUD, OFFLINE

### Community 141 - "AuthCallback"
Cohesion: 0.67
Nodes (3): AuthCallback, PasswordRecovery, SignedIn

## Knowledge Gaps
- **126 isolated node(s):** `PasswordRecovery`, `INSTALL`, `KEEP`, `WITHDRAW`, `CUSTOM` (+121 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 463 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **98 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `AndroidIntegration` connect `Spesifikasi scheduler dan timer` to `PendingAuth`, `TazerViewModel.kt`, `TazerViewModel`, `ImageScan.kt`, `ReleaseUpdateSetting.kt`, `ReleaseUiTest`, `CredentialCipher`, `SessionVault`, `AndroidIntegrationTest.kt`, `TazerDatabase`, `SubtaskRepository`, `ReleaseUiTest.kt`?**
  _High betweenness centrality (0.065) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `TaskRepository` (e.g. with `.acknowledgementCannotClearAnEditMadeDuringARequest()` and `.queuesOfflineChangesAcrossCloseAndReplaysSixEntityTypesAndTombstones()`) actually correct?**
  _`TaskRepository` has 2 INFERRED edges - model-reasoned connections that need verification._
- **What connects `PasswordRecovery`, `INSTALL`, `KEEP` to the rest of the system?**
  _126 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `handler.ts` be split into smaller, more focused modules?**
  _Cohesion score 0.13105413105413105 - nodes in this community are weakly interconnected._
- **Why does `TazerDatabase` connect `TazerDatabase` to `TaskRepository`, `FocusRuntime`, `TaskRepositoryTest`, `WorkSession`, `assertequals`, `ReleaseUiTest`, `TazerBackup.kt`, `WidgetContent.kt`, `SubtaskRepository`, `TazerDao`, `FocusRuntime.kt`, `WidgetContent.kt`, `TazerDatabase`, `WidgetContent.kt`, `CloudSyncTest.kt`, `FocusStartTest`, `ReleaseUiTest.kt`?**
  _High betweenness centrality (0.062) - this node is a cross-community bridge._
- **Are the 7 inferred relationships involving `FocusRuntime` (e.g. with `FocusAlarms` and `FocusDnd`) actually correct?**
  _`FocusRuntime` has 7 INFERRED edges - model-reasoned connections that need verification._
- **Should `TaskRepository` be split into smaller, more focused modules?**
  _Cohesion score 0.0538497874350496 - nodes in this community are weakly interconnected._