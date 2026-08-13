## Context

See `proposal.md` for motivation and `explore.md` for the framing this
design treats as confirmed and binding, per your explicit confirmation:

1. This capability defines the **generic Agent Framework** — the Agent
   contract, registry, consumption boundary, and Recommendation
   artifact — not the Architecture Compliance Agent or any other
   specific agent from `project.md` §5's catalogue. The same
   framework-before-implementations shape `define-analysis-framework`
   and `define-rule-framework` each already established one layer
   down.
2. `aip-ai` is the module, per `CLAUDE.md`'s own module chain.
3. The v1 output boundary is **Recommendation**.
4. Proposed changes, code generation, remediation execution,
   deployment, verification, and human-approval workflow are outside
   this capability.
5. Recommendation SHALL be a separate artifact from Finding — Finding's
   own immutability and deterministic identity (`define-finding-model`
   Decisions 3, 4) are not reopened or weakened.
6. An Agent consumes deterministic upstream artifacts and produces a
   new, AI-owned Recommendation.
7. AI output SHALL NOT modify CSM, `AnalysisResult`s,
   `RuleEvaluationResult`s, Findings, or Rule decisions.

Binding external facts this design does not reopen:
- `aip-core` already hosts `aip.core.csm` and, per `define-finding-model`,
  `RuleEvaluationResultSource`. `aip-rules` already specifies
  `RuleEvaluationResult`. `aip-findings` (not yet built) will specify
  `Finding`: a durable, immutable artifact with two independent
  identities — a snapshot-bound **Evaluation Identity** and a
  cross-snapshot **Logical Finding Identity** — direct traceability to
  source CSM Snapshot identity and concerned CSM element identity, and
  fields for `Recommendation`/`Remediation availability` that Finding
  Model's own Decision 5 leaves unpopulated in v1, naming Agent
  Framework as the anticipated future capability that gives those
  fields meaning. None of this is modified by this change.
- `canonical-software-model` already describes Recommendations as
  "AI- or rule-generated guidance attached to a Finding" that
  "SHALL reference the relevant CSM elements by identity and SHALL NOT
  silently modify CSM knowledge as a side effect of being generated."
- `project.md` §3.2's Deterministic-Analysis-Before-AI principle and
  §7's Non-Goal ("Treat AI-generated recommendations as unquestionable
  truth") both bound this design's validation posture (Decision 7).
- `define-finding-model` already specifies `Single-Repository Finding
  Scope`: every Finding concerns exactly one repository's CSM
  Snapshot. Since every Recommendation references exactly one Finding
  (Decision 2), Agent Framework inherits single-repository scope
  directly from this already-approved upstream fact — this design does
  not make a new decision about it, the same inheritance
  `define-finding-model` itself already applied one layer down for the
  identical reason.

## Goals / Non-Goals

**Goals:**
- Resolve whether an intermediate AI Reasoning artifact is needed
  between Agent and Recommendation.
- Resolve Recommendation's relationship to Finding.
- Resolve Recommendation identity — the central question — without
  either inventing a fictitious determinism guarantee for AI-generated
  content or abandoning deterministic artifact identity altogether.
- Resolve the Agent consumption boundary, preferring the narrowest
  boundary that satisfies the framework.
- Resolve Recommendation multiplicity and whether conflict/precedence
  semantics are needed.
- Resolve Recommendation Confidence's meaning and ownership, distinct
  from Finding's own fixed Confidence.
- Resolve the validation/publishing model, including what a mechanical
  validator can and cannot check for AI-generated content.
- Resolve module and `aip-core` contract boundaries, without adding a
  contract merely for symmetry with prior layers.
- Establish AI-content isolation as a first-class boundary.
- State explicitly what determinism guarantees this framework does,
  and does not, provide.

**Non-Goals:**
- Writing `openspec/specs/agent-framework/spec.md` (Specify phase).
- Designing the Architecture Compliance Agent, or any other specific
  agent from `project.md` §5 — a future capability's concern.
- Proposed Change, code generation, remediation execution, deployment,
  verification of remediation, or human-approval workflow (Decision 9).
- Choosing a concrete LLM provider, model, or prompt-engineering
  approach — implementation detail, not architecture.
- Concrete `RecommendationStore` persistence technology — deferred the
  same way every prior store's persistence technology was deferred at
  its own Design stage.
- Reopening any of `define-analysis-framework`'s, `define-rule-framework`'s,
  or `define-finding-model`'s binding decisions.
