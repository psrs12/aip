# Explore: Implement Rule Framework

Status: preserved exploration notes carried into this change. Not a
tracked schema artifact — retained for traceability from
proposal/design decisions back to the reasoning that produced them,
following the same convention used by every prior Explore in this
project.

This is a **rewrite** of this change's own original `explore.md`. That
version found this change blocked on `implement-analysis-framework`
not existing at all, and surfaced an unresolved `AnalysisResult`
type-placement ambiguity it could not settle alone. Per explicit
direction, `implement-analysis-framework` was sequenced and planned
first (`Explore → Design → Tasks`, merged into `main`). This rewrite
re-examines both findings against that now-completed planning work,
rather than layering new findings on top of stale ones.

## Problem Statement

`define-rule-framework` is fully specified, reviewed, and merged (19
requirements, 42 scenarios, 59 tasks). `implement-analysis-framework`
is now also fully *planned* — `explore.md`, `proposal.md`, `design.md`,
`tasks.md` (15 sections, 52 tasks) — but, per `openspec list`, has
**zero of its 52 tasks completed**. This Explore re-checks the
codebase directly, confirms what changed and what didn't since the
original version, and re-evaluates whether this change can now proceed
to Design.

## What actually exists in the codebase right now

Checked directly, not assumed — **unchanged from the original
version**:

```text
Maven modules built so far: aip-core, aip-csm-builder (per the root
pom.xml <modules> list) — identical to before

NOT present anywhere in the codebase, still:
  - aip-analysis, aip-rules, aip-findings, aip-ai modules
  - CsmSnapshotSource, AnalysisView, CsmScope, AnalysisResult,
    AnalysisResultId, AnalysisResultStore, AnalysisResultSource
  - RuleEvaluationResult and every Rule Framework type
```

`implement-analysis-framework`'s own planning being complete and
merged did not add a single line of Java. The literal, checkable fact
from the original Explore still holds: **this change's own approved
`tasks.md`, read literally, cannot compile past its own Section 4
without `CsmSnapshotSource` and `AnalysisView` existing in `aip-core`
— and they still don't.**

## What changed: the `AnalysisResult` placement ambiguity is now resolved

This is the substantive change since the original version, and it is
good news. The original Explore raised, as its second major finding, a
genuine unresolved question spanning `define-analysis-framework` and
`define-rule-framework`: does `AnalysisResult` live in `aip-core` as a
single promoted type, or get disaggregated into `aip-core`-native
constituent parts the way `CsmSnapshotSource` avoids ever exposing
`aip-csm-builder`'s own `Snapshot`?

`implement-analysis-framework/design.md` Decision 1 already answered
this, binding on that change and directly informative here:
`AnalysisResult` is a single, non-generic `aip-core` type (`aip.core.csm`)
with an opaque `Object` payload — option (b) from the original
Explore, not option (a). Two further decisions from that same document
complete the picture this change needs:

- **Decision 2**: `CsmScope` — the shared Scope declaration type
  `define-rule-framework/design.md` Decision 3 already committed to
  reusing for Rule Scope — is also an `aip-core` type, planned (not yet
  built) in `implement-analysis-framework` itself. This directly
  answers the original Explore's question 3: Rule Scope *is* `CsmScope`,
  verbatim, no new type for this change to define at all once
  `implement-analysis-framework`'s own Section 2 lands.
- **Decision 3**: `CsmSnapshotId` — a `(repositoryIdentifier, sequenceNumber)`
  record — is `AnalysisView`'s own source-snapshot-identity type, the
  thing `define-rule-framework/design.md` Decision 4 already commits
  `RuleEvaluationResult` to obtaining *from* `AnalysisView` rather than
  a direct `CsmSnapshotSource` dependency.

None of this is new architecture this Explore is proposing — it is
reporting what a sibling, already-approved planning document already
decided, carried forward as binding context the same way this project
has repeatedly carried forward one capability's resolved decisions
into the next (e.g. Rule Framework's own Decision 3 reusing Analysis
Scope's shape verbatim).

## The same type-placement question, one layer up: where does `RuleEvaluationResult` live?

Raised in the original Explore as a question that would recur; now
this change's own version of it, informed by the precedent
`implement-analysis-framework` just set. `define-finding-model/design.md`
Decision 11 already commits a future `RuleEvaluationResultSource` to
living in `aip-core` and returning `RuleEvaluationResult` instances by
identity — the exact same shape `AnalysisResultSource` has to
`AnalysisResult`. Applying `implement-analysis-framework`'s own
resolution consistently (a single, non-generic `aip-core` type with an
opaque payload, not a disaggregated shape) is the strongly-favored
candidate for `RuleEvaluationResult` too — for the same reason: `Finding
Model` already names `RuleEvaluationResultSource` as a needed future
`aip-core` contract, the same "already-named future consumer" argument
that justified `AnalysisResult`'s own promotion.

Carried forward as a strong candidate, not silently assumed — this
change's own Design phase should state it explicitly, the same way
`implement-analysis-framework/proposal.md` recorded its own Binding
Decisions explicitly rather than leaving them implicit in `design.md`
alone.

## What this means for sequencing this change

The original Explore's three sequencing options are now reducible to
one real question, since planning-level sequencing already happened
(you resolved it): **does this change's own Design/Tasks planning
proceed now, ahead of `implement-analysis-framework`'s actual code
landing, or does even planning wait?**

Planning does not require compiled code to exist — every `define-*`
change in this project's history designed against sibling
specifications that were themselves unimplemented (Rule Framework's
own `define-rule-framework` was designed entirely against Analysis
Framework's *specification*, never its code). The same reasoning
applies here: this change's Design and Tasks phases can proceed now,
informed by `implement-analysis-framework`'s own approved (though
unbuilt) decisions.

**What cannot happen yet is checking off this change's own tasks** —
Sections referencing `CsmSnapshotSource`, `AnalysisView`, `CsmScope`,
or `AnalysisResult` will need those symbols to exist, which requires
`implement-analysis-framework`'s own tasks to actually be executed
first. This change's own `tasks.md`, once written, should record that
prerequisite explicitly in its first section — not as a silent
assumption, the same discipline the original Explore already
established and this rewrite preserves.

## Open architectural questions for Design

Reduced from five to two, since planning-sequencing, `AnalysisResult`
placement, and shared-Scope-type questions are now resolved by
`implement-analysis-framework`'s own approved artifacts:

1. **`RuleEvaluationResult` placement** — confirm the strong candidate
   above (single, non-generic `aip-core` type, opaque payload,
   mirroring `AnalysisResult` exactly) as this change's own binding
   decision, or find a reason to diverge.
2. **Fixture strategy** — can this change's own tests be built entirely
   against hand-built fixtures implementing `CsmSnapshotSource`/
   `AnalysisView`/`AnalysisResultSource` (using only `aip-core` types,
   once those exist), the same fixture-first pattern
   `implement-analysis-framework` itself used for `CsmSnapshotSource`?
   This determines whether this change's *tests* can be written and
   even run before `implement-analysis-framework`'s own code lands
   (against fixtures satisfying the same contracts), even though the
   *production* code obviously cannot compile without the real types
   existing in `aip-core`.

## Recommended Next Step

Proceed to Design. Question 1 is this Explore's central remaining
architectural fork and should be resolved explicitly before Design
builds on it, per the same discipline applied to every prior fork in
this project — but it is a much narrower, better-grounded question
than the original Explore faced, since `implement-analysis-framework`'s
own precedent answers it by direct, consistent analogy rather than
leaving two genuinely open candidates. Question 2 is lower-stakes and
can be resolved alongside it.
