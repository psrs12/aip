## Context

`design.md` and `spec.md` under `openspec/changes/define-rule-framework/`
are the approved source of truth for *what* Rule Framework does; this
document covers *how* it is implemented, the same relationship
`implement-analysis-framework/design.md` has to
`define-analysis-framework`. Three questions raised during this
change's own `explore.md` (in both its original and rewritten forms)
were resolved by explicit direction before this phase began, and are
binding, carried forward unchanged:

1. **`RuleEvaluationResult` placement**: promoted to `aip-core`,
   mirroring `AnalysisResult`'s own placement exactly.
2. **No real `aip-analysis` adapter**: fixtures only; a real
   integration is a follow-up concern for whenever
   `implement-analysis-framework`'s own code actually lands.
3. **Sequencing**: this change's planning proceeds now; its task
   *execution* waits on `implement-analysis-framework`'s own 52 tasks
   being complete.

Binding external facts this design does not reopen:
- `aip-core` already hosts (per implementation, not just plan) the CSM
  domain model and the Repository Evidence contract.
- `aip-csm-builder` already hosts a full, working implementation
  (unchanged by this or the preceding change).
- `implement-analysis-framework/design.md` (approved, not yet built)
  fixes the following `aip-core` shapes this change depends on
  directly: `CsmSnapshotId(repositoryIdentifier: String,
  sequenceNumber: long)`; `CsmSnapshotSource` (`elements()`,
  `relationships()`, `id(): CsmSnapshotId`); `CsmScope` (declared
  entity/relationship kinds, optional containment anchor, optional
  native-attribute predicate) — Rule Scope *is* `CsmScope`, no new
  type; `AnalysisView` (per-Subject effective-knowledge-plus-conflict
  projection, exposing its source `CsmSnapshotId`); `AnalysisResult`
  (final class, non-generic, `Object` payload, in `aip.core.csm`);
  `AnalysisResultId` (deterministic function of Analyzer id/version,
  `CsmSnapshotId`, Scope instance); `AnalysisResultStore` (stays in
  `aip-analysis`, not `aip-core`).
- `define-rule-framework`'s seven binding decisions (parameterized
  Rule Types; declared-inputs composition, no Rule-to-Rule
  dependencies; Rule Scope reuses Analysis Scope's shape; Result
  identity extended with the consumed `AnalysisResult` set;
  layered outcome recording with the `NOT_APPLICABLE`-vs-no-Result
  binding sub-decision; validation/publish gate;
  `aip-rules` depends on `aip-core` only via `AnalysisResultSource`)
  are unmodified.

## Goals / Non-Goals

**Goals:**
- Give `AnalysisResultSource`, `RuleEvaluationResult`,
  `RuleEvaluationResultId`, and the Rule Evaluation Outcome
  vocabulary concrete Java shapes.
- Resolve `RuleEvaluationResultStore`'s module placement.
- Confirm Rule Scope's implementation is a direct, unmodified reuse of
  `CsmScope` — no new type, no adapter.
- Establish the fixture layer's shape and the explicit prerequisite
  gate on `implement-analysis-framework`'s own task completion.
- Establish test organization, mirroring
  `implement-analysis-framework` and `implement-csm-builder`.

**Non-Goals:**
- Reopening any `define-rule-framework` or `define-analysis-framework`
  decision.
- A real `aip-analysis` adapter (Binding Decision 2).
- Finding Model or Agent Framework implementation.
- Choosing `RuleEvaluationResultStore`'s concrete persistence
  technology — deferred, per `define-rule-framework/design.md`
  Decision 4/6 itself.
