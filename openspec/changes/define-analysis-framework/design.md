## Context

See `proposal.md` for motivation and `explore.md` for the seven
resolved directional decisions this design treats as binding
constraints (dedicated `aip-analysis` module; Analysis Results durable
in concept, persistence technology deferred; Analyzers consume an
Analysis View of effective knowledge + conflict status; deterministic-
only, no AI/LLM surface; incremental analysis architecturally
supported, sophisticated invalidation deferred; single-repository v1;
Analyzers declare an explicit Analysis Scope). This document resolves
the six architectural questions Explore carried forward, plus the
Analysis Scope vocabulary Explore left open, and records the resulting
architecture. No code exists yet — this change, like
`define-csm-builder` before it, is specification and design only.

Binding external facts this design does not reopen:
- `aip-core` already hosts the CSM domain model (`aip.core.csm`),
  including `Subject`, `EffectiveKnowledgeStatus`, and
  `SubjectConflictMarker` (added during `implement-csm-builder` to
  close a gap found mid-implementation).
- `aip-csm-builder` already hosts `Snapshot`/`SnapshotStore`/
  `SnapshotManifest` as its own persistence layer, and
  `CsmValidator`/`SnapshotPublisher` as its own validation-before-
  publish gate. Neither is modified by this change.
- CLAUDE.md's module chain (`aip-core → aip-analyzer → aip-rules →
  aip-ai → aip-cli/aip-server`) names no home for this capability, per
  Explore's framing; resolved by Decision 1 below.

## Goals / Non-Goals

**Goals:**
- Resolve where Analysis Framework's code lives and what it depends on.
- Resolve whether Analyzers may depend on one another's output.
- Resolve where Analysis View construction belongs.
- Resolve the durable Analysis Result identity/traceability model.
- Resolve whether Analysis Results require a validation/publish gate.
- Resolve the concurrency model for Analyzer execution.
- Resolve the Analysis Scope vocabulary and how it relates to Analyzer
  applicability (including multi-language/ecosystem-specific
  Analyzers).

**Non-Goals:**
- Writing `openspec/specs/analysis-framework/spec.md` (Specify phase).
- Choosing the Analysis Result persistence *technology* (file-backed,
  embedded DB, other) — Explore decision 2 explicitly defers this the
  same way `implement-csm-builder` deferred Snapshot persistence
  technology until its own Decision 2 resolved the abstraction shape
  first. This design resolves the abstraction shape; the concrete
  implementation is `implement-analysis-framework`'s job.
- Any Rule Framework, Finding Model, or Agent Framework concern. This
  design goes only as far as producing a durable Analysis Result a
  future Rule Framework can read.
- Writing any Analyzer implementation (dependency-cycle detection,
  coupling metrics, etc.) — this change defines the framework they
  plug into, not the Analyzers themselves.
- Implementation code of any kind.

## Decisions

### 1. Module placement and the `aip-core` snapshot-read contract

**Problem:** `aip-analysis` needs to read CSM content, but CSM content
is currently only reachable through `aip-csm-builder`'s own
`Snapshot`/`SnapshotStore` types — the *producer's* persistence
implementation, not a general contract. Depending on those types
directly would couple a downstream, many-future-consumers module
(`aip-analysis`, and eventually Rule Framework, Agent Framework, a
future CLI) to one specific producer's implementation module, which
`implement-csm-builder`'s own Decision 9 (`mapping` never depends on
`snapshot`, even within the *same* module) already treated as an
anti-pattern one level down.

**Decision:** `aip-core` gains a minimal, read-only contract —
`CsmSnapshotSource` — exposing exactly what any consumer needs and
nothing about how it was produced or persisted: the CSM elements and
relationships, plus the minimal identity to reference it elsewhere
(repository identifier, sequence number). This mirrors
`implement-csm-builder` Decision 1's own resolution for the Repository
Evidence contract almost exactly: `aip-core` hosts the *contract*,
`aip-csm-builder` is the first *producer* of instances of it (its own
`Snapshot` type is adapted to satisfy `CsmSnapshotSource`, not
replaced), and `aip-analysis` is the first *consumer*, depending only
on `aip-core`, never on `aip-csm-builder`.

`aip-analysis` is a new module. It depends on `aip-core` only.
Concretely wiring a real `CsmSnapshotSource` implementation
(`aip-csm-builder`'s `Snapshot`, or a future alternative) into an
Analysis run is deferred to whatever future application/CLI entry
point already has that job for CSM Builder's own `SnapshotPublisher` —
this design does not build that wiring, the same way
`implement-csm-builder` never wired a real Repository Understanding
pipeline into CSM Builder either.

```
                        aip-core
        +------------------------------------------+
        |  aip.core.csm                             |
        |   - CSM domain model                      |
        |   - Subject / EffectiveKnowledgeStatus /   |
        |     SubjectConflictMarker (existing)       |
        |   - CsmSnapshotSource (new, this change)   |
        |   - AnalysisView (new, this change)        |
        +---------------------+----------------------+
                    ^                       ^
          depends on |                       | depends on
                    |                       |
        +-----------+--------+   +----------+-----------+
        |  aip-csm-builder    |   |  aip-analysis         |
        |  (existing;         |   |  (new module;         |
        |   Snapshot adapts   |   |   THIS CHANGE)         |
        |   to                |   |   Analyzer / Registry /|
        |   CsmSnapshotSource)|   |   Orchestrator         |
        +---------------------+   +------------------------+

              aip-csm-builder and aip-analysis do not
                    depend on each other
