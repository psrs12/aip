## Context

See `proposal.md` for motivation and `explore.md` for the framing this
design treats as confirmed and binding: Architecture Compliance Agent
is `project.md` §11's final named evolution step — the first
*concrete* agent, built entirely from already-specified extension
points (Rule Framework's Rule Type mechanism, Agent Framework's Agent
mechanism), not a new pipeline layer. This document does not modify
`openspec/specs/canonical-software-model/spec.md`, `define-analysis-
framework`, `define-rule-framework`, `define-finding-model`, or
`define-agent-framework`.

Binding external facts this design does not reopen:
- `canonical-software-model`'s `Architectural Boundary Representation`
  requirement: an Architectural Boundary is a relationship between two
  or more Architecture Components (or a Component and an External
  System) describing a structural constraint ("must not depend on",
  "must only communicate via"), carrying declared-or-inferred
  provenance. Evaluating compliance is explicitly named as "a function
  of the Policy/Rule Model," not the CSM.
- `canonical-software-model`'s `CSM Conceptual Vocabulary` requirement:
  the CSM's core relationship types include, at minimum, `dependency`
  and `boundary/constraint` as two of its eight closed kinds — both
  already exist, unmodified, for this design to read.
- `canonical-software-model`'s `Dependency and Structural Relationships`
  requirement (scenario: Dependency direction is queryable): CSM
  relationship content is closed-world queryable — "the CSM SHALL
  expose a directed dependency relationship from A to B if, and only
  if, such a dependency is represented as CSM knowledge." This fact is
  not new here; it is load-bearing for Decision 3's `NOT_APPLICABLE`
  vs. `PASS` distinction below.
- `define-rule-framework`'s Rule Type Contract, Rule Scope Declaration
  (containment-level anchors are always CSM elements — established
  during `define-finding-model`'s own Review, not reopened here), Kind-
  Based Rule Applicability, and the binding distinction between "not
  applicable, no Result" and "applicable and evaluated, NOT_APPLICABLE
  Result" (`define-rule-framework/design.md` Decision 5's binding
  sub-decision) are all unmodified and directly load-bearing for
  Decision 3 below.
- `define-agent-framework`'s Agent Contract (exactly one Finding
  consumed per invocation, via `FindingSource` only) and Decision 4's
  named extension mechanism (a future agent's own Design may justify a
  scoped additional input beyond bare Finding, if concretely needed)
  are unmodified and directly load-bearing for Decision 5 below.
- `define-rule-framework/proposal.md`'s Impact section informally
  anticipated a future "boundary-compliance Rule Type" as
  `implement-rule-framework` work. This is prose in a proposal's
  Impact section, not a requirement or a claimed artifact — it does
  not bind this design, but it is the reason Decision 1 below treats
  the ownership question as real rather than invented.

## Goals / Non-Goals

**Goals:**
- Resolve who specifies the boundary-compliance Rule Type's concrete
  policy shape, without reopening or duplicating Rule Framework's own
  generic mechanism.
- Confirm or refute that this capability introduces no new artifact
  type and no new module.
- Specify the boundary-compliance Rule Type's Rule Scope, condition,
  and Category/Severity configuration precisely enough to be testable.
- Specify the Architecture Compliance Agent's consumption boundary and
  Recommendation content shape precisely enough to be testable.
- Resolve whether declared vs. inferred boundary provenance affects
  handling.
- Confirm "compliance" stays scoped to boundary violations.
- Confirm no new identity, traceability, or validation mechanism is
  needed anywhere in this capability.

**Non-Goals:**
- Writing `openspec/specs/architecture-compliance-agent/spec.md`
  (Specify phase).
- Designing any other `project.md` §5 agent (Architecture Drift,
  Dependency Boundary, Layering, Domain Boundary, API Architecture,
  Event Architecture, or any Design/Security/Risk/Compliance agent) —
  each a separate future capability.
- Specifying "must only communicate via" boundary constraints — CSM
  does not yet formalize enough structural vocabulary (e.g. what an
  "approved integration path" looks like as CSM content) to check this
  precisely without inventing native-attribute conventions no prior
  layer has established; deferred, named explicitly (Decision 3).
- Any broader-context Agent input beyond bare Finding (Decision 5) —
  a real future possibility, not built here without concrete need.
- Deciding what `project.md` capability, if any, comes after this one
  — `project.md` §11 names nothing further; out of scope.
- Modifying any archived or already-approved specification.
- Implementation code of any kind.

## Where this capability does not need new architecture

Named explicitly, mirroring the "cannot simply copy an established
pattern" callout every prior Design has used — here inverted, because
this capability's own defining trait is the opposite kind of finding:

