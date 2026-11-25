# Graph Report - Project-B  (2026-11-25)

## Corpus Check
- Corpus is ~14,513 words - fits in a single context window. You may not need a graph.

## Summary
- 359 nodes · 691 edges · 16 communities (11 shown, 5 thin omitted)
- Extraction: 97% EXTRACTED · 3% INFERRED · 0% AMBIGUOUS · INFERRED: 19 edges (avg confidence: 0.88)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- handler.ts
- Spesifikasi scheduler dan timer
- Task
- Supabase Auth
- TazerViewModel
- Aturan global
- TazerDao
- TaskRepositoryTest
- package.json
- .analyze()
- compilerOptions
- WorkSessionState
- public.tazer_entitlement()

## God Nodes (most connected - your core abstractions)
1. `Task` - 22 edges
2. `TaskRepository` - 20 edges
3. `Spesifikasi scheduler dan timer` - 17 edges
4. `TazerDao` - 15 edges
5. `TaskRepositoryTest` - 14 edges
6. `createHandler()` - 14 edges
7. `TazerDatabase` - 13 edges
8. `FixedSchedule` - 12 edges
9. `compilerOptions` - 12 edges
10. `UserProfile` - 11 edges

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

## Communities (16 total, 5 thin omitted)

### Community 0 - "handler.ts"
Cohesion: 0.07
Nodes (46): Analysis, analysisOutput(), analysisSchema, integerBetween(), isUtcTimestamp(), object(), TaskInput, taskInput() (+38 more)

### Community 1 - "Spesifikasi scheduler dan timer"
Cohesion: 0.06
Nodes (62): Android :app, Android data layer, Android timer layer, APK Debug, com.tazer.taskorganizer, Compose UI and ViewModel, Graphify, Kotlin domain (+54 more)

### Community 2 - "Task"
Cohesion: 0.12
Nodes (13): AnalysisSource, AI, OFFLINE, FixedSchedule, FixedScheduleKind, BREAK, ROUTINE, Task (+5 more)

### Community 3 - "Supabase Auth"
Cohesion: 0.09
Nodes (33): Backend, Server secret store, account_entitlements, AI_UNAVAILABLE, analysis_usage, Analyzer input validation, Atomic quota RPC, EMAIL_UNVERIFIED (+25 more)

### Community 4 - "TazerViewModel"
Cohesion: 0.11
Nodes (18): TaskStatus, DONE, IN_PROGRESS, PENDING, MainActivity, Factory, formatDate(), LoginScreen() (+10 more)

### Community 5 - "Aturan global"
Cohesion: 0.09
Nodes (23): APK sideload, Aturan global, Batas modul dan struktur target, GitHub Releases gratis, Handoff `i-have-adhd`, i-have-adhd handoff, Node.js + TypeScript, Pemeriksaan sebelum handoff (+15 more)

### Community 8 - "package.json"
Cohesion: 0.14
Nodes (15): devDependencies, @types/node@^22.0.0, typescript@~5.9.3, engines, node, name, private, scripts (+7 more)

### Community 9 - ".analyze()"
Cohesion: 0.37
Nodes (3): OfflineAnalysis, OfflineAnalyzer, OfflineAnalyzerTest

### Community 10 - "compilerOptions"
Cohesion: 0.14
Nodes (13): compilerOptions, lib, module, noUncheckedIndexedAccess, noUnusedLocals, noUnusedParameters, outDir, rootDir (+5 more)

### Community 11 - "WorkSessionState"
Cohesion: 0.29
Nodes (7): WorkSessionState, CANCELLED, DONE, EXTENDED, OVERTIME, PENDING, RUNNING

### Community 12 - "public.tazer_entitlement()"
Cohesion: 0.53
Nodes (3): public.account_entitlements, public.analysis_usage, public.tazer_entitlement()

## Knowledge Gaps
- **37 isolated node(s):** `PENDING`, `IN_PROGRESS`, `DONE`, `OFFLINE`, `AI` (+32 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 94 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **5 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Spesifikasi scheduler dan timer` connect `Spesifikasi scheduler dan timer` to `Aturan global`?**
  _High betweenness centrality (0.166) - this node is a cross-community bridge._
- **What connects `PENDING`, `IN_PROGRESS`, `DONE` to the rest of the system?**
  _37 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `handler.ts` be split into smaller, more focused modules?**
  _Cohesion score 0.06965174129353234 - nodes in this community are weakly interconnected._
- **Why does `FixedSchedule` connect `Task` to `Spesifikasi scheduler dan timer`, `TazerDao`?**
  _High betweenness centrality (0.153) - this node is a cross-community bridge._
- **Should `Spesifikasi scheduler dan timer` be split into smaller, more focused modules?**
  _Cohesion score 0.06240084611316764 - nodes in this community are weakly interconnected._
- **Why does `Task` connect `Task` to `TazerViewModel`, `TazerDao`, `TaskRepositoryTest`?**
  _High betweenness centrality (0.138) - this node is a cross-community bridge._
- **Should `Task` be split into smaller, more focused modules?**
  _Cohesion score 0.11538461538461539 - nodes in this community are weakly interconnected._
> Penggunaan token ekstraksi Markdown tidak tersedia dari runtime. Angka nol adalah nilai default.
