# Graph Report - Project-B  (2026-11-25)

## Corpus Check
- 47 files · ~26,620 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 15 file(s) not represented in the graph (top: .xml 8, .properties 2, (none) 1)

## Summary
- 804 nodes · 1949 edges · 33 communities (23 shown, 10 thin omitted)
- Extraction: 94% EXTRACTED · 6% INFERRED · 0% AMBIGUOUS · INFERRED: 108 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `1fc839f9`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- handler.ts
- Spesifikasi scheduler dan timer
- TaskRepository
- Supabase Auth
- TazerViewModel
- Aturan global
- TazerDao
- TaskRepositoryTest
- package.json
- .analyze()
- compilerOptions
- AndroidIntegration
- public.tazer_entitlement()
- SchedulerTest
- ByokVault.kt
- IntegrationError
- WorkspaceScreen
- MainActivity.kt
- GPT-6.2 Astra escalation
- Smart Shift
- Rencana eksekusi Tazer
- Focus timer state machine
- TazerState
- SettingsScreen
- HTTP error envelope

## God Nodes (most connected - your core abstractions)
1. `TazerViewModel` - 49 edges
2. `AndroidIntegration` - 41 edges
3. `fail()` - 38 edges
4. `TaskRepository` - 30 edges
5. `SchedulerTest` - 28 edges
6. `TazerDao` - 26 edges
7. `Task` - 24 edges
8. `Scheduler` - 23 edges
9. `TaskRepositoryTest` - 22 edges
10. `IntegrationError` - 22 edges

## Surprising Connections (you probably didn't know these)
- `Slot kosong dan batas sesi` --references--> `FixedSchedule`  [INFERRED]
  docs/SCHEDULER_SPEC.md → android/app/src/main/java/com/tazer/taskorganizer/data/Entities.kt
- `Room` --references--> `UserProfile`  [INFERRED]
  docs/DATA_AND_API_CONTRACT.md → android/app/src/main/java/com/tazer/taskorganizer/data/Entities.kt
- `Room` --references--> `Task`  [INFERRED]
  docs/DATA_AND_API_CONTRACT.md → android/app/src/main/java/com/tazer/taskorganizer/data/Entities.kt
- `Room` --references--> `FixedSchedule`  [INFERRED]
  docs/DATA_AND_API_CONTRACT.md → android/app/src/main/java/com/tazer/taskorganizer/data/Entities.kt
- `Room` --references--> `WorkSession`  [INFERRED]
  docs/DATA_AND_API_CONTRACT.md → android/app/src/main/java/com/tazer/taskorganizer/data/Entities.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Transactional work history and replanning** — agents_room_repository, docs_data_and_api_contract_worksession, docs_data_and_api_contract_task, docs_scheduler_spec_smart_shift, docs_scheduler_spec_dynamic_reschedule, docs_scheduler_spec_weighted_time_allocation [EXTRACTED 1.00]

## Communities (33 total, 10 thin omitted)

### Community 0 - "handler.ts"
Cohesion: 0.07
Nodes (46): Analysis, analysisOutput(), analysisSchema, integerBetween(), isUtcTimestamp(), object(), TaskInput, taskInput() (+38 more)

### Community 1 - "Spesifikasi scheduler dan timer"
Cohesion: 0.11
Nodes (24): Android :app, Android data layer, Android timer layer, APK Debug, com.tazer.taskorganizer, Compose UI and ViewModel, Kotlin domain, Room repository (+16 more)

### Community 2 - "TaskRepository"
Cohesion: 0.16
Nodes (4): FixedScheduleKind, BREAK, ROUTINE, TaskRepository

### Community 3 - "Supabase Auth"
Cohesion: 0.14
Nodes (22): Backend, Server secret store, account_entitlements, analysis_usage, Analyzer input validation, Atomic quota RPC, Entitlement akun dan kuota bulanan, Gemini API Free (+14 more)

### Community 4 - "TazerViewModel"
Cohesion: 0.06
Nodes (18): AnalysisSource, AI, OFFLINE, ByokProvider, GEMINI, GROQ, IntegrationException, SubscriptionTier (+10 more)

### Community 5 - "Aturan global"
Cohesion: 0.15
Nodes (13): Aturan global, Batas modul dan struktur target, Graphify, Handoff `i-have-adhd`, i-have-adhd handoff, Pemeriksaan sebelum handoff, Ponytail, Tazer (Task Organizer) (+5 more)

### Community 6 - "TazerDao"
Cohesion: 0.07
Nodes (19): FixedSchedule, Task, TaskStatus, DONE, IN_PROGRESS, PENDING, UserProfile, WorkSession (+11 more)

### Community 8 - "package.json"
Cohesion: 0.14
Nodes (15): devDependencies, @types/node@^22.0.0, typescript@~5.9.3, engines, node, name, private, scripts (+7 more)

### Community 9 - ".analyze()"
Cohesion: 0.33
Nodes (3): OfflineAnalysis, OfflineAnalyzer, OfflineAnalyzerTest

### Community 10 - "compilerOptions"
Cohesion: 0.14
Nodes (13): compilerOptions, lib, module, noUncheckedIndexedAccess, noUnusedLocals, noUnusedParameters, outDir, rootDir (+5 more)

