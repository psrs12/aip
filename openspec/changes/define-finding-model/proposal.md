## Why

Rule Framework (`define-rule-framework`) is specified and produces
`RuleEvaluationResult`s — durable, deterministic evaluation-run
artifacts carrying `PASS`/`FAIL`/`NOT_APPLICABLE`, but deliberately
scoped to that alone (its own `Rule Evaluation Results Are a Distinct
Concept From CSM Knowledge, Analysis Results, and Findings`
requirement). Per `project.md` §11's evolution order, Finding Model is
next: the layer that interprets `RuleEvaluationResult`s into the
`project.md` §4.8-shaped `Finding` — the artifact a developer,
architect, or CI pipeline actually consumes. Without it, Rule
Framework's output has no consumer-facing form, and `project.md` §3.3's
Explainability principle and §3.5's remediation workflow
(`Finding -> Recommendation -> ... -> Verification`) both have no
starting artifact to build from.

## What Changes

- Introduces the **Finding** capability: a durable, traceable,
  consumer-facing artifact derived from one or more
  `RuleEvaluationResult`s, carrying the `project.md` §4.8 shape
  (`Finding ID, Rule ID, Category, Severity, Confidence, Location,
  Evidence, Description, Impact`) — scoped per Design's resolution of
  whether `Recommendation`/`Remediation availability` belong in v1.
- Resolves whether a Finding requires exactly one, or may derive from
  many, `RuleEvaluationResult`s, and whether any Finding may exist
  without one at all.
- Resolves Finding identity: whether a single snapshot-bound identity
  (mirroring `AnalysisResult`/`RuleEvaluationResult`) suffices, or
  whether cross-snapshot tracking requires a second, logical identity
  concept.
- Resolves whether Finding Model introduces lifecycle/mutable state,
  or stays within this project's established immutable-artifact
  discipline.
- Establishes module placement (candidate: a new `aip-findings`
  sibling module) and, if warranted, a new `aip-core` read contract
  for `RuleEvaluationResult` consumption (candidate:
  `RuleEvaluationResultSource`, mirroring `AnalysisResultSource`).
- Clarifies, without modifying, `canonical-software-model`'s own
  "Findings are the structured output of Analysis" text: this change
  treats Rule Framework -> Finding Model as a further refinement of
  that same pipeline transition, the same interpretive move already
  confirmed for Analysis Framework's and Rule Framework's own
  relationships to that archived text.
- No implementation code in this change — specification and design
  only. Does not design AI-assisted/inferred Findings, remediation
  workflows, or suppression workflows — out of scope per Explore,
  reserved for future capabilities.

## Capabilities

### New Capabilities
- `finding-model`: the Finding artifact shape, its relationship to
  `RuleEvaluationResult`, identity/traceability model, aggregation
  semantics (if any), and validation/publishing gate. Requirements and
  scenarios are written during Specify, informed by `explore.md` and
  this change's `design.md`.

### Modified Capabilities
(none — `canonical-software-model`, `software-repository-understanding`,
`csm-builder`, `analysis-framework`, and `rule-framework` are fixed
inputs; this change does not alter their requirements)

## Impact

- **Affected specs**: adds `openspec/specs/finding-model/spec.md` once
  Specify is reached. No changes to any existing specification.
- **Affected code**: none yet — specification and design only, per the
  mandatory Explore → Propose → Design → Specify → Review → Implement
  → Test → Verify → Archive workflow (`CLAUDE.md`). A future
  `implement-finding-model` change performs implementation.
- **Dependencies**: candidate — a new `aip-findings` module depending
  on `aip-core` only, per the contract-ownership pattern
  `define-analysis-framework` and `define-rule-framework` each
  established; not pre-decided until Design resolves module placement.
- **Affects future work**: establishes the Finding artifact a future
  Agent Framework enriches with AI-assisted Recommendations, and any
  future remediation/suppression workflow builds on.
