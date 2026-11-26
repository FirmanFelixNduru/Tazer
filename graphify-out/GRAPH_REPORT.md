# Graph Report - Project-B  (2026-11-27)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 2335 nodes · 6853 edges · 149 communities (63 shown, 86 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 502 edges (avg confidence: 0.87)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `0e59132d`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Spesifikasi scheduler dan timer
- handler.ts
- DailyBriefing.kt
- adapters.test.ts
- WeeklyStatistics.kt
- FocusRuntime
- TaskRepositoryTest
- Spesifikasi scheduler dan timer
- SchedulePages.kt
- ReleaseUiTest
- FocusService.kt
- FocusNotifications
- WidgetContent.kt
- TazerGlyph
- IcsParser
- WorkSessionState
- TazerDao
- TaskPackage
- TazerApp.kt
- parseJson
- TaskScreens.kt
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
- DATA_AND_API_CONTRACT.md
- WorkSession
- ReleaseUiTest.kt
- AccountScreens.kt
- TazerBackup
- TazerState
- package.json
- ByokVault.kt
- Focus timer state machine
- tazer-v3.sql
- compilerOptions
- TazerDatabase
- instant
- 20261125000000_entitlement.sql
- tazer-v1.sql
- ScheduleDraft
- ReleaseUiTest.kt
- public.tazer_sync_pull
- TazerViewModel
- Autentikasi dan endpoint HTTP
- TaskRepository
- HTTP error envelope
- ImageScan.kt
- image.ts
- FocusDndTest.kt
- OfflineAnalyzerTest
- ThemeSetting
- IcsParserTest
- Rencana eksekusi Tazer
- node.ts
- CalendarRepository.kt
- FocusScreen
- SubtaskRepository
- CourseColors
- AndroidIntegrationTest.kt
- WidgetContent.kt
- providers.ts
- ScheduleScreens.kt
- supabase.ts
- SessionVault
- FocusWidgetTest
- .focusAction
- FocusRuntime.kt
- DailyBriefingTest
- TazerViewModel
- CourseColorUi.kt
- TaskEditor

## God Nodes (most connected - your core abstractions)
1. `TazerViewModel` - 100 edges
2. `TaskRepository` - 80 edges
3. `FocusRuntime` - 70 edges
4. `FocusMoment` - 62 edges
5. `ReleaseUiTest` - 59 edges
6. `WorkSession` - 55 edges
7. `AndroidIntegration` - 54 edges
8. `fail()` - 54 edges
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
- `Task checklist: Subtask with composite owner foreign key` --references--> `SubtaskRepository`  [INFERRED]
  docs/DATA_AND_API_CONTRACT.md → android/app/src/main/java/com/tazer/taskorganizer/data/SubtaskRepository.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Transactional work history and replanning** — agents_room_repository, docs_data_and_api_contract_worksession, docs_data_and_api_contract_task, docs_scheduler_spec_smart_shift, docs_scheduler_spec_dynamic_reschedule, docs_scheduler_spec_weighted_time_allocation [EXTRACTED 1.00]
- **Agenda pada kalender lokal** — docs_data_and_api_contract_calendarevent, docs_data_and_api_contract_worksession, docs_data_and_api_contract_fixedschedule, docs_data_and_api_contract_task [EXTRACTED 1.00]
- **Persistensi status, progres, dan sisa waktu saat jeda** — docs_scheduler_spec_paused, docs_scheduler_spec_jeda_dan_lanjutkan, docs_data_and_api_contract_worksession_pausedremainingms, docs_data_and_api_contract_worksession_pausedfromstate, docs_data_and_api_contract_worksession_workbeforesegmentms, docs_data_and_api_contract_worksession_continuousworkbeforesegmentms [INFERRED 0.95]

## Communities (149 total, 86 thin omitted)

### Community 0 - "Spesifikasi scheduler dan timer"
Cohesion: 0.14
Nodes (22): Android data layer, Android timer layer, Claude 5.5 model mapping, Compose UI and ViewModel, Impeccable, Kotlin domain, Room repository, Verifikasi render Android Compose (+14 more)

