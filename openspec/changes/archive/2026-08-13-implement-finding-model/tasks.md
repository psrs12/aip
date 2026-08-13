## Scope Notes — Explicitly Deferred (Not Tasks in This Change)

The following are deliberately out of scope and listed here for
traceability only — not as checkbox tasks:

- AI-generated or manually-authored Finding creation (`define-finding-
  model/design.md` Decision 1).
- Finding lifecycle workflow/UI/presentation, mutable lifecycle state,
  and lifecycle-event artifacts (`define-finding-model/design.md`
  Decision 4).
- Recommendation generation and remediation workflow (`define-finding-
  model/design.md` Decision 5, Non-Goals).
- Concrete `FindingStore` persistence technology (this change's own
  `design.md` Decision 8, Non-Goals) — only the abstraction is in
  scope.
- Finding aggregation/deduplication beyond the v1 1:1 relationship
  (`define-finding-model/design.md` Decision 7).
- A real `aip-rules` adapter (this change's own `design.md` Binding
  Decision — fixtures only).
- Reaching the literal `aip-csm-builder`-constructed Repository
  element identity for the unanchored case (this change's own
  `design.md` Decision 5 — a named, accepted v1 limitation).
- Any change to `canonical-software-model`, `software-repository-
  understanding`, `csm-builder`, `define-analysis-framework`,
  `define-rule-framework`, `implement-analysis-framework`,
  `implement-rule-framework`, or `define-finding-model`'s own
  specification.

## 1. `aip-findings` Module Scaffolding and Dependency-Graph Guard

- [x] 1.1 Add the `aip-findings` Maven module (Java 21+) as a sibling
  of `aip-csm-builder`, `aip-analysis`, and `aip-rules`, per this
  change's own `design.md` Decision 9.
- [x] 1.2 Configure `aip-findings`'s POM to depend on `aip-core` only —
  no dependency on `aip-rules`, `aip-analysis`, `aip-csm-builder`, or
  `aip-analyzer` (`design.md` Decision 9; `define-finding-model/
  design.md` Decisions 10, 11; spec `CSM and Rule Evaluation Content
  Reached Only Through Established Contracts`). Depends on: 1.1.
- [x] 1.3 Add the standard `check-module-dependencies.sh` and
  `check-fixture-package-scope.sh` guard executions, mirroring every
  sibling module. Depends on: 1.2.
- [x] 1.4 Wire three executions of the generic `scripts/check-no-
  module-reference.sh` (built during `implement-rule-framework`)
  against `aip-findings`: forbidding `aip\.rules\.`, `aip\.analysis\.`,
  and `aip\.csmbuilder\.` (`design.md` Decision 9). Depends on: 1.3.
- [x] 1.5 Set up shared test infrastructure and directory conventions
  for `aip-findings`, consistent with `aip-csm-builder`/`aip-analysis`/
  `aip-rules`. Depends on: 1.1.

## 2. `aip-core`: `RuleEvaluationResultSource` Contract

- [x] 2.1 Define the `RuleEvaluationResultSource` contract in
  `aip-core` (`aip.core.csm`): `read(RuleEvaluationResultId):
  Optional<RuleEvaluationResult>`, `list(ruleIdentifier: String,
  snapshotId: CsmSnapshotId): Set<RuleEvaluationResult>` (`design.md`
  Decision 7; `define-finding-model/design.md` Decision 11; spec `Rule
  Evaluation Result Source Shape`). Depends on: 1.1.
