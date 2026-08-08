## Context

See `proposal.md` for motivation. See `explore.md` for the full exploration
this design formalizes.

This document is conceptual and technology-independent. It defines *what*
the Canonical Software Model (CSM) is, *what belongs in it and what does
not*, and *how it relates to adjacent models*. It does not choose a storage
technology, a graph library, a serialization format, or any Java type. Those
are implementation decisions for a later change, once a spec exists and an
implementation change is proposed against it.

Two capabilities are in play across the wider program but only one is in
scope here:

```
Software Repository Understanding  ───produces evidence───▶  Canonical Software Model
        (separate capability,                                  (THIS CHANGE)
         separate change)
```

This change defines the CSM side of that arrow — including the shape of
the interface the evidence side must eventually satisfy — but does not
define Repository Understanding itself.

## Goals / Non-Goals

**Goals:**
- Define the CSM's boundary: what it is responsible for representing, and
  where that responsibility ends.
- Define a small, closed, language-neutral conceptual vocabulary (entity
  kinds and relationship types) sufficient for architecture, design, risk,
  and compliance analysis.
- Define how raw repository evidence becomes CSM knowledge, and how that
  knowledge stays traceable back to its evidence.
- Define a uniform provenance and confidence model so every CSM element's
  trustworthiness is explicit and machine-readable.
- Define how the CSM interfaces with, but stays separate from, the
  Repository Evidence Model, the Runtime Model, and the Policy/Rule Model.
- Define what incremental updates, multi-repository systems, and CSM
  versioning require structurally, without specifying mechanisms.

**Non-Goals:**
- Choosing a data structure, storage engine, schema language, or file
  format for the CSM.
- Defining the Java type system, package layout, or any class design.
- Designing the Repository Understanding capability itself (separate
  change).
- Designing the Policy/Rule language, the Finding model, or any specific
  analysis Agent (later capabilities per `project.md` §11).
- Enumerating the complete, final set of vocabulary kinds — this design
  fixes the *categories* and the *extension mechanism*, not an exhaustive
  catalog (that belongs in the spec).

## Decisions

### 1. CSM Boundary

**Problem:** Without an explicit boundary, the CSM tends to absorb anything
"about the software" — source text, config values, infrastructure detail,
policy, telemetry — becoming an unbounded, language-coupled, constantly
invalidated blob.

**Options:**
- (A) CSM as a comprehensive system-of-record for everything discoverable
  about a system (evidence, structure, policy, runtime, business context
  all merged).
- (B) CSM as a thin structural/relational graph, with adjacent concerns
  kept in separate models that the CSM references by identity.
- (C) No explicit CSM at all — each analyzer keeps its own ad-hoc
  in-memory representation, re-derived per run.

**Trade-offs:**
- (A) maximizes convenience for any single query at the cost of coupling
  every future capability to one giant, language-sensitive, fast-changing
  structure; violates language independence and incremental analysis.
- (C) avoids upfront design cost but makes cross-analyzer reasoning,
  explainability, and reproducibility (project.md Reliability) impossible,
  since there is no shared, stable representation to reference from a
  Finding.
- (B) requires disciplined interface design between models but keeps each
  model's concerns, change cadence, and ownership separate.

**Recommendation:** (B). The CSM is bounded to **structural and relational
facts about a software system that are needed for cross-cutting
architecture, design, risk, and compliance reasoning, expressed in
language-neutral terms**. Anything that is evidence for those facts, a rule
evaluated against those facts, an observation of those facts at runtime, or
business/organizational context about those facts, lives in an adjacent
model and is referenced by stable identity, not embedded.

### 2. CSM Responsibilities

**Problem:** "Boundary" says what's excluded; the CSM also needs a positive
statement of what it exists to do, so later specs can be checked against
intent rather than just against exclusion rules.

**Options:**
- (A) CSM as a pure structural inventory (what exists, containment only).
- (B) CSM as structure + relationships (what exists and how it connects) +
  provenance metadata, but no interpretation.
- (C) CSM as structure + relationships + provenance + interpretive layer
  (Architecture Components, boundaries) with explicit provenance tagging
  distinguishing discovered from declared/inferred content.

**Trade-offs:**
- (A) is safest and cheapest but insufficient — architecture analysis is
  fundamentally about relationships (dependency direction, layering), not
  inventory alone.
- (B) supports dependency/layering analysis but stops short of representing
  the "component" and "boundary" concepts that policies are actually
  written against, pushing that translation into every rule or agent
  individually — duplicated, inconsistent effort.
- (C) centralizes the discovered-structure-to-architectural-concept mapping
  once, in the model, with provenance intact — more upfront design, but
  removes duplicated ad-hoc mapping later and keeps the distinction between
  fact and interpretation visible everywhere the mapping is used.

**Recommendation:** (C). The CSM is responsible for:
1. Representing the structural inventory of a system (containment
   hierarchy) in language-neutral terms.