### Community 1 - "handler.ts"
Cohesion: 0.13
Nodes (17): TaskInput, createHandler(), Dependencies, error(), errors, json(), Readiness, readJson() (+9 more)

### Community 2 - "DailyBriefing.kt"
Cohesion: 0.16
Nodes (11): BriefingKind, EVENING, MORNING, BriefingMessage, BriefingSession, BriefingSettings, BriefingTask, morning() (+3 more)

### Community 3 - "adapters.test.ts"
Cohesion: 0.22
Nodes (7): analysisSchema, auth, entitlement, env, input, result, suggested

### Community 5 - "WeeklyStatistics.kt"
Cohesion: 0.11
Nodes (18): PaceCalibration, PaceSample, civilDate(), CourseFocus, DailyFocus, EstimateAccuracy, onTimeStreak(), StatisticsSession (+10 more)

### Community 7 - "FocusRuntime"
Cohesion: 0.13
Nodes (3): FocusRuntime, FocusSettings, FocusPermissionsDialog()

### Community 9 - "Spesifikasi scheduler dan timer"
Cohesion: 0.17
Nodes (5): AndroidIntegration, Session, Tokens, scanBody(), fail()

### Community 12 - "SchedulePages.kt"
Cohesion: 0.06
Nodes (3): ExportIcsButton(), readDocument(), writeDocument()

### Community 13 - "ReleaseUiTest"
Cohesion: 0.15
Nodes (3): FocusStartTest, ReleaseUiTest, ThemePreference

### Community 16 - "FocusNotifications"
Cohesion: 0.07
Nodes (12): FocusNotificationTest, FocusAlarm, FocusAlarms, FocusEvent, FocusEventKind, ACTIVE, CUTOFF, DONE (+4 more)

### Community 18 - "TazerGlyph"
Cohesion: 0.07
Nodes (28): TazerGlyph, Add, ArrowRight, Calendar, Camera, Check, CsvFile, Delete (+20 more)

### Community 19 - "IcsParser"
Cohesion: 0.11
Nodes (15): nextTrigger(), IcsCalendar, IcsItem, Event, WeeklyBlock, IcsParser, EventLength, EventTime (+7 more)

### Community 20 - "WorkSessionState"
Cohesion: 0.11
Nodes (9): FocusNotificationSoundTest, WorkSessionState, CANCELLED, DONE, EXTENDED, OVERTIME, PAUSED, PENDING (+1 more)

### Community 23 - "TazerApp.kt"
Cohesion: 0.08
Nodes (10): captureUri(), ImageSource, rememberImageSource(), ScanImageButton(), ScanReview(), FloatingTimerDeck(), TazerApp(), TazerIcon() (+2 more)

### Community 24 - "parseJson"
Cohesion: 0.19
Nodes (13): analyzeGemini(), analyzeGroq(), analyzeWithProvider(), geminiText(), integerSchema(), schema(), validateByok(), backendJson() (+5 more)

### Community 25 - "TaskScreens.kt"
Cohesion: 0.13
Nodes (8): TaskQuery, TaskSearch, FilteredEmpty(), TaskFilterBar(), TaskAction(), TasksScreen(), TaskTag(), Pencarian dan warna mata kuliah

### Community 27 - "Supabase Auth"
Cohesion: 0.12
Nodes (26): Backend, Server secret store, account_entitlements, analysis_usage, Analyzer input validation, Atomic quota RPC, Entitlement akun dan kuota bulanan, Gemini API Free (+18 more)

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
Cohesion: 0.14
Nodes (7): FocusDnd, FocusDndAction, focusDndAction(), INSTALL, KEEP, WITHDRAW, FocusDndStateTest

### Community 33 - "PendingAuth"
Cohesion: 0.12
Nodes (9): AuthFlow, GOOGLE, RECOVERY, SIGN_UP, AuthPkce, isAuthCallback(), PendingAuth, AuthPkceTest (+1 more)

### Community 34 - "TazerViewModel.kt"
Cohesion: 0.10
Nodes (11): AnalysisSource, AI, OFFLINE, AnalysisSuggestion, AnalyzerMode, BYOK, CLOUD, OFFLINE (+3 more)

