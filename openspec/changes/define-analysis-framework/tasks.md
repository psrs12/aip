## 1. Module and Contract Foundations

- [ ] 1.1 Create the `aip-analysis` module (Java 21+, Maven), depending on `aip-core` only, per `design.md` Decision 1.
- [ ] 1.2 Add a dependency-graph CI check (mirroring `aip-csm-builder`'s own) that fails the build if `aip-analysis` gains any non-test dependency outside `aip-core` — in particular, no dependency on `aip-csm-builder`, `aip-analyzer`, or any Policy/Rule Model module. Depends on: 1.1.
- [ ] 1.3 Define the `CsmSnapshotSource` contract in `aip-core` (`aip.core.csm`): CSM elements, CSM relationships, and a stable snapshot identity (repository identifier plus a value distinguishing it from other snapshots of the same repository), per `design.md` Decision 1 and `spec.md`'s `CSM Snapshot Source Shape` requirement. Depends on: 1.1.
- [ ] 1.4 Add tests confirming `CsmSnapshotSource` is implementable independent of any specific producer — a hand-built fixture satisfying the contract, distinct from `aip-csm-builder`'s own `Snapshot` type — analyzes identically to any other conforming implementation, per `spec.md`'s `Analysis Framework is agnostic to snapshot production` scenario. Depends on: 1.3.

## 2. Analysis View Construction (`aip-core`)

- [ ] 2.1 Implement `AnalysisView` in `aip-core` (`aip.core.csm`): built from a `CsmSnapshotSource`'s elements/relationships using the existing `SubjectConflictMarker`, producing a per-Subject effective-knowledge-plus-conflict-status (`EFFECTIVE`/`CONFLICTED`) projection, per `design.md` Decision 3 and `spec.md`'s `Analysis View Construction` requirement. Depends on: 1.3.
- [ ] 2.2 Ensure `AnalysisView` introduces no new CSM entity kind, relationship type, or persisted state, and is never itself treated as a durable artifact independent of its source snapshot — per `spec.md`'s `Analysis View Is a Derived Projection, Not a New Canonical Model` requirement. Depends on: 2.1.
- [ ] 2.3 Add tests confirming: effective knowledge is correctly reflected for an uncontested Subject; conflicting same-category assertions are marked `CONFLICTED` with every assertion preserved; two constructions from the same snapshot content produce equivalent views; the Analysis View is never independently persisted. Depends on: 2.1, 2.2.

## 3. Analyzer Contract and Registry

- [ ] 3.1 Define the Analyzer contract (`aip-analysis`): a stable identifier, a monotonically versioned identifier, and an Analysis Scope declaration (see Section 4), producing exactly one `AnalysisResult` per applicable Analysis Scope instance given an `AnalysisView`, per `design.md` Decisions 2 and 7 and `spec.md`'s `Analyzer Contract` requirement. Depends on: 2.1.
- [ ] 3.2 Implement the Analyzer Registry: registration API, accepting no ordering or dependency declaration between Analyzers, per `design.md` Decision 2 and `spec.md`'s `Analyzer Independence` requirement. Depends on: 3.1.
- [ ] 3.3 Add contract tests for the Analyzer interface itself (identifier/version/scope obligations), independent of any specific Analyzer implementation. Depends on: 3.1, 3.2.
- [ ] 3.4 Add a test proving Analyzer independence: an Analyzer's result is unaffected by which other Analyzers are registered, and no declared inter-Analyzer dependency is honored even if supplied. Depends on: 3.2.

## 4. Analysis Scope Declaration and Applicability

- [ ] 4.1 Implement Analysis Scope as a declared, non-empty set of CSM entity kinds and/or relationship types, with an optional containment-level anchor, per `design.md` Decision 7 and `spec.md`'s `Analysis Scope Declaration` requirement. Depends on: 3.1.
- [ ] 4.2 Implement kind-based applicability: an Analyzer is applicable to a given Analysis Scope instance if, and only if, at least one declared kind is present in that instance's CSM content; an inapplicable Analyzer is skipped without error, per `spec.md`'s `Kind-Based Analyzer Applicability` requirement. Depends on: 4.1.
- [ ] 4.3 Implement scope-instance enumeration: an unanchored Analyzer is invoked exactly once, covering the whole repository; an anchored Analyzer is invoked once per matching contained element present in the CSM Snapshot. Depends on: 4.1.
- [ ] 4.4 Implement native-attribute applicability refinement: an Analyzer MAY layer a predicate over native-evidence-attribute content already present within its declared kind-based scope, without introducing a separate scope-declaration or registration mechanism, per `spec.md`'s `Native-Attribute Applicability Refinement` requirement. Depends on: 4.2.
- [ ] 4.5 Add tests for: kind present/absent applicability, unanchored single invocation, anchored per-element invocation, and native-attribute-refined applicability (matching and non-matching). Depends on: 4.1–4.4.

## 5. Analysis Orchestrator

- [ ] 5.1 Implement the Analysis Orchestrator: constructs exactly one `AnalysisView` per run before invoking any Analyzer, then dispatches every applicable (Analyzer, Analysis Scope instance) pair, per `spec.md`'s `Analysis View Construction` and `Analyzer Contract` requirements. Depends on: 2.1, 3.2, 4.2, 4.3.
- [ ] 5.2 Implement v1 dispatch as a simple sequential loop — no concurrency infrastructure (thread pool, executor service) introduced in this version, per `design.md` Decision 6. Depends on: 5.1.
- [ ] 5.3 Ensure the Analyzer contract and dispatch loop impose no ordering dependency between Analyzers — an Analyzer's result SHALL NOT depend on invocation order or on sequential-vs-concurrent execution, per `spec.md`'s `Analyzer Execution Order Independence` requirement. Depends on: 5.1, 5.2.
- [ ] 5.4 Add a test confirming identical Analyzer results across two runs with reversed relative invocation order between two applicable Analyzers. Depends on: 5.3.

## 6. Analysis Result Identity and Traceability

- [ ] 6.1 Implement deterministic `AnalysisResult` identity as a pure function of (Analyzer identifier, Analyzer version, source CSM Snapshot identity, Analysis Scope instance) — never random or otherwise non-reproducible, per `design.md` Decision 4 and `spec.md`'s `Deterministic Analysis Result Identity` requirement. Depends on: 5.1.
- [ ] 6.2 Implement `AnalysisResult` traceability: a reference to the producing Analyzer's identifier and version, a reference to the source CSM Snapshot's identity, and the Analysis Scope instance covered, per `spec.md`'s `Analysis Result Traceability` requirement. Depends on: 6.1.
- [ ] 6.3 Add identity-stability tests: identical inputs across two separate runs produce identical Result identity; a differing Analyzer version yields a distinguishable identity with the Analysis Scope instance and source snapshot identity otherwise unchanged. Depends on: 6.1.
- [ ] 6.4 Add traceability tests confirming a Result is traceable to both its producing Analyzer/version and its source CSM Snapshot identity by inspection alone. Depends on: 6.2.

## 7. Analysis Result Payload

- [ ] 7.1 Implement `AnalysisResult`'s substantive content as an opaque, Analyzer-defined payload the framework never constrains, interprets, or validates beyond the traceability/referential-integrity expectations defined elsewhere — mirroring CSM's own `NativeAttributes` escape hatch, per `design.md` Decision 4 and `spec.md`'s `Analysis Result Payload Is Analyzer-Defined` requirement. Depends on: 6.1.
- [ ] 7.2 Add tests confirming two Analyzers with unrelated internal payload shapes are both accepted, and that framework storage/retrieval/validation operations never require interpreting payload content. Depends on: 7.1.

## 8. Analysis Result Store Abstraction

- [ ] 8.1 Define an `AnalysisResultStore` abstraction (interface) — write, read-by-identity, list-by-(Analyzer, snapshot) — so the persistence mechanism can be replaced later without changing Analysis Framework's own orchestration logic, per `design.md` Decision 4 (mirroring `aip-csm-builder`'s own `SnapshotStore` abstraction). Depends on: 6.1.
- [ ] 8.2 Implement Analysis Results as durable, individually identifiable artifacts retrievable independent of the producing process, per `spec.md`'s `Analysis Results Are Durable, Individually Identifiable Artifacts` requirement. The concrete persistence technology is explicitly deferred (`design.md` Non-Goals; Explore decision 2) — implement against the `AnalysisResultStore` abstraction only, not a chosen technology. Depends on: 8.1.
- [ ] 8.3 Add tests, written against the `AnalysisResultStore` abstraction rather than any concrete implementation, confirming a Result remains retrievable after its producing run ends and that a new Result does not overwrite or affect a prior, differently-identified one. Depends on: 8.1, 8.2.

