## Purpose

The CSM Builder is the deterministic, evidence-driven transformation
capability that constructs `observed` Canonical Software Model (CSM)
knowledge exclusively from the Repository Evidence Model, without
performing architectural interpretation, inference, or any AI/LLM
reasoning.

## Requirements

### Requirement: Deterministic, Evidence-Driven Transformation Only
CSM Builder SHALL construct CSM content only through mechanical,
deterministic transformation of Repository Evidence. CSM Builder SHALL
NOT use AI reasoning, heuristic inference, or probabilistic judgment to
construct, classify, or omit any CSM element or relationship. Given the
same Repository Evidence Model as input, CSM Builder SHALL produce the
same CSM content on every run.

#### Scenario: No AI or heuristic judgment used to construct CSM content
- **WHEN** CSM Builder constructs any CSM element or relationship
- **THEN** it SHALL be traceable to a mechanical mapping rule applied to
  Repository Evidence, and SHALL NOT be the product of AI reasoning or
  probabilistic inference

#### Scenario: Repeated construction from unchanged evidence is identical
- **WHEN** CSM Builder is run twice against the same, unchanged
  Repository Evidence Model
- **THEN** both runs SHALL produce the same CSM content

---

### Requirement: Repository Evidence as Sole Construction Input
CSM Builder SHALL construct CSM content exclusively from the Repository
Evidence Model. CSM Builder SHALL NOT depend on the Policy/Rule Model or
the Runtime Model for its own construction.

#### Scenario: Construction proceeds without policy or runtime data
- **WHEN** CSM Builder is run against a repository with Repository
  Evidence available but no governance policies or runtime telemetry
  configured
- **THEN** CSM Builder SHALL still be able to construct valid CSM
  content for that repository

#### Scenario: No policy or runtime dependency in construction
- **WHEN** CSM Builder constructs any CSM element or relationship
- **THEN** that construction SHALL NOT require reading from, or
  otherwise depend on, the Policy/Rule Model or the Runtime Model

---

### Requirement: Exclusion of Architectural Inference and Declared-Knowledge Construction
CSM Builder SHALL NOT construct an Architecture Component, an
Architectural Boundary, or any Business Context element (Business
Capability, Domain, Ownership, or Criticality mapping). CSM Builder
SHALL NOT invent, propose, or record any architectural grouping of
Modules or Packages, however apparent from naming or structure. Every
CSM element CSM Builder constructs SHALL carry `observed` provenance
(see Provenance Assignment for Constructed Knowledge); CSM Builder
SHALL NOT construct an element carrying `declared` or `inferred`
provenance.

#### Scenario: No Architecture Component constructed
- **WHEN** CSM Builder processes Repository Evidence, including
  Package or Module evidence with a name suggestive of an architectural
  role
- **THEN** CSM Builder SHALL NOT construct an Architecture Component
  element

#### Scenario: No Architectural Boundary constructed
- **WHEN** CSM Builder processes Repository Evidence
- **THEN** CSM Builder SHALL NOT construct an Architectural Boundary
  element

#### Scenario: No Business Context element constructed
- **WHEN** CSM Builder processes Repository Evidence
- **THEN** CSM Builder SHALL NOT construct a Business Capability,
  Domain, Ownership, or Criticality mapping element

#### Scenario: No declared or inferred provenance ever assigned
- **WHEN** CSM Builder constructs any CSM element
- **THEN** that element's provenance classification SHALL be
  `observed`, and SHALL NOT be `declared` or `inferred`

---

### Requirement: Exclusion of New CSM Vocabulary
CSM Builder SHALL construct CSM content using only entity kinds and
relationship types already defined by the Canonical Software Model
specification. CSM Builder SHALL NOT introduce a new CSM entity kind,
relationship type, provenance category, or confidence representation.

#### Scenario: Only existing CSM vocabulary used
- **WHEN** CSM Builder constructs any CSM element or relationship
- **THEN** its entity kind or relationship type SHALL be one already
  defined by the Canonical Software Model specification

#### Scenario: Unmappable evidence does not create new vocabulary
- **WHEN** Repository Evidence has no evident mapping onto an existing
  CSM entity kind or relationship type
- **THEN** CSM Builder SHALL NOT construct a CSM element or
  relationship for it rather than introduce a new one

