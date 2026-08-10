## Context

See `proposal.md` for motivation and `explore.md`'s findings (summarized
in the conversation that produced this change) for the reasoning this
design formalizes. This document is conceptual and
technology-independent: it defines the architecture of the CSM Builder
capability and resolves the six items the proposal carried forward as
"Unresolved Questions for Design." It does not choose a programming
language, storage engine, or concurrency framework, except where a
technology-level distinction is necessary to state an architectural
boundary clearly — such mentions are explicitly labeled "implementation
option," not decided here.

The boundary this design preserves, unchanged from the proposal (Shape
A, stakeholder-confirmed):

```
Repository Evidence          ◀── already specified (archived,
    ↓  (consumed by,             software-repository-understanding)
       not built here)
CSM Builder                  ◀── THIS CHANGE
    ↓  (produces, deterministic,
       observed-only)
Canonical Software Model     ◀── already specified (archived,
                                  canonical-software-model)
```

CSM Builder MAY only construct `observed` CSM knowledge, from evidence
alone, mechanically. It SHALL NOT infer Architecture Components,
Architectural Boundaries, or Business Context; SHALL NOT introduce new
CSM vocabulary; SHALL NOT assign confidence to observed knowledge;
SHALL NOT perform AI/LLM reasoning; SHALL NOT evaluate policy; and SHALL
NOT depend on the Policy/Rule Model or Runtime Model for construction.
Every decision below operates strictly inside that boundary.

## Goals / Non-Goals

**Goals:**
- Define the CSM Builder's overall architecture: how Repository
  Evidence is consumed and transformed into CSM elements and
  relationships.
- Define CSM element and relationship identity derivation, sufficient
  to satisfy CSM's `Incremental Model Updates` stable-identity
  requirement.
- Define provenance-record construction for every CSM element CSM
  Builder produces.
- Resolve the six items the proposal named as Design-phase decisions:
  snapshot vs. live graph, dependency-evidence reconciliation, External
  System construction, `ConfigReference` representation,
  partial-evidence handling, and CSM Builder/mapper versioning.
- Define how CSM Builder invokes CSM's already-approved conflict,
  precedence, and validation mechanisms, without reimplementing or
  extending them.
- Define the extension mechanism for new Evidence kinds, symmetric to
  the extension mechanisms already established in both archived specs.

**Non-Goals:**
- Choosing a storage technology, snapshot-persistence format, or
  concurrency framework.
- Designing any future `declared`- or `inferred`-knowledge-producing
  capability (e.g. a future Architecture Compliance Agent or a
  human-declaration ingestion mechanism) — those remain separate,
  unassigned future capabilities.
- Introducing any new CSM entity kind, relationship type, provenance
  category, or confidence representation — every decision below maps
  onto vocabulary the CSM specification already defines.