2. Representing relationships between structural elements (dependency,
   implementation, exposure, integration).
3. Representing architecture-level groupings (Components) and boundaries
   over that structure, whether declared or inferred.
4. Carrying provenance and confidence for every element that is not raw
   discovered fact.
5. Exposing stable identity for its elements so adjacent models (evidence,
   runtime, policy, business) can reference them without being embedded in
   them.

It is explicitly **not** responsible for: storing evidence bodies,
evaluating policy, collecting runtime telemetry, or owning business/org
data (see §3).

### 3. What Explicitly Does NOT Belong in the CSM

**Problem:** Boundary and responsibilities are abstract until tested
against concrete categories of information that are tempting to fold in.

**Decision (not a trade-off — a direct exclusion list, justified per item):**

| Excluded | Why | Where it actually lives |
|---|---|---|
| Raw source text, full ASTs, method bodies | Implementation detail, language-specific, high-churn, not needed for structural reasoning beyond signatures | Repository Evidence Model, referenced by file/location |
| Configuration values, secrets | Sensitive, environment-specific, not structural | Configuration/evidence store; CSM holds only the fact of a reference (`reads-config`, `references-secret`) |
| Full infrastructure-as-code detail (security groups, IAM policies, network rules) | Large adjacent graph with its own concerns; only deployment topology matters structurally | Infrastructure model; CSM holds a thin "Deployable Unit" node and a mapping relationship |
| Governance policies and architecture constraints | Rules are evaluated *against* the CSM; embedding them creates a circular, versioning-incompatible coupling between rule changes and structure snapshots | Policy/Rule Model (§16) |
| Runtime telemetry streams (traces, metrics, logs, incident history) | Different cadence (continuous) and different evidence species (observational, not structural); would turn a reproducible snapshot into a live sink | Runtime Model (§16), joined by identity key |
| Business org charts, team rosters, personal contact information | Identity/HR concern, not architecture; changes on an organizational cadence | External identity/org system, referenced by owner ID |
| Findings | A Finding is a *product* of evaluating policy against a CSM snapshot — not part of the model it was evaluated against | Finding Model (future capability) |
| Historical trend/drift analytics | A derived comparison *across* CSM snapshots, not a property of any one snapshot | Analytics layer over CSM snapshot history |

### 4. CSM Conceptual Vocabulary

**Problem:** The CSM must represent Java, and eventually other languages,
without becoming shaped like any one of them.

**Options:**
- (A) Vocabulary mirrors one language's constructs (e.g., Java-shaped:
  class, interface, package) and other languages are shoehorned in later.
- (B) Vocabulary is a small, closed set of abstract structural kinds and
  relationship types, deliberately chosen to be a common denominator across
  mainstream languages, with an explicit escape hatch for constructs that
  don't map cleanly.
- (C) Vocabulary is fully open/extensible — any analyzer can register new
  kinds and relationship types as needed.

**Trade-offs:**
- (A) is fastest to start (matches the initial Java-only implementation)
  but directly violates language independence (project.md §3.4) and forces
  a breaking redesign at the first non-Java analyzer.
- (C) maximizes analyzer flexibility but destroys the value of a *canonical*
  model — policies, rules, and cross-language comparison all depend on a
  known, closed vocabulary to be written against.
- (B) requires more upfront thought to choose the right common-denominator
  set, but preserves both language independence and canonicity; the escape
  hatch (an opaque, per-node "native evidence" attribute bag) absorbs
  anything that doesn't fit without expanding the schema.

**Recommendation:** (B). The vocabulary has two parts:
- **Structural kinds** (closed, versioned enumeration): containment levels
  (Repository, Project, Module, Package) and code-structure concepts (Type,
  Interface, Method) — chosen because they exist, under some name, in every
  mainstream language AIP targets.
- **Relationship types** (closed, versioned enumeration): containment
  (`contains`), dependency (`depends-on`), implementation (`implements`/
  `extends`), invocation (`calls`), exposure (`exposes`/`consumes`),
  integration (`integrates-with`), composition (`composed-of`), and mapping
  relationships used by business context (`realizes`, `owns`,
  `belongs-to-domain`) and boundaries (`constrains`).

Each language analyzer is responsible for mapping its native constructs
onto this vocabulary. A construct that doesn't map is preserved as an
opaque **native evidence attribute** on the nearest matching node — it is
never used to justify adding a new structural kind casually. Extending the
vocabulary itself is a deliberate, versioned change to the CSM spec (§20),
not something an analyzer does unilaterally.

The exact, exhaustive list of kinds and relationship types (and their
attributes) is a specification-level decision, not fixed further here.

### 5. Repository Evidence → CSM Transformation

