## Context

`design.md` and `specs/architecture-compliance-agent/spec.md` under
`openspec/changes/define-architecture-compliance-agent/` are the
approved source of truth for *what* this capability does; this
document covers *how* it is implemented, the same relationship
`implement-agent-framework/design.md` has to `define-agent-framework`.
Three points raised in `proposal.md`'s own Binding Decisions are
resolved concretely here:

1. **Constraint-kind representation**: a `NativeAttributes` key/value
   convention on `BOUNDARY_CONSTRAINT` relationships.
2. **Violation identification without a new Agent input**: dynamic,
   per-instance `Description`/`Impact` content on the
   `RuleEvaluationResult` payload, surfaced onto `Finding` unmodified
   by Finding Model's own existing mechanism.
3. **Deterministic, template-based Agent realization**: no real model
   call in v1.

Binding external facts this design does not reopen:
- `aip-core` already hosts (per implementation, not just plan)
  `ArchitectureComponentElement`, `CsmRelationshipType.BOUNDARY_CONSTRAINT`,
  `CsmRelationshipType.DEPENDENCY`, `NativeAttributes`,
  `RuleEvaluationOutcome`, `Finding`, `FindingMetadata`, `Confidence`.
- `aip-rules` already hosts a full, working `RuleType`/`RuleTypeRegistry`/
  `RuleEvaluationOrchestrator` implementation (`implement-rule-framework`).
- `aip-findings` already hosts a full, working `FindingConstructor`/
  `FindingValidator`/`FindingPublisher` implementation
  (`implement-finding-model`).
- `aip-ai` already hosts a full, working `Agent`/`AgentRegistry`/
  `RecommendationConstructor`/`RecommendationValidator`/
  `RecommendationPublisher` implementation (`implement-agent-framework`).
- `define-architecture-compliance-agent`'s nine binding decisions (Rule
  Type ownership; no new artifact type/module; Rule Scope/condition/
  applicability semantics; fixed Category/Severity; Finding-only Agent
  consumption; Recommendation content minimums; provenance propagation
  without differentiation; narrow compliance scope; no new identity/
  traceability/validation mechanism) are unmodified.

## Goals / Non-Goals

**Goals:**
- Give the boundary-compliance Rule Type and Architecture Compliance
  Agent concrete Java shapes and package placement.
- Resolve the constraint-kind representation and violation-
  identification gaps named in Context, concretely and testably.
- Confirm the Agent's v1 realization is deterministic and
  template-based, with no real model call.
- Establish test organization, mirroring `implement-agent-framework`.

**Non-Goals:**
- Reopening any `define-architecture-compliance-agent`,
  `define-agent-framework`, `define-finding-model`,
  `define-rule-framework`, or `define-analysis-framework` decision.
