## 1. Module and Contract Foundations

- [ ] 1.1 Create the `aip-analysis` module (Java 21+, Maven), depending on `aip-core` only, per `define-analysis-framework/design.md` Decision 1.
- [ ] 1.2 Add a dependency-graph CI check (mirroring `aip-csm-builder`'s own) that fails the build if `aip-analysis` gains any non-test dependency outside `aip-core` — in particular, no dependency on `aip-csm-builder`, `aip-analyzer`, or any Policy/Rule Model module, per this change's own `design.md` Binding Decision 2 (no real `aip-csm-builder` adapter). Depends on: 1.1.
- [ ] 1.3 Define `CsmSnapshotId` in `aip-core` (`aip.core.csm`): a record of `(repositoryIdentifier: String, sequenceNumber: long)`, structurally aligned with but independent of `aip-csm-builder`'s own `Snapshot` identity shape, per `design.md` Decision 3. Depends on: 1.1.
- [ ] 1.4 Define the `CsmSnapshotSource` contract in `aip-core` (`aip.core.csm`): `elements(): Set<CsmElement>`, `relationships(): Set<CsmRelationship>`, `id(): CsmSnapshotId`, per `design.md` Decision 3 and `define-analysis-framework/spec.md`'s `CSM Snapshot Source Shape` requirement. Depends on: 1.3.
- [ ] 1.5 Add tests confirming `CsmSnapshotSource` is implementable independent of any specific producer — a hand-built fixture satisfying the contract, distinct from `aip-csm-builder`'s own `Snapshot` type — analyzes identically to any other conforming implementation, per `spec.md`'s `Analysis Framework is agnostic to snapshot production` scenario. Depends on: 1.4.

## 2. Shared Scope Declaration (`aip-core`)

- [ ] 2.1 Define `CsmScope` in `aip-core` (`aip.core.csm`): a declared, non-empty set of `CsmEntityKind` and/or `CsmRelationshipType` values, an optional containment-level anchor (`CsmEntityKind`), and an optional native-attribute predicate, per `design.md` Decision 2 — the general Scope declaration type `define-rule-framework/design.md` Decision 3 already commits to sharing with a future Rule Scope. Depends on: 1.1.
- [ ] 2.2 Add tests confirming `CsmScope` correctly represents: kind-only declarations, an anchored declaration, and a native-attribute-refined declaration — independent of any consumer (Analyzer or, later, Rule Type). Depends on: 2.1.

## 3. Analysis View Construction (`aip-core`)

- [ ] 3.1 Implement `AnalysisView` in `aip-core` (`aip.core.csm`): built from a `CsmSnapshotSource`'s elements/relationships using the existing `SubjectConflictMarker`, producing a per-Subject effective-knowledge-plus-conflict-status (`EFFECTIVE`/`CONFLICTED`) projection, per `define-analysis-framework/design.md` Decision 3 and `spec.md`'s `Analysis View Construction` requirement. Depends on: 1.4.
- [ ] 3.2 Ensure `AnalysisView` introduces no new CSM entity kind, relationship type, or persisted state, and is never itself treated as a durable artifact independent of its source snapshot — per `spec.md`'s `Analysis View Is a Derived Projection, Not a New Canonical Model` requirement. Depends on: 3.1.
- [ ] 3.3 Implement `AnalysisView`'s exposure of its source CSM Snapshot's own identity (`CsmSnapshotId`), per `define-analysis-framework/spec.md`'s amended `Analysis View Construction` requirement (the addendum made while specifying Rule Framework). Depends on: 3.1, 1.3.
- [ ] 3.4 Add tests confirming: effective knowledge is correctly reflected for an uncontested Subject; conflicting same-category assertions are marked `CONFLICTED` with every assertion preserved; two constructions from the same snapshot content produce equivalent views; the Analysis View is never independently persisted; the source CSM Snapshot identity is correctly exposed. Depends on: 3.1, 3.2, 3.3.

## 4. Analyzer Contract and Registry (`aip-analysis`)

- [ ] 4.1 Define the Analyzer contract (`aip-analysis`): a stable identifier, a monotonically versioned identifier, and a `CsmScope` declaration (Section 2), producing exactly one `AnalysisResult` per applicable Scope instance given an `AnalysisView`, per `define-analysis-framework/design.md` Decisions 2 and 7 and `spec.md`'s `Analyzer Contract` requirement. Depends on: 3.1, 2.1.
- [ ] 4.2 Implement the Analyzer Registry: registration API, accepting no ordering or dependency declaration between Analyzers, per `design.md` Decision 2 and `spec.md`'s `Analyzer Independence` requirement. Depends on: 4.1.
- [ ] 4.3 Add contract tests for the Analyzer interface itself (identifier/version/scope obligations), independent of any specific Analyzer implementation. Depends on: 4.1, 4.2.
- [ ] 4.4 Add a test proving Analyzer independence: an Analyzer's result is unaffected by which other Analyzers are registered, and no declared inter-Analyzer dependency is honored even if supplied. Depends on: 4.2.

## 5. Scope Applicability

- [ ] 5.1 Implement kind-based applicability: an Analyzer is applicable to a given `CsmScope` instance if, and only if, at least one declared kind is present in that instance's CSM content; an inapplicable Analyzer is skipped without error, per `spec.md`'s `Kind-Based Analyzer Applicability` requirement. Depends on: 2.1.
- [ ] 5.2 Implement scope-instance enumeration: an unanchored Analyzer is invoked exactly once, covering the whole repository; an anchored Analyzer is invoked once per matching contained element present in the CSM Snapshot. Depends on: 2.1.
- [ ] 5.3 Implement native-attribute applicability refinement: an Analyzer MAY layer a predicate over native-evidence-attribute content already present within its declared kind-based scope, without introducing a separate scope-declaration or registration mechanism, per `spec.md`'s `Native-Attribute Applicability Refinement` requirement. Depends on: 5.1.
- [ ] 5.4 Add tests for: kind present/absent applicability, unanchored single invocation, anchored per-element invocation, and native-attribute-refined applicability (matching and non-matching). Depends on: 5.1-5.3.

## 6. Analysis Orchestrator (`aip-analysis`)

- [ ] 6.1 Implement the Analysis Orchestrator: constructs exactly one `AnalysisView` per run before invoking any Analyzer, then dispatches every applicable (Analyzer, Scope instance) pair, per `spec.md`'s `Analysis View Construction` and `Analyzer Contract` requirements. Depends on: 3.1, 4.2, 5.2, 5.3.
- [ ] 6.2 Implement v1 dispatch as a simple sequential loop — no concurrency infrastructure (thread pool, executor service) introduced in this version, per `define-analysis-framework/design.md` Decision 6. Depends on: 6.1.
- [ ] 6.3 Ensure the Analyzer contract and dispatch loop impose no ordering dependency between Analyzers — an Analyzer's result SHALL NOT depend on invocation order or on sequential-vs-concurrent execution, per `spec.md`'s `Analyzer Execution Order Independence` requirement. Depends on: 6.1, 6.2.
- [ ] 6.4 Add a test confirming identical Analyzer results across two runs with reversed relative invocation order between two applicable Analyzers. Depends on: 6.3.

## 7. `AnalysisResult` Identity and Traceability (`aip-core`)

- [ ] 7.1 Define `AnalysisResult` as a final (non-sealed, non-generic) class in `aip-core` (`aip.core.csm`), per this change's `design.md` Decision 1 (Binding Decision 1's concrete shape): identity (`AnalysisResultId`, Section 7.2), producing Analyzer identifier/version, source `CsmSnapshotId`, the `CsmScope` instance covered, and an opaque `Object` payload field. Depends on: 6.1, 1.3, 2.1.
- [ ] 7.2 Define `AnalysisResultId` in `aip-core`: a deterministic function of (Analyzer identifier, Analyzer version, `CsmSnapshotId`, Scope instance) — never random or otherwise non-reproducible, per `design.md` Decision 4 and `define-analysis-framework/design.md` Decision 4 and `spec.md`'s `Deterministic Analysis Result Identity` requirement. Depends on: 7.1.
- [ ] 7.3 Implement `AnalysisResult` traceability: a reference to the producing Analyzer's identifier and version, a reference to the source `CsmSnapshotId`, and the Scope instance covered — all directly inspectable fields, per `spec.md`'s `Analysis Result Traceability` requirement. Depends on: 7.1.
- [ ] 7.4 Add identity-stability tests: identical inputs across two separate runs produce identical `AnalysisResultId`; a differing Analyzer version yields a distinguishable identity with the Scope instance and source snapshot identity otherwise unchanged. Depends on: 7.2.
- [ ] 7.5 Add traceability tests confirming a Result is traceable to both its producing Analyzer/version and its source CSM Snapshot identity by inspection alone. Depends on: 7.3.

