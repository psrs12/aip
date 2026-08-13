## Scope Notes — Explicitly Deferred (Not Tasks in This Change)

The following are deliberately out of scope for
`implement-architecture-compliance-agent` (the future change these
tasks describe the design/spec surface for) and are listed here for
traceability only — not as checkbox tasks, since nothing below is
meant to become "done":

- "Must only communicate via" and any other `boundary/constraint`
  constraint shape beyond "must not depend on" (`design.md` Decision 3,
  Non-Goals; spec `Must-Only-Communicate-Via Constraints Are Out of
  Scope`).
- Any Agent input beyond a bare Finding — e.g. surveying other approved
  boundaries elsewhere in the repository (`design.md` Decision 5).
- Generated code, diffs, or a Proposed Change of any kind (`design.md`
  Decision 6; spec `Architecture Compliance Agent Recommendation
  Excludes Code and Proposed Changes`).
- Severity differentiation by boundary provenance, or by any other
  run-time computation (`design.md` Decisions 4, 7).
- Any other `project.md` §5 agent (Architecture Drift, Dependency
  Boundary, Layering, Domain Boundary, API Architecture, Event
  Architecture, or any Design/Security/Risk/Compliance agent).
- A new module, a new `aip-core` contract, or any new identity/
  traceability/validation mechanism (`design.md` Decisions 2, 9) —
  this change configures existing extension points only.
- Any change to `canonical-software-model`, `define-analysis-framework`,
  `define-rule-framework`, `define-finding-model`, or
  `define-agent-framework` — all fixed inputs (`design.md` Context;
  Cross-Capability Impacts, which found no gap requiring one).

## 1. Boundary-Compliance Rule Type Registration

- [ ] 1.1 Register the boundary-compliance Rule Type with Rule
  Framework's existing Rule Type registry, using its established
  registration mechanism unchanged (`design.md` Decisions 1, 2, 9;
  spec `Boundary-Compliance Rule Type Registration`).
- [ ] 1.2 Add a test confirming registration requires no change to
  Rule Framework's own evaluation, identity, traceability, or
  validation mechanisms (spec `Boundary-Compliance Rule Type
  Registration`, scenario: Boundary-compliance Rule Type registers
  without core mechanism changes). Depends on: 1.1.

## 2. Boundary-Compliance Rule Scope and Analyzer-Input Declaration

- [ ] 2.1 Declare the boundary-compliance Rule Type's Rule Scope: the
  `dependency` and `boundary/constraint` CSM relationship kinds, with
  a containment-level anchor of Architecture Component, no
  native-attribute predicate (`design.md` Decision 3; spec
  `Boundary-Compliance Rule Scope Declaration`). Depends on: 1.1.
- [ ] 2.2 Declare an empty set of required Analyzer identifiers for
  this Rule Type — both relationship kinds are read directly from the
  Analysis View (`design.md` Decision 3, Alternatives Considered; spec
  `Boundary-Compliance Rule Type Declares No Analyzer Inputs`). Depends
  on: 2.1.
- [ ] 2.3 Add tests confirming: the Rule Scope identifies both
  relationship kinds; the Rule Type is invoked once per Architecture
  Component present in the CSM Snapshot; the declared Analyzer set is
  empty; evaluation completes using only Analysis View content, with
  no AnalysisResult consumed (spec `Boundary-Compliance Rule Scope
  Declaration`, both scenarios; `Boundary-Compliance Rule Type
  Declares No Analyzer Inputs`, both scenarios). Depends on: 2.1, 2.2.

## 3. Boundary-Compliance Condition Logic — Must-Not-Depend-On

- [ ] 3.1 Implement the per-constraint condition: for a "must not
  depend on" `boundary/constraint` relationship involving the anchored
  Architecture Component and a target, determine whether an observed
  `dependency` relationship exists from the Component to that same
  target (`design.md` Decision 3; spec `Boundary-Compliance Condition
  — Must-Not-Depend-On Violations`). Depends on: 2.1.
- [ ] 3.2 Add tests confirming: no observed dependency to the target
  satisfies the condition; an observed dependency to the target leaves
  the condition unsatisfied (spec `Boundary-Compliance Condition —
  Must-Not-Depend-On Violations`, both scenarios). Depends on: 3.1.