---

### Requirement: Structural Entity Mapping
CSM Builder SHALL map a `Repository` Evidence Item to a CSM `Repository`
element, a `Project` Evidence Item to a CSM `Project` element, a
`Module` Evidence Item to a CSM `Module` element, a `Package` Evidence
Item to a CSM `Package` element, a `SourceUnit` Evidence Item to a CSM
`Type` element regardless of its native construct kind label, and a
Method-level Evidence Item to a CSM `Method` element.

#### Scenario: Structural evidence maps to corresponding CSM entities
- **WHEN** CSM Builder processes a `Repository`, `Project`, `Module`,
  `Package`, `SourceUnit`, or Method-level Evidence Item
- **THEN** it SHALL construct the corresponding CSM `Repository`,
  `Project`, `Module`, `Package`, `Type`, or `Method` element

#### Scenario: Every native source-construct kind maps to Type
- **WHEN** CSM Builder processes a `SourceUnit` Evidence Item with any
  native construct kind label (e.g. class, struct, interface, record)
- **THEN** it SHALL construct a CSM `Type` element, and SHALL NOT
  construct a distinct CSM entity kind for that native label

---

### Requirement: Native Construct Kind Preserved as Attribute
CSM Builder SHALL preserve a `SourceUnit` Evidence Item's native
construct kind label as an attribute on the resulting `Type` element,
using the Canonical Software Model's native-evidence-attribute
mechanism, rather than discarding it.

#### Scenario: Native kind label retained on the Type element
- **WHEN** CSM Builder constructs a `Type` element from a `SourceUnit`
  Evidence Item
- **THEN** the resulting `Type` element SHALL retain the Evidence
  Item's native construct kind label as an attribute

---

### Requirement: Unmanaged Evidence Mapping
CSM Builder SHALL map the Repository Evidence Model's reserved
unmanaged Project Evidence Item to a CSM `Project` element with
`observed` provenance, using the same reserved identity, rather than
omitting unmanaged evidence from CSM content.

#### Scenario: Unmanaged Project evidence produces a CSM Project element
- **WHEN** CSM Builder processes the reserved unmanaged Project
  Evidence Item
- **THEN** it SHALL construct a corresponding CSM `Project` element
  with `observed` provenance

---

### Requirement: CSM Element Identity Derivation
CSM Builder SHALL derive each CSM element's identity as a deterministic
function of the identity of the Repository Evidence Item(s) that
produced it. For a CSM element derived from exactly one Evidence Item,
the CSM element's identity SHALL change if, and only if, that Evidence
Item's identity changes. For a `Package` element, whose evidence may be
discovered incrementally across multiple `SourceUnit` Evidence Items,
CSM Builder SHALL derive its identity from its containing `Module`
element's identity and its native namespace name, independent of which
specific `SourceUnit` Evidence Items currently populate it.

#### Scenario: CSM identity stable when evidence identity is stable
- **WHEN** CSM Builder re-derives a CSM element from Repository Evidence
  whose identity has not changed since a prior run
- **THEN** the resulting CSM element SHALL retain the same identity it
  had in that prior run

#### Scenario: Package identity survives member evidence changes
- **WHEN** a `SourceUnit` Evidence Item is added to or removed from a
  Package's native namespace, leaving other member Evidence Items
  unchanged
- **THEN** the corresponding `Package` element's identity SHALL remain
  unchanged

---

### Requirement: CSM Relationship Identity Derivation
CSM Builder SHALL derive a CSM relationship's identity from its source
entity identity, target entity identity, and relationship type. For a
`dependency` relationship, CSM Builder SHALL NOT include the
relationship's dependency-kind qualifier in its identity.

#### Scenario: Dependency relationship identity stable when kind qualifier changes
- **WHEN** a `dependency` relationship's kind qualifier changes between
  two runs (e.g. from unqualified to `compile-time`) while its source
  and target entities remain unchanged
- **THEN** the relationship SHALL retain the same identity across both
  runs

---

### Requirement: Provenance Assignment for Constructed Knowledge
CSM Builder SHALL assign every CSM element and relationship it
constructs a Structured Provenance Record with provenance category
`observed`, a source reference identifying the originating Repository
Evidence Item(s) by their evidence identity, and a timestamp reflecting
when CSM Builder constructed or last re-derived it. CSM Builder SHALL
NOT assign a confidence level to any element or relationship it
constructs.