```

**Alternative considered:** `aip-analysis` depends on `aip-csm-builder`
directly, importing `Snapshot`/`SnapshotStore`. Rejected — simpler
short-term, but re-creates exactly the coupling
`implement-csm-builder` deliberately avoided internally (Decision 9),
one layer up, and would force every future CSM consumer to depend on
CSM Builder's own implementation module rather than a stable contract.

**Alternative considered:** fold `aip-analysis` into `aip-csm-builder`
itself (no new module). Rejected — conflates construction (CSM
Builder's job, already complete and scoped) with consumption (this
capability's job), and blurs `aip-csm-builder`'s own dependency-graph
guard, which currently and deliberately enforces "depends on
`aip-core` only."

### 2. Analyzer independence — no Analyzer-to-Analyzer dependency graph in v1

**Problem:** CSM Builder's Evidence-Kind Mappers are deliberately
independent of one another (Section 5's identity derivation is a pure
function of Evidence identity specifically so no Mapper needs another
Mapper's output). Analyzers plausibly are not: a cycle detector could
reasonably build on a dependency-graph Analyzer's traversal rather than
recomputing it.

**Decision:** Analyzers remain independent of one another in v1 — no
declared Analyzer-to-Analyzer dependency, no topological execution
order, no Analyzer reads another Analyzer's `AnalysisResult`. Every
Analyzer reads only from the `AnalysisView` (Decision 3) built once per
run from the CSM Snapshot. Recomputing shared substrate (e.g. two
Analyzers both traversing the dependency graph) is an accepted,
revisitable v1 cost, not a design flaw to engineer away now.

**Alternative considered:** a declared Analyzer dependency graph with
topological execution order (mirroring a build-system task graph).
Rejected for v1 — real complexity (cycle detection in the *Analyzer*
graph itself, versioning "which version of Analyzer B's output does
Analyzer A expect," re-derivation cascades when a dependency changes)
for a benefit (avoiding redundant computation) that is a performance
concern, not a correctness one, at the scale this capability is being
built for first. `implement-csm-builder`'s own Extension Mechanism
Verification precedent (Section 20: registering a new Mapper requires
no change to core mechanisms) is worth preserving here too — the
simplest way to guarantee that is for Analyzers to never depend on each
other's registration or output shape.

**Alternative considered:** a "Derived View" caching layer — shared,
reusable intermediate structures (e.g. "the dependency graph") computed
once per run and exposed alongside the `AnalysisView`, without
Analyzers depending on each other directly. Not rejected outright, but
deferred: this is a strictly additive optimization (a new, optional
input Analyzers may consume) that can be introduced later without
breaking any Analyzer already conforming to the "Analyzer reads only
the `AnalysisView`" contract this decision establishes now. Recorded
here so a future change doesn't need to rediscover this option.

### 3. Analysis View construction lives in `aip-core`, as a general CSM capability

**Problem:** Building an "effective knowledge + conflict status" view
requires applying `aip-core`'s existing `SubjectConflictMarker` to a
set of CSM elements/relationships. Is that `aip-analysis`'s own
responsibility, or does it belong to a shared boundary?

**Decision:** The pure computation — given CSM elements and
relationships, produce a `Subject → (effective content,
EffectiveKnowledgeStatus)` view — is a general CSM capability and lives
in `aip-core` as a new `AnalysisView` type, built directly on top of
`SubjectConflictMarker` (which already operates on plain
`List<CsmElement>`/`List<CsmRelationship>`, with no dependency on
`Snapshot` or any other module-specific type). This mirrors exactly how
`CsmValidator` already operates on plain element/relationship lists
rather than on `aip-csm-builder`'s `SnapshotContent`.

`aip-analysis`'s own responsibility is orchestration-level only: given
a `CsmSnapshotSource` (Decision 1), build its `AnalysisView` once per
run (via `aip-core`'s new utility) and hand it to every applicable
registered Analyzer. This is the same shape
`aip-csm-builder`'s own `ConflictedSubjects` already establishes as
precedent: general mechanism in `aip-core`, thin module-specific
integration in the consuming module.

**Alternative considered:** build `AnalysisView` construction inside
`aip-analysis` itself, operating on `aip-core` types but living in the
consumer module. Rejected — every future consumer of CSM content
(Rule Framework, a future CLI query surface, Agent Framework) will need
the identical "what's the effective knowledge and conflict status for
this subject" answer; duplicating that logic per consumer, or forcing
every future consumer to depend on `aip-analysis` just to get it, is
strictly worse than the one-line-of-reasoning fix of keeping it general
in `aip-core` from the start.

### 4. Analysis Result identity, traceability, and the result-store abstraction

**Problem:** What makes an `AnalysisResult` durable and traceable, and
what shape does storing/retrieving one take (independent of storage
*technology*, which stays deferred)?

**Decision:**
- **Identity** is a deterministic function of (Analyzer identifier,
  Analyzer version, source `CsmSnapshotSource` identity, the specific
  Analysis Scope instance covered) — never random/UUID-based,
  mirroring every identity-derivation discipline already established
  by `implement-csm-builder` (Sections 5, 8's `ElementIdentityDeriver`
  precedent). This is what lets incremental re-analysis (Explore
  decision 5) recognize "this Result's inputs are unchanged" without
  needing a separate change-tracking mechanism.
- **Traceability**: every `AnalysisResult` records which Analyzer (+
  version) produced it, which CSM Snapshot identity it was computed
  against, and which Analysis Scope instance it covered — directly
  analogous to `aip-csm-builder`'s own `MapperAttribution`/Snapshot
  Manifest, one layer up. This is required for AIP's Explainability
  principle (`project.md` §3.3) to hold at this layer, not merely
  convenient.
- **Payload shape is Analyzer-defined and opaque to the framework.**
  The framework does not need to understand what is *inside* a
  cycle-detector's result versus a coupling-metric's result — only how
  to store, retrieve, and trace it. This mirrors how CSM's own
  `NativeAttributes` is the vocabulary's own opaque escape hatch,
  applied here to Analysis Result content instead of CSM element
  attributes.
- **Store abstraction**: an `AnalysisResultStore` interface — write,
  read-by-identity, list-by-(Analyzer, snapshot) — is defined now,
  mirroring `SnapshotStore`'s own shape (`write`/`read`/
  `sequenceNumbers`/`readLatest`). Exactly one concrete implementation
  choice is deferred to `implement-analysis-framework`, the same way
  `implement-csm-builder` resolved `SnapshotStore`'s abstraction in its
  own Decision 2 before choosing `FilesystemSnapshotStore` as its first
  (and, so far, only) implementation.

**Alternative considered:** Analysis Results are always ephemeral,
recomputed on demand, never persisted. Rejected by Explore decision 2
already (durable in concept) — recorded here only to note *why* that
decision matters mechanically: without durability, "traceable from a
Finding back through the Analysis Result to the CSM elements involved"
(the Explainability chain `project.md` §6 describes: Facts → Analysis →
Rules → Findings → AI Reasoning → Recommendations) has nothing to
trace back *to* once the process that computed it exits.

### 5. Analysis Results require a validation/publish gate — with Analysis-specific checks, not CSM's checks reused verbatim

**Problem:** CSM Builder gates snapshot publication with
`CsmValidator`/`SnapshotPublisher` (an invalid snapshot is never
written). Does Analysis Framework need an equivalent, given Analysis
Results are not CSM knowledge at all (a distinct concept in the CSM
spec's own Evidence → Knowledge → Analysis → Findings pipeline) and
carry no provenance-category/confidence vocabulary to validate against?

**Decision:** Yes, but the checks are different in kind, not copied
from `CsmValidator`. An `AnalysisResultValidator` (mirroring
`CsmValidator`'s shape: a pure function returning a `ValidationResult`)
checks, before an `AnalysisResultPublisher` (mirroring
`SnapshotPublisher`) ever calls `AnalysisResultStore.write`:
- **Referential integrity** — every CSM element/Subject identity an
  `AnalysisResult` references SHALL actually exist in the CSM Snapshot
  it claims to be computed against. This is the same traceability
  discipline `aip-csm-builder`'s `ProvenanceGuard` already enforces one
  layer down (an element's provenance SHALL trace back to the Evidence
  that produced it), applied here to Analysis Results referencing CSM
  content instead.
- **Analyzer/version consistency** — an `AnalysisResult`'s declared
  producer SHALL correspond to a currently registered Analyzer.
- **Scope containment** — an `AnalysisResult`'s content SHALL stay
  within its Analyzer's declared Analysis Scope (Decision 7) — defense-
  in-depth against a buggy Analyzer silently overreaching what it
  claimed to operate on, the same spirit as `ExcludedRelationshipTypeGuard`
  in CSM Builder.

**Alternative considered:** no validation gate at all, trusting each
Analyzer to behave. Rejected — CSM Builder's own precedent throughout
`implement-csm-builder` (`ProvenanceGuard`, `ExcludedRelationshipTypeGuard`,
`CsmValidator`) is consistently defense-in-depth: guards exist
independent of whether today's implementations are careful, so a future
Analyzer author's mistake fails loudly at publish time rather than
silently producing untraceable or out-of-scope results.

### 6. Concurrency: contract permits it, v1 implementation stays sequential

**Problem:** CSM Builder chose sequential-only execution because Mapper
output accumulates into one order-sensitive result (a shared
`resolvedIds` map threaded through dispatch, relationship builders
consuming it afterward). Decision 2 above removes that reason for
Analyzers — each produces its own independent `AnalysisResult` from an
immutable `AnalysisView`, with no shared mutable accumulator between
them.

**Decision:** The Analyzer contract itself does not assume or require
sequential execution — an Analyzer receives an immutable `AnalysisView`
and returns an independent `AnalysisResult`, with no dependency on
execution order or on any other Analyzer having already run (Decision
2). This makes concurrent execution *safe by construction*, not merely
possible. The v1 `AnalysisOrchestrator` **implementation**, however,
starts with a simple sequential loop — the same "simplest thing that
satisfies the contract, no unjustified infrastructure" discipline
`implement-csm-builder`'s own `design.md` applied to Snapshot
persistence (its Decision 2) and concurrency (its Decision 4).
Introducing a concurrent executor later is a substitution behind the
same contract, not a redesign — exactly the escape hatch
`implement-csm-builder`'s own Decision 4 already named for itself,
inherited here rather than re-argued.

**Alternative considered:** commit to concurrent execution
infrastructure now, given `project.md` §8's Performance/Scalability
quality attributes. Rejected for v1 — real infrastructure (executor
management, error aggregation across threads, deterministic result
ordering for reproducibility) for a benefit not yet measured as
necessary. Nothing about this decision blocks introducing it once it
is.

### 7. Analysis Scope vocabulary and its relationship to Analyzer applicability

**Problem:** Explore resolved that every Analyzer declares an explicit
Analysis Scope (binding), but left the scope vocabulary itself, and how
multi-language/ecosystem-specific applicability interacts with it, open.

**Decision:** An Analysis Scope is a declared set of `CsmEntityKind`
and/or `CsmRelationshipType` values the Analyzer reads — e.g. a cycle
detector declares `{MODULE, DEPENDENCY}` — directly mirroring
`EvidenceKindMapper.supportedKind()`'s own declared-kind discipline,
generalized from a single kind to a set since one Analyzer typically
needs several kinds together. An Analyzer MAY additionally narrow its
scope to a specific containment anchor (a CSM element identity, or "the
whole repository" as the default) for Analyzers whose natural unit is
narrower than the full snapshot.

**Applicability** (whether a registered Analyzer runs at all for a
given `AnalysisView`) is a two-part, layered check:
1. **Kind-based applicability** (required): the snapshot contains at
   least one element/relationship of a kind in the Analyzer's declared
   scope. Mirrors the existing "no Mapper registered for this kind →
   skip, not an error" pattern from `MappingOrchestrator`.
2. **Native-attribute applicability** (optional, layered on top): an
   Analyzer meaningful only for a specific ecosystem (e.g. an
   npm-specific dependency-scope Analyzer) additionally predicates on
   `NativeAttributes` content already present on in-scope elements
   (e.g. a `buildSystem=npm` attribute) — resolving Explore's open
   question 6 as a *refinement* of kind-based scope, not a separate
   mechanism requiring its own registration/dispatch machinery.

This same declared-scope mechanism is also what Decision 6's
incremental-analysis architecture (Explore decision 5) keys off:
whichever CSM entity kinds/relationships changed between two snapshots
determines which Analyzers' declared scopes are affected and therefore
eligible for re-run — the same shape `implement-csm-builder`'s own
`ChangeStatus`-keyed incremental scoping used one layer down, without
this design needing to invent a second, unrelated incrementality
mechanism.

**Alternative considered:** scope expressed purely in terms of CSM
containment levels (Repository/Project/Module/.../Method). Rejected as
the primary mechanism — it fits Analyzers whose natural unit is a
container, but not one whose natural unit is a relationship type or a
cross-cutting subgraph (e.g. "all DEPENDENCY edges," which spans many
containers). Containment-anchor narrowing is kept as a *secondary,
optional* refinement (above) rather than the primary vocabulary.

**Alternative considered:** scope expressed purely in terms of CSM's
own `Subject` concept. Rejected as the primary mechanism — `Subject` is
deliberately fine-grained (one anchor + one assertion kind), which fits
an Analyzer reasoning about one fact at a time but not one reasoning
over a whole subgraph (cycle detection needs many subjects
simultaneously, not one). `AnalysisView` remains internally organized
by `Subject` (Decision 3); Analysis Scope is a coarser, kind-based
declaration over what portion of that view an Analyzer is entitled to
read.

## Risks / Trade-offs

- [Analyzer independence (Decision 2) means shared computations (e.g.
  dependency-graph traversal) may be recomputed by multiple Analyzers
  in the same run] → Mitigation: accepted v1 cost; the "Derived View"
  caching layer alternative is recorded and additive, not precluded, if
  this becomes a measured problem.
- [The new `aip-core` contract (`CsmSnapshotSource`, Decision 1) has no
  concrete implementation until a future change adapts
  `aip-csm-builder`'s `Snapshot` to satisfy it] → Mitigation: this is
  the same sequencing CSM Builder itself accepted for the Repository
  Evidence contract relative to a real RU implementation — the contract
  is real and specifiable now; a concrete adapter is
  `implement-analysis-framework`'s job, not a blocker to specifying
  this capability.
- [Deferring the concurrency implementation (Decision 6) could mean
  Analysis Framework's first real-world use is slower than it needs to
  be on large repositories] → Mitigation: the contract already permits
  concurrent execution without a breaking change; this is a deferred
  optimization, not a foreclosed one.
- [The Analysis Scope vocabulary (Decision 7) may prove too coarse once
  real Analyzers are written against it — e.g. an Analyzer needing
  finer-grained scope than any (kind-set, anchor) pair expresses] →
  Mitigation: the vocabulary is additive-extensible (a new scope
  dimension can be introduced later, the same way CSM's own vocabulary
  is versioned/extensible per `CSM Versioning and Evolution`); not
  discovering this until real Analyzers exist is an accepted risk of
  designing the framework before its first concrete Analyzer.