## 9. Analysis Result Validation and Publishing

- [ ] 9.1 Implement `AnalysisResultValidator`: referential integrity (every CSM element/Subject identity an `AnalysisResult` references exists within its source CSM Snapshot), Analyzer/version consistency (the declared producer corresponds to a currently registered Analyzer), and scope containment (the Result's content stays within its Analyzer's declared Analysis Scope) — per `design.md` Decision 5 and `spec.md`'s `Analysis Result Validation Before Publication` requirement. Depends on: 6.2, 3.2, 4.1.
- [ ] 9.2 Implement `AnalysisResultPublisher`: gates `AnalysisResultStore.write` so a Result that fails validation is never written at all, mirroring `aip-csm-builder`'s own `SnapshotPublisher`/`CsmValidator` gate pattern. Depends on: 9.1, 8.1.
- [ ] 9.3 Add tests for a passing outcome and for each of the three failing-validation cases (referential integrity, Analyzer/version consistency, scope containment), confirming a failing Result is never published. Depends on: 9.1, 9.2.

## 10. Determinism and Scope Boundary Enforcement

- [ ] 10.1 Add tests/guards confirming no AI/LLM call, heuristic scoring, or probabilistic-inference code path exists anywhere in `aip-analysis`, per `spec.md`'s `Deterministic, Snapshot-Driven Analysis Only` requirement. Depends on: 1.1.
- [ ] 10.2 Add a test confirming repeated analysis over unchanged CSM Snapshot content with the same registered Analyzers produces Analysis Results with identical identity and content. Depends on: 5.1, 6.1.
- [ ] 10.3 Add tests confirming the Analysis Framework never reads Policy/Rule Model content, Runtime Model telemetry, or Repository Evidence directly, per `spec.md`'s `CSM Snapshot as Sole Analysis Input` requirement. Depends on: 5.1.
- [ ] 10.4 Add tests confirming one analysis run's Analysis Results all reference exactly one repository's CSM Snapshot identity, and that a cross-repository External System reference is analyzed only as it appears within the single snapshot being analyzed, per `spec.md`'s `Single-Repository Analysis Scope` requirement. Depends on: 6.2.
- [ ] 10.5 Add tests confirming producing an Analysis Result never alters CSM Snapshot content, and that Analysis Results are stored/queried separately from CSM content, per `spec.md`'s `Analysis Results Are Not CSM Knowledge` requirement. Depends on: 9.2.