- [ ] 3.3 Enforce that only "must not depend on" `boundary/constraint`
  relationships are evaluated — any other constraint shape (e.g. "must
  only communicate via") present on the Component SHALL NOT be
  evaluated as part of this condition (`design.md` Decision 3,
  Non-Goals; spec `Must-Only-Communicate-Via Constraints Are Out of
  Scope`). Depends on: 3.1.
- [ ] 3.4 Add a test confirming a non-"must not depend on" constraint
  is present on a Component but not evaluated (spec `Must-Only-
  Communicate-Via Constraints Are Out of Scope`). Depends on: 3.3.

## 4. Boundary-Compliance Applicability, NOT_APPLICABLE, and Outcome Semantics

- [ ] 4.1 Implement kind-based applicability: an Architecture Component
  with neither a `dependency` nor a `boundary/constraint` relationship
  present is not evaluated, and no RuleEvaluationResult is produced for
  it (`design.md` Decision 3; spec `Kind-Based Applicability for
  Boundary-Compliance Checking`). Depends on: 2.1.
- [ ] 4.2 Implement NOT_APPLICABLE determination: a Component
  satisfying kind-based applicability but with `dependency`
  relationships and no applicable "must not depend on" constraint at
  all produces NOT_APPLICABLE (`design.md` Decision 3; spec
  `NOT_APPLICABLE Semantics for Boundary-Compliance Checking`, first
  scenario). Depends on: 4.1, 3.1.
- [ ] 4.3 Implement PASS determination for the zero-dependency case: a
  Component with at least one applicable "must not depend on"
  constraint and no `dependency` relationship at all produces PASS,
  not NOT_APPLICABLE — vacuous compliance is compliance, per CSM's own
  closed-world relationship queryability (`design.md` Decision 3,
  citing `canonical-software-model`'s `Dependency and Structural
  Relationships` requirement; spec `NOT_APPLICABLE Semantics for
  Boundary-Compliance Checking`, second scenario; `Boundary-Compliance
  Outcome Determination`, third scenario). Depends on: 4.1, 3.1.
- [ ] 4.4 Implement FAIL/PASS determination for the general case: FAIL
  if the condition is unsatisfied for any applicable constraint, PASS
  if satisfied for every applicable constraint the Component has
  (`design.md` Decision 3; spec `Boundary-Compliance Outcome
  Determination`, first two scenarios). Depends on: 4.3, 3.1.
- [ ] 4.5 Add a comprehensive test matrix covering all four outcome
  paths against a real fixture: no relevant relationships (no Result);
  dependency present, no applicable constraint (NOT_APPLICABLE);
  applicable constraint present, no dependency (PASS); applicable
  constraint violated by an observed dependency (FAIL); applicable
  constraint present with a non-violating dependency (PASS). Depends
  on: 4.1, 4.2, 4.3, 4.4.

## 5. Boundary-Compliance Category and Severity Configuration

- [ ] 5.1 Declare a single, fixed Category and a single, fixed
  Severity for this Rule Type, applied uniformly to every
  RuleEvaluationResult it produces, with no per-instance variation
  (`design.md` Decision 4; spec `Boundary-Compliance Category and
  Severity`). Depends on: 1.1.
- [ ] 5.2 Add a test confirming two different Architecture Components,
  each with a FAIL outcome, carry the same declared Category and
  Severity (spec `Boundary-Compliance Category and Severity`). Depends
  on: 5.1, 4.4.

## 6. Architecture Compliance Agent Registration

- [ ] 6.1 Register the Architecture Compliance Agent with Agent
  Framework's existing Agent registry, using its established
  registration mechanism unchanged — registers independently of the
  Rule Type's own implementation status, though it has nothing to
  consume until Sections 1-5 produce Findings (`design.md` Decisions
  1, 2, 9; spec `Architecture Compliance Agent Registration`). Depends
  on: none.
- [ ] 6.2 Add a test confirming registration requires no change to
  Agent Framework's own consumption, identity, traceability, or
  validation mechanisms (spec `Architecture Compliance Agent
  Registration`). Depends on: 6.1.

## 7. Architecture Compliance Agent Consumption Boundary

- [ ] 7.1 Implement the Agent to consume exactly one Finding per
  invocation, obtained through the Finding Source, declaring no input
  beyond Finding consumption (`design.md` Decision 5; spec
  `Architecture Compliance Agent Consumption Boundary`). Depends
  on: 6.1.
- [ ] 7.2 Add a test confirming the Agent reads no CSM, Analysis, or
  RuleEvaluationResult content directly — only the Finding obtained
  through the Finding Source (spec `Architecture Compliance Agent
  Consumption Boundary`). Depends on: 7.1.

## 8. Architecture Compliance Agent Recommendation Content Shape

- [ ] 8.1 Implement Recommendation content identifying the concerned
  Architecture Component, the dependency target, and the violated
  boundary relationship (`design.md` Decision 6; spec `Architecture
  Compliance Agent Recommendation Content`, first scenario). Depends
  on: 7.1.
- [ ] 8.2 Implement a general remediation approach expressed as
  guidance content within the Recommendation (`design.md` Decision 6;
  spec `Architecture Compliance Agent Recommendation Content`, second
  scenario). Depends on: 8.1.
- [ ] 8.3 Enforce that Recommendation content never includes generated
  code, a diff, or a Proposed Change of any kind (`design.md` Decision
  6, citing `define-agent-framework` Decision 9; spec `Architecture
  Compliance Agent Recommendation Excludes Code and Proposed
  Changes`). Depends on: 8.1.
- [ ] 8.4 Add a test confirming a constructed Recommendation's content
  contains no code or diff content (spec `Architecture Compliance
  Agent Recommendation Excludes Code and Proposed Changes`). Depends
  on: 8.3.

## 9. Boundary Provenance Propagation

- [ ] 9.1 Confirm the violated `boundary/constraint` relationship's
  declared-or-inferred provenance classification remains reachable
  through existing Finding Evidence and traceability content, with no
  new mechanism introduced to carry it (`design.md` Decision 7; spec
  `Boundary Provenance Is Propagated, Not Differentiated`, second
  scenario). Depends on: 4.4, 8.1.
- [ ] 9.2 Add a test confirming outcome, Category, and Severity are
  determined identically for a declared-boundary violation and an
  inferred-boundary violation (spec `Boundary Provenance Is
  Propagated, Not Differentiated`, first scenario). Depends on: 5.1,
  9.1.

## 10. Compliance Scope Enforcement

- [ ] 10.1 Confirm the boundary-compliance Rule Type and the
  Architecture Compliance Agent evaluate and produce output for
  Architectural Boundary violations only — no other architecture,
  design, security, risk, or compliance concern (`design.md` Decision
  8; spec `Compliance Scope Limited to Boundary Violations`). Depends
  on: 4.4, 8.1.

## 11. No-New-Mechanism Confirmation Tests

- [ ] 11.1 Add a test confirming every output this capability produces
  is an ordinary RuleEvaluationResult, Finding, or Recommendation,
  using Rule Framework's and Agent Framework's existing mechanisms
  unmodified (`design.md` Decisions 2, 9; spec `Architecture
  Compliance Agent Introduces No New Artifact or Mechanism`, first
  scenario). Depends on: 4.4, 8.1.
- [ ] 11.2 Add a test confirming this capability's RuleEvaluationResults,
  Findings, and Recommendations are validated, published, and traced
  using the identity, traceability, and validation mechanisms those
  artifacts already define, unmodified (spec `Architecture Compliance
  Agent Introduces No New Artifact or Mechanism`, second scenario).
  Depends on: 11.1.
- [ ] 11.3 Add a dependency-graph/registration test confirming the
  boundary-compliance Rule Type and the Architecture Compliance Agent
  are registered within Rule Framework's and Agent Framework's
  existing implementing modules, respectively, with no new module and
  no new shared read contract introduced (spec `Architecture
  Compliance Agent Introduces No New Artifact or Mechanism`, third
  scenario). Depends on: 1.1, 6.1.

## 12. Fixture/Test Infrastructure

- [ ] 12.1 Build fixture CSM Snapshots containing Architecture
  Components with varying combinations of `dependency` and
  `boundary/constraint` relationships (none, dependency-only,
  boundary-only, both non-violating, both violating), covering every
  outcome path in Section 4.5.
- [ ] 12.2 Build fixture `boundary/constraint` relationships covering
  both declared and inferred provenance, and both "must not depend on"
  and at least one other constraint shape (to exercise Section 3.3's
  exclusion). Depends on: 12.1.
- [ ] 12.3 Build a fixture Finding representing a boundary violation,
  sufficient to exercise Sections 6–10 without depending on a
  real Rule Framework implementation of Section 1–5. Depends
  on: 12.1, 12.2.

## 13. Requirement/Scenario Traceability and End-to-End Verification

- [ ] 13.1 Build a traceability matrix mapping each of `specs/
  architecture-compliance-agent/spec.md`'s 16 requirements and 26
  scenarios to the task(s) and test(s) that implement/verify it,
  following `implement-csm-builder`'s own `traceability.md` precedent.
  Depends on: Sections 1–12.
- [ ] 13.2 Confirm every one of `design.md`'s 9 Decisions has at least
  one corresponding implementation task and test above; record any gap
  found rather than silently leaving it uncovered. Depends on: 13.1.
- [ ] 13.3 Add an end-to-end test: construct a fixture CSM Snapshot
  with a violating dependency, evaluate the boundary-compliance Rule
  Type to produce a FAIL RuleEvaluationResult, construct and publish
  the resulting Finding, invoke the Architecture Compliance Agent
  against it, and construct, validate, and publish the resulting
  Recommendation — confirming the full path (Sections 1–11) is
  consistent end to end. Depends on: 4.5, 5.2, 8.4, 9.2, 10.1, 11.2,
  12.3.
- [ ] 13.4 Run full validation (`openspec validate
  architecture-compliance-agent --strict` once specs are synced, or
  the equivalent check available during implementation) and record the
  result in this change's verification artifact. Depends on: 13.1,
  13.2.
