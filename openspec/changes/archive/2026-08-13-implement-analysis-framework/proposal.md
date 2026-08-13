## Why

The `analysis-framework` capability is fully specified, reviewed, and
merged (`openspec/changes/define-analysis-framework/` — 18
requirements, 39 scenarios, `tasks.md` with 14 sections and 47 tasks),
but no implementation exists anywhere in this repository. Per
`project.md`'s evolution order and `CLAUDE.md`'s mandatory workflow,
Explore → Propose → Design → Specify → Review are complete for
Analysis Framework; nothing has entered Implement. This change carries
Analysis Framework from approved specification into working code, so
CSM Snapshots can, for the first time, actually be analyzed rather
than only described — and so `implement-rule-framework` (sequenced
after this change, per explicit direction during its own Explore) has
a real `CsmSnapshotSource`, `AnalysisView`, and `AnalysisResult` to
build against.

## What Changes

- Implement the `CsmSnapshotSource` contract in `aip-core`: CSM
  elements, CSM relationships, and a stable snapshot identity.
- Implement `AnalysisView` in `aip-core`: a per-Subject
  effective-knowledge-plus-conflict-status projection built from a
  `CsmSnapshotSource`, using the existing `SubjectConflictMarker`.
- Implement the Analyzer contract and registry, Analysis Scope
  declaration (kind-based and native-attribute applicability,
  anchored/unanchored scope-instance enumeration), and the Analysis
  Orchestrator (sequential v1 dispatch, no concurrency infrastructure).
- Implement `AnalysisResult` **as a canonical `aip-core` type** — its
  deterministic identity, explicit traceability, and opaque,
  Analyzer-defined payload — per the binding placement decision below.
- Implement `AnalysisResultStore` as a persistence abstraction with
  concrete technology deferred, and the
  `AnalysisResultValidator`/`AnalysisResultPublisher` validate-before-
  publish gate.
- Implement a fixture layer for `CsmSnapshotSource`/CSM content,
  test-only, analogous to `aip-csm-builder`'s own
  `RepositoryEvidenceModelBuilder` — **not** a live adapter over
  `aip-csm-builder`'s real `Snapshot`, per the binding scope decision
  below.
- Implement the determinism, single-repository-scope, and
  CSM-Snapshot-as-sole-input boundary enforcement the archived spec
  requires, and confirm the extension mechanism (a new Analyzer
  requires no core mechanism change).

This change does not modify the behavior specified by
`analysis-framework`, `canonical-software-model`, `csm-builder`, or
`software-repository-understanding` — it implements what is already
approved. It does not implement Rule Framework, Finding Model, or
Agent Framework. It does not wire a real connection to `aip-csm-builder`'s
actual output (see Binding Decisions, below).

## Binding Decisions (locked scope, carried from Explore)

Two questions `explore.md` raised were resolved explicitly before
Design, and are binding on this change:

1. **`AnalysisResult` placement**: `AnalysisResult` is promoted to
   `aip-core` as the canonical framework-level artifact type — one
   logical artifact, not split into separate identity/traceability/
   payload core types. Its payload remains Analyzer-defined and opaque
   to `aip-core`. This uses `CsmElement`/`AnalysisView` as precedent
   (a type multiple modules produce/consume, general enough to live in
   `aip-core` from the start) rather than `CsmSnapshotSource`'s own
   decomposable shape. Every other established property is preserved
   unchanged: Analyzers produce `AnalysisResult`; identity stays
   deterministic; traceability stays explicit; the payload stays
   opaque; `AnalysisResultStore` stays an abstraction with concrete
   persistence deferred.
2. **No real CSM Builder adapter**: this change does not add a real
   `aip-csm-builder` → `CsmSnapshotSource` adapter. `aip-analysis`
   remains fully independent of `aip-csm-builder`. Fixture-based
   `CsmSnapshotSource` implementations are sufficient for this
   change's own tests. The real adapter/integration is recorded as a
   follow-up concern — if and when it is built, it belongs on the
   producer side (`aip-csm-builder`) or through an equivalent
   integration mechanism, never by reversing `aip-analysis`'s own
   dependency direction.

Neither decision reopens any of `define-analysis-framework`'s seven
binding design decisions — both are implementation-scope resolutions
of questions that document left open by construction (module
placement for a type it never assigned one to; adapter wiring, which
no `define-analysis-framework` artifact addresses at all).

## Impact

- **Affected code**: adds the `aip-analysis` module; adds
  `CsmSnapshotSource`, `AnalysisView`, and `AnalysisResult` to
  `aip-core`. No changes to `aip-csm-builder` or any existing
  `aip-core` type.
- **Affected specs**: none — `skip_specs: true`, since this change
  implements `analysis-framework`'s already-approved requirements
  without altering them.
- **Dependencies**: `aip-analysis` depends on `aip-core` only, per
  `design.md` Decision 1, enforced by a dependency-graph CI check
  mirroring `aip-csm-builder`'s own.
- **Affects future work**: unblocks `implement-rule-framework`
  (sequenced immediately after this change) and establishes the
  `aip-core`-type-placement precedent (`AnalysisResult`, and by
  extension `RuleEvaluationResult`, `Finding`, `Recommendation` at
  their own later `implement-*` changes) this proposal's Binding
  Decision 1 sets.
