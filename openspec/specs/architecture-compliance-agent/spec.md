## Purpose

The Architecture Compliance Agent capability specifies the first
concrete, worked example of this project's full evaluation pipeline: a
boundary-compliance Rule Type that detects Architectural Boundary
violations, and an Architecture Compliance Agent that consumes the
resulting Findings and produces explanatory Recommendations — both
configured entirely from already-specified generic mechanisms, not new
architecture.

The archived Canonical Software Model specification names Architectural
Boundary compliance evaluation as "a function of the Policy/Rule
Model," explicitly deferred from the CSM itself. This capability,
together with Analysis Framework, Rule Framework, Finding Model, and
Agent Framework, fulfills that deferral concretely: CSM Snapshot
content flows through Analysis Framework and Rule Framework's already-
specified mechanisms to produce boundary-violation Findings, which
Agent Framework's already-specified mechanism carries to a concrete
Recommendation. This capability introduces no new pipeline layer and
no new artifact type — it is a configuration of the Rule Type and
Agent extension points those capabilities already established.

## Requirements

### Requirement: Architecture Compliance Agent Introduces No New Artifact or Mechanism
This capability SHALL be realized entirely through Rule Framework's
Rule Type extension mechanism and Agent Framework's Agent extension
mechanism. It SHALL NOT introduce a new durable artifact type, a new
identity scheme, a new traceability mechanism, a new validation
mechanism, or a new shared read contract beyond what
RuleEvaluationResult, Finding, Recommendation, and their existing
Source contracts already establish. This capability's boundary-
compliance Rule Type and Architecture Compliance Agent SHALL be
realized without introducing a new module.

#### Scenario: No new artifact type is introduced
- **WHEN** this capability's outputs are inspected
- **THEN** every output SHALL be an ordinary RuleEvaluationResult,
  Finding, or Recommendation, produced through Rule Framework's and
  Agent Framework's existing mechanisms unchanged

#### Scenario: No new identity, traceability, or validation mechanism is introduced
- **WHEN** this capability's RuleEvaluationResults, Findings, or
  Recommendations are validated, published, or traced
- **THEN** that validation, publication, and traceability SHALL use
  the identity, traceability, and validation mechanisms
  RuleEvaluationResult, Finding, and Recommendation already define,
  unmodified

#### Scenario: No new module or shared contract is introduced
- **WHEN** the boundary-compliance Rule Type and the Architecture
  Compliance Agent are realized
- **THEN** they SHALL be registered within Rule Framework's and Agent
  Framework's existing implementing modules, respectively, and SHALL
  NOT require a new shared read contract beyond those the Rule Type
  and Agent contracts already expose

---

### Requirement: Boundary-Compliance Rule Type Registration
The boundary-compliance Rule Type SHALL be registered as an ordinary
Rule Type through Rule Framework's existing registration mechanism.
Registering it SHALL require no change to Rule Framework's own
evaluation, identity, traceability, or validation mechanisms.

#### Scenario: Boundary-compliance Rule Type registers without core mechanism changes
- **WHEN** the boundary-compliance Rule Type is registered
- **THEN** Rule Framework's evaluation, identity, traceability, and
  validation mechanisms SHALL remain unchanged, and this Rule Type
  SHALL be evaluable using them as they already exist

---

### Requirement: Boundary-Compliance Rule Scope Declaration
The boundary-compliance Rule Type's Rule Scope SHALL declare the
`dependency` and `boundary/constraint` CSM relationship kinds, with a
containment-level anchor of Architecture Component. It SHALL declare
no native-attribute predicate refinement.

#### Scenario: Rule Scope declares dependency and boundary/constraint relationship kinds
- **WHEN** the boundary-compliance Rule Type's Rule Scope is inspected
- **THEN** it SHALL identify the `dependency` and `boundary/constraint`
  CSM relationship kinds as the content it reads

#### Scenario: Rule Scope anchors on Architecture Component
- **WHEN** the boundary-compliance Rule Type is evaluated
- **THEN** it SHALL be invoked once for each Architecture Component
  present in the CSM Snapshot, each invocation covering that
  Component's own Rule Scope instance

---

### Requirement: Boundary-Compliance Rule Type Declares No Analyzer Inputs
The boundary-compliance Rule Type SHALL declare an empty set of
required Analyzer identifiers. It SHALL read `dependency` and
`boundary/constraint` relationship content directly from the Analysis
View, without requiring any Analyzer's AnalysisResult.

#### Scenario: No Analyzer input is declared
- **WHEN** the boundary-compliance Rule Type is registered
- **THEN** its declared set of required Analyzer identifiers SHALL be
  empty

#### Scenario: Relationship content is read directly from the Analysis View
- **WHEN** the boundary-compliance Rule Type is evaluated
- **THEN** it SHALL complete evaluation using only `dependency` and
  `boundary/constraint` relationship content already present in the
  Analysis View, consuming no AnalysisResult

---

