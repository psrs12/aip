## Scope Notes — Explicitly Deferred (Not Tasks in This Change)

The following are deliberately out of scope for `implement-rule-framework`
(the future change these tasks describe the design/spec surface for) and
are listed here for traceability only — not as checkbox tasks, since
nothing below is meant to become "done":

- A DSL or adopted external rule/policy engine (`design.md` Decision 1,
  Alternatives Considered).
- Rule-to-Rule composition or declared ordering between Rules
  (`design.md` Decision 2; spec `No Rule-to-Rule Composition`).
- Concrete Rule Evaluation Result persistence technology (`design.md`
  Decision 4, Non-Goals) — only the `RuleEvaluationResultStore`
  abstraction is in scope.
- Finding Model implementation — Rule Evaluation Result is Finding
  Model's future input, not built here (`design.md` Non-Goals;
  spec Purpose).
- Retention/compaction strategy for `PASS` outcomes — a future store
  concern (`design.md` Decision 5).

## 1. `aip-rules` Module Scaffolding and Dependency-Graph Guard

- [ ] 1.1 Add the `aip-rules` Maven module (Java 21+) as a sibling of
  `aip-csm-builder` and `aip-analysis`, per `design.md` Decision 7's
  dependency diagram.
- [ ] 1.2 Configure `aip-rules`'s POM to depend on `aip-core` only —
  no dependency on `aip-analysis`, `aip-csm-builder`, or `aip-analyzer`
  (`design.md` Decision 7; spec `CSM and Analysis Content Reached Only
  Through Established Contracts`). Depends on: 1.1.
- [ ] 1.3 Add a dependency-graph CI check that fails the build if
  `aip-rules` gains any non-test dependency outside `aip-core`,
  mirroring `aip-csm-builder`'s and `aip-analysis`'s own guards
  (`design.md` Decision 7). Depends on: 1.2.
- [ ] 1.4 Set up shared test infrastructure and directory conventions
  for `aip-rules`, consistent with `aip-csm-builder`/`aip-analysis`.
  Depends on: 1.1.

## 2. `aip-core`: Shared Rule Scope Declaration

- [ ] 2.1 Extract Analysis Scope's existing declaration shape (declared
  non-empty CSM entity-kind/relationship-type set, optional
  containment-level anchor, optional native-attribute predicate) into
  a general, reusable Scope declaration type in `aip-core`, without
  modifying Analysis Scope's own already-approved vocabulary
  (`design.md` Decision 3; spec `Rule Scope Declaration`). Depends
  on: none (reads existing `aip-analysis` Analysis Scope specification
  only).
- [ ] 2.2 Define Rule Scope as this shared declaration type, verbatim —
  no Rule-specific fields added (`design.md` Decision 3). Depends
  on: 2.1.
- [ ] 2.3 Add tests confirming Rule Scope and Analysis Scope share the
  identical underlying declaration shape (no silent drift between the
  two) (`design.md` Decision 3, Alternatives Considered). Depends
  on: 2.2.

## 3. `aip-core`: `AnalysisResultSource` Contract

- [ ] 3.1 Define the `AnalysisResultSource` contract in `aip-core`:
  retrieval of an Analysis Result by its own identity, and retrieval
  of every Analysis Result produced by a given Analyzer identifier
  against a given source CSM Snapshot identity (`design.md` Decision 7;
  spec `Analysis Result Source Shape`). Depends on: 1.1 (module
  present; contract itself lives in `aip-core` and has no `aip-rules`
  dependency).
- [ ] 3.2 Add contract tests for `AnalysisResultSource` independent of
  any concrete implementation, mirroring `CsmSnapshotSource`'s own
  contract-test pattern (spec `Analysis Result Source Shape` scenario:
  Rule Framework is agnostic to Analysis Result production). Depends
  on: 3.1.

## 4. AnalysisView Consumption and Source CSM Snapshot Identity Propagation

- [ ] 4.1 Implement `aip-rules` read access to `AnalysisView` for direct
  CSM-content facts within a Rule Type's declared Rule Scope
  (`design.md` Decisions 2, 3; spec `CSM and Analysis Content Reached
  Only Through Established Contracts`). Depends on: 1.2, 2.2.