- Implementing `aip-analysis`'s own Analyzer/orchestrator engine, or
  resolving Analysis Framework's own pre-existing "no live producer"
  gap (`implement-analysis-framework/design.md`'s own named risk).

## Decisions

### 1. `RuleEvaluationResult`'s concrete shape: a single, non-generic `aip-core` type with an opaque `Object` payload

**Problem:** Given Binding Decision 1 (promotion to `aip-core`), what
is `RuleEvaluationResult`'s concrete Java shape?

**Decision:** `RuleEvaluationResult` is a **final class** (not sealed —
one artifact kind, Rule-Type-defined variation only in its payload) in
`aip.core.csm`, mirroring `AnalysisResult`'s shape field-for-field
where the underlying concept matches: identity
(`RuleEvaluationResultId`, Decision 2), the producing Rule's identifier
and version, the source `CsmSnapshotId` (obtained from `AnalysisView`,
never a direct `CsmSnapshotSource` dependency — the established
Decision 4 rule, unmodified), the `CsmScope` instance covered
(Decision 3), the complete set of consumed `AnalysisResultId`s
(`Set<AnalysisResultId>`), a `RuleEvaluationOutcome` value (Decision
4), and an opaque `Object` payload field.

**Alternatives considered:**
(none beyond what `AnalysisResult`'s own Decision 1 already settled —
this decision only confirms the identical reasoning transfers, per
your explicit direction; no new fork was found)

### 2. `RuleEvaluationResultId`: identity extended with the consumed `AnalysisResult` set

**Problem:** What concrete type represents `RuleEvaluationResult`'s
identity, given `define-rule-framework/design.md` Decision 4 extends
the established identity shape with a *set*, not a scalar?

**Decision:** `RuleEvaluationResultId`, a new `aip-core` record, is a
deterministic function of (Rule identifier, Rule version,
`CsmSnapshotId`, `CsmScope` instance, `Set<AnalysisResultId>`) —
computed via stable structural hashing over all five components,
consistent with `AnalysisResultId`'s own precedent (Decision 4 of
`implement-analysis-framework/design.md`) extended by one term, exactly
as `define-rule-framework/design.md` Decision 4 itself specifies. Two
evaluations with different consumed `AnalysisResultId` sets — even
with everything else identical — SHALL produce different
`RuleEvaluationResultId`s, per that same Decision, unmodified here.

**Alternatives considered:**
(none — this decision only fixes the concrete type; the identity
scheme itself was already settled and is not reopened)

### 3. Rule Scope: `CsmScope`, reused verbatim

**Problem:** Does this change need its own Rule Scope type?

**Decision:** **No.** Rule Scope *is* `CsmScope`
(`implement-analysis-framework/design.md` Decision 2), used exactly as
Analysis Scope uses it — no `aip-rules`-local wrapper, no adapter, no
new type of any kind. This is the direct implementation of
`define-rule-framework/design.md` Decision 3's own binding commitment,
made concrete now that `CsmScope`'s own shape has been fixed by
`implement-analysis-framework`'s planning.

**Alternatives considered:**
(none — this is a confirmation, not a fork; inventing any variant here
would directly contradict already-approved architecture)

### 4. `RuleEvaluationOutcome`: an `aip-core` enum with three values

**Problem:** Where does the `PASS`/`FAIL`/`NOT_APPLICABLE` vocabulary
live, and how is the binding "no Result at all" vs. "`NOT_APPLICABLE`
Result" distinction implemented?

**Decision:** `RuleEvaluationOutcome` is a three-value `aip-core` enum
(`PASS`, `FAIL`, `NOT_APPLICABLE`) — a field on `RuleEvaluationResult`,
alongside it in `aip.core.csm`, the same way `EffectiveKnowledgeStatus`
sits alongside `CsmElement`. The "not applicable, no Result at all"
case (kind-based applicability failure) is implemented as: no
`RuleEvaluationResult` is constructed at all — there is no fourth enum
value or sentinel for it, consistent with `define-rule-framework/spec.md`'s
own binding distinction (`Kind-Based Rule Applicability`,
`NOT_APPLICABLE` is a produced Result, not the absence of one).

**Alternatives considered:**
- **A fourth enum value (e.g. `NOT_EVALUATED`) representing the
  no-Result case.** Rejected — directly contradicts the already-approved
  binding distinction: "no Result" means no `RuleEvaluationResult`
  instance exists, not an instance carrying a special outcome value.
  Modeling it as an enum value would silently reopen a decision this
  design must not reopen.

