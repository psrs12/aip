# Explore: Implement Analysis Framework

Status: preserved exploration notes carried into this change. Not a
tracked schema artifact — retained for traceability from
proposal/design decisions back to the reasoning that produced them,
following the same convention used by
`archive/2026-08-11-implement-csm-builder/explore.md` and
`implement-rule-framework/explore.md` (the change this one was
sequenced ahead of, per explicit direction).

## Problem Statement

`define-analysis-framework` is fully specified, reviewed, and merged
(`explore.md`, `proposal.md`, `design.md`, `specs/analysis-framework/
spec.md` — 18 requirements, 39 scenarios — and `tasks.md` — 14
sections, 47 tasks). This change turns that specification into working
code, the same relationship `implement-csm-builder` had to
`define-csm-builder`.

`implement-rule-framework`'s own Explore (immediately preceding this
one) found that Rule Framework's implementation cannot proceed until
Analysis Framework's own `aip-core` contributions exist — that finding
is why this change was sequenced first, per explicit direction. This
Explore's job is to establish whether *this* change has an analogous
bootstrapping problem of its own, and to resolve one real
specification ambiguity `implement-rule-framework`'s Explore already
surfaced but could not settle on its own: where does the
`AnalysisResult` type itself live.

## What actually exists in the codebase right now

Checked directly:

```text
Maven modules built so far: aip-core, aip-csm-builder

aip-core/src/main/java/aip/core/csm/  - the full CSM domain model
  (CsmElement and its 8 implementations, CsmElementId, CsmEntityKind,
  CsmRelationship, CsmRelationshipType, CsmValidator, DependencyKind,
  EffectiveKnowledgeStatus, NativeAttributes, ProvenanceCategory,
  ProvenanceRecord, Subject, SubjectConflictMarker, ValidationResult)

aip-core/src/main/java/aip/core/evidence/  - the Repository Evidence
  contract, implemented as part of implement-csm-builder itself
  (EvidenceItem, EvidenceId, EvidenceKind, DiscoveryOutcome,
  ChangeStatus, LifecycleState, and related types)

aip-csm-builder/  - the full, working CSM Builder implementation:
  Mappers for all seven structural entity kinds, identity derivation,
  provenance/traceability, containment and dependency relationship
  construction, Subject/conflict marking (ConflictedSubjects),
  CsmValidator/SnapshotPublisher gate, FilesystemSnapshotStore,
  and PriorElementLookup for incremental re-derivation

NOT present anywhere: aip-analysis, aip-rules, aip-findings, aip-ai;
  CsmSnapshotSource, AnalysisView, any Scope declaration type,
  AnalysisResult and everything Sections 6-9 of this change's own
  approved tasks.md build
```

**Unlike `implement-csm-builder`'s own situation (Repository
Understanding specified but never built) and unlike
`implement-rule-framework`'s (Analysis Framework specified but never
built), this change's own immediate upstream — CSM Builder — is
already fully implemented and working.** There is no bootstrapping
problem here in the sense either of those two prior Explores faced.

## Framing

```text
Repository (real code)
    v
Repository Understanding    <- SPEC ONLY (archived). Still no
                                implementation anywhere.
    v
Repository Evidence Model   <- IMPLEMENTED (aip-core, as part of
                                implement-csm-builder)
    v
CSM Builder                 <- IMPLEMENTED, working (aip-csm-builder)
    v
CSM Snapshot (aip-csm-builder's own Snapshot/SnapshotStore)
    v
Analysis Framework           <- THIS CHANGE
    v
AnalysisResult
```

CSM Builder's own upstream gap (no real Repository Understanding)
still exists and is unchanged by this change — it is not this
capability's problem to solve, the same way it was not
`implement-csm-builder`'s problem to solve beyond the fixture-based
Option C it already chose. This change's own upstream, by contrast, is
solid.

## What the already-approved `tasks.md` already resolved correctly

Read closely before treating anything as an open question, because
several apparent risks are already handled:

- **`aip-analysis` never depends on `aip-csm-builder`** (`design.md`
  Decision 1; `tasks.md` 1.2, 14.1) — confirmed as a hard,
  CI-enforced boundary, not merely a convention.
- **`CsmSnapshotSource` is tested against a hand-built fixture, not a
  live adapter over `aip-csm-builder`'s real `Snapshot`** (`tasks.md`
  1.4: "a hand-built fixture satisfying the contract, **distinct
  from** `aip-csm-builder`'s own `Snapshot` type"; Section 12's
  Fixture Layer). This is `implement-csm-builder`'s own Option C
  pattern, reapplied here — and it resolves cleanly on its own merits
  this time, not as a workaround for a missing upstream, but as the
  correct way to keep `aip-analysis` decoupled from `aip-csm-builder`
  permanently, per Decision 1's own dependency-direction rule.