- Modifying the archived `canonical-software-model` specification.
- Implementation code of any kind.

## Where Agent Framework cannot simply copy an established pattern

Named explicitly, per instruction:

- **Identity (Decision 3).** Every prior artifact's identity is a
  deterministic function of meaningful inputs such that *identical
  inputs always yield the identical artifact* — the whole point being
  deduplication and reproducibility. Recommendation is the first
  artifact where identical meaningful inputs (same Agent, same
  version, same Finding) can legitimately, correctly produce *multiple
  distinct* artifacts, because the generation step itself is not
  reproducible. Identity here serves a different purpose: unique
  identification of each independently-produced artifact, not
  collapsing repeated identical requests into one.
- **Determinism guarantee (Decision 11).** Every prior artifact
  guarantees "repeated construction over unchanged input is
  identical" as an actual, testable property of its *content*.
  Recommendation is the first artifact where this guarantee applies
  only to its identity and provenance, explicitly not to its
  generated content.
- **Validation (Decision 7).** Every prior validator can mechanically
  confirm its artifact is *correct* in a meaningful sense (referential
  integrity implies correctness, because the artifact's content is
  itself deterministically derived). A `RecommendationValidator` can
  confirm a Recommendation is well-formed and correctly attributed,
  but cannot mechanically confirm its guidance is *good* guidance —
  substantive quality is structurally unverifiable by this layer.

Everything else below (contract ownership in `aip-core`, the
framework-before-implementations shape, module-per-capability
placement, validate-before-publish gate structure) is a direct,
un-modified continuation of established pattern.

## Decisions

### 1. No intermediate AI Reasoning artifact

**Problem:** `project.md` §6's pipeline separates "AI Reasoning" from
"Recommendations" as two named concepts. Does Agent Framework need a
persisted intermediate artifact between an Agent's invocation and its
Recommendation, or does "AI Reasoning" describe an internal step with
no architectural artifact of its own?

**Decision:** **No intermediate artifact.** An Agent produces a
Recommendation directly. "AI Reasoning" is the internal process an
Agent's own code performs (constructing a prompt/context from a
Finding, invoking a model, interpreting the result) — not a durable,
identified, traceable artifact this framework tracks separately. Every
concrete benefit an intermediate artifact could offer — traceability
(what was this generated from), reproducibility metadata (what model/
configuration produced it), separation of internal process from
external output — is already provided by Recommendation's own
Generation Provenance record (Decision 3). Introducing a second
artifact would duplicate that without adding a guarantee the framework
doesn't already have another way to provide, and would create a
second identity/traceability/validation surface for no concrete
benefit — the same "don't invent a mechanism the established pattern
doesn't actually need" discipline `define-finding-model` Decision 8
already applied to Scope.

An Agent's internal reasoning content (e.g. chain-of-thought, if a
model produces it) is explicitly **not** exposed through this
framework's contracts and is **not** required to be persisted —
consistent with your instruction and with keeping AI-specific
mechanics isolated (Decision 10).

**Alternatives considered:**
- **A separate, persisted "AI Reasoning" artifact**, itself
  identified/traceable, that a Recommendation references. Rejected —
  no concrete architectural benefit beyond what Generation Provenance
  already provides; risks becoming a place internal model output
  (including chain-of-thought) accidentally gets persisted and exposed,
  which your instructions explicitly prohibit.
- **Recommendation carries no provenance at all, treating generation as
  an opaque black box.** Rejected — directly loses the explainability
  `project.md` §3.3 requires and the traceability chain this system
  has consistently built at every layer; addressed instead by folding
  provenance directly into Recommendation (Decision 3), not by adding
  a second artifact.

### 2. Recommendation ownership: a separate artifact referencing its source Finding

**Problem:** Does Recommendation content live on the Finding itself
(populating its already-shaped `Recommendation`/`Remediation
availability` fields), or as an independent artifact?

**Decision:** Recommendation is a **new, distinct artifact type**
that references its source Finding by identity — both the Finding's
Evaluation Identity (the precise snapshot-bound artifact the Agent
actually read) and its Logical Finding Identity (carried forward for
cross-snapshot traceability, the same reason Finding itself carries
both). Recommendation SHALL NOT be written into, or represented as
part of, the Finding it references; producing a Recommendation SHALL
NOT alter that Finding's fields.

