## Why

Analysis Framework (`define-analysis-framework`) is specified and
establishes the deterministic computation layer over the CSM, but it
deliberately excludes policy entirely (its own `CSM Snapshot as Sole
Analysis Input` requirement). Per `project.md` §11's evolution order,
Rule Framework is next: the layer that evaluates organization-defined
policy against `AnalysisView`/`AnalysisResult` content, producing the
input a future Finding Model formalizes into structured findings.
Without it, `project.md` §3.7's "Policy as Code" principle — and the
canonical example policies it names ("Handlers must not access
repositories directly," etc.) — has nowhere to live.

## What Changes

- Introduces the Rule Framework capability: a **Rule Type** contract
  (AIP-authored, versioned code, mirroring Analysis Framework's
  Analyzer contract) plus a declarative **Rule** — a configured
  instance of a Rule Type, parameterized against CSM vocabulary — that
  organizations author and version-control, per `project.md` §3.7.
- Introduces **Rule Evaluation Result**: the durable, traceable output
  of evaluating one Rule against one Rule Scope instance, carrying a
  pass/fail/not-applicable outcome — not yet a `project.md` §4.8-shaped
  Finding.
- Establishes a new module, `aip-rules` (already named in CLAUDE.md's
  module chain), depending on `aip-core` only — never on `aip-analysis`
  or `aip-analyzer` directly.
- Extends `aip-core` with a new `AnalysisResultSource` read contract
  (mirroring `CsmSnapshotSource`) and a shared, reusable Scope
  declaration shape (reusing Analysis Scope's already-specified
  vocabulary without modifying that specification).
- Clarifies, without modifying, `canonical-software-model`'s own
  Evidence→Knowledge→Analysis→Findings pipeline: this change treats
  Analysis Framework → Rule Framework → Finding Model as a refinement
  of that pipeline's single "Analysis → Findings" transition into three
  architectural layers, the same interpretive move already confirmed
  for Analysis Framework's own relationship to that pipeline.
- No implementation code in this change — specification and design
  only. Does not design Finding Model beyond the output boundary Rule
  Evaluation Result establishes for it.

## Capabilities

### New Capabilities
- `rule-framework`: the Rule Type contract, Rule configuration/scope
  model, evaluation orchestration, and the durable Rule Evaluation
  Result artifact and its identity/traceability/validation model.
  Requirements and scenarios are written during Specify, informed by
  the resolved decisions in `explore.md` and this change's `design.md`.

### Modified Capabilities
(none — `canonical-software-model`, `software-repository-understanding`,
`csm-builder`, and `analysis-framework` are fixed inputs; this change
does not alter their requirements)

## Impact

- **Affected specs**: adds `openspec/specs/rule-framework/spec.md` once
  Specify is reached. No changes to any existing specification —
  including `analysis-framework`, which remains as approved in
  `define-analysis-framework`.
- **Affected code**: none yet — specification and design only, per the
  mandatory Explore → Propose → Design → Specify → Review → Implement
  → Test → Verify → Archive workflow (`CLAUDE.md`). A future
  `implement-rule-framework` change performs implementation.
- **Dependencies**: `aip-rules` depends on `aip-core` only — the CSM
  domain model, `AnalysisView`, and the new `AnalysisResultSource`
  contract this change's `design.md` adds there. It SHALL NOT depend on
  `aip-analysis`, `aip-csm-builder`, or `aip-analyzer` directly, mirroring
  the contract-ownership pattern `define-analysis-framework` established
  for `CsmSnapshotSource`.
- **Affects future work**: establishes the Rule Evaluation Result
  contract a future Finding Model formalizes into structured findings,
  and the Rule Type/registry pattern a future `implement-rule-framework`
  change and its built-in Rule Types (e.g. a boundary-compliance Rule
  Type, satisfying `canonical-software-model`'s own `Architectural
  Boundary Representation` requirement) will build on.