- **No new artifact type.** Every prior capability introduced exactly
  one (CSM Snapshot, `AnalysisResult`, `RuleEvaluationResult`, Finding,
  Recommendation). This capability produces `RuleEvaluationResult`s and
  Findings (from a concrete Rule Type) and Recommendations (from a
  concrete Agent) — all through mechanisms Rule Framework and Agent
  Framework already fully specify.
- **No new identity, traceability, or validation mechanism.** A
  concrete Rule Type's `RuleEvaluationResult`s use
  `RuleEvaluationResult`'s existing identity/traceability/validation
  scheme unchanged; a concrete Agent's Recommendations use
  Recommendation's existing Artifact Identity/Generation Provenance/
  validation scheme unchanged. Nothing here is new.
- **No new `aip-core` contract.** `CsmSnapshotSource`,
  `AnalysisResultSource`, `RuleEvaluationResultSource`, and
  `FindingSource` already expose everything a boundary-compliance Rule
  Type and an Architecture Compliance Agent each need.
- **Likely no new module** (Decision 2).

What *is* genuinely new, and is this capability's actual content: a
concrete Rule Scope/condition/configuration (Decision 3, 4), a
concrete consumption/content shape for one specific Agent (Decision 5,
6), and the resolution of a real cross-capability ownership question
(Decision 1) — configuration and policy content, not mechanism.

## Decisions

### 1. Rule Type ownership: this capability specifies the boundary-compliance Rule Type's concrete policy shape

**Problem:** `define-rule-framework/proposal.md` informally named a
future "boundary-compliance Rule Type" as `implement-rule-framework`
work, written before this capability existed. Does this capability
specify that Rule Type's concrete policy shape, assume it is specified
elsewhere, or split the concern?

