## Why

Finding Model (`define-finding-model`) is specified and produces
Findings — durable, immutable, deterministically-identified,
consumer-facing artifacts — but deliberately leaves their
`Recommendation` and `Remediation availability` fields unpopulated,
naming Agent Framework as the future capability expected to fill that
gap (`define-finding-model/design.md` Cross-Capability Impacts). Per
`project.md` §11's evolution order, Agent Framework is next: the first
capability in this entire evolution order whose job is to introduce AI
reasoning, per `project.md` §6's own `AI Reasoning -> Recommendations`
pipeline stage. Without it, every deterministic capability built so
far (CSM Builder through Finding Model) has no path to the
explainable, AI-assisted guidance `project.md` §1's own Purpose
statement promises ("provide explainable recommendations").

## What Changes

- Introduces the **Agent Framework** capability: a generic **Agent**
  contract (AIP-authored, versioned code, mirroring Analyzer's and
  Rule Type's own shape) that specialized agents implement — not any
  specific agent (Architecture Compliance Agent and the rest of
  `project.md` §5's catalogue remain separate, later capabilities).
- Introduces **Recommendation**: a new, distinct, durable artifact
  that references a Finding by identity without mutating it, carrying
  AI-generated guidance — the first artifact in this system whose
  *content* is not guaranteed reproducible, while its own identity and
  provenance remain deterministic.
- Establishes a new module, `aip-ai` (already named in `CLAUDE.md`'s
  module chain), as the first AI-bearing module in this system —
  isolating AI-specific mechanics (prompts, model/provider details,
  generation configuration) away from every deterministic capability
  built so far.
- Resolves the central new tension this capability introduces:
  reconciling AI-generated content's inherent non-reproducibility with
  this project's established deterministic-identity discipline,
  without weakening CSM, `AnalysisResult`, `RuleEvaluationResult`, or
  Finding's own existing determinism/immutability guarantees.
- Scopes this capability's output boundary to Recommendation only —
  Proposed Change, code generation, remediation execution, deployment,
  verification, and human-approval workflow are explicitly out of
  scope, reserved for a capability `project.md` does not yet name.
- Clarifies, without modifying, `canonical-software-model`'s own
  Recommendations text ("AI- or rule-generated guidance attached to a
  Finding") and `project.md` §6's `AI Reasoning -> Recommendations`
  pipeline stage: this change treats Agent Framework as the layer
  where that stage is actually built, continuing the same refinement
  relationship already confirmed for Analysis Framework, Rule
  Framework, and Finding Model's own relationships to CSM's archived
  pipeline text.
- No implementation code in this change — specification and design
  only. Does not design the Architecture Compliance Agent (or any
  other specific agent), remediation execution, or human-approval
  workflow.

## Capabilities

### New Capabilities
- `agent-framework`: the Agent contract, registry, consumption
  boundary, Recommendation artifact shape, its identity/provenance
  model, and its validation/publishing gate. Requirements and
  scenarios are written during Specify, informed by `explore.md` and
  this change's `design.md`.

### Modified Capabilities
(none — `canonical-software-model`, `software-repository-understanding`,
`csm-builder`, `analysis-framework`, `rule-framework`, and
`finding-model` are fixed inputs; this change does not alter their
requirements)

## Impact

- **Affected specs**: adds `openspec/specs/agent-framework/spec.md`
  once Specify is reached. No changes to any existing specification.
- **Affected code**: none yet — specification and design only, per the
  mandatory Explore → Propose → Design → Specify → Review → Implement
  → Test → Verify → Archive workflow (`CLAUDE.md`). A future
  `implement-agent-framework` change performs implementation.
- **Dependencies**: candidate — a new `aip-ai` module depending on
  `aip-core` only, per the contract-ownership pattern every prior
  capability has established; the exact `aip-core` contract surface
  (Finding consumption, and whether any additional established
  contracts are needed) is not pre-decided until Design resolves it.
- **Affects future work**: establishes the Recommendation artifact and
  Agent contract that the Architecture Compliance Agent, and every
  other `project.md` §5 agent, will be built against; and the
  provenance/isolation model any future remediation-execution
  capability will need to trust.
