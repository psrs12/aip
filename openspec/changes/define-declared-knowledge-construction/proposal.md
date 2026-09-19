## Why

The Canonical Software Model specification already fully defines
`declared` and `inferred` knowledge as first-class provenance
categories — `Provenance Classification of Knowledge`, `Effective
Knowledge and Precedence` (`DECLARED > OBSERVED > INFERRED`),
`Architecture Component Representation`, and `Architectural Boundary
Representation` all exist, are implemented in `aip-core`
(`ArchitectureComponentElement`, `CsmRelationshipType.BOUNDARY_CONSTRAINT`,
`ProvenanceCategory`), and are exercised by `SubjectConflictMarker`. But
no capability in this codebase produces a `declared` or `inferred` CSM
element. CSM Builder is mechanically forbidden from doing so
(`csm-builder`'s own `Exclusion of Architectural Inference and
Declared-Knowledge Construction` requirement, enforced by
`scripts/check-no-excluded-construction.sh`) — a deliberate v1 scoping
choice, not an oversight; `implement-csm-builder/design.md`'s own
Non-Goals named "designing any future declared- or inferred-knowledge-
producing capability" as a separate, unassigned future capability from
the start.

The direct consequence, identified in
`openspec/changes/explore-next-evolution/explore.md` §3/§5/§10.2: the
Architecture Compliance Agent — the flagship first agent
`openspec/project.md` §5 names — requires Architecture Component and
Architectural Boundary content to evaluate any policy at all, and today
has no mechanism, anywhere in the implemented system, that could
produce that content from a real, analyzed repository. It can only be
exercised via hand-built test fixtures. Every other capability in the
seven-step pipeline (`project.md` §11) is now implemented and tested;
this is the specific, named gap blocking the one built Agent from ever
running against real data.

This change resolves where and how declared (and, if warranted,
inferred) CSM knowledge gets constructed — closing the highest-priority
item in `explore-next-evolution`'s own recommended sequencing for
making the already-built pipeline usable against real repositories.

## What Changes

- Introduces a new capability responsible for constructing `declared`
  CSM knowledge — at minimum, Architecture Component and Architectural
  Boundary elements — from an explicit, human-authored source, and
  publishing it as CSM content the existing Effective Knowledge/
  Precedence/Conflict-Marking machinery (already implemented in
  `aip-core`) can reconcile against `observed` content from CSM
  Builder.
- Resolves, as binding Design decisions (not decided here in Proposal):
  module placement (new module vs. an existing module's new mode/
  subpackage); the declaration mechanism itself (what a human/
  authoritative source actually writes, and in what form); whether any
  `inferred` construction is in scope for this change or deferred
  entirely; and how constructed declared knowledge reaches the same
  snapshot/store surface `observed` CSM content uses today without
  reopening CSM Builder's own "observed-only" construction boundary or
  any other existing module-independence guard.
- Does **not** change `canonical-software-model`'s or `csm-builder`'s
  existing, approved requirements — the vocabulary and precedence rules
  this change targets are already specified; CSM Builder's own
  observed-only exclusion is a boundary this change respects, not
  revisits.
- Does **not** build a real `aip-csm-builder` → `aip-analysis` adapter,
  a concrete `Analyzer`, or concrete `*Store` persistence technology —
  explicitly deferred to later changes, per `explore-next-evolution`'s
  own priority order (items 2 and 3).
- No implementation code in this change — specification and design
  only, per the mandatory Explore → Propose → Design → Specify → Review
  → Implement workflow (`CLAUDE.md`).

## Capabilities

### New Capabilities
- `declared-knowledge-construction`: the capability that constructs
  `declared` (and, pending Design's decision, possibly `inferred`) CSM
  elements and relationships from an explicit source outside Repository
  Evidence, publishes them as CSM content, and makes them available
  alongside `observed` content for the existing Effective Knowledge/
  Precedence machinery to reconcile. Requirements and scenarios are
  written during Specify, informed by this change's own `design.md`.

### Modified Capabilities
(none — `canonical-software-model` and `csm-builder` are archived,
approved specifications; this change does not alter their requirements.
`canonical-software-model` already fully specifies the `declared`/
`inferred` vocabulary this new capability targets; `csm-builder`'s own
observed-only exclusion is a boundary this change respects.)

## Impact

- **Affected specs**: adds `openspec/specs/declared-knowledge-
  construction/spec.md` once Specify is reached. No changes to
  `canonical-software-model`, `csm-builder`, `analysis-framework`,
  `rule-framework`, `finding-model`, `agent-framework`, or
  `architecture-compliance-agent`.
- **Affected code**: none yet — specification and design only. A
  future `implement-declared-knowledge-construction` change performs
  implementation, mirroring every other capability's own
  define-then-implement sequencing in this project.
- **Dependencies**: genuinely open — Design must resolve whether this
  capability is a new module (depending on `aip-core` only, per every
  existing sibling's own precedent) or a new mode within
  `aip-csm-builder` itself. Either way, it SHALL NOT weaken or bypass
  `csm-builder`'s existing `Exclusion of Architectural Inference and
  Declared-Knowledge Construction` requirement for CSM Builder's own
  construction packages — this change adds a producer of declared
  knowledge elsewhere in the architecture, it does not relax CSM
  Builder's own boundary.
- **Affects future work**: unblocks Architecture Compliance Agent
  running against real, non-fixture Architecture Component/Boundary
  content, and — per `explore-next-evolution`'s own sequencing —
  precedes the real `aip-csm-builder` → `aip-analysis` adapter and
  `*Store` persistence work named as items 2 and 3 of the same
  priority order.
