## Why

The `rule-framework` capability is fully specified, reviewed, and
merged (`openspec/changes/define-rule-framework/` — 19 requirements,
42 scenarios, 59 tasks), but no implementation exists anywhere in this
repository. This change carries Rule Framework from approved
specification into working code, the same relationship
`implement-csm-builder` had to `define-csm-builder` and
`implement-analysis-framework` (sequenced immediately before this
change, per explicit direction, and itself fully planned though not
yet implemented) has to `define-analysis-framework`.

## What Changes

- Implement `AnalysisResultSource` in `aip-core`: Rule Framework's own
  contribution to `aip-core` (`define-rule-framework/design.md`
  Decision 7) — read an `AnalysisResult` by identity, list
  `AnalysisResult`s by (Analyzer identifier, source CSM Snapshot
  identity).
- Implement `RuleEvaluationResult` **as a canonical `aip-core` type**,
  mirroring `AnalysisResult`'s own placement exactly, per the binding
  placement decision below.
- Implement the Rule Type contract, Rule declarative configuration,
  Rule Scope (`CsmScope`, reused verbatim — no new type), kind-based
  and native-attribute applicability, and the binding distinction
  between "not applicable, no Result" and "applicable and evaluated,
  `NOT_APPLICABLE` Result."
- Implement `RuleEvaluationResult` identity (`RuleEvaluationResultId`,
  extending the established shape with the complete consumed
  `AnalysisResult` identity set), traceability, and its opaque,
  Rule-Type-defined payload.
- Implement `RuleEvaluationResultStore` as a persistence abstraction
  (staying in `aip-rules`, not promoted), and the
  `RuleEvaluationResultValidator`/`RuleEvaluationResultPublisher`
  validate-before-publish gate.
- Implement a fixture layer for `CsmSnapshotSource`/`AnalysisView`/
  `AnalysisResultSource`, test-only — **not** a live adapter over a
  real `aip-analysis` implementation, per the binding scope decision
  below.
- Implement the determinism, single-repository-scope, and
  established-contracts-only boundary enforcement the archived spec
  requires, and confirm the extension mechanism (a new Rule Type
  requires no core mechanism change).

This change does not modify the behavior specified by
`rule-framework`, `analysis-framework`, `canonical-software-model`,
`csm-builder`, or `software-repository-understanding` — it implements
what is already approved. It does not implement Finding Model or Agent
Framework. It does not implement Analysis Framework's own
`aip-analysis` orchestration engine — this change depends on that
engine's *types* existing in `aip-core`, not on its runtime behavior.

## Binding Decisions (locked scope, carried from Explore)

1. **`RuleEvaluationResult` placement**: `RuleEvaluationResult` is
   promoted to `aip-core` as the canonical framework-level artifact
   type, mirroring `AnalysisResult`'s own already-implemented-in-plan
   placement exactly — one logical artifact, non-generic, an opaque
   `Object` payload, not split into separate core types. Justified by
   the identical reasoning `implement-analysis-framework/design.md`
   Decision 1 already gave for `AnalysisResult`: `Finding Model`
   already names a future `RuleEvaluationResultSource` `aip-core`
   contract, the same already-named-future-consumer argument that
   resolved `AnalysisResult`'s own placement.
2. **No real `aip-analysis` adapter**: this change does not wire a
   real connection to an `aip-analysis` implementation — none exists
   yet (`implement-analysis-framework` is fully planned, zero tasks
   executed). `aip-rules` remains fully independent of `aip-analysis`
   at the module level regardless (`define-rule-framework/design.md`
   Decision 7), and this change's own tests use fixture
   implementations of `CsmSnapshotSource`/`AnalysisView`/
   `AnalysisResultSource`, mirroring `implement-analysis-framework`'s
   own fixture-first approach to `CsmSnapshotSource` one layer down.
3. **Sequencing**: this change's Design and Tasks planning proceeds
   now, informed by `implement-analysis-framework`'s own approved
   (though unbuilt) decisions — planning does not require compiled
   code to exist, the same way every `define-*` change in this
   project designed against unimplemented sibling specifications. This
   change's own task *execution*, however, requires
   `implement-analysis-framework`'s own 52 tasks to be complete first
   — `CsmSnapshotSource`, `AnalysisView`, `CsmScope`, `AnalysisResult`,
   and `AnalysisResultId` must actually exist and compile in `aip-core`
   before this change's own `aip-core` additions
   (`AnalysisResultSource`, `RuleEvaluationResult`,
   `RuleEvaluationResultId`) can be written as real, non-fixture code.
   This is recorded explicitly in `tasks.md`'s own first section, not
   silently assumed.

## Impact

- **Affected code**: adds the `aip-rules` module; adds
  `AnalysisResultSource`, `RuleEvaluationResult`,
  `RuleEvaluationResultId`, and a Rule Evaluation Outcome type to
  `aip-core`. No changes to `aip-csm-builder`, `aip-analysis` (not yet
  implemented), or any existing `aip-core` type.
- **Affected specs**: none — `skip_specs: true`, since this change
  implements `rule-framework`'s already-approved requirements without
  altering them.
- **Dependencies**: `aip-rules` depends on `aip-core` only, per
  `define-rule-framework/design.md` Decision 7, enforced by a
  dependency-graph CI check mirroring `aip-csm-builder`'s and
  `aip-analysis`'s own.
- **Prerequisite**: `implement-analysis-framework`'s own 52 tasks must
  be complete before this change's own tasks can be executed (Binding
  Decision 3). This change's planning artifacts do not wait on that,
  per the same reasoning.
- **Affects future work**: unblocks `implement-finding-model`, and
  extends the `aip-core`-type-placement precedent
  (`RuleEvaluationResult`, following `AnalysisResult`) to `Finding` and
  `Recommendation` at their own later `implement-*` changes.
