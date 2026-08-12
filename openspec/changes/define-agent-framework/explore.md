# Explore: Agent Framework

Status: preserved exploration notes carried into this change. Not a
tracked schema artifact — retained for traceability from
proposal/design decisions back to the reasoning that produced them,
following the same convention used by
`archive/2026-08-11-implement-csm-builder/explore.md`,
`define-analysis-framework/explore.md`, `define-rule-framework/explore.md`,
and `define-finding-model/explore.md`.

## Problem Statement

Per `project.md` §11's evolution order, Agent Framework is next after
Finding Model (specified; not yet implemented):

```text
CSM Snapshot -> Analysis Framework -> AnalysisView / AnalysisResults
             -> Rule Framework -> RuleEvaluationResults
             -> Finding Model -> Findings
             -> Agent Framework -> ???
             -> Architecture Compliance Agent (separate, later capability)
```

Every capability so far has been deliberately, explicitly deterministic
and AI-free — Analysis Framework, Rule Framework, and Finding Model
each independently reached the same conclusion (no AI/LLM surface in
v1) for the same reason: `project.md` §3.2's Deterministic-Analysis-
Before-AI principle. Agent Framework is different in kind, not just in
position: it is the first capability in this entire evolution order
whose job is specifically to introduce AI reasoning. This Explore
covers what Agent Framework is responsible for, what it consumes, what
it produces, and where the established patterns (deterministic
identity, immutable artifacts, contract ownership in `aip-core`,
validate-before-publish gates) hold, bend, or genuinely break.

## Framing: this is the pipeline's named AI boundary, not an ordinary next layer

### `project.md` §6's AI Philosophy pipeline, read literally this time

```text
Facts -> Analysis -> Rules -> Findings -> AI Reasoning -> Recommendations
```

Every previous Explore treated this pipeline as background confirmation
that Finding Model stays pre-AI. Agent Framework is the layer this
pipeline names explicitly: **AI Reasoning**, producing
**Recommendations**. Unlike the "Analysis -> Findings" collapse CSM's
archived text made (later refined across three capabilities), this
text already separates "AI Reasoning" from "Recommendations" as two
concepts — worth carrying forward as a candidate two-artifact or
two-step shape (an Agent *reasons*, producing a *Recommendation*),
mirroring this system's own repeated pattern of one concept becoming
one new durable artifact per layer (CSM Snapshot -> AnalysisResult ->
RuleEvaluationResult -> Finding), rather than assuming "AI Reasoning"
is just an internal step inside Recommendation construction with no
significance of its own. Not decided here — a genuine question for
Design.

### `canonical-software-model`'s own Recommendation text — already grounds this capability's output shape