#### Scenario: Constructed element carries an observed provenance record
- **WHEN** CSM Builder constructs a CSM element or relationship
- **THEN** it SHALL carry a Structured Provenance Record with category
  `observed`, a source reference to the originating Evidence Item(s),
  and a construction timestamp

#### Scenario: No confidence assigned to constructed knowledge
- **WHEN** CSM Builder constructs any CSM element or relationship
- **THEN** it SHALL NOT carry a confidence level of any kind

---

### Requirement: Structural Containment Construction
CSM Builder SHALL construct CSM containment relationships
(Repository→Project→Module→Package→Type[→Method]) directly from the
Repository Evidence Model's own structural containment relationships,
without introducing containment not present in evidence.

#### Scenario: Evidence containment produces corresponding CSM containment
- **WHEN** a structural containment relationship exists between two
  Repository Evidence Items (e.g. a Module containing a Package)
- **THEN** CSM Builder SHALL construct the corresponding CSM
  containment relationship between their mapped CSM elements

---

### Requirement: File Evidence Becomes a Location Attribute, Not a Relationship
CSM Builder SHALL represent a `Type` or `Method` element's originating
File Evidence Item as a source-location attribute on that element. CSM
Builder SHALL NOT construct a CSM relationship to a File Evidence Item,
and SHALL NOT treat a File Evidence Item as a structural container of
any CSM element.

#### Scenario: File reference becomes a source-location attribute
- **WHEN** CSM Builder constructs a `Type` or `Method` element from
  evidence that references a File Evidence Item for physical location
- **THEN** the resulting CSM element SHALL carry a source-location
  attribute derived from that File Evidence Item, and SHALL NOT carry a
  relationship to a File entity

---

### Requirement: Dependency Relationship Construction
CSM Builder SHALL construct exactly one CSM `dependency` relationship
per distinct (source, target) pair evidenced by a
`ManifestDependencyEdge`, an `ImportEdge`, or both, between the same two
entities. CSM Builder SHALL NOT construct more than one `dependency`
relationship for the same (source, target) pair from these two Evidence
kinds.

#### Scenario: Manifest-only evidence produces a dependency relationship
- **WHEN** only a `ManifestDependencyEdge` exists between two Modules
- **THEN** CSM Builder SHALL construct exactly one `dependency`
  relationship between their corresponding CSM elements

#### Scenario: Source-level-only evidence produces a dependency relationship
- **WHEN** only an `ImportEdge` exists between two source constructs
  with no corresponding `ManifestDependencyEdge`
- **THEN** CSM Builder SHALL construct exactly one `dependency`
  relationship between their corresponding CSM elements

#### Scenario: Both evidence kinds produce a single relationship
- **WHEN** both a `ManifestDependencyEdge` and an `ImportEdge` exist
  between the same two entities
- **THEN** CSM Builder SHALL construct exactly one `dependency`
  relationship between their corresponding CSM elements, not two

---

### Requirement: Dependency Kind Classification
Where a `ManifestDependencyEdge`'s native scope string maps to CSM's
compile-time, runtime, or test-only dependency-kind vocabulary via a
versioned, build-system-specific mapping, CSM Builder SHALL qualify the
constructed `dependency` relationship with that kind. Where the native
scope string does not map to CSM's dependency-kind vocabulary, CSM
Builder SHALL construct the `dependency` relationship without a kind
qualifier, rather than guessing a kind.

#### Scenario: Mappable native scope produces a kind-qualified relationship
- **WHEN** a `ManifestDependencyEdge`'s native scope string is mapped
  by a versioned build-system-specific mapping to `compile-time`,
  `runtime`, or `test-only`
- **THEN** the constructed `dependency` relationship SHALL carry that
  kind qualifier

#### Scenario: Unmappable native scope produces an unqualified relationship
- **WHEN** a `ManifestDependencyEdge`'s native scope string has no
  corresponding entry in the build-system-specific mapping
- **THEN** CSM Builder SHALL construct the `dependency` relationship
  without a kind qualifier, and SHALL NOT assign one by guessing

---

