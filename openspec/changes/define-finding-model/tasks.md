## Scope Notes — Explicitly Deferred (Not Tasks in This Change)

The following are deliberately out of scope for `implement-finding-model`
(the future change these tasks describe the design/spec surface for)
and are listed here for traceability only — not as checkbox tasks,
since nothing below is meant to become "done":

- AI-generated or manually-authored Finding creation (`design.md`
  Decision 1; spec `Finding Provenance Requires At Least One
  RuleEvaluationResult`).
- Finding lifecycle workflow/UI/presentation, mutable lifecycle state,
  and lifecycle-event artifacts (`design.md` Decision 4; spec `No
  Mutable Lifecycle State or Lifecycle Event Artifact in This Version`,
  `Finding Lifecycle Presentation Is Out of Scope`).
- Recommendation generation and remediation workflow (`design.md`
  Decision 5, Non-Goals).
- Concrete Finding persistence technology (`design.md` Decision 9,
  Non-Goals) — only the store abstraction is in scope.
- Finding aggregation/deduplication implementation beyond the v1 1:1
  relationship (`design.md` Decision 7; spec `Aggregation Is Not
  Performed in This Version`).
- Any change to `canonical-software-model`, `define-analysis-framework`,
  or `define-rule-framework` — all fixed inputs (`design.md` Context;
  Cross-Capability Impacts, which found no gap requiring one).

## 1. `aip-findings` Module Scaffolding and Dependency-Graph Guard

- [ ] 1.1 Add the `aip-findings` Maven module (Java 21+) as a sibling of
  `aip-csm-builder`, `aip-analysis`, and `aip-rules`, per `design.md`
  Decision 10's dependency diagram.
- [ ] 1.2 Configure `aip-findings`'s POM to depend on `aip-core` only —
  no dependency on `aip-rules`, `aip-analysis`, `aip-csm-builder`, or
  `aip-analyzer` (`design.md` Decisions 10, 11; spec `CSM and Rule
  Evaluation Content Reached Only Through Established Contracts`).
  Depends on: 1.1.
- [ ] 1.3 Add a dependency-graph CI check that fails the build if
  `aip-findings` gains any non-test dependency outside `aip-core`,
  mirroring `aip-csm-builder`'s, `aip-analysis`'s, and `aip-rules`'s
  own guards (`design.md` Decision 10). Depends on: 1.2.
- [ ] 1.4 Set up shared test infrastructure and directory conventions
  for `aip-findings`, consistent with `aip-csm-builder`/`aip-analysis`/
  `aip-rules`. Depends on: 1.1.

## 2. `aip-core`: `RuleEvaluationResultSource` Contract

- [ ] 2.1 Define the `RuleEvaluationResultSource` contract in
  `aip-core`: retrieval of a RuleEvaluationResult by its own identity,
  and retrieval of every RuleEvaluationResult produced by a given Rule
  identifier against a given source CSM Snapshot identity (`design.md`
  Decision 11; spec `Rule Evaluation Result Source Shape`). Depends
  on: 1.1 (module present; contract itself lives in `aip-core` and has
  no `aip-findings` dependency).