### 5. `AnalysisResultSource`: this change's own `aip-core` contribution

**Problem:** `define-rule-framework/design.md` Decision 7 commits
`AnalysisResultSource` to `aip-core`, but no `implement-*` change has
built it — not `implement-analysis-framework` (it doesn't own this
contract; `aip-rules` does, per that same Decision 7), and not this
change until now.

**Decision:** `AnalysisResultSource` is an interface in `aip.core.csm`,
defined **by this change**: `read(AnalysisResultId): Optional<AnalysisResult>`,
`list(analyzerIdentifier: String, snapshotId: CsmSnapshotId): Set<AnalysisResult>`
— mirroring `CsmSnapshotSource`'s own shape. Since `AnalysisResult`
itself already lives in `aip-core` (per `implement-analysis-framework`'s
own Decision 1), this contract's return types require no import from
`aip-analysis` at all — the same clean separation `CsmSnapshotSource`
achieves relative to `aip-csm-builder`.

**Alternatives considered:**
- **Wait for `implement-analysis-framework` to define
  `AnalysisResultSource` itself, even though `define-rule-framework`
  assigns it to Rule Framework's own scope.** Rejected — `design.md`
  Decision 7 is unambiguous about ownership; building it here is
  implementing already-approved architecture, not inventing a new
  question.

### 6. `RuleEvaluationResultStore` stays in `aip-rules`, not promoted to `aip-core`

**Problem:** Now that `RuleEvaluationResult` lives in `aip-core`,
should its store abstraction move there too?