### Requirement: External System Construction for Unresolved Dependencies
Where a `ManifestDependencyEdge`'s target artifact coordinate does not
correspond to any `Project` or `Module` Evidence Item discovered within
the repository's Repository Evidence Model, CSM Builder SHALL construct
a CSM `External System` element named from that artifact coordinate,
and an `integration` relationship from the source `Module` element to
it, consistent with the Canonical Software Model's own representation
of a Module integrating with a third-party system. CSM Builder SHALL
NOT populate the `External System` element's criticality, integration
protocol, or owner attributes, since none of these is mechanically
discoverable from evidence.

#### Scenario: Unresolved dependency target produces an External System element
- **WHEN** a `ManifestDependencyEdge`'s target artifact does not
  correspond to any Project or Module in the repository's Repository
  Evidence Model
- **THEN** CSM Builder SHALL construct an `External System` element for
  that target, connected by an `integration` relationship from the
  source Module

#### Scenario: External System attributes left unset when unknown
- **WHEN** CSM Builder constructs an `External System` element under
  this requirement
- **THEN** it SHALL leave the criticality, integration protocol, and
  owner attributes unset rather than inferring values for them

---

### Requirement: Exclusion of Configuration Reference Representation
CSM Builder SHALL NOT construct a CSM element or relationship from a
`ConfigFile` or `ConfigReference` Evidence Item. Such evidence SHALL
remain valid, queryable Repository Evidence without a corresponding CSM
element.

#### Scenario: No CSM element constructed from configuration evidence
- **WHEN** CSM Builder processes a `ConfigFile` or `ConfigReference`
  Evidence Item
- **THEN** it SHALL NOT construct any corresponding CSM element or
  relationship

#### Scenario: Configuration evidence remains valid without CSM representation
- **WHEN** a repository's Repository Evidence Model contains
  `ConfigReference` Evidence Items with no corresponding CSM element
- **THEN** the absence of a corresponding CSM element SHALL NOT be
  treated as invalid Repository Evidence or invalid CSM content

---

### Requirement: API Contract Relationship Construction
CSM Builder SHALL map an `ApiContractDeclaration` Evidence Item to an
`exposure/consumption` relationship attached to the declaring `Type` or
`Method` element, carrying the Evidence Item's structural summary as an
attribute. CSM Builder SHALL NOT construct a consumer entity for this
relationship unless a corresponding consumer already exists as CSM
content from other evidence.

#### Scenario: API contract evidence produces an exposure relationship
- **WHEN** CSM Builder processes an `ApiContractDeclaration` Evidence
  Item
- **THEN** it SHALL construct an `exposure/consumption` relationship
  attached to the declaring Type or Method element

#### Scenario: No invented consumer for an API contract
- **WHEN** CSM Builder constructs an `exposure/consumption`
  relationship from an `ApiContractDeclaration` Evidence Item with no
  evidenced consumer
- **THEN** it SHALL NOT construct a new CSM element to represent an
  unevidenced consumer

---

### Requirement: Exclusion of Relationship Types Without Corresponding Evidence
CSM Builder SHALL NOT construct an `implementation/extension` or
`invocation` CSM relationship, since the Repository Evidence Model, as
currently specified, provides no Evidence kind from which either
relationship type could be mechanically derived. This absence is a
deliberate, stated scope boundary of this capability's current mapping
coverage, not an oversight, and SHALL NOT, by itself, be treated as a
defect in Repository Evidence or in CSM Builder's construction.

#### Scenario: No implementation/extension relationship constructed
- **WHEN** CSM Builder processes a Repository Evidence Model
- **THEN** it SHALL NOT construct an `implementation/extension` CSM
  relationship, since no Repository Evidence kind currently provides
  evidence for one

#### Scenario: No invocation relationship constructed
- **WHEN** CSM Builder processes a Repository Evidence Model
- **THEN** it SHALL NOT construct an `invocation` CSM relationship,
  since no Repository Evidence kind currently provides evidence for one

#### Scenario: Future evidence coverage extends without redesign
- **WHEN** a future Repository Evidence kind provides mechanical
  evidence for an `implementation/extension` or `invocation`
  relationship
- **THEN** CSM Builder MAY construct that relationship type via a newly
  registered Evidence-Kind Mapper (see Extension Mechanism for New
  Evidence Kinds), without this requirement's exclusion needing to
  change beyond registering that Mapper

