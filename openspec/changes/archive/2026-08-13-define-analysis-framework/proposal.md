## Why

The Canonical Software Model is now built and populated (CSM Builder,
`implement-csm-builder`, merged) but nothing consumes it yet. Per
`project.md` §11's evolution order, Analysis Framework is the next
capability: the deterministic computation layer that runs pluggable
Analyzers against a CSM snapshot, producing intermediate Analysis
Results that a future Rule Framework will evaluate policy against and
a future Finding Model will formalize into structured findings.
Without it, the CSM has no consumer and the rest of the evolution chain
(Rule Framework → Finding Model → Agent Framework → Architecture
Compliance Agent) has nothing to build on.

## What Changes

- Introduces the Analysis Framework capability: a registry of
  Analyzers, an orchestrator that runs them against a CSM Snapshot, and
  a durable Analysis Result artifact — architecturally mirroring, but
  not mechanically copying, CSM Builder's own Evidence-Kind
  Mapper/Registry/Orchestrator/Snapshot shape.
- Introduces the **Analysis View**: what an Analyzer actually reads —
  a CSM subject's effective knowledge plus its conflict status
  (`EFFECTIVE`/`CONFLICTED`), not raw CSM content and not a
  precedence-resolved value with conflict information discarded.
- Introduces **Analysis Scope**: every Analyzer declares, explicitly,
  what portion of the CSM it operates over — required for both
  applicability and for architecturally supporting incremental
  re-analysis later.
- Establishes a new module, `aip-analysis`, filling the gap in
  CLAUDE.md's module chain between CSM output and the future
  `aip-rules` module.
- Deterministic-only in this version: no AI/LLM surface inside an
  Analyzer. No cross-repository analysis. No implementation code in
  this change — specification and design only.

## Capabilities

### New Capabilities
- `analysis-framework`: the pluggable-Analyzer execution framework —
  Analyzer contract, registry, orchestration, Analysis View
  construction, and the durable Analysis Result artifact and its
  identity/traceability model. Requirements and scenarios are written
  during Specify, informed by the resolved decisions in `explore.md`
  and the decisions this change's `design.md` will record.

### Modified Capabilities
(none — `canonical-software-model`, `software-repository-understanding`,
and `csm-builder` are archived, fixed inputs; this change does not
alter their requirements)

## Impact

- **Affected specs**: adds `openspec/specs/analysis-framework/spec.md`
  once Specify is reached. No changes to the three existing archived
  specifications.
- **Affected code**: none yet — this change is specification and
  design only, per the mandatory Explore → Propose → Design → Specify
  → Review → Implement → Test → Verify → Archive workflow (`CLAUDE.md`).
  A future `implement-analysis-framework` change performs
  implementation, the same relationship `implement-csm-builder` had to
  `define-csm-builder`.
- **Dependencies**: `aip-analysis` will depend on `aip-core` only —
  the CSM domain model (including `Subject`/`SubjectConflictMarker`/
  `EffectiveKnowledgeStatus`) plus a new, minimal `CsmSnapshotSource`
  read contract `design.md` Decision 1 adds to `aip-core` for exposing
  persisted CSM snapshot content. `aip-analysis` SHALL NOT depend on
  `aip-csm-builder` directly (that would couple a downstream consumer
  to one specific producer's implementation module), on `aip-analyzer`
  (the future Repository Understanding implementation), or on any
  Policy/Rule Model — matching the CSM's own `CSM Relationship to the
  Repository Evidence Model` boundary.
- **Affects future work**: establishes the Analysis Result contract a
  future Rule Framework will evaluate policy against, and the
  Analyzer/registry pattern a future `implement-analysis-framework`
  change and its Analyzers will build on.