`canonical-software-model/spec.md`'s `Preservation of Evidence,
Knowledge, Analysis, Findings, Recommendations, and Remediation as
Distinct Concepts` requirement already names Recommendations directly
(not something this Explore is inferring): **"Recommendations (AI- or
rule-generated guidance attached to a Finding)"** — and its own
scenario text: *"a Recommendation or Remediation proposal... SHALL
reference the relevant CSM elements by identity and SHALL NOT silently
modify CSM knowledge as a side effect of being generated."* Two things
worth pulling out explicitly:

- **"attached to a Finding"** — the same relationship shape every
  prior layer has used (a new artifact *referencing* its input, never
  mutating it). This is direct textual support for treating
  Recommendation as its own distinct, Finding-referencing artifact,
  continuing the pattern rather than reopening how Finding Model
  already committed to Finding's own immutability.
- **"AI- or rule-generated"** — CSM's own text does not assume every
  Recommendation is AI-produced. A rule-generated Recommendation (e.g.
  a Rule Type declaring static remediation guidance as part of its own
  configuration, the same way it already declares Severity/Category
  per `define-finding-model`'s Decision 5) is explicitly permitted by
  this text. Whether Agent Framework's v1 scope covers only
  AI-generated Recommendations, or also a simpler rule-generated path,
  is a genuine open question — not resolved here.

### `define-finding-model`'s own design.md already named this dependency

Not invented for this Explore: `define-finding-model/design.md`'s
Cross-Capability Impacts section states directly: *"Agent Framework
(future) is now known to depend on: Finding's Logical Finding Identity
and Evaluation Identity (for any lifecycle/prioritization work it
performs) and the deferred `Recommendation`/`Remediation availability`
fields it will be the first capability to actually populate."* This is
load-bearing, already-approved grounding: Agent Framework is expected
to consume Findings (both identities), and is expected to be the
capability that finally puts content into Finding's own
`Recommendation` and `Remediation availability` fields — which
Finding Model deliberately left unpopulated, not absent as a concept.
This raises an immediate, concrete question for Design: does Agent
Framework populate those fields **on the Finding itself** (which would
require Finding to become mutable, or a new Finding revision to be
constructed — both explicitly rejected by Finding Model's own Decision
4), or does it produce a **separate Recommendation artifact** that
*references* a Finding's identity, leaving Finding itself permanently
immutable as designed? The "attached to a Finding" language above
argues strongly for the latter — surfaced explicitly here as a likely
genuine tension between `project.md` §4.8's literal field list (which
reads as if Recommendation lives *on* Finding) and Finding Model's own
already-approved immutability discipline (which Agent Framework's
Design must not reopen).

## Framing: module placement is the most directly pre-named case yet

`CLAUDE.md`'s module chain (`aip-core -> aip-analyzer -> aip-rules ->
aip-ai -> aip-cli/aip-server`) names `aip-ai` immediately after
`aip-rules` — and unlike every prior module-placement question in this
project's history, `aip-ai` is not just adjacent, it is the one module
name in that chain whose purpose (AI-specific concerns) matches this
capability's own defining trait (the first AI-bearing layer) exactly.
The real graph so far:

```text
  aip-core
  - depended on directly by every deterministic module below

  aip-analyzer    - RU implementation, not yet built
  aip-csm-builder - CSM Builder, implemented
  aip-analysis    - Analysis Framework, not yet built
  aip-rules       - Rule Framework, not yet built
  aip-findings    - Finding Model, not yet built

  aip-analyzer, aip-csm-builder, aip-analysis, aip-rules, and
  aip-findings are siblings - none depends on any of the others;
  all are deterministic, AI-free

  aip-ai          - Agent Framework, this change - the first AI-bearing
                    module; plausibly depends on aip-core (for CSM/
                    Finding contracts) but SHALL NOT be a dependency of
                    any of the five deterministic siblings above

  aip-cli / aip-server - future, depends on the above