### Requirement: Boundary-Compliance Condition — Must-Not-Depend-On Violations
For an Architecture Component instance, the boundary-compliance Rule
Type's condition SHALL determine, for each applicable `boundary/
constraint` relationship expressing a "must not depend on" constraint
involving that Component and a target Component or External System,
whether an observed `dependency` relationship exists from that
Component to the same target. The condition SHALL be satisfied
(non-violating) when no such observed `dependency` relationship exists
for any applicable "must not depend on" constraint, and SHALL be
unsatisfied (violating) when at least one does.

#### Scenario: No violating dependency satisfies the condition
- **WHEN** an Architecture Component has a "must not depend on"
  `boundary/constraint` relationship targeting another Component, and
  no observed `dependency` relationship exists from the Component to
  that target
- **THEN** the condition SHALL be satisfied for that constraint

#### Scenario: A violating dependency does not satisfy the condition
- **WHEN** an Architecture Component has a "must not depend on"
  `boundary/constraint` relationship targeting another Component, and
  an observed `dependency` relationship exists from the Component to
  that same target
- **THEN** the condition SHALL be unsatisfied for that constraint

---

### Requirement: Must-Only-Communicate-Via Constraints Are Out of Scope
The boundary-compliance Rule Type SHALL NOT evaluate "must only
communicate via" or any `boundary/constraint` relationship expressing
a constraint shape other than "must not depend on" in this version.

#### Scenario: Non-"must not depend on" constraints are not evaluated
- **WHEN** an Architecture Component has a `boundary/constraint`
  relationship expressing a constraint other than "must not depend on"
  (e.g. "must only communicate via")
- **THEN** the boundary-compliance Rule Type SHALL NOT evaluate that
  relationship as part of its condition

---

### Requirement: Kind-Based Applicability for Boundary-Compliance Checking
An Architecture Component instance with neither a `dependency` nor a
`boundary/constraint` relationship present SHALL NOT be evaluated by
the boundary-compliance Rule Type, and no RuleEvaluationResult of any
outcome SHALL be produced for it.

#### Scenario: Component with no relevant relationships is not evaluated
- **WHEN** an Architecture Component has no `dependency` relationship
  and no `boundary/constraint` relationship
- **THEN** the boundary-compliance Rule Type SHALL NOT evaluate that
  Component, and SHALL NOT produce a RuleEvaluationResult for it

---

### Requirement: NOT_APPLICABLE Semantics for Boundary-Compliance Checking
An Architecture Component instance that satisfies kind-based
applicability (at least one `dependency` or `boundary/constraint`
relationship present) but has **no** applicable "must not depend on"
`boundary/constraint` relationship at all — only `dependency`
relationships, with no constraint to evaluate them against — SHALL
produce a RuleEvaluationResult with outcome NOT_APPLICABLE. This is
the only case in which this Rule Type produces NOT_APPLICABLE: because
CSM relationship content is closed-world queryable (`canonical-
software-model`'s `Dependency and Structural Relationships`
requirement, scenario: Dependency direction is queryable), the absence
of a `dependency` relationship to a given target is itself a directly
determinable fact whenever at least one applicable constraint exists
to evaluate it against — it does not make PASS/FAIL undeterminable,
and SHALL NOT produce NOT_APPLICABLE.

#### Scenario: Dependencies with no boundary to check against produce NOT_APPLICABLE
- **WHEN** an Architecture Component has one or more `dependency`
  relationships but no applicable "must not depend on" `boundary/
  constraint` relationship
- **THEN** the boundary-compliance Rule Type SHALL produce a
  RuleEvaluationResult with outcome NOT_APPLICABLE for that Component

#### Scenario: A boundary with no dependencies at all is compliant, not inapplicable
- **WHEN** an Architecture Component has an applicable "must not
  depend on" `boundary/constraint` relationship but no `dependency`
  relationship at all
- **THEN** the boundary-compliance Rule Type SHALL produce a
  RuleEvaluationResult with outcome PASS for that Component, not
  NOT_APPLICABLE — the absence of any dependency is itself a
  determinable, compliant fact

---

### Requirement: Boundary-Compliance Outcome Determination
An Architecture Component instance with at least one applicable "must
not depend on" `boundary/constraint` relationship SHALL produce a
RuleEvaluationResult with outcome FAIL if the condition (see
Boundary-Compliance Condition — Must-Not-Depend-On Violations) is
unsatisfied for any applicable constraint, and outcome PASS if the
condition is satisfied for every applicable constraint — including
when the Component has no `dependency` relationship at all, which
SHALL be treated as trivially satisfying every applicable constraint.

#### Scenario: A violation produces FAIL
- **WHEN** an Architecture Component's condition is unsatisfied for at
  least one applicable "must not depend on" constraint
- **THEN** the boundary-compliance Rule Type SHALL produce a
  RuleEvaluationResult with outcome FAIL

#### Scenario: No violation produces PASS
- **WHEN** an Architecture Component's condition is satisfied for
  every applicable "must not depend on" constraint it has
- **THEN** the boundary-compliance Rule Type SHALL produce a
  RuleEvaluationResult with outcome PASS

#### Scenario: No dependencies at all still produces PASS, not NOT_APPLICABLE
- **WHEN** an Architecture Component has at least one applicable "must
  not depend on" constraint and no `dependency` relationship at all
- **THEN** the boundary-compliance Rule Type SHALL produce a
  RuleEvaluationResult with outcome PASS

---

### Requirement: Boundary-Compliance Category and Severity
The boundary-compliance Rule Type SHALL declare a single, fixed
Category and a single, fixed Severity, applied uniformly to every
RuleEvaluationResult it produces. The boundary-compliance Rule Type
SHALL NOT vary Severity based on the number of violating dependency
relationships, the identity of the violated constraint, or any other
run-time computation.

#### Scenario: Category and Severity are fixed across all instances
- **WHEN** the boundary-compliance Rule Type produces
  RuleEvaluationResults for two different Architecture Components,
  each with a FAIL outcome
- **THEN** both SHALL carry the same declared Category and the same
  declared Severity

---

### Requirement: Architecture Compliance Agent Registration
The Architecture Compliance Agent SHALL be registered as an ordinary
Agent through Agent Framework's existing registration mechanism.
Registering it SHALL require no change to Agent Framework's own
consumption, identity, traceability, or validation mechanisms.

#### Scenario: Architecture Compliance Agent registers without core mechanism changes
- **WHEN** the Architecture Compliance Agent is registered
- **THEN** Agent Framework's consumption, identity, traceability, and
  validation mechanisms SHALL remain unchanged, and this Agent SHALL
  be evaluable using them as they already exist

---

### Requirement: Architecture Compliance Agent Consumption Boundary
The Architecture Compliance Agent SHALL consume exactly one Finding
per invocation, obtained through the Finding Source, and SHALL declare
no input beyond Finding consumption in this version.

#### Scenario: Agent consumes only a Finding
- **WHEN** the Architecture Compliance Agent is invoked
- **THEN** it SHALL obtain its input exclusively through the Finding
  Source, and SHALL NOT read any other CSM, Analysis, or
  RuleEvaluationResult content directly

---

### Requirement: Architecture Compliance Agent Recommendation Content
A Recommendation produced by the Architecture Compliance Agent SHALL
include, at minimum: identification of the concerned Architecture
Component and the dependency target involved in the violation,
identification of the violated boundary relationship, and a general
remediation approach expressed as guidance content rather than
executable instructions.

#### Scenario: Recommendation identifies the violation
- **WHEN** the Architecture Compliance Agent produces a Recommendation
  for a boundary-violation Finding
- **THEN** that Recommendation's content SHALL identify the concerned
  Architecture Component, the dependency target, and the violated
  boundary relationship

#### Scenario: Recommendation includes remediation guidance
- **WHEN** the Architecture Compliance Agent produces a Recommendation
- **THEN** that Recommendation's content SHALL include a general
  remediation approach expressed as guidance content

---

### Requirement: Architecture Compliance Agent Recommendation Excludes Code and Proposed Changes
A Recommendation produced by the Architecture Compliance Agent SHALL
NOT include generated code, a diff, or a Proposed Change of any kind.

#### Scenario: No code or diff is produced
- **WHEN** the Architecture Compliance Agent produces a Recommendation
- **THEN** its content SHALL NOT include generated code, a diff, or any
  form of Proposed Change artifact

---

### Requirement: Boundary Provenance Is Propagated, Not Differentiated
The boundary-compliance Rule Type and the Architecture Compliance
Agent SHALL NOT vary outcome, Category, or Severity based on whether
the violated boundary relationship's provenance is declared or
inferred. The violated boundary relationship's provenance
classification SHALL remain reachable through existing Finding
Evidence and traceability content, unchanged.

#### Scenario: Outcome and Severity are unaffected by boundary provenance
- **WHEN** a boundary violation is evaluated for a declared boundary
  relationship and, separately, for an inferred boundary relationship
- **THEN** the resulting outcome, Category, and Severity SHALL be
  determined identically in both cases

#### Scenario: Boundary provenance remains traceable
- **WHEN** a Finding or Recommendation concerning a boundary violation
  is inspected
- **THEN** it SHALL be possible to determine whether the violated
  boundary relationship's provenance was declared or inferred, through
  existing Finding Evidence and traceability content

---

### Requirement: Compliance Scope Limited to Boundary Violations
This capability's Rule Type and Agent SHALL evaluate and produce
Recommendations for Architectural Boundary violations only. This
capability SHALL NOT evaluate or produce Recommendations for any other
architecture, design, security, risk, or compliance concern.

#### Scenario: Only boundary violations are within scope
- **WHEN** the boundary-compliance Rule Type or the Architecture
  Compliance Agent processes content
- **THEN** it SHALL do so only for Architectural Boundary violations,
  and SHALL NOT evaluate or produce output for any other concern