**Decision:** This capability **specifies the boundary-compliance Rule
Type's concrete policy shape** — its Rule Scope, condition, and
Category/Severity configuration (Decisions 3, 4) — as its own
requirement content. Rule Framework's own generic mechanism (the Rule
Type contract, registry, evaluation, identity, traceability,
validation) remains entirely unmodified and is not re-specified here;
this capability only supplies the concrete values a Rule Type author
fills in, exactly as `define-rule-framework`'s own `Extension
Mechanism for New Rule Types` requirement anticipates any Rule Type
author doing. A future `implement-rule-framework` change need only
build the generic mechanism; a future
`implement-architecture-compliance-agent` change (depending on both
`implement-rule-framework` and `implement-agent-framework`) implements
this capability's own concrete Rule Type and Agent together, since
they are one demonstrated vertical slice, not two independently
schedulable pieces.

**Alternatives considered:**
- **Assume the boundary-compliance Rule Type is specified entirely by
  a future `implement-rule-framework` change, and specify only the
  Agent half here.** Rejected — this capability's own Agent has
  nothing concrete to consume without a concrete Rule Type producing
  boundary-violation Findings; deferring the Rule Type's policy shape
  to an unscoped future change would leave this capability's own
  specification incomplete and untestable as a whole.
- **This capability also re-specifies Rule Framework's generic
  mechanism**, to be self-contained. Rejected — would duplicate and
  risk drifting from `define-rule-framework`'s own already-approved,
  binding specification; this capability supplies configuration, not a
  second copy of the mechanism it configures.

### 2. No new artifact type; likely no new module

**Problem:** Does this capability need a new durable artifact, a new
module, or purely configuration of existing extension points?

**Decision:** **No new artifact type.** This capability's outputs are
ordinary `RuleEvaluationResult`s, Findings, and Recommendations,
produced entirely through Rule Framework's and Agent Framework's
already-specified mechanisms. **No new module** — the boundary-
compliance Rule Type registers within `aip-rules`; the Architecture
Compliance Agent registers within `aip-ai`. This mirrors
`implement-csm-builder`'s own precedent: many concrete Mappers
(Repository, Project, Module, Package, Type, Method, API Contract...)
all live within one module, `aip-csm-builder`, rather than each
requiring its own. `project.md` §5 names dozens of future agents
across five categories; a one-module-per-agent pattern would not scale
the way one-module-per-*framework* did for the six framework-level
layers already built, and nothing about this capability's own content
(configuration values, not new mechanism) requires the isolation a new
module exists to provide.

**Alternatives considered:**
- **A new module** (e.g. `aip-agents-architecture`), for isolation or
  future-proofing. Rejected — no concrete need identified; every
  module this project has created so far was justified by a genuine
  new mechanism or dependency-boundary concern (CSM Builder's own
  evidence-to-CSM transformation, Rule Framework's own contract
  ownership question), neither of which applies to a Rule Type/Agent
  pair that is pure configuration of already-isolated modules.

### 3. Boundary-compliance Rule Type: Scope, condition, and applicability semantics

**Problem:** What, precisely, does this Rule Type read and check?

**Decision:** The Rule Scope declares two CSM relationship kinds —
`dependency` and `boundary/constraint`, both already part of the CSM's
core, closed relationship vocabulary — with a containment-level anchor
of **Architecture Component** (invoked once per Architecture Component
present in the CSM Snapshot, per Rule Scope Declaration's established
element-anchor discipline). No native-attribute predicate refinement
is used in v1. This Rule Type declares **no required Analyzer
identifiers** — Rule Type Contract explicitly permits this set to be
empty, and both relationship kinds this Rule Type reads are raw CSM
content directly available through the Analysis View, needing no
dedicated Analyzer.

For a given anchored Architecture Component instance, the condition
is: for each `boundary/constraint` relationship expressing a "must not
depend on" constraint involving this Component (as the constrained
party) and a target Component or External System, check whether an
observed `dependency` relationship exists from this Component to that
same target. **v1 checks only the "must not depend on" constraint
shape** — "must only communicate via" and any other natural-language
constraint variant are explicitly out of scope (Non-Goals), because
CSM does not yet formalize enough structural vocabulary to check them
precisely.

This concretely exercises Rule Framework's own established
applicability semantics for the first time against a real policy:
- **Kind-based applicability** (no Result produced at all): an
  Architecture Component instance with neither a `dependency` nor a
  `boundary/constraint` relationship present fails kind-based
  applicability entirely — this Rule Type is never invoked for it.
- **`NOT_APPLICABLE`** (a produced Result): an Architecture Component
  instance that has at least one relationship of a declared kind
  (satisfying kind-based applicability) but has **no** applicable
  "must not depend on" `boundary/constraint` relationship at all — only
  `dependency` relationships, with no constraint to check them against
  — is applicable and evaluated, but no PASS/FAIL determination can be
  made, because the content the condition depends on (a constraint to
  evaluate against) is genuinely absent — an explicit `NOT_APPLICABLE`
  Result, not silence. This is the only `NOT_APPLICABLE` case: it does
  **not** arise merely because a Component has zero `dependency`
  relationships, because CSM relationship content is closed-world
  queryable (`canonical-software-model`'s own `Dependency and
  Structural Relationships` requirement, scenario: Dependency direction
  is queryable — "the CSM SHALL expose a directed dependency
  relationship from A to B if, and only if, such a dependency is
  represented as CSM knowledge") — "no dependency to target X exists"
  is itself a directly, positively determinable fact, not absent
  content, whenever at least one applicable constraint is present to
  evaluate it against.
- **`PASS`**: the Component has at least one applicable "must not
  depend on" `boundary/constraint` relationship, and no observed
  `dependency` relationship violates it — including, correctly, the
  case where the Component has *no* `dependency` relationships at all:
  vacuous compliance is still compliance, not an absence of content.
- **`FAIL`**: at least one observed `dependency` relationship violates
  an applicable "must not depend on" `boundary/constraint` relationship.

**Alternatives considered:**
- **Anchor on the `boundary/constraint` relationship itself, rather
  than the Architecture Component.** Rejected — Rule Scope Declaration
  (mirroring Analysis Scope Declaration) anchors are containment-level,
  always CSM elements, never relationships; this was already
  established during `define-finding-model`'s own Review and is not
  reopened here.
- **Declare a required Analyzer input**, on the assumption raw
  relationship content needs pre-computation. Rejected — both
  `dependency` and `boundary/constraint` relationships are already
  directly queryable CSM content via the Analysis View; inventing an
  Analyzer dependency here would be an unforced addition with no
  concrete need.
- **Specify "must only communicate via" checking now, as a stretch
  goal.** Rejected — would require inventing native-attribute
  conventions for "approved integration path" that no prior
  specification establishes; naming this as an explicit v1
  simplification is more honest than a half-specified check.

### 4. Category and Severity: fixed, Rule-Type-declared configuration

**Problem:** What Category and Severity does this Rule Type declare?

**Decision:** This Rule Type declares a single, fixed Category (an
architecture-boundary-violation classification) and a single, fixed
Severity, applied uniformly to every `FAIL` Result it produces — no
per-instance variation, consistent with `define-finding-model`
Decision 5's established `Severity and Category Are Rule-Type-
Declared Configuration` discipline, unmodified here. The exact literal
values (e.g. the specific Severity level) are implementation
configuration, not an architectural decision this design needs to fix.

**Alternatives considered:**
- **Vary Severity by some property of the violation** (e.g. how many
  dependency relationships violate the boundary). Rejected — would
  reintroduce Finding Model's own already-rejected "Finding Model
  computes Severity from context" alternative one layer further
  upstream; Severity stays Rule-Type-declared and static, unchanged
  from established discipline.

### 5. Agent consumption boundary: Finding only, no scoped extension in v1

**Problem:** Does the Architecture Compliance Agent need content
beyond the bare Finding `FindingSource` already exposes, to produce
useful guidance?

**Decision:** **Finding only, in v1** — no scoped extension. A
boundary-violation Finding, per `define-finding-model`'s own
established traceability, already directly carries: the concerned CSM
element identity (the violating Architecture Component), source CSM
Snapshot identity, and Evidence content reaching the underlying
`RuleEvaluationResult` and, transitively, the CSM provenance chain for
both the violating dependency and the violated boundary relationship.
This is sufficient for this Agent's v1 Recommendation content
(Decision 6): explaining the violation and suggesting a general
remediation approach does not require surveying other approved
boundaries elsewhere in the repository — that would be a genuine
enhancement, not a v1 necessity. `define-agent-framework` Decision 4's
own reasoning is honored directly: the narrow, already-established
boundary is used unless a *concrete* need justifies an extension, and
this capability has not identified one strong enough to justify it now.

**Alternatives considered:**
- **Extend consumption to include other boundary relationships
  elsewhere in the same CSM Snapshot**, so the Agent can suggest
  reusing an existing approved integration pattern. Rejected for v1 —
  a real, named future enhancement (Risks/Trade-offs), but not
  necessary for this capability's v1 Recommendation content to be
  useful, and `define-agent-framework` Decision 4 explicitly reserves
  this kind of extension for a *demonstrated* need, not a speculative
  one.

### 6. Recommendation content shape

**Problem:** What does this Agent's Recommendation actually say?

**Decision:** This Agent's Recommendation content (still an opaque
payload to the generic Agent Framework, per `define-agent-framework`'s
`Recommendation Content Is Agent-Defined and Opaque to the Framework`
— this design only fixes what *this* Agent's own content contains, not
a framework-level shape) SHALL include, at minimum: identification of
the violating relationship (the concerned Architecture Component and
the dependency target), identification of the violated boundary
relationship, and a general remediation approach expressed in prose
(e.g. "remove this dependency" or "introduce an approved integration
boundary between the two"). It SHALL NOT include generated code, a
diff, or a Proposed Change of any kind — consistent with
`define-agent-framework` Decision 9's output boundary, unmodified.

**Alternatives considered:**
- **Leave Recommendation content shape entirely to the Agent's own
  discretion**, relying solely on Agent Framework's generic opaque-
  payload treatment. Rejected — this capability is meant to be a
  concrete, worked example; leaving its own content expectations
  completely unspecified would make this capability's own
  specification untestable at the one place it has something specific
  to say.

### 7. Boundary provenance (declared vs. inferred): propagated, not differentiated

**Problem:** Does a violation of a *declared* boundary get treated
differently from a violation of an *inferred* one?

**Decision:** **No difference in outcome, Category, or Severity.**
Both are evaluated identically by the Rule Type (Decision 3) and
receive the same fixed Category/Severity (Decision 4). The violated
boundary relationship's own provenance classification (declared or
inferred) is already faithfully carried through this capability's
output without any new mechanism — Finding's own Evidence content
already exposes CSM provenance records transitively
(`define-finding-model` Decision 6), so a consumer or this Agent can
already see whether the violated boundary was declared or inferred.
The Agent's Recommendation content (Decision 6) MAY reference this
provenance in its prose (e.g., noting a boundary was inferred rather
than explicitly declared) as an internal reasoning choice — this is
ordinary Agent-authored content, not a framework-level distinction
this design needs to mandate or forbid.

**Alternatives considered:**
- **Differentiate Severity by provenance** (e.g. inferred-boundary
  violations rated lower Severity, reflecting lower certainty).
  Rejected — Severity is fixed, Rule-Type-declared configuration
  (Decision 4); introducing a second axis of variation for one
  specific Rule Type would be an unforced, non-generalizable special
  case.

### 8. Compliance scope stays narrow: boundary violations only

**Problem:** Does "Architecture Compliance" mean something broader
than boundary violations?

**Decision:** **No — scoped narrowly to boundary violations**, the one
concrete policy shape `canonical-software-model` already models via
`Architectural Boundary Representation`. `project.md` §5 lists six
other, sibling Architecture Agents (Drift, Dependency Boundary,
Layering, Domain Boundary, API Architecture, Event Architecture) as
separate concerns, not sub-concerns of this one; reading "Compliance"
broadly enough to absorb any of them would blur boundaries `project.md`
itself keeps distinct.

**Alternatives considered:**
- **A broader reading covering multiple architecture policy shapes.**
  Rejected — no second concrete, ready-built policy shape exists
  anywhere in this project's approved specifications to ground a
  broader scope in; inventing one now would be speculative.

### 9. No new identity, traceability, or validation mechanism

**Problem:** Does this capability's concrete content require any
extension to `RuleEvaluationResult`'s, Finding's, or Recommendation's
own established identity/traceability/validation mechanisms?

**Decision:** **No.** A concrete `RuleEvaluationResult` for this Rule
Type uses the identity/traceability/validation scheme
`define-rule-framework` already fully specifies, unmodified. A
concrete Finding uses `define-finding-model`'s scheme, unmodified. A
concrete Recommendation uses `define-agent-framework`'s scheme,
unmodified. This capability's only genuinely new content is
configuration (Decisions 3, 4, 6) and one resolved ownership question
(Decision 1) — confirmed explicitly here rather than left to be
inferred, per "Where this capability does not need new architecture,"
above.

**Alternatives considered:**
(none — this decision exists to record a confirmation, not resolve a
genuine fork; no alternative to a "no new mechanism" finding was
seriously in tension with the evidence)

```text
  aip-core        - unchanged: CsmSnapshotSource, AnalysisResultSource,
                    RuleEvaluationResultSource, FindingSource

  aip-rules       - Rule Framework (specified, not yet implemented)
                    + this change's boundary-compliance Rule Type
                      (Decisions 3, 4) - configuration, not new
                      mechanism

  aip-ai          - Agent Framework (specified, not yet implemented)
                    + this change's Architecture Compliance Agent
                      (Decisions 5, 6) - configuration, not new
                      mechanism

  No new module, no new aip-core contract, no new artifact type
