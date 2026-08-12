## Purpose

The Analysis Framework runs deterministic, independently pluggable
Analyzers against a Canonical Software Model snapshot's effective,
conflict-annotated knowledge, producing durable, traceable Analysis
Results that a future Rule Framework evaluates policy against.

The archived Canonical Software Model specification's own
Evidence-Knowledge-Analysis-Findings pipeline uses "Analysis" to mean
evaluation of policy or heuristics against CSM knowledge. This
capability refines that single pipeline stage into two architectural
layers: the Analysis Framework performs policy-independent,
deterministic computation over CSM knowledge, and a future Rule
Framework layers policy interpretation on top of its output. This is a
refinement of that term into two layers, not a change to the archived
specification's own meaning of "Analysis" — every Analysis Result this
specification defines is a pre-policy computational artifact, never
itself a policy evaluation.

## ADDED Requirements

### Requirement: Deterministic, Snapshot-Driven Analysis Only
The Analysis Framework SHALL produce every Analysis Result by
deterministic computation over CSM Snapshot content only. It SHALL NOT
use AI/LLM reasoning, heuristic scoring, or probabilistic inference to
produce an Analysis Result in this version.

#### Scenario: No AI or heuristic judgment used to produce an Analysis Result
- **WHEN** the Analysis Framework runs any registered Analyzer
- **THEN** the resulting Analysis Result SHALL be derivable by
  deterministic computation alone, with no AI/LLM call or heuristic
  scoring step involved in producing it

#### Scenario: Repeated analysis over unchanged input is identical
- **WHEN** the Analysis Framework is run twice against the same CSM
  Snapshot content with the same registered Analyzers
- **THEN** both runs SHALL produce Analysis Results with identical
  identity and identical content

---

### Requirement: CSM Snapshot as Sole Analysis Input
The Analysis Framework SHALL construct its Analysis View, and
therefore every Analysis Result, exclusively from CSM Snapshot content.
It SHALL NOT depend on Policy/Rule Model content or Runtime Model
telemetry for its own analysis, and SHALL NOT read Repository Evidence
directly. This reflects the two-layer refinement of "Analysis"
described in Purpose: policy interpretation belongs to a future Rule
Framework, never to the Analysis Framework itself.

#### Scenario: Analysis proceeds without policy or runtime data
- **WHEN** a CSM Snapshot exists for a repository but no governance
  policy or runtime telemetry has been configured for it
- **THEN** the Analysis Framework SHALL still be able to run its
  registered Analyzers and produce Analysis Results for that snapshot

#### Scenario: No direct Repository Evidence dependency
- **WHEN** an Analyzer requires structural or relational information
  to complete its analysis
- **THEN** it SHALL obtain that information from the Analysis View
  alone, never by reading Repository Evidence directly

---

### Requirement: Single-Repository Analysis Scope
Since the Canonical Software Model is itself scoped to a single
repository as its unit of identity, the Analysis Framework SHALL scope
one analysis run to exactly one repository's CSM Snapshot. Analyzing
relationships that span multiple repositories is out of scope for this
version.

#### Scenario: One analysis run covers one repository's snapshot
- **WHEN** the Analysis Framework performs a run
- **THEN** every Analysis Result produced by that run SHALL reference
  exactly one repository's CSM Snapshot identity

#### Scenario: Cross-repository analysis is not attempted
- **WHEN** a CSM Snapshot represents a Module with an External System
  reference to another repository's service (per the CSM's own
  Multi-Repository Scope boundary)
- **THEN** the Analysis Framework SHALL analyze that reference only as
  it appears within the single snapshot being analyzed, and SHALL NOT
  attempt to retrieve or analyze the other repository's own CSM

---

### Requirement: CSM Snapshot Source Shape
A CSM Snapshot Source SHALL expose, at minimum: the CSM elements and
relationships constructed for a repository as of a specific
construction run, and a stable identity for that snapshot (at minimum,
the repository identifier and a value distinguishing it from other
snapshots of the same repository). The Analysis Framework SHALL depend
only on this shape, independent of which capability produced or how it
persisted the underlying snapshot.

#### Scenario: Snapshot source exposes elements, relationships, and identity
- **WHEN** the Analysis Framework reads a CSM Snapshot Source
- **THEN** it SHALL obtain that snapshot's CSM elements, its CSM
  relationships, and a stable identity distinguishing it from every
  other snapshot of the same repository

#### Scenario: Analysis Framework is agnostic to snapshot production
- **WHEN** two different CSM Snapshot Sources expose equivalent
  elements, relationships, and identity shape, produced by different
  upstream mechanisms
