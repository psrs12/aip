## Scope Notes — Explicitly Deferred (Not Tasks in This Change)

The following are deliberately out of scope and listed here for
traceability only — not as checkbox tasks:

- "Must only communicate via" and any other `boundary/constraint`
  constraint shape beyond "must not depend on" (`define-architecture-
  compliance-agent/design.md` Decision 3, Non-Goals).
- Any Agent input beyond a bare Finding (`define-architecture-
  compliance-agent/design.md` Decision 5).
- Generated code, diffs, or a Proposed Change of any kind
  (`define-architecture-compliance-agent/design.md` Decision 6).
- Severity differentiation by boundary provenance or any run-time
  computation (`define-architecture-compliance-agent/design.md`
  Decisions 4, 7).
- Any other `project.md` §5 agent.
- A new module, a new `aip-core` contract, or any new identity/
  traceability/validation mechanism (`define-architecture-compliance-
  agent/design.md` Decisions 2, 9).
- A real LLM/model-provider adapter (this change's own `design.md`
  Decision 6 — deterministic, template-based Agent in v1).
- A dedicated `constraintKind` field on `CsmRelationship` itself (this
  change's own `design.md` Decision 2 — `NativeAttributes` only).
- Any change to `canonical-software-model`, `software-repository-
  understanding`, `csm-builder`, `define-analysis-framework`,
  `define-rule-framework`, `define-finding-model`,
  `define-agent-framework`, `implement-analysis-framework`,
  `implement-rule-framework`, `implement-finding-model`,
  `implement-agent-framework`, or `define-architecture-compliance-
  agent`'s own specification.

## 1. Boundary-Compliance Rule Type Registration and Package Placement

- [x] 1.1 Add `BoundaryComplianceRuleType` to a new `aip.rules.
  boundarycompliance` subpackage within the already-existing
  `aip-rules` module — no new module (`design.md` Decision 1;
  `define-architecture-compliance-agent/design.md` Decisions 1, 2, 9;
  spec `Boundary-Compliance Rule Type Registration`).
- [x] 1.2 Add a test confirming registration (via the existing
  `RuleTypeRegistry`) requires no change to Rule Framework's own
  evaluation, identity, traceability, or validation mechanisms (spec,
  scenario: Boundary-compliance Rule Type registers without core
  mechanism changes). Depends on: 1.1.

## 2. Constraint-Kind Representation and Rule Scope Declaration

- [x] 2.1 Define `BoundaryConstraintKind` in `aip.rules.
  boundarycompliance`: the `constraint-kind` `NativeAttributes` key
  and the recognized `must-not-depend-on` value (`design.md` Decision
  2). Depends on: 1.1.
- [x] 2.2 Declare the boundary-compliance Rule Type's Rule Scope: the
  `DEPENDENCY` and `BOUNDARY_CONSTRAINT` CSM relationship kinds, with
  a containment-level anchor of `ARCHITECTURE_COMPONENT`, no native-
  attribute predicate on the Scope declaration itself (`design.md`
  Decision 3; `define-architecture-compliance-agent/design.md`
  Decision 3; spec `Boundary-Compliance Rule Scope Declaration`).
  Depends on: 1.1.
- [x] 2.3 Declare an empty set of required Analyzer identifiers — both
  relationship kinds are read directly from the Analysis View (spec
  `Boundary-Compliance Rule Type Declares No Analyzer Inputs`). Depends
  on: 2.2.
- [x] 2.4 Add tests confirming: the Rule Scope identifies both
  relationship kinds; the Rule Type is invoked once per Architecture
  Component present in the CSM Snapshot; the declared Analyzer set is
  empty; evaluation completes using only Analysis View content (spec,
  all four scenarios across both requirements). Depends on: 2.2, 2.3.

## 3. Boundary-Compliance Condition Logic — Must-Not-Depend-On

- [x] 3.1 Implement the per-constraint condition: for each applicable
  `BOUNDARY_CONSTRAINT` relationship sourced at the anchor and carrying
  `constraint-kind: must-not-depend-on` (Section 2.1), determine
  whether a `DEPENDENCY` relationship sourced at the anchor targets the
  same `CsmElementId` (`design.md` Decision 3; spec `Boundary-
  Compliance Condition — Must-Not-Depend-On Violations`). Depends
  on: 2.2, 2.1.
- [x] 3.2 Add tests confirming: no observed dependency to the target
  satisfies the condition; an observed dependency to the target leaves
  it unsatisfied (spec, both scenarios). Depends on: 3.1.
- [x] 3.3 Enforce that a `BOUNDARY_CONSTRAINT` relationship without the
  `constraint-kind: must-not-depend-on` `NativeAttributes` value is
  never evaluated as part of this condition (`design.md` Decisions 2,
  3; spec `Must-Only-Communicate-Via Constraints Are Out of Scope`).
  Depends on: 3.1.
- [x] 3.4 Add a test confirming a non-"must not depend on" constraint
  (e.g. a `BOUNDARY_CONSTRAINT` relationship with a different or absent
  `constraint-kind` value) is present on a Component but not evaluated.
  Depends on: 3.3.

## 4. Applicability, NOT_APPLICABLE, and Outcome Semantics

- [x] 4.1 Confirm kind-based applicability (no `DEPENDENCY` or
  `BOUNDARY_CONSTRAINT` relationship present -> no invocation, no
  Result) is enforced entirely by `CsmScopeEvaluator`'s own already-
  implemented mechanism — no new logic needed (spec `Kind-Based
  Applicability for Boundary-Compliance Checking`). Depends on: 2.2.
- [x] 4.2 Implement `NOT_APPLICABLE` determination: applicable
  (kind-based) but zero applicable "must not depend on" constraints
  present produces `RuleTypeEvaluation.notApplicable(...)` (spec
  `NOT_APPLICABLE Semantics for Boundary-Compliance Checking`, first
  scenario). Depends on: 4.1, 3.1.
- [x] 4.3 Implement `PASS` for the zero-dependency case: at least one
  applicable constraint and zero `DEPENDENCY` relationships at the
  anchor produces `PASS`, not `NOT_APPLICABLE` (spec, second scenario;
  `Boundary-Compliance Outcome Determination`, third scenario). Depends
  on: 4.2, 3.1.
- [x] 4.4 Implement `FAIL`/`PASS` for the general case: `FAIL` if the
  condition is unsatisfied for any applicable constraint; `PASS` if
  satisfied for every applicable constraint (spec `Boundary-Compliance
  Outcome Determination`, first two scenarios). Depends on: 4.3, 3.1.
- [x] 4.5 Add a comprehensive fixture-based test matrix covering all
  outcome paths: no relevant relationships (no Result); dependency
  present, no applicable constraint (`NOT_APPLICABLE`); applicable
  constraint present, no dependency (`PASS`); applicable constraint
  violated (`FAIL`); applicable constraint present with a non-violating
  dependency (`PASS`). Depends on: 4.1, 4.2, 4.3, 4.4.

## 5. `FAIL` Payload: `BoundaryComplianceDiagnostic`

- [x] 5.1 Define `BoundaryComplianceDiagnostic` in `aip.rules.
  boundarycompliance`: a `FindingMetadata`-implementing record with
  fixed `category()`/`severity()` and per-instance, dynamically
  computed `description()`/`impact()` identifying the violating
  Component, dependency target, and violated boundary relationship
  (`design.md` Decisions 4, 5; spec `Boundary-Compliance Category and
  Severity`). Depends on: 4.4.
- [x] 5.2 Wire `BoundaryComplianceDiagnostic` as the `FAIL` outcome's
  payload; `PASS`/`NOT_APPLICABLE` carry a plain, non-`FindingMetadata`
  `String` explanation (`design.md` Decision 4). Depends on: 5.1.
- [x] 5.3 Add a test confirming two different Architecture Components,
  each with a `FAIL` outcome, carry the same declared Category and
  Severity, but differing (per-instance) Description content (spec
  `Boundary-Compliance Category and Severity`). Depends on: 5.2.

## 6. Architecture Compliance Agent Registration and Package Placement

- [x] 6.1 Add `ArchitectureComplianceAgent` to a new `aip.ai.
  architecturecompliance` subpackage within the already-existing
  `aip-ai` module — no new module (`design.md` Decision 1;
  `define-architecture-compliance-agent/design.md` Decisions 1, 2, 9;
  spec `Architecture Compliance Agent Registration`).
- [x] 6.2 Add a test confirming registration (via the existing
  `AgentRegistry`) requires no change to Agent Framework's own
  consumption, identity, traceability, or validation mechanisms (spec,
  scenario: Architecture Compliance Agent registers without core
  mechanism changes). Depends on: 6.1.

## 7. Architecture Compliance Agent Consumption Boundary

- [x] 7.1 Implement `invoke(Finding)` reading only `finding`'s own
  fields — `concernedElementId()`, `description()`, `category()`,
  `severity()` — with no `RuleEvaluationResultSource`, `FindingSource`,
  or other content-bearing dependency (`design.md` Decisions 5, 6;
  spec `Architecture Compliance Agent Consumption Boundary`). Depends
  on: 6.1.
- [x] 7.2 Add a test confirming the Agent reads no CSM, Analysis, or
  RuleEvaluationResult content directly — only the Finding it is given
  (spec). Depends on: 7.1.

## 8. Recommendation Content Shape

- [x] 8.1 Define `ArchitectureComplianceRecommendationContent` in
  `aip.ai.architecturecompliance`: `violatingComponentId`,
  `violationDescription` (carried from `finding.description()`
  unmodified), `remediationGuidance` (a fixed v1 template string)
  (`design.md` Decision 7; spec `Architecture Compliance Agent
  Recommendation Content`, first scenario). Depends on: 7.1.
- [x] 8.2 Implement `remediationGuidance` as general, guidance-content
  prose — never executable instructions (spec, second scenario).
  Depends on: 8.1.
- [x] 8.3 Confirm `ArchitectureComplianceRecommendationContent`'s own
  field list has no code/diff-shaped field, structurally guaranteeing
  `Architecture Compliance Agent Recommendation Excludes Code and
  Proposed Changes` (`design.md` Decision 7). Depends on: 8.1.
- [x] 8.4 Add a test confirming a constructed Recommendation's content
  contains no code or diff content (spec). Depends on: 8.3.

## 9. Boundary Provenance Propagation

- [x] 9.1 Confirm the violated `BOUNDARY_CONSTRAINT` relationship's
  declared-or-inferred provenance classification remains reachable
  through existing Finding Evidence/traceability content, with no new
  mechanism introduced (spec `Boundary Provenance Is Propagated, Not
  Differentiated`, second scenario). Depends on: 4.4, 8.1.
- [x] 9.2 Add a test confirming outcome, Category, and Severity are
  determined identically for a declared-boundary violation and an
  inferred-boundary violation (spec, first scenario). Depends on: 5.3,
  9.1.

## 10. Compliance Scope Enforcement

- [x] 10.1 Confirm the boundary-compliance Rule Type and Architecture
  Compliance Agent evaluate and produce output for Architectural
  Boundary violations only — no other concern (spec `Compliance Scope
  Limited to Boundary Violations`). Depends on: 4.4, 8.1.

## 11. No-New-Mechanism Confirmation Tests

- [x] 11.1 Add a test confirming every output this capability produces
  is an ordinary `RuleEvaluationResult`, `Finding`, or `Recommendation`,
  using Rule Framework's and Agent Framework's existing mechanisms
  unmodified (spec `Architecture Compliance Agent Introduces No New
  Artifact or Mechanism`, first scenario). Depends on: 4.4, 8.1.
- [x] 11.2 Add a test confirming this capability's `RuleEvaluationResult`s,
  Findings, and Recommendations are validated, published, and traced
  using each artifact's own already-established mechanisms, unmodified
  (spec, second scenario). Depends on: 11.1.
- [x] 11.3 Add a test confirming the boundary-compliance Rule Type and
  Architecture Compliance Agent are registered within `aip-rules` and
  `aip-ai` respectively, with no new module and no new shared read
  contract (spec, third scenario). Depends on: 1.1, 6.1.

## 12. Fixture/Test Infrastructure

- [x] 12.1 Build fixture CSM Snapshots containing Architecture
  Components with varying combinations of `DEPENDENCY` and
  `BOUNDARY_CONSTRAINT` relationships (none, dependency-only,
  boundary-only, both non-violating, both violating), including a
  `constraint-kind: must-not-depend-on` helper and a non-matching-
  constraint-kind helper, covering every outcome path in Section 4.5.
- [x] 12.2 Build fixture `BOUNDARY_CONSTRAINT` relationships covering
  both declared and inferred provenance. Depends on: 12.1.
- [x] 12.3 Build a fixture Finding representing a boundary violation
  (via `BoundaryComplianceDiagnostic`), sufficient to exercise Sections
  6–10 without depending on a full pipeline run. Depends on: 12.1,
  12.2, 5.1.

## 13. Requirement/Scenario Traceability and End-to-End Verification

- [x] 13.1 Build a traceability matrix mapping each of
  `define-architecture-compliance-agent/specs/architecture-compliance-
  agent/spec.md`'s 14 requirements and scenarios to the task(s)/
  test(s) that implement/verify it, following `implement-agent-
  framework`'s own `traceability.md` precedent. Depends on: Sections
  1–12.
- [x] 13.2 Confirm every one of this change's own `design.md`'s 7
  Decisions, and every one of `define-architecture-compliance-agent/
  design.md`'s 9 Decisions, has at least one corresponding
  implementation task and test above; record any gap found rather than
  silently leaving it uncovered. Depends on: 13.1.
- [x] 13.3 Add an end-to-end fixture-based test: construct a fixture
  CSM Snapshot with a violating dependency, evaluate the boundary-
  compliance Rule Type to produce a `FAIL` `RuleEvaluationResult`,
  construct and publish the resulting Finding, invoke the Architecture
  Compliance Agent against it, and construct, validate, and publish
  the resulting Recommendation — confirming the full path (Sections
  1–11) is consistent end to end; also cover the `NOT_APPLICABLE`/
  `PASS`/no-Result cases producing no Finding/Recommendation in the
  same run. Depends on: 4.5, 5.3, 8.4, 9.2, 10.1, 11.2, 12.3.
- [x] 13.4 Run full validation (`openspec validate architecture-
  compliance-agent --strict` once specs are synced, or the equivalent
  check available during implementation) and record the result in this
  change's verification artifact. Depends on: 13.1, 13.2.