---

### Requirement: Partial-Evidence Construction
CSM Builder SHALL construct a CSM element from `partial` Repository
Evidence using whatever structure the Evidence Item successfully
captured, assigning it `observed` provenance identically to an element
constructed from `complete` evidence. CSM Builder SHALL NOT attach an
incompleteness marker, flag, or confidence-like attribute to a CSM
element on account of its originating evidence being `partial`.

#### Scenario: Partial evidence still produces a CSM element
- **WHEN** CSM Builder processes a Repository Evidence Item with a
  discovery outcome of `partial`
- **THEN** it SHALL construct a CSM element from the successfully
  captured structure, with `observed` provenance

#### Scenario: No incompleteness marker attached to the CSM element
- **WHEN** CSM Builder constructs a CSM element from `partial`
  Repository Evidence
- **THEN** the resulting CSM element SHALL NOT carry any attribute
  indicating incompleteness or reduced trust

---

### Requirement: Failed-Evidence Non-Construction
CSM Builder SHALL NOT construct a CSM element from a Repository Evidence
Item with a discovery outcome of `failed`. The absence of a
corresponding CSM element for `failed` evidence SHALL NOT, by itself,
be treated as invalid CSM content.

#### Scenario: Failed evidence produces no CSM element
- **WHEN** CSM Builder processes a Repository Evidence Item with a
  discovery outcome of `failed`
- **THEN** it SHALL NOT construct a corresponding CSM element

---

### Requirement: Snapshot-Based CSM Construction
CSM Builder SHALL produce a new, individually identifiable CSM snapshot
on each construction run, rather than mutating a single long-lived CSM
graph in place. A CSM snapshot SHALL reflect the CSM elements
constructible from the Repository Evidence Model's content as of that
run.

#### Scenario: Each run produces a distinguishable snapshot
- **WHEN** CSM Builder is run against a repository more than once
- **THEN** each run SHALL produce a CSM snapshot distinguishable from
  prior snapshots for that repository, rather than overwriting a single
  shared CSM in place

#### Scenario: Prior snapshots remain retrievable
- **WHEN** a new CSM snapshot is constructed for a repository
- **THEN** prior snapshots for that repository SHALL remain retrievable
  and unaffected by the new snapshot's construction

---

### Requirement: Incremental Snapshot Scope
CSM Builder SHALL scope a snapshot's construction using the Repository
Evidence Model's per-run change-status classification: CSM elements
derived solely from `UNCHANGED` Evidence Items SHALL be carried forward
into the new snapshot without re-derivation; CSM elements derived from
`ADDED` or `MODIFIED` Evidence Items SHALL be (re)constructed.

#### Scenario: Unchanged evidence is carried forward without re-derivation
- **WHEN** a discovery run classifies a Repository Evidence Item as
  `UNCHANGED`
- **THEN** CSM Builder SHALL carry the corresponding CSM element
  forward into the new snapshot without re-deriving it

#### Scenario: Changed evidence triggers re-derivation
- **WHEN** a discovery run classifies a Repository Evidence Item as
  `ADDED` or `MODIFIED`
- **THEN** CSM Builder SHALL (re)construct the corresponding CSM
  element for the new snapshot

---

### Requirement: Evidence Lifecycle Interaction
CSM Builder SHALL omit a CSM element from a new snapshot once its sole
originating Repository Evidence Item's change status is classified
`REMOVED`, without waiting for that Evidence Item to reach the `PURGED`
lifecycle state.

#### Scenario: Removed evidence omits the corresponding element
- **WHEN** a Repository Evidence Item's change status is classified
  `REMOVED` for a given run
- **THEN** CSM Builder SHALL omit the corresponding CSM element from
  the snapshot constructed for that run

#### Scenario: CSM Builder does not wait for purge
- **WHEN** a Repository Evidence Item is `TOMBSTONED` but not yet
  `PURGED`
- **THEN** CSM Builder SHALL still omit the corresponding CSM element
  from the current snapshot rather than waiting for the `PURGED` state

---

### Requirement: CSM Builder Mapper Versioning
CSM Builder SHALL maintain an internal, monotonically versioned
identifier for each Evidence-Kind Mapper it uses. Where a Mapper's
version differs from the version recorded for a previously constructed
CSM element, CSM Builder SHALL treat that element as eligible for
re-derivation in the next snapshot, even when the underlying Repository
Evidence's change status is `UNCHANGED`.