- [x] 2.2 Add contract tests for `RuleEvaluationResultSource`
  independent of any concrete implementation, mirroring
  `AnalysisResultSource`'s own contract-test pattern (spec `Rule
  Evaluation Result Source Shape`, scenario: Finding Model is agnostic
  to RuleEvaluationResult production). Depends on: 2.1.

## 3. `aip-core`: `FindingMetadata` Contract and Outcome Qualification

- [x] 3.1 Define `FindingMetadata` in `aip-core` (`aip.core.csm`):
  `category(): String`, `severity(): String`, `description(): String`,
  `impact(): String` (`design.md` Decision 3). Depends on: 1.1.
- [x] 3.2 Implement the outcome-qualification rule: a
  `RuleEvaluationResult` qualifies for Finding construction iff
  `outcome() == FAIL` and `payload() instanceof FindingMetadata`;
  every other combination produces no Finding, silently (`design.md`
  Decision 4). Depends on: 3.1, 2.1.
- [x] 3.3 Add tests confirming: `PASS` never qualifies; `NOT_APPLICABLE`
  never qualifies; `FAIL` with a non-`FindingMetadata` payload does not
  qualify; `FAIL` with a `FindingMetadata`-conforming payload qualifies
  (`design.md` Decision 4). Depends on: 3.2.

## 4. Finding Domain/Artifact Model

- [x] 4.1 Define `Finding` as a final, non-generic `aip-core` type:
  Finding ID (Evaluation Identity), Logical Finding Identity, producing
  Rule identifier, source CSM Snapshot identity, concerned CSM element
  identity, non-empty referenced `RuleEvaluationResultId` set,
  Category, Severity, Confidence, Description, Impact — no
  Recommendation or Remediation-availability field (`design.md`
  Decision 1; `define-finding-model/design.md` Decisions 3, 5; spec
  `Finding Shape`). Depends on: 2.1, 3.1.
- [x] 4.2 Implement Finding as an artifact distinct from
  `RuleEvaluationResult` — no shared representation, no relabeling
  (`define-finding-model/design.md` Decision 2; spec `Finding Is a
  Distinct Artifact From RuleEvaluationResult`). Depends on: 4.1.
- [x] 4.3 Implement the Finding's `RuleEvaluationResultId` reference as
  a non-empty `Set` type, capable of holding more than one identity
  (`define-finding-model/design.md` Decision 2; spec `Finding-to-
  RuleEvaluationResult Reference Set and V1 Multiplicity`). Depends
  on: 4.1.

## 5. Evaluation Identity

- [x] 5.1 Define `EvaluationIdentity` in `aip-core`: a deterministic
  function of the complete set of referenced `RuleEvaluationResultId`
  values, sorted before hashing for `Set`-order independence
  (`design.md` Decision 2; `define-finding-model/design.md` Decision 3;
  spec `Evaluation Identity`). Depends on: 4.3.
- [x] 5.2 Add tests: identical `RuleEvaluationResultId` sets produce
  identical `EvaluationIdentity`; differing sets produce distinguishable
  `EvaluationIdentity`; set order does not affect the result (spec
  `Evaluation Identity`, both scenarios). Depends on: 5.1.
- [x] 5.3 Add a test confirming `EvaluationIdentity` changes whenever
  the underlying evaluation run changes, even when the concerned CSM
  element is unchanged (`define-finding-model/design.md` Decision 3).
  Depends on: 5.1.

## 6. Logical Finding Identity

- [x] 6.1 Define `LogicalFindingIdentity` in `aip-core`: a deterministic
  function of the producing Rule's identifier (excluding version) and
  the concerned CSM element identity — deliberately excluding source
  CSM Snapshot identity, `RuleEvaluationResultId`, and consumed
  `AnalysisResultId`s (`design.md` Decision 2; `define-finding-model/
  design.md` Decision 3; spec `Logical Finding Identity`). Depends
  on: 4.1.
- [x] 6.2 Implement concerned-element extraction: anchored Rule Scope
  instance -> its `anchorElementId()`; unanchored Rule Scope instance ->
  the Finding-Model-local `repositoryIdentifier`-derived synthetic
  element identity (`design.md` Decision 5). Depends on: 6.1.
- [x] 6.3 Enforce that Logical Finding Identity computation never reads
  a relationship identity or any content from a `RuleEvaluationResult`'s
  opaque payload beyond `FindingMetadata` (`define-finding-model/
  design.md` Decision 3's relationship-anchor-exclusion paragraph;
  spec: A relationship-reading Rule Type resolves to its anchor
  element, not a relationship identity). Depends on: 6.2.
- [x] 6.4 Add tests: same Rule + same concerned element across two CSM
  Snapshots -> same Logical Finding Identity; same Rule at two versions
  -> same Logical Finding Identity; different concerned elements ->
  distinguishable Logical Finding Identity; an unanchored Rule Scope
  instance resolves to the synthetic repository-subject identity (spec
  `Logical Finding Identity`, all five scenarios). Depends on: 6.2, 6.3.
- [x] 6.5 Implement Evaluation Identity and Logical Finding Identity as
  two independently-computed, separately retained values, with no
  operation that conflates or derives one from the other (spec
  `Evaluation Identity and Logical Finding Identity Are Distinct`, both
  scenarios). Depends on: 5.1, 6.1.

## 7. Finding Provenance and RuleEvaluationResult Relationship

- [x] 7.1 Enforce that Finding construction requires a non-empty
  qualifying `RuleEvaluationResult` reference set, failing construction
  if none is supplied (`define-finding-model/design.md` Decision 1;
  spec `Finding Provenance Requires At Least One RuleEvaluationResult`).
  Depends on: 4.3, 3.2.
- [x] 7.2 Confirm no Finding Model operation exists that produces a
  Finding independent of at least one `RuleEvaluationResult` (spec:
  No mechanism exists for provenance-independent Findings). Depends
  on: 7.1.
- [x] 7.3 Add a test confirming a `RuleEvaluationResult` is never
  treated as a Finding until Finding construction produces one
  referencing it (spec `Finding Is a Distinct Artifact From
  RuleEvaluationResult`). Depends on: 4.2.

## 8. Finding Construction (`FindingConstructor`)

- [x] 8.1 Implement `FindingConstructor`: given a qualifying
  `RuleEvaluationResult` (Section 3), constructs exactly one `Finding`
  — Evaluation Identity from its own identity (v1 singleton reference
  set, Section 5), Logical Finding Identity from its Rule identifier
  and concerned element (Section 6), Category/Severity/Description/
  Impact copied unmodified from its payload's `FindingMetadata` (never
  computed or varied), Confidence fixed to `Confidence.HIGH` (`design.md`
  Decisions 4, 6; `define-finding-model/design.md` Decisions 2, 5, 7).
  Depends on: 5.1, 6.1, 3.2, 4.1.
- [x] 8.2 Implement `FindingConstructor` as a pure, deterministic
  function — no AI/LLM call, no heuristic or probabilistic step, no
  random identity generation (spec `Deterministic, Declarative Finding
  Construction Only`). Depends on: 8.1.
- [x] 8.3 Add a repeatability test: constructing a Finding twice from
  the same `RuleEvaluationResult` produces identical Evaluation
  Identity and identical Logical Finding Identity (spec: Repeated
  construction over unchanged input is identical). Depends on: 8.2.
- [x] 8.4 Add a test confirming Severity/Category are never varied
  based on aggregation, run-time computation, or any heuristic (spec
  `Severity and Category Are Rule-Type-Declared Configuration`,
  scenario: Finding Model does not compute Severity from context).
  Depends on: 8.1.
- [x] 8.5 Add a test confirming Recommendation/Remediation-availability
  content is never populated (spec `Finding Shape`, scenario:
  Recommendation and remediation availability are not populated).
  Depends on: 8.1.

## 9. Finding Evidence and Traceability

- [x] 9.1 Implement the Finding's Evidence reference as the concerned
  CSM element identity plus the referenced `RuleEvaluationResultId`
  set (from which producing Rule and consumed `AnalysisResultId`s are
  already reachable) — never raw Repository Evidence content
  (`define-finding-model/design.md` Decision 6; spec `Finding Evidence
  Means Traceability Content, Not Repository Evidence`). Depends
  on: 8.1.
- [x] 9.2 Enforce that Finding construction never reads Repository
  Evidence directly (spec: Finding Model does not read Repository
  Evidence). Depends on: 9.1.
- [x] 9.3 Implement direct retention, on every Finding, of: source CSM
  Snapshot identity, concerned CSM element identity, producing Rule
  identifier, and the complete referenced `RuleEvaluationResultId` set
  — each determinable without resolving a referenced
  `RuleEvaluationResult` first (spec `Finding Traceability`, all three
  scenarios). Depends on: 4.1, 5.1, 6.1.

## 10. Finding Immutability

- [x] 10.1 Confirm `Finding` offers no operation that alters any field,
  Evaluation Identity, or Logical Finding Identity after construction
  (spec `Finding Immutability`, scenario: No operation modifies an
  existing Finding). Depends on: 4.1.
- [x] 10.2 Add a test confirming re-evaluation with a changed outcome
  produces a new, separately identified Finding — sharing the prior
  Finding's Logical Finding Identity while differing in Evaluation
  Identity, never a mutation of the prior one (spec: A changed outcome
  produces a new Finding, not a mutation). Depends on: 8.2, 6.1, 5.1.

## 11. Cross-Snapshot Comparison Semantics and Lifecycle Boundary

- [x] 11.1 Add a test confirming Logical Finding Identity values can be
  compared across two evaluation runs' Findings without resolving
  either run's `RuleEvaluationResult`s (spec `Cross-Snapshot Comparison
  via Logical Finding Identity`). Depends on: 6.1.
- [x] 11.2 Confirm Finding construction attaches no open/resolved/new/
  or lifecycle-state label to a Finding (spec: Finding Model does not
  itself label a comparison as resolved or new). Depends on: 8.1.
- [x] 11.3 Confirm `Finding` carries no mutable lifecycle-state field,
  and that `aip-findings` introduces no lifecycle-event/status-
  transition artifact type or presentation mechanism (spec `No Mutable
  Lifecycle State or Lifecycle Event Artifact in This Version`,
  `Finding Lifecycle Presentation Is Out of Scope`). Depends on: 4.1.

## 12. Finding Validation (`FindingValidator`)

- [x] 12.1 Implement `FindingValidator`, checking: every referenced
  `RuleEvaluationResultId` exists and is resolvable via
  `RuleEvaluationResultSource.read(...)`; Evaluation Identity and
  Logical Finding Identity are each independently recomputable from,
  and consistent with, the Finding's referenced set/`(ruleIdentifier,
  concernedElementId)` pair; the reference set is non-empty
  (`design.md` Decision 8; spec `Finding Validation Before
  Publication`). Depends on: 5.1, 6.1, 7.1, 9.3, 2.1.
- [x] 12.2 Enforce that `FindingValidator` never re-verifies a
  referenced `RuleEvaluationResult`'s own consumed `AnalysisResultId`s
  or CSM element/Subject identities — trusting
  `RuleEvaluationResultValidator`'s own already-completed work
  (`design.md` Decision 8's trust-boundary reasoning; spec: Validation
  does not re-verify the upstream Analysis-to-Rule chain). Depends
  on: 12.1.
- [x] 12.3 Add tests for all four validation scenarios: valid Finding
  published; Finding referencing an unpublished/unresolvable
  `RuleEvaluationResult` rejected; Finding with an empty reference set
  rejected; Finding with inconsistent identity rejected (spec `Finding
  Validation Before Publication`, first four scenarios). Depends
  on: 12.1.

## 13. Finding Publishing (`FindingPublisher`)

- [x] 13.1 Implement `FindingPublisher`, calling `FindingStore.write`
  only for a Finding that passes `FindingValidator` (`design.md`
  Decision 8). Depends on: 12.1, 14.1.
- [x] 13.2 Add a test confirming a Finding that fails validation is
  never published. Depends on: 13.1.

## 14. `FindingStore` Abstraction

- [x] 14.1 Define the `FindingStore` abstraction (interface only; no
  concrete implementation) in `aip-findings`, not `aip-core`
  (`design.md` Decision 8; spec `Findings Are Durable, Individually
  Identifiable Artifacts`). Depends on: 4.1.
- [x] 14.2 Add tests confirming a Finding remains retrievable by its
  Evaluation Identity after the producing run's process has exited,
  and that a new, differently-identified Finding never overwrites a
  prior one (spec, both scenarios). Depends on: 14.1, 5.1.

## 15. Extension Mechanism for Future Rule Types

- [x] 15.1 Confirm Finding construction, identity, traceability, and
  validation (Sections 4–9, 12) contain no Rule-Type-specific
  branching — they operate generically over any qualifying
  `RuleEvaluationResult`, reading Category/Severity/Description/Impact
  only through the `FindingMetadata` contract (`design.md` Decision 3;
  spec `Extension Behavior — New Rule Types Require No Finding Model
  Changes`). Depends on: 6.3, 8.4, 12.1.
- [x] 15.2 Add a test exercising a second, structurally distinct
  example `FindingMetadata`-conforming payload (different Category/
  Severity, different anchored/unanchored Rule Scope shape), proving
  Finding construction/identity/traceability/validation apply
  unchanged (spec, scenario: A new Rule Type's RuleEvaluationResults
  are consumed without Finding Model changes). Depends on: 15.1.

## 16. Dependency-Boundary and Architectural-Invariant Tests

- [x] 16.1 Add a dependency-graph test proving `aip-findings` has no
  reachable dependency on `aip-rules`, `aip-analysis`,
  `aip-csm-builder`, or `aip-analyzer` (spec `Rule Evaluation Result
  Consumption Through the Rule Evaluation Result Source Only`).
  Depends on: 1.4.
- [x] 16.2 Add a test confirming Finding Model consumes
  `RuleEvaluationResult`s exclusively through
  `RuleEvaluationResultSource` — never through a direct dependency on
  the producing module. Depends on: 16.1, 2.1.
- [x] 16.3 Add a test confirming Finding Model reads no Repository
  Evidence and depends on no Runtime Model content, directly or
  transitively (spec `CSM and Rule Evaluation Content Reached Only
  Through Established Contracts`, both scenarios). Depends on: 16.1.
- [x] 16.4 Add a test confirming `aip-findings` requires no Scope
  declaration of any kind, and that Finding targeting is determined
  solely from the concerned CSM element identity already present in a
  referenced `RuleEvaluationResult`'s Rule Scope instance
  (`define-finding-model/design.md` Decision 8; spec `Finding Model Has
  No Scope Concept`, both scenarios). Depends on: 6.2.
- [x] 16.5 Add tests confirming producing a Finding never alters CSM
  content, Analysis Results, or `RuleEvaluationResult`s, and that a
  CSM/Analysis-Result/RuleEvaluationResult query never returns Findings
  (spec `Findings Are a Distinct Concept From CSM Knowledge, Analysis
  Results, and Rule Evaluation Results`, both scenarios). Depends
  on: 4.2, 14.1.
- [x] 16.6 Add a test confirming every Finding produced by one
  construction run references exactly one repository's CSM Snapshot
  identity (spec `Single-Repository Finding Scope`). Depends on: 9.3.
- [x] 16.7 Add a test confirming no aggregation occurs — one Finding
  per qualifying `RuleEvaluationResult`, never more than one combined
  into a single Finding (spec `Aggregation Is Not Performed in This
  Version`). Depends on: 8.1.

## 17. Fixture/Test Infrastructure

- [x] 17.1 Build a fixture `RuleEvaluationResult` construction helper
  (anchored and unanchored Rule Scope instances, all three outcomes,
  `FindingMetadata`-conforming and non-conforming payloads), using
  `RuleEvaluationResult.of(...)` directly — no CSM-element-graph
  builder needed (`design.md` Decision 10). Depends on: 3.1.
- [x] 17.2 Build `InMemoryRuleEvaluationResultSource`, satisfying the
  Section 2 contract. Depends on: 2.2, 17.1.
- [x] 17.3 Build `InMemoryFindingStore`, satisfying the Section 14
  abstraction. Depends on: 14.1.
- [x] 17.4 Build `StubFindingMetadata`, a simple `FindingMetadata`
  implementation for use across test classes. Depends on: 3.1.

## 18. Requirement/Scenario Traceability and End-to-End Verification

- [x] 18.1 Build a traceability matrix mapping each of
  `define-finding-model/specs/finding-model/spec.md`'s 25 requirements
  and 52 scenarios to the task(s)/test(s) that implement/verify it,
  following `implement-rule-framework`'s own `traceability.md`
  precedent. Depends on: Sections 1–17.
- [x] 18.2 Confirm every one of this change's own `design.md`'s 10
  Decisions, and every one of `define-finding-model/design.md`'s 11
  Decisions, has at least one corresponding implementation task and
  test above; record any gap found rather than silently leaving it
  uncovered. Depends on: 18.1.
- [x] 18.3 Add an end-to-end fixture-based test: construct, validate,
  and publish a Finding from a fixture qualifying `RuleEvaluationResult`,
  then retrieve it by Evaluation Identity and independently recompute
  its Logical Finding Identity, confirming the full path (Sections
  2–3, 8, 12–14) is consistent end to end; also confirm a non-
  qualifying `RuleEvaluationResult` (PASS, NOT_APPLICABLE, or FAIL
  without `FindingMetadata`) produces no Finding in the same run.
  Depends on: 13.1, 14.2.
- [x] 18.4 Run full validation (`openspec validate finding-model
  --strict` once specs are synced, or the equivalent check available
  during implementation) and record the result in this change's
  verification artifact. Depends on: 18.1, 18.2.

## 19. Architectural Invariant Enforcement

- [x] 19.1 Finalize the dependency-graph CI checks enforcing
  `aip-findings` depends on `aip-core` only. Depends on: 1.4.
- [x] 19.2 Final gate: confirm (via review checklist and CI) that this
  change contains no Agent Framework implementation, that
  `canonical-software-model`, `software-repository-understanding`,
  `csm-builder`, `define-analysis-framework`, `define-rule-framework`,
  `implement-analysis-framework`'s own specification,
  `implement-rule-framework`'s own specification, and
  `define-finding-model`'s own specification remain unmodified, and
  that every decision in `define-finding-model/design.md` (Decisions
  1-11) and this change's own `design.md` (Decisions 1-10) holds as
  implemented. Depends on: all preceding sections.