### Community 11 - "AndroidIntegration"
Cohesion: 0.08
Nodes (25): AndroidIntegration, Session, Tokens, analyzeGemini(), analyzeGroq(), analyzeWithProvider(), integerSchema(), schema() (+17 more)

### Community 12 - "public.tazer_entitlement()"
Cohesion: 0.27
Nodes (6): Android client, Supabase and providers, Tazer backend, public.account_entitlements, public.analysis_usage, public.tazer_entitlement()

### Community 16 - "SchedulerTest"
Cohesion: 0.12
Nodes (10): DeadlineRisk, PlannedWork, SchedulePlan, Scheduler, Option, Work, ScheduleRule, SchedulingTask (+2 more)

### Community 17 - "ByokVault.kt"
Cohesion: 0.06
Nodes (13): AuthFlow, GOOGLE, RECOVERY, SIGN_UP, AuthPkce, isAuthCallback(), PendingAuth, ByokCredential (+5 more)

### Community 19 - "IntegrationError"
Cohesion: 0.06
Nodes (21): AndroidIntegrationTest, backendJson(), HttpResponse, parseJson(), IntegrationError, AI_UNAVAILABLE, AUTH_REJECTED, BYOK_KEY_MISSING (+13 more)

### Community 21 - "WorkspaceScreen"
Cohesion: 0.16
Nodes (11): TimelineRow, TimelineScreen(), formatDate(), formatTime(), schedulingTask(), TaskEditor(), TasksScreen(), SectionIcon() (+3 more)

### Community 23 - "GPT-6.2 Astra escalation"
Cohesion: 0.15
Nodes (14): APK sideload, GitHub Releases gratis, Node.js + TypeScript, Rp 0 free tier, Signed APK Release, Cloudflare Workers Free, Web Fetch API, AlarmManager.setExactAndAllowWhileIdle (+6 more)

### Community 24 - "Smart Shift"
Cohesion: 0.34
Nodes (14): FixedSchedule, Room schema, Task, UserProfile, WorkSession, WorkSession.state, boundedShares, Callback revision check (+6 more)

### Community 25 - "Rencana eksekusi Tazer"
Cohesion: 0.15
Nodes (13): Android Keystore AES-GCM, BYOK, FREE tier, Kuota bulanan UTC, PRO tier, SubscriptionTier, Awalan system prompt untuk semua peran, Eskalasi ke GPT-6.2 Astra (+5 more)

### Community 26 - "Focus timer state machine"
Cohesion: 0.36
Nodes (11): Belum Selesai and Selesai tabs, OVERDUE display status, Task.status, CANCELLED session, DONE task, EXTENDED session, Focus timer state machine, OVERTIME (+3 more)

### Community 27 - "TazerState"
Cohesion: 0.54
Nodes (5): AccountSettings(), ByokForm(), LoginScreen(), SecretField(), TazerState

### Community 28 - "SettingsScreen"
Cohesion: 0.57
Nodes (7): dayName(), FixedEditor(), ProfileForm(), SettingsScreen(), TimeField(), formatMinute(), parseMinute()

### Community 29 - "HTTP error envelope"
Cohesion: 0.29
Nodes (7): AI_UNAVAILABLE, EMAIL_UNVERIFIED, HTTP error envelope, INVALID_AUTH, PRO_REQUIRED, QUOTA_EXHAUSTED, VERIFICATION_UNAVAILABLE

## Knowledge Gaps
- **62 isolated node(s):** `PENDING`, `IN_PROGRESS`, `DONE`, `OFFLINE`, `AI` (+57 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 206 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **10 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `FixedSchedule` connect `TazerDao` to `Spesifikasi scheduler dan timer`, `TaskRepository`, `TazerViewModel`, `TaskRepositoryTest`, `TaskScreens.kt`, `SettingsScreen`?**
  _High betweenness centrality (0.179) - this node is a cross-community bridge._
- **Are the 4 inferred relationships involving `AndroidIntegration` (e.g. with `.configuredLoopbackEnforcesTiersAndChargesOneProRequest()` and `AuthPkce`) actually correct?**
  _`AndroidIntegration` has 4 INFERRED edges - model-reasoned connections that need verification._
- **What connects `PENDING`, `IN_PROGRESS`, `DONE` to the rest of the system?**
  _62 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `handler.ts` be split into smaller, more focused modules?**
  _Cohesion score 0.06559356136820925 - nodes in this community are weakly interconnected._
- **Why does `Spesifikasi scheduler dan timer` connect `Spesifikasi scheduler dan timer` to `Smart Shift`, `Focus timer state machine`, `Aturan global`, `GPT-6.2 Astra escalation`?**
  _High betweenness centrality (0.150) - this node is a cross-community bridge._
- **Are the 30 inferred relationships involving `fail()` (e.g. with `.configuredLoopbackEnforcesTiersAndChargesOneProRequest()` and `.analyzeByok()`) actually correct?**
  _`fail()` has 30 INFERRED edges - model-reasoned connections that need verification._
- **Should `Spesifikasi scheduler dan timer` be split into smaller, more focused modules?**
  _Cohesion score 0.11231884057971014 - nodes in this community are weakly interconnected._