## 8. `AnalysisResult` Payload

- [ ] 8.1 Implement `AnalysisResult`'s `Object` payload as an opaque, Analyzer-defined value the framework never constrains, interprets, or validates beyond the traceability/referential-integrity expectations defined elsewhere — mirroring CSM's own `NativeAttributes` escape hatch, per `design.md` Decision 1 and `define-analysis-framework/design.md` Decision 4 and `spec.md`'s `Analysis Result Payload Is Analyzer-Defined` requirement. Depends on: 7.1.
- [ ] 8.2 Add tests confirming two Analyzers with unrelated internal payload shapes are both accepted, and that framework storage/retrieval/validation operations never require interpreting payload content. Depends on: 8.1.

## 9. `AnalysisResultStore` Abstraction (`aip-analysis`)

- [ ] 9.1 Define an `AnalysisResultStore` abstraction (interface) **in `aip-analysis`, not `aip-core`** — write, read-by-identity, list-by-(Analyzer, snapshot) — per this change's `design.md` Decision 5 (mirroring `SnapshotStore`'s own placement in `aip-csm-builder`, not `aip-core`) and `define-analysis-framework/design.md` Decision 4. Depends on: 7.1.
- [ ] 9.2 Implement Analysis Results as durable, individually identifiable artifacts retrievable independent of the producing process, per `spec.md`'s `Analysis Results Are Durable, Individually Identifiable Artifacts` requirement. The concrete persistence technology is explicitly deferred — implement against the `AnalysisResultStore` abstraction only. Depends on: 9.1.
- [ ] 9.3 Add tests, written against the `AnalysisResultStore` abstraction rather than any concrete implementation, confirming a Result remains retrievable after its producing run ends and that a new Result does not overwrite or affect a prior, differently-identified one. Depends on: 9.1, 9.2.

