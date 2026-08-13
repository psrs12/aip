## Scope Notes — Explicitly Deferred (Not Tasks in This Change)

The following are deliberately out of scope for this change and are
listed here for traceability only — not as checkbox tasks, since
nothing below is meant to become "done":

- A DSL or adopted external rule/policy engine (`define-rule-framework/
  design.md` Decision 1, Alternatives Considered).
- Rule-to-Rule composition or declared ordering between Rules
  (`define-rule-framework/design.md` Decision 2; spec `No Rule-to-Rule
  Composition`).
- Concrete `RuleEvaluationResultStore` persistence technology
  (`define-rule-framework/design.md` Decision 4, Non-Goals; this
  change's own `design.md` Non-Goals) — only the abstraction is in
  scope.
- Finding Model implementation — Rule Evaluation Result is Finding
  Model's future input, not built here.
- Retention/compaction strategy for `PASS` outcomes.
- A real `aip-analysis` implementation or adapter (this change's own
  `design.md` Binding Decision 2) — fixtures only.
- Any change to `canonical-software-model`, `software-repository-
  understanding`, `csm-builder`, `define-analysis-framework`,
  `implement-analysis-framework`, or `define-rule-framework`'s own
  specification.

## 0. Prerequisite Gate — `implement-analysis-framework` Must Be Complete

- [ ] 0.1 Confirm `implement-analysis-framework`'s own 52 tasks are all
  complete — `CsmSnapshotSource`, `CsmSnapshotId`, `CsmScope`,
  `AnalysisView`, `AnalysisResult`, and `AnalysisResultId` all exist
  and compile in `aip-core` — before proceeding to Section 1 of this
  change (`design.md` Decision 9). This is a blocking gate, not a
  parallelizable task: no task below this one may begin until it is
  satisfied.

## 1. `aip-rules` Module Scaffolding and Dependency-Graph Guard

- [ ] 1.1 Add the `aip-rules` Maven module (Java 21+) as a sibling of
  `aip-csm-builder` and `aip-analysis`, per `define-rule-framework/
  design.md` Decision 7's dependency diagram. Depends on: 0.1.
- [ ] 1.2 Configure `aip-rules`'s POM to depend on `aip-core` only —
  no dependency on `aip-analysis`, `aip-csm-builder`, or `aip-analyzer`
  (`design.md` Decision 7 [define-rule-framework]; spec `CSM and
  Analysis Content Reached Only Through Established Contracts`).
  Depends on: 1.1.
- [ ] 1.3 Add a dependency-graph CI check that fails the build if
  `aip-rules` gains any non-test dependency outside `aip-core`,
  mirroring `aip-csm-builder`'s and `aip-analysis`'s own guards.
  Depends on: 1.2.
- [ ] 1.4 Set up shared test infrastructure and directory conventions
  for `aip-rules`, consistent with `aip-csm-builder`/`aip-analysis`.
  Depends on: 1.1.

## 2. Rule Scope: Confirm `CsmScope` Reuse (No New Type)

- [ ] 2.1 Confirm `CsmScope` (built by `implement-analysis-framework`,
  Section 2 of that change's own `tasks.md`) is used directly as Rule
  Scope, with no `aip-rules`-local wrapper or adapter type — per this
  change's own `design.md` Decision 3 and `define-rule-framework/
  design.md` Decision 3. Depends on: 0.1.
- [ ] 2.2 Add tests confirming Rule Scope instances constructed via
  `CsmScope` behave identically whether used by an Analyzer or a Rule
  Type — no silent drift between the two consumers (`design.md`
  Decision 3, Alternatives Considered [define-rule-framework]).
  Depends on: 2.1.

## 3. `aip-core`: `AnalysisResultSource` Contract

- [ ] 3.1 Define the `AnalysisResultSource` contract in `aip-core`
  (`aip.core.csm`): `read(AnalysisResultId): Optional<AnalysisResult>`,
  `list(analyzerIdentifier: String, snapshotId: CsmSnapshotId):
  Set<AnalysisResult>` — this change's own `aip-core` contribution,
  per `define-rule-framework/design.md` Decision 7 and this change's
  own `design.md` Decision 5. Depends on: 1.1, 0.1.
- [ ] 3.2 Add contract tests for `AnalysisResultSource` independent of
  any concrete implementation, mirroring `CsmSnapshotSource`'s own
  contract-test pattern (spec `Analysis Result Source Shape` scenario:
  Rule Framework is agnostic to Analysis Result production). Depends
  on: 3.1.

## 4. AnalysisView Consumption and Source CSM Snapshot Identity Propagation

- [ ] 4.1 Implement `aip-rules` read access to `AnalysisView` for
  direct CSM-content facts within a Rule Type's declared Rule Scope
  (`design.md` Decisions 2, 3 [define-rule-framework]; spec `CSM and
  Analysis Content Reached Only Through Established Contracts`).
  Depends on: 1.2, 2.1.
- [ ] 4.2 Implement retrieval of the source `CsmSnapshotId` from the
  `AnalysisView` (per `define-analysis-framework`'s `Analysis View
  Construction` requirement, as amended) rather than any direct
  `CsmSnapshotSource` dependency (`design.md` Decision 4
  [define-rule-framework]; spec `CSM and Analysis Content Reached Only
  Through Established Contracts`, scenario: Source CSM Snapshot
  identity is obtained from the Analysis View). Depends on: 4.1.
- [ ] 4.3 Add a compile-time/dependency-graph test proving `aip-rules`
  has no reachable dependency on `CsmSnapshotSource` or any
  `aip-csm-builder` type (spec `CSM and Analysis Content Reached Only
  Through Established Contracts`). Depends on: 4.2, 1.3.

## 5. Rule Type Contract and Versioning

- [ ] 5.1 Define the Rule Type contract (`aip-rules`): stable
  identifier, monotonic version, declared set of required Analyzer
  identifiers (may be empty), and a declared Rule Scope (`CsmScope`)
  — mirroring the Analyzer contract's own placement in `aip-analysis`,
  per this change's own `design.md` Decision 7 and `define-rule-
  framework/design.md` Decisions 1, 2; spec `Rule Type Contract`.
  Depends on: 2.1.
- [ ] 5.2 Enforce that a Rule Type is evaluable using exactly its
  declared inputs, with no undeclared CSM or Analysis content read
  (`design.md` Decision 2, Alternatives Considered [define-rule-
  framework]; spec `Rule Type Contract`, scenario: Rule Type reads no
  undeclared content). Depends on: 5.1, 4.1.
- [ ] 5.3 Add tests confirming a Rule Type's declared inputs are static
  and known before evaluation, never resolved as a dynamic dependency
  graph. Depends on: 5.1.

## 6. Declarative Rule Definition and Configuration

- [ ] 6.1 Define the Rule configuration shape (`aip-rules`-local, no
  executable logic): a reference to exactly one registered Rule Type
  plus configuration values (entity kinds, relationship types, element
  identities, and/or native-attribute predicate values), per this
  change's own `design.md` Decision 7 and `define-rule-framework/
  design.md` Decision 1; spec `Rule Declaration`. Depends on: 5.1.
- [ ] 6.2 Implement independent evaluation of multiple Rules configured
  against the same Rule Type, each using only its own configuration
  (spec `Rule Declaration`, scenario: One Rule Type supports multiple
  independently configured Rules). Depends on: 6.1.
- [ ] 6.3 Enforce that no Rule consumes another Rule's Rule Evaluation
  Result, and that Rule Framework accepts no declared ordering or
  dependency between Rules (spec `No Rule-to-Rule Composition`).
  Depends on: 6.1.
- [ ] 6.4 Add tests confirming a Rule Evaluation Result is unaffected by
  which other Rules are registered in the same run (spec `No
  Rule-to-Rule Composition`, scenario: Rule Evaluation Result is
  unaffected by which other Rules are registered). Depends on: 6.3.

## 7. Rule Registration and Extension Mechanism

- [ ] 7.1 Implement a Rule Type registry (registration API,
  identifier-based lookup), mirroring `AnalyzerRegistry`'s shape (spec
  `Extension Mechanism for New Rule Types`). Depends on: 5.1.
- [ ] 7.2 Implement Rule registration against a registered Rule Type,
  rejecting a Rule that references an unregistered Rule Type (spec
  `Rule Declaration`; `Rule Evaluation Result Validation Before
  Publication`). Depends on: 6.1, 7.1.
- [ ] 7.3 Confirm registering a new Rule Type requires no change to
  evaluation, identity, traceability, or validation mechanisms (spec
  `Extension Mechanism for New Rule Types`). Depends on: 7.1.

## 8. Rule Applicability and Scope Evaluation

- [ ] 8.1 Implement Rule Scope instance enumeration via `CsmScope`:
  unanchored → once per repository; anchored → once per matching
  contained element present in the CSM Snapshot (spec `Rule Scope
  Declaration`). Depends on: 2.1, 4.1.
- [ ] 8.2 Implement kind-based applicability: a Rule Scope instance
  with no CSM content matching the Rule Type's declared kinds SHALL
  NOT be evaluated, and SHALL produce no Rule Evaluation Result of any
  outcome (spec `Kind-Based Rule Applicability`). Depends on: 8.1.
- [ ] 8.3 Implement native-attribute applicability refinement as an
  optional predicate over already-present native-evidence-attribute
  content, without introducing a separate scope-declaration or
  registration mechanism (spec `Native-Attribute Rule Applicability
  Refinement`). Depends on: 8.2.
- [ ] 8.4 Add tests for both applicability layers: no matching kind →
  no evaluation; matching kind but failing native-attribute predicate →
  no evaluation; matching kind and satisfied predicate → evaluated.
  Depends on: 8.2, 8.3.
- [ ] 8.5 Enforce single-repository evaluation scope: every Rule
  Evaluation Result produced by one evaluation run references exactly
  one repository's `CsmSnapshotId` (spec `Single-Repository Rule
  Evaluation Scope`). Depends on: 4.2, 8.1.

## 9. Analysis Result Consumption

- [ ] 9.1 Implement Rule Type consumption of its declared Analyzer
  identifiers' `AnalysisResult`s exclusively through
  `AnalysisResultSource` (spec `Analysis Result Consumption Through the
  Analysis Result Source Only`). Depends on: 3.1, 5.1.
- [ ] 9.2 Add a dependency-graph test proving a Rule Type never reads
  `AnalysisResult`s via a direct dependency on the producing module
  (spec `Analysis Result Consumption Through the Analysis Result
  Source Only`). Depends on: 9.1, 1.3.
- [ ] 9.3 Add tests confirming a Rule Type evaluates identically against
  two different `AnalysisResultSource` implementations exposing
  equivalent content (spec `Analysis Result Source Shape`, scenario:
  Rule Framework is agnostic to Analysis Result production). Depends
  on: 9.1.

## 10. Direct AnalysisView Access Through Established Contracts

- [ ] 10.1 Confirm and enforce that all CSM content a Rule Type reads
  comes from `AnalysisView` alone — no direct Repository Evidence
  read, no Runtime Model dependency, no dependency on the module that
  constructed the CSM Snapshot (spec `CSM and Analysis Content Reached
  Only Through Established Contracts`). Depends on: 4.1.
- [ ] 10.2 Add tests covering both established-contract boundaries
  together: CSM content via `AnalysisView` only, `AnalysisResult`s via
  `AnalysisResultSource` only (spec `CSM and Analysis Content Reached
  Only Through Established Contracts`, scenario: No direct Repository
  Evidence or Runtime Model dependency). Depends on: 10.1, 9.1.

## 11. Deterministic Rule Evaluation

- [ ] 11.1 Implement Rule evaluation as a pure function of a Rule's
  declared inputs (its Rule Scope instance's `AnalysisView` content
  plus its consumed `AnalysisResult`s) — no AI/LLM call, no heuristic
  or probabilistic scoring step (spec `Deterministic, Declarative Rule
  Evaluation Only`). Depends on: 8.1, 9.1.
- [ ] 11.2 Add repeatability tests: evaluating the same Rule twice
  against the same Analysis View and the same consumed
  `AnalysisResult`s produces identical identity and identical outcome
  (spec `Deterministic, Declarative Rule Evaluation Only`, scenario:
  Repeated evaluation over unchanged input is identical). Depends
  on: 11.1.

## 12. `RuleEvaluationOutcome` — PASS / FAIL / NOT_APPLICABLE

- [ ] 12.1 Define `RuleEvaluationOutcome` as a three-value `aip-core`
  enum (`PASS`, `FAIL`, `NOT_APPLICABLE`), per this change's own
  `design.md` Decision 4. Depends on: 0.1, 11.1.
- [ ] 12.2 Implement the binding distinction (`define-rule-framework/
  design.md` Decision 5's binding sub-decision): a Rule Scope instance
  that fails kind-based or native-attribute applicability (Section 8)
  is never invoked and produces no `RuleEvaluationResult` of any
  outcome — not a fourth enum value or sentinel, per this change's own
  `design.md` Decision 4, Alternatives Considered; a Rule Scope
  instance that passes applicability but whose condition-specific
  content is absent produces an explicit `NOT_APPLICABLE`
  `RuleEvaluationResult` (spec `Rule Evaluation Outcome Semantics`,
  scenario: Missing condition-specific content produces
  NOT_APPLICABLE, not silence). Depends on: 12.1, 8.2, 8.3.
- [ ] 12.3 Implement that every outcome — `PASS`, `FAIL`, or
  `NOT_APPLICABLE` — is offered for validation and publication, with
  no outcome discarded before that point solely on the basis of what
  it is (spec `Rule Evaluation Outcome Semantics`, scenario: Every
  outcome is offered for publication). Depends on: 12.1.
- [ ] 12.4 Add tests covering all three outcomes plus the no-Result
  applicability-failure case as four distinct, individually verified
  situations (spec `Rule Evaluation Outcome Semantics`; `Kind-Based
  Rule Applicability`). Depends on: 12.2, 12.3.

## 13. `RuleEvaluationResult` and `RuleEvaluationResultId` (`aip-core`)

- [ ] 13.1 Define `RuleEvaluationResult` as a final, non-generic class
  in `aip-core` (`aip.core.csm`) — producing Rule identifier/version,
  source `CsmSnapshotId`, `CsmScope` instance, complete consumed
  `Set<AnalysisResultId>`, `RuleEvaluationOutcome`, and an opaque
  `Object` payload — per this change's own `design.md` Decisions 1
  (Binding Decision 1). Depends on: 12.1, 0.1.
- [ ] 13.2 Define `RuleEvaluationResultId` in `aip-core`: a
  deterministic function of (Rule identifier, Rule version,
  `CsmSnapshotId`, `CsmScope` instance, `Set<AnalysisResultId>`) — no
  random or otherwise non-reproducible generation, per this change's
  own `design.md` Decision 2 and `define-rule-framework/design.md`
  Decision 4; spec `Deterministic Rule Evaluation Result Identity`.
  Depends on: 13.1.
- [ ] 13.3 Add identity-stability tests: identical inputs across two
  runs produce identical `RuleEvaluationResultId` (spec `Deterministic
  Rule Evaluation Result Identity`, scenario: Identical inputs produce
  identical Result identity). Depends on: 13.2.
- [ ] 13.4 Add identity-distinguishability tests: a differing consumed
  `AnalysisResult` set, and a differing Rule version, each yield a
  distinguishable identity with all else held constant (spec
  `Deterministic Rule Evaluation Result Identity`, both scenarios).
  Depends on: 13.2.

## 14. Traceability

- [ ] 14.1 Implement traceability-reference retention on every
  `RuleEvaluationResult`: producing Rule identifier and version,
  source `CsmSnapshotId`, `CsmScope` instance, and complete consumed
  `AnalysisResultId` set — all directly inspectable fields (spec `Rule
  Evaluation Result Traceability`). Depends on: 13.1.
- [ ] 14.2 Add tests confirming a `RuleEvaluationResult` is traceable
  back to its producing Rule and its full consumed `AnalysisResult`
  set (spec `Rule Evaluation Result Traceability`, both scenarios).
  Depends on: 14.1.

## 15. `RuleEvaluationResult` Payload

- [ ] 15.1 Implement `RuleEvaluationResult`'s `Object` payload as an
  opaque, Rule-Type-defined value the framework never constrains,
  interprets, or validates beyond the traceability/referential-
  integrity expectations defined elsewhere, per this change's own
  `design.md` Decision 1 and `define-rule-framework/design.md`
  Decision 4; spec `Rule Evaluation Result Payload Is Rule-Type-
  Defined`. Depends on: 13.1.
- [ ] 15.2 Add tests confirming two Rule Types' differently-shaped
  diagnostic payloads are both stored and retrieved without
  interpretation (spec `Rule Evaluation Result Payload Is
  Rule-Type-Defined`, both scenarios). Depends on: 15.1.

## 16. `RuleEvaluationResultStore` Abstraction (`aip-rules`)

- [ ] 16.1 Define the `RuleEvaluationResultStore` abstraction
  (interface only; no concrete implementation) **in `aip-rules`, not
  `aip-core`** — mirroring `AnalysisResultStore`'s own placement in
  `aip-analysis`, per this change's own `design.md` Decision 6; spec
  `Rule Evaluation Results Are Durable, Individually Identifiable
  Artifacts`. Depends on: 13.1.
- [ ] 16.2 Add tests confirming individual retrievability by identity
  after the producing run ends, and that a new, differently-identified
  Result never overwrites a prior one (spec `Rule Evaluation Results
  Are Durable, Individually Identifiable Artifacts`, both scenarios).
  Depends on: 16.1.

## 17. Validation and Publishing Gate

- [ ] 17.1 Implement `RuleEvaluationResultValidator`, checking:
  referential integrity of every consumed `AnalysisResultId` and every
  referenced CSM element/Subject identity; Rule/version consistency
  against the currently registered Rule; and Result content staying
  within its Rule Type's declared `CsmScope` (spec `Rule Evaluation
  Result Validation Before Publication`). Depends on: 7.2, 13.2, 14.1.
- [ ] 17.2 Implement `RuleEvaluationResultPublisher`, calling
  `RuleEvaluationResultStore.write` only for a Result that passes
  validation. Depends on: 17.1, 16.1.
- [ ] 17.3 Add tests for all four validation scenarios: valid Result
  published; Result referencing an unavailable `AnalysisResult` or CSM
  identity rejected; Result from an unregistered Rule/version
  rejected; Result exceeding its Rule Type's declared scope rejected
  (spec `Rule Evaluation Result Validation Before Publication`, all
  four scenarios). Depends on: 17.1.

## 18. Fixture Layer

- [ ] 18.1 Implement a test-only fixture-building API for
  `CsmSnapshotSource`/`AnalysisView`/`AnalysisResultSource`, using
  `aip-core` types exclusively (`CsmElement`, `CsmRelationship`,
  `CsmSnapshotId`, `CsmScope`, `AnalysisResult`, `AnalysisResultId`) —
  no real `aip-analysis` adapter, per this change's own `design.md`
  Decision 8 and Binding Decision 2. Depends on: 3.1, 0.1.
- [ ] 18.2 Add a build/lint check confirming the fixture package is
  test-scope only and is never imported by `aip-rules`'s production
  code, mirroring `aip-csm-builder`'s and `aip-analysis`'s own
  `check-fixture-package-scope.sh`. Depends on: 18.1.

## 19. Dependency and Architectural Invariant Tests

- [ ] 19.1 Add a test confirming `RuleEvaluationResult`s are never
  written into, or represented as, CSM content or an `AnalysisResult`
  (spec `Rule Evaluation Results Are a Distinct Concept From CSM
  Knowledge, Analysis Results, and Findings`, scenario: Producing a
  Rule Evaluation Result does not alter CSM or Analysis content).
  Depends on: 16.1.
- [ ] 19.2 Add a test confirming a CSM-content or `AnalysisResult`
  query never returns `RuleEvaluationResult`s (spec `Rule Evaluation
  Results Are a Distinct Concept From CSM Knowledge, Analysis Results,
  and Findings`, scenario: Rule Evaluation Results are stored
  separately from CSM and Analysis content). Depends on: 16.1.
- [ ] 19.3 Add a full-module dependency-graph test asserting
  `aip-rules`, `aip-csm-builder`, and `aip-analysis` remain siblings —
  none depends on either of the other two. Depends on: 1.3.
- [ ] 19.4 Add an invariant test enumerating every requirement in this
  section together: no Rule-to-Rule composition (Section 6.3), no
  direct producing-module dependency (Sections 4.3, 9.2), no CSM/
  Analysis-content mutation (19.1), no cross-artifact confusion
  (19.2). Depends on: 19.1, 19.2, 19.3, 6.3.

## 20. Extension Tests — New Rule Types Require No Core Mechanism Changes

- [ ] 20.1 Implement a second, structurally distinct example Rule Type
  (in test scope) exercising a different Rule Scope shape and a
  different declared Analyzer-input set than the first, to prove the
  registry (Section 7) generalizes (spec `Extension Mechanism for New
  Rule Types`). Depends on: 7.1, 5.1.
- [ ] 20.2 Add a test proving registering this second Rule Type
  requires no change to `RuleEvaluationResult` identity, traceability,
  or validation mechanisms — the same mechanisms from Sections 13, 14,
  and 17 apply unmodified (spec `Extension Mechanism for New Rule
  Types`, scenario: New Rule Type registered without core mechanism
  changes). Depends on: 20.1, 13.2, 14.1, 17.1.

## 21. Requirement/Scenario Traceability and Coverage

- [ ] 21.1 Build a traceability matrix mapping each of `define-rule-
  framework/specs/rule-framework/spec.md`'s 19 requirements and 42
  scenarios to the task(s) and test(s) that implement/verify it,
  following `implement-csm-builder`'s own `traceability.md` precedent.
  Depends on: Sections 1-20.
- [ ] 21.2 Confirm every one of `define-rule-framework/design.md`'s
  seven Decisions (plus the Decision 5 binding sub-decision) and this
  change's own `design.md`'s nine Decisions has at least one
  corresponding implementation task and test above; record any gap
  found rather than silently leaving it uncovered. Depends on: 21.1.
- [ ] 21.3 Add an end-to-end fixture-based test evaluating a
  representative Rule against a fixture `AnalysisView` and fixture
  `AnalysisResultSource` content, covering PASS, FAIL, and
  NOT_APPLICABLE outcomes in one scenario. Depends on: 11.1, 12.4,
  17.2, 18.1.
- [ ] 21.4 Run full validation (`openspec validate rule-framework
  --strict` once specs are synced, or the equivalent check available
  during implementation) and record the result in this change's
  verification artifact. Depends on: 21.1, 21.2.

## 22. Architectural Invariant Enforcement

- [ ] 22.1 Finalize the dependency-graph CI check enforcing `aip-rules`
  depends on `aip-core` only — never `aip-analysis`, `aip-csm-builder`,
  or `aip-analyzer`. Depends on: 1.3.
- [ ] 22.2 Add a source-scan check confirming no code path in
  `aip-rules` depends on a real `aip-analysis` implementation type —
  confirming this change's Binding Decision 2 (no real adapter) held
  throughout implementation, not merely at design time. Depends
  on: 18.1.
- [ ] 22.3 Final gate: confirm (via review checklist and CI) that this
  change contains no Finding Model or Agent Framework implementation,
  that `canonical-software-model`, `software-repository-understanding`,
  `csm-builder`, `define-analysis-framework`, `implement-analysis-
  framework`'s own specification, and `define-rule-framework`'s own
  specification remain unmodified, and that every decision in
  `define-rule-framework/design.md` (Decisions 1-7) and this change's
  own `design.md` (Decisions 1-9, plus the three Binding Decisions)
  holds as implemented. Depends on: all preceding sections.
