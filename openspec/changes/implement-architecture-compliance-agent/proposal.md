## Why

The `architecture-compliance-agent` capability is fully specified,
reviewed, and merged (`openspec/changes/define-architecture-compliance-
agent/` — 14 requirements, ~25 scenarios), but no implementation
exists anywhere in this repository. This change carries it from
approved specification into working code, the same relationship
`implement-agent-framework` had to `define-agent-framework` (completed
and merged), and — unlike every prior `implement-*` change — this one
introduces **no new module and no new artifact type**: it registers a
concrete Rule Type within the already-implemented `aip-rules` module
and a concrete Agent within the already-implemented `aip-ai` module,
exactly as `define-architecture-compliance-agent/design.md` Decision 2
anticipates.

## What Changes

- Implement the boundary-compliance Rule Type (`aip-rules`, new
  `aip.rules.boundarycompliance` subpackage): a Rule Type with no
  declared Analyzer inputs, Rule Scope declaring `DEPENDENCY` and
  `BOUNDARY_CONSTRAINT` relationship kinds anchored at `ARCHITECTURE_COMPONENT`,
  evaluating "must not depend on" boundary-constraint violations with
  the full `PASS`/`FAIL`/`NOT_APPLICABLE`/no-Result applicability
  semantics `define-architecture-compliance-agent/design.md` Decision
  3 specifies precisely.
- Implement the Architecture Compliance Agent (`aip-ai`, new
  `aip.ai.architecturecompliance` subpackage): a concrete Agent
  consuming exactly one boundary-violation Finding and producing a
  Recommendation identifying the violation and a general remediation
  approach, per Decision 6.
- Resolve, concretely, two implementation-level gaps the upstream
  design left at the conceptual level (see Binding Decisions, below):
  how a `BOUNDARY_CONSTRAINT` relationship's "must not depend on"
  constraint shape is actually represented on a CSM relationship that
  has no dedicated constraint-kind field, and how the Agent — whose
  own consumption boundary is Finding-only, per `define-agent-framework`
  Decision 4 — obtains the violation's dependency-target and
  violated-boundary-relationship identification without any new
  `aip-core` contract or Agent input.
- Add fixture-based tests demonstrating the full pipeline: a fixture
  CSM Snapshot with a boundary-constraint violation, evaluated by the
  boundary-compliance Rule Type into a `FAIL` `RuleEvaluationResult`,
  constructed into a Finding, and consumed by the Architecture
  Compliance Agent into a Recommendation.

This change does not modify the behavior specified by
`architecture-compliance-agent`, `agent-framework`, `finding-model`,
`rule-framework`, `analysis-framework`, `canonical-software-model`,
`csm-builder`, or `software-repository-understanding` — it implements
what is already approved, plus the concrete resolution of two
implementation-level gaps named below (neither reopens an upstream
binding decision; both fill in a mechanism the upstream design left at
the conceptual level, the same pattern `implement-finding-model` and
`implement-rule-framework` each already followed for their own
analogous gaps). It does not implement any other `project.md` §5
agent, and it does not wire a real LLM provider — the Architecture
Compliance Agent is realized as a deterministic, template-based Agent
in v1 (see Binding Decisions).

## Binding Decisions (locked scope, carried from Explore)

1. **No new module, no new artifact type**: carried forward
   unmodified from `define-architecture-compliance-agent/design.md`
   Decisions 1, 2, 9 — this change adds classes to the already-
   existing `aip-rules` and `aip-ai` modules only.
