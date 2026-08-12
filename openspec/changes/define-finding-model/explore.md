# Explore: Finding Model

Status: preserved exploration notes carried into this change. Not a
tracked schema artifact — retained for traceability from
proposal/design decisions back to the reasoning that produced them,
following the same convention used by
`archive/2026-08-11-implement-csm-builder/explore.md`,
`define-analysis-framework/explore.md`, and
`define-rule-framework/explore.md`.

## Problem Statement

Per `project.md` §11's evolution order, Finding Model is next after
Rule Framework (specified; not yet implemented):

```text
CSM Snapshot -> Analysis Framework -> AnalysisView / AnalysisResults
             -> Rule Framework -> RuleEvaluationResults
             -> Finding Model -> ???
```

Rule Framework produces `RuleEvaluationResult`s — durable,
deterministic, individually-identifiable evaluation-run artifacts
carrying `PASS`/`FAIL`/`NOT_APPLICABLE`. `project.md` §4.8 separately
names a `Finding` shape (`Finding ID, Rule ID, Category, Severity,
Confidence, Location, Evidence, Description, Impact, Recommendation,
Remediation availability`). This Explore investigates whether these
are the same thing wearing two names, or two genuinely distinct
artifacts — and if the latter, what separates them.

**Working hypothesis, stated explicitly per your instruction not to
assume `FAIL == Finding`:** `RuleEvaluationResult` is Rule Framework's
own internal evaluation-run record — produced for every outcome
(including `PASS`/`NOT_APPLICABLE`), for audit and traceability.
`Finding` is a higher-level, consumer-facing interpretation of that
record — the thing a developer, architect, or CI pipeline actually
sees and acts on. If that's right, Finding Model's job is not a
rename; it's a **promotion-plus-interpretation** step: filtering
(which outcomes become Findings), enrichment (adding content
`RuleEvaluationResult`'s opaque payload doesn't carry — `Category`,
`Description`, `Impact`), and possibly aggregation. This is carried
forward as a hypothesis, not a decision — Design needs to confirm or
refute it against each of the 14 questions below.

## Framing: module placement, and a third instance of the pipeline-refinement tension

### Module placement

`CLAUDE.md`'s module chain (`aip-core -> aip-analyzer -> aip-rules ->
aip-ai -> aip-cli/aip-server`) places `aip-ai` immediately after
`aip-rules`, with no named slot for Finding Model. Its literal reading
is already known-stale (per `define-analysis-framework`'s and
`define-rule-framework`'s own Explore phases — `aip-analyzer` and
`aip-analysis` are siblings, not a chain link). The real graph so far:

```text
  aip-core
  - depended on directly by every module below; depends on none of them

  aip-analyzer   - RU implementation, not yet built     (depends on aip-core only)
  aip-csm-builder - CSM Builder, implemented            (depends on aip-core only)
  aip-analysis   - Analysis Framework, not yet built     (depends on aip-core only)
  aip-rules      - Rule Framework, not yet built         (depends on aip-core only)

  aip-analyzer, aip-csm-builder, aip-analysis, and aip-rules are
  siblings - none depends on any of the others

  Finding Model  - this change, module placement still open
  aip-ai (?)     - future, depends on the above (not designed here)
  aip-cli / aip-server - future, depends on the above (not designed here)