- **THEN** the Analysis Framework SHALL analyze both without requiring
  any change to its own behavior

---

### Requirement: Analysis View Construction
For a given CSM Snapshot Source, the Analysis Framework SHALL construct
exactly one Analysis View per analysis run before invoking any
Analyzer. The Analysis View SHALL represent, for every Subject (per the
CSM's own Subject Identification for Knowledge Assertions) present in
the snapshot's content, that Subject's effective knowledge (per the
CSM's Effective Knowledge and Precedence rules) together with its
conflict status (EFFECTIVE or CONFLICTED, per the CSM's Same-Category
Conflict Marking and Resolution rules). An Analyzer SHALL be able to
complete its analysis using only Analysis View content, without direct
access to the CSM Snapshot Source, Repository Evidence, or Policy/Rule
content.

#### Scenario: Analysis View reflects effective knowledge for an uncontested subject
- **WHEN** a Subject in the CSM Snapshot has exactly one knowledge
  assertion
- **THEN** the Analysis View SHALL represent that assertion as the
  Subject's effective knowledge, with status EFFECTIVE

#### Scenario: Analysis View marks conflicting knowledge as CONFLICTED
- **WHEN** a Subject in the CSM Snapshot has two or more competing,
  same-category knowledge assertions that conflict, per the CSM's own
  conflict-marking rules
- **THEN** the Analysis View SHALL represent that Subject's status as
  CONFLICTED, and SHALL preserve every competing assertion rather than
  arbitrarily selecting one

#### Scenario: Analysis performed without direct snapshot or evidence access
- **WHEN** an Analyzer evaluates a condition against the Analysis View
- **THEN** the Analyzer SHALL be able to complete that evaluation using
  only Analysis View content, without reading the CSM Snapshot Source,
  Repository Evidence, or Policy/Rule content directly

---

### Requirement: Analysis View Is a Derived Projection, Not a New Canonical Model
The Analysis View SHALL be a read-oriented projection derived entirely
from a CSM Snapshot's own content and the CSM's existing conflict-
marking mechanism. It SHALL NOT introduce a new entity kind,
relationship type, or persisted state beyond what the CSM Snapshot and
the CSM's own vocabulary already define, and it SHALL NOT itself be
treated as a durable artifact independent of the CSM Snapshot it was
derived from.

#### Scenario: Analysis View content is fully re-derivable from its source snapshot
- **WHEN** an Analysis View is constructed twice from the same CSM
  Snapshot content
- **THEN** both constructions SHALL produce equivalent Analysis View
  content, with no additional information present that did not already
  exist in the CSM Snapshot

#### Scenario: Analysis View is not independently persisted
- **WHEN** an analysis run completes
- **THEN** only the Analysis Results it produced SHALL be treated as
  durable output; the Analysis View itself SHALL NOT be stored as a
  separate, independently retrievable artifact

---

### Requirement: Analyzer Contract
A registered Analyzer SHALL declare a stable identifier, a
monotonically versioned identifier, and an explicit Analysis Scope (see
Analysis Scope Declaration). Given an Analysis View and an Analysis
Scope instance it is applicable to, an Analyzer SHALL deterministically
produce exactly one Analysis Result for that instance.

#### Scenario: Analyzer declares identifier, version, and scope
- **WHEN** an Analyzer is registered with the Analysis Framework
- **THEN** it SHALL carry a stable identifier, a version, and an
  Analysis Scope declaration

#### Scenario: Analyzer produces one Result per applicable scope instance
- **WHEN** an Analyzer is applicable to more than one Analysis Scope
  instance within a single Analysis View (see Analysis Scope
  Declaration)
- **THEN** the Analysis Framework SHALL invoke that Analyzer once per
  applicable instance, and each invocation SHALL produce its own
  Analysis Result

---

### Requirement: Analyzer Independence
An Analyzer SHALL NOT read another Analyzer's Analysis Result, and
SHALL NOT depend on any other Analyzer having been registered,
invoked, or having produced a result. The Analysis Framework SHALL NOT
provide a mechanism for declaring an ordering or dependency between
Analyzers in this version.

#### Scenario: Analyzer result is unaffected by which other Analyzers are registered
- **WHEN** an Analyzer is invoked against a given Analysis View
- **THEN** its Analysis Result SHALL be the same regardless of which
  other Analyzers are also registered and invoked in that run

#### Scenario: No declared dependency between Analyzers is honored
- **WHEN** the Analysis Framework registers a set of Analyzers
- **THEN** it SHALL NOT accept, require, or act on a declared
  execution-order dependency between any two of them

---

### Requirement: Analyzer Execution Order Independence
An Analyzer's Analysis Result SHALL NOT depend on the relative order in
which applicable Analyzers are invoked, nor on whether they are
invoked sequentially or concurrently with one another.

#### Scenario: Result is identical regardless of invocation order
- **WHEN** two applicable Analyzers are invoked in one relative order in
  one run, and in the reverse relative order in a separate run against
  the same Analysis View
- **THEN** each Analyzer's own Analysis Result SHALL be identical
  across both runs

---

### Requirement: Analysis Scope Declaration
Every registered Analyzer SHALL declare an Analysis Scope: a non-empty
set of CSM entity kinds and/or CSM relationship types the Analyzer
reads. An Analyzer MAY additionally declare a containment-level anchor
narrowing its invocation to specific contained elements (e.g. once per
Module) rather than the whole repository; an Analyzer with no declared
anchor SHALL be invoked once for the whole repository.

#### Scenario: Analyzer declares a kind-based scope
- **WHEN** an Analyzer is registered
- **THEN** its Analysis Scope SHALL identify at least one CSM entity
  kind or CSM relationship type it reads

#### Scenario: Unanchored Analyzer is invoked once per repository
- **WHEN** an Analyzer declares no containment-level anchor
- **THEN** the Analysis Framework SHALL invoke it exactly once per
  analysis run, covering the whole repository's Analysis View

#### Scenario: Anchored Analyzer is invoked once per matching contained element
- **WHEN** an Analyzer declares a containment-level anchor (e.g.
  Module)
- **THEN** the Analysis Framework SHALL invoke it once for each element
  at that containment level present in the CSM Snapshot, each
  invocation covering that element's own Analysis Scope instance

---

### Requirement: Kind-Based Analyzer Applicability
The Analysis Framework SHALL determine an Analyzer's applicability to a
given Analysis Scope instance by whether the corresponding CSM content
contains at least one element or relationship of a kind within that
Analyzer's declared Analysis Scope. An inapplicable Analyzer SHALL be
skipped without error and SHALL NOT produce an Analysis Result for that
instance.

#### Scenario: Analyzer with no matching kind present is skipped
- **WHEN** none of an Analyzer's declared Analysis Scope kinds are
  present in a given Analysis Scope instance's CSM content
- **THEN** the Analysis Framework SHALL skip that Analyzer for that
  instance without treating the absence as an error

#### Scenario: Analyzer with a matching kind present is invoked
- **WHEN** at least one of an Analyzer's declared Analysis Scope kinds
  is present in a given Analysis Scope instance's CSM content
- **THEN** the Analysis Framework SHALL invoke that Analyzer for that
  instance

---

### Requirement: Native-Attribute Applicability Refinement
An Analyzer MAY further restrict its own applicability using a
predicate over native-evidence-attribute content already present on
in-scope CSM elements or relationships (for example, restricting
itself to a specific build system or language ecosystem). This
refinement SHALL NOT require the Analysis Framework to introduce a
separate scope-declaration or registration mechanism for
language/ecosystem-specific applicability.

#### Scenario: Ecosystem-specific Analyzer is skipped when no matching native attribute is present
- **WHEN** an Analyzer's applicability predicate requires a native
  attribute value that is absent from every element/relationship
  within its Analysis Scope instance
- **THEN** the Analysis Framework SHALL skip that Analyzer for that
  instance without treating the absence as an error

#### Scenario: Ecosystem-specific Analyzer is invoked when its native attribute predicate is satisfied
- **WHEN** an Analyzer's applicability predicate matches a native
  attribute value present within its declared, kind-based Analysis
  Scope
- **THEN** the Analysis Framework SHALL invoke that Analyzer for that
  instance

---

### Requirement: Deterministic Analysis Result Identity
An Analysis Result's identity SHALL be a deterministic function of: the
producing Analyzer's identifier, the Analyzer's version, the source CSM
Snapshot's identity, and the specific Analysis Scope instance covered.
Random or otherwise non-reproducible identity generation SHALL NOT be
used.

#### Scenario: Identical inputs produce identical Result identity
- **WHEN** the same Analyzer, at the same version, is invoked against
  the same Analysis Scope instance derived from the same CSM Snapshot
  identity, across two separate analysis runs
- **THEN** both runs SHALL produce an Analysis Result with the same
  identity

#### Scenario: A different Analyzer version yields a distinguishable identity
- **WHEN** an Analyzer's version differs between two runs, with its
  Analysis Scope instance and source CSM Snapshot identity otherwise
  unchanged
- **THEN** the two runs' Analysis Result identities SHALL be
  distinguishable from one another

---

### Requirement: Analysis Result Traceability
Every Analysis Result SHALL retain a reference to its producing
Analyzer's identifier and version, a reference to the source CSM
Snapshot's identity, and the Analysis Scope instance it covers,
sufficient to trace the Result back to the CSM content it was computed
from.

#### Scenario: Result traceable to its producing Analyzer
- **WHEN** an Analysis Result is inspected
- **THEN** it SHALL be possible to determine which Analyzer, and which
  version of that Analyzer, produced it

#### Scenario: Result traceable to its source CSM Snapshot
- **WHEN** an Analysis Result is inspected
- **THEN** it SHALL be possible to determine which CSM Snapshot
  identity it was computed against

---

### Requirement: Analysis Result Payload Is Analyzer-Defined
The Analysis Framework SHALL treat an Analysis Result's substantive
analytical content as an opaque payload whose internal shape is defined
by its producing Analyzer. The Analysis Framework SHALL NOT constrain,
interpret, or validate that payload's internal structure beyond the
traceability and referential-integrity expectations defined elsewhere
in this specification.

#### Scenario: Differently-shaped payloads from different Analyzers are both accepted
- **WHEN** two different Analyzers produce Analysis Results with
  unrelated internal payload shapes
- **THEN** the Analysis Framework SHALL store and retrieve both without
  rejecting either on the basis of payload shape

#### Scenario: Framework operations do not require understanding payload content
- **WHEN** the Analysis Framework stores, retrieves, or validates an
  Analysis Result
- **THEN** it SHALL be able to do so without interpreting the meaning
  of that Result's payload content

---

### Requirement: Analysis Results Are Durable, Individually Identifiable Artifacts
The Analysis Framework SHALL treat each Analysis Result as an
individually identifiable artifact of an analysis run, retrievable
independent of the process that produced it, analogous to how a CSM
Snapshot is individually identifiable and retrievable. The specific
persistence mechanism is not constrained by this requirement.

#### Scenario: Analysis Result remains retrievable after the producing run ends
- **WHEN** an analysis run that produced an Analysis Result has
  completed and its process has exited
- **THEN** that Analysis Result SHALL remain retrievable by its
  identity

#### Scenario: A new Analysis Result does not overwrite a prior, differently-identified one
- **WHEN** a new analysis run produces an Analysis Result with an
  identity distinct from a prior run's Analysis Result
- **THEN** the prior Analysis Result SHALL remain retrievable and
  unaffected

---

### Requirement: Analysis Result Validation Before Publication
The Analysis Framework SHALL validate a constructed Analysis Result
before it is considered usable output, checking at minimum: every CSM
element or Subject identity the Result references SHALL exist within
the source CSM Snapshot it was computed against; the Result's declared
producing Analyzer and version SHALL correspond to a currently
registered Analyzer; and the Result's content SHALL remain within its
Analyzer's declared Analysis Scope. An Analysis Result that fails this
validation SHALL NOT be published as usable output.

#### Scenario: Valid Result is published as usable output
- **WHEN** a constructed Analysis Result satisfies every validation
  check
- **THEN** it SHALL be published as usable output

#### Scenario: Result referencing a CSM identity absent from its source snapshot is rejected
- **WHEN** a constructed Analysis Result references a CSM element or
  Subject identity that does not exist within the CSM Snapshot it
  claims to be computed against
- **THEN** it SHALL fail validation and SHALL NOT be published

#### Scenario: Result from an unregistered Analyzer/version is rejected
- **WHEN** a constructed Analysis Result declares a producing Analyzer
  identifier and version that does not correspond to a currently
  registered Analyzer
- **THEN** it SHALL fail validation and SHALL NOT be published

#### Scenario: Result exceeding its Analyzer's declared scope is rejected
- **WHEN** a constructed Analysis Result's content extends beyond its
  producing Analyzer's declared Analysis Scope
- **THEN** it SHALL fail validation and SHALL NOT be published

---

### Requirement: Analysis Results Are Not CSM Knowledge
An Analysis Result SHALL NOT be represented as, written into, or
otherwise treated as CSM content. It is a distinct concept in the
Evidence-Knowledge-Analysis-Findings pipeline the CSM specification
itself defines, referencing CSM elements by identity without becoming
part of the CSM.

#### Scenario: Producing an Analysis Result does not alter CSM content
- **WHEN** the Analysis Framework produces an Analysis Result from a
  CSM Snapshot
- **THEN** the source CSM Snapshot's content SHALL remain unchanged as
  a direct result of that production

#### Scenario: Analysis Results are stored separately from CSM content
- **WHEN** a consumer queries CSM content for a repository
- **THEN** that query SHALL NOT return Analysis Results as if they were
  CSM elements or relationships