**Problem:** Evidence is language-specific, per-file, and low-level.
The CSM is language-neutral, cross-cutting, and relational. Something has
to perform that transformation, and its behavior needs to be a defined
contract even though this change doesn't design the evidence side.

**Options:**
- (A) Direct, ad-hoc translation embedded in each analyzer with no shared
  contract.
- (B) A defined transformation *stage* (conceptually, a "CSM builder")
  that consumes a defined Repository Evidence Model and produces CSM
  elements, with the mapping rules externalized per language/analyzer.
- (C) The CSM and evidence are the same structure; analyzers write directly
  into the CSM.

**Trade-offs:**
- (A) is simplest short-term but reproduces the language-coupling problem
  §4 solves, one analyzer at a time, and provides no place to enforce
  provenance tagging consistently.
- (C) collapses the two capabilities this program deliberately separates
  (Repository Understanding vs. CSM), reintroducing the exact conflation
  the Explore phase identified as the central risk.
- (B) keeps the two capabilities cleanly separable and testable
  independently (an evidence-producing analyzer can be validated against
  the Evidence Model; the builder can be validated against the CSM spec
  independently of any one language).

**Recommendation:** (B). Conceptually:

```
Language Analyzer  ──▶  Repository Evidence Model  ──▶  CSM Builder (transformation stage)  ──▶  CSM
 (language-specific)      (language-specific facts,       (applies vocabulary mapping +          (language-
                           still shaped by the source       provenance/confidence rules)           neutral)
                           language/tool)
```

The transformation stage's responsibilities: map evidence onto the closed
vocabulary (§4), attach provenance (§8) marking the result `discovered`,
attach confidence (§9) where the mapping is heuristic rather than
mechanical (e.g., stereotype inference), and preserve a traceability link
from every CSM element back to the evidence/location that produced it.
The Repository Evidence Model itself — its shape, and the analyzer
capability that produces it — is designed in the separate Repository
Understanding change; this design only fixes the shape of the interface
the CSM side depends on (evidence in, CSM elements + provenance out).

### 6. Evidence vs. Knowledge

**Problem:** "Evidence" and "knowledge" are used almost interchangeably in
casual discussion but need to mean different things for the model to be
coherent.

**Decision:** **Evidence** is a raw, language/tool-specific observation
(a line of source, an import statement, a config file entry, a deployment
manifest fragment) — it is not interpreted and does not carry architectural
meaning by itself. **Knowledge** is a CSM element (a node or a relationship)
— an interpreted, language-neutral, provenance-tagged fact or assertion
built *from* one or more pieces of evidence (or from a declaration, or from
an inference). The CSM contains knowledge, never evidence directly; it
holds references to the evidence that justifies each piece of knowledge.
This distinction is what makes §3's exclusion of raw evidence coherent
rather than arbitrary, and it is the reason every CSM element needs a
provenance tag (§8) — provenance records *which kind of act* (discovery,
declaration, inference, external assertion) turned evidence into knowledge.

### 7. Observed vs. Declared vs. Inferred Knowledge

**Problem:** Not all knowledge is produced the same way, and treating it as
uniform risks a low-confidence guess being consumed with the same trust as
a mechanically verified fact.

**Options:**
- (A) Single undifferentiated "knowledge" bucket; trust is left to
  whichever consumer chooses to think about it.
- (B) Three explicit categories — **observed** (mechanically derived from
  evidence, e.g. an import edge), **declared** (asserted by a human or an
  authoritative external source, e.g. an architect naming a Component),
  and **inferred** (produced by AIP's own heuristics/AI reasoning over
  evidence, e.g. a guessed Component boundary) — each tagged explicitly on
  every CSM element.
- (C) Two categories only (fact vs. non-fact), collapsing declared and
  inferred together.

**Trade-offs:**
- (A) is simplest but directly contradicts project.md §3.8 and §6, which
  require observed facts and AI-derived conclusions to remain
  distinguishable through to Findings.
- (C) is closer but conflates two very different trust profiles: a
  declared fact from an architect is authoritative-but-unverified, while
  an inferred fact from AIP is a probabilistic guess — conflating them
  would mean a policy engine can't choose to, say, only fail a build on
  observed+declared violations and merely warn on inferred ones.
- (B) costs one more category to track everywhere but preserves exactly the
  distinctions project.md requires and gives downstream consumers (rules,
  Findings, AI reasoning) the granularity to react differently to each.

**Recommendation:** (B). This three-way category is the **provenance
axis** used throughout the CSM (formalized in §8).

### 8. Provenance Model

**Problem:** Provenance needs to be more than a single flag if it is to be
useful for explainability (project.md §3.3) — a consumer needs to know not
just "how was this known" but "from what, and how strongly."

**Options:**
- (A) A single `provenance` enum value per element: `observed | declared |
  inferred`.
- (B) A structured provenance record per element: category (from §7),
  source reference (which evidence, which declaration, which
  inference run/model), and a timestamp, kept independent from — but
  paired with — a separate confidence value (§9).
