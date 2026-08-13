# Explore: Analysis Framework

Status: preserved exploration notes carried into this change. Not a
tracked schema artifact — retained for traceability from
proposal/design decisions back to the reasoning that produced them,
following the same convention used by
`archive/2026-08-11-implement-csm-builder/explore.md`.

## Problem Statement

Per `project.md` §11's capability evolution order, Analysis Framework
is next after the Canonical Software Model (whose builder,
`implement-csm-builder`, is now implemented and merged). Nothing about
Analysis Framework has been specified yet. This Explore covers what the
capability is actually responsible for, where it lives architecturally,
and what it consumes/produces — before any proposal commits to
requirements.

## Framing: a naming collision surfaced first

Two orderings exist in this repository and do not map 1:1:

```
project.md §11 (CAPABILITY evolution):
  Software Repository Understanding → Canonical Software Model → Analysis Framework
      → Rule Framework → Finding Model → Agent Framework → Architecture Compliance Agent

CLAUDE.md (MODULE chain):
  aip-core → aip-analyzer → aip-rules → aip-ai → aip-cli/aip-server
```

`aip-analyzer` is **not** "the Analysis Framework module." The archived
`implement-csm-builder/design.md` (Decision 1) is explicit that
`aip-analyzer` is the future Repository Understanding implementation —
Language Analyzers, Build-System Detectors, Configuration Recognizers
that *produce* Repository Evidence. Its own dependency diagram shows CSM
snapshots "consumed by future `aip-rules`, etc.," skipping past any
Analysis Framework module entirely. CLAUDE.md's module chain, as
written, has no named home for this capability. **Resolved below.**

## Conceptual grounding, from the existing specs

The CSM spec's `Preservation of Evidence, Knowledge, Analysis,
Findings...` requirement defines the pipeline:

```
Evidence → Knowledge (CSM) → Analysis → Findings → Recommendations → Remediation
```

...and defines Analysis as "the evaluation of policy or heuristics
against CSM knowledge." Read literally that's circular with the
evolution order (Rule Framework, where policy-as-code lives, comes
*after* Analysis Framework) unless Analysis Framework is understood as
the policy-independent computation layer: a registry of pluggable
Analyzers (dependency-graph traversal, cycle detection, fan-in/fan-out,
coupling/cohesion metrics, layer/boundary checks) that produce
CSM-referencing Analysis Results, with Rule Framework later expressing
org-specific policy *in terms of* those results (+ raw CSM), and
Finding Model formalizing policy violations. This reading was proposed
during Explore and is treated as settled by the resolved decisions
below (see "Deterministic-only" and the `aip-analysis` module
decision).

The shape strongly echoes CSM Builder's own architecture:

```
CSM Builder                          Analysis Framework
────────────                         ──────────────────
Repository Evidence Model      →     CSM Snapshot (+ Analysis View)
EvidenceKindMapper (per kind)  →     Analyzer (per concern)
EvidenceKindMapperRegistry     →     AnalyzerRegistry
MappingOrchestrator            →     AnalysisOrchestrator / Runner
MappingResult                  →     Analysis Result
SnapshotStore                  →     Analysis Result store (concept resolved;
                                        persistence technology deferred)