2. **"Must not depend on" constraint-kind representation (new gap,
   resolved here)**: `CsmRelationship` has a dedicated
   `dependencyKind` field for `DEPENDENCY` relationships but no
   analogous dedicated field for a `BOUNDARY_CONSTRAINT` relationship's
   constraint shape ("must not depend on" vs. "must only communicate
   via" vs. others) — neither `define-architecture-compliance-agent/
   design.md` nor `spec.md` names how this is represented on actual
   CSM content. This change resolves it using CSM's own designed
   escape hatch (`CSM Conceptual Vocabulary`'s native-evidence-
   attribute mechanism, already used by `implement-csm-builder` for
   exactly this kind of native-construct-shape preservation): a
   `BOUNDARY_CONSTRAINT` relationship's `NativeAttributes` carries a
   `constraint-kind` key, recognized value `must-not-depend-on` in
   v1. A `BOUNDARY_CONSTRAINT` relationship without this key, or with
   any other value, is not an applicable "must not depend on"
   constraint (per `Must-Only-Communicate-Via Constraints Are Out of
   Scope`) — this is additive interpretation of already-existing CSM
   vocabulary, not a new relationship type, entity kind, or `aip-core`
   field.
3. **Violation identification without a new Agent input (new gap,
   resolved here)**: the Agent's own consumption boundary is Finding
   only (`define-agent-framework` Decision 4, unmodified). A Finding's
   `Category`/`Severity` are Rule-Type-declared and fixed uniformly
   across every instance (`architecture-compliance-agent/spec.md`'s
   own `Boundary-Compliance Category and Severity` requirement — but
   notably, that requirement fixes only Category and Severity, not
   Description or Impact). This change resolves the identification
   requirement by having the boundary-compliance Rule Type compute
   `Description`/`Impact` **dynamically per FAIL instance** —
   embedding the violating dependency target's and violated boundary
   relationship's identities as formatted text — so the Agent can
   read the concerned Architecture Component directly from Finding's
   own `Location` field and the dependency-target/violated-boundary
   identification directly from Finding's own `Description`, with no
   new `aip-core` contract, no `RuleEvaluationResultSource` dependency
   for the Agent, and no reopening of `define-agent-framework`
   Decision 4.
4. **Architecture Compliance Agent is a deterministic, template-based
   Agent in v1**: consistent with `define-architecture-compliance-
   agent/design.md`'s own Non-Goal (no concrete LLM provider chosen
   at the architecture level) and `implement-agent-framework`'s own
   precedent (fixture/stub Agents only, no real LLM adapter), this
   Agent's `invoke` implementation constructs its Recommendation
   content and remediation guidance from fixed templates plus the
   Finding's own traceable content — it never calls an external model.
   `Agent`'s own contract permits this: only the model-call step's
   *output* is non-deterministic where a model call exists; nothing in
   `Agent Contract` requires a model call to exist at all.

## Impact

- **Affected code**: adds `aip.rules.boundarycompliance` to `aip-rules`
  and `aip.ai.architecturecompliance` to `aip-ai`. No changes to
  `aip-core`, `aip-csm-builder`, `aip-analysis`, `aip-findings`, or any
  existing type in either `aip-rules` or `aip-ai`.
- **Affected specs**: none — `skip_specs: true`, since this change
  implements `architecture-compliance-agent`'s already-approved
  requirements without altering them; Binding Decisions 2–3 fill gaps
  the upstream design left unaddressed rather than altering anything
  it did settle.
- **Dependencies**: unchanged — the boundary-compliance Rule Type
  depends only on what `aip-rules` already depends on (`aip-core`);
  the Architecture Compliance Agent depends only on what `aip-ai`
  already depends on (`aip-core`). No new module, no new
  dependency-graph CI check.
- **Prerequisite**: `implement-rule-framework`, `implement-finding-model`,
  and `implement-agent-framework` are already complete and merged —
  `RuleType`, `RuleTypeRegistry`, `Agent`, `AgentRegistry`,
  `RecommendationConstructor`, `Finding`, and `FindingMetadata` all
  already exist and compile. No blocking prerequisite gate is needed.
- **Affects future work**: establishes the concrete worked-example
  precedent `project.md` §5's remaining agents (Architecture Drift,
  Dependency Boundary, Layering, Domain Boundary, API Architecture,
  Event Architecture, and the Design/Security/Risk/Compliance
  categories) can each follow for their own future `define-*`/
  `implement-*` changes.
