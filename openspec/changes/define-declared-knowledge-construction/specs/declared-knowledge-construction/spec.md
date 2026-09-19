## Purpose

Declared Knowledge Construction lets a human or authoritative external
source assert Architecture Component and Architectural Boundary
knowledge — CSM content the Canonical Software Model already defines
but that CSM Builder is, by design, forbidden from ever constructing —
validates it against an existing CSM baseline, and publishes it so it
can be combined with `observed` content for downstream Analysis and
Rule evaluation.

## ADDED Requirements

### Requirement: Declared Knowledge Construction Is Deterministic
Declared Knowledge Construction SHALL produce every constructed element
and relationship by deterministic transcription of an explicitly
supplied Declaration. It SHALL NOT use AI/LLM reasoning, heuristic
scoring, or probabilistic inference to construct, select, or modify any
element or relationship.

#### Scenario: No AI or heuristic judgment used to construct declared content
- **WHEN** Declared Knowledge Construction processes a supplied
  Declaration
- **THEN** the resulting element or relationship SHALL be derivable by
  deterministic transcription of that Declaration alone, with no
  AI/LLM call or heuristic scoring step involved

#### Scenario: Repeated construction over unchanged input is identical
- **WHEN** Declared Knowledge Construction is run twice against the
  same baseline CSM content and the same set of Declarations
- **THEN** both runs SHALL produce constructed content with identical
  identity and identical content

---

### Requirement: Declared Knowledge Scope Excludes Inferred Construction
Declared Knowledge Construction SHALL construct only elements and
relationships carrying `declared` provenance. It SHALL NOT construct,
select, or publish any element or relationship carrying `inferred`
provenance in this version.

#### Scenario: Only declared provenance is constructed
- **WHEN** Declared Knowledge Construction constructs an element or
  relationship from a Declaration
- **THEN** that element or relationship's provenance classification
  SHALL be `declared`

#### Scenario: No inferred content is ever produced
- **WHEN** Declared Knowledge Construction runs to completion
- **THEN** no element or relationship it produces SHALL carry
  `inferred` provenance

---

### Requirement: CSM Baseline as Referential Integrity Input
Declared Knowledge Construction SHALL accept a baseline CSM Snapshot
Source and use it only to validate that a Declaration's referenced CSM
element identities exist. It SHALL depend only on the CSM Snapshot
Source shape, independent of which capability produced or how it
persisted the underlying baseline, and SHALL NOT require a direct
dependency on CSM Builder or any other specific producer.

#### Scenario: Baseline exposes existing element identities for validation
- **WHEN** Declared Knowledge Construction validates a Declaration
- **THEN** it SHALL check the Declaration's referenced CSM element
  identities against the supplied baseline CSM Snapshot Source's own
  elements

#### Scenario: Construction is agnostic to baseline production
- **WHEN** two different CSM Snapshot Sources expose equivalent
  elements, produced by different upstream mechanisms
- **THEN** Declared Knowledge Construction SHALL validate Declarations
  against either without requiring any change to its own behavior

---

### Requirement: Architecture Component Declaration Shape
An Architecture Component Declaration SHALL identify a component name
and a non-empty set of existing CSM element identities it is composed
of. Declared Knowledge Construction SHALL construct an Architecture
Component element from a valid Architecture Component Declaration,
referencing that composition by identity rather than duplicating the
referenced elements' own content.

#### Scenario: Valid composition declaration constructs a component
- **WHEN** an Architecture Component Declaration names one or more CSM
  element identities present in the supplied baseline
- **THEN** Declared Knowledge Construction SHALL construct an
  Architecture Component element referencing that composition by
  identity

#### Scenario: Declaration referencing a nonexistent element identity is rejected
- **WHEN** an Architecture Component Declaration names a CSM element
  identity absent from the supplied baseline
- **THEN** Declared Knowledge Construction SHALL reject that
  Declaration and SHALL NOT construct an Architecture Component from it

---

### Requirement: Boundary Constraint Declaration Shape
A Boundary Constraint Declaration SHALL identify a source Architecture
Component identity, a target Architecture Component or External System
identity, and a constraint kind. Declared Knowledge Construction SHALL
construct a Boundary Constraint relationship from a valid Boundary
Constraint Declaration only when its source and target identities
correspond to Architecture Components (or an External System)
constructed or already present within the same publication batch or
baseline.

#### Scenario: Valid boundary declaration constructs a boundary relationship
- **WHEN** a Boundary Constraint Declaration's source and target
  identities correspond to Architecture Components constructed in the
  same batch
- **THEN** Declared Knowledge Construction SHALL construct a Boundary
  Constraint relationship between them

#### Scenario: Declaration referencing an unpublished component identity is rejected
- **WHEN** a Boundary Constraint Declaration's source or target
  identity does not correspond to any Architecture Component
  constructed in the same batch or present in the baseline
- **THEN** Declared Knowledge Construction SHALL reject that
  Declaration and SHALL NOT construct a Boundary Constraint
  relationship from it

---

### Requirement: Must-Not-Depend-On Constraint Vocabulary Reuse
Declared Knowledge Construction SHALL express a "must not depend on"
Boundary Constraint Declaration using the existing `constraint-kind`
native-attribute convention (`constraint-kind` = `must-not-depend-on`)
already used to encode this constraint shape. It SHALL NOT introduce a
second, independent encoding for the same constraint shape.

#### Scenario: Must-not-depend-on declaration uses the existing convention
- **WHEN** Declared Knowledge Construction constructs a Boundary
  Constraint relationship from a "must not depend on" Declaration
