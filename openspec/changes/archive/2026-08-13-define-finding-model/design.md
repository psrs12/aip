## Context

See `proposal.md` for motivation and `explore.md` for the framing this
design treats as confirmed and binding: `canonical-software-model`'s
archived "policy evaluated -> Finding produced" pipeline transition is
refined, not contradicted, into
`CSM Snapshot -> Analysis Framework -> AnalysisView/AnalysisResults
-> Rule Framework -> RuleEvaluationResults -> Finding Model`, the same
interpretive move already confirmed for Analysis Framework's and Rule
Framework's own relationships to that archived text. This document
does not modify `openspec/specs/canonical-software-model/spec.md`.

Binding external facts this design does not reopen:
- `aip-core` already hosts `aip.core.csm`, including `AnalysisView`,
  `CsmSnapshotSource`, and the shared Scope declaration type.
- `aip-rules` already specifies `RuleEvaluationResult`: a durable
  artifact carrying `PASS`/`FAIL`/`NOT_APPLICABLE`, with identity a
  deterministic function of (producing Rule identifier, Rule version,
  source CSM Snapshot identity, Rule Scope instance, complete consumed
  `AnalysisResult` identity set), full traceability to that same
  tuple, an opaque Rule-Type-defined payload, and its own
  `RuleEvaluationResultValidator`/`RuleEvaluationResultPublisher` gate.
  None of this is modified by this change.
- CSM element and relationship identity (per the archived
  `implement-csm-builder` design) is **already a pure, deterministic
  function of originating Evidence identity** — stable across
  re-derivation runs by construction (`implement-csm-builder`'s own
  Section 5 identity-stability tests: unchanged-evidence stability,
  Package identity survival, dependency-relationship identity
  stability). This fact is not new here; it is load-bearing for
  Decision 3 below.
- `project.md` §4.8 names the Finding shape this design's Decision 5
  works from.
- `define-rule-framework` already specifies `Single-Repository Rule
  Evaluation Scope`: one Rule Framework evaluation run, and every
  RuleEvaluationResult it produces, is scoped to exactly one
  repository's CSM Snapshot. Since every Finding traces to at least
  one RuleEvaluationResult (Decision 1), Finding Model inherits
  single-repository scope directly from this already-approved
  upstream requirement — this design does not make a new decision
  about it, the same way it does not re-decide anything else already
  settled one layer down.

## Goals / Non-Goals

**Goals:**
- Resolve whether a Finding can exist without a `RuleEvaluationResult`.
- Resolve Finding identity — whether a single, snapshot-bound identity
  suffices, or whether a second, snapshot-independent identity concept
  is genuinely needed, and if so, derive it without inventing a second
  incompatible identity mechanism from nothing.
- Resolve Finding lifecycle without introducing mutable state unless
  demonstrated necessary.
- Resolve Finding-to-RuleEvaluationResult cardinality, shape,
  Confidence provenance, Severity ownership, Evidence meaning,
  traceability depth, aggregation/grouping determinism, and the
  validation/publishing gate.
- Resolve module placement and `aip-core` contract ownership.
- Resolve whether Finding needs its own Scope concept.
- Identify, explicitly, where Finding Model cannot simply copy an
  established pattern from Analysis Framework or Rule Framework.

**Non-Goals:**
- Writing `openspec/specs/finding-model/spec.md` (Specify phase).
- AI-generated or manually-created Findings — a future capability's
  concern (Decision 1).
- Remediation workflows, suppression workflows, and any
  lifecycle-presentation/dashboard mechanism beyond the deterministic
  ingredients Decision 4 establishes — deferred, not designed here.
- Concrete `FindingStore` persistence technology — deferred the same
  way `AnalysisResultStore`'s and `RuleEvaluationResultStore`'s
  persistence technology were each deferred at their own Design stage.
- Reopening any of `define-analysis-framework`'s seven or
  `define-rule-framework`'s seven binding decisions.
- Modifying the archived `canonical-software-model` specification.
- Implementation code of any kind.

## Where Finding Model cannot simply copy an established pattern

Named explicitly, per instruction, rather than left implicit:

- **Identity (Decision 2).** Every prior artifact's identity is a
  *single*, deterministic function that includes source CSM Snapshot
  identity — correct for an evaluation-run artifact, wrong for
  "recognize the same issue across snapshots." No prior layer has
  needed a second identity concept. Finding Model is the first.
- **Lifecycle (Decision 3).** Every prior artifact is a produce-once,
  never-mutated, never-overwritten record. Nothing before Finding
  Model has had to consider "open vs. resolved" at all — evaluation
  artifacts don't get "resolved," they simply stop being reproduced.
  Finding Model is the first layer where a consumer's ordinary
  vocabulary ("is this still a problem?") presses against that
  discipline.