- Producing an exhaustive, closed catalog of every Evidence-kind-to-CSM
  mapping rule (e.g. every build system's exact scope-string table) —
  this design fixes the mechanism and the categories; exhaustive data
  is implementation/reference detail.

## Decisions

### 1. Overall CSM Builder Architecture

**Problem:** Define the shape of the transformation pipeline precisely
enough to reason about identity, incrementality, and extensibility,
without over-specifying implementation.

**Architecture:** A **Mapping Orchestrator** consumes a Repository
Evidence Model (or an incremental delta of one, per Decision 12) and
applies a registry of pluggable **Evidence-Kind Mappers** — one per
Repository Evidence kind CSM Builder supports (`Repository`, `Project`,
`Module`, `Package`, `SourceUnit`, `Method`-level evidence,
`ManifestDependencyEdge`, `ImportEdge`, `ApiContractDeclaration`, and,
per Decision 8, explicitly *not* `ConfigReference` for now). Each
Mapper receives Evidence Items of its kind and produces zero or more
CSM elements/relationships, each carrying a provenance record
(Decision 4) and a derived identity (Decision 3). This mirrors, one
layer up, the same "pluggable registry per unit of native vocabulary"
pattern both archived specs already use (RU's Language Analyzer /
Build-System Detector / Configuration Recognizer registries; CSM's own
closed-but-extensible vocabulary).

**Alternative considered:** a single monolithic transformation function
handling all evidence kinds inline. Rejected for the same reason RU
rejected a monolithic multi-language parser: it recreates coupling this
architecture's registry pattern exists to avoid, and makes adding a new
Evidence kind (or a new CSM Builder version for one kind, see Decision
13) require touching a shared function rather than registering a new,
independent Mapper.

### 2. Entity Mapping

**Decision:** Structural Repository Evidence maps onto CSM structural
entities 1:1, using only existing CSM vocabulary:

| Repository Evidence | CSM Entity |
|---|---|
| `Repository` Evidence Item | `Repository` |
| `Project` Evidence Item | `Project` |
| `Module` Evidence Item (including the single default Module RU produces for a sub-module-less Project) | `Module` |
| `Package` Evidence Item | `Package` |
| `SourceUnit` Evidence Item (any native kind label — class, struct, interface, record, ...) | `Type` |
| Method-level Evidence Item (where present) | `Method` |

Every `SourceUnit`, regardless of its native kind label, maps onto
CSM's single `Type` entity kind — CSM's `CSM Conceptual Vocabulary`
defines no separate `Interface` (or `Struct`, `Record`, ...) entity
kind. The native label is preserved as an attribute on the `Type`
element (using the CSM's own native-evidence-attribute-bag mechanism
already described by `CSM Conceptual Vocabulary`'s "unmappable
construct" scenario), never as a distinct CSM entity kind.

**Unmanaged pseudo-Project:** RU's reserved "unmanaged" Project Evidence
Item maps onto a real, `observed` CSM `Project` entity, using the same
reserved identity RU already assigns. Rationale: unmanaged evidence is
still evidence of real code; excluding it from CSM would create a
silent analysis blind spot exactly like the ones RU's own
`Partial-Analysis Outcome Reporting` requirement was designed to
prevent one layer down. No interpretation is required to represent
"this Project exists and is named 'unmanaged'" — it is exactly as
mechanical as any other Project mapping.

### 3. CSM Element Identity Derivation

**Decision:** A CSM element's identity is a deterministic, pure
function of the identity of the Repository Evidence Item(s) that
produced it:
- For entities with a 1:1 evidence relationship (`Repository`,
  `Project`, `Module`, `SourceUnit`→`Type`, Method-level Evidence
  Item→`Method`): CSM identity = `f(evidence identity)` — a stable
  transformation (e.g. namespacing the evidence identity under a CSM
  identity scheme) that changes if, and only if, the evidence identity
  changes. This directly inherits RU's already-solved rename-stability
  guarantee (`Evidence Identity`) at no additional cost.
- For entities aggregated from evidence discovered incrementally across
  multiple files (`Package`): CSM identity = `f(containing Module
  identity, native namespace name)` — independent of which specific
  `SourceUnit` Evidence Items currently populate it, so a `Package`'s
  identity survives individual member files being added or removed.
- For relationships (containment, dependency, integration,
  exposure/consumption, and any other CSM relationship type CSM Builder
  constructs): identity = `f(source entity identity, target entity
  identity, relationship type, and, for dependency relationships, no
  kind qualifier)` — the kind qualifier is an attribute of the
  relationship, not part of its identity, so a dependency relationship's
  identity is stable even when its kind qualifier changes between runs
  (see Decision 6).

**Rationale:** This satisfies CSM's `Incremental Model Updates`
requirement ("Every CSM element SHALL carry a stable identity that
persists across re-analysis runs as long as the underlying structure it
represents has not changed") without inventing a separate CSM-side
identity scheme — CSM identity is anchored to, and inherits the
stability properties of, RU's own already-approved identity scheme.

### 4. Provenance Record Construction

**Decision:** Every CSM element CSM Builder produces carries a
`Structured Provenance Record` (per CSM's own requirement) with:
provenance category `observed` (always — CSM Builder produces no
other category, per Shape A); source reference = the identity of the
originating Repository Evidence Item(s) (satisfying CSM's traceability
requirement directly, since RU's evidence identity is itself stable and
addressable); timestamp = the time CSM Builder constructed or last
re-derived the element (not the evidence's own discovery timestamp,
which RU already records separately at the evidence layer).

No confidence level is attached to any element CSM Builder produces,
per CSM's own rule that `observed` elements "SHALL NOT require a
confidence level."

### 5. Structural Containment Relationship Construction

**Decision:** CSM Builder constructs CSM containment relationships
(Repository→Project→Module→Package→Type[→Method]) directly from RU's
already-1:1-aligned structural containment relationships (RU's
`Package Evidence and Containment` and related requirements). This is
close to mechanical: no interpretation is required, since both
containment chains were deliberately engineered to align during RU's
own design. RU's File Evidence Item (a reference/location anchor, never
a structural container, per RU's `File Evidence and Kind-Specific
Layering`) becomes a source-location *attribute* on the corresponding
`Type`/`Method` CSM element, exactly mirroring CSM's own `Source File
Representation` requirement — never a CSM relationship to a file
entity, since CSM has no such entity kind.

### 6. Dependency Evidence Reconciliation

**Problem (carried forward as Explore Question 4 / Proposal item 2):**
RU produces two distinct dependency Evidence kinds
(`ManifestDependencyEdge`, `ImportEdge`) that must combine into CSM's
single `dependency` relationship type.

**Options considered:**
- (a) Manifest-authoritative: one CSM `dependency` relationship per
  (source, target); kind qualifier derived only from the
  manifest-declared edge; source-level references retained as
  traceability only, not represented as a separate relationship.
- (b) Dual-relationship: manifest-declared edges become CSM
  `dependency` relationships; source-level reference edges become a
  *different* CSM relationship type. Rejected — no existing CSM
  relationship type (containment, dependency, implementation/extension,
  invocation, exposure/consumption, integration, composition,
  boundary/constraint) is an honest fit for a bare source-level import
  reference that isn't also a formal dependency or a call, and forcing
  one would either misuse an existing relationship type's meaning or
  require introducing a new one — both violate this change's "no new
  CSM vocabulary" constraint.
- (c) Confirmation-flag: one CSM `dependency` relationship per (source,
  target); kind qualifier populated from the manifest-declared edge
  when its native scope string is mappable; the relationship carries an
  attribute recording whether a corresponding source-level reference
  also exists, as **enrichment of an existing relationship**, not a
  second relationship or a new vocabulary concept.

**Decision: (a), without (c).** CSM Builder constructs exactly one CSM
`dependency` relationship per (source, target) pair evidenced by either
a manifest-declared edge, a source-level reference edge, or both:
- If only a manifest-declared edge exists: the relationship carries a
  kind qualifier when the native scope string maps to CSM's
  compile-time/runtime/test-only vocabulary (see mapping-table note
  below), and no kind qualifier when it does not (an honest, valid
  state per CSM's own "where that distinction is discoverable from
  evidence" phrasing — an unmapped native scope string is a legitimate
  case of the distinction not being discoverable, not an error to
  force-guess through).
- If only a source-level reference edge exists (e.g. a dynamic or
  reflective reference the build manifest doesn't declare, or a
  reference inside the unmanaged pseudo-Project): the relationship is
  still constructed, with no kind qualifier.
- If both exist: the relationship carries the manifest-derived kind
  qualifier (when mappable), and is otherwise constructed identically
  to the manifest-only case.

**Option (c) is not adopted, on stakeholder review.** The original
rationale for (c) leaned on CSM's `CSM Relationship to the Runtime
Model` requirement's "runtime confirmation enriches an existing
relationship" scenario as precedent. On review, that scenario is
narrowly scoped to *runtime telemetry* confirming a *static*
dependency — a cross-model enrichment CSM explicitly authorized for
that specific case. Reusing the general pattern for a purely static,
same-model corroboration between two Repository Evidence kinds is an
analogy, not a literal application of anything CSM's spec already
authorizes, and risks becoming an implicit attribute-level vocabulary
extension no deliberate, versioned CSM change actually approved.
Rather than invent it, whether and how to record source-level
corroboration of a manifest-declared dependency is left as an open,
deferred decision (see Open Questions) — CSM Builder's v1 behavior
simply does not record it.

The native-scope-string → CSM-dependency-kind mapping itself is a
versioned, per-build-system lookup table (e.g. Maven's `compile`,
`provided` → `compile-time`; `test` → `test-only`; `runtime` →
`runtime`; equivalents for other build systems) — the mechanism is
decided here; the exhaustive table is reference data, not an
architectural decision, and is explicitly deferred (see Open
Questions).

### 7. External System Construction

**Problem (carried forward as Explore Question 3 / Proposal item 3):**
Whether, and how, CSM Builder represents an unresolved dependency
target using CSM's existing `External System` vocabulary.

**Decision:** CSM Builder constructs a minimal `External System`
boundary node using a purely mechanical rule: a manifest-declared
dependency edge whose target artifact coordinate does not correspond to
any `Project` or `Module` Evidence Item discovered within this
repository's evidence is represented as an `integration` relationship
from the source `Module` to a new `External System` element, named
using the target artifact's coordinate as declared by the manifest.
This uses CSM's `External System Representation` requirement's own
stated connector type verbatim — that requirement's own scenario
("Outbound integration represented without modeling internals") already
specifies "the CSM SHALL represent the third-party system as an
External System boundary node connected via an integration
relationship." An earlier draft of this decision used `dependency`
instead; that was a defect (caught in stakeholder review), not a
deliberate divergence — `integration` is what CSM's own spec requires
for a Module-to-External-System connection, and `dependency` remains
reserved for the intra-repository Module-to-Module case (Decision 6).
Other `External System` attributes (criticality, integration protocol,
owner) are left unset, consistent with CSM's own "where known"
qualifier — none of them is mechanically knowable from evidence alone.
This uses only existing CSM vocabulary (`External System` and
`integration` are already core CSM vocabulary) and requires no
judgment: "does a matching Project/Module exist in this repository's
evidence" is a lookup, not an inference.

This does not attempt cross-repository correlation (per Explore's
Observation 6): an `External System` node constructed this way may, in
fact, correspond to a Module built by another repository this AIP
instance also analyzes — CSM Builder has no evidence to know that, and
does not guess. Reconciling `External System` nodes across repositories
is explicitly out of this capability's scope, consistent with CSM's own
`Multi-Repository Scope` requirement treating an `External System`
boundary reference as sufficient "at minimum."

### 8. `ConfigReference` Representation

**Problem (carried forward as Explore Question 5 / Proposal item 4):**
No existing CSM relationship type (containment, dependency,
implementation/extension, invocation, exposure/consumption,
integration, composition, boundary/constraint) is an honest fit for "a
Module reads a configuration key."

**Decision: out of scope for this capability's initial construction.**
CSM Builder does not construct any CSM element or relationship from
`ConfigReference` or `ConfigFile` Evidence Items. This evidence remains
valid, queryable Repository Evidence — traceable and complete — but is
simply not yet promoted to CSM knowledge. This is a legitimate, valid
state under CSM's own model: the `Repository Evidence to CSM
Transformation` requirement's own scenario states "Evidence without
corresponding CSM element... no corresponding CSM element SHALL be
considered to exist until the transformation stage has produced it" —
not producing one for a given evidence kind is not an error.

**Rejected alternative:** forcing `ConfigReference` onto the nearest
approximate existing relationship type (e.g. `composition` or
`integration`). Rejected because doing so would misuse an existing
relationship type's defined meaning to cover a fact it wasn't designed
to represent — a subtler version of the same "no new CSM vocabulary"
violation this change must avoid. A future, deliberate, versioned CSM
vocabulary change (e.g. a `configuration-reference` relationship type)
is the honest path if this capability is ever extended to cover it —
explicitly not decided here.

### 9. API Contract Evidence Mapping

**Decision:** An `ApiContractDeclaration` Evidence Item maps onto CSM's
`exposure/consumption` relationship type, attached to the declaring
`Type` or `Method` (the endpoint/interface's own source construct). CSM
Builder does **not** construct a consumer entity for this relationship
unless a corresponding consumer already exists as CSM content from
other evidence. Unlike Decision 7's unresolved-dependency case, an API
contract declaration carries no target artifact coordinate or other
identifying fact about who consumes it — Decision 7's `External System`
mechanism is anchored specifically to a manifest-declared target
identity, which API contract evidence simply does not supply. Applying
that mechanism here would mean fabricating an anonymous, unevidenced
"external caller" entity, which is exactly the kind of invented
construct Shape A prohibits. CSM Builder therefore constructs only the
`exposure/consumption` relationship's declaring side; the consuming
side is populated only when independently evidenced.

**Correction on review:** an earlier draft of this decision said the
`External System` mechanism from Decision 7 should apply here as a
fallback. That was inconsistent with this capability's own "no invented
consumer" rule and was never reflected in the specification, which
correctly implemented the no-invented-consumer behavior throughout.
This decision is corrected to match the specification, not the other
way around.

The lightweight structural summary RU's evidence already carries
(operation names, paths, verbs) is preserved as a relationship or
`Type`/`Method` attribute; the full schema body remains referenced by
location only, per RU's own choice not to duplicate it and per CSM's
`CSM Scope Boundary` exclusion of raw evidence bodies from CSM content.

### 10. Partial-Evidence Handling

**Problem (carried forward as Explore Question 8 / Proposal item 5):**
How CSM Builder treats a CSM element built from `partial` (vs.
`complete`) Repository Evidence, without introducing anything
confidence-shaped onto `observed` knowledge.

**Decision:** CSM Builder constructs a CSM element from `partial`
Repository Evidence using whatever structure was successfully
extracted, treating it identically to an element built from `complete`
evidence for provenance purposes — still `observed`, still no
confidence, no CSM-side "incompleteness" flag or attribute of any kind.
Incompleteness remains visible exclusively by following the CSM
element's traceability reference (Decision 4) back to the originating
Evidence Item and inspecting *its* `partial` discovery-outcome status —
a fact that already exists at the evidence layer and does not need
duplicating at the CSM layer. For `failed` Repository Evidence (no
structure extracted at all), CSM Builder constructs no corresponding
CSM element — consistent with CSM's own `Method-Level Representation
Capability` precedent that absence of an element is not itself invalid.

**Rejected alternative:** attaching an incompleteness marker to the CSM
element itself. Rejected because CSM's `CSM Validation Expectations`
requirement enumerates the complete list of what makes a CSM element
valid, and nothing resembling "completeness" appears in it — adding
such a marker would be exactly the kind of confidence-adjacent,
CSM-Builder-invented attribute the "no new CSM vocabulary" constraint
rules out.

### 11. Snapshot vs. Live Graph

**Problem (carried forward as Explore Question 6 / Proposal item 1):**
Whether CSM Builder incrementally mutates a long-lived CSM graph in
place, or produces a fresh, versioned CSM snapshot each run.

**Decision: versioned snapshot per run.** Each CSM Builder run produces
a new, individually identifiable CSM snapshot (per CSM's own `CSM
Versioning and Evolution` requirement: "Each CSM snapshot for a
repository SHALL be individually identifiable... rather than
overwritten in place"). A snapshot is a complete, self-contained CSM
for the repository as of that run — elements present in evidence at
that time are present in the snapshot; elements whose evidence is
`REMOVED` (RU's change-status vocabulary) are simply absent from the
new snapshot, while the prior snapshot containing them remains
retrievable, untouched, by definition of versioning.

**Rejected alternative:** a live, incrementally-mutated CSM graph.
Rejected specifically because of this change's "no new CSM vocabulary"
constraint: a live graph requires the CSM itself to carry some
tombstone/purge lifecycle concept for elements whose evidence was
removed (mirroring RU's own `PRESENT`/`TOMBSTONED`/`PURGED`), and no
such concept exists in CSM's currently-approved vocabulary. Introducing
one would be exactly the kind of new CSM vocabulary this change is
constrained not to add. The snapshot approach answers the "what happens
when evidence is removed" question using only what CSM's spec already
provides (versioning), at the cost of needing Decision 16 below to keep
declared knowledge (owned by a separate, future capability) anchored
correctly across snapshot boundaries.

### 12. Incremental Re-derivation Scope

**Decision:** CSM Builder consumes RU's per-run change-status
classification (`ADDED`/`UNCHANGED`/`MODIFIED`/`REMOVED`) to scope each
snapshot's construction: elements derived from `UNCHANGED` evidence are
carried forward into the new snapshot without re-running their Mapper;
elements derived from `ADDED` or `MODIFIED` evidence are (re)computed
by the relevant Mapper; elements whose sole source evidence is
`REMOVED` are omitted from the new snapshot (see Decision 11). This
mirrors RU's own `Incremental Analysis Scope` requirement one layer up:
CSM Builder's own "blast radius" is bounded to CSM elements whose
construction directly depends on changed evidence — e.g. a `Module`'s
dependency relationships are recomputed when its `ManifestDependencyEdge`
evidence changes, but an unrelated `Type` elsewhere in the same Module
is not.

RU's lifecycle-state vocabulary (`PRESENT`/`TOMBSTONED`/`PURGED`) maps
onto this scope as follows: evidence in the `PRESENT` state (whether via
`ADDED`, `UNCHANGED`, or `MODIFIED` classification) is available for
mapping; evidence that has entered `TOMBSTONED` is treated, for CSM
construction purposes, identically to `REMOVED` — the corresponding CSM
element is omitted from the new snapshot starting with the run in which
the evidence's change-status is `REMOVED` (i.e. CSM Builder does not
wait for the evidence to reach `PURGED` before dropping the CSM
element; the RU-side tombstone retention window exists for RU's own
evidence-layer auditability, not to delay CSM construction).

### 13. CSM Builder / Mapper Versioning

**Problem (carried forward as Explore Question 7 / Proposal item 6):**
RU's `Analyzer Version Reprocessing` covers changes to RU's own
analyzers, not changes to CSM Builder's own mapping logic.

**Decision:** CSM Builder maintains its own internal, monotonically
versioned identifier per Evidence-Kind Mapper (an operational
bookkeeping concept internal to CSM Builder, not CSM vocabulary — it
never appears as a CSM element attribute; only the resulting
provenance-record timestamp, already required by CSM, reflects that a
re-derivation occurred). When a Mapper's version changes, every CSM
element/relationship it previously produced becomes eligible for
re-derivation on the next run, even when the underlying Repository
Evidence's own change-status is `UNCHANGED` — directly analogous to
RU's own `Analyzer Version Reprocessing` precedent, one layer up. This
lets a CSM Builder improvement (e.g. an expanded Maven-scope mapping
table, per Decision 6) propagate into freshly-derived snapshots without
requiring any change to the underlying repository or its evidence.

### 14. Conflict and Precedence Invocation

**Decision:** CSM Builder invokes CSM's already-approved
`Same-Category Conflict Marking and Resolution` mechanism exactly as
specified, without modification, whenever it produces two `observed`
assertions about the same subject (per CSM's own `Subject
Identification for Knowledge Assertions` rule) that conflict — e.g. two
manifest sources somehow yielding contradictory scope declarations for
what resolves to the same dependency-relationship subject. CSM Builder
does not attempt to resolve such conflicts itself; both assertions are
preserved with their own provenance records, and the subject's
effective knowledge is marked `CONFLICTED`, exactly as CSM's spec
already requires. No new conflict-handling logic is introduced.

### 15. Validation Integration

**Decision:** Before a snapshot produced by a CSM Builder run is
considered complete, CSM Builder validates it against CSM's own `CSM
Validation Expectations` requirement (defined vocabulary membership,
valid structured provenance on every element, absence of confidence on
`observed` elements, `Architecture Component`/`Architectural Boundary`
never `observed` — trivially satisfied since CSM Builder never
constructs either — traceable reference to originating evidence on
every element, and so on). A snapshot that fails this validation is not
published as usable CSM content — consistent with CSM's own `Validation
is performed before analysis` scenario, which already requires
validation to happen before any analysis, rule evaluation, or AI
reasoning consumes a snapshot.

### 16. Declared-Knowledge Isolation From Snapshots

**Decision:** Because Decision 11 makes each snapshot a fresh,
independently-constructed artifact, any future `declared` knowledge
(e.g. a human-declared Business Capability mapping to a `Module`) MUST
be persisted separately from any individual snapshot, keyed by CSM
element *identity* (Decision 3) rather than by snapshot instance. This
is not a mechanism CSM Builder itself implements (declared-knowledge
ingestion is explicitly out of this capability's scope, per Shape A) —
it is a constraint this design places on any future declared-knowledge
capability, so that a declared mapping anchored to a `Module` whose
identity persists across snapshots (per Decision 3's stability
guarantee) remains valid without needing to be re-declared every time
CSM Builder produces a new snapshot. This directly satisfies CSM's own
`Incremental Model Updates` requirement that "declared mappings... that
do not depend on the changed evidence" must not be invalidated by
unrelated re-derivation.

### 17. Extension Mechanism for New Evidence Kinds

**Decision:** Support for a new Repository Evidence kind (e.g. a future
IaC-semantic evidence kind, or a future `ConfigReference`-covering CSM
vocabulary extension) is introduced only by registering a new
Evidence-Kind Mapper against the Mapping Orchestrator (Decision 1). This
mirrors, one layer up, the extension discipline both archived specs
already establish (RU's three registries; CSM's own "new entity kind
only via deliberate, versioned specification change"). Introducing a
new Mapper never requires modifying CSM Builder's identity-derivation
mechanism (Decision 3), provenance-construction mechanism (Decision 4),
or incremental-scoping mechanism (Decision 12) — those are Mapper-agnostic.

### 18. Relationship Types Without Corresponding Evidence

**Problem (raised in stakeholder review):** CSM's core relationship
vocabulary includes `implementation/extension` and `invocation`, but
the Repository Evidence Model, as currently specified, has no Evidence
kind carrying an "implements/extends" fact or a call-graph fact.
Leaving this silent (as an earlier draft did) is ambiguous — a reader
cannot tell whether the omission is deliberate or an oversight.

**Decision:** CSM Builder explicitly does not construct
`implementation/extension` or `invocation` relationships, and states
this as a first-class, deliberate scope boundary rather than leaving it
implicit. This is symmetric to Decision 8's `ConfigReference`
exclusion — both are honest "no evidence, no construction" statements,
not silent gaps. Should Repository Evidence ever gain an Evidence kind
carrying an implements/extends or call-graph fact, covering it requires
only registering a new Evidence-Kind Mapper (Decision 17); it does not
require revisiting this decision's boundary itself.

This decision introduces no new CSM vocabulary — `implementation/extension`
and `invocation` are already core CSM relationship types; CSM Builder
simply does not yet have evidence to construct either from.

## Risks / Trade-offs

- [Two build-system-format mapping tables (RU's Build-System Detector
  Registry and CSM Builder's own native-scope→CSM-dependency-kind
  table, Decision 6) can drift apart as build systems are added] →
  Mitigation: the two tables are versioned independently but should be
  updated together as a matter of process; an unmapped native scope
  string is always a safe, honest fallback (no kind qualifier) rather
  than a broken state, so drift degrades gracefully rather than
  producing wrong answers.
- [Snapshot-per-run (Decision 11) means a consumer must always resolve
  "the current CSM" as "the latest snapshot," rather than there being
  one canonical live graph] → Mitigation: this is exactly what CSM's
  own `CSM Versioning and Evolution` requirement already anticipates;
  it is not a new burden this design introduces.
- [Declared-knowledge isolation (Decision 16) places a real constraint
  on a capability this change does not itself build] → Mitigation:
  documented explicitly here so the eventual declared-knowledge
  capability's own design does not have to rediscover this constraint;
  the constraint follows directly from CSM's own already-approved
  `Incremental Model Updates` requirement, so it is not a new
  invention, just an early statement of an existing obligation.
- [`ConfigReference` evidence being entirely unrepresented in CSM
  (Decision 8) may surprise a future consumer expecting configuration
  facts to already be CSM-queryable] → Mitigation: RU's own evidence
  remains fully queryable and traceable; this is an explicit,
  documented scope boundary, not a silent gap — a future, deliberate
  CSM vocabulary change is the correct path if this becomes a real
  need.
- [External System construction (Decision 7) may produce multiple
  distinct `External System` nodes for what is, in reality, the same
  external artifact referenced under slightly different manifest
  coordinates across different Modules] → Mitigation: accepted as a
  known limitation of a purely mechanical, non-correlating rule;
  reconciling near-duplicate External System nodes would require
  judgment CSM Builder is not permitted to exercise under Shape A.
- [CSM Builder's own mapper-versioning mechanism (Decision 13) adds
  operational bookkeeping symmetric to, but separate from, RU's
  Discovery Manifest] → Mitigation: judged necessary for the same
  reason RU's own analyzer-version reprocessing was judged necessary —
  without it, a CSM Builder improvement could never propagate without
  an unrelated repository change forcing re-derivation.
- [Not recording source-level corroboration of a manifest-declared
  dependency (Decision 6, option (c) rejected) discards a signal RU's
  own design.md flagged as potentially useful for a future Dependency
  Boundary Agent — e.g. "declared but never actually imported"] →
  Mitigation: accepted; the signal is not lost, only unrecorded at the
  CSM layer — a consumer that needs it can still derive it by directly
  cross-referencing RU's `ManifestDependencyEdge` and `ImportEdge`
  evidence, which both remain fully traceable from the CSM
  `dependency` relationship. Recording it as CSM content is deferred,
  not foreclosed.
- [CSM's own vocabulary writes `implementation/extension` and
  `exposure/consumption` as slash-joined pairs without stating whether
  each is one relationship type or two] → This is a pre-existing
  ambiguity in the archived `canonical-software-model` specification,
  not something this change introduces or is positioned to resolve.
  CSM Builder uses the vocabulary as archived, verbatim, in both
  cases. Flagged here as an upstream terminology concern for a future,
  separate CSM specification clarification — not addressed by this
  change.

## Migration Plan

Greenfield — no existing CSM Builder implementation to migrate from.
Sequencing note: this design does not require, and should not wait for,
any future declared- or inferred-knowledge capability to exist first;
Decision 16 states the constraint that capability must respect when it
is eventually designed, without this change building any part of it.

## Open Questions

- Exact native-scope-string → CSM-dependency-kind mapping tables per
  build system (Decision 6) — reference data, not an architectural
  decision; deferred to specification/implementation.
- Whether, and how, source-level corroboration of a manifest-declared
  dependency should ever be recorded as CSM content (Decision 6, option
  (c) rejected for now) — deferred rather than resolved by inventing an
  unauthorized relationship attribute; revisit only if a future,
  deliberate CSM vocabulary change explicitly authorizes such an
  attribute.
- Exact persistence mechanism for CSM snapshots (Decision 11) — an
  implementation choice; this design only commits to "individually
  identifiable, not overwritten in place," which CSM's spec already
  requires.
- Exact persistence mechanism for declared knowledge isolated from
  snapshots (Decision 16) — belongs to whichever future capability
  ingests declared knowledge, not to this one.
- Exact concurrency/parallelism mechanism for CSM Builder's own
  Mapping Orchestrator — an implementation choice, not resolved here,
  mirroring RU's own equivalent deferral.
- Whether a future, deliberate CSM vocabulary change ever introduces a
  relationship type covering `ConfigReference` evidence (Decision 8) —
  explicitly not decided by this design; belongs to a future
  `canonical-software-model` change if it is ever pursued.