- [ ] 4.2 Implement retrieval of the source CSM Snapshot identity from
  the `AnalysisView` (per `define-analysis-framework`'s `Analysis View
  Construction` requirement, as amended) rather than any direct
  `CsmSnapshotSource` dependency (`design.md` Decision 4; spec `CSM and
  Analysis Content Reached Only Through Established Contracts`,
  scenario: Source CSM Snapshot identity is obtained from the Analysis
  View). Depends on: 4.1.
- [ ] 4.3 Add a compile-time/dependency-graph test proving `aip-rules`
  has no reachable dependency on `CsmSnapshotSource` or any
  `aip-csm-builder` type (spec `CSM and Analysis Content Reached Only
  Through Established Contracts`). Depends on: 4.2, 1.3.

## 5. Rule Type Contract and Versioning

- [ ] 5.1 Define the Rule Type contract: stable identifier, monotonic
  version, declared set of required Analyzer identifiers (may be
  empty), and a declared Rule Scope (`design.md` Decisions 1, 2; spec
  `Rule Type Contract`). Depends on: 2.2.
- [ ] 5.2 Enforce that a Rule Type is evaluable using exactly its
  declared inputs, with no undeclared CSM or Analysis content read
  (`design.md` Decision 2, Alternatives Considered; spec `Rule Type
  Contract`, scenario: Rule Type reads no undeclared content). Depends
  on: 5.1, 4.1.
- [ ] 5.3 Add tests confirming a Rule Type's declared inputs are static
  and known before evaluation, never resolved as a dynamic dependency
  graph (`design.md` Decision 2). Depends on: 5.1.

## 6. Declarative Rule Definition and Configuration

- [ ] 6.1 Define the Rule configuration shape: a reference to exactly
  one registered Rule Type plus configuration values (entity kinds,
  relationship types, element identities, and/or native-attribute
  predicate values) — no executable logic, no source code (`design.md`
  Decision 1; spec `Rule Declaration`). Depends on: 5.1.
- [ ] 6.2 Implement independent evaluation of multiple Rules configured
  against the same Rule Type, each using only its own configuration
  (spec `Rule Declaration`, scenario: One Rule Type supports multiple
  independently configured Rules). Depends on: 6.1.
- [ ] 6.3 Enforce that no Rule consumes another Rule's Rule Evaluation
  Result, and that Rule Framework accepts no declared ordering or
  dependency between Rules (`design.md` Decision 2; spec `No
  Rule-to-Rule Composition`). Depends on: 6.1.
- [ ] 6.4 Add tests confirming a Rule Evaluation Result is unaffected by
  which other Rules are registered in the same run (spec `No
  Rule-to-Rule Composition`, scenario: Rule Evaluation Result is
  unaffected by which other Rules are registered). Depends on: 6.3.

## 7. Rule Registration and Extension Mechanism

- [ ] 7.1 Implement a Rule Type registry (registration API,
  identifier-based lookup), mirroring `AnalyzerRegistry`'s shape
  (`design.md` Decision 1; spec `Extension Mechanism for New Rule
  Types`). Depends on: 5.1.
- [ ] 7.2 Implement Rule registration against a registered Rule Type,
  rejecting a Rule that references an unregistered Rule Type (spec
  `Rule Declaration`; `Rule Evaluation Result Validation Before
  Publication`). Depends on: 6.1, 7.1.
- [ ] 7.3 Confirm registering a new Rule Type requires no change to
  evaluation, identity, traceability, or validation mechanisms —
  design this as an explicit registry extension point, not
  case-by-case wiring (spec `Extension Mechanism for New Rule Types`).
  Depends on: 7.1.

## 8. Rule Applicability and Scope Evaluation

- [ ] 8.1 Implement Rule Scope instance enumeration: unanchored → once
  per repository; anchored → once per matching contained element
  present in the CSM Snapshot (`design.md` Decision 3; spec `Rule
  Scope Declaration`). Depends on: 2.2, 4.1.
- [ ] 8.2 Implement kind-based applicability: a Rule Scope instance with
  no CSM content matching the Rule Type's declared kinds SHALL NOT be
  evaluated, and SHALL produce no Rule Evaluation Result of any outcome
  (spec `Kind-Based Rule Applicability`). Depends on: 8.1.
- [ ] 8.3 Implement native-attribute applicability refinement as an
  optional predicate over already-present native-evidence-attribute
  content, without introducing a separate scope-declaration or
  registration mechanism (spec `Native-Attribute Rule Applicability
  Refinement`). Depends on: 8.2.
- [ ] 8.4 Add tests for both applicability layers: no matching kind →
  no evaluation; matching kind but failing native-attribute predicate →
  no evaluation; matching kind and satisfied predicate → evaluated
  (spec `Kind-Based Rule Applicability`, `Native-Attribute Rule
  Applicability Refinement`). Depends on: 8.2, 8.3.
- [ ] 8.5 Enforce single-repository evaluation scope: every Rule
  Evaluation Result produced by one evaluation run references exactly
  one repository's CSM Snapshot identity (spec `Single-Repository Rule
  Evaluation Scope`). Depends on: 4.2, 8.1.

## 9. Analysis Result Consumption

- [ ] 9.1 Implement Rule Type consumption of its declared Analyzer
  identifiers' Analysis Results exclusively through
  `AnalysisResultSource` (`design.md` Decisions 2, 7; spec `Analysis
  Result Consumption Through the Analysis Result Source Only`).
  Depends on: 3.1, 5.1.
- [ ] 9.2 Add a dependency-graph test proving a Rule Type never reads
  Analysis Results via a direct dependency on the producing module
  (spec `Analysis Result Consumption Through the Analysis Result
  Source Only`). Depends on: 9.1, 1.3.
- [ ] 9.3 Add tests confirming a Rule Type evaluates identically against
  two different `AnalysisResultSource` implementations exposing
  equivalent content (spec `Analysis Result Source Shape`, scenario:
  Rule Framework is agnostic to Analysis Result production). Depends
  on: 9.1.

## 10. Direct AnalysisView Access Through Established Contracts

- [ ] 10.1 Confirm and enforce that all CSM content a Rule Type reads
  comes from `AnalysisView` alone — no direct Repository Evidence read,
  no Runtime Model dependency, no dependency on the module that
  constructed the CSM Snapshot (spec `CSM and Analysis Content Reached
  Only Through Established Contracts`). Depends on: 4.1.
- [ ] 10.2 Add tests covering both established-contract boundaries
  together: CSM content via `AnalysisView` only, Analysis Results via
  `AnalysisResultSource` only (spec `CSM and Analysis Content Reached
  Only Through Established Contracts`, scenario: No direct Repository
  Evidence or Runtime Model dependency). Depends on: 10.1, 9.1.

## 11. Deterministic Rule Evaluation

- [ ] 11.1 Implement Rule evaluation as a pure function of a Rule's
  declared inputs (its Rule Scope instance's `AnalysisView` content
  plus its consumed Analysis Results) — no AI/LLM call, no heuristic
  or probabilistic scoring step (`design.md` Decision 1; spec
  `Deterministic, Declarative Rule Evaluation Only`). Depends on: 8.1,
  9.1.
- [ ] 11.2 Add repeatability tests: evaluating the same Rule twice
  against the same Analysis View and the same consumed Analysis
  Results produces identical identity and identical outcome (spec
  `Deterministic, Declarative Rule Evaluation Only`, scenario:
  Repeated evaluation over unchanged input is identical). Depends
  on: 11.1.

## 12. Outcome Semantics — PASS / FAIL / NOT_APPLICABLE

- [ ] 12.1 Implement the three-outcome vocabulary (`PASS`, `FAIL`,
  `NOT_APPLICABLE`) as the exhaustive outcome set for an evaluated Rule
  Scope instance (`design.md` Decision 5; spec `Rule Evaluation Outcome
  Semantics`). Depends on: 11.1.
- [ ] 12.2 Implement the binding distinction from `design.md` Decision
  5's binding sub-decision: a Rule Scope instance that fails
  kind-based or native-attribute applicability (Section 8) is never
  invoked and produces no Rule Evaluation Result of any outcome; a
  Rule Scope instance that passes applicability but whose
  condition-specific content is absent produces an explicit
  `NOT_APPLICABLE` Rule Evaluation Result (spec `Rule Evaluation
  Outcome Semantics`, scenario: Missing condition-specific content
  produces NOT_APPLICABLE, not silence). Depends on: 12.1, 8.2, 8.3.
- [ ] 12.3 Implement that every outcome — `PASS`, `FAIL`, or
  `NOT_APPLICABLE` — is offered for validation and publication, with
  no outcome discarded before that point solely on the basis of what
  it is (`design.md` Decision 5; spec `Rule Evaluation Outcome
  Semantics`, scenario: Every outcome is offered for publication).
  Depends on: 12.1.
- [ ] 12.4 Add tests covering all three outcomes plus the no-Result
  applicability-failure case as four distinct, individually verified
  situations (spec `Rule Evaluation Outcome Semantics`; `Kind-Based
  Rule Applicability`). Depends on: 12.2, 12.3.

## 13. Deterministic Rule Evaluation Result Identity

- [ ] 13.1 Implement Rule Evaluation Result identity as a deterministic
  function of: the producing Rule's identifier, the Rule's version,
  the source CSM Snapshot identity (Section 4.2), the Rule Scope
  instance covered, and the complete set of consumed Analysis Result
  identities — no random or otherwise non-reproducible generation
  (`design.md` Decision 4; spec `Deterministic Rule Evaluation Result
  Identity`). Depends on: 4.2, 8.1, 9.1, 12.1.
- [ ] 13.2 Add identity-stability tests: identical inputs across two
  runs produce identical identity (spec `Deterministic Rule Evaluation
  Result Identity`, scenario: Identical inputs produce identical
  Result identity). Depends on: 13.1.
- [ ] 13.3 Add identity-distinguishability tests: a differing consumed
  Analysis Result set, and a differing Rule version, each yield a
  distinguishable identity with all else held constant (spec
  `Deterministic Rule Evaluation Result Identity`, scenarios:
  Different consumed Analysis Results yield a distinguishable
  identity; A different Rule version yields a distinguishable
  identity). Depends on: 13.1.

## 14. Traceability

- [ ] 14.1 Implement traceability-reference retention on every Rule
  Evaluation Result: producing Rule identifier and version, source CSM
  Snapshot identity, Rule Scope instance, and complete consumed
  Analysis Result identity set (`design.md` Decision 4; spec `Rule
  Evaluation Result Traceability`). Depends on: 13.1.
- [ ] 14.2 Add tests confirming a Rule Evaluation Result is traceable
  back to its producing Rule and its full consumed Analysis Result set
  (spec `Rule Evaluation Result Traceability`, both scenarios). Depends
  on: 14.1.

## 15. Rule Evaluation Result Persistence Abstraction and Payload Shape

- [ ] 15.1 Define the `RuleEvaluationResultStore` abstraction
  (interface only; no concrete implementation) mirroring
  `AnalysisResultStore`'s and `SnapshotStore`'s own precedent
  (`design.md` Decision 4, Non-Goals; spec `Rule Evaluation Results
  Are Durable, Individually Identifiable Artifacts`). Depends on:
  13.1.
- [ ] 15.2 Treat a Rule Evaluation Result's diagnostic content beyond
  its outcome as an opaque, Rule-Type-defined payload — no framework
  interpretation or shape constraint beyond traceability/referential
  requirements (spec `Rule Evaluation Result Payload Is
  Rule-Type-Defined`). Depends on: 15.1.
- [ ] 15.3 Add tests confirming individual retrievability by identity
  after the producing run ends, and that a new, differently-identified
  Result never overwrites a prior one (spec `Rule Evaluation Results
  Are Durable, Individually Identifiable Artifacts`, both scenarios).
  Depends on: 15.1.
- [ ] 15.4 Add tests confirming two Rule Types' differently-shaped
  diagnostic payloads are both stored and retrieved without
  interpretation (spec `Rule Evaluation Result Payload Is
  Rule-Type-Defined`, both scenarios). Depends on: 15.2.

## 16. Validation and Publishing Gate

- [ ] 16.1 Implement `RuleEvaluationResultValidator`, checking:
  referential integrity of every consumed Analysis Result identity and
  every referenced CSM element/Subject identity; Rule/version
  consistency against the currently registered Rule; and Result
  content staying within its Rule Type's declared Rule Scope
  (`design.md` Decision 6; spec `Rule Evaluation Result Validation
  Before Publication`). Depends on: 7.2, 13.1, 14.1.
- [ ] 16.2 Implement `RuleEvaluationResultPublisher`, calling
  `RuleEvaluationResultStore.write` only for a Result that passes
  validation (`design.md` Decision 6). Depends on: 16.1, 15.1.
- [ ] 16.3 Add tests for all four validation scenarios: valid Result
  published; Result referencing an unavailable Analysis Result or CSM
  identity rejected; Result from an unregistered Rule/version
  rejected; Result exceeding its Rule Type's declared scope rejected
  (spec `Rule Evaluation Result Validation Before Publication`, all
  four scenarios). Depends on: 16.1.

## 17. Dependency and Architectural Invariant Tests

- [ ] 17.1 Add a test confirming Rule Evaluation Results are never
  written into, or represented as, CSM content or an Analysis Result
  (spec `Rule Evaluation Results Are a Distinct Concept From CSM
  Knowledge, Analysis Results, and Findings`, scenario: Producing a
  Rule Evaluation Result does not alter CSM or Analysis content).
  Depends on: 15.1.
- [ ] 17.2 Add a test confirming a CSM-content or Analysis-Result query
  never returns Rule Evaluation Results (spec `Rule Evaluation Results
  Are a Distinct Concept From CSM Knowledge, Analysis Results, and
  Findings`, scenario: Rule Evaluation Results are stored separately
  from CSM and Analysis content). Depends on: 15.1.
- [ ] 17.3 Add a full-module dependency-graph test asserting
  `aip-rules`, `aip-csm-builder`, and `aip-analysis` remain siblings —
  none depends on either of the other two (`design.md` Decision 7
  diagram). Depends on: 1.3.
- [ ] 17.4 Add an invariant test enumerating every requirement in this
  section together: no Rule-to-Rule composition (Section 6.3), no
  direct producing-module dependency (Sections 4.3, 9.2), no CSM/
  Analysis-content mutation (17.1), no cross-artifact confusion
  (17.2). Depends on: 17.1, 17.2, 17.3, 6.3.

## 18. Extension Tests — New Rule Types Require No Core Mechanism Changes

- [ ] 18.1 Implement a second, structurally distinct example Rule Type
  (in test scope) exercising a different Rule Scope shape and a
  different declared Analyzer-input set than the first, to prove the
  registry (Section 7) generalizes (spec `Extension Mechanism for New
  Rule Types`). Depends on: 7.1, 5.1.
- [ ] 18.2 Add a test proving registering this second Rule Type
  requires no change to Rule Evaluation Result identity, traceability,
  or validation mechanisms — the same mechanisms from Sections 13, 14,
  and 16 apply unmodified (spec `Extension Mechanism for New Rule
  Types`, scenario: New Rule Type registered without core mechanism
  changes). Depends on: 18.1, 13.1, 14.1, 16.1.

## 19. Requirement/Scenario Traceability and Coverage

- [ ] 19.1 Build a traceability matrix mapping each of
  `specs/rule-framework/spec.md`'s 19 requirements and 42 scenarios to
  the task(s) and test(s) that implement/verify it, following
  `implement-csm-builder`'s own `traceability.md` precedent. Depends
  on: Sections 1–18.
- [ ] 19.2 Confirm every one of `design.md`'s seven Decisions (plus the
  Decision 5 binding sub-decision) has at least one corresponding
  implementation task and test above; record any gap found rather than
  silently leaving it uncovered. Depends on: 19.1.
- [ ] 19.3 Run full validation (`openspec validate rule-framework
  --strict` once specs are synced, or the equivalent check available
  during implementation) and record the result in this change's
  verification artifact. Depends on: 19.1, 19.2.
