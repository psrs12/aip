# Explore: Rule Framework

Status: preserved exploration notes carried into this change. Not a
tracked schema artifact — retained for traceability from
proposal/design decisions back to the reasoning that produced them,
following the same convention used by
`archive/2026-08-11-implement-csm-builder/explore.md` and
`define-analysis-framework/explore.md`.

## Problem Statement

Per `project.md` §11's capability evolution order, Rule Framework is
next after Analysis Framework (specified; not yet implemented). This
Explore covers what Rule Framework is responsible for, what it
consumes from Analysis Framework and the CSM, what it produces, and
where it lives architecturally — before any proposal commits to
requirements.

## Framing: module placement is easy this time, but CLAUDE.md's chain is now stale

Unlike Analysis Framework (which had no named module slot and forced a
naming-collision resolution), CLAUDE.md's module chain — `aip-core →
aip-analyzer → aip-rules → aip-ai → aip-cli/aip-server` — names
`aip-rules` explicitly. No collision to resolve here.

But the chain's *literal* reading (`aip-analyzer → aip-rules`, straight
line) is no longer accurate after `define-analysis-framework`'s own
Decision 1: `aip-analyzer` (the future Repository Understanding
implementation) and `aip-analysis` (this session's new module) are
**siblings**, both depending on `aip-core` only, neither on the other.
`aip-rules` almost certainly depends on `aip-analysis` (it needs
Analysis Results), not on `aip-analyzer` (which produces Repository
Evidence, a layer `aip-rules` has no reason to touch directly). The
real graph, as decisions accumulate, looks more like:

```
                              aip-core
                                 |
        +---------+---------+---+---+-----------------+
        |         |         |       |                 |
   aip-analyzer  aip-csm-builder  aip-analysis   (future consumers
   (RU impl,     (CSM Builder,    (this session's  read aip-core's
    not built)    implemented)    define; not      contracts
                                  implemented)      directly too)
                                       |
                                  aip-rules  <- this change
                                       |
                                    aip-ai
                                       |
                              aip-cli / aip-server
```

CLAUDE.md's own written chain text was never updated when
`define-analysis-framework` resolved this — worth flagging as a small
housekeeping item (not fixed here; CLAUDE.md is a project-instructions
file, not something an Explore should edit).

## A second, structurally identical pipeline-refinement tension

`canonical-software-model/spec.md`'s own `Preservation of Evidence,
Knowledge, Analysis, Findings, Recommendations, and Remediation as
Distinct Concepts` requirement names exactly six concepts in one flat
chain:

```
Evidence -> Knowledge -> Analysis -> Findings -> Recommendations -> Remediation
```

"Analysis" is defined there as "the evaluation of policy or heuristics
against CSM knowledge," and "Findings" as "the structured output of
Analysis" — its own scenario text says outright: *"WHEN a policy is
evaluated against a CSM Snapshot and produces a violation THEN the
resulting Finding SHALL be produced."* No intermediate concept is named
between "policy evaluated" and "Finding produced."

`project.md` §11 splits that single transition into **three**
capabilities: Analysis Framework → Rule Framework → Finding Model. This
is the exact same shape of tension already found and resolved (with
your confirmation) for Analysis Framework's own relationship to CSM's
"Analysis" definition — and I'd extend the same resolution pattern
here: **project.md's evolution order is refining CSM's single "Analysis
→ Findings" pipeline stage into three architectural layers, not
contradicting it.** Concretely, under that reading:

- **Analysis Framework** (already specified) — policy-independent
  computation over CSM knowledge, producing Analysis Results.
- **Rule Framework** (this change) — evaluates declared policy against
  Analysis Results (+ CSM's own effective/conflict-annotated
  knowledge), producing a pre-Finding evaluation outcome per (Rule,
  subject) — pass, fail, or not-applicable. Not yet severity-scored,
  not yet carrying a recommendation.
- **Finding Model** (future) — formalizes *failing* Rule Evaluation
  Results into the richer structure `project.md` §4.8 describes
  (severity, confidence, impact, recommendation, remediation
  availability...).

I'm carrying this forward as the working frame rather than a decided
fact — it's the same kind of interpretation that needed your explicit
confirmation last time, and I'd want the same here before treating it
as binding.

## What Rule Framework consumes

Grounded directly in what already exists (not invented for this
Explore):

- **`AnalysisView`** (`aip-core`) — `define-analysis-framework`
  Decision 3 explicitly designed this as a *general* capability,
  reusable by "every future consumer of CSM content (Rule Framework,
  a future CLI query surface, Agent Framework)," precisely so
  duplicating "what's the effective knowledge and conflict status for
  this subject" per consumer wasn't necessary. Rule Framework reading
  it directly isn't a new decision — it's cashing in a design choice
  already made with this consumer in mind.
- **`AnalysisResult`s** (`aip-analysis`) — for rules that need
  pre-computed structural analysis (a cycle detector's output, a
  coupling metric) rather than a single CSM fact.
- **Architectural Boundary relationships specifically** — CSM's own
  `Architectural Boundary Representation` requirement already commits
  to this: "Evaluating compliance with a boundary is a function of the
  Policy/Rule Model and SHALL NOT be performed by the CSM itself." A
  Boundary is CSM content (declared/inferred), reachable through the
  `AnalysisView` like any other Subject — this is a concrete,
  already-specified example of what one category of Rule looks like:
  "does an observed dependency violate a declared boundary."

Rule Framework does **not** consume Repository Evidence, Runtime
telemetry, or `aip-csm-builder`'s own `Snapshot`/`SnapshotStore` types
directly — the same boundary discipline `define-analysis-framework`
already established, extended one layer further.

```
CSM Snapshot -> CsmSnapshotSource -> AnalysisView -+-> Analyzers -> AnalysisResult -+
                                                    |                                |
                                                    +--------------------------------+--> Rule Framework -> ???