- [ ] 2.2 Add contract tests for `RuleEvaluationResultSource`
  independent of any concrete implementation, mirroring
  `AnalysisResultSource`'s own contract-test pattern (spec `Rule
  Evaluation Result Source Shape`, scenario: Finding Model is agnostic
  to RuleEvaluationResult production). Depends on: 2.1.

## 3. Finding Domain/Artifact Model

- [ ] 3.1 Define the Finding artifact type: Finding ID (Evaluation
  Identity), Logical Finding Identity, producing Rule identifier,
  Category, Severity, Confidence, Location, Evidence reference,
  Description, Impact — with no Recommendation or Remediation-
  availability field populated (`design.md` Decisions 3, 5; spec
  `Finding Shape`). Depends on: 2.1.
- [ ] 3.2 Implement Finding as an artifact distinct from
  RuleEvaluationResult — no shared representation, no relabeling
  (`design.md` Decision 2; spec `Finding Is a Distinct Artifact From
  RuleEvaluationResult`). Depends on: 3.1.
- [ ] 3.3 Implement the Finding's RuleEvaluationResult reference as a
  non-empty set type, capable of holding more than one identity
  (`design.md` Decision 2; spec `Finding-to-RuleEvaluationResult
  Reference Set and V1 Multiplicity`). Depends on: 3.1.

## 4. Evaluation Identity

- [ ] 4.1 Implement Evaluation Identity as a deterministic function of
  the complete set of a Finding's referenced RuleEvaluationResult
  identities (`design.md` Decision 3; spec `Evaluation Identity`).
  Depends on: 3.3.
- [ ] 4.2 Add tests confirming identical RuleEvaluationResult sets
  produce identical Evaluation Identity, and differing sets produce
  distinguishable Evaluation Identity (spec `Evaluation Identity`,
  both scenarios). Depends on: 4.1.
- [ ] 4.3 Add a test confirming Evaluation Identity changes whenever
  the underlying evaluation run changes, even when the concerned CSM
  element is unchanged — i.e., that it remains snapshot-bound
  (`design.md` Decision 3). Depends on: 4.1.

## 5. Logical Finding Identity

- [ ] 5.1 Implement Logical Finding Identity as a deterministic
  function of the producing Rule's identifier (excluding version) and
  the concerned CSM element identity — the Rule Scope instance's
  containment-level anchor element, or the Repository element for an
  unanchored Rule Scope instance (`design.md` Decision 3; spec
  `Logical Finding Identity`). Depends on: 3.1.
- [ ] 5.2 Enforce that Logical Finding Identity computation reads only
  the anchor element identity already present in the referenced
  RuleEvaluationResult's Rule Scope instance — never a relationship
  identity, and never any content read from that RuleEvaluationResult's
  opaque, Rule-Type-defined payload (`design.md` Decision 3's
  relationship-anchor-exclusion paragraph; spec `Logical Finding
  Identity`, scenario: A relationship-reading Rule Type resolves to
  its anchor element, not a relationship identity). Depends on: 5.1.
- [ ] 5.3 Add tests: same Rule + same concerned element across two CSM
  Snapshots yields the same Logical Finding Identity; same Rule at two
  versions yields the same Logical Finding Identity; different
  concerned elements yield distinguishable Logical Finding Identities;
  an unanchored Rule Scope instance resolves to the Repository
  element's identity (spec `Logical Finding Identity`, all five
  scenarios). Depends on: 5.1, 5.2.
- [ ] 5.4 Implement Evaluation Identity and Logical Finding Identity as
  two independently-computed values retained separately on a Finding,
  with no Finding Model operation that conflates, merges, or derives
  one from the other (`design.md` Decision 3; spec `Evaluation
  Identity and Logical Finding Identity Are Distinct`, both
  scenarios). Depends on: 4.1, 5.1.

## 6. Finding Provenance and RuleEvaluationResult Relationship

- [ ] 6.1 Enforce that Finding construction requires a non-empty
  RuleEvaluationResult reference set, failing construction if none is
  supplied (`design.md` Decision 1; spec `Finding Provenance Requires
  At Least One RuleEvaluationResult`). Depends on: 3.3.
- [ ] 6.2 Confirm no Finding Model operation exists that produces a
  Finding independent of at least one RuleEvaluationResult — no
  AI-generated or manually-authored Finding path (`design.md`
  Decision 1; spec `Finding Provenance Requires At Least One
  RuleEvaluationResult`, scenario: No mechanism exists for
  provenance-independent Findings). Depends on: 6.1.
- [ ] 6.3 Add a test confirming a RuleEvaluationResult is never treated
  as a Finding until the Finding Model constructs one referencing it
  (spec `Finding Is a Distinct Artifact From RuleEvaluationResult`).
  Depends on: 3.2.

## 7. Concerned CSM Element Identity / Location Semantics

- [ ] 7.1 Implement extraction of the concerned CSM element identity
  from a referenced RuleEvaluationResult's Rule Scope instance
  (anchored case) or the Repository element (unanchored case), reused
  by both Logical Finding Identity (Section 5) and the Finding's
  Location field (`design.md` Decisions 3, 6; spec `Logical Finding
  Identity`, `Finding Shape`). Depends on: 5.1.
- [ ] 7.2 Populate the Finding's Location field directly from this
  extraction, retained on the Finding itself rather than requiring a
  consumer to resolve it from a referenced RuleEvaluationResult (spec
  `Finding Traceability`, scenario: Concerned CSM element identity is
  directly retained). Depends on: 7.1.

## 8. Severity, Category, Confidence, Recommendation, and Remediation Metadata Semantics

- [ ] 8.1 Implement Severity and Category as values copied unmodified
  from the producing Rule Type's static configuration — never computed,
  inferred, or varied by Finding Model (`design.md` Decision 5; spec
  `Severity and Category Are Rule-Type-Declared Configuration`).
  Depends on: 3.1.
- [ ] 8.2 Implement Confidence as a single, fixed, maximal value for
  every v1 Finding, regardless of Rule or CSM content (`design.md`
  Decision 5; spec `Finding Shape`, scenario: Confidence is fixed for
  every v1 Finding). Depends on: 3.1.
- [ ] 8.3 Enforce that Recommendation and Remediation-availability are
  never populated with content by Finding Model in v1 (`design.md`
  Decision 5; spec `Finding Shape`, scenario: Recommendation and
  remediation availability are not populated). Depends on: 3.1.
- [ ] 8.4 Add a test confirming Finding Model does not vary Severity
  based on aggregation, run-time computation, or any heuristic (spec
  `Severity and Category Are Rule-Type-Declared Configuration`,
  scenario: Finding Model does not compute Severity from context).
  Depends on: 8.1.

## 9. Finding Evidence and Traceability

- [ ] 9.1 Implement the Finding's Evidence reference as CSM
  element/relationship identities and provenance references reachable
  through the CSM's own Structured Provenance Record mechanism — never
  raw Repository Evidence content (`design.md` Decision 6; spec
  `Finding Evidence Means Traceability Content, Not Repository
  Evidence`). Depends on: 3.1.
- [ ] 9.2 Enforce that Finding Model never reads Repository Evidence
  directly while constructing a Finding's Evidence reference (spec
  `Finding Evidence Means Traceability Content, Not Repository
  Evidence`, scenario: Finding Model does not read Repository
  Evidence). Depends on: 9.1.
- [ ] 9.3 Implement direct retention, on every Finding, of: source CSM
  Snapshot identity, concerned CSM element identity, producing Rule
  identifier, and the complete referenced RuleEvaluationResult identity
  set — each determinable without resolving a referenced
  RuleEvaluationResult first (`design.md` Decision 6; spec `Finding
  Traceability`, all three scenarios). Depends on: 4.1, 5.1, 7.2.

## 10. Finding Creation/Promotion From RuleEvaluationResults

- [ ] 10.1 Implement Finding construction as a pure, deterministic
  function of its referenced RuleEvaluationResult(s) — no AI/LLM call,
  no heuristic or probabilistic step, no random identity generation
  (`design.md` Decisions 1, 3; spec `Deterministic, Declarative
  Finding Construction Only`). Depends on: 4.1, 5.1, 8.1, 9.1.
- [ ] 10.2 Add a repeatability test: constructing a Finding twice from
  the same RuleEvaluationResult identity set produces identical
  Evaluation Identity and identical Logical Finding Identity (spec
  `Deterministic, Declarative Finding Construction Only`, scenario:
  Repeated construction over unchanged input is identical). Depends
  on: 10.1.
- [ ] 10.3 Implement v1 construction behavior producing exactly one
  Finding per qualifying RuleEvaluationResult, with its reference set
  containing exactly that one identity (`design.md` Decisions 2, 7;
  spec `Finding-to-RuleEvaluationResult Reference Set and V1
  Multiplicity`, scenario: V1 construction produces exactly one
  reference per Finding). Depends on: 3.3, 10.1.

## 11. Finding Immutability

- [ ] 11.1 Implement Finding as immutable once constructed and
  validated — no operation that alters its identity, referenced
  RuleEvaluationResult set, or any other field after construction
  (`design.md` Decision 4; spec `Finding Immutability`, scenario: No
  operation modifies an existing Finding). Depends on: 3.1.
- [ ] 11.2 Implement re-evaluation with a changed outcome as
  construction of a new, separately identified Finding — never a
  mutation of the prior one, sharing its Logical Finding Identity while
  differing in Evaluation Identity (`design.md` Decision 4; spec
  `Finding Immutability`, scenario: A changed outcome produces a new
  Finding, not a mutation). Depends on: 11.1, 5.1, 4.1.

## 12. Cross-Snapshot Logical Finding Identity Comparison Semantics

- [ ] 12.1 Ensure Logical Finding Identity values are exposed in a form
  a consumer can compare across two evaluation runs' Findings without
  resolving either run's RuleEvaluationResults or CSM Snapshot content
  (`design.md` Decision 4; spec `Cross-Snapshot Comparison via Logical
  Finding Identity`, scenario: Logical Finding Identity presence is
  comparable across two runs). Depends on: 5.1.
- [ ] 12.2 Confirm Finding construction attaches no open/resolved/new/
  or other lifecycle-state label to a Finding — comparison semantics
  are exposed, not interpreted, by Finding Model (spec `Cross-Snapshot
  Comparison via Logical Finding Identity`, scenario: Finding Model
  does not itself label a comparison as resolved or new). Depends
  on: 12.1.

## 13. Explicit Lifecycle Boundary — No Mutable State, Events, or Presentation in V1

- [ ] 13.1 Confirm the Finding type (Section 3) carries no field that
  is mutated in place to reflect a lifecycle-state change (`design.md`
  Decision 4; spec `No Mutable Lifecycle State or Lifecycle Event
  Artifact in This Version`, scenario: Finding carries no mutable
  lifecycle-state field). Depends on: 3.1.
- [ ] 13.2 Confirm no lifecycle-event or status-transition artifact
  type is introduced anywhere in `aip-findings` (spec `No Mutable
  Lifecycle State or Lifecycle Event Artifact in This Version`,
  scenario: No lifecycle-event artifact is produced). Depends on: 1.1.
- [ ] 13.3 Confirm `aip-findings` offers no query, view, dashboard, or
  workflow presenting lifecycle state to a consumer beyond exposing
  Logical Finding Identity and Evaluation Identity on each Finding
  (`design.md` Decision 4; spec `Finding Lifecycle Presentation Is Out
  of Scope`). Depends on: 12.1.

## 14. Finding Validation

- [ ] 14.1 Implement `FindingValidator`, checking: every referenced
  RuleEvaluationResult identity exists and is already published; the
  Finding's Evaluation Identity and Logical Finding Identity are each
  independently recomputable from, and consistent with, its referenced
  RuleEvaluationResult set; and the reference set is non-empty
  (`design.md` Decision 9; spec `Finding Validation Before
  Publication`). Depends on: 4.1, 5.1, 6.1, 9.3, 2.1.
- [ ] 14.2 Enforce that `FindingValidator` does not re-verify a
  referenced RuleEvaluationResult's own consumed Analysis Result or
  CSM element/Subject identities — it trusts that
  `RuleEvaluationResultValidator` already established that
  (`design.md` Decision 9's trust-boundary reasoning; spec `Finding
  Validation Before Publication`, scenario: Validation does not
  re-verify the upstream Analysis-to-Rule chain). Depends on: 14.1.
- [ ] 14.3 Add tests for all four validation scenarios: valid Finding
  published; Finding referencing an unpublished RuleEvaluationResult
  rejected; Finding with an empty reference set rejected; Finding with
  inconsistent identity rejected (spec `Finding Validation Before
  Publication`, first four scenarios). Depends on: 14.1.

## 15. Finding Publishing

- [ ] 15.1 Implement `FindingPublisher`, calling `FindingStore.write`
  only for a Finding that passes `FindingValidator` (`design.md`
  Decision 9). Depends on: 14.1, 16.1.
- [ ] 15.2 Add a test confirming a Finding that fails validation is not
  published (spec `Finding Validation Before Publication`, covered
  jointly with Section 14.3's rejection scenarios — verified here at
  the publish boundary specifically). Depends on: 15.1.

## 16. Result/Persistence Abstraction

- [ ] 16.1 Define the `FindingStore` abstraction (interface only; no
  concrete implementation) mirroring `RuleEvaluationResultStore`'s and
  `AnalysisResultStore`'s own precedent (`design.md` Decision 9,
  Non-Goals; spec `Findings Are Durable, Individually Identifiable
  Artifacts`). Depends on: 3.1.
- [ ] 16.2 Add tests confirming a Finding remains retrievable by its
  Evaluation Identity after the producing run's process has exited,
  and that a new, differently-identified Finding never overwrites a
  prior one (spec `Findings Are Durable, Individually Identifiable
  Artifacts`, both scenarios). Depends on: 16.1, 4.1.

## 17. Extension Mechanism for Future Rule Types

- [ ] 17.1 Confirm Finding construction, identity, traceability, and
  validation mechanisms (Sections 3–5, 9, 14) contain no Rule-Type-
  specific branching — they operate generically over any
  RuleEvaluationResult, reading only its standardized traceability
  fields and its producing Rule Type's declared Category/Severity
  configuration (`design.md`'s extensibility corollary after Decision
  11; spec `Extension Behavior — New Rule Types Require No Finding
  Model Changes`). Depends on: 5.2, 8.1, 14.1.
- [ ] 17.2 Add a test exercising a second, structurally distinct
  example Rule Type (test scope only) — a different declared Category/
  Severity, a different Rule Scope shape — proving Finding
  construction, identity, traceability, and validation apply unchanged
  (spec `Extension Behavior — New Rule Types Require No Finding Model
  Changes`, scenario: A new Rule Type's RuleEvaluationResults are
  consumed without Finding Model changes). Depends on: 17.1.

## 18. Dependency-Boundary and Architectural-Invariant Tests

- [ ] 18.1 Add a dependency-graph test proving `aip-findings` has no
  reachable dependency on `aip-rules`, `aip-analysis`,
  `aip-csm-builder`, or `aip-analyzer` (`design.md` Decisions 10, 11;
  spec `Rule Evaluation Result Consumption Through the Rule Evaluation
  Result Source Only`). Depends on: 1.3.
- [ ] 18.2 Add a test confirming `aip-findings` consumes
  RuleEvaluationResults exclusively through `RuleEvaluationResultSource`
  — never through a direct dependency on the module that produced them
  (spec `Rule Evaluation Result Consumption Through the Rule Evaluation
  Result Source Only`). Depends on: 18.1, 2.1.
- [ ] 18.3 Add a test confirming Finding Model reads no Repository
  Evidence and depends on no Runtime Model content, directly or
  transitively (spec `CSM and Rule Evaluation Content Reached Only
  Through Established Contracts`, both scenarios). Depends on: 18.1.
- [ ] 18.4 Add a test confirming `aip-findings` requires no Scope
  declaration of any kind, and that Finding targeting is determined
  solely from the concerned CSM element identity already present in a
  referenced RuleEvaluationResult's Rule Scope instance (`design.md`
  Decision 8; spec `Finding Model Has No Scope Concept`, both
  scenarios). Depends on: 7.1.
- [ ] 18.5 Add tests confirming producing a Finding never alters CSM
  content, Analysis Results, or RuleEvaluationResults, and that a
  CSM/Analysis-Result/RuleEvaluationResult query never returns Findings
  (spec `Findings Are a Distinct Concept From CSM Knowledge, Analysis
  Results, and Rule Evaluation Results`, both scenarios). Depends
  on: 3.2, 16.1.
- [ ] 18.6 Add a test confirming every Finding produced by one
  construction run references exactly one repository's CSM Snapshot
  identity (`design.md` Context's single-repository-scope corollary;
  spec `Single-Repository Finding Scope`). Depends on: 9.3.
- [ ] 18.7 Add a test confirming no aggregation occurs — one Finding
  per qualifying RuleEvaluationResult, never more than one
  RuleEvaluationResult combined into a single Finding (`design.md`
  Decision 7; spec `Aggregation Is Not Performed in This Version`).
  Depends on: 10.3.

## 19. Fixture/Test Infrastructure

- [ ] 19.1 Build fixture RuleEvaluationResults (anchored and
  unanchored Rule Scope instances, PASS/FAIL/NOT_APPLICABLE outcomes,
  single- and multi-Analyzer consumed-AnalysisResult sets) sufficient
  to exercise Sections 4–14 without depending on a real Rule Framework
  implementation. Depends on: 2.1.
- [ ] 19.2 Build a fixture `RuleEvaluationResultSource` implementation
  (in-memory, test scope) satisfying the contract from Section 2, for
  use by every other section's tests. Depends on: 2.2, 19.1.
- [ ] 19.3 Build a fixture `FindingStore` implementation (in-memory,
  test scope) satisfying Section 16's abstraction, for use by Sections
  14–17's tests. Depends on: 16.1.

## 20. Requirement/Scenario Traceability and End-to-End Verification

- [ ] 20.1 Build a traceability matrix mapping each of
  `specs/finding-model/spec.md`'s 25 requirements and 52 scenarios to
  the task(s) and test(s) that implement/verify it, following
  `implement-csm-builder`'s own `traceability.md` precedent. Depends
  on: Sections 1–19.
- [ ] 20.2 Confirm every one of `design.md`'s 11 Decisions (plus its
  two corollaries — single-repository scope and extension behavior —
  and Decision 3's relationship-anchor-exclusion paragraph) has at
  least one corresponding implementation task and test above; record
  any gap found rather than silently leaving it uncovered. Depends
  on: 20.1.
- [ ] 20.3 Add an end-to-end test: construct, validate, and publish a
  Finding from a fixture RuleEvaluationResult, then retrieve it by
  Evaluation Identity and independently recompute its Logical Finding
  Identity, confirming the full path (Sections 2–3, 10, 14–16) is
  consistent end to end. Depends on: 15.1, 16.2.
- [ ] 20.4 Run full validation (`openspec validate finding-model
  --strict` once specs are synced, or the equivalent check available
  during implementation) and record the result in this change's
  verification artifact. Depends on: 20.1, 20.2.