### Community 35 - "DateTimeButtons.kt"
Cohesion: 0.12
Nodes (9): NumberWheel(), PickerDateButton(), PickerTimeButton(), platformTheme(), TimeSpinner(), ProfileForm(), TimeField(), formatMinute() (+1 more)

### Community 36 - "Signed APK Release"
Cohesion: 0.12
Nodes (18): APK sideload, GitHub Releases gratis, Node.js + TypeScript, Rp 0 free tier, Signed APK Release, Cloudflare Workers Free, Web Fetch API, AlarmManager.setExactAndAllowWhileIdle (+10 more)

### Community 37 - "fail"
Cohesion: 0.12
Nodes (21): Account, analysis(), analysisSubtasks(), AuthCallback, PasswordRecovery, SignedIn, ByokProvider, GEMINI (+13 more)

### Community 41 - "DATA_AND_API_CONTRACT.md"
Cohesion: 0.07
Nodes (31): Android :app, APK Debug, Aturan global, Batas modul dan struktur target, com.tazer.taskorganizer, Graphify, Handoff `i-have-adhd`, i-have-adhd handoff (+23 more)

### Community 42 - "WorkSession"
Cohesion: 0.16
Nodes (15): CalendarEvent, FixedSchedule, Subtask, Task, TaskStatus, DONE, IN_PROGRESS, PENDING (+7 more)

### Community 43 - "ReleaseUiTest.kt"
Cohesion: 0.10
Nodes (3): CalendarRepositoryTest, ActivityMonitor, SyncConflictTest

### Community 44 - "AccountScreens.kt"
Cohesion: 0.10
Nodes (22): AmbientOrbs(), LoginScreen(), LogoBadge(), NeuField(), NeuIconButton(), SecretField(), SignInButton(), SignUpButton() (+14 more)

### Community 47 - "TazerState"
Cohesion: 0.27
Nodes (5): AccountSettings(), ByokForm(), SettingsScreen(), TazerState, Workspace

### Community 48 - "package.json"
Cohesion: 0.14
Nodes (15): devDependencies, @types/node@^22.0.0, typescript@~5.9.3, engines, node, name, private, scripts (+7 more)

### Community 51 - "ByokVault.kt"
Cohesion: 0.15
Nodes (4): ByokVault, CredentialCipher, credentialStorage(), AccountUid

### Community 52 - "Focus timer state machine"
Cohesion: 0.22
Nodes (20): Belum Selesai and Selesai tabs, OVERDUE display status, Task.status, CANCELLED session, DONE task, Durasi fokus penuh, elapsedRealtime, EXTENDED session (+12 more)

### Community 53 - "tazer-v3.sql"
Cohesion: 0.25
Nodes (13): `CalendarEvent`, `FixedSchedule`, IF, index_CalendarEvent_ownerUid_startEpochDay, index_FixedSchedule_ownerUid_dayOfWeek, index_Task_id_ownerUid, index_Task_ownerUid_status_completedAt, index_WorkSession_ownerUid_state_plannedStartAt (+5 more)

### Community 55 - "compilerOptions"
Cohesion: 0.14
Nodes (13): compilerOptions, lib, module, noUncheckedIndexedAccess, noUnusedLocals, noUnusedParameters, outDir, rootDir (+5 more)

### Community 57 - "TazerDatabase"
Cohesion: 0.11
Nodes (11): CloudSync, Change, SyncResult, SyncKind, EVENT, FIXED, PROFILE, SESSION (+3 more)

### Community 59 - "20261125000000_entitlement.sql"
Cohesion: 0.24
Nodes (8): Android client, Supabase and providers, Tazer backend, public.account_entitlements, public.analysis_usage, public.tazer_entitlement(), public.tazer_entitlement(), tazer-db

### Community 60 - "tazer-v1.sql"
Cohesion: 0.29
Nodes (11): `FixedSchedule`, IF, index_FixedSchedule_ownerUid_dayOfWeek, index_Task_id_ownerUid, index_Task_ownerUid_status_completedAt, index_WorkSession_ownerUid_state_plannedStartAt, index_WorkSession_taskId_ownerUid, room_master_table (+3 more)