- A real LLM/model-provider adapter.
- Any other `project.md` §5 agent.
- Checking "must only communicate via" or any other constraint shape —
  explicitly out of scope upstream (Decision 3's own Non-Goal),
  unchanged here.

## Decisions

### 1. Package placement: new subpackages, no new module

**Problem:** Where, concretely, do these classes live within the
already-existing `aip-rules` and `aip-ai` modules?

**Decision:** New subpackages — `aip.rules.boundarycompliance` and
`aip.ai.architecturecompliance` — rather than the existing flat
`aip.rules`/`aip.ai` packages. Both modules have been flat
single-package until now (their own generic framework classes only);
this is the first concrete Rule Type/Agent pair, and keeping it in a
distinct subpackage makes "generic mechanism" versus "concrete
configuration" visually and structurally separate within each module,
without requiring a new module (`define-architecture-compliance-agent/
design.md` Decision 2, unmodified) or a new dependency-graph CI check
(both subpackages remain inside their existing module's already-
guarded boundary).

**Alternatives considered:**
- **Add these classes directly to the existing flat `aip.rules`/
  `aip.ai` packages.** Rejected — as `project.md` §5's remaining
  agents are eventually built, a flat package would mix generic
  framework code with an unbounded number of concrete Rule Type/Agent
  implementations; establishing the subpackage-per-concrete-capability
  convention now, on the first one, costs nothing and avoids a larger
  reorganization later.

### 2. Constraint-kind representation: a `NativeAttributes` convention

**Problem:** Concretely, how does a `BOUNDARY_CONSTRAINT` relationship
express that it is a "must not depend on" constraint, given
`CsmRelationship` has no dedicated constraint-kind field the way it
does for `DEPENDENCY`'s own `dependencyKind`?

**Decision:** A `NativeAttributes` key/value pair, per CSM's own
`CSM Conceptual Vocabulary` escape hatch ("the construct SHALL be
preserved as an opaque native-evidence attribute on the nearest
matching CSM element, and SHALL NOT cause a new entity kind or
relationship type to be introduced"):

```
key:   "constraint-kind"
value: "must-not-depend-on"   (the only v1-recognized value)
```

A small, `aip-rules`-local constant holder,
`BoundaryConstraintKind`, defines this key and the recognized value —
not a new `aip-core` type, since this is Rule-Type-specific
*interpretation* of already-general-purpose `NativeAttributes`
content, exactly the same relationship every prior native-attribute-
reading Rule Type/Analyzer has to the attribute keys it reads (per
`Native-Attribute Rule Applicability Refinement`'s own established
precedent — though here the attribute is read as part of condition
logic, not Scope-level applicability refinement, since Decision 3's
own text is explicit that "no native-attribute predicate refinement is
used" for this Rule Type's *Scope declaration*). A `BOUNDARY_CONSTRAINT`
relationship missing this attribute, or carrying any other value, is
never treated as an applicable "must not depend on" constraint — this
directly realizes `Must-Only-Communicate-Via Constraints Are Out of
Scope`.

**Alternatives considered:**
- **Add a dedicated `constraintKind` field to `CsmRelationship`
  itself**, mirroring `dependencyKind`. Rejected — this would modify
  an already-approved, already-implemented `aip-core` type
  (`implement-rule-framework`'s own binding shape), well beyond this
  change's scope; `NativeAttributes` is CSM's own designed mechanism
  for exactly this situation.
- **Infer "must not depend on" from relationship direction/target
  alone, with no explicit marker at all.** Rejected — `Must-Only-
  Communicate-Via Constraints Are Out of Scope` requires
  distinguishing constraint *shapes*, which direction/target content
  alone cannot express; an explicit marker is required.

### 3. Boundary-compliance Rule Type: concrete shape

**Problem:** Concrete Java shape for the Rule Type Decision 3
(`define-architecture-compliance-agent/design.md`) specifies?

**Decision:**

```java
public final class BoundaryComplianceRuleType implements RuleType {
  public static final String IDENTIFIER = "rule-type.architecture-compliance.boundary-compliance";
  identifier() -> IDENTIFIER
  version() -> 1
  requiredAnalyzerIdentifiers() -> Set.of()
  scope() -> CsmScope.ofRelationshipTypes(Set.of(DEPENDENCY, BOUNDARY_CONSTRAINT))
               .anchoredAt(CsmEntityKind.ARCHITECTURE_COMPONENT)
  evaluate(Rule, AnalysisView, CsmScopeInstance, Map<String, Set<AnalysisResult>>) -> RuleTypeEvaluation
}
```

`evaluate`'s own logic: resolve the anchor element; read every
`BOUNDARY_CONSTRAINT` relationship sourced at the anchor whose
`NativeAttributes` carries `constraint-kind: must-not-depend-on`
(Decision 2) — the *applicable constraints*; if empty, return
`RuleTypeEvaluation.notApplicable(...)` (spec `NOT_APPLICABLE Semantics
for Boundary-Compliance Checking`); otherwise, for each applicable
constraint, check whether a `DEPENDENCY` relationship sourced at the
anchor targets the same `CsmElementId` — any match is a violation.
Zero violations (including the case of zero `DEPENDENCY` relationships
at the anchor entirely) returns `RuleTypeEvaluation.pass(...)`; one or
more violations returns `RuleTypeEvaluation.fail(payload)`, where
`payload` is a `BoundaryComplianceDiagnostic` (Decision 5, below).
Kind-based applicability itself (no `BOUNDARY_CONSTRAINT` or
`DEPENDENCY` relationship present at all -> no invocation, no Result)
is enforced by `CsmScopeEvaluator`'s own already-implemented,
unmodified mechanism — this Rule Type's own `evaluate` is never even
called in that case.

**Alternatives considered:** none beyond the shape
`define-architecture-compliance-agent/design.md` Decision 3 already
fixes precisely; this decision only supplies the Java realization.

### 4. `PASS`/`NOT_APPLICABLE`/`FAIL` payload shape

**Problem:** What payload does each outcome carry?

**Decision:**
- `NOT_APPLICABLE`: a fixed `BoundaryComplianceDiagnostic` is **not**
  constructed — `NOT_APPLICABLE` never qualifies for Finding
  construction anyway (`implement-finding-model/design.md` Decision 4:
  only `FAIL` + a `FindingMetadata`-conforming payload qualifies), so
  its payload is a plain `String` explanation, opaque and never
  interpreted further.
- `PASS`: likewise a plain `String` explanation — never becomes a
  Finding, per the same established outcome-qualification rule.
- `FAIL`: a `BoundaryComplianceDiagnostic` record implementing {@link
  aip.core.csm.FindingMetadata} (Decision 5, below) — the only outcome
  whose payload must satisfy `FindingMetadata`, since it is the only
  outcome this Rule Type ever produces that is meant to become a
  Finding.

**Alternatives considered:** none — this is the direct, unmodified
application of `implement-finding-model/design.md` Decision 4's own
outcome-qualification rule to this Rule Type's own three outcomes.

### 5. `BoundaryComplianceDiagnostic`: dynamic per-instance Category/Severity/Description/Impact

**Problem:** Concrete shape resolving Binding Decision 3
(`proposal.md`) — how does `FAIL`'s payload carry both the fixed
Category/Severity `Boundary-Compliance Category and Severity` requires
*and* the per-instance violation identification the Agent (Finding-
only) ultimately needs?

**Decision:**

```java
public record BoundaryComplianceDiagnostic(
    CsmElementId violatingComponentId,
    CsmElementId dependencyTargetId,
    CsmElementId violatedBoundaryRelationshipId) implements FindingMetadata {

  private static final String CATEGORY = "architecture-boundary-violation";
  private static final String SEVERITY = "high";

  category() -> CATEGORY;   // fixed, per spec's own requirement
  severity() -> SEVERITY;   // fixed, per spec's own requirement
  description() -> "Architecture Component " + violatingComponentId
      + " depends on " + dependencyTargetId
      + ", violating boundary constraint " + violatedBoundaryRelationshipId + ".";
  impact() -> "This dependency crosses a declared or inferred architectural"
      + " boundary and may introduce unwanted coupling.";
}
```

`category()`/`severity()` are `static final String` constants — fixed
uniformly across every instance, satisfying `Boundary-Compliance
Category and Severity` exactly. `description()` is computed **per
instance** from the record's own three `CsmElementId` fields — this is
the concrete mechanism resolving Binding Decision 3: since Finding
Model copies `RuleEvaluationResult`'s payload `description()` value
onto `Finding.description()` unmodified (`implement-finding-model/
design.md` Decision 3, `FindingConstructor`'s own already-implemented,
unmodified logic), the violating dependency target and violated
boundary relationship identities become directly readable from
`Finding.description()` with zero new mechanism anywhere in Finding
Model or Agent Framework. `impact()` is fixed static text in v1 (no
per-instance variation needed to satisfy the spec's own minimums).

**Alternatives considered:**
- **Fixed, static `description()`/`impact()` text with no per-instance
  detail**, relying on `Finding.concernedElementId()` (the violating
  Component) alone. Rejected — this alone cannot satisfy `Architecture
  Compliance Agent Recommendation Content`'s requirement that the
  Recommendation identify "the dependency target involved in the
  violation" and "the violated boundary relationship," since neither
  is otherwise reachable from a bare Finding without this dynamic
  content.
- **Give `aip-ai` a dependency on `RuleEvaluationResultSource`** so the
  Agent could resolve the underlying `RuleEvaluationResult` and read
  its payload's structured fields directly. Rejected — directly
  reopens `define-agent-framework` Decision 4's Finding-only
  consumption boundary, which `proposal.md`'s Binding Decision 3
  explicitly avoids reopening.

### 6. Architecture Compliance Agent: concrete shape, deterministic realization

**Problem:** Concrete Java shape, and how does `invoke` construct a
Recommendation without a real model call (Binding Decision 4,
`proposal.md`)?

**Decision:**

```java
public final class ArchitectureComplianceAgent implements Agent {
  public static final String IDENTIFIER = "agent.architecture-compliance";
  identifier() -> IDENTIFIER
  version() -> 1
  invoke(Finding finding) -> AgentInvocationResult
}
```

`invoke`'s logic is a pure, deterministic function of `finding`'s own
already-readable fields — no model call: constructs
`ArchitectureComplianceRecommendationContent` (Decision 7, below) from
`finding.concernedElementId()` (the violating Component),
`finding.description()` (the dependency-target/violated-boundary
identification text, per Decision 5), and a fixed remediation-guidance
template string; returns a fixed `confidence` (`0.9` — high but not
maximal, reflecting that this Agent's guidance is itself template-
generated, not model-elicited, while still being derived directly from
a deterministic Rule violation with no uncertainty about the
underlying fact); and a `GenerationProvenance` naming
`modelProviderIdentifier = "deterministic-template"`,
`modelVersion = "v1"` — honestly documenting that no external model
was called, rather than fabricating a provider name that implies one
was.

**Alternatives considered:**
- **Leave `invoke` unimplemented / throw `UnsupportedOperationException`**,
  treating "wire a real LLM" as a hard prerequisite. Rejected —
  `Agent`'s own contract does not require a model call to exist;
  a deterministic, template-based realization is honest, fully
  functional, and satisfies every `spec.md` requirement this
  capability names, including the fixture end-to-end test's own need
  for a working Agent to exercise.

### 7. `ArchitectureComplianceRecommendationContent`: concrete Recommendation content shape

**Problem:** Concrete shape for the opaque Recommendation content
`Architecture Compliance Agent Recommendation Content` requires?

**Decision:**

```java
public record ArchitectureComplianceRecommendationContent(
    CsmElementId violatingComponentId,
    String violationDescription,
    String remediationGuidance) { }
```

`violationDescription` is `finding.description()`'s own value, carried
through unmodified (already contains the dependency-target/violated-
boundary identification, per Decision 5). `remediationGuidance` is a
fixed v1 template string (e.g. "Consider removing this dependency, or
introduce an approved integration boundary between the two
components.") — a general remediation approach expressed as guidance
content, per spec, never generated code, a diff, or a Proposed Change.

**Alternatives considered:** none beyond the shape spec's own
requirement text already fixes; this decision only supplies the Java
realization.

## Risks / Trade-offs

- [The `constraint-kind` `NativeAttributes` convention (Decision 2) has
  no CSM Builder-side producer yet — no Mapper in `aip-csm-builder`
  populates it from real evidence, since Architecture Component/
  Boundary construction is itself explicitly excluded from CSM Builder
  (`canonical-software-model`'s `Exclusion of Architectural Inference
  and Declared-Knowledge Construction`)] → Expected and accepted: this
  Rule Type is designed to read Architecture Component/Boundary
  content from a future declared-knowledge or inference capability
  (per `ArchitectureComponentElement`'s own javadoc), not from CSM
  Builder; this change's own fixture tests construct this content
  directly, the same way `aip-rules`'s and `aip-findings`'s own
  fixture layers always have.
- [Dynamic per-instance `Description`/`Impact` content (Decision 5)
  departs from `define-finding-model/design.md`'s own illustrative
  framing of Description as "static template text, parameterizable by
  the Rule's own configuration"] → Not a violation: `architecture-
  compliance-agent/spec.md`'s own `Boundary-Compliance Category and
  Severity` requirement fixes only Category and Severity as uniform
  across instances, deliberately not Description/Impact: this was
  checked explicitly against the literal requirement text, not assumed.
- [The Architecture Compliance Agent's deterministic, template-based
  realization (Decision 6) means its v1 Recommendations are
  formulaic, not genuinely AI-reasoned] → Accepted and named
  explicitly, consistent with `implement-agent-framework`'s own
  no-real-LLM-adapter scope; a future change wiring a real model
  provider would replace this Agent's internal `invoke` logic without
  needing to change its registration, contract, or any consumer of its
  Recommendations.