```

This echo is useful as a reference point but is *not* assumed to mean
"copy CSM Builder's architecture mechanically" — see the open questions
below on where the two shapes plausibly diverge (analyzer
interdependencies in particular).

## Resolved Directional Decisions

**Status: stakeholder-confirmed, binding on this change.** These settle
scope/boundary questions raised during Explore; they are not full
designs — mechanism-level detail (exact interfaces, storage technology,
scope taxonomy) is Design-phase work.

1. **Dedicated `aip-analysis` module.** Resolves the naming collision
   above. `aip-analysis` sits between CSM output and (future)
   `aip-rules` in the dependency graph, depending on `aip-core` (and,
   for reading persisted CSM snapshots, on whatever module/interface
   exposes them — see Open Questions). CLAUDE.md's module chain is
   understood to have a gap here that this decision fills, not a chain
   this change reinterprets away.

2. **Analysis Results are first-class durable analytical artifacts —
   conceptually.** Like a CSM Snapshot, an Analysis Result is treated
   as an identifiable, individually retrievable artifact of a run, not
   a value that only ever exists transiently in memory during one
   request. **Persistence technology is explicitly deferred** to
   Design (mirroring `implement-csm-builder` Decision 2's own
   sequencing: resolve the concept and its obligations now, pick
   file-backed/embedded-DB/other later against those obligations).

3. **Analyzers consume an "Analysis View."** Not raw CSM content
   directly, and not "effective knowledge only" either: an Analysis
   View bundles a subject's effective knowledge (per CSM's own
   `Effective Knowledge and Precedence`) *together with* its conflict
   status (`EFFECTIVE`/`CONFLICTED`, per `Same-Category Conflict
   Marking and Resolution` — already implemented in `aip-core` as
   `EffectiveKnowledgeStatus`/`SubjectConflictMarker`, currently
   under-exercised since CSM Builder only produces `observed` content).
   This lets an Analyzer choose to abstain, flag reduced confidence, or
   otherwise react to conflict — rather than silently reasoning over a
   precedence-resolved value with the conflict information thrown away
   before it ever reaches the Analyzer.

4. **Deterministic-only in v1 — no AI/LLM surface.** Mirrors CSM
   Builder's own "Deterministic, Evidence-Driven Transformation Only"
   invariant. AI involvement (reasoning, explanation, recommendation,
   prioritization per CLAUDE.md's "Deterministic Analysis Before AI")
   is deferred to a later capability (Agent Framework, or
   Recommendations at the Finding Model layer) — never inside an
   Analyzer itself in this version.

5. **Incremental analysis: architecturally supported, sophisticated
   invalidation deferred.** The framework's shape must not preclude
   scoping re-analysis to what actually changed (mirroring CSM
   Builder's own `Incremental Snapshot Scope`), but v1 is not required
   to implement fine-grained, dependency-aware invalidation across
   Analyzers. A coarser mechanism (e.g., re-run whatever an Analyzer's
   declared scope overlaps with a changed CSM region) is acceptable for
   v1; a more precise invalidation graph is future work.

6. **v1 analysis is single-repository.** Matches the CSM's own
   `Multi-Repository Scope` requirement (CSM is scoped to a single
   repository; cross-repository facts appear only as External System
   boundary references). Analysis Framework inherits that same
   boundary for v1 — no cross-repository analysis capability yet.

7. **Analyzers declare an explicit Analysis Scope.** Every registered
   Analyzer states, declaratively, what portion/shape of the CSM it
   operates over — not implicitly inferred from what it happens to
   query at runtime. This is what decision 5's incremental-analysis
   architecture and any future dependency-between-analyzers ordering
   would key off. **The concrete scope vocabulary (what scope levels
   exist — whole-repository, per-Module, per-Component, per-subject,
   something else) is an open question, not resolved here — see below.**

## Analyzer Scope — resolved principle, open mechanism

- **Resolved:** Analyzer Scope exists as a first-class, declared
  concept every Analyzer states explicitly (decision 7 above). This is
  binding.
- **Open:** the scope vocabulary itself. Candidates, not yet chosen:
  - Mirror CSM containment levels directly (Repository / Project /
    Module / Package / Type\[/Method\]) — simple, but may not fit an
    Analyzer whose natural unit is a *subject* (e.g. one dependency
    edge) or a *relationship type* rather than a containment level.
  - A set-of-CSM-entity-kinds-and-relationship-types declaration (e.g.
    "this Analyzer reads MODULE nodes and DEPENDENCY edges") — finer
    grained, more directly useful for incremental invalidation, but
    more machinery to define and enforce.
  - Something scoped to Subject (reusing CSM's own `Subject` concept
    from `Subject Identification for Knowledge Assertions`) — would
    unify Analyzer Scope with the same equivalence rule CSM's own
    conflict/precedence machinery already uses, but subjects may be too
    fine-grained for analyzers reasoning over whole subgraphs (e.g.
    cycle detection).

This needs a concrete decision at Design time; Explore surfaces the
candidates without picking one.

## Remaining Architectural Questions — carried to Design, not resolved here

These don't change the scope/boundary decisions above, but need
resolution before Specification can be written precisely:

1. **Analysis Result shape and traceability.** Does an Analysis Result
   carry something provenance-like (which CSM Snapshot identity it was
   computed against, which Analyzer + Analyzer version produced it —
   mirroring CSM Builder's `MapperAttribution`/Snapshot Manifest), so a
   Finding built from it later remains traceable back through the
   Analysis Result to the CSM elements involved? The CSM spec's own
   `Structured Provenance Record` expectations don't directly apply to
   Analysis Results (they're not CSM knowledge), but an analogous
   traceability discipline seems necessary for AIP's Explainability
   principle (`project.md` §3.3) to hold at this layer too.

2. **Analyzer interdependencies.** CSM Builder's Evidence-Kind Mappers
   are deliberately independent — no Mapper depends on another Mapper's
   output, only on the Evidence Model itself (Section 5's identity
   derivation is explicitly designed to keep them order-independent).
   Analyzers plausibly are **not** independent of each other: a cycle
   detector might reasonably build on a dependency-graph Analyzer's
   output rather than recomputing traversal itself. Does Analysis
   Framework need a declared Analyzer dependency graph (with
   topological execution order), or does every Analyzer read only from
   the Analysis View (CSM + conflict status) and never from another
   Analyzer's Result, keeping the CSM-Builder-style independence and
   pushing any composition into Rule Framework instead? This materially
   affects the orchestrator's shape and is worth settling early.

3. **Concurrency/parallelism.** CSM Builder deliberately chose
   sequential-only execution (Decision 4) because Mapper output
   accumulates into one deterministic, order-sensitive result.
   Analyzers producing independent Results for independent subjects
   look more like an embarrassingly parallel problem — does Analysis
   Framework inherit CSM Builder's "no concurrency infrastructure"
   discipline by default, or is this the first capability where
   parallel execution is worth deliberately introducing (given
   `project.md` §8's Performance/Scalability quality attributes)? Not
   resolved here; flagged since it's a real point of divergence from
   the CSM Builder precedent, not an obvious "just copy the pattern"
   answer.

4. **Where does Analysis View construction live?** Building an
   Analysis View requires applying `aip-core`'s
   `SubjectConflictMarker`/`EffectiveKnowledgeStatus` machinery to a
   CSM Snapshot's content. Is that `aip-analysis`'s own responsibility
   (reading directly from wherever CSM snapshots are persisted), or
   does it belong to a thin, shared boundary consumed by both
   `aip-analysis` and any future consumer that also needs an effective
   view (e.g. a future CLI/query surface)? Related to decision 1
   (module placement) but not fully settled by it.

5. **Validation gate for Analysis Results.** CSM Builder gates
   publication with `CsmValidator`/`SnapshotPublisher` (an invalid
   snapshot is never persisted). Does Analysis Framework need an
   analogous gate for Analysis Results, or is that unnecessary given
   decision 4 (deterministic-only, no probabilistic content to
   validate against a confidence/vocabulary checklist the way CSM
   content is)?

6. **Multi-language interaction with Analyzer Scope.** Some Analyzers
   may only be meaningful for certain native construct kinds or
   ecosystems (e.g. an npm-specific dependency-scope analyzer). Is that
   expressed through the same Analyzer Scope mechanism (open question
   above), or is it a separate applicability concept layered on top?

## Recommended Next Step

The scope/boundary questions that most needed stakeholder judgment —
module placement, Analysis Result durability, what an Analyzer
consumes, AI's role, incrementality's ambition, repository scope, and
whether Analyzer Scope is a first-class concept — are now resolved and
binding. The six remaining architectural questions above are real but
are Design-phase decisions in the same sense CSM Builder's own deferred
snapshot-persistence-mechanism and concurrency questions were: they
need answers before Design is *finished*, not before Proposal can
start. Ready to move to Proposal when you are.
