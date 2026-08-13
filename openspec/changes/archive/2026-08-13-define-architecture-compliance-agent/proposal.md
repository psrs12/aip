## Why

Rule Framework and Agent Framework are both specified (not yet
implemented), each as a generic, implementation-agnostic contract. Per
`project.md` §11's evolution order, Architecture Compliance Agent is
the final named step: the first *concrete* agent, demonstrating the
full pipeline — CSM Snapshot through Recommendation — for one real
policy. Without it, every framework built so far (CSM Builder through
Agent Framework) has never been exercised end to end against an actual
governance concern, and `canonical-software-model`'s own
`Architectural Boundary Representation` requirement — which explicitly
defers boundary-compliance evaluation to "a function of the Policy/Rule
Model" — has no concrete Rule Type or Agent yet built to fulfill that
deferral.

## What Changes

- Specifies a concrete **boundary-compliance Rule Type**: its Rule
  Scope (which CSM relationship kinds it reads, its containment-level
  anchor), its condition (correlating observed dependency relationships
  against declared/inferred boundary relationships), and its
  Category/Severity configuration — registered within `aip-rules`,
  using Rule Framework's already-specified mechanisms unchanged.
- Specifies a concrete **Architecture Compliance Agent**: its
  consumption of boundary-violation Findings (Findings only, via
  `FindingSource`), and the expected content shape of the
  Recommendations it produces — registered within `aip-ai`, using
  Agent Framework's already-specified mechanisms unchanged.
- Resolves a genuine cross-capability ownership question `define-rule-
  framework/proposal.md` left informally open (it anticipated a
  "boundary-compliance Rule Type" as future `implement-rule-framework`
  work, written before this capability existed): this change owns
  specifying the Rule Type's concrete policy shape; Rule Framework's
  own generic mechanism remains unmodified and unclaimed by this
  change.
- Introduces **no new durable artifact type** and, candidately, **no
  new module** — the first capability in this evolution order to be
  purely a concrete configuration of already-specified extension
  points (a Rule Type, an Agent) rather than a new pipeline layer.
- Scopes "compliance" narrowly to boundary violations — the one
  concrete policy shape `canonical-software-model` already models —
  not a broader, undefined "architecture compliance" surface.
- No implementation code in this change — specification and design
  only. Does not design any other `project.md` §5 agent (Architecture
  Drift, Dependency Boundary, Layering, Domain Boundary, API
  Architecture, Event Architecture, or any Design/Security/Risk/
  Compliance agent).

## Capabilities

### New Capabilities
- `architecture-compliance-agent`: the boundary-compliance Rule Type's
  concrete Scope/condition/configuration, and the Architecture
  Compliance Agent's concrete consumption boundary and Recommendation
  content shape. Requirements and scenarios are written during
  Specify, informed by `explore.md` and this change's `design.md`.

### Modified Capabilities
(none — `canonical-software-model`, `software-repository-understanding`,
`csm-builder`, `analysis-framework`, `rule-framework`, `finding-model`,
and `agent-framework` are fixed inputs; this change does not alter
their requirements)

## Impact

- **Affected specs**: adds
  `openspec/specs/architecture-compliance-agent/spec.md` once Specify
  is reached. No changes to any existing specification — including
  `rule-framework`, whose own `Extension Mechanism for New Rule Types`
  requirement this change exercises rather than modifies.
- **Affected code**: none yet — specification and design only, per the
  mandatory Explore → Propose → Design → Specify → Review → Implement
  → Test → Verify → Archive workflow (`CLAUDE.md`). A future
  `implement-architecture-compliance-agent` change performs
  implementation.
- **Dependencies**: candidate — this change adds code to `aip-rules`
  (a registered Rule Type) and `aip-ai` (a registered Agent), neither
  requiring a new module or a new `aip-core` contract; not pre-decided
  until Design confirms it.
- **Affects future work**: establishes the first concrete, worked
  example every other `project.md` §5 agent (and its own supporting
  Rule Type, where applicable) can follow — a template for what
  "define an agent" and "define a Rule Type" actually look like in
  practice, distinct from the frameworks' own generic contracts.