```

Whether Finding Model is its own module (mirroring the
one-capability-one-module discipline `aip-csm-builder`/`aip-analysis`/
`aip-rules` each already established) or something else is a real
question (#12 below) — not pre-resolved here.

### The pipeline-refinement tension, a third time

`canonical-software-model/spec.md`'s `Preservation of Evidence,
Knowledge, Analysis, Findings, Recommendations, and Remediation as
Distinct Concepts` requirement names Findings as **"the structured
output of Analysis"** — a direct, flat transition, with Rule Framework
not named as an intermediate step (unsurprising; that requirement
predates Rule Framework's own definition). Its own scenario text: *"WHEN
a policy is evaluated against a CSM snapshot and produces a violation
THEN the resulting Finding SHALL be produced..."* — again collapsing
"policy evaluated" directly to "Finding produced," with nothing named
in between.

This is the same shape of tension already found and resolved (with
your explicit confirmation each time) for Analysis Framework's and
Rule Framework's own relationships to this same archived text. The
same reading extends cleanly a third time: **`project.md`'s evolution
order refines CSM's single "policy evaluated -> Finding produced"
transition into two architectural layers (Rule Framework, then Finding
Model), not contradicting it.** Concretely:

- **Rule Framework** (specified) — evaluates declared policy,
  producing `RuleEvaluationResult`s: pass, fail, or not-applicable,
  for every applicable Rule Scope instance, undiscriminated.
- **Finding Model** (this change) — the actual "structured output of
  Analysis" CSM's text describes: the artifact a consumer reads,
  carrying `project.md` §4.8's richer shape.

Carried forward as the working frame, not a decided fact — needing
your explicit confirmation before Design builds on it, per the same
pattern as the prior two rounds.

One more textual data point worth surfacing directly: CSM's own
`Preservation of...` requirement describes Recommendations as **"AI-
or rule-generated guidance attached to a Finding"** (emphasis on
"or") — it does not say Recommendations are exclusively AI-authored.
This bears directly on question 7 below.

### `project.md` §6's AI Philosophy pipeline — a distinct, narrower vocabulary worth reading carefully

```text
Facts -> Analysis -> Rules -> Findings -> AI Reasoning -> Recommendations
```

This is a *third* pipeline vocabulary in this project (distinct from
both CSM's six-concept chain and `project.md` §11's seven-capability
evolution order), but it's the most directly relevant one here: it
places **Findings before AI Reasoning**, not after or intermingled with
it. Read plainly, this argues that Finding Model itself should stay on
the deterministic side of the AI boundary `project.md` §3.2 draws
("AIP should avoid using an LLM as a substitute for deterministic
analysis") — Findings are produced by Rules (deterministic), and only
*then* does AI Reasoning operate on them to produce Recommendations.
If that reading holds, Finding Model is architecturally a peer of Rule
Framework (deterministic, `aip-core`-contract-based, no AI surface),
not a peer of `aip-ai`. This is a candidate answer to question 12, not
a settled one — flagged here because it's grounded directly in
`project.md` text, not invented for this Explore.

## What Finding Model consumes — grounded in what already exists

- **`RuleEvaluationResult`s** (`aip-rules`, per `define-rule-framework`)
  — the immediate, obvious input. Already carries: producing Rule
  identifier/version, source CSM Snapshot identity, Rule Scope
  instance, consumed `AnalysisResult` identity set, an opaque
  Rule-Type-defined payload, and a `PASS`/`FAIL`/`NOT_APPLICABLE`
  outcome — durable and individually retrievable via
  `RuleEvaluationResultStore`.
- **Transitively, `AnalysisResult`s and `AnalysisView` content** — not
  necessarily read directly by Finding Model (see question 9); already
  reachable through `RuleEvaluationResult`'s own traceability chain,
  which `define-rule-framework/design.md` Decision 4 itself names
  explicitly: *"(future) Finding -> Rule Evaluation Result ->
  AnalysisResults -> AnalysisView Subjects -> CSM elements ->
  originating Evidence."* Finding Model is not inventing this chain —
  it's the layer that already-written sentence was written to
  anticipate.
- **Not** Repository Evidence, Runtime telemetry, or any producing
  module directly — the same boundary discipline every prior layer
  (`aip-csm-builder`, `aip-analysis`, `aip-rules`) has held, extended
  one layer further.

## Open architectural questions for Design

None of these have been through a stakeholder-confirmation round yet.

### 1. What constitutes a Finding?

`project.md` §4.8 gives a concrete shape: `Finding ID, Rule ID,
Category, Severity, Confidence, Location, Evidence, Description,
Impact, Recommendation, Remediation availability`. Several of these
fields raise their own sub-questions (Severity — question 7;
Confidence — see below; Recommendation — question 7; Evidence —
question 8), but the shape itself is a solid starting point, already
specified at the product level, not invented here.

One genuine tension: `Confidence` is listed as a Finding field, but
`RuleEvaluationResult` carries no confidence concept — Rule evaluation
is deterministic (`Deterministic, Declarative Rule Evaluation Only`),
and CSM's own Confidence representation is explicitly reserved for
*inferred* knowledge, not evaluation outcomes. Where does a Finding's
Confidence value come from if its originating `RuleEvaluationResult`
is always "certain" by construction? Candidates: (a) Confidence is
always a fixed/maximal value for v1, deterministic Rule-derived
Findings, becoming meaningful only once Agent Framework introduces
AI-assisted or inference-based Findings; (b) Confidence doesn't belong
in v1's Finding shape at all, deferred like other `project.md` §4.8
fields already deferred elsewhere. Genuinely open.

### 2. Relationship between Finding and RuleEvaluationResult

Is a Finding a distinct artifact that *references* one or more
`RuleEvaluationResult`s (mirroring how `RuleEvaluationResult`
references, but is distinct from, the `AnalysisResult`s it consumes),
or is "Finding" simply the name given to a `RuleEvaluationResult` once
published (no new artifact type, just a `FAIL` result exposed under
new vocabulary)? The precedent set by every prior layer transition
(CSM Snapshot -> AnalysisView/AnalysisResult -> RuleEvaluationResult)
has always introduced a genuinely new, identity-extended artifact
rather than relabeling the prior one — arguing for the former. Not
pre-decided here.

### 3. One Finding from one or many RuleEvaluationResults?

A realistic governance scenario: the same architectural violation
(e.g. "Module X depends on External System Y without an approved
boundary") might be evaluated per-element by an anchored Rule Scope,
producing many `RuleEvaluationResult`s that a consumer would want
grouped into one Finding ("12 violations of this boundary rule"),
or Finding Model might stay 1:1 with `RuleEvaluationResult`s and leave
grouping to a presentation layer entirely outside this capability's
scope. This is tightly coupled to question 10 (aggregation) — kept as
a separate question because "does the *relationship* support
multiplicity" (a data-model question) is logically prior to "*should*
Finding Model perform aggregation" (a behavioral question).

### 4. Can a Finding exist without a RuleEvaluationResult?

The central fork your instructions ask to investigate explicitly, not
assume. Arguments both ways:

- **Against:** `project.md` §3.2's Deterministic-Before-AI principle
  and the §6 Facts->Analysis->Rules->Findings->AI-Reasoning ordering
  both suggest Findings are always Rule-derived in v1 — AI Reasoning
  is positioned strictly *after* Findings, implying AI does not
  originate them, only reasons about already-produced ones.
- **For:** `project.md` §5 lists many future agents (Design Quality,
  Security, Risk...) whose checks may not all cleanly fit Rule
  Framework's deterministic, parameterized-Rule-Type shape forever —
  and `project.md` §3.8's Evidence-Based Intelligence principle talks
  about AI-generated conclusions in a way that could imply an
  AI-native Finding path eventually exists.
- Resolution for *v1* likely leans "no" (every Finding traces to at
  least one `RuleEvaluationResult`, consistent with `project.md` §6's
  ordering and with keeping Finding Model itself deterministic per
  the Analysis Framework/Rule Framework precedent of "no AI/LLM
  surface in v1") — but this is Design's call, not asserted here.

### 5. Finding identity and stability across CSM snapshots

This is the sharpest tension with established precedent found in this
Explore. Every identity scheme so far (`AnalysisResult`,
`RuleEvaluationResult`) is a deterministic function that *includes the
source CSM Snapshot identity* — meaning identity necessarily changes
with every new snapshot, even if the underlying condition is
unchanged. That's correct and intentional for an evaluation-run
artifact. But a Finding, in the ordinary sense the word is used in
governance/scanning tools, usually needs a *second*, snapshot-independent
identity so a consumer can ask "is this the same problem I saw last
week, still open?" — CSM's own snapshot-scoped-per-run identity
discipline doesn't answer that question by itself. Candidate: a
two-tier identity — (a) a per-evaluation-run instance identity,
following the established pattern exactly, for durability/audit; and
(b) a stable "logical Finding key" (e.g., a function of the Rule
identifier and the identity of the CSM element/Subject it concerns,
deliberately excluding snapshot identity) used for cross-snapshot
tracking. This would be new to this system — no established
precedent, worth flagging as a genuine design fork rather than a
mechanical continuation of the pattern.

### 6. Finding lifecycle — does it belong in v1?

Related to question 5. If Findings are tracked across snapshots
("open", "resolved" by no longer appearing, "suppressed" by
policy/human override), does that require a *mutable* lifecycle-state
field — which would be the first mutable artifact anywhere in this
system, breaking the "durable, immutable, superseded-not-overwritten"
discipline every prior layer holds to — or can lifecycle be expressed
non-destructively (each evaluation run produces new, immutable
Findings; "resolved" is simply "no longer produced by the latest run,"
computed by a consumer comparing runs, not stored as a state
transition on the Finding itself)? The non-destructive reading is more
consistent with precedent; a stateful reading is closer to what
`project.md` §3.5's remediation workflow (`Finding -> Recommendation ->
... -> Verification`) seems to imply is needed eventually. Whether
that stateful need belongs in *this* capability's v1, or is deferred
the way incremental analysis sophistication and retention policy were
each deferred at their own layers, is open.

### 7. Severity/prioritization semantics and ownership

`project.md` §3.2 explicitly lists "prioritization" as a use case for
AI, not deterministic analysis. But `project.md` §4.8 lists Severity
as a Finding field, and Finding Model (per the working hypothesis and
the §6 ordering) is meant to stay pre-AI-Reasoning. Candidates: (a)
Severity is a static property of the *Rule Type* (declared once, like
a Rule Type's other configuration, e.g. "boundary violations are
always HIGH") — deterministic, consistent with keeping Finding Model
itself AI-free; (b) Severity is computed by Finding Model from
context (e.g., number of aggregated `RuleEvaluationResult`s, blast
radius) — still deterministic, but a genuinely new kind of
Finding-Model-owned logic beyond pass-through; (c) Severity is left
absent/placeholder in v1's Finding, to be set by a future Agent
Framework prioritization step, consistent with CSM's own text framing
Recommendations (and by extension possibly Severity-refinement) as
attached *after* Finding creation. `project.md`'s own "or rule-generated
guidance" phrasing for Recommendations (see Framing, above) suggests
(a) is the most textually supported default, but this is genuinely
open.

### 8. Finding evidence and supporting context

`project.md` §4.8 names `Evidence` as a Finding field, but this
project's own vocabulary already gives "Evidence" a specific, narrower
meaning (`aip.core.evidence` — raw, tool-specific Repository Evidence,
explicitly distinct from CSM Knowledge). A Finding almost certainly
should **not** reach Repository Evidence directly — that would violate
the established-contracts-only boundary every layer since CSM Builder
has held. More likely, "Finding evidence" means: the CSM
element/Subject identities and provenance records the Finding concerns
(already reachable via `RuleEvaluationResult`'s own traceability
chain), formatted for a consumer — i.e., a *view* over existing
traceability, not a new evidence-gathering responsibility. Worth
confirming in Design so `project.md`'s word choice doesn't get
silently reinterpreted as a new dependency.

### 9. Finding traceability back to Rule, RuleEvaluationResult, AnalysisResult, and CSM Snapshot

Directly anticipated by `define-rule-framework/design.md`'s own
Decision 4 traceability-chain sentence (quoted above, under "What
Finding Model consumes"). The open question isn't *whether* this
traceability exists — it clearly must, continuing the pattern — but
whether Finding Model re-exposes the *full* chain itself (Finding
carries direct references to `AnalysisResult` and CSM Snapshot
identities, duplicating what `RuleEvaluationResult` already carries)
or only references its constituent `RuleEvaluationResult`(s) and
relies on *their* already-established traceability for the rest —
i.e., each layer trusts the immediately-prior layer's own traceability
rather than re-flattening the entire chain at every step. The latter
is more consistent with how `RuleEvaluationResult` itself relates to
`AnalysisResult` (it references AnalysisResult identities directly
alongside CSM Snapshot identity — actually a *counter-example*: Rule
Framework's own Decision 4 chose to include source CSM Snapshot
identity directly rather than relying solely on transitively reaching
it through consumed AnalysisResults). This suggests Finding Model may
need to make the same choice explicitly, not assume the answer by
analogy.

### 10. Aggregation and deduplication semantics

If Finding Model aggregates multiple `RuleEvaluationResult`s into one
Finding (question 3), the identity-extension pattern established by
`RuleEvaluationResult`'s own Decision 4 (identity extended to include
the *complete set* of consumed upstream identities) generalizes
cleanly: a Finding's identity could be a deterministic function
including the complete set of consumed `RuleEvaluationResult`
identities. The harder question is what determines *what gets
grouped* — is grouping deterministic and structural (e.g., "same Rule,
same containing Module" — a fixed, declared grouping key), or does it
require judgment calls the deterministic-only discipline is meant to
avoid in v1? CSM's own `Non-Destructive Preservation of Competing
Knowledge` principle argues that whatever aggregation happens must not
silently drop the individual `RuleEvaluationResult`s being aggregated
— they'd need to remain independently traceable inside the aggregated
Finding, not discarded.

### 11. Finding validation and publishing

Strong precedent for a `FindingValidator`/`FindingPublisher` pair,
mirroring `CsmValidator`/`SnapshotPublisher`,
`AnalysisResultValidator`/`AnalysisResultPublisher`, and
`RuleEvaluationResultValidator`/`RuleEvaluationResultPublisher` — three
consecutive layers have each independently reached the same
validate-before-publish gate. The specific checks Finding-level
validation would need to perform are open: does it re-verify
referential integrity across the *entire* chain (down to CSM Snapshot
identity), or only that its own directly-referenced
`RuleEvaluationResult` identities currently exist and are themselves
already-published — trusting that each prior layer's own gate already
did its part of the verification? No prior layer has had to answer
this "how much do I re-verify vs. trust" question explicitly yet,
because this is the first layer built entirely on top of another
already-gated layer's output rather than on raw-er content.

### 12. Module placement and dependency direction

Covered in Framing above. Candidates: (a) a new sibling module (e.g.
`aip-findings`), depending on `aip-core` only, continuing the
one-capability-one-module discipline; (b) folded into `aip-rules` —
likely rejected for the same reason CSM Builder/Analysis
Framework/Rule Framework each stayed distinct modules (conflates two
capabilities `project.md` §11 treats as distinct evolution steps); (c)
folded into `aip-ai` — likely rejected, since `project.md` §6's own
ordering places Findings *before* AI Reasoning, and `CLAUDE.md`
explicitly bars AI-specific concerns from core/deterministic
capabilities. (a) is the strongest candidate on current evidence, not
pre-decided here.

### 13. Whether any shared contract belongs in `aip-core`

Following the by-now three-times-repeated pattern
(`CsmSnapshotSource`, `AnalysisResultSource`), a
`RuleEvaluationResultSource` contract in `aip-core` — read a
`RuleEvaluationResult` by identity, list results by (Rule identifier,
source CSM Snapshot identity) — is the natural continuation, letting a
Finding-Model module depend on `aip-core` only, never on `aip-rules`
directly. The same multi-future-consumer argument
`define-rule-framework/design.md` Decision 7 already made for
`AnalysisResultSource` applies again (Agent Framework and a future
CLI/query surface are plausible additional readers of
`RuleEvaluationResult`s, not just Finding Model). Strong candidate, not
pre-decided.

Separately: does Finding need its own Scope-declaration concept
(mirroring Analysis Scope / Rule Scope), or does it have no independent
scope at all because it doesn't evaluate anything against CSM directly
— it only consumes and reinterprets already-evaluated
`RuleEvaluationResult`s? Current evidence suggests the latter (Finding
Model would be the first layer in this chain *without* its own Scope
concept) — worth confirming explicitly rather than mechanically
copying Scope forward a third time just because the pattern exists.

### 14. Relationship to the archived CSM definition of Findings

Covered under "The pipeline-refinement tension, a third time" above.
Needs the same explicit confirmation the prior two rounds each
required before Design treats it as settled.

## Recommended Next Step

The framing (module-placement direction, the pipeline-refinement
reading of CSM's "structured output of Analysis" text) is grounded
enough to carry forward as working assumptions, but per the
established pattern, needs your explicit confirmation before Design
builds on it. Of the 14 questions, **question 4 (can a Finding exist
without a RuleEvaluationResult) and question 5 (snapshot-independent
Finding identity/stability)** are the two most likely to reshape
everything else if answered differently than the working hypotheses
suggest — question 5 in particular is the first place this system's
established identity discipline has run into genuine tension rather
than extending cleanly, and deserves the most deliberate attention in
Design.