## 11. Extension Mechanism Verification

- [ ] 11.1 Confirm and document that registering a new Analyzer for a previously-unused Analysis Scope requires no change to Analysis View construction, Result identity/traceability, or validation mechanisms — mirroring `implement-csm-builder`'s own Extension Mechanism Verification (its Section 20). Depends on: 2.1, 6.1, 9.1.
- [ ] 11.2 Add a test registering a stub/no-op Analyzer for a hypothetical new Analysis Scope, confirming core mechanisms are unaffected. Depends on: 11.1.

## 12. Fixture Layer

- [ ] 12.1 Implement a test-only fixture-building API for `CsmSnapshotSource`/CSM content, analogous to `aip-csm-builder`'s own `RepositoryEvidenceModelBuilder`, using `aip-core` types exclusively. Depends on: 1.3.
- [ ] 12.2 Add a build/lint check confirming the fixture package is test-scope only and is never imported by `aip-analysis`'s production code, mirroring `aip-csm-builder`'s own `check-fixture-package-scope.sh`. Depends on: 12.1.

## 13. Requirement/Scenario Traceability and Coverage

- [ ] 13.1 Build a traceability matrix mapping each of the 18 requirements / 39 scenarios in `specs/analysis-framework/spec.md` to its automated test(s).
- [ ] 13.2 Add end-to-end fixture-based tests running the full Analysis Orchestrator against a representative multi-Analyzer, multi-scope-instance fixture scenario (mirroring `implement-csm-builder`'s own `CsmBuilderFixtureEndToEndTest`). Depends on: 5.1, 4.3, 9.2, 12.1.
- [ ] 13.3 Add a CI check confirming no test or implementation source declares a type duplicating an existing CSM/Evidence contract (e.g. a parallel `Subject`, `CsmElement`, or snapshot type outside `aip-core`), mirroring `implement-csm-builder`'s own `check-no-duplicate-evidence-types.sh`. Depends on: 1.3, 2.1.

## 14. Architectural Invariant Enforcement

- [ ] 14.1 Finalize the dependency-graph CI check enforcing `aip-analysis` depends on `aip-core` only — never `aip-csm-builder`, `aip-analyzer`, or any Policy/Rule Model module. Depends on: 1.2.
- [ ] 14.2 Add a source-scan check confirming no concurrency infrastructure (thread pool, executor service) exists anywhere in `aip-analysis`'s v1 orchestrator, per `design.md` Decision 6. Depends on: 5.2.
- [ ] 14.3 Final gate: confirm (via review checklist and CI) that this change contains no Rule Framework, Finding Model, or Agent Framework implementation, that `canonical-software-model`, `software-repository-understanding`, and `csm-builder` remain unmodified, and that every decision in `design.md` (Decisions 1–7) holds as implemented. Depends on: all preceding sections.