- **Scope (Decision 8).** Every prior layer (Mapper, Analyzer, Rule
  Type) declares an explicit Scope and computes its own applicability
  against CSM content. Finding Model evaluates nothing against CSM
  directly — it interprets already-evaluated `RuleEvaluationResult`s.
  It is the first layer in this chain with no Scope concept at all.

Everything else below (validation/publish gate, `aip-core` contract
ownership, module-per-capability placement, deterministic-identity
discipline itself) is a direct, un-modified continuation of
established pattern.

## Decisions

### 1. Provenance requirement: every Finding traces to at least one RuleEvaluationResult

**Problem:** Can a Finding exist without a `RuleEvaluationResult` —
e.g., a future AI-native or manually-authored Finding — or must every
Finding originate from Rule Framework's deterministic evaluation?

**Decision:** In this version, **every Finding SHALL trace to at least
one `RuleEvaluationResult`.** No Finding Model mechanism for
AI-generated, manually-authored, or otherwise `RuleEvaluationResult`-
independent Findings exists in v1. This follows directly from
`project.md` §6's own ordering (`Facts -> Analysis -> Rules -> Findings
-> AI Reasoning -> Recommendations`) — Findings are positioned strictly
*before* AI Reasoning, meaning AI does not originate Findings, it
reasons about already-produced ones — and from `project.md` §3.2's
Deterministic-Analysis-Before-AI principle, which every prior layer
(Analyzer, Rule Type) has already honored by excluding an AI/LLM
surface entirely. Keeping Finding Model itself deterministic and
`RuleEvaluationResult`-sourced is the third consecutive layer to make
this same choice, not a new one.

**Alternatives considered:**
- **Allow Finding Model to accept externally-supplied Findings** (e.g.
  a manually-filed architecture concern, or a future AI agent's
  direct output), alongside Rule-derived ones. Rejected for v1 —
  weakens the provenance contract every Finding would otherwise
  uniformly satisfy (full traceability to a Rule, a CSM Snapshot, and
  ultimately Evidence), and there is no `project.md`-stated v1
  requirement forcing this now. Explicitly not foreclosed permanently:
  `project.md` §5's many future agents, and §3.8's Evidence-Based
  Intelligence principle, both leave room for a future capability
  (plausibly under Agent Framework) to introduce an AI-native or
  manual Finding-authoring path *alongside* this one — as a
  deliberate, separately-designed addition, not a weakening of this
  decision.
- **Require exactly one `RuleEvaluationResult` per Finding** (no
  aggregation ever). Considered and folded into Decision 2 instead —
  this decision only settles *whether zero is allowed* (no), not how
  many above one.

### 2. Finding-to-RuleEvaluationResult relationship: a distinct artifact referencing a non-empty set

**Problem:** Is a Finding a new artifact that references
`RuleEvaluationResult`(s), or merely `RuleEvaluationResult` renamed
once published? If distinct, can one Finding derive from more than
one `RuleEvaluationResult`?

**Decision:** A Finding is a **distinct artifact** — consistent with
every prior layer transition (CSM Snapshot -> AnalysisResult ->
RuleEvaluationResult), each of which introduced a genuinely new,
identity-extended type rather than relabeling its input. A Finding's
defining reference is a **non-empty set of `RuleEvaluationResult`
identities** (not a single identity) — generalizing the same
"identity/reference is a set, not a scalar" shape
`define-rule-framework/design.md` Decision 4 already introduced one
layer down (there, the set of consumed `AnalysisResult` identities).
This settles the *data-model* question (question 3/6 from Explore):
the relationship structurally supports one-to-many.

Whether Finding Model's v1 *construction behavior* actually populates
that set with more than one element is a separate, behavioral
question — resolved conservatively in Decision 7 (Aggregation): v1
always produces exactly one Finding per `RuleEvaluationResult` (the
set has exactly one member in practice), while the shape itself does
not need to change later if aggregation is added, avoiding a second
migration the way `AnalysisResultSource`'s early placement in
`aip-core` was itself justified as avoiding one.

**Alternatives considered:**
- **A Finding references exactly one `RuleEvaluationResult`
  (scalar reference, no future aggregation path).** Rejected — would
  force a breaking identity-shape change later if aggregation (a
  realistic future need, per Explore question 3) is ever added,
  repeating exactly the kind of forced migration this project's own
  precedent (`CsmSnapshotSource`'s introduction after the fact) has
  already had to absorb once and deliberately avoided a second time
  when placing `AnalysisResultSource` in `aip-core` early.
- **Finding is not a distinct artifact; `RuleEvaluationResult` itself
  gains a "published as Finding" flag.** Rejected — directly
  contradicts `define-rule-framework`'s own `Rule Evaluation Results
  Are a Distinct Concept From CSM Knowledge, Analysis Results, and
  Findings` requirement, which is a completed, binding decision this
  design does not reopen.

### 3. Finding identity: two distinct, deterministic identity concepts

**Problem:** Every established identity scheme includes source CSM
Snapshot identity, making identity change on every new evaluation run
even when the underlying issue is unchanged. A Finding, in ordinary
governance-tool usage, needs to answer "is this the same problem as
last time?" — which snapshot-inclusive identity cannot answer by
itself. Does Finding Model need a second identity concept, and if so,
what is it a deterministic function of?

**Decision:** Finding Model uses **two distinct, both fully
deterministic, identity concepts** — not a replacement of the
established pattern, an addition to it:

- **Evaluation Identity** — a deterministic function of the complete
  set of consumed `RuleEvaluationResult` identities (Decision 2),
  following the established pattern exactly (each identity in that
  set already transitively encodes source CSM Snapshot identity, Rule
  identifier/version, and Rule Scope instance). Two Findings with the
  same Evaluation Identity are the literal same evaluation-run
  artifact. This is what makes a Finding durable and
  individually-retrievable, mirroring `AnalysisResult` and
  `RuleEvaluationResult` precedent unchanged.
- **Logical Finding Identity** — a deterministic function of **the
  producing Rule's identifier (not its version) and the CSM element
  identity the underlying Rule Scope instance's containment-level
  anchor concerns** — deliberately **excluding** source CSM Snapshot
  identity. This is possible, and requires no new mechanism, because
  of the load-bearing fact named in Context: CSM element identity is
  *already* a pure function of Evidence identity, stable across CSM
  Snapshots by construction (`implement-csm-builder` Section 5). A
  Rule Scope instance's containment-level anchor element identity
  (present, for anchored Rule Types, inside the
  `RuleEvaluationResult`'s own traceability — Rule Scope Declaration)
  is therefore *itself* already snapshot-independent; Logical Finding
  Identity does nothing more than fold that already-stable identity
  together with the stable Rule identifier. For an unanchored Rule
  Type (whole-repository Rule Scope), the "subject" is the Repository
  CSM element itself, whose identity is equally snapshot-independent
  by the same fact — the mechanism is uniform across anchored and
  unanchored Rule Scopes, no special case needed.

  **A Rule Scope instance's anchor is always a CSM *element* identity,
  never a relationship identity** — Rule Scope Declaration (mirroring
  Analysis Scope Declaration) defines a containment-level anchor as
  narrowing invocation to "specific contained elements," even when the
  Rule Scope's own declared kinds include relationship types read as
  content within that invocation. A Rule Type whose condition concerns
  a *specific* relationship (e.g. one particular forbidden dependency
  edge, among several read within one anchored invocation) has no
  standardized, Framework-visible identity for that specific
  relationship to key on — only its own opaque, Rule-Type-defined
  payload (`Rule Evaluation Result Payload Is Rule-Type-Defined`) knows
  which relationship triggered the outcome, and Finding Model does not
  interpret that payload. Logical Finding Identity therefore resolves
  to the *anchor element's* identity in every case; relationship-level
  distinction within one anchor's invocation, if ever needed, is a
  future concern for Rule Type payload conventions, not something this
  design invents a Framework-level mechanism for now (see Risks/
  Trade-offs).

  Rule *identifier*, not *version*, is used deliberately: including
  version would make Logical Finding Identity change on every Rule
  Type version bump, defeating its entire purpose (recognizing the
  same logical issue across evaluation runs, including runs where the
  checking logic itself was refined).

Two Findings sharing a Logical Finding Identity but differing
Evaluation Identity represent the same logical issue, observed across
two different evaluation runs (typically two different CSM Snapshots,
though re-evaluation against an unchanged snapshot with a bumped
Analyzer version is equally possible, per `RuleEvaluationResult`'s own
Decision 4 reasoning). Neither identity is optional or derived from
the other; both are computed directly and independently, and both are
retained on every Finding (Decision 6, Traceability).

**Alternatives considered:**
- **Single, snapshot-bound identity only (no Logical Finding
  Identity).** Rejected — this is the "blindly reuse the established
  pattern" option your instructions explicitly warned against; it
  would make cross-snapshot tracking structurally impossible without
  a later breaking change, for no offsetting benefit (nothing about
  Evaluation Identity's own correctness requires omitting a second,
  independent concept alongside it).
- **Logical Finding Identity is Rule-version-inclusive** (exactly
  mirroring Evaluation Identity's own precedent of including every
  dependency's version). Rejected — explained above: defeats the
  purpose the identity exists to serve.
- **Logical Finding Identity computed by a content hash of the
  Finding's own diagnostic payload**, rather than Rule identifier +
  subject identity. Rejected — payload content is Rule-Type-opaque
  (per `RuleEvaluationResult`'s own `Rule Evaluation Result Payload Is
  Rule-Type-Defined` requirement, unchanged here) and may legitimately
  vary run-to-run for the same logical issue (e.g. a coupling metric's
  exact numeric value drifting slightly); hashing it would make
  Logical identity spuriously unstable — an inversion of what it's
  for.
- **A wholly new, invented cross-snapshot correlation algorithm**
  (e.g. fuzzy/similarity matching across Findings with no exact-identity
  relationship). Rejected per your explicit instruction not to invent
  a logical-identity algorithm without evaluating alternatives, and
  because the deterministic option above fully satisfies the
  requirement without needing anything approximate or heuristic.

### 4. Finding lifecycle: derived, not stored — no mutable state introduced

**Problem:** Does tracking "is this still a problem" require a
mutable lifecycle-state field on Finding (the first mutable artifact
in this system), an immutable revision/event log, or can it be
computed entirely from what Decisions 2–3 already establish?

**Decision:** **No mutable state, and no new event/revision artifact,
is introduced.** Finding Model's v1 responsibility stops at making
Logical Finding Identity (Decision 3) and Evaluation Identity
(Decision 2) both present and queryable on every Finding — the
deterministic *ingredients* lifecycle tracking needs. Lifecycle itself
(open/resolved, or any richer state) is a **derived, query-time
interpretation** a consumer computes by comparing which Logical
Finding Identities are present in the latest evaluation run's Findings
against which were present in a prior run — e.g., "resolved" simply
means a Logical Finding Identity that appeared in a previous run's
Findings does not appear in the latest run's. No Finding is ever
mutated, superseded-in-place, or deleted to express this; each
evaluation run's Findings remain independently durable and retrievable
exactly as `RuleEvaluationResult`s already are (`RuleEvaluationResult`s
Are Durable, Individually Identifiable Artifacts`, extended here
unchanged).