```

## What Rule Framework produces — genuinely open

Given the pipeline-refinement framing above, Rule Framework's output
is *not* a CSM element (barred by `Architecture Constraints Excluded
from the CSM` and `Findings are not stored in the CSM`) and *not yet*
a full `project.md` §4.8-shaped Finding (that richness — severity,
recommendation, remediation availability — is Finding Model's job,
which may also involve AI-assisted enrichment per Agent Framework).
What sits in between needs a name and a shape; this Explore doesn't
resolve it, only frames the question its Design phase needs to answer.

## Open architectural questions for Design

Unlike Analysis Framework's Explore, none of these have been through a
stakeholder-confirmation round yet — presenting them as genuinely open,
not pre-resolved:

1. **Policy-as-Code representation and authoring surface.** This is
   the single biggest fork. Analyzers (Analysis Framework) are
   AIP-authored Java code, registered by the platform itself, with no
   end-user authoring surface. But `project.md` §3.7 explicitly wants
   *organizations* to define policies ("Handlers must not access
   repositories directly," etc.) — implying Rules need to be
   authorable by someone who isn't writing and compiling Java against
   this codebase. Candidates:
   - A small, purpose-built DSL/predicate language over CSM vocabulary
     (entity kinds, relationship types, element identities — the exact
     terms `Architecture Constraints Excluded from the CSM` already
     names as what a policy is expressed in terms of).
   - A general-purpose rule/policy engine format (e.g. something
     Rego/OPA-shaped) adopted rather than invented.
   - A hybrid: a small number of AIP-authored, parameterized Rule
     *types* (like Analyzers) that organizations configure with
     arguments, rather than writing free-form logic — trading
     expressiveness for safety/simplicity and closer to Analyzer's own
     shape.
   - Rules-as-Java-code too, deferring true policy-as-code authoring to
     a later capability. Would be the simplest v1, but arguably
     contradicts `project.md` §3.7's own intent if taken as final
     rather than a deliberately narrow first cut.
2. **Rule-to-Analyzer/AnalysisResult relationship.** Can one Rule
   compose multiple Analysis Results and raw `AnalysisView` facts
   together (e.g. "no Module may depend on an External System without
   an approved integration boundary" needs both a dependency fact and
   a boundary-declaration fact)? If so, what does that composition
   look like, and does it require Analysis Results to be queryable by
   more than bare identity (e.g. "all Results from Analyzer X against
   this snapshot")?
3. **Rule Scope/applicability.** Does Rule Framework need its own
   scope-declaration mechanism mirroring Analyzer Scope, or does a
   Rule's own predicate (over CSM vocabulary) implicitly define its
   scope without a parallel declared-scope concept?
4. **Rule Evaluation Result identity, traceability, and durability.**
   Every precedent so far (CSM Snapshot, `AnalysisResult`) uses the
   same deterministic-identity + traceability + durable-store shape.
   Does Rule Framework's output follow the identical pattern (Rule
   identifier + version + source Analysis/CSM Snapshot identity +
   evaluated subject), or does something about policy evaluation argue
   for a different shape?
5. **Are passing evaluations recorded, or only violations?** CSM's own
   non-destructive-preservation principle (competing knowledge is
   always preserved, never silently dropped) suggests recording every
   outcome for auditability, not only failures — but that has real
   volume/storage implications a violations-only design wouldn't.
6. **Validation/publishing gate.** Does a Rule Evaluation Result need
   an analogous validator/publisher pair (referential integrity to its
   source Analysis Results/CSM Snapshot, Rule/version consistency), per
   the pattern `CsmValidator`/`SnapshotPublisher` and
   `AnalysisResultValidator`/`AnalysisResultPublisher` both already
   established?
7. **Module dependency shape.** Does `aip-rules` need its own
   `aip-core`-hosted read contract for Analysis Results (mirroring
   `CsmSnapshotSource`), so it never depends on `aip-analysis`'s own
   `AnalysisResultStore` implementation directly — repeating the exact
   reasoning `define-analysis-framework` Decision 1 already applied one
   layer down?

## Recommended Next Step

The framing questions (module placement, the pipeline-refinement
reading) are grounded enough to treat as working assumptions, but — per
how Analysis Framework's own equivalent tension was handled — the
"refinement, not contradiction" interpretation of CSM's Analysis→
Findings pipeline needs your explicit confirmation before Design builds
on it as settled. The seven open questions above are Design-phase
material once that's confirmed, with question 1 (policy authoring
surface) being the one most likely to reshape everything downstream if
answered differently than expected.
