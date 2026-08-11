# Architectural Invariants 1–10

tasks.md 24.6's final gate requires confirming "all invariants 1–10
hold" — but no single place in this change's planning artifacts ever
enumerated all ten explicitly; they were introduced inline, one at a
time, as `(invariant N)` parenthetical citations scattered across
`tasks.md` as each relevant task was written. This file reconstructs
the full list from those citations (invariants 1–9 are all directly
citable; invariant 10 is inferred — see its own note below) and
confirms, for each, what actually enforces it as of this change's
completion.

| # | Invariant | First cited at | Enforced by |
|---|---|---|---|
| 1 | `aip-csm-builder` depends on `aip-core` only. | tasks.md 1.2 | `scripts/check-module-dependencies.sh`, wired into `aip-csm-builder`'s `verify` phase. |
| 2 | `aip-csm-builder` never depends on `aip-analyzer` or any Repository Understanding implementation module. | tasks.md 1.2 | Same script (both invariants are the same dependency-graph check: an allow-list of exactly `aip:aip-core`). Also structurally true: no such module exists in this repository (`pom.xml`'s `<modules>` lists only `aip-core`, `aip-csm-builder`). |
| 3 | `aip.core.evidence` is the single, authoritative Repository Evidence contract — no duplicate/parallel Evidence type exists anywhere else. | tasks.md 3.6 | `EvidenceVocabularyClosureTest` (vocabulary-closure half) + `scripts/check-no-duplicate-evidence-types.sh` (full-codebase-scan half, tasks.md 24.2), wired into the parent `pom.xml`'s `verify` phase. |
| 4 | The fixture-building API (`aip.csmbuilder.test.fixtures`) is test-scope only; CSM Builder's production code never imports it. | tasks.md 21.1 | `scripts/check-fixture-package-scope.sh`, wired into `aip-csm-builder`'s `verify` phase. |
| 5 | The Snapshot persistence mechanism can be replaced without changing CSM Builder's construction logic. | tasks.md 15.1 | `SnapshotStore` abstraction (interface) + `FilesystemSnapshotStore` as its only implementation; `aip.csmbuilder.mapping` never imports `aip.csmbuilder.snapshot` (see that package's own `package-info.java`). |
| 6 | The Dependency-Kind Classifier is decoupled from any specific mapping-table implementation. | tasks.md 10.1 | `DependencyKindClassifier` abstraction (interface) + `ResourceDependencyKindClassifier` as its only implementation, reading versioned resource files. |
| 7 | The Mapping Orchestrator executes sequentially only — no concurrency infrastructure. | tasks.md 4.3 | `MappingOrchestrator.construct` is a single-threaded `for` loop; no `ExecutorService`, thread pool, or parallel stream appears anywhere in `aip-csm-builder`'s construction path. `MappingOrchestratorTest.repeatedRunsAgainstUnchangedInputProduceIdenticalDispatchOrder` and `.itemsAreDispatchedInDeterministicSortedOrder` confirm the deterministic-order consequence of this. |
| 8 | No test file name implies real-pipeline Repository Understanding integration coverage. | tasks.md 22.2 | `scripts/check-test-naming.sh`, wired into `aip-csm-builder`'s `verify` phase. |
| 9 | The future real-pipeline integration test's name and location are reserved now, undocumented as a written test. | tasks.md 22.3 | `aip.csmbuilder.integration.RepositoryToCsmPipelineIntegrationTest` exists as a real, compiling, no-`@Test`-methods placeholder file. |
| 10 | CSM Builder's construction is deterministic and evidence-driven only: no AI/LLM/heuristic/probabilistic-inference code path, no Architecture Component/Architectural Boundary/Business Context construction, and no dependency on a Policy/Rule Model or Runtime Model. | *Inferred* — see note below | `scripts/check-no-ai-heuristic-imports.sh` + `scripts/check-no-excluded-construction.sh` (both wired into `aip-csm-builder`'s `verify` phase) + `ArchitecturalInvariantTest` (runtime confirmation against the richest available fixture, plus a reflection-based API-surface check) — tasks.md 24.3, 24.4, 24.5. |

## Note on invariant 10

Unlike invariants 1–9, no task or design.md passage ever writes
"(invariant 10)" next to a specific claim. Section 24 has three task
items — 24.3 (no AI/heuristic code), 24.4 (no Architecture
Component/Boundary/Business Context), 24.5 (no Policy/Rule/Runtime
Model dependency) — that cite no invariant number at all, unlike 24.1
and 24.2, which cite invariants 1/2 and 3 explicitly. Since exactly one
invariant number (10) remains unclaimed and exactly three related,
uncited claims remain, the most coherent reading is that these three
were always meant as one compound invariant, mirroring how the
archived `csm-builder` specification itself groups the same three
concerns under two adjacent requirements (`Deterministic,
Evidence-Driven Transformation Only`, `Repository Evidence as Sole
Construction Input`, and `Exclusion of Architectural Inference and
Declared-Knowledge Construction`). This reconstruction is recorded here
explicitly, rather than left implicit, so a future reader does not have
to re-derive it from scattered citations the way this change's
implementation had to.

## Final gate confirmations (tasks.md 24.6)

- **No Repository Understanding implementation code**: confirmed by
  `pom.xml`'s module list (`aip-core`, `aip-csm-builder` only — no
  `aip-analyzer` or RU implementation module exists), and by every
  RU-shaped test input in this change coming from
  `aip.csmbuilder.test.fixtures`, never a real RU pipeline (invariants
  8, 9 above).
- **Both archived specifications remain unmodified**: confirmed via
  `git log --oneline main..HEAD -- openspec/changes/archive/` and `--
  openspec/specs/`, both of which show only the original scaffolding
  commit (`ec6c5cb`, which *created* these files) and no subsequent
  commit from this change's implementation work.
- **All invariants 1–10 hold**: per the table above.