- (C) No structured provenance; provenance is reconstructed after the fact
  from audit logs.

**Trade-offs:**
- (A) is minimal and satisfies the letter of "distinguish provenance" but
  loses traceability — a Finding referencing an inferred element couldn't
  point back to *why* it was inferred, undermining explainability.
- (C) defers cost but makes explainability an afterthought bolted onto logs
  rather than a property of the model itself, and breaks under incremental
  re-analysis (old audit entries become ambiguous once elements are
  re-derived).
- (B) costs a small, fixed structure per element but directly satisfies
  project.md §3.3 (every finding must explain what/where/why) by letting a
  Finding's explanation walk from the finding, to the CSM element, to the
  provenance record, to the originating evidence or declaration.

**Recommendation:** (B). Every CSM element carries a provenance record with
at minimum: **category** (observed/declared/inferred), **source
reference** (a stable pointer to the evidence, the declaring
actor/document, or the inference process that produced it), and
**timestamp** (when this knowledge was established, needed for §17
incremental updates and §20 versioning). Provenance is a property of the
element, not a side-channel log — an element without a defensible
provenance record is not valid CSM content.

### 9. Confidence Model

**Problem:** Within `inferred` (and sometimes `declared`, e.g. a
possibly-stale external assertion) knowledge, not all guesses are equally
trustworthy, and rule evaluation needs a way to react proportionally.

**Options:**
- (A) No confidence value — treat all inferred knowledge as equally
  uncertain.
- (B) A numeric confidence score (e.g., a 0–1 range) attached wherever
  provenance is `inferred`, with `observed` and `declared` knowledge
  implicitly treated as fully confident (not scored).
- (C) A qualitative confidence tier (e.g., High/Medium/Low) instead of a
  numeric score.

**Trade-offs:**
- (A) is simplest but forces every consumer to treat "AIP is 95% sure this
  is the Order component" identically to "AIP has a weak guess" —
  insufficient for prioritization-driven use cases (project.md §4.9,
  §4.10) that explicitly need confidence to gate automation level.
- (B) gives fine-grained control for future automation-gating (e.g.,
  "only auto-remediate above 0.9 confidence") but risks false precision —
  a numeric score implies more calibration than early heuristics can
  honestly provide.