This means Finding's own `Recommendation` and `Remediation
availability` fields (present in its shape per `project.md` §4.8 and
`define-finding-model` Decision 5) remain **permanently** unpopulated
by design, not merely deferred — the actual content those field names
describe lives in the separately-referenced Recommendation artifact
instead. This is not a reopening of `define-finding-model`'s Decision
5 (which already left those fields unpopulated in v1); it is
confirmation that the reason was structural, not temporary, consistent
with the "attached to a Finding" language `canonical-software-model`
itself uses.

A Recommendation references exactly one Finding in v1 — no compound,
multi-Finding Recommendation shape. Unlike Finding's own
one-RuleEvaluationResult-to-set generalization
(`define-finding-model` Decision 2, motivated by a concrete,
named realistic-volume scenario), no concrete driver for a
multi-Finding Recommendation exists in this Explore or these Design
questions; inventing that generality now would be exactly the
"contract/shape merely for symmetry" your instructions warn against.

**Alternatives considered:**
- **Populate Finding's own `Recommendation` field directly.** Rejected
  — requires either mutating an immutable artifact (directly
  contradicting `define-finding-model` Decision 4) or constructing a
  new Finding revision carrying the same Logical Finding Identity
  purely to attach a Recommendation (conflating a deterministic,
  Rule-derived artifact with AI-generated content, and duplicating
  Finding's own identity machinery for a reason it was never designed
  to serve).
- **Recommendation references a set of Findings**, mirroring Finding's
  own set-shaped RuleEvaluationResult reference. Rejected for v1 — no
  concrete need named; would be invented generality.

### 3. Recommendation identity: deterministic Artifact Identity, separate from non-deterministic content

**Problem — the central question:** Every prior identity scheme
guarantees identical inputs produce the identical artifact. AI-
generated content is not, in general, reproducible. How does
Recommendation get a deterministic, collision-free identity without
falsely claiming its *content* is reproducible?

**Decision:** Two distinct concepts, deliberately not conflated:

- **Recommendation Artifact Identity** — a deterministic function of:
  the producing Agent's identifier, the Agent's version, the source
  Finding's Evaluation Identity, and a **Generation Identifier**: an
  opaque, unique token assigned at generation time (e.g. a UUID or a
  monotonic per-invocation sequence), present precisely because the
  same (Agent, version, Finding) triple can legitimately produce more
  than one independently-valid Recommendation across separate
  invocations. The Generation Identifier is **not** derived from the
  generated content itself — it identifies *the invocation*, not the
  text. Given the same recorded identity components (including the
  same Generation Identifier), the same Recommendation is always
  retrievable — that is what "deterministic artifact identity" means
  here: reproducible *lookup*, not reproducible *generation*.
- **Generation Provenance** — a record, retained on the Recommendation
  but explicitly **not** part of its identity computation, of: the
  Agent identifier/version (duplicated from identity for
  self-contained inspection), the model/provider identifier and
  version, generation configuration (e.g. temperature, prompt-template
  identifier — opaque values, not interpreted by the framework), and a
  generation timestamp. This is what makes a Recommendation
  explainable (`project.md` §3.3) without claiming its content is
  reproducible.

This directly answers the question your instructions posed: the
framework preserves deterministic **artifact** identity (every
Recommendation has one stable, collision-free identity, durably
retrievable) while making no claim that regenerating from the same
(Agent, version, Finding) would produce identical, or even similar,
content — that guarantee simply does not hold at this layer, and this
design does not pretend otherwise.

**Alternatives considered:**
- **Content-derived identity** (e.g. a hash of the generated text).
  Rejected — would make identity itself non-deterministic in the sense
  that matters (you cannot predict or verify it without already having
  the content), and would conflate "this is the same artifact" with
  "this is the same text," which are different questions; also
  inconsistent with every prior layer's practice of never hashing an
  opaque payload into identity (`RuleEvaluationResult`'s own payload is
  explicitly excluded from its identity for the same reason).
- **Identity omitting the Generation Identifier** (Agent + version +
  Finding Evaluation Identity only, mirroring every prior layer's pure
  input-function shape). Rejected — this is the "force an existing
  deterministic pattern onto AI output" your instructions warned
  against directly: two independent, differently-worded, equally valid
  Recommendations from the same inputs would collide on identity,
  silently overwriting or conflicting with each other, which is worse
  than acknowledging the genuine multiplicity this layer has that no
  prior layer did.
- **A single generation-run identity with no separate artifact
  identity** (treat the whole generation event as the identity,
  content included). Rejected — collapses two different concerns
  (durable, stable artifact identification vs. explanatory metadata
  about how it was produced) into one, losing the ability to reason
  about "this artifact" independent of "how it was made," which every
  other layer's identity/traceability split already keeps separate.
- **Requiring byte-for-byte reproducible AI output as a v1
  constraint** (e.g. mandating temperature 0 and treating any
  non-reproducibility as a defect). Rejected explicitly, per your
  instruction — this would be a fragile, false guarantee even at
  temperature 0 (model version drift, provider-side non-determinism),
  and conflating "reproducible" with "trustworthy" is not something
  this design should assert.

### 4. Consumption boundary: Findings only, via a new `FindingSource` contract

**Problem:** Do Agents read only Findings, or also other established
`aip-core` contracts (`AnalysisView`, `AnalysisResultSource`,
`RuleEvaluationResultSource`), per `CLAUDE.md`'s broader "shared AIP
models and analysis results" language?

**Decision:** **Findings only**, read exclusively through a new
`FindingSource` contract in `aip-core` — the narrowest boundary that
satisfies the framework, per your explicit preference. This is
justified, not merely narrow-for-its-own-sake: Finding Model's own
Decision 6 already made Finding directly retain everything an Agent
needs to reason well — source CSM Snapshot identity, concerned CSM
element identity, producing Rule identifier, the complete referenced
RuleEvaluationResult identity set, and (via Finding Evidence) CSM
provenance references — without an Agent needing to separately resolve
`AnalysisView` or `RuleEvaluationResult` content itself. `CLAUDE.md`'s
"shared AIP models and analysis results" language is read here as
describing the *category* of content Agents work from (AIP-produced,
deterministic, already-governed content, as opposed to arbitrary
repository or application access — which your instructions explicitly
prohibit), not as mandating direct multi-contract dependency. This
mirrors every prior layer's own discipline: each consumes only its
immediate upstream artifact's content, never reaching further back
than necessary.

**Alternatives considered:**
- **Findings plus `AnalysisResultSource`/`RuleEvaluationResultSource`/
  `AnalysisView` directly.** Rejected for v1 — Finding's own
  traceability already exposes the *identities* needed to reach that
  content if a future need justifies it; adding three more direct
  dependencies now, with no concrete Agent yet built to demonstrate
  the need, would be exactly the kind of unforced architectural
  expansion your instructions warn against. Not foreclosed
  permanently: if a specific future agent (e.g. Architecture
  Compliance Agent) genuinely needs raw `RuleEvaluationResult` payload
  content Finding's own Evidence doesn't carry, that agent's own
  design can justify a declared, scoped additional input the same way
  a Rule Type declares its Analyzer inputs — a decision for that
  future capability, not invented speculatively here.
- **Arbitrary repository/application access** (e.g. an Agent reading
  source files or calling other AIP services directly). Rejected
  explicitly per your instruction — would violate every established-
  contracts-only boundary this system has held since CSM Builder.

### 5. Multiple Recommendations: one invocation, one Recommendation; no conflict/precedence semantics

**Problem:** Does one Agent invocation produce one Recommendation? May
one Finding have multiple Recommendations? Does the framework need
conflict/precedence handling akin to CSM's `SubjectConflictMarker`?

**Decision:** One Agent invocation produces exactly one Recommendation.
A Finding **may** have any number of Recommendations — from the same
Agent invoked more than once, or from different registered Agents —
with **no conflict or precedence semantics** of any kind. Every
Recommendation referencing a given Finding remains independently
valid, retrievable, and unranked by the framework itself; if two
Recommendations disagree, both are simply present, exactly as
produced. This is a deliberate departure from CSM's own
`SubjectConflictMarker` precedent, justified by a real distinction
your instructions state directly: CSM's conflict marking exists
because competing *knowledge assertions* about the same fact need a
precedence rule to determine what is *effective* (a fact-correctness
question); Recommendations are **guidance, not authoritative fact** —
there is no "effective" Recommendation to compute, only a set of
independent suggestions a human consumer weighs themselves.

**Corollary — Agent invocation independence and concurrency:** because
invocations carry no conflict/precedence relationship (above) and
because each invocation reads exactly one Finding via `FindingSource`
and writes exactly one new Recommendation with no shared mutable state
between invocations (Decision 11's "Versioned, deterministic Agent
behavior at the contract level" framing — an Agent's own contract code
is ordinary, deterministic, versioned code; only its model-call step's
output is non-deterministic), Agent invocations are independent of one
another the same way Analyzer invocations were established as
independent one layer down (`define-analysis-framework` Decision 2)
and Rule evaluations one layer below that (`define-rule-framework`'s
`No Rule-to-Rule Composition`). The framework's contract therefore
permits concurrent or reordered invocation without affecting
correctness; a v1 implementation MAY execute invocations sequentially,
the same sequential-v1-behind-a-concurrency-permitting-contract choice
`define-analysis-framework` Decision 6 already made. This is not a new
decision, only the same independence property Decision 5 (above) and
Decision 11 already establish, read at the invocation-scheduling level
rather than the Recommendation-content level.

**Alternatives considered:**
- **Rank or mark one Recommendation per Finding as "primary."**
  Rejected — would require a precedence *policy* (most recent? highest
  Confidence? a specific Agent's priority?) this design has no
  principled basis to invent, and would misrepresent Recommendations
  as more authoritative than `project.md` §7's own Non-Goal
  ("unquestionable truth") permits treating them.
- **Deduplicate near-identical Recommendations.** Rejected — requires
  a content-similarity judgment this framework has no mechanical way
  to make reliably, and risks silently dropping a legitimately
  different, differently-worded suggestion — the same "nothing
  silently dropped" discipline this project holds to elsewhere.

### 6. Recommendation Confidence: Agent-produced, mandatory, payload-level

**Problem:** What does Recommendation Confidence mean, who sets it,
and how does it relate to Finding's own fixed Confidence?

**Decision:** Recommendation Confidence is **Agent-produced**: each
Agent invocation declares its own Confidence value for the
Recommendation it emits, sourced from whatever signal that Agent's own
logic has access to (a model's own expressed certainty, a heuristic
the Agent applies, or a fixed value if an Agent has no finer-grained
signal) — the framework does not compute, derive, or override it.
Confidence is **mandatory** on every Recommendation, keeping the field
uniformly present the way every other artifact's fields in this system
are either mandatory or explicitly fixed, never optional. Confidence
is retained as **payload-level content** (like Generation Provenance,
explicitly excluded from Recommendation Artifact Identity per Decision
3) — two Recommendations differing only in their declared Confidence
value are still the same kind of thing, but two separately-generated
Recommendations already have distinct identities via their Generation
Identifiers regardless, so this exclusion has no practical
identity-collision consequence.

This is explicitly, semantically distinct from Finding's own
Confidence (fixed/maximal, because Finding Model's own Decision 5
established that every v1 Finding traces to deterministic Rule
evaluation with no meaningful variation to express) — Recommendation
Confidence is the first place in this system a genuinely variable
Confidence value has an honest source, and this design does not
conflate the two fields despite their shared name.

**Alternatives considered:**
- **Framework-derived Confidence** (e.g. computed from some property
  of the Agent or its history). Rejected — the framework has no
  principled signal to compute this from that would be more honest
  than the Agent's own declaration; inventing one would be a
  fabricated precision this design should avoid.
- **Optional Confidence.** Rejected — breaks the uniform-shape
  discipline every prior artifact's fields hold to, and would leave
  in place a nullable branch that must be interpreted by every future
  reader.
- **Confidence as part of identity.** Rejected — content-derived
  identity was already rejected in Decision 3 for the same class of
  reason; Confidence is exactly the kind of variable content Decision
  3 excludes.

### 7. Validation and publishing: structural and referential integrity only, never substantive correctness

**Problem:** Does Recommendation need a validator/publisher gate like
every prior layer? If so, what can it mechanically check for
AI-generated content specifically?

**Decision:** Yes. A `RecommendationValidator` checks, before a
`RecommendationPublisher` ever calls `RecommendationStore.write`:
- **Referential integrity** — the referenced Finding (by Evaluation
  Identity) SHALL exist and SHALL already be published.
- **Agent identity/version consistency** — the declared producing
  Agent and its version SHALL correspond to a currently registered
  Agent.
- **Generation Provenance completeness** — the Generation Provenance
  record (Decision 3) SHALL be present and SHALL carry a non-empty
  model/provider identifier and a generation timestamp.
- **Required-field presence** — Confidence (Decision 6) and the
  Recommendation's own content/guidance field SHALL be present.
- **Isolation from deterministic facts** — a Recommendation SHALL NOT
  itself carry a mutated copy of, or a claimed update to, any CSM
  element, `AnalysisResult`, `RuleEvaluationResult`, or Finding field;
  structurally, the Recommendation type has no field capable of
  representing such a claim, and this check exists to keep that
  invariant enforced defense-in-depth, not merely by type-shape
  accident.

**What this validator explicitly does NOT do, per your instruction:**
it does not, and structurally cannot, evaluate whether a
Recommendation's guidance is substantively *correct*, *good*, or
*actionable* — that is not a mechanically checkable property, and
this design does not pretend a validator can establish it. This is a
deliberate, named boundary, not an oversight: it is the sharpest
instance of "Where Agent Framework cannot simply copy an established
pattern" above, because every prior validator's referential-integrity
check *was* effectively a correctness check (deterministic content
derived correctly from valid inputs is correct content); here, valid
inputs and correct structure say nothing about whether the guidance
itself is sound.

**Alternatives considered:**
- **No validation gate, trusting Agent implementations.** Rejected for
  the same reason every prior layer's own Decision rejected it — a
  future Agent implementation defect (a missing provenance field, a
  reference to an unpublished Finding) should fail loudly at publish
  time, not silently produce an untraceable Recommendation.
- **A validator that scores or gates on Confidence** (e.g. rejecting
  low-Confidence Recommendations). Rejected — this would smuggle a
  substantive-quality judgment into a structural gate, exactly what
  the "not substantive correctness" boundary above prohibits; a
  consumer is free to filter by Confidence themselves, but the
  framework's own publish gate should not.

### 8. Module and contract boundaries: `aip-ai` depends on `aip-core` only, via a new `FindingSource` contract; `RecommendationStore` stays in `aip-ai`

**Problem:** What does `aip-ai` depend on, and which parts of this
design's shape belong in `aip-core` versus `aip-ai` itself?

**Decision:**
- `aip-core` gains a fourth read contract, **`FindingSource`** —
  mirroring `RuleEvaluationResultSource`'s shape exactly: read a
  Finding by its Evaluation Identity, list Findings by (Logical
  Finding Identity or source CSM Snapshot identity). `aip-ai` depends
  on `aip-core` only — `FindingSource` and the shared `aip-core` types
  its own identity fields reference — and SHALL NOT depend on
  `aip-findings`, `aip-rules`, `aip-analysis`, or `aip-csm-builder`
  directly. The same multi-future-consumer reasoning
  `define-rule-framework` Decision 7 and `define-finding-model`
  Decision 11 each already gave applies again: Agent Framework is not
  necessarily the only future reader of Findings.
- **`RecommendationStore`** (the persistence abstraction for
  Recommendation, mirroring `FindingStore`/`RuleEvaluationResultStore`/
  `AnalysisResultStore`) is defined **within `aip-ai`**, not promoted
  to `aip-core`. This is a deliberate departure from the
  early-promotion pattern the three prior contracts each followed —
  justified because, unlike `CsmSnapshotSource`/`AnalysisResultSource`/
  `RuleEvaluationResultSource` (each promoted because a specific,
  already-named future consumer existed — Agent Framework itself, for
  the latter two), no capability named anywhere in `project.md` or
  this project's history is yet known to need to read Recommendations
  as an upstream input the way Rule Framework needed `AnalysisResult`s.
  Promoting it now would be a contract created merely for symmetry
  with the prior three, which your instructions explicitly prohibit.
  If a future capability is later named that needs to read
  Recommendations, promoting `RecommendationStore`'s shape into
  `aip-core` at that point repeats a migration this system has already
  shown is cheap to perform when it's actually needed.
- **Agent registration** (an `AgentRegistry`, mirroring
  `AnalyzerRegistry`/the Rule Type registry) lives entirely within
  `aip-ai` — no cross-module need has been named for it.
- **Generation Provenance** (Decision 3) is a field/record on
  Recommendation itself, defined within `aip-ai`'s own Recommendation
  type — not a separate contract.

**Alternatives considered:**
- **Promote `RecommendationStore` to `aip-core` now, for consistency
  with the prior three source contracts.** Rejected — explicitly,
  per your instruction not to create a contract merely for symmetry;
  no named consumer justifies it yet.
- **`aip-ai` depends on `aip-findings` directly**, since
  `FindingStore` is already an interface. Rejected — same reasoning
  `define-finding-model` Decision 11 and `define-rule-framework`
  Decision 7 each already gave and rejected for their own analogous
  cases.

```text
  aip-core
  - depended on directly by every module below; depends on none of them
  - gains FindingSource in this change

  aip-analyzer    - RU implementation, not yet built     (depends on aip-core only)
  aip-csm-builder - CSM Builder, implemented              (depends on aip-core only)
  aip-analysis    - Analysis Framework, not yet built     (depends on aip-core only)
  aip-rules       - Rule Framework, not yet built         (depends on aip-core only)
  aip-findings    - Finding Model, not yet built           (depends on aip-core only)

  aip-analyzer, aip-csm-builder, aip-analysis, aip-rules, and
  aip-findings are siblings - none depends on any of the others;
  all are deterministic, AI-free

  aip-ai          - Agent Framework, this change          (depends on aip-core only) <- this change
                    - the first AI-bearing module
                    - owns RecommendationStore and AgentRegistry internally,
                      not promoted to aip-core
                    - not a dependency of, and not depended on by, any of
                      the five deterministic modules above

  aip-cli / aip-server - future, depends on the above (not designed here)