Whether *building* that comparison (a lifecycle view/query surface) is
itself part of this v1 capability, or a separate future concern, is
scoped conservatively: **out of scope for v1.** Finding Model
guarantees the ingredients exist and are correct; a lifecycle-
presentation mechanism is deferred the same way CSM Builder deferred
full precedence/resolution sophistication and Rule Framework deferred
retention policy — a real, named gap, not a silently assumed one.

**Alternatives considered:**
- **Explicit mutable lifecycle-state field** (`OPEN`/`RESOLVED`/
  `SUPPRESSED`, updated in place). Rejected — would be the first
  mutable artifact anywhere in this system, breaking the
  durable/immutable/superseded-not-overwritten discipline every prior
  layer holds to, for a capability (lifecycle presentation) that the
  derived approach already achieves without it.
- **Immutable lifecycle *events*** (a separate, append-only
  "Finding-transitioned-to-resolved" record per logical Finding).
  Rejected for v1 — this would still respect immutability, but adds a
  second artifact type and its own identity/traceability/validation
  obligations for a capability the simpler "compare Logical Finding
  Identity presence across runs" approach already provides without
  any new mechanism. Not foreclosed permanently — if a future need
  arises for explicit lifecycle *reasons* (e.g., "why was this
  suppressed," which absence-based comparison cannot express), an
  event log is the natural next step, deliberately deferred rather
  than built speculatively now.
- **No lifecycle concept addressed at all, leaving it entirely
  undefined.** Rejected as stated — `project.md` §3.5's remediation
  workflow (`Finding -> Recommendation -> ... -> Verification`)
  clearly anticipates a Finding's status changing over time; ignoring
  the question rather than establishing the deterministic ingredients
  for it would leave a real future need unaddressed by accident.

### 5. Finding shape and Confidence provenance

**Problem:** `project.md` §4.8 lists `Finding ID, Rule ID, Category,
Severity, Confidence, Location, Evidence, Description, Impact,
Recommendation, Remediation availability`. Which of these does v1
Finding Model actually populate, and where does a deterministically-
derived Finding's Confidence value come from?

**Decision:** v1's Finding carries: `Finding ID` (Evaluation Identity,
Decision 2), `Logical Finding Identity` (Decision 3), `Rule ID`
(producing Rule identifier — already on the referenced
`RuleEvaluationResult`(s), surfaced directly per Decision 6), `Category`
(Rule-Type-declared static classification, configured the same way
`Severity` is, immediately below), `Severity` (Rule-Type-declared
static configuration — see this same Decision's `Alternatives
considered`, below, for why this is not Finding-Model-computed),
`Location`
(the CSM element identity Decision 3's Logical Finding Identity
already computes from), `Evidence` (Decision 6's traceability content
— not raw Repository Evidence), `Description` (Rule-Type-declared
static template text, parameterizable by the Rule's own configuration
— the same "declarative configuration, no executable logic" shape
`define-rule-framework/design.md` Decision 1 established for Rule
itself), and `Impact` (Rule-Type-declared static text, same shape as
Description). **`Confidence` is fixed at a single, maximal,
deterministic value for every v1 Finding** — since every v1 Finding
traces exclusively to deterministic Rule evaluation (Decision 1), and
CSM's own Confidence representation is explicitly reserved for
*inferred* knowledge, not deterministic evaluation outcomes; a
meaningful, *varying* Confidence value has no deterministic source
until a future AI-assisted or inferred Finding path exists (explicitly
out of scope, Decision 1). **`Recommendation` and `Remediation
availability` are not populated by Finding Model in v1** — left absent
rather than defaulted to an empty placeholder value, consistent with
CSM's own text describing Recommendations as guidance "attached to a
Finding" (implying a later, separate attachment step, not a Finding-
Model-owned field) and with `project.md` §6's ordering placing AI
Reasoning/Recommendations strictly after Findings.

**Alternatives considered:**
- **Confidence varies per Finding based on some heuristic (e.g. how
  many `RuleEvaluationResult`s corroborate it).** Rejected for v1 —
  this would be Finding-Model-owned heuristic scoring, which
  `project.md` §3.2's Deterministic-Before-AI principle and this
  design's own Decision 1 both argue against introducing at this
  layer; a fixed value is the honest representation of "this is
  certain by construction," not an evasion of the question.
  Explicitly revisitable once inferred/AI-assisted Findings exist.
- **Populate `Recommendation`/`Remediation availability` with
  Rule-Type-declared static text now**, since Category/Description/
  Impact are already resolved that way. Rejected — CSM's own text
  specifically calls out Recommendations as "attached to a Finding"
  (a distinct step), and `project.md` §3.5's remediation workflow
  frames Recommendation as following Finding, not co-produced with
  it; conflating the two now would blur a boundary `project.md`
  itself draws, for no v1 requirement forcing it.

### 6. Traceability depth and Finding Evidence

**Problem:** Does a Finding carry the full chain of identities down to
source CSM Snapshot directly, or only its immediate
`RuleEvaluationResult` reference(s), trusting their own already-
established traceability for the rest? And what does "Finding
Evidence" (`project.md` §4.8) actually mean, given this project's
narrower, established meaning of "Evidence" (raw Repository Evidence)?

**Decision:** A Finding carries its consumed `RuleEvaluationResult`
identity set (Decision 2) **directly**, and additionally surfaces —
by direct reference, not by re-deriving — the source CSM Snapshot
identity and the concerned CSM element identity (Decision 3's
containment-level-anchor identity) that those `RuleEvaluationResult`(s)
already carry. This mirrors the choice
`define-rule-framework/design.md` Decision 4 itself made one layer
down: `RuleEvaluationResult` did not rely solely on transitively
reaching CSM Snapshot identity through its consumed `AnalysisResult`s
— it included that identity directly, reasoning that identity/
traceability should be a faithful, directly-inspectable function of
everything a Result depends on, not something requiring a client to
walk multiple hops to reconstruct. Finding Model applies the same
reasoning: "Finding traceable to Rule, RuleEvaluationResult,
AnalysisResult, and CSM Snapshot" (Explore question 9) is satisfied by
directly retaining the `RuleEvaluationResult` identity set (from which
producing Rule and consumed `AnalysisResult`s are already reachable —
no need to re-flatten those two further, since `RuleEvaluationResult`
already exposes them) plus directly retaining CSM Snapshot identity
and the concerned CSM element identity (needed for Decision 3's
Logical Finding Identity anyway, so surfacing it explicitly costs
nothing new).

**`Finding Evidence` means this traceability content, formatted for a
consumer** — the concerned CSM element identity plus any CSM
element/relationship identities and provenance references a Finding's
underlying `RuleEvaluationResult`(s) read within their Rule Scope
instance, reachable transitively to originating
Repository Evidence via CSM's own already-established `Structured
Provenance Record` mechanism (`Provenance record supports
explanation`). Finding Model SHALL NOT read Repository Evidence
directly — the established established-contracts-only boundary every
layer since CSM Builder has held (Rule Framework's own `CSM and
Analysis Content Reached Only Through Established Contracts`
requirement) extends here unchanged, one layer further.

**Alternatives considered:**
- **Finding references only its `RuleEvaluationResult` identity
  set, with CSM Snapshot identity and concerned-element identity
  reached only transitively (walking into the referenced Results to
  find them).** Rejected — inconsistent with the precedent Rule
  Framework itself set (see Decision reasoning above); also would
  force every Finding consumer to always resolve at least one
  `RuleEvaluationResult` before it could answer "which CSM Snapshot is
  this Finding about," a basic traceability question `project.md`
  §3.3's Explainability principle argues should be answerable
  directly.
- **"Finding Evidence" means literal Repository Evidence content,
  inlined or referenced directly.** Rejected — violates the
  established-contracts-only dependency boundary, and duplicates
  content already reachable through CSM provenance without a
  direct dependency.

### 7. Aggregation and grouping: none performed in v1; the shape allows it later

**Problem:** Should Finding Model group multiple `RuleEvaluationResult`s
into a single Finding, and if so, on what deterministic basis?

**Decision:** **v1 Finding Model performs no aggregation.** Every
Finding's consumed `RuleEvaluationResult` identity set (Decision 2)
contains exactly one member in v1's actual construction behavior — the
data model supports multiplicity (Decision 2) precisely so this
decision can be revisited later without a breaking identity-shape
change, but *choosing* a deterministic grouping key (e.g. "same Rule,
same containing Module") is a genuine policy judgment this design
declines to invent speculatively, consistent with your instruction not
to introduce Finding Model as another rule/policy engine. Grouping
correctness has real failure modes either way (too coarse hides
distinct issues; too fine floods a consumer with near-duplicates) that
deserve a concrete, motivated use case before being designed, not a
guess made now.

**Alternatives considered:**
- **Design a deterministic grouping key now** (e.g., group by `(Rule
  identifier, containing-Module CSM element identity)`). Rejected for
  v1 — this is exactly the kind of unforced architectural invention
  your instructions ask to avoid; nothing in `project.md` or the
  completed Rule Framework/Analysis Framework specifications names a
  concrete grouping requirement, so designing one now would be
  speculative rather than grounded.
- **Prohibit multiplicity structurally** (scalar reference, no future
  aggregation path at all). Rejected — already addressed under
  Decision 2's own alternatives; would force a breaking change later.

### 8. Finding Model has no Scope concept

**Problem:** Every prior layer (Mapper, Analyzer, Rule Type) declares
an explicit Scope and computes applicability against CSM content
directly. Does Finding Model need one too, continuing the pattern
mechanically?

**Decision:** **No.** Finding Model does not evaluate anything against
CSM content — it interprets already-evaluated, already-scoped
`RuleEvaluationResult`s. A `RuleEvaluationResult`'s own Rule Scope
instance already fully determined what CSM content was relevant;
Finding Model has no independent applicability question left to ask.
Introducing a Scope concept here would be copying an established
pattern mechanically rather than because Finding Model's own
responsibilities require it — exactly what your instructions warn
against. Finding Model is the first layer in this chain without a
Scope concept, named explicitly (see "Where Finding Model cannot
simply copy an established pattern," above, treated as the mirror
case: here the established pattern is correctly *not* reused, rather
than needing modification to be reused).

**Alternatives considered:**
- **Reuse the shared `aip-core` Scope declaration for Finding Model
  too**, for consistency with Analyzer/Rule Type. Rejected — there is
  no applicability computation for it to describe; a Scope declaration
  with nothing to gate would be vestigial, adding a concept without a
  corresponding responsibility.

### 9. Validation and publishing: yes, trusting each prior layer's own gate rather than re-verifying the full chain

**Problem:** Does Finding need a validator/publisher gate like every
prior layer? If so, how much of the chain does it re-verify — only its
direct `RuleEvaluationResult` references, or the full chain back to
CSM Snapshot?

**Decision:** Yes — a `FindingValidator` checks, before a
`FindingPublisher` ever calls `FindingStore.write`:
- **Referential integrity, immediate layer only** — every referenced
  `RuleEvaluationResult` identity (Decision 2) SHALL exist and SHALL
  already be published (per `RuleEvaluationResult`'s own validation
  gate). Finding Model does **not** re-verify referential integrity of
  those Results' own consumed `AnalysisResult`s or CSM content —
  it trusts that `RuleEvaluationResultValidator` already did so. This
  is the first explicit "trust the immediately-prior layer's own gate,
  don't re-verify the whole chain" choice in this system — every prior
  gate validated against content one layer *closer to raw* than
  itself (CSM against Evidence-derived construction; Analysis Result
  against CSM Snapshot content; Rule Evaluation Result against
  Analysis Results and CSM content), never against another artifact's
  *own already-completed validation*. Re-verifying the entire chain at
  every layer would make each successive layer's validation cost grow
  unboundedly with pipeline depth, for no correctness benefit beyond
  what the immediately-prior gate already guarantees.
- **Identity consistency** — a Finding's declared Evaluation Identity
  and Logical Finding Identity (Decision 3) SHALL be independently
  recomputable from its own referenced `RuleEvaluationResult`
  identity set and be self-consistent with them.
- **Provenance requirement (Decision 1)** — a Finding's
  `RuleEvaluationResult` reference set SHALL be non-empty.

**Alternatives considered:**
- **Re-verify the full chain down to CSM Snapshot on every Finding.**
  Rejected — reasoning above; also inconsistent with this system's own
  layering intent (each layer's gate is defense-in-depth for *its own*
  construction step, not a re-audit of every earlier step).
- **No validation gate at all, trusting `RuleEvaluationResult`'s own
  published state unconditionally with no Finding-level check.**
  Rejected for the same reason every prior layer's own Decision
  rejected it: guards exist independent of implementation quality, so
  a Finding-construction defect (e.g., a stale or unpublished
  `RuleEvaluationResult` reference) fails loudly at publish time
  rather than silently producing an untraceable Finding.

### 10. Module placement: a new `aip-findings` sibling module

**Problem:** Where does Finding Model live — a new module, folded into
`aip-rules`, or folded into `aip-ai`?

**Decision:** A new module, **`aip-findings`**, sibling to
`aip-csm-builder`, `aip-analysis`, and `aip-rules` — none of the four
depends on any other. This continues the one-capability-one-module
discipline every prior capability has used, and is directly supported
by `project.md` §6's own pipeline text
(`Facts -> Analysis -> Rules -> Findings -> AI Reasoning ->
Recommendations`), which places Findings *before*, and therefore
architecturally separate from, AI Reasoning.

```text
  aip-core
  - depended on directly by every module below; depends on none of them

  aip-analyzer   - RU implementation, not yet built     (depends on aip-core only)
  aip-csm-builder - CSM Builder, implemented            (depends on aip-core only)
  aip-analysis   - Analysis Framework, not yet built     (depends on aip-core only)
  aip-rules      - Rule Framework, not yet built         (depends on aip-core only)
  aip-findings   - Finding Model, not yet built          (depends on aip-core only) <- this change

  aip-analyzer, aip-csm-builder, aip-analysis, aip-rules, and
  aip-findings are siblings - none depends on any of the others

  aip-ai (?)     - future, depends on the above (not designed here)
  aip-cli / aip-server - future, depends on the above (not designed here)
```

**Alternatives considered:**
- **Fold Finding Model into `aip-rules`.** Rejected for the same
  reason CSM Builder, Analysis Framework, and Rule Framework each
  stayed distinct modules: `project.md` §11 treats Rule Framework and
  Finding Model as distinct evolution steps, and folding them would
  blur `aip-rules`'s own dependency-graph guard the same way folding
  Rule Framework into Analysis Framework was already rejected for.
- **Fold Finding Model into `aip-ai`.** Rejected — `project.md` §6's
  own ordering places Findings *before* AI Reasoning, and this
  design's Decision 1 keeps Finding Model itself deterministic and
  AI-free in v1; `CLAUDE.md` also explicitly bars AI-specific concerns
  from capabilities that must stay AI-independent. Naming a
  deterministic capability's home module `aip-ai` would misstate what
  it is.

### 11. `aip-core` gains a `RuleEvaluationResultSource` contract; `aip-findings` depends on `aip-core` only

**Problem:** Does `aip-findings` depend on `aip-rules` directly to
read `RuleEvaluationResult`s, repeating the same question
`define-rule-framework` Decision 7 already answered one layer down for
`AnalysisResultSource`?

**Decision:** `aip-core` gains a third read contract,
`RuleEvaluationResultSource` — mirroring `AnalysisResultSource`'s and
`CsmSnapshotSource`'s own shape exactly: read a `RuleEvaluationResult`
by identity, list `RuleEvaluationResult`s by (Rule identifier, source
CSM Snapshot identity). `aip-findings` depends on `aip-core` only —
this new `RuleEvaluationResultSource` and the shared `aip-core` types
its own identity/traceability fields already reference — and SHALL NOT
depend on `aip-rules` directly. Whether `aip-findings` also reads
`AnalysisView` directly, for any Finding-formatting purpose beyond
what `RuleEvaluationResult` traceability already carries, was left
open here for Specify to determine on concrete need; Specify found no
such need — every `finding-model/spec.md` requirement is satisfiable
from `RuleEvaluationResultSource` content alone, so `aip-findings`'s
`aip-core` dependency in v1 is `RuleEvaluationResultSource` and the
shared types only, not `AnalysisView`. The reasoning transfers
unchanged from
`define-rule-framework/design.md` Decision 7's own multi-future-
consumer argument: Finding Model is not the only plausible future
reader of `RuleEvaluationResult`s (Agent Framework and a future
CLI/query surface are named candidates there already), so placing the
contract in `aip-core` now costs little and avoids a second forced
migration later.

**Alternatives considered:**
- **`aip-findings` depends on `aip-rules` directly**, since
  `RuleEvaluationResultStore` is already an interface. Rejected — same
  reasoning `define-rule-framework` Decision 7 already gave and
  rejected for the analogous `aip-analysis` case: the marginal cost of
  one more small, general, read-only `aip-core` contract is low
  relative to the cost of a second consumer later forcing the same
  migration `CsmSnapshotSource` and `AnalysisResultSource` were each
  introduced to avoid.

**Corollary — extensibility to new Rule Types requires no Finding
Model change:** not a separate decision, but a direct consequence of
three decisions already made. Decision 5 makes Category and Severity
uniformly Rule-Type-declared configuration Finding Model only ever
copies, never interprets; Decision 9 and this Decision make Finding
Model's own construction, identity, traceability, and validation
mechanisms operate generically over any RuleEvaluationResult, with no
Rule-Type-specific branching anywhere in them; and this Decision's
`RuleEvaluationResultSource` contract is itself producer-agnostic.
Together, these mean registering a new Rule Type with Rule Framework
requires no Finding Model change — the same extensibility property
`define-rule-framework`'s own `Extension Mechanism for New Rule Types`
requirement established one layer down, inherited here rather than
re-decided.

## Cross-Capability Impacts

- **No modification to `canonical-software-model`, `define-analysis-
  framework`, or `define-rule-framework`.** Unlike the AnalysisView
  gap found while specifying Rule Framework, this Design found no
  missing exposure in either upstream capability: `RuleEvaluationResult`
  already carries everything Decision 3's Logical Finding Identity and
  Decision 6's traceability need (Rule identifier, Rule Scope
  instance's containment-level-anchor CSM element identity, source CSM
  Snapshot identity), and CSM element identity's own
  already-established snapshot-independence (`implement-csm-builder`
  Section 5) is exactly what makes Decision 3 possible without
  inventing a new mechanism or reopening any prior decision. This is
  reported explicitly because Design was instructed to surface, not
  silently absorb, any cross-capability contract gap — and none was
  found.
- **`aip-core` gains one new type** (`RuleEvaluationResultSource`,
  Decision 11) — additive, no change to any existing `aip-core`
  content.
- **Agent Framework** (future) is now known to depend on: Finding's
  Logical Finding Identity and Evaluation Identity (for any lifecycle/
  prioritization work it performs) and the deferred `Recommendation`/
  `Remediation availability` fields (Decision 5) it will be the first
  capability to actually populate.

## Risks / Trade-offs

- [Logical Finding Identity (Decision 3) depends on a Rule Scope
  instance's containment-level anchor always being a CSM element
  identity] → For anchored Rule Scopes this is direct; for unanchored
  (whole-repository) Rule Scopes the "subject" is the Repository CSM
  element itself — verified uniform in Decision 3, not a gap.
- [Logical Finding Identity and Location (Decision 3, Decision 6)
  resolve only to a Rule Scope instance's *anchor element* identity,
  never to a specific relationship identity, even when a Rule Type's
  condition concerns one particular relationship among several read
  within that anchor's invocation] → Named explicitly in Decision 3:
  Rule Scope anchoring (mirroring Analysis Scope) is always
  element-level, so a specific relationship's identity is only
  reachable via a Rule Type's own opaque payload, which Finding Model
  does not interpret. Accepted v1 limitation — a Finding concerning a
  Module with three forbidden dependency edges resolves to one Logical
  Finding Identity per Module per Rule, not one per edge, until a
  future need motivates a payload-interpretation convention or a
  relationship-anchored Scope concept upstream.
- [Excluding Rule version from Logical Finding Identity (Decision 3)
  means a Rule Type bug fix that changes a Finding's outcome from
  FAIL to PASS for the same subject will not automatically "close" the
  old Logical Finding Identity through identity alone] → Mitigation:
  this is exactly what the derived-lifecycle approach (Decision 4)
  handles — the next run's Findings simply won't include that Logical
  Identity, which the same "absent from latest run" comparison already
  interprets as resolved, whether the cause was a genuine subject
  change or a Rule Type version change. No special case needed, but
  worth naming as the reason this design doesn't need to distinguish
  the two causes.
- [No aggregation in v1 (Decision 7) may produce Finding volume
  proportional to `RuleEvaluationResult` volume, with no grouping
  relief] → Accepted v1 cost, mirroring `RuleEvaluationResult`'s own
  accepted "every outcome, retention deferred" posture; revisit once a
  concrete grouping need is demonstrated rather than guessed at.
- [Deferring lifecycle *presentation* entirely (Decision 4) means v1
  ships the deterministic ingredients for cross-snapshot tracking
  without yet building anything that uses them] → Deliberate,
  consistent with this project's repeated pattern of deferring
  concrete consumption/presentation concerns (persistence technology,
  retention policy) while establishing the correct underlying
  contract first.
- [Placing `RuleEvaluationResultSource` in `aip-core` now (Decision
  11), ahead of a second concrete consumer existing] → Same
  low-cost-if-wrong mitigation `define-rule-framework` Decision 7
  already accepted for `AnalysisResultSource`; not re-litigated here.