```

## Cross-Capability Impacts

- **No modification to `canonical-software-model`, `define-analysis-
  framework`, `define-rule-framework`, `define-finding-model`, or
  `define-agent-framework`.** Checked explicitly. This capability
  fulfills, rather than modifies, `define-rule-framework/proposal.md`'s
  own informal anticipation of a boundary-compliance Rule Type
  (Decision 1) — that anticipation was prose in a proposal's Impact
  section, not a claimed artifact or requirement, so fulfilling it here
  is not a reopening.
- **`define-rule-framework`'s own `Extension Mechanism for New Rule
  Types` requirement, and `define-agent-framework`'s own `Agent
  Registration and Extension Behavior` requirement, are both exercised
  concretely for the first time** by this capability — a validation of
  those requirements' own design intent, not a change to them.
- **A future `implement-architecture-compliance-agent` change** would
  depend on both `implement-rule-framework` and
  `implement-agent-framework` being complete, since it registers a
  concrete Rule Type and a concrete Agent against mechanisms neither
  of those implementation changes builds until they exist.
- **Future `project.md` §5 agents** (Architecture Drift, Dependency
  Boundary, Layering, Domain Boundary, API Architecture, Event
  Architecture, and the Design/Security/Risk/Compliance categories)
  now have a concrete precedent to follow for what "define a Rule
  Type and an Agent" looks like as a worked example, distinct from the
  frameworks' own generic contracts.

## Risks / Trade-offs

- [v1's boundary-compliance Rule Type checks only "must not depend on"
  constraints (Decision 3), not "must only communicate via" or other
  constraint shapes] → Named explicitly as a scope limitation, not a
  silently dropped case; revisit once CSM's own vocabulary (or a
  native-attribute convention) is rich enough to check the other
  shapes precisely.
- [No broader-context Agent input (Decision 5) means v1 Recommendations
  give general remediation guidance rather than pointing to a specific,
  already-used approved integration pattern elsewhere in the same
  repository] → Accepted v1 limitation; `define-agent-framework`
  Decision 4's own extension mechanism is available if this proves
  insufficient once real Recommendations are evaluated.
- [Fixed, undifferentiated Severity regardless of boundary provenance
  (Decision 7) may understate risk for a Component with many inferred-
  boundary violations versus one declared-boundary violation] →
  Accepted v1 simplification, consistent with Rule-Type-declared-
  static-Severity discipline established upstream; not unique to this
  capability.
- [This capability's "no new module" decision (Decision 2) sets a
  precedent every future `project.md` §5 agent may be expected to
  follow, even though none of them has been designed yet] → Named as
  an assumption future agents' own Design phases should confirm for
  themselves, not treated as automatically binding on capabilities
  this design does not cover.