### Community 62 - "ScheduleDraft"
Cohesion: 0.07
Nodes (14): XlsxReader, PreviewRow, ScheduleDraft, SchedulePreview, ScheduleTable, Columns, TableImport, extractScheduleWithGemini() (+6 more)

### Community 63 - "ReleaseUiTest.kt"
Cohesion: 0.09
Nodes (4): networkAvailability(), NetworkCallback, MainActivity, Factory

### Community 64 - "public.tazer_sync_pull"
Cohesion: 0.22
Nodes (5): public, public.%I, public.tazer_sync_pull(), public.tazer_sync_push(), auth.users

### Community 66 - "Autentikasi dan endpoint HTTP"
Cohesion: 0.22
Nodes (9): Analyzer, Autentikasi dan endpoint HTTP, Envelope error, GEMINI_MODEL, GET /health, Health readiness (ready), Kontrak data dan API, SUPABASE_PUBLISHABLE_KEY (+1 more)

### Community 67 - "TaskRepository"
Cohesion: 0.05
Nodes (25): FixedScheduleKind, BREAK, ROUTINE, WorkSession, FocusChange, TaskRepository, continuousWorkMs(), focusAvailableMs() (+17 more)

### Community 68 - "HTTP error envelope"
Cohesion: 0.29
Nodes (7): AI_UNAVAILABLE, EMAIL_UNVERIFIED, HTTP error envelope, INVALID_AUTH, PRO_REQUIRED, QUOTA_EXHAUSTED, VERIFICATION_UNAVAILABLE

### Community 69 - "ImageScan.kt"
Cohesion: 0.19
Nodes (12): ByokCredential, compressForScan(), extractWithGemini(), geminiImageJson(), ImageExtraction, imageExtraction(), scanInstructions(), scanSampleSize() (+4 more)

### Community 70 - "image.ts"
Cohesion: 0.10
Nodes (26): integerBetween(), calendarDate(), clock(), decodedSize(), ImageExtraction, imageExtractionOutput(), imageExtractionSchema, imageInput() (+18 more)

### Community 76 - "OfflineAnalyzerTest"
Cohesion: 0.33
Nodes (3): OfflineAnalysis, OfflineAnalyzer, OfflineAnalyzerTest

### Community 93 - "ThemeSetting"
Cohesion: 0.40
Nodes (3): isDark(), ThemeSetting(), ThemeSystemBars()

### Community 102 - "Rencana eksekusi Tazer"
Cohesion: 0.15
Nodes (13): Android Keystore AES-GCM, BYOK, FREE tier, Kuota bulanan UTC, PRO tier, SubscriptionTier, Awalan system prompt untuk semua peran, Eskalasi ke Claude Opus 5.5 (+5 more)

### Community 105 - "node.ts"
Cohesion: 0.13
Nodes (13): Env, scheduleExtractionSchema, mockDependencies(), bridgeError(), createNodeServer(), loopback, NodeEnv, requestBody() (+5 more)

### Community 113 - "CalendarRepository.kt"
Cohesion: 0.19
Nodes (4): IcsImportTest, deleteEvent(), IcsImportCounts, observeEvents()

### Community 114 - "FocusScreen"
Cohesion: 0.14
Nodes (13): DeferFocusDialog(), ExtensionDialog(), focusClock(), FocusRhythm(), FocusScreen(), formatTimer(), isOvertime(), timerText() (+5 more)

### Community 116 - "CourseColors"
Cohesion: 0.05
Nodes (22): CourseColorStore, CourseColors, CourseSwatch, CourseTone, NoteBlock, Bullet, Heading, Paragraph (+14 more)

### Community 119 - "WidgetContent.kt"
Cohesion: 0.17
Nodes (15): FocusLink, FocusLinkKind, ACTIVE, EXTEND, REMINDER, TazerThemeMode, DARK, LIGHT (+7 more)

### Community 120 - "providers.ts"
Cohesion: 0.20
Nodes (18): realHandler(), imageInstructions(), scheduleInstructions(), geminiConfig(), geminiImage(), geminiOutput(), geminiSubtaskSchema, groqOutput() (+10 more)

### Community 121 - "ScheduleScreens.kt"
Cohesion: 0.05
Nodes (24): AgendaItem, AgendaPill(), CalendarScreen(), CalendarWeek(), MonthPicker(), BriefingRow(), DailyBriefingSetting(), ScheduleAddButton() (+16 more)

