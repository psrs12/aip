## Why

The `finding-model` capability is fully specified, reviewed, and
merged (`openspec/changes/define-finding-model/` — 25 requirements,
52 scenarios, 61 tasks), but no implementation exists anywhere in this
repository. This change carries Finding Model from approved
specification into working code, the same relationship
`implement-rule-framework` had to `define-rule-framework` (completed
and merged) and `implement-analysis-framework` had to
`define-analysis-framework` (completed and merged).

## What Changes

- Implement `RuleEvaluationResultSource` in `aip-core`: Finding
  Model's own contribution to `aip-core` (`define-finding-model/
  design.md` Decision 11) — read a `RuleEvaluationResult` by identity,
  list `RuleEvaluationResult`s by (Rule identifier, source CSM
  Snapshot identity).
- Implement `Finding` **as a canonical `aip-core` type**, mirroring
  `AnalysisResult`'s and `RuleEvaluationResult`'s own placement, per
  the binding placement decision below.
- Implement `Finding`'s two distinct identity concepts — `Evaluation
  Identity` (snapshot-bound, deterministic function of the complete
  referenced `RuleEvaluationResult` identity set) and `Logical Finding
  Identity` (snapshot- and Rule-version-independent, deterministic
  function of the producing Rule's identifier and the concerned CSM
  element identity) — both in `aip-core`.
- Resolve, concretely, a gap `define-finding-model/design.md` left
  unaddressed at the conceptual level: the mechanism by which a
  Finding's Category/Severity/Description/Impact are actually obtained
  from "the producing Rule Type's static configuration" without
  `aip-findings` depending on `aip-rules`, and which
  `RuleEvaluationResult` outcomes "qualify" for Finding construction
  (see Binding Decisions, below).
- Implement Finding construction, validation (`FindingValidator`), and
  publishing (`FindingPublisher`), mirroring the validate-before-
  publish gate every prior layer already established.
- Implement `FindingStore` as a persistence abstraction in
  `aip-findings` (not promoted to `aip-core`, mirroring
  `RuleEvaluationResultStore`'s and `AnalysisResultStore`'s own
  placement).
- Implement a fixture layer for `RuleEvaluationResultSource`, test
  only — not a live adapter over a real `aip-rules` implementation.
- Confirm the extension mechanism (a new Rule Type, declaring
  Category/Severity via the mechanism this change defines, requires no
  Finding Model change).

This change does not modify the behavior specified by `finding-model`,
`rule-framework`, `analysis-framework`, `canonical-software-model`,
`csm-builder`, or `software-repository-understanding` — it implements
what is already approved, plus the concrete resolution of two
implementation-level gaps named below (neither reopens an upstream
binding decision; both fill in a mechanism the upstream design left at
the conceptual level). It does not implement Agent Framework,
Recommendation generation, or any lifecycle presentation/dashboard
mechanism.

## Binding Decisions (locked scope, carried from Explore)

1. **`Finding` placement**: `Finding` is promoted to `aip-core` as the
   canonical framework-level artifact type, mirroring `AnalysisResult`'s
   and `RuleEvaluationResult`'s already-implemented placement exactly —
   justified by the same already-named-future-consumer argument
   `define-finding-model/design.md` Decision 11 itself gives (Agent
   Framework and a future CLI/query surface are named future readers).
2. **Category/Severity/Description/Impact provenance mechanism (new
   gap, resolved here)**: `define-finding-model/design.md` Decision 5
   states these are "taken directly from static configuration declared
   by the producing Rule Type," but neither it nor `spec.md` names how
   Finding Model — which depends on `aip-core` only, never `aip-rules`
   — actually reaches that configuration, given `RuleEvaluationResult`'s
   own payload is Rule-Type-opaque. This change resolves it: a new
   `aip-core` contract, `FindingMetadata`, that a `RuleEvaluationResult`'s
   payload MAY implement to expose Category/Severity/Description/
   Impact; Finding construction reads it through that contract only,
   never by inspecting a payload's concrete Rule-Type-specific type.
   This is additive to `aip-core` and does not modify `RuleEvaluationResult`'s
   already-approved shape (its payload remains a plain, opaque
   `Object` field; `FindingMetadata` is a structural contract a payload
   value may or may not satisfy, never a required field).
3. **Outcome-qualification mechanism (new gap, resolved here)**:
   neither `define-finding-model/design.md` nor `spec.md` states which
   `RuleEvaluationResult` outcomes "qualify" for Finding construction
   (the phrase spec.md's own `Aggregation Is Not Performed in This
   Version` requirement uses, without defining it) — `explore.md`
   named this an open question for Design to confirm and it was never
   explicitly settled. This change resolves it: a `RuleEvaluationResult`
   qualifies if and only if its outcome is `FAIL` **and** its payload
   implements `FindingMetadata` (Binding Decision 2). `PASS` and
   `NOT_APPLICABLE` outcomes never qualify, regardless of payload
   shape; a `FAIL` outcome whose payload does not implement
   `FindingMetadata` produces no Finding either — mirroring the "no
   Result at all" no-error-silently-skip pattern `Kind-Based Rule
   Applicability` already established one layer down, applied here to
   Finding construction instead of Rule evaluation.
4. **Unanchored Rule Scope "concerned element" resolution (named
   limitation, not silently absorbed)**: `define-finding-model/design.md`
   Decision 3 states the concerned CSM element for an unanchored Rule
   Scope instance is "the Repository CSM element's own identity," and
   its own Cross-Capability Impacts section claims no upstream
   exposure gap exists. Implementation reveals this claim does not
   fully hold: the literal Repository CSM element identity is a
   function of its originating Repository Evidence Item's identity
   (`aip-csm-builder`'s `ElementIdentityDeriver.fromEvidenceId`),
   reachable only from `aip-csm-builder`, a module `aip-findings` has
   no dependency on and none is being introduced. This change resolves
   it pragmatically: for the unanchored case, the concerned element
   identity used in Logical Finding Identity and Location is a
   Finding-Model-local, deterministic function of the source CSM
   Snapshot's own `repositoryIdentifier` alone — stable across
   snapshots of the same repository (satisfying every actual spec
   scenario, none of which asserts equality to a real
   `aip-csm-builder`-constructed Repository element identity) but not
   guaranteed equal to that real identity. Named explicitly as a
   deliberate, boundary-respecting v1 limitation in `design.md`'s
   Risks section, not a silent deviation.
5. **No real `aip-rules` adapter**: this change does not wire a real
   connection to an `aip-rules` implementation for testing —
   `aip-findings` remains fully independent of `aip-rules` at the
   module level regardless (`define-finding-model/design.md` Decision
   11), and this change's own tests use a fixture
   `RuleEvaluationResultSource` implementation, mirroring
   `implement-rule-framework`'s own fixture-first approach one layer
   down.

## Impact

- **Affected code**: adds the `aip-findings` module; adds
  `RuleEvaluationResultSource`, `Finding`, `EvaluationIdentity`,
  `LogicalFindingIdentity`, and `FindingMetadata` to `aip-core`. No
  changes to `aip-csm-builder`, `aip-analysis`, `aip-rules`, or any
  existing `aip-core` type.
- **Affected specs**: none — `skip_specs: true`, since this change
  implements `finding-model`'s already-approved requirements without
  altering them; Binding Decisions 2–4 fill gaps the upstream design
  left unaddressed rather than altering anything it did settle.
- **Dependencies**: `aip-findings` depends on `aip-core` only, per
  `define-finding-model/design.md` Decisions 10, 11, enforced by a
  dependency-graph CI check mirroring `aip-csm-builder`'s,
  `aip-analysis`'s, and `aip-rules`'s own, reusing the generic
  `scripts/check-no-module-reference.sh` built during
  `implement-rule-framework`.
- **Prerequisite**: `implement-rule-framework`'s own 66 tasks are
  already complete and merged — `RuleEvaluationResult`,
  `RuleEvaluationResultId`, `RuleEvaluationOutcome`, and `CsmScope`/
  `CsmScopeInstance` already exist and compile in `aip-core`. No
  blocking prerequisite gate is needed this time (unlike
  `implement-rule-framework`'s own gate on `implement-analysis-
  framework`, which was still in progress at that change's own start).
- **Affects future work**: unblocks Agent Framework's own eventual
  consumption of Findings (Logical Finding Identity, Evaluation
  Identity, and the deferred `Recommendation`/`Remediation
  availability` fields it will be the first capability to populate).