## 10. Analysis Result Validation and Publishing (`aip-analysis`)

- [ ] 10.1 Implement `AnalysisResultValidator`: referential integrity (every CSM element/Subject identity an `AnalysisResult` references exists within its source CSM Snapshot), Analyzer/version consistency (the declared producer corresponds to a currently registered Analyzer), and scope containment (the Result's content stays within its Analyzer's declared `CsmScope`) — per `define-analysis-framework/design.md` Decision 5 and `spec.md`'s `Analysis Result Validation Before Publication` requirement. Depends on: 7.3, 4.2, 2.1.
- [ ] 10.2 Implement `AnalysisResultPublisher`: gates `AnalysisResultStore.write` so a Result that fails validation is never written at all, mirroring `aip-csm-builder`'s own `SnapshotPublisher`/`CsmValidator` gate pattern. Depends on: 10.1, 9.1.
- [ ] 10.3 Add tests for a passing outcome and for each of the three failing-validation cases (referential integrity, Analyzer/version consistency, scope containment), confirming a failing Result is never published. Depends on: 10.1, 10.2.

## 11. Determinism and Scope Boundary Enforcement

- [ ] 11.1 Add tests/guards confirming no AI/LLM call, heuristic scoring, or probabilistic-inference code path exists anywhere in `aip-analysis`, per `spec.md`'s `Deterministic, Snapshot-Driven Analysis Only` requirement. Depends on: 1.1.
- [ ] 11.2 Add a test confirming repeated analysis over unchanged CSM Snapshot content with the same registered Analyzers produces Analysis Results with identical identity and content. Depends on: 6.1, 7.2.
- [ ] 11.3 Add tests confirming the Analysis Framework never reads Policy/Rule Model content, Runtime Model telemetry, or Repository Evidence directly, per `spec.md`'s `CSM Snapshot as Sole Analysis Input` requirement. Depends on: 6.1.
- [ ] 11.4 Add tests confirming one analysis run's Analysis Results all reference exactly one repository's CSM Snapshot identity, and that a cross-repository External System reference is analyzed only as it appears within the single snapshot being analyzed, per `spec.md`'s `Single-Repository Analysis Scope` requirement. Depends on: 7.3.
- [ ] 11.5 Add tests confirming producing an Analysis Result never alters CSM Snapshot content, and that Analysis Results are stored/queried separately from CSM content, per `spec.md`'s `Analysis Results Are Not CSM Knowledge` requirement. Depends on: 10.2.