### Community 122 - "supabase.ts"
Cohesion: 0.24
Nodes (12): Analysis, analysisOutput(), analysisSubtasks(), isUtcTimestamp(), object(), taskInput(), httpsOrigin(), invalidAuthCodes (+4 more)

### Community 126 - "SessionVault"
Cohesion: 0.17
Nodes (11): SessionVault, 1. Login (mengikuti `login-neumorphic-blue-reference.jpg`), 2. Seluruh aplikasi (mengikuti `app-ui-neumorphic-mobile-reference.jpg`, warna diganti biru-putih), 4. Saran per peran, 5. Gerbang persetujuan, Aksesibilitas (wajib, `impeccable` craft floor), Aset, Referensi visual · Neumorphic biru-putih (+3 more)

### Community 134 - "FocusRuntime.kt"
Cohesion: 0.11
Nodes (5): FocusPresetMode, CUSTOM, DEEP_WORK, POMODORO, FocusReceiver

### Community 142 - "TazerViewModel"
Cohesion: 0.13
Nodes (3): IntegrationException, rejectsSession(), TazerViewModel

### Community 154 - "CourseColorUi.kt"
Cohesion: 0.19
Nodes (7): color(), CourseChip(), CourseColorSetting(), courseContainer(), CourseDot(), CoursePalette, paletteContainer()

### Community 161 - "TaskEditor"
Cohesion: 0.20
Nodes (9): SubtaskDraft, scanFailureMessage(), ChecklistItem, checklistItem(), ImportPackageButton(), TaskEditor(), asDraft(), ChecklistChange (+1 more)

## Knowledge Gaps
- **134 isolated node(s):** `PasswordRecovery`, `ACTIVE`, `EXTEND`, `REMINDER`, `DARK` (+129 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 480 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **86 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `TaskRepository` connect `TaskRepository` to `FocusWidgetTest`, `TazerViewModel.kt`, `FocusRuntime.kt`, `FocusRuntime`, `TaskRepositoryTest`, `ReleaseUiTest.kt`, `ReleaseUiTest`, `TazerViewModel`, `CloudSyncTest.kt`, `TazerDatabase`?**
  _High betweenness centrality (0.076) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `TaskRepository` (e.g. with `.acknowledgementCannotClearAnEditMadeDuringARequest()` and `.queuesOfflineChangesAcrossCloseAndReplaysSixEntityTypesAndTombstones()`) actually correct?**
  _`TaskRepository` has 2 INFERRED edges - model-reasoned connections that need verification._
- **What connects `PasswordRecovery`, `ACTIVE`, `EXTEND` to the rest of the system?**
  _134 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Spesifikasi scheduler dan timer` be split into smaller, more focused modules?**
  _Cohesion score 0.14285714285714285 - nodes in this community are weakly interconnected._
- **Why does `TazerViewModel` connect `TazerViewModel` to `WeeklyStatistics.kt`, `.focusAction`, `FocusRuntime`, `Spesifikasi scheduler dan timer`, `SchedulePages.kt`, `IcsParser`, `TaskPackage`, `TazerApp.kt`, `TaskScreens.kt`, `.change`, `TaskEditor`, `TazerViewModel.kt`, `DateTimeButtons.kt`, `WorkSession`, `AccountScreens.kt`, `TazerBackup`, `TazerState`, `TazerDatabase`, `ReleaseUiTest.kt`, `TazerViewModel`, `TaskRepository`, `FocusScreen`, `SubtaskRepository`, `CourseColors`, `WidgetContent.kt`, `ScheduleScreens.kt`?**
  _High betweenness centrality (0.071) - this node is a cross-community bridge._
- **Are the 7 inferred relationships involving `FocusRuntime` (e.g. with `FocusAlarms` and `FocusDnd`) actually correct?**
  _`FocusRuntime` has 7 INFERRED edges - model-reasoned connections that need verification._
- **Should `handler.ts` be split into smaller, more focused modules?**
  _Cohesion score 0.13105413105413105 - nodes in this community are weakly interconnected._