```

Whether `aip-ai` depends on `aip-core` only (continuing the established
discipline) or genuinely needs something broader is a real question —
see "What Agent Framework consumes," below — but the module *name and
position* are unusually well-grounded already, unlike the naming
struggles Analysis Framework's and Rule Framework's own Explores had
to work through.

## What Agent Framework consumes — the first place the established "one layer down only" contract pattern may not cleanly apply

Every prior consumption boundary in this project has been narrow and
single-layer: an Analyzer reads only `AnalysisView`; a Rule Type reads
only `AnalysisView` plus its declared Analyzers' `AnalysisResult`s;
Finding Model reads only `RuleEvaluationResult`s (via
`RuleEvaluationResultSource`), explicitly not `AnalysisView` directly
(confirmed, not just assumed, during `define-finding-model`'s own
Review). The obvious continuation would be: an Agent reads only
`Finding`s, via a new `FindingSource` contract in `aip-core` —
extending the by-now four-times-repeated `CsmSnapshotSource` /
`AnalysisResultSource` / `RuleEvaluationResultSource` pattern one more
layer.

But `CLAUDE.md`'s own AI Agent Architecture section says: *"Agents
should consume shared AIP models and analysis results"* — plural,
general, not narrowly "Findings only." And a genuine practical
argument supports this: a good AI-generated Recommendation plausibly
needs more context than a Finding's own already-summarized `Evidence`
field carries (e.g., surrounding CSM structure, the full
`RuleEvaluationResult` payload, possibly `AnalysisView` content) to
produce a well-grounded explanation rather than a shallow
restatement of the Finding itself. This is a genuine, not
manufactured, tension worth carrying into Design rather than resolving
here: does Agent Framework read **only Findings** (narrowest,
most consistent with established discipline), or does it read
**Findings plus the same established contracts every prior layer
already exposes** (`AnalysisView`, `AnalysisResultSource`,
`RuleEvaluationResultSource` — all already in or reachable from
`aip-core`, so no *new* boundary violation, just broader read access
than any single prior layer needed)? The latter would still honor
every existing dependency-direction rule (`aip-ai` -> `aip-core` only,
never a producer module directly) — it would just mean Agent Framework
is the first consumer to legitimately need more than one of
`aip-core`'s established read contracts at once.

## The central tension: non-determinism meets a system built entirely on determinism

Every identity scheme in this system — CSM element/relationship
identity, `AnalysisResult` identity, `RuleEvaluationResult` identity,
Finding's own Evaluation Identity and Logical Finding Identity — is
deterministic by explicit, repeated design choice, each one verified
with a "repeated construction over unchanged input is identical"
scenario. An AI/LLM call is not, in general, deterministic: the same
prompt against the same model can legitimately produce different text
on different invocations (temperature, sampling, model version drift
between runs). This is the first genuine, structural collision between
an established project-wide invariant and what this capability's own
job requires — not a "can we make this deterministic too" question
(the answer is plainly no, that's what distinguishes AI reasoning from
deterministic analysis in `project.md` §3.2's own framing) but a
"what does identity, traceability, and reproducibility even mean for a
Recommendation" question. Candidates worth naming, not deciding:

- **Identity keyed on inputs, not output content** — a Recommendation's
  identity is a deterministic function of (Agent identifier, Agent
  version, referenced Finding identity/identities, and perhaps a
  request/prompt-template identifier), *not* of the generated text
  itself. Two invocations against the same inputs would then produce
  two *different* Recommendations (different generated content) but
  the identity scheme itself stays deterministic and collision-free —
  mirroring how `RuleEvaluationResult`'s identity already excludes its
  own opaque payload content from the identity computation. This
  would need an explicit answer to "is a re-run's new Recommendation a
  new artifact alongside the old one, or does something need to pick a
  canonical one" — an ordering/precedence question this system has
  handled before (CSM's own `SubjectConflictMarker`) for a different
  reason (competing knowledge), possibly reusable for a related but
  distinct reason (competing AI outputs).
- **No identity determinism guarantee for Recommendation content
  itself, only for its traceable inputs** — accept that "identical
  inputs -> identical output" simply does not hold at this layer, and
  make that an explicit, named departure from precedent rather than a
  silently broken invariant.

This tension deserves the same weight Finding Model's own identity
tension received in its own Explore — flagged here as the single
question most likely to shape everything else Design resolves for
this capability.

## Human-Controlled Remediation — where this capability's output boundary stops

`project.md` §3.5 states plainly: *"AIP may recommend or generate
remediation, but autonomous modification of production code shall not
be the default behavior,"* with an explicit workflow: `Finding ->
Recommendation -> Proposed Change -> Human Approval -> Implementation
-> Tests -> Verification`. `project.md` §11's evolution order itself
stops at `Architecture Compliance Agent` — it does not name a
"Remediation" capability at all, and `project.md` §4.10 describes
Remediation as something AIP "should eventually support," not
something this generation of capabilities builds. This grounds a
strong candidate output boundary: **Agent Framework's v1 scope stops
at Recommendation; "Proposed Change," human approval workflow,
implementation, and verification are out of scope**, reserved for a
capability `project.md` does not yet name. Not pre-decided as binding
here, but strongly supported by both the evolution order's own
stopping point and `project.md` §7's explicit Non-Goal: *"Treat
AI-generated recommendations as unquestionable truth."*

## What "Agent Framework" itself means, vs. any specific agent

`project.md` §5 names two-dozen-plus potential agents (Architecture
Compliance Agent, Design Quality Agent, Security Architecture Agent,
Technical Debt Agent...) grouped into five categories, but also states
directly: *"The initial implementation shall focus on establishing the
platform and agent framework before implementing a large number of
specialized agents."* `project.md` §11's own evolution order reinforces
this: `Agent Framework` is its own step, with `Architecture Compliance
Agent` — the *first* concrete agent — named as a distinct, later step
after it. This is the same shape every prior capability pair has
already established: `Analysis Framework` (the generic contract) before
any concrete Analyzer; `Rule Framework` (Rule Type contract) before any
concrete Rule Type. By the same pattern, **this change specifies the
generic Agent contract, registry, consumption/production boundary, and
identity/traceability/validation model — not the Architecture
Compliance Agent itself, and not any specific agent's reasoning
logic.** Carried forward as the working frame, consistent with every
prior round's own Explore, needing the same explicit confirmation
before Design builds on it.

CLAUDE.md's own AI Agent Architecture section adds one more constraint
already stated, not invented here: *"Agents should not independently
redefine the meaning of the software system"* — i.e., whatever an
Agent consumes (Findings, and possibly the broader contracts named
above), it reads that content as already-authoritative, the same
non-negotiable-input posture every prior layer has held toward its own
input.

## Open architectural questions for Design

None of these have been through a stakeholder-confirmation round yet.

1. **What does an Agent produce — a Recommendation, or something
   richer?** Is Recommendation Agent Framework's sole output artifact,
   or does "AI Reasoning" (per `project.md` §6) deserve its own
   intermediate representation before Recommendation? Leaning toward
   "Recommendation is the one new artifact," mirroring this system's
   one-new-artifact-per-layer pattern, but not decided.
2. **Does a Recommendation live on the Finding, or reference it as a
   separate artifact?** The central tension named above under CSM's
   own "attached to a Finding" text vs. Finding Model's already-approved
   immutability. Strong candidate: separate, Finding-referencing
   artifact — not decided.
3. **Identity and reproducibility for a non-deterministic artifact.**
   The central tension of this Explore. Candidates named above
   (input-keyed identity vs. an explicit, named departure from the
   deterministic-identity precedent) — not decided.
4. **Consumption boundary — Findings only, or Findings plus the
   broader established `aip-core` contracts?** Named above; a real,
   not manufactured, tension between narrowest-consistent-with-
   precedent and CLAUDE.md's own "shared AIP models and analysis
   results" language.
5. **Does Agent Framework need conflict/precedence handling for
   multiple Recommendations?** If more than one Agent (or one Agent
   re-run) can legitimately produce more than one Recommendation for
   the same Finding, does Design need a mechanism analogous to CSM's
   own `SubjectConflictMarker`, or is "many Recommendations per
   Finding, no forced precedence" acceptable given Recommendations are
   guidance, not authoritative facts?
6. **Confidence — finally meaningful, but computed how?** Every prior
   layer either had no Confidence concept or fixed it at a maximal
   constant (Finding Model's own Decision 5, explicitly because
   nothing upstream of it was non-deterministic). Agent Framework is
   the first layer where a genuinely-variable Confidence value has an
   honest source. Does the Agent contract require a declared Confidence
   value per Recommendation, and if so, sourced from the model's own
   signal, a heuristic, or left to each Agent's own discretion within
   a required range?
7. **Validation/publishing gate — same shape, different checks.** Every
   prior layer has one; the mechanical shape almost certainly carries
   over (a `RecommendationValidator`/`RecommendationPublisher`, or
   equivalent), but what does validation check for AI-generated content
   specifically — referential integrity to the Finding it concerns
   (certainly), but also something about not silently altering facts
   (`project.md` §6: *"AI should not silently change facts, analysis
   results, or policy decisions"*)? What would a validator actually
   check to enforce that, mechanically?
8. **Module dependency shape.** Almost certainly `aip-ai`, depending on
   `aip-core` only (per every established precedent) — but *how much*
   of `aip-core`'s established read-contract surface it depends on is
   open (see question 4). Does `aip-core` need a new `FindingSource`
   contract mirroring `RuleEvaluationResultSource`, continuing the
   pattern a fifth time?
9. **Output boundary — does this capability stop at Recommendation?**
   Strong candidate per the Human-Controlled Remediation framing above
   (Proposed Change / implementation / verification reserved for a
   later, unnamed capability) — not decided.
10. **AI-specific content isolation.** `project.md` §8's Security
    quality attribute and `CLAUDE.md`'s own CSM boundary text both
    already establish that AI prompts and AI-specific implementation
    details must not leak into deterministic layers. Does this design
    need an explicit requirement stating prompts/model-call details
    never appear in `aip-core`, in a Finding, or in any deterministic
    artifact's own fields — keeping this discipline enforced from the
    Agent Framework side now that an actual AI-bearing module exists to
    isolate content into?
11. **Determinism discipline for the Agent *contract* itself, distinct
    from Agent *output*.** Every prior contract (Analyzer, Rule Type)
    is itself ordinary, deterministic, versioned code — only its
    *declared inputs* vary. Does the Agent contract (identifier,
    version, declared consumed content, registration) stay exactly
    this same shape, with only the *body* of what an Agent does
    (an AI/LLM call) being the genuinely new, non-deterministic part?
    Likely yes — worth confirming explicitly rather than assuming
    non-determinism spreads to the contract shape itself.

## Recommended Next Step

The framing (module name/position, the "generic framework, not a
specific agent" scope boundary, the Human-Controlled-Remediation
output-boundary reading) is grounded enough to carry forward as
working assumptions, but per the established pattern, needs your
explicit confirmation before Design builds on it. Of the eleven
questions, **question 3 (identity/reproducibility for non-deterministic
output)** is this Explore's equivalent of Finding Model's own Logical-
Identity tension — the single question most likely to reshape
everything else if answered differently than the candidates above
suggest, because unlike every prior tension in this project's history,
it is not resolvable by finding an already-established fact to lean
on (the way Finding Model's tension was resolved by CSM element
identity's own pre-existing snapshot-independence) — it requires an
actual new judgment call about what "identity" means for content this
system has never had to identify before.