```

### 9. Output boundary: Recommendation, and nothing further

**Problem:** Where does this capability's scope end?

**Decision:** This capability's output is **Recommendation only**.
Explicitly out of scope, per your instruction: Proposed Change, code
generation, remediation execution, deployment, verification of
remediation, and human-approval workflow. `project.md` §11's own
evolution order supports this directly — it stops at `Architecture
Compliance Agent`, naming no "Remediation" capability at all, and
`project.md` §4.10 describes Remediation as something AIP "should
eventually support," not something this generation of capabilities
builds.

**Alternatives considered:**
- **Include a "Proposed Change" artifact now**, since `project.md`
  §3.5's remediation workflow names it as Recommendation's immediate
  successor. Rejected — no specification exists yet for what a
  Proposed Change is (its own shape, identity, and validation model
  would need a full Design pass of its own), and `project.md`'s own
  evolution order does not name it as part of this capability; adding
  it now would be exactly the unforced scope expansion your
  instructions warn against.

### 10. AI-content isolation as a first-class boundary

**Problem:** Where do prompt templates, model/provider details, and
generation configuration live, such that they never leak into
deterministic domain artifacts?

**Decision:** All AI-specific mechanics — prompt template identifiers
or content, model/provider identifiers and versions, generation
configuration (temperature, sampling parameters, etc.) — live
**exclusively within Recommendation's own Generation Provenance
record** (Decision 3), itself defined and interpreted only within
`aip-ai`. `aip-core`, every CSM type, every `AnalysisResult`,
`RuleEvaluationResult`, and Finding field SHALL NOT carry any
AI-specific content of this kind — consistent with `CLAUDE.md`'s own
"the CSM must not contain AI prompts or AI-specific implementation
details" boundary, extended here to every deterministic artifact this
system has, not only the CSM. `project.md` §8's Security quality
attribute ("AI prompts/results must be handled securely") is satisfied
structurally by this same isolation: securing AI-specific content
becomes an `aip-ai`-scoped concern, not one every deterministic module
must separately account for.

**Alternatives considered:**
- **Record a reference to generation configuration on the Finding
  itself** (e.g. "this Finding has an AI-generated Recommendation
  available"). Rejected — this is precisely the kind of AI-awareness
  leaking into a deterministic artifact this boundary exists to
  prevent; a consumer wanting to know whether Recommendations exist
  for a Finding queries `RecommendationStore`/`FindingSource`
  together, not a flag on Finding itself.

### 11. Non-determinism: explicit guarantees, explicit non-guarantees

**Problem:** What, precisely, can Agent Framework promise, given that
its central artifact's content is not reproducible?

**Decision:** The framework guarantees:
- **Deterministic Recommendation Artifact Identity** (Decision 3) —
  every Recommendation has one stable, collision-free identity,
  durably retrievable.
- **Stable, complete provenance** — every Recommendation's Generation
  Provenance record faithfully and permanently records what produced
  it (Agent, version, model/provider, configuration, timestamp).
- **Reproducible input context** — the exact Finding (by Evaluation
  Identity) an Agent read is always determinable and durably
  retrievable, independent of whether re-running the Agent against it
  would produce the same output.
- **Versioned, deterministic Agent *behavior* at the contract level**
  — an Agent's own code (how it constructs a prompt/context from a
  Finding, which model it calls, how it packages the result) is
  ordinary versioned code, exactly like an Analyzer's or Rule Type's;
  only the model-call step's *output* is non-deterministic, not the
  surrounding contract or orchestration.
- **Immutability of the Recommendation record once published** — like
  every prior artifact, a published Recommendation is never mutated;
  a re-generation is a new, separately-identified Recommendation
  (Decision 3, Decision 5).

The framework explicitly does **NOT** guarantee:
- That two invocations of the same Agent version against the same
  Finding produce identical, or even similar, Recommendation content.
- That a Recommendation's content is deduplicated against other
  Recommendations for the same Finding (Decision 5).
- That Recommendation content is substantively correct (Decision 7).

This decision is this design's direct answer to the tension named in
"Where Agent Framework cannot simply copy an established pattern,"
above — stated as an explicit list rather than left to be inferred
from the other ten decisions individually.

## Cross-Capability Impacts

- **No modification to `canonical-software-model`, `define-analysis-
  framework`, `define-rule-framework`, or `define-finding-model`.**
  Checked explicitly, per instruction to surface rather than silently
  absorb any cross-capability gap. `Finding`'s own already-approved
  traceability (source CSM Snapshot identity, concerned CSM element
  identity, producing Rule identifier, RuleEvaluationResult identity
  set, both Finding identities) is sufficient for every Recommendation
  requirement this design makes; no upstream capability needed a new
  exposure the way `AnalysisView` did while specifying Rule Framework.
- **`define-finding-model`'s own `Recommendation`/`Remediation
  availability` Finding fields are now understood to be permanently
  unpopulated by design**, not merely deferred (Decision 2) — a
  clarification of intent, not a reopening of that decision's actual
  content (those fields were already unpopulated in v1; this design
  explains why that remains true going forward rather than being a
  temporary v1 gap).
- **`aip-core` gains one new type** (`FindingSource`, Decision 8) —
  additive, no change to any existing `aip-core` content.
- **A future Architecture Compliance Agent** (or any other
  `project.md` §5 agent) is now known to depend on: the Agent contract,
  `FindingSource`, and Recommendation's identity/provenance shape this
  design establishes. If such an agent needs content beyond what
  `Finding` already exposes, that is a scoped extension for that
  future capability's own Design to justify (Decision 4), not
  something assumed here.
- **A future remediation-execution capability** (not yet named in
  `project.md`) would depend on Recommendation's identity and content
  shape as its own starting input, the same relationship Finding has
  to RuleEvaluationResult today.

## Risks / Trade-offs

- [Recommendation Artifact Identity's Generation Identifier (Decision
  3) is an opaque token, not derived from meaningful content — two
  Recommendations can differ in every visible respect except that
  token] → Intentional: the token's only job is uniqueness per
  invocation, not meaning. Mitigation: Generation Provenance (Decision
  3) carries the actual explanatory content; a consumer never needs to
  interpret the Generation Identifier itself.
- [No deduplication or conflict resolution across multiple
  Recommendations for the same Finding (Decision 5) may produce
  visible noise — many similar-but-not-identical suggestions
  accumulating for one Finding over repeated runs] → Accepted v1
  trade-off, consistent with this project's repeated "nothing silently
  dropped, retention/curation deferred to a store or presentation
  concern" pattern (`define-rule-framework`'s own Decision 5 accepted
  the same class of trade-off for `PASS` outcome retention). Revisit
  if it becomes a measured problem.
- [`RecommendationValidator`'s explicit inability to check substantive
  correctness (Decision 7) means a well-formed but poor-quality
  Recommendation passes validation and is published] → Named
  explicitly as a structural limit, not a gap to close later by adding
  a smarter validator — `project.md` §7's own Non-Goal already frames
  AI-generated content as requiring human judgment, not framework-level
  quality gating.
- [Deferring `RecommendationStore`'s promotion to `aip-core` (Decision
  8) is a bet that no second consumer materializes before it would be
  convenient to have promoted it early] → Mitigation: same low-cost-
  if-wrong reasoning this project has accepted for every other
  contract-placement decision — the alternative (a contract sitting in
  `aip-core` unused by a second consumer) was explicitly what your
  instructions asked this design to avoid creating without
  justification, so the trade-off is deliberately taken the other way
  this time.
- [The narrow Findings-only consumption boundary (Decision 4) may
  prove insufficient once a concrete agent (e.g. Architecture
  Compliance Agent) is actually designed, if it needs richer context
  than Finding's own traceability carries] → Genuinely open, flagged
  rather than guessed at; Decision 4's own reasoning names the
  extension mechanism (a future agent's own scoped, justified
  additional declared input) rather than pre-building for a need not
  yet demonstrated.