#### Scenario: Mapper version change triggers re-derivation
- **WHEN** an Evidence-Kind Mapper's version changes between two CSM
  Builder runs, and the Repository Evidence it maps has not itself
  changed
- **THEN** CSM Builder SHALL re-derive the corresponding CSM element
  using the new Mapper version rather than carrying the prior element
  forward unchanged

---

### Requirement: Conflict Marking Reuse
Where CSM Builder constructs two `observed` assertions about the same
subject that conflict, CSM Builder SHALL apply the Canonical Software
Model's existing same-category conflict marking mechanism, preserving
both assertions and marking the subject's effective knowledge
`CONFLICTED`, rather than resolving the conflict itself or introducing
new conflict-handling behavior.

#### Scenario: Conflicting observed assertions are preserved and marked
- **WHEN** CSM Builder constructs two `observed` assertions about the
  same subject that conflict with one another
- **THEN** it SHALL preserve both assertions with their own provenance
  records and mark the subject's effective knowledge `CONFLICTED`,
  using the Canonical Software Model's existing conflict-marking
  mechanism

#### Scenario: CSM Builder does not arbitrate conflicts
- **WHEN** two `observed` assertions constructed by CSM Builder
  conflict
- **THEN** CSM Builder SHALL NOT select one as effective by any means
  other than the Canonical Software Model's own precedence and
  conflict-resolution mechanisms

---

### Requirement: Snapshot Validation Before Use
CSM Builder SHALL validate a constructed CSM snapshot against the
Canonical Software Model's validation expectations before that snapshot
is considered usable. A snapshot that fails this validation SHALL NOT
be published as usable CSM content.

#### Scenario: Valid snapshot is usable
- **WHEN** a CSM snapshot constructed by CSM Builder satisfies the
  Canonical Software Model's validation expectations
- **THEN** it SHALL be considered usable CSM content

#### Scenario: Invalid snapshot is not published
- **WHEN** a CSM snapshot constructed by CSM Builder fails to satisfy
  the Canonical Software Model's validation expectations
- **THEN** CSM Builder SHALL NOT publish that snapshot as usable CSM
  content

---

### Requirement: CSM Element Identity Stability Across Snapshots
A CSM element's identity, once established, SHALL remain stable across
snapshots for as long as its underlying Repository Evidence identity
persists, independent of whether that element was included in every
intervening snapshot's construction.

This stability is intended to make it possible for a future
declared-knowledge mapping anchored to a CSM element's identity (e.g. a
Business Capability mapping to a Module) to remain valid across
snapshot boundaries without requiring re-declaration on every CSM
Builder run. That anchoring mechanism itself belongs to whichever
future capability ingests declared knowledge, not to CSM Builder, and
is not a CSM Builder obligation this requirement tests.

#### Scenario: Element identity persists across snapshots
- **WHEN** a CSM element's underlying Repository Evidence identity is
  unchanged between two CSM Builder runs
- **THEN** the CSM element SHALL carry the same identity in both runs'
  snapshots

---

### Requirement: Extension Mechanism for New Evidence Kinds
Support for constructing CSM content from an additional Repository
Evidence kind SHALL be introduced only by registering a new
Evidence-Kind Mapper against CSM Builder's existing construction
contracts. Introducing such support SHALL NOT require modifying CSM
Builder's identity-derivation mechanism, provenance-construction
mechanism, or incremental-scoping mechanism.

#### Scenario: New evidence kind added without core mechanism changes
- **WHEN** a new Evidence-Kind Mapper is registered for a previously
  unmapped Repository Evidence kind
- **THEN** CSM Builder's identity-derivation, provenance-construction,
  and incremental-scoping mechanisms SHALL remain unchanged

---

### Requirement: Evidence Traceability Preservation
Every CSM element or relationship CSM Builder constructs SHALL retain a
reference sufficient to trace it back to the specific Repository
Evidence Item(s) it was derived from.

#### Scenario: CSM element traceable to its originating evidence
- **WHEN** a CSM element constructed by CSM Builder is inspected
- **THEN** it SHALL be possible to trace that element back to the
  specific Repository Evidence Item(s) that produced it