## 12. Extension Mechanism Verification

- [ ] 12.1 Confirm and document that registering a new Analyzer for a previously-unused `CsmScope` requires no change to Analysis View construction, Result identity/traceability, or validation mechanisms — mirroring `implement-csm-builder`'s own Extension Mechanism Verification (its Section 20). Depends on: 3.1, 7.2, 10.1.
- [ ] 12.2 Add a test registering a stub/no-op Analyzer for a hypothetical new `CsmScope`, confirming core mechanisms are unaffected. Depends on: 12.1.

## 13. Fixture Layer

- [ ] 13.1 Implement a test-only fixture-building API for `CsmSnapshotSource`/CSM content, analogous to `aip-csm-builder`'s own `RepositoryEvidenceModelBuilder`, using `aip-core` types exclusively, per `design.md` Decision 6 and this change's Binding Decision 2 (no real `aip-csm-builder` adapter). Depends on: 1.4.
- [ ] 13.2 Add a build/lint check confirming the fixture package is test-scope only and is never imported by `aip-analysis`'s production code, mirroring `aip-csm-builder`'s own `check-fixture-package-scope.sh`. Depends on: 13.1.

## 14. Requirement/Scenario Traceability and Coverage

- [ ] 14.1 Build a traceability matrix mapping each of the 18 requirements / 39 scenarios in `define-analysis-framework/specs/analysis-framework/spec.md` to its automated test(s), following `implement-csm-builder`'s own `traceability.md` precedent. Depends on: Sections 1-13.
- [ ] 14.2 Add end-to-end fixture-based tests running the full Analysis Orchestrator against a representative multi-Analyzer, multi-scope-instance fixture scenario (mirroring `implement-csm-builder`'s own `CsmBuilderFixtureEndToEndTest`), per `design.md` Decision 7. Depends on: 6.1, 5.2, 10.2, 13.1.
- [ ] 14.3 Add a CI check confirming no test or implementation source declares a type duplicating an existing CSM/Evidence contract (e.g. a parallel `Subject`, `CsmElement`, `AnalysisResult`, or snapshot type outside `aip-core`), mirroring `implement-csm-builder`'s own `check-no-duplicate-evidence-types.sh`. Depends on: 1.4, 3.1, 7.1.

## 15. Architectural Invariant Enforcement

- [ ] 15.1 Finalize the dependency-graph CI check enforcing `aip-analysis` depends on `aip-core` only — never `aip-csm-builder`, `aip-analyzer`, or any Policy/Rule Model module. Depends on: 1.2.
- [ ] 15.2 Add a source-scan check confirming no concurrency infrastructure (thread pool, executor service) exists anywhere in `aip-analysis`'s v1 orchestrator, per `define-analysis-framework/design.md` Decision 6. Depends on: 6.2.
- [ ] 15.3 Add a source-scan check confirming no code path in `aip-csm-builder` implements or references `CsmSnapshotSource`, `CsmScope`, or `AnalysisResult` — confirming this change's Binding Decision 2 (no real adapter) held throughout implementation, not merely at design time. Depends on: 1.4, 2.1, 7.1.
- [ ] 15.4 Final gate: confirm (via review checklist and CI) that this change contains no Rule Framework, Finding Model, or Agent Framework implementation, that `canonical-software-model`, `software-repository-understanding`, `csm-builder`, and `define-analysis-framework`'s own specification remain unmodified, and that every decision in both `define-analysis-framework/design.md` (Decisions 1-7) and this change's own `design.md` (Decisions 1-7, plus the two Binding Decisions) holds as implemented. Depends on: all preceding sections.