- **A consequence worth naming explicitly, not silently accepted:**
  this change, as currently scoped by its own approved `tasks.md`,
  builds `CsmSnapshotSource` (the contract) and tests it against
  fixtures, but **does not** build a concrete adapter making
  `aip-csm-builder`'s actual `Snapshot`/`SnapshotStore` satisfy
  `CsmSnapshotSource` for real. Such an adapter would need to live
  *inside* `aip-csm-builder` (the only module permitted to depend on
  both `aip-core` and its own `Snapshot` type) — modifying
  `implement-csm-builder`'s already-archived work, which no task in
  either `implement-csm-builder`'s or this change's approved `tasks.md`
  does. Left as-is, after this change lands, Analysis Framework will
  be fully implemented and fully tested, but **not yet wired to real
  CSM Builder output** — a real, working component with no live
  producer feeding it, the mirror image of `implement-csm-builder`'s
  own "real component, no live consumer" situation one layer down.
  Worth confirming this is the intended shape for this change (an
  adapter would be a natural, small follow-up change, not
  necessarily this one's job) rather than silently discovered later.

## Open architectural question: where does `AnalysisResult` itself live?

Carried forward from `implement-rule-framework/explore.md`, now
properly this change's own question to resolve, since this is the
change that actually defines `AnalysisResult`.

`define-analysis-framework/tasks.md` Section 6 ("Analysis Result
Identity and Traceability") and Section 7 ("Analysis Result Payload")
never state a module for `AnalysisResult` itself — unlike Section 1
("`aip-core`") and Section 3 ("`aip-analysis`"), which both say so
explicitly. `design.md` Decision 4 discusses identity, traceability,
payload shape, and the `AnalysisResultStore` abstraction, mirroring
`aip-csm-builder`'s own `MapperAttribution`/`SnapshotStore` precedent
by name — but never assigns a package to the type.

This matters concretely because `define-rule-framework/design.md`
Decision 7 already commits `AnalysisResultSource` (a **future**
`aip-rules` contract, not built by this change) to living in `aip-core`
and returning `AnalysisResult` instances by identity. For that
already-approved commitment to be satisfiable without `aip-rules`
depending on `aip-analysis`, whatever `AnalysisResultSource` returns
must be reachable from `aip-core` alone. Two candidates, carried
forward with the same reasoning already laid out in
`implement-rule-framework/explore.md`:

- **(a) Disaggregated `aip-core` constituent parts** — an identity
  type, a traceability record, and an opaque payload holder (mirroring
  `NativeAttributes`) all live in `aip-core`; `AnalysisResult` as a
  single bundled type, if it exists as a class at all, is
  `aip-analysis`-internal, analogous to how `aip-csm-builder`'s own
  richer `Snapshot` type is never exposed by `CsmSnapshotSource`.
- **(b) `AnalysisResult` itself is an `aip-core` type** (interface or
  record), constructed by `aip-analysis`'s own Analyzers the same way
  `aip-csm-builder`'s Mappers construct `aip-core`'s `CsmElement`
  implementations without `CsmElement` itself living in
  `aip-csm-builder`. This extends `AnalysisView`'s own placement
  reasoning (`design.md` Decision 3: general mechanism belongs in
  `aip-core` from the start, once a future consumer is already named)
  one type further, since `AnalysisResultSource`'s own future
  existence is that same named future consumer.

**This Explore leans toward (b)** — not decided here, but worth stating
why: `CsmElement` is the closest existing precedent for "a type
multiple modules need to produce and consume without any one of them
owning it," and it already lives in `aip-core` as a sealed interface
with all eight implementations alongside it. `AnalysisResult` (and, by
the same reasoning, `RuleEvaluationResult`, `Finding`, and
`Recommendation` at their own later `implement-*` changes) is
structurally the same kind of thing: one conceptual artifact kind,
produced by many interchangeable producers (Analyzers), consumed by at
least one already-named future module (`aip-rules`). Option (a)'s
`CsmSnapshotSource` precedent is a partial counter-example, but a weak
one: it works there specifically because `CsmSnapshotSource`'s
*content* (elements, relationships) was already decomposable into
existing `aip-core` types before `CsmSnapshotSource` was designed —
`AnalysisResult` has no equivalent pre-existing decomposition, since
its payload is deliberately opaque and Analyzer-defined, closer in
spirit to `NativeAttributes` wrapping arbitrary content than to a set
of already-typed CSM elements.

Not adopted as binding without your confirmation, per the same
discipline every prior Design phase in this project has followed for a
genuine fork.

## Other open questions for Design

1. **`AnalysisResult` type placement** (above) — the central question.
2. **Real `CsmSnapshotSource` adapter** — confirm whether wiring
   `aip-csm-builder`'s actual `Snapshot` to satisfy `CsmSnapshotSource`
   is in this change's scope (would touch already-archived
   `implement-csm-builder` code) or is deliberately deferred to a
   later, small integration change, consistent with what the
   currently-approved `tasks.md` already does (fixtures only).
3. **Whether Decision 4's `AnalysisResultStore` interface shape needs
   adjustment** if option (b) is chosen — an `aip-core`-hosted
   `AnalysisResult` type would let `AnalysisResultStore` itself
   plausibly also move to `aip-core` (mirroring `AnalysisResultSource`'s
   own eventual promotion one layer up in Rule Framework's design), or
   it could stay `aip-analysis`-hosted with only the type it stores
   living in `aip-core` — a smaller, connected question Design should
   settle alongside question 1, not independently.

## Recommended Next Step

This change has no sequencing blocker of its own — it can proceed
straight to Design once question 1 (`AnalysisResult` placement) is
confirmed, since that answer shapes which package nearly every type in
Sections 6-9 of the approved `tasks.md` belongs in. Question 2 (real
adapter) is lower-stakes — it can be answered either way without
reshaping this change's own structure, only its scope boundary — but
still worth an explicit answer before Design treats it as settled.