**Decision:** **No — stays in `aip-rules`**, mirroring
`AnalysisResultStore`'s own precedent (Decision 5 of
`implement-analysis-framework/design.md`) and `SnapshotStore`'s before
it: the *artifact* is promoted; the *store abstraction* (write/read/
list, eventual persistence technology) remains a producer-module
concern. A future `RuleEvaluationResultSource`
(`define-finding-model`'s own, not built by this change) is a
separate, smaller, read-only `aip-core` contract — the same
relationship this change's own `AnalysisResultSource` has to
`AnalysisResultStore`, one layer down.

**Alternatives considered:**
(none beyond what Decision 5 of `implement-analysis-framework/design.md`
already established as precedent; no new reasoning needed)

### 7. Rule Type contract and Rule declarative configuration: `aip-rules`-local

**Problem:** Where do the Rule Type contract (AIP-authored code) and
Rule (declarative configuration) live?

**Decision:** Both are `aip-rules`-local — the Rule Type contract
mirrors the Analyzer contract's own placement in `aip-analysis`
exactly (an interface: stable identifier, version, declared required
Analyzer identifiers, a `CsmScope`, and an evaluation method given an
`AnalysisView` and its declared `AnalysisResult`s, producing a
`RuleEvaluationOutcome` plus opaque payload). Rule (the configuration
binding a Rule Type to specific CSM vocabulary) is a plain,
`aip-rules`-local data type — no executable logic, per
`define-rule-framework/design.md` Decision 1, unmodified.

**Alternatives considered:**
(none — directly mirrors the Analyzer/`aip-analysis` precedent
established one layer down; no genuine fork)

### 8. Fixture layer: `CsmSnapshotSource`, `AnalysisView`, and `AnalysisResultSource` fixtures, no real `aip-analysis` adapter

**Problem:** How is this change tested without a working
`aip-analysis` implementation?

**Decision:** A test-only fixture-building API, `aip-rules`-local,
constructs `CsmSnapshotSource`/`AnalysisView`/`AnalysisResultSource`-
conforming instances directly from `aip-core` types (`CsmElement`,
`CsmRelationship`, `CsmSnapshotId`, `CsmScope`, `AnalysisResult`,
`AnalysisResultId`) — analogous to `implement-analysis-framework`'s own
`CsmSnapshotSource` fixture layer, and to `aip-csm-builder`'s
`RepositoryEvidenceModelBuilder` before that. No production code path
in `aip-rules` depends on it; a build/lint check confirms the fixture
package is test-scope only.

**Alternatives considered:**
- **Wait for a real `aip-analysis` implementation to test against.**
  Rejected — this is exactly Binding Decision 2, restated here as the
  concrete testing consequence: fixtures are sufficient and preferred,
  consistent with how every prior `implement-*` change in this project
  has tested against fixtures rather than a fully wired-up upstream.

### 9. Prerequisite gate: this change's task execution requires `implement-analysis-framework`'s own 52 tasks complete

**Problem:** Since `CsmSnapshotSource`, `AnalysisView`, `CsmScope`,
`AnalysisResult`, and `AnalysisResultId` must actually exist in
`aip-core` before this change's own `aip-core` additions
(`AnalysisResultSource`, `RuleEvaluationResult`,
`RuleEvaluationResultId`) can be written as real code, how is this
prerequisite tracked?

**Decision:** `tasks.md`'s own first section records this explicitly as
a **blocking prerequisite task**, not a silent assumption — this
change's own task execution does not begin until
`implement-analysis-framework`'s 52 tasks show complete. This is a
whole-change gate, not a partial-subset one: even though, in principle,
only `implement-analysis-framework`'s own `aip-core`-targeted tasks
(Sections 1-3, 7-8 of that change's `tasks.md`) are strictly needed for
this change's *types* to compile, gating on that change's own
Section 6 (Orchestrator) and later — its own task-ordering choice — is
the simplest, least error-prone way to avoid this change's own
execution tracking a moving, partial subset of a sibling change's
in-progress work.

**Alternatives considered:**
- **Gate only on `implement-analysis-framework`'s `aip-core`-targeted
  Sections (1-3, 7-8).** Rejected for now — technically sufficient,
  but would require this change's own execution to track a specific
  subset of another change's task list rather than a simple "is that
  change done" check, adding coordination overhead for a benefit
  (starting a few tasks earlier) this design does not judge worth the
  complexity. Revisitable if `implement-analysis-framework`'s own
  execution stalls partway with its `aip-core` work already done.

## Risks / Trade-offs

- [`RuleEvaluationResult` now lives in `aip-core`, the second
  framework-level artifact type placed there after `AnalysisResult`,
  both with opaque `Object` payloads] → Consistent, deliberate
  extension of established precedent; not treated as an accumulating
  risk, since `aip-core`'s own role (general, producer-agnostic
  vocabulary) is exactly what both types fit.
- [No real `aip-analysis` adapter (Binding Decision 2) means this
  change, even once complete, has no live producer feeding it real
  `AnalysisResult` content — the same "real component, no live
  upstream" position `implement-analysis-framework` itself named
  relative to `aip-csm-builder`, now recurring one layer up] → Named
  explicitly, consistent with how each prior `implement-*` change has
  named the identical shape of gap; a future integration change is the
  natural resolution.
- [Gating this change's entire task execution on
  `implement-analysis-framework`'s full 52-task completion (Decision
  9) may leave this change idle longer than strictly necessary] →
  Accepted trade-off for simplicity; the alternative (partial gating)
  was explicitly considered and rejected above.
- [`RuleEvaluationResultId`'s five-component identity (Decision 2) is
  the most complex identity computation in this project so far] →
  Not a new risk this change introduces — `define-rule-framework/design.md`
  Decision 4 already accepted this complexity; this design only gives
  it a concrete type.

## Migration Plan

None — `aip-rules` is a new module; no existing code changes (once
`implement-analysis-framework`'s own code exists; this change makes no
change to `aip-csm-builder` or `aip-analysis`).

## Open Questions

None remaining — the confirmed binding decision (`RuleEvaluationResult`
placement) and the two decisions carried from `explore.md` are all
reflected above; no new fork was found while giving them concrete
shape.