- **THEN** the constructed relationship SHALL carry the
  `constraint-kind` native attribute with value `must-not-depend-on`

---

### Requirement: Declared Provenance on Every Constructed Element
Every element or relationship Declared Knowledge Construction
constructs SHALL carry a `declared` provenance classification. It SHALL
NOT construct an Architecture Component or Boundary Constraint
relationship carrying `observed` or `inferred` provenance.

#### Scenario: Constructed component carries declared provenance
- **WHEN** Declared Knowledge Construction constructs an Architecture
  Component element
- **THEN** that element's provenance classification SHALL be `declared`

#### Scenario: Constructed boundary relationship carries declared provenance
- **WHEN** Declared Knowledge Construction constructs a Boundary
  Constraint relationship
- **THEN** that relationship's provenance classification SHALL be
  `declared`

---

### Requirement: Declared Knowledge Validation Before Publication
Declared Knowledge Construction SHALL validate constructed content
before it is considered usable output, checking at minimum: every CSM
element identity an Architecture Component Declaration references
SHALL exist within the supplied baseline; every identity a Boundary
Constraint Declaration references SHALL correspond to an Architecture
Component constructed in the same batch or present in the baseline; and
every constructed element or relationship's provenance classification
SHALL be `declared`. Content that fails this validation SHALL NOT be
published as usable output, and SHALL NOT be written to a Declared
Knowledge Store as valid declared content.

#### Scenario: Valid declared content is published
- **WHEN** constructed declared content satisfies every validation
  check
- **THEN** it SHALL be published as usable output and written to the
  Declared Knowledge Store

#### Scenario: Invalid declared content is never published or stored
- **WHEN** constructed declared content fails any validation check
- **THEN** it SHALL NOT be published as usable output, and SHALL NOT
  be written to the Declared Knowledge Store

---

### Requirement: Deterministic Declared Knowledge Artifact Identity
A published declared-knowledge artifact's identity SHALL be a
deterministic function of the repository identity, the baseline CSM
Snapshot Source's own identity it was validated against, and a
sequence value distinguishing it from other declared-knowledge
artifacts for the same repository. Random or otherwise non-reproducible
identity generation SHALL NOT be used.

#### Scenario: Identical inputs produce identical artifact identity
- **WHEN** the same set of Declarations is validated against the same
  baseline identity across two separate construction runs
- **THEN** both runs SHALL produce a declared-knowledge artifact with
  the same identity

---

### Requirement: Declared Knowledge Artifacts Are Durable, Individually Identifiable
Declared Knowledge Construction SHALL treat each published
declared-knowledge artifact as an individually identifiable artifact,
retrievable independent of the process that produced it, through a
Declared Knowledge Store abstraction. The specific persistence
mechanism is not constrained by this requirement.

#### Scenario: Declared knowledge artifact remains retrievable after the producing run ends
- **WHEN** a construction run that published a declared-knowledge
  artifact has completed and its process has exited
- **THEN** that artifact SHALL remain retrievable by its identity

---

### Requirement: Declared Knowledge Is Published as a CSM Snapshot Source
Declared Knowledge Construction SHALL publish its constructed content
as an artifact conforming to the CSM Snapshot Source shape, exposing
only the elements and relationships it constructed, tied to the same
repository identity as the baseline it was validated against.

#### Scenario: Published declared content conforms to the CSM Snapshot Source shape
- **WHEN** declared content is published
- **THEN** the resulting artifact SHALL expose its constructed
  elements, its constructed relationships, and a stable identity, per
  the CSM Snapshot Source shape

---

### Requirement: Combining Observed and Declared CSM Content
It SHALL be possible to combine one CSM Snapshot Source of `observed`
content and one published declared-knowledge artifact of the same
repository identity into a single combined CSM Snapshot Source exposing
the union of both sources' elements and relationships. Combining SHALL
NOT require the producer of `observed` content and Declared Knowledge
Construction to depend on one another, and SHALL NOT perform any
conflict resolution beyond exposing the union — resolving competing
knowledge for the same subject remains the responsibility of the
existing Effective Knowledge and Precedence mechanism applied
downstream.

#### Scenario: Combined source exposes both sources' content
- **WHEN** an observed CSM Snapshot Source and a declared-knowledge
  artifact of the same repository are combined
- **THEN** the combined CSM Snapshot Source SHALL expose every element
  and relationship from both sources

#### Scenario: Combining requires no dependency between the two producers
- **WHEN** an observed CSM Snapshot Source and a declared-knowledge
  artifact are combined
- **THEN** neither producer's own code SHALL be required to depend on
  the other's

#### Scenario: Sources of different repositories cannot be combined
- **WHEN** two CSM Snapshot Sources with different repository
  identities are supplied for combination
- **THEN** combining SHALL be rejected rather than silently producing a
  combined source spanning two repositories

---

### Requirement: Declared Knowledge Is Not Constructed by CSM Builder
Declared Knowledge Construction's mechanism SHALL remain independent of
CSM Builder's own construction packages. This requirement does not
modify CSM Builder's existing exclusion of Architecture Component or
Boundary Constraint construction; it restates that Declared Knowledge
Construction is the mechanism satisfying the gap that exclusion
deliberately left open, not a relaxation of it.

#### Scenario: CSM Builder's own construction remains unaffected
- **WHEN** Declared Knowledge Construction is introduced
- **THEN** CSM Builder's own construction packages SHALL remain
  forbidden from constructing an Architecture Component or Boundary
  Constraint relationship, unchanged
