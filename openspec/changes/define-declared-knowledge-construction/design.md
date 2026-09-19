## Context

See `proposal.md` for motivation. This document resolves the open
questions the proposal deliberately deferred: module placement, the
declaration mechanism, whether `inferred` construction is in scope, and
how constructed declared knowledge reaches the same surface `observed`
CSM content uses today.

Binding external facts this design does not reopen:
- `aip-core` already hosts the full target vocabulary: `Provenance
  Classification of Knowledge` (`ProvenanceCategory.DECLARED`/
  `INFERRED`/`OBSERVED`), `ArchitectureComponentElement` (constructor-
  enforced non-`OBSERVED` provenance), `CsmRelationshipType.BOUNDARY_CONSTRAINT`,
  and `CsmValidator`'s boundary-provenance check. None of these need to
  change.
- `csm-builder`'s own `Exclusion of Architectural Inference and
  Declared-Knowledge Construction` requirement forbids CSM Builder's
  construction packages (`aip.csmbuilder.mapper`/`mapping`) from ever
  constructing an `ArchitectureComponentElement` or a
  `BOUNDARY_CONSTRAINT` relationship, enforced by
  `scripts/check-no-excluded-construction.sh`. This design does not
  touch CSM Builder at all — it adds a producer of declared knowledge
  elsewhere in the architecture, never inside CSM Builder's own
  construction packages.
- `csm-builder`'s `CSM Element Identity Stability Across Snapshots`
  requirement states its own rationale directly: "This stability is
  intended to make it possible for a future declared-knowledge mapping
  anchored to a CSM element's identity... to remain valid across
  snapshot boundaries... That anchoring mechanism itself belongs to
  whichever future capability ingests declared knowledge, not to CSM
  Builder." This design is that capability, and treats CSM element
  identity (`CsmElementId`) as the stable anchor it was designed to be.
- `aip-rules`'s `BoundaryComplianceRuleType` already reads
  `BOUNDARY_CONSTRAINT` relationships via a specific, existing
  `NativeAttributes` convention — `constraint-kind` =
  `must-not-depend-on` (`aip.rules.boundarycompliance.BoundaryConstraintKind`)
  — not a dedicated `CsmRelationship` field. Any `BOUNDARY_CONSTRAINT`
  relationship this new capability constructs must satisfy that exact,
  already-established convention to be evaluable by the one Rule Type
  that exists, not a second, competing encoding.
- `CsmSnapshotSource` (`aip-core`) is a minimal, producer-agnostic
  contract — elements, relationships, and a stable snapshot identity.
  `aip-analysis` and `aip-rules` already depend on it without depending
  on `aip-csm-builder`; this design follows the identical pattern.
- `check-no-csm-builder-analysis-adapter.sh` forbids `aip-csm-builder`
  from referencing `CsmSnapshotSource`/`CsmScope`/`AnalysisResult` at
  all. This design's new module never touches `aip-csm-builder` either,
  so this guard is unaffected and unrelaxed.

## Goals / Non-Goals

**Goals:**
- Resolve where declared-knowledge-construction code lives and what it
  depends on.
- Resolve how a human (or authoritative external source) actually
  asserts declared knowledge in v1.
- Resolve how declared elements/relationships reference existing CSM
  structure (Modules, Packages) without this capability depending on
  `aip-csm-builder`.
- Resolve how constructed declared knowledge combines with `observed`
  CSM content so a downstream consumer (Analysis View, Rule evaluation)
  can see both together.
- Resolve whether `inferred` knowledge construction is in scope for
  this change.
- Resolve the validation/publish gate for constructed declared content.

**Non-Goals:**
- Writing `openspec/specs/declared-knowledge-construction/spec.md`
  (Specify phase).
- Any file format, DSL, or non-Java authoring surface for declarations
  — this design resolves the declaration *mechanism* (a plain value
  type), not an authoring *experience*. Mirrors `define-rule-framework/
  design.md` Decision 1's own explicit deferral of a policy DSL for
  `Rule` — the identical gap, now named for declared knowledge too, not
  closed here either.
- Any `inferred`-knowledge-producing mechanism (heuristics, AI/LLM
  reasoning). Explicitly deferred — see Decision 2.
- Choosing concrete persistence technology for this capability's own
  `*Store` — deferred the same way every prior layer's `*Store` deferred
  it (`SnapshotStore` → `AnalysisResultStore` → `RuleEvaluationResultStore`
  → `FindingStore` → `RecommendationStore`, per `explore-next-evolution/
  explore.md` §3's own inventory).
- The real `aip-csm-builder` → `aip-analysis` adapter, or a concrete
  `Analyzer` — explicitly out of scope per `proposal.md`, item 2 of
  `explore-next-evolution`'s own priority order.
- Wiring this capability into a real CLI/server entry point — none
  exists yet for any capability in this codebase.
- Modifying `ArchitectureComponentElement`'s or `CsmValidator`'s
  existing constructor/validation guards — this design reuses them
  exactly as they exist today.

## Decisions

### 1. Module placement: a new module, `aip-declared-knowledge`

**Problem:** Does this capability live in a new module, or as a new
mode/subpackage inside `aip-csm-builder`?

**Decision:** A new module, `aip-declared-knowledge` (package
`aip.declaredknowledge`), depending on `aip-core` only — mirroring
every sibling built so far (`aip-csm-builder`, `aip-analysis`,
`aip-rules`, `aip-findings`, `aip-ai`), each independently dependent on
`aip-core` and never on one another. Enforced identically:
`scripts/check-module-dependencies.sh aip-declared-knowledge
aip:aip-core`, wired into this module's own `verify` phase.

**Alternative considered:** a new subpackage inside `aip-csm-builder`
(e.g. `aip.csmbuilder.declared`). Rejected — `aip-csm-builder`'s own
`check-no-excluded-construction.sh` guard is scoped to `aip.csmbuilder.
mapper`/`mapping` specifically, precisely so a future declared-knowledge
producer would not need to touch it; adding one *inside* the same
module would put a declared-knowledge-constructing package alongside a
module whose defining architectural property (per `implement-csm-
builder/design.md`) is "constructs `observed` content only," inviting
exactly the confusion `csm-builder`'s own Exclusion requirement was
written to prevent structurally. A new module keeps the two production
concerns (observed construction vs. declared construction) as
independently testable, independently CI-guarded siblings — the same
reasoning `implement-analysis-framework/design.md` Decision 1 used for
`aip-analysis` itself.

### 2. Scope: `declared` knowledge only in v1; `inferred` remains a separate, unassigned future capability

**Problem:** `implement-csm-builder/design.md`'s own Non-Goals named
"designing any future `declared`- or `inferred`-knowledge-producing
capability" together, as one deferred pair. Does this change resolve
both?

**Decision:** No — this change constructs `declared` knowledge only.
`inferred` knowledge, by the CSM's own `Provenance Classification of
Knowledge` definition, is "produced by AIP's own heuristics or AI
reasoning" — building a producer of it now would mean introducing
heuristic or AI-reasoning logic before any real, non-fixture
`observed`/`declared` content exists to reason over, which is exactly
the premature-AI-layer risk `explore-next-evolution/explore.md` §7
already names as a reason *not* to build a real LLM adapter yet, and
which `CLAUDE.md`'s own "Deterministic Analysis Before AI" principle
argues against generally. `declared` knowledge construction is fully
deterministic — a human or authoritative external source states a
fact; nothing is inferred — so it carries none of that risk and can
proceed independently. A future, separate capability may address
`inferred` construction once real declared/observed content exists to
validate heuristics against.

**Alternative considered:** resolve both in one change, since
`implement-csm-builder/design.md` named them as a pair. Rejected —
they have materially different risk profiles (deterministic vs.
heuristic/AI) and materially different urgency (declared knowledge
directly unblocks Architecture Compliance Agent today; inferred
knowledge does not, since nothing currently requires it). Splitting
them is consistent with this project's own "specify one capability at
a time" discipline throughout `project.md` §11's evolution order.

### 3. Declaration mechanism: a plain Java value type, no DSL

**Problem:** How does a human (or authoritative external source)
actually state "Component A = {Module X, Module Y}" or "Component A
must not depend on Component B"?

**Decision:** A `Declaration` is a plain, immutable Java value —
mirroring `Rule`'s own v1 shape exactly (`define-rule-framework/
design.md`: "a `Rule` is a plain, serializable value... constructed by
Java code calling `new Rule(...)`"). At minimum, two declaration
shapes: an Architecture Component composition declaration (a name plus
one or more existing `CsmElementId`s) and a Boundary Constraint
declaration (a source Architecture Component identity, a target
Architecture Component or External System identity, and a constraint
kind). No file format, DSL, or non-Java authoring surface is built.

**Alternative considered:** a YAML/JSON declaration file with a parser.
Rejected for v1 — `define-rule-framework/design.md` Decision 1 already
declined to build an equivalent authoring surface for `Rule` for the
same reason: real usage patterns for what an authoring format needs to
express are not yet known, and building one prematurely risks the
"classes... simply because they appear useful" anti-pattern `CLAUDE.md`
warns against. This is recorded as a named, explicit gap (mirroring
`explore-next-evolution/explore.md` §4's own "Policy authoring has no
data-driven surface" finding, now naming the identical gap for declared
knowledge) — not a decision that this gap should never be closed.

### 4. Referential integrity: declarations are validated against a supplied baseline `CsmSnapshotSource`

**Problem:** A composition declaration names existing Module/Package
identities. Without depending on `aip-csm-builder`, how does this
capability know those identities are real, and not a typo or a
reference to structure that no longer exists?

**Decision:** Construction takes two inputs: a baseline
`CsmSnapshotSource` (the `observed` content the declarations are
declared *against* — supplied by whatever caller has one, exactly the
same "depends on the contract, not the producer" shape `aip-analysis`
and `aip-rules` already use) and a set of `Declaration`s. A
`DeclaredKnowledgeValidator` checks, before publication: every
`CsmElementId` a composition declaration references SHALL exist within
the supplied baseline; every Architecture Component identity a
Boundary Constraint declaration references SHALL exist among the
declared Architecture Components being published in the same batch (an
Architecture Component is never part of the `observed` baseline, per
`csm-builder`'s own exclusion). This is the anchoring mechanism
`CSM Element Identity Stability Across Snapshots` names as "belonging
to whichever future capability ingests declared knowledge" — resolved
here as depending on `CsmSnapshotSource` (an `aip-core` contract), not
on `aip-csm-builder`.

**Alternative considered:** no referential-integrity check at all,
trusting the declaring human. Rejected — every prior layer in this
codebase (`ProvenanceGuard`, `ExcludedRelationshipTypeGuard`,
`CsmValidator`, `AnalysisResultValidator`, Rule Framework's own
evaluation-input validation) is consistently defense-in-depth; a typo'd
`CsmElementId` failing loudly at construction time, rather than
silently producing an Architecture Component that references nothing,
is the same discipline applied one layer further out.

### 5. Boundary Constraint encoding reuses the existing `constraint-kind` convention

**Problem:** How does a constructed `BOUNDARY_CONSTRAINT` relationship
actually express "must not depend on" so `BoundaryComplianceRuleType`
(the one Rule Type that exists) can evaluate it?

**Decision:** Reuse `aip-rules`'s existing `constraint-kind`
`NativeAttributes` convention (`ATTRIBUTE_KEY = "constraint-kind"`,
`MUST_NOT_DEPEND_ON = "must-not-depend-on"`) verbatim — this module
constructs a `BOUNDARY_CONSTRAINT` `CsmRelationship` carrying that exact
attribute key/value for a "must not depend on" declaration, and SHALL
NOT introduce a second, parallel encoding for the same concept. Other
constraint shapes (e.g. "must only communicate via") remain
unconstructed by this capability in v1, consistent with
`BoundaryComplianceRuleType`'s own current scope (`Must-Only-
Communicate-Via Constraints Are Out of Scope`) — there would be no
consumer for them yet regardless.

**Alternative considered:** invent this module's own boundary-
constraint vocabulary independent of `aip-rules`'s existing one.
Rejected outright — would silently produce `BOUNDARY_CONSTRAINT`
relationships the one existing Rule Type can never evaluate, defeating
this change's own stated purpose of letting Architecture Compliance
Agent run against real declared content.

### 6. Output shape and combining with `observed` content: a new, general `aip-core` combinator, not a special-case merge

**Problem:** `AnalysisView.from(CsmSnapshotSource)` and Rule Framework's
own consumption both take exactly one `CsmSnapshotSource`. This
capability produces its own declared-only content. How does a
downstream consumer see `observed` and `declared` content together
without this module depending on `aip-csm-builder`, and without
reopening the deferred real adapter (item 2)?

**Decision:** This capability publishes its own `CsmSnapshotSource`-
conforming artifact containing only the newly constructed declared
elements/relationships, tied to the same repository identity as the
baseline it was validated against. A new, general `aip-core` mechanism
— `CompositeCsmSnapshotSource` (placement precedent: `AnalysisView`,
`CsmScope`, `CsmScopeEvaluator`, `CsmScopeChangeDetector` — a general
CSM capability usable by any future consumer, not `aip-declared-
knowledge`-local) — takes two or more `CsmSnapshotSource` instances of
the same repository identity and exposes their unioned elements and
relationships as one combined `CsmSnapshotSource`. A caller combines
one `observed` source (from `aip-csm-builder`, fixture or real) with
one `declared` source (from this capability) to get the single,
combined source `AnalysisView`/Rule evaluation already expect. This
requires no change to `aip-csm-builder`, no relaxation of
`check-no-csm-builder-analysis-adapter.sh` (which governs `aip-csm-
builder`'s own outbound references, not a third party combining two
`CsmSnapshotSource`s), and works identically today against two
fixtures, or later against a real adapter's output — genuinely
adapter-agnostic.

**Alternative considered:** have this new module depend on `aip-csm-
builder` directly to merge against a real `Snapshot`. Rejected — this
is exactly the sibling-module coupling this project has avoided at
every prior layer (`implement-analysis-framework/design.md` Decision
1's own rejected alternative, verbatim reasoning applied here).

**Alternative considered:** defer combining entirely, publishing only a
declared-only source with no combination mechanism at all. Rejected —
would leave this change unable to demonstrate its own stated purpose
(closing the gap for Architecture Compliance Agent, which needs both
`DEPENDENCY` (`observed`) and `BOUNDARY_CONSTRAINT` (`declared`)
content together, per `BoundaryComplianceRuleType`'s own declared
scope) even in a fixture-based end-to-end test.

### 7. Validation/publish gate and `*Store` abstraction

**Problem:** Does this capability need its own validate-before-publish
gate, mirroring every prior layer?

**Decision:** Yes. `DeclaredKnowledgeValidator` (Decision 4's
referential-integrity check, plus reuse of `aip-core`'s existing
`CsmValidator` for the boundary-provenance check it already performs)
gates a `DeclaredKnowledgePublisher`, which alone may write to a new
`DeclaredKnowledgeStore` abstraction (write/read-by-identity/list) —
mirroring `SnapshotStore`/`AnalysisResultStore`/
`RuleEvaluationResultStore`'s own shape exactly, concrete persistence
technology deferred identically. Identity for a published declared-
knowledge artifact is a deterministic function of the repository
identity, the baseline `CsmSnapshotSource` identity it was validated
against, and a sequence/version number for that repository's declared
content — never random, mirroring every other artifact's identity
discipline in this codebase.

**Alternative considered:** no store at all, treating declared content
as always ephemeral/recomputed. Rejected — declared knowledge is
exactly the kind of knowledge Non-Destructive Preservation of
Competing Knowledge expects to remain "independently identifiable and
auditable," which requires durability the same way every other
artifact layer in this pipeline already committed to.

## Risks / Trade-offs

- [No DSL/file format for declarations (Decision 3) means only
  Java-writing users can author declared knowledge in v1] → Mitigation:
  identical, already-accepted limitation for `Rule` authoring today;
  a future policy-as-code authoring capability could plausibly cover
  both uniformly, but is not blocked by this decision.
- [`CompositeCsmSnapshotSource` (Decision 6) is a new general surface in
  `aip-core`, and a new consumer of it always exists as soon as this
  module does] → Mitigation: kept deliberately minimal — union of
  elements/relationships only, no conflict resolution beyond what
  `AnalysisView`'s existing `SubjectConflictMarker` already performs
  downstream; not a new reconciliation mechanism.
- [Declarations are validated against whatever baseline
  `CsmSnapshotSource` is supplied at construction time, which may be
  stale relative to the latest real CSM Builder run once a real adapter
  exists] → Mitigation: `CSM Element Identity Stability Across
  Snapshots` already guarantees identity stability for as long as the
  underlying Repository Evidence persists; validation always re-checks
  against whatever baseline is actually supplied, never trusting a
  previously-validated result silently.
- [This change alone still does not make Architecture Compliance Agent
  runnable against a *real*, non-fixture CSM Builder output — the real
  `aip-csm-builder` → `aip-analysis` adapter (item 2) is still required]
  → Mitigation: explicitly out of scope here, named as the next change
  in `explore-next-evolution`'s own priority order; this change removes
  the other blocker (no producer of declared content at all) so item 2
  is sufficient to complete the picture once built.