- (C) is coarser but honest about the current state of the art for
  heuristic/AI-derived architectural inference, and is easier to reason
  about in policy ("treat Low-confidence inferred components as advisory
  only").

**Recommendation:** (C) for this design stage, with room to refine to (B)
later without breaking the model: confidence is a **qualitative tier**
attached to `inferred` knowledge (and optionally to `declared` knowledge
sourced from an unverified external system). `observed` knowledge does not
carry a confidence tier — it is treated as fact, subject only to normal
evidence-quality caveats (e.g., an analyzer bug), which is a data-quality
concern, not a modeling one. The exact tier scheme (labels, and whether it
later becomes numeric) is deferred to the specification, since it doesn't
change this design's structure, only its granularity.

### 10. Architecture Component Representation

**Problem:** Identified in Explore as the pivot point of the whole model —
Components are simultaneously a grouping over discovered structure and an
architectural interpretation that may be declared or inferred.

**Options:**
- (A) Component is just another containment level, no different in kind
  from Package or Module.
- (B) Component is a distinct CSM concept: a named grouping *over* existing
  structural elements (Modules/Packages), always carrying a provenance tag
  (declared or inferred, never "observed" since a Component boundary is
  never a raw mechanical fact), with confidence attached when inferred.
- (C) Components are excluded from the CSM entirely and modeled purely in
  the Policy/Rule layer as ad-hoc groupings referenced by each policy.

**Trade-offs:**
- (A) is simple but wrong: unlike Package/Module (which are mechanically
  derived from build/AST evidence), a Component boundary is *asserted or
  guessed*, not observed — collapsing the distinction would let an
  unreviewed inference silently gain the same trust as a build-file fact.
- (C) avoids the modeling problem by punting it to policy, but then every
  policy that references "the Order Service" has to redefine that grouping
  itself, and different policies could disagree about what the Order
  Service even contains — no single source of truth for a concept that
  most policies are actually written against.
- (B) requires carrying provenance/confidence at the Component level
  specifically (not just inheriting from its members), which is a small
  additional bookkeeping cost, but gives every consumer one authoritative,
  trust-annotated definition of each component.

**Recommendation:** (B). An Architecture Component is a first-class CSM
concept: a named grouping composed of one or more structural elements
(Modules/Packages, and transitively their Types), always carrying a
provenance tag of `declared` or `inferred` (never `observed`), with a
confidence tier when `inferred`. Multiple Components may exist with
different provenance for the same underlying structure (e.g., an inferred
guess pending human confirmation, which upon confirmation is superseded by
a `declared` Component) — reconciliation of competing definitions is a
specification-level rule, not fixed further here.

### 11. Architectural Boundaries

**Problem:** A boundary is the thing a layering/dependency policy actually
enforces ("Component A must not depend on Component B") — it needs a CSM
representation distinct from the Components it relates, and distinct from
the policy that declares it.

**Options:**
- (A) Boundaries live only in the Policy/Rule Model, expressed purely as
  rule text referencing Component names by string.
- (B) Boundaries are a CSM relationship type (`constrains` or similar)
  between Components, declared as CSM content, which policies then
  reference and evaluate rather than re-declare.
- (C) Boundaries are inferred automatically from observed dependency
  patterns and never separately declared.

**Trade-offs:**
- (A) is simplest but loses a structural place to represent "this boundary
  exists and is intentional" independent of any one policy's phrasing,
  making it hard to ask "what boundaries does this system have" without
  parsing every policy.
- (C) is appealing (fully automatic) but confuses *description* (a boundary
  that architecturally should exist) with *observation* (current
  dependencies happen to respect or violate it) — a boundary is inherently
  a statement of intent, not a mechanical fact, so it cannot be purely
  observed.
- (B) keeps the boundary as CSM content (queryable, versioned, provenance-
  tagged like a Component) while leaving *enforcement* (pass/fail
  evaluation) to the Policy/Rule Model, consistent with §15's separation.

**Recommendation:** (B). An Architectural Boundary is a CSM relationship
between two or more Components (or Component and external system),
carrying a provenance tag (`declared` or `inferred`, following the same
logic as §10) describing the *intended* relationship constraint (e.g.,
"must not depend on", "must only communicate via"). The Policy/Rule Model
references these boundaries — and may additionally declare constraints not
yet captured as CSM boundaries — but the act of *evaluating* compliance
happens in the Policy/Rule Model, not in the CSM (§15, §16).

### 12. Business Context

**Problem:** Explored as a mapping-layer question: business concepts
(Capability, Domain, Ownership, Criticality) matter for prioritization but
must not be fused into technical identity.

**Options:**
- (A) Fuse business attributes directly onto technical nodes (e.g., a
  `criticality` field on Module).
- (B) Model business concepts as their own thin CSM nodes, connected to
  technical nodes via explicit mapping relationships (`realizes`, `owns`,
  `belongs-to-domain`), each independently provenance-tagged (typically
  `declared`, since these are externally asserted).
- (C) Keep business context entirely outside the CSM, in a separate system
  joined only at Finding-generation time.

**Trade-offs:**
- (A) is convenient for simple queries but couples technical structure's
  lifecycle to business context's lifecycle — a reorg or roadmap change
  would force touching technical nodes, and a re-analysis of code would
  risk clobbering business metadata that has nothing to do with the code
  change.
- (C) cleanly separates lifecycles but forces every consumer that wants
  "risk-weighted findings" to perform an external join, duplicated per
  consumer, and makes it hard to query the CSM itself for "what
  capabilities does this system implement."
- (B) keeps lifecycles independent (§17 incremental analysis benefits
  directly — a business remap doesn't invalidate code-derived knowledge and
  vice versa) while still letting the CSM answer business-relevant
  queries directly via traversal.

**Recommendation:** (B). Business Capability, Domain, Ownership, and
Criticality are modeled as their own CSM nodes/attributes, connected to
technical nodes (Component, Module) via dedicated relationship types,
carrying `declared` provenance (since they are externally asserted, not
discovered) — see also §6's evidence/knowledge distinction: an external
assertion is knowledge without being evidence-derived.

### 13. External Systems

**Problem:** AIP needs to represent that a dependency crosses a boundary
into something it does not and should not attempt to model internally
(e.g., a third-party SaaS, an external database, a partner API).

**Options:**
- (A) Attempt to model external systems with the same fidelity as internal
  ones (Types, Methods, etc.), inferring structure where possible.
- (B) Model external systems as opaque **boundary nodes**: attributed on
  the exterior (name, kind, criticality, integration protocol, owner) but
  with no represented interior.
- (C) Do not represent external systems in the CSM at all; treat any
  outbound call as an unresolved/dangling relationship.

**Trade-offs:**
- (A) is usually infeasible (no access to the third party's source) and
  even when partially feasible (e.g., a shared internal service with its
  own repo) duplicates work that belongs to *that* system's own CSM, not
  this one.
- (C) loses the ability to reason about blast radius, vendor risk, or
  SPOFs — exactly the risk-analysis capability project.md §4.5/§4.6
  requires.
- (B) gives risk/compliance analysis what it needs (criticality, ownership,
  protocol) without pretending to model what AIP cannot see.

**Recommendation:** (B). External System is a CSM node kind with a closed,
mostly-opaque attribute set; internal Components/Modules connect to it via
`integrates-with` / `calls-external` relationships. Where an "external"
system is actually another internally-owned repository (a service AIP
could analyze separately), it is still represented as a boundary node from
*this* system's CSM perspective — cross-system federation is a §18
concern, not solved by inlining one CSM into another.

### 14. Dependencies and Relationships

**Problem:** Dependency information is the single most-used input for
architecture rules (layering, circularity), so its representation needs
enough nuance to support real policies without becoming its own miniature
type system.

**Options:**
- (A) A single generic `depends-on` edge type with no further qualification.
- (B) A small set of qualified relationship types/attributes: direction
  (implied by edge), dependency *kind* (e.g., compile-time, runtime,
  test-only), and strength/multiplicity where discoverable, layered on top
  of the same closed vocabulary approach as §4.
- (C) A fully generic, attribute-bag relationship model where any analyzer
  can attach arbitrary key/value metadata to any edge.

**Trade-offs:**
- (A) is too coarse — "must not depend on" policies often specifically mean
  compile-time/runtime dependencies, not test-only ones, so a single edge
  type would produce false positives.
- (C) maximizes flexibility but reintroduces the language-coupling and
  non-canonicity risk §4 was designed to avoid, at the relationship level
  instead of the node level.
- (B) mirrors the node vocabulary decision: a closed set of qualified
  relationship types, extensible only through the same deliberate,
  versioned process as node kinds.

**Recommendation:** (B), consistent with §4: relationships are typed from
the same closed, versioned vocabulary, with a small set of qualifying
attributes (dependency kind, and provenance/confidence per §7–§9, since a
relationship can itself be `observed`, `declared`, or `inferred` — e.g., an
inferred Component-level dependency rolled up from many observed Type-level
dependencies).

### 15. Architecture Constraints

**Problem:** Constraints look like they belong "in" the model they
constrain, but Explore concluded they are a different category of
information entirely (intent/rule, not fact).

**Decision:** Architecture Constraints and Governance Policies are **not**
CSM content (reaffirming Explore's conclusion and §3's exclusion list).
They live in a separate Policy/Rule Model, expressed in terms of CSM
vocabulary — referencing node kinds, relationship types, and specific
Components/Boundaries by stable identity — and are *evaluated against* a
CSM snapshot to produce Findings. This is what makes "policy as code"
(project.md §3.7) and a stable CSM schema simultaneously possible: policies
version and evolve independently of any one system's structural snapshot,
and a CSM snapshot doesn't need to change when an unrelated policy is
edited.

### 16. Relationship to Adjacent Models

**Problem:** Three adjacent models have come up repeatedly (Repository
Evidence, Runtime, Policy/Rule); their interface *shape* needs to be fixed
even though none of them is designed in this change.

**Decision — for each adjacent model, the interface is a reference, not an
embedding:**

```
                     ┌─────────────────────────┐
                     │  Repository Evidence      │
                     │  Model (separate change)  │
                     └────────────┬─────────────┘
                                  │ produces evidence,
                                  │ consumed by the CSM
                                  │ Builder (§5)
                                  ▼
┌───────────────┐        ┌─────────────────────┐        ┌───────────────────┐
│  Runtime Model │◀──────▶│  Canonical Software  │◀──────▶│  Policy/Rule Model  │
│ (observability)│ join   │       Model           │ read   │ (future capability) │
└───────────────┘  by ID └─────────────────────┘        └───────────────────┘
   enriches edges/nodes      the structural/relational        expressed over CSM
   with confirmation          graph, with provenance            vocabulary; produces
   signals; never             (§8) and confidence (§9)          Findings by evaluating
   ingested wholesale         attached throughout                against a CSM snapshot
```

- **Repository Evidence Model**: upstream of the CSM; the CSM Builder (§5)
  consumes it and never the reverse. The CSM never holds evidence bodies,
  only references back to them (§6).
- **Runtime Model**: joined via a stable runtime-identity key on relevant
  CSM nodes (e.g., a Component's deployed service name); may enrich
  specific CSM relationships with observational confirmation (e.g., "this
  static dependency was also observed at runtime") but the telemetry
  stream itself is never stored in the CSM (§3).
- **Policy/Rule Model**: downstream/parallel; reads the CSM (structure,
  Components, Boundaries) and produces Findings. The CSM has no
  dependency on the Policy/Rule Model — a CSM snapshot must be valid and
  usable even if no policies exist yet.

### 17. Incremental Updates to the CSM

**Problem:** Re-deriving the entire CSM from scratch on every change is
wasteful and breaks the "large repositories do not require complete
analysis for every change" requirement (project.md §8 Performance).

**Options:**
- (A) Always rebuild the full CSM from all evidence on every analysis run.
- (B) Give every CSM element stable identity that persists across runs,
  scope re-derivation to the subgraph affected by changed evidence (using
  change information from the Repository Evidence Model, e.g. changed
  files/modules), and leave unaffected elements — including their
  provenance/confidence — untouched.
- (C) Version the CSM only at the whole-snapshot level (full snapshots,
  no element-level identity), relying on snapshot diffing after the fact.

**Trade-offs:**
- (A) is simplest to reason about but does not scale and actively
  contradicts an explicit quality attribute.
- (C) is simpler than (B) to build first but makes it impossible to answer
  "did this specific Component change" without diffing entire snapshots,
  and complicates keeping business-context mappings (§12) stable across
  unrelated code changes, since there's no element to anchor them to
  across snapshots.
- (B) costs more upfront (an identity scheme that survives re-analysis)
  but is the only option that supports both incremental re-derivation and
  stable anchors for adjacent-model references (§16) and business mappings
  (§12) that must not be disturbed by unrelated code changes.

**Recommendation:** (B). This requires, at the design level (mechanism
deferred to spec/implementation):
- **Stable element identity** that does not change merely because the
  CSM was regenerated (as opposed to identity that changes because the
  underlying structure genuinely changed).
- **Change-scoping input** from the Repository Evidence Model (which
  evidence changed since the last build) so the CSM Builder can limit
  re-derivation to the affected subgraph plus anything dependent on it.
- **Layered volatility**: code-derived (`observed`) knowledge is expected
  to change frequently; declared/business knowledge (§12) changes on a
  different cadence and must not be invalidated by unrelated code-derived
  changes.

### 18. Multi-Repository Systems

**Problem:** A real system is often composed of many repositories (a
microservice architecture, a polyrepo). Explore left open whether Repository
is the CSM root or merely a contained node.

**Options:**
- (A) One CSM per repository, strictly rooted at Repository, with no
  native concept of a system spanning multiple CSMs.
- (B) A CSM can represent a "System" as a root concept above Repository,
  composed of one or more Repositories, allowing a single CSM to span a
  polyrepo system.
- (C) Each repository produces its own CSM, and a separate, higher-level
  federation layer composes multiple CSMs into a system-level view without
  merging them into one CSM.

**Trade-offs:**
- (A) is simplest and matches the initial single-repository focus
  (project.md's evolution order starts with Repository Understanding
  singular) but cannot represent cross-repository dependencies/boundaries
  natively, which are common and architecturally significant in
  microservice systems.
- (B) solves multi-repository representation directly but means every CSM
  consumer must handle a System root even when analyzing a single
  repository, and raises identity/versioning complexity (repositories
  evolve independently; a System-level snapshot must reconcile
  independently-changing Repository subgraphs).
- (C) keeps each repository's CSM independently buildable, versionable,
  and incrementally updatable (§17) — matching each repo's own release
  cadence — while still enabling system-level views through federation
  that reads multiple CSMs and their External System boundary nodes (§13)
  to connect them, treating "another repo I don't own in this analysis
  run" the same way as any other external system until federated.

**Recommendation:** (C), consistent with §13 and §17: each repository
produces its own CSM, independently versioned and incrementally updated.
Cross-repository relationships are represented, from each side, as
External System boundary references (§13) at minimum; a dedicated
federation capability that resolves matching boundary references across
multiple CSMs into a system-level view is a plausible future capability,
but is **not** designed here — this decision only commits to CSM identity
being scoped per-repository so that future federation remains possible
without redesigning the CSM itself.

### 19. Language Independence

**Problem:** This cuts across nearly every other decision, so it is
restated here as its own decision to make the enforcement mechanism
explicit rather than aspirational.

**Decision:** Language independence is achieved structurally, not by
policy statement alone, through the combination of: (a) the closed,
versioned vocabulary (§4, §14) that all analyzers map onto; (b) the native
evidence attribute bag that absorbs anything language-specific that
doesn't fit the vocabulary, without expanding the CSM schema per language;
and (c) the separation of the CSM Builder (§5) from the Repository Evidence
Model, meaning a new language requires a new analyzer plus a new
evidence-to-CSM mapping, never a change to the CSM's own structural
definition. A CSM consumer (a rule, an agent) that only understands the
closed vocabulary can operate identically regardless of which language(s)
produced the underlying evidence.

### 20. Versioning/Evolution of the CSM

**Problem:** Both the *schema* (the vocabulary itself) and *instances*
(a particular system's CSM snapshot) need an evolution story, and these
are different problems.

**Options for schema evolution:**
- (A) The vocabulary is fixed permanently at spec time; any new concept
  requires a breaking replacement of the CSM capability.
- (B) The vocabulary is explicitly versioned; additions (new kinds,
  relationship types, attributes) are backward-compatible by default, and
  any change that would alter the meaning of existing elements is a
  deliberate, called-out breaking change requiring its own change proposal.

**Options for instance evolution:**
- (A) Only the latest CSM snapshot for a repository is retained.
- (B) CSM snapshots are retained historically (or at least identifiable by
  version/timestamp), enabling drift detection and reproducibility audits
  over time, consistent with project.md's Reliability quality attribute
  (repeatable results for the same inputs).

**Trade-offs:** A fixed-forever schema (schema option A) is unrealistic
given this is explicitly the *first* formal CSM design and Explore already
surfaced multiple deferred vocabulary questions (§4, open questions below);
it would force a breaking rewrite at the first real gap. Retaining only the
latest instance (instance option A) is cheaper but forecloses drift
detection and incremental-analysis history (§17) and undermines
reproducibility audits.

**Recommendation:** Schema option (B) and instance option (B). The CSM
specification itself carries a version; additive changes (new kind, new
relationship type, new optional attribute) are non-breaking and can be
adopted by existing snapshots without re-derivation; changes that alter
the meaning or identity semantics of existing elements are breaking and
require an explicit change proposal, following the same OpenSpec workflow
used for this change. CSM snapshots are versioned/timestamped artifacts,
not overwritten in place, enabling both incremental analysis (§17) and
longitudinal drift analysis as a future capability built *on* the CSM
rather than *in* it.

## Risks / Trade-offs

- [Closed vocabulary proves too narrow for a language with a fundamentally
  different structural model (e.g., a functional language with no classes)]
  → Mitigation: the native-evidence attribute bag (§4, §19) absorbs
  unmapped constructs without forcing a premature schema change; a genuine
  gap becomes a deliberate, versioned vocabulary addition (§20), not a
  redesign.
- [Provenance/confidence bookkeeping adds overhead to every analyzer and
  every CSM element] → Mitigation: judged necessary in Explore and
  reaffirmed here — the alternative (silent conflation of fact and
  inference) directly contradicts project.md §3.8/§6 and would surface as
  a much costlier trust problem once AI-assisted remediation is introduced.
- [Deferring an exhaustive vocabulary catalog and a numeric confidence
  scheme to the specification stage could let underspecified areas leak
  into implementation] → Mitigation: both are flagged as open questions
  below and should be explicitly resolved in `specs/canonical-software-model/spec.md`
  before implementation begins, not discovered during coding.
- [Per-repository CSM scoping (§18) delays multi-repository/system-level
  analysis capability] → Mitigation: accepted deliberately — Explore and
  this design both conclude federation is a distinct, later capability;
  building it prematurely risks coupling CSM identity to a federation
  scheme that isn't yet validated by a single-repository implementation.
- [Architecture Component/Boundary provenance rules (declared vs. inferred,
  and how conflicting definitions are reconciled) are specified only at
  the concept level here, not as concrete resolution rules] → Mitigation:
  flagged as an open question; must be resolved in the spec, since it
  affects concrete requirement wording, not just internal design.

## Migration Plan

Not applicable in the infrastructure/deployment sense — this is a
greenfield design with no existing CSM implementation to migrate from.
Sequencing note: this change should reach spec before the Repository
Understanding change reaches implementation, since Repository Understanding
should be built against the evidence-to-CSM interface shape defined here
(§5, §16), even though Repository Understanding is specified and
implemented as a separate change.

## Open Questions

The following were open at the time this design was written and have
since been resolved in `specs/canonical-software-model/spec.md`, and are
recorded here only for history: the confidence model (resolved as
qualitative HIGH/MEDIUM/LOW, extensible to numeric later), the
reconciliation rule for competing Architecture Component/Boundary
knowledge (resolved via the DECLARED > OBSERVED > INFERRED precedence
order plus CONFLICTED marking for same-category conflicts), Source File
representation (resolved as excluded from the CSM vocabulary, retained as
a location attribute on Type/Method), and Method-level node optionality
(resolved as optional per analyzer, with Type-level as the minimum
required capability).

Remaining and newly identified open questions, intentionally deferred as
future work:

- **Exact, exhaustive catalog of structural kinds and relationship types**
  (§4, §14) — the core vocabulary defined in the specification is
  intentionally non-exhaustive and extensible per the CSM Versioning and
  Evolution requirement; the full catalog is deferred to future,
  additive specification changes as new languages and analysis needs
  arise.
- **Numeric confidence mapping** — the specification fixes the qualitative
  HIGH/MEDIUM/LOW model now (§9); the exact numeric ranges each level
  would map to, if and when a numeric confidence representation is
  introduced, are deferred to that future specification change.
- **API contract schema body placement** — whether an API contract's
  schema body lives as a CSM reference plus an evidence-store body, or as
  an inline lightweight structural summary, is deferred to the future
  specification that defines API/contract representation in more depth.
- **Multi-repository federation mechanism** — per-repository CSM scoping
  is fixed by this design (§18); the mechanism by which a future
  capability would resolve matching External System references across
  multiple repositories' CSMs into a system-level view is explicitly out
  of scope here and deferred to that future capability.
