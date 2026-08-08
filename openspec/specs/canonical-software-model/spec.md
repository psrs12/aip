# Canonical Software Model Specification

## Purpose

The Canonical Software Model (CSM) is the durable, language-independent
structural and relational representation of a software system that AIP's
architecture, design, risk, and compliance analysis is performed against,
carrying explicit provenance and confidence so that observed fact, declared
intent, and inferred interpretation remain distinguishable throughout
downstream analysis.

## Requirements

### Requirement: CSM Purpose and Reasoning Target
The CSM SHALL serve as the sole structural/relational representation that
architecture, design, risk, and compliance analysis is performed against,
such that an analyzer or rule needs no direct access to source code,
configuration, or infrastructure definitions to evaluate structural or
relational conditions.

#### Scenario: Analysis performed without direct source access
- **WHEN** a rule evaluates a dependency-direction policy against a system
- **THEN** the rule SHALL be able to complete evaluation using only CSM
  content (entities, relationships, provenance, confidence), without
  reading source files, build files, or infrastructure definitions directly

#### Scenario: CSM absent or incomplete for a system
- **WHEN** no CSM has yet been built for a given repository
- **THEN** architecture, design, risk, and compliance analysis SHALL NOT be
  reported as having been performed against that repository

---

### Requirement: CSM Scope Boundary
The CSM SHALL be limited to structural and relational facts about a
software system that are needed for cross-cutting architecture, design,
risk, and compliance reasoning, expressed in language-neutral terms. The
CSM SHALL NOT contain raw evidence bodies (including Source File content,
see Source File Representation), configuration values or secrets, full
infrastructure-as-code detail, governance policies or architecture
constraints, runtime telemetry streams, business organizational detail
beyond thin cross-reference metadata, Findings, or cross-snapshot trend
analytics.

#### Scenario: Raw evidence excluded from the CSM
- **WHEN** a CSM element is built from a source file
- **THEN** the CSM element SHALL reference the originating evidence by
  location rather than embedding the evidence's raw content (e.g. source
  text, full AST, method body)

#### Scenario: Configuration values excluded from the CSM
- **WHEN** a module reads a configuration key or references a secret
- **THEN** the CSM SHALL represent the fact that the reference exists
  without storing the configuration value or secret content

#### Scenario: Governance policy excluded from the CSM
- **WHEN** an organization defines a governance policy or architecture
  constraint
- **THEN** the policy or constraint definition SHALL NOT be stored as CSM
  content, and SHALL instead be represented in a separate Policy/Rule Model
  that references CSM vocabulary

#### Scenario: Runtime telemetry excluded from the CSM
- **WHEN** an observability platform emits traces, metrics, or logs for a
  deployed system
- **THEN** the raw telemetry stream SHALL NOT be stored as CSM content

---

### Requirement: Preservation of Evidence, Knowledge, Analysis, Findings, Recommendations, and Remediation as Distinct Concepts
The specification and any content built from it SHALL preserve the
distinction between: Evidence (a raw, language/tool-specific observation),
Knowledge (an interpreted, language-neutral, provenance-tagged CSM element
built from evidence, declaration, or inference), Analysis (the evaluation
of policy or heuristics against CSM knowledge), Findings (the structured
output of Analysis), Recommendations (AI- or rule-generated guidance
attached to a Finding), and Remediation (a proposed or applied change
addressing a Finding). No CSM element SHALL conflate two or more of these
concepts into a single artifact.

#### Scenario: Evidence is not stored as knowledge
- **WHEN** a language analyzer extracts a raw import statement from source
- **THEN** the CSM SHALL store the interpreted relationship (e.g. a
  `depends-on` edge) as knowledge with a reference to the originating
  evidence, and SHALL NOT store the raw import statement text as a CSM
  element in its own right

#### Scenario: Findings are not stored in the CSM
- **WHEN** a policy is evaluated against a CSM snapshot and produces a
  violation
- **THEN** the resulting Finding SHALL be produced and stored as a
  separate artifact referencing the CSM snapshot and elements involved,
  and SHALL NOT be written back into the CSM as a structural or relational
  element

#### Scenario: Recommendations and remediation reference but do not alter the CSM
- **WHEN** a Recommendation or Remediation proposal is generated for a
  Finding
- **THEN** it SHALL reference the relevant CSM elements by identity and
  SHALL NOT silently modify CSM knowledge as a side effect of being
  generated

---

### Requirement: CSM Conceptual Vocabulary
The CSM SHALL define a core, versioned set of language-neutral structural
entity kinds and relationship types sufficient to represent containment,
code structure, dependency, and architectural grouping, independent of any
single programming language's native constructs. This core vocabulary is
intentionally non-exhaustive: additional entity kinds and relationship
types MAY be introduced in future versioned specification changes,
following the CSM Versioning and Evolution requirement, without
invalidating existing CSM content.

The core structural entity kinds SHALL include, at minimum: Repository,
Project, Module, Package, Type, Architecture Component, and External
System. Method is also part of the core vocabulary, but its instantiation
by a given analyzer is optional (see Method-Level Representation
Capability). The core relationship types SHALL include, at minimum:
containment, dependency, implementation/extension, invocation,
exposure/consumption, integration, composition, and boundary/constraint
relationships.

#### Scenario: Entity expressed using core vocabulary
- **WHEN** a language analyzer produces evidence for a structural element
- **THEN** the corresponding CSM element SHALL be classified as one of the
  defined entity kinds, and SHALL NOT introduce a new entity kind outside
  the defined vocabulary without a deliberate, versioned specification
  change

#### Scenario: Unmappable language construct preserved without vocabulary expansion
- **WHEN** a language analyzer encounters a native construct that does not
  map cleanly onto the defined vocabulary
- **THEN** the construct SHALL be preserved as an opaque native-evidence
  attribute on the nearest matching CSM element, and SHALL NOT cause a new
  entity kind or relationship type to be introduced without a deliberate,
  versioned specification change

#### Scenario: Vocabulary extended without breaking existing content
- **WHEN** a future specification change introduces a new entity kind or
  relationship type not present in the core vocabulary
- **THEN** existing CSM elements and relationships expressed using the
  prior core vocabulary SHALL remain valid and unaffected by the extension

---

### Requirement: Source File Representation
Source File SHALL NOT be represented as a first-class CSM entity kind.
Source File and its content SHALL remain part of the Repository Evidence
Model. Type and Method CSM elements MAY retain a source-location reference
(e.g., file path and position) as an attribute for traceability purposes,
without that reference constituting a CSM relationship to a Source File
entity.

#### Scenario: Source File absent from CSM vocabulary
- **WHEN** the CSM's defined entity kinds are enumerated
- **THEN** Source File SHALL NOT appear among them

#### Scenario: Source location retained as an attribute
- **WHEN** a Type or Method CSM element is created from evidence
- **THEN** it MAY carry a source-location attribute (file path and
  position) referencing the originating evidence, without introducing a
  Source File node or a containment relationship to one

#### Scenario: File-level concerns remain evidence concerns
- **WHEN** reasoning is required about a file's raw content or file-level
  organization (e.g., multiple types declared in one file)
- **THEN** that reasoning SHALL be performed against the Repository
  Evidence Model rather than the CSM

---

### Requirement: Method-Level Representation Capability
Type-level representation SHALL be the minimum required capability for any
language analyzer contributing to the CSM: every analyzer SHALL be capable
of producing Type CSM elements for the language(s) it supports.
Method-level CSM elements are OPTIONAL and MAY be omitted by an analyzer
that does not support method-level extraction. Where an analysis capability
requires method-level detail (including, at minimum, call relationships,
data flow analysis, or security flow analysis), the CSM SHALL represent
Methods as CSM elements sufficient to support that analysis; where
method-level elements are absent, analyses requiring them SHALL NOT be
reported as having been performed.

#### Scenario: Type-only analyzer produces a valid CSM
- **WHEN** a language analyzer supports Type-level extraction only
- **THEN** the resulting CSM content SHALL be valid without any Method
  elements present

#### Scenario: Method elements available when required
- **WHEN** a call-relationship, data-flow, or security-flow analysis is to
  be performed against a given language's evidence
- **THEN** the CSM for that evidence SHALL include Method-level elements
  sufficient to support that analysis

#### Scenario: Method-dependent analysis not claimed without method-level elements
- **WHEN** Method-level elements are absent for a given Module
- **THEN** any analysis requiring call relationships, data flow, or
  security flow for that Module SHALL NOT be reported as having been
  performed

---

### Requirement: Containment Relationships Between Structural Entities
The CSM SHALL represent the containment hierarchy of a software system:
Repository contains Project(s), Project contains Module(s), Module
contains Package(s), and Package contains Type(s). Where Method-level
representation is present for a Type (see Method-Level Representation
Capability), that Type contains or declares its Method(s).

#### Scenario: Containment traversal
- **WHEN** a consumer queries the CSM for all Types contained within a
  given Module
- **THEN** the CSM SHALL return every Type reachable via containment
  relationships from that Module through its Packages

#### Scenario: Type contains Method when Method-level representation is present
- **WHEN** a language analyzer provides Method-level representation for a
  given Type (see Method-Level Representation Capability)
- **THEN** the CSM SHALL represent that Type as containing or declaring
  its Method(s) via a containment relationship, and this Type-to-Method
  containment SHALL NOT be represented when the analyzer provides no
  Method-level representation for that Type

---

### Requirement: Dependency and Structural Relationships
The CSM SHALL represent relationships between structural entities beyond
containment, including dependency, implementation/extension, invocation,
and exposure/consumption. Each dependency relationship SHALL be qualified
by a dependency kind (at minimum: compile-time, runtime, or test-only)
where that distinction is discoverable from evidence. The invocation
relationship type applies only where Method-level elements are present for
the entities involved.

#### Scenario: Dependency direction is queryable
- **WHEN** a rule evaluates whether Module A depends on Module B
- **THEN** the CSM SHALL expose a directed dependency relationship from A
  to B if, and only if, such a dependency is represented as CSM knowledge

#### Scenario: Dependency kind is preserved when discoverable
- **WHEN** a dependency is discoverable as test-only from build evidence
- **THEN** the corresponding CSM relationship SHALL be qualified as
  test-only rather than represented identically to a compile-time
  dependency

---

### Requirement: Repository Evidence to CSM Transformation
The CSM SHALL be constructed exclusively from a defined Repository Evidence
Model via a transformation stage that maps evidence onto the CSM's closed
vocabulary. Every CSM element derived from evidence SHALL retain a
traceable reference back to the evidence and source location that produced
it. The CSM SHALL NOT be populated by direct, untraced write access from an
analyzer.

#### Scenario: Traceability from CSM element to evidence
- **WHEN** a CSM element representing a Type is inspected
- **THEN** it SHALL be possible to trace that element back to the specific
  evidence (e.g. file and location) from which it was derived

#### Scenario: Evidence without corresponding CSM element
- **WHEN** repository evidence exists but has not yet been processed by the
  transformation stage
- **THEN** no corresponding CSM element SHALL be considered to exist until
  the transformation stage has produced it

---

### Requirement: Evidence and Knowledge Distinction
The CSM SHALL distinguish Evidence (a raw, uninterpreted, language/tool-
specific observation) from Knowledge (an interpreted, language-neutral CSM
element built from one or more pieces of evidence, a declaration, or an
inference). Every CSM element SHALL be Knowledge; no CSM element SHALL be,
or be treated as, raw Evidence.

#### Scenario: CSM element is always interpreted knowledge
- **WHEN** a CSM element is created
- **THEN** it SHALL carry a provenance record (see Structured Provenance
  Record) identifying how it was derived, distinguishing it from the
  uninterpreted evidence it may reference

---

### Requirement: Provenance Classification of Knowledge
Every CSM element SHALL carry a provenance classification of exactly one
of: **observed** (mechanically derived from evidence), **declared**
(asserted by a human or an authoritative external source), or **inferred**
(produced by AIP's own heuristics or AI reasoning). A CSM element's
provenance classification SHALL be preserved and exposed to any consumer
of that element. Where multiple elements with different provenance
classifications exist for the same subject, they SHALL be reconciled only
as described in Non-Destructive Preservation of Competing Knowledge and
Effective Knowledge and Precedence, never by discarding a classification.

#### Scenario: Observed dependency from build evidence
- **WHEN** a dependency relationship is derived directly from a build
  manifest
- **THEN** the relationship SHALL be classified as observed

#### Scenario: Declared architecture component
- **WHEN** an architect explicitly names a set of packages as a component
- **THEN** the resulting Architecture Component element SHALL be
  classified as declared

#### Scenario: Inferred architecture component
- **WHEN** AIP heuristically groups packages into a candidate component
  without an explicit human declaration
- **THEN** the resulting Architecture Component element SHALL be
  classified as inferred, and SHALL NOT be classified as observed or
  declared

---

### Requirement: Structured Provenance Record
Each CSM element's provenance SHALL be recorded as a structured record
containing, at minimum: the provenance category (observed, declared, or
inferred), a stable reference to the source (the originating evidence, the
declaring actor or document, or the inference process), and a timestamp
indicating when the knowledge was established.

#### Scenario: Provenance record supports explanation
- **WHEN** a Finding references a CSM element
- **THEN** it SHALL be possible to trace from the Finding, through the CSM
  element's provenance record, to the originating evidence, declaration, or
  inference that produced the element

#### Scenario: Missing provenance is invalid
- **WHEN** a CSM element is constructed without a provenance record
- **THEN** the element SHALL be considered invalid CSM content (see CSM
  Validation Expectations)

---

### Requirement: Confidence for Inferred Knowledge
Every CSM element or relationship classified as inferred SHALL carry a
confidence level expressed using one of three qualitative levels: **HIGH**,
**MEDIUM**, or **LOW**. Elements classified as observed SHALL NOT require a
confidence level, and SHALL be treated as fact subject only to
evidence-quality caveats. Declared elements MAY carry a confidence level
when sourced from an unverified external source, but are otherwise treated
as authoritative.

The qualitative confidence model SHALL remain extensible: a future
specification change MAY introduce a numeric confidence representation
(e.g., a 0-1 score) in addition to, or as a refinement of, the qualitative
levels, provided each qualitative level maps unambiguously to a defined
numeric range and existing qualitative-only CSM content remains valid
without modification.

#### Scenario: Inferred element without confidence is invalid
- **WHEN** a CSM element is classified as inferred but has no confidence
  level
- **THEN** the element SHALL be considered invalid CSM content

#### Scenario: Confidence expressed using one of three defined levels
- **WHEN** a confidence level is assigned to an inferred CSM element
- **THEN** the value SHALL be exactly one of HIGH, MEDIUM, or LOW, and no
  other value SHALL be considered valid

#### Scenario: Confidence available for downstream gating
- **WHEN** a policy or automation workflow needs to distinguish
  high-confidence from low-confidence inferred knowledge
- **THEN** the CSM SHALL expose the confidence level of any inferred
  element for that purpose

#### Scenario: Numeric confidence introduced without breaking existing content
- **WHEN** a future specification change introduces a numeric confidence
  representation
- **THEN** existing CSM elements carrying only a qualitative HIGH, MEDIUM,
  or LOW level SHALL remain valid without requiring modification

---

### Requirement: Subject Identification for Knowledge Assertions
For the purposes of provenance reconciliation (Non-Destructive
Preservation of Competing Knowledge, Effective Knowledge and Precedence,
and Same-Category Conflict Marking and Resolution), a **subject** SHALL be
identified as the combination of: (a) the anchored CSM entity or entities
the assertion is about, and (b) the specific relationship type or
attribute being asserted about that anchor (for example, its Architecture
Component membership, its dependency-kind classification toward a
particular other entity, or its criticality). Two knowledge assertions
SHALL be considered to apply to the same subject — and therefore eligible
to compete, conflict, or be reconciled with one another — if, and only if,
both (a) and (b) match exactly. Assertions that share an anchor entity but
assert different relationship types or attributes SHALL NOT be considered
knowledge about the same subject.

#### Scenario: Differing assertion types are not the same subject
- **WHEN** a declared assertion states a Module's Architecture Component
  membership and a separate observed assertion states the same Module's
  dependency-kind classification toward another Module
- **THEN** the two assertions SHALL NOT be considered knowledge about the
  same subject, and neither SHALL be treated as competing with the other

#### Scenario: Matching anchor and assertion type is the same subject
- **WHEN** two assertions both state the Architecture Component membership
  of the same Module
- **THEN** the two assertions SHALL be considered knowledge about the same
  subject, and are therefore eligible to compete under precedence or
  same-category conflict rules

#### Scenario: Subject identification applied consistently
- **WHEN** the CSM determines whether assertions are competing, effective,
  or conflicting
- **THEN** Non-Destructive Preservation of Competing Knowledge, Effective
  Knowledge and Precedence, and Same-Category Conflict Marking and
  Resolution SHALL all apply this same subject-identification rule, rather
  than each defining subject equivalence independently

---

### Requirement: Non-Destructive Preservation of Competing Knowledge
When evidence, a declaration, or an inference produces knowledge about a
subject (see Subject Identification for Knowledge Assertions) that already
has existing CSM knowledge associated with it, the CSM SHALL preserve all
competing elements rather than overwriting or deleting prior knowledge.
Each competing element SHALL retain its own distinct, structured
provenance record (see Structured Provenance Record) so that declared,
observed, and inferred knowledge about the same subject remain
independently identifiable and auditable.

#### Scenario: Competing inferred and declared knowledge both retained
- **WHEN** an Architecture Component is first created as inferred and a
  human later declares a conflicting grouping for overlapping structure
- **THEN** the CSM SHALL retain both the inferred element and the declared
  element as distinct, individually provenance-tagged elements, rather
  than deleting or overwriting the inferred element

#### Scenario: Superseded knowledge remains available for audit
- **WHEN** an effective/reconciled view of knowledge is computed for a
  subject with competing elements (see Effective Knowledge and Precedence)
- **THEN** the knowledge not selected as effective SHALL remain retrievable
  in the CSM for auditability and explainability, rather than being
  removed

---

### Requirement: Effective Knowledge and Precedence
The CSM SHALL support deriving an effective (reconciled) view of knowledge
for a given subject (see Subject Identification for Knowledge Assertions)
when multiple competing elements with different provenance classifications
exist for that subject. The effective view SHALL be determined using the
precedence order **DECLARED > OBSERVED > INFERRED**, such that declared
knowledge is treated as effective over observed knowledge, and observed
knowledge is treated as effective over inferred knowledge, for the same
subject. Determining the effective view SHALL NOT delete, overwrite, or
otherwise remove the lower-precedence knowledge (see Non-Destructive
Preservation of Competing Knowledge). Where competing elements for the
same subject share the same precedence category and conflict with one
another, precedence alone SHALL NOT determine an effective element, since
precedence distinguishes only between categories, not within one (see
Same-Category Conflict Marking and Resolution).

#### Scenario: Declared knowledge takes precedence over observed knowledge
- **WHEN** an architect declares that the dependency from Module A to
  Module B is to be classified as test-only, while build evidence
  mechanically observed that same dependency as compile-time
- **THEN** the effective knowledge for that dependency's kind
  classification SHALL be the declared test-only classification, and the
  observed compile-time classification SHALL remain recorded but SHALL NOT
  be treated as effective

#### Scenario: Observed knowledge takes precedence over inferred knowledge
- **WHEN** an inferred Architecture Component grouping conflicts with an
  observed structural relationship for the same subject
- **THEN** the effective knowledge SHALL follow the observed relationship,
  and the inferred grouping SHALL remain recorded but SHALL NOT be treated
  as effective

#### Scenario: Inferred knowledge is effective when uncontested
- **WHEN** only inferred knowledge exists for a subject with no competing
  declared or observed knowledge
- **THEN** the inferred knowledge SHALL be treated as the effective
  knowledge for that subject

#### Scenario: Effective knowledge selection is explainable
- **WHEN** a consumer queries the effective knowledge for a subject that
  has competing elements
- **THEN** the CSM SHALL be able to indicate which element was selected as
  effective and that it was selected by precedence, and SHALL be able to
  list the lower-precedence alternatives on request

---

### Requirement: Same-Category Conflict Marking and Resolution
When multiple knowledge assertions apply to the same subject (see Subject
Identification for Knowledge Assertions), share the same precedence
category (all DECLARED, all OBSERVED, or all INFERRED), and conflict with
one another, the CSM SHALL NOT arbitrarily select one assertion as
effective. All conflicting assertions SHALL be preserved, each with its
own structured provenance record (see Structured Provenance Record and
Non-Destructive Preservation of Competing Knowledge). The effective
knowledge for that subject SHALL be marked as **CONFLICTED** until an
explicit resolution is provided.

A resolution SHALL be recorded as a CSM knowledge element carrying the
**declared** provenance category (see Provenance Classification of
Knowledge); resolving a conflict SHALL NOT introduce, or be treated as, a
fourth provenance category. A resolution's structured provenance record
SHALL identify: the resolving actor or authoritative source, a timestamp,
and a reference sufficient to audit which conflicting assertions the
resolution addresses. The resolution SHALL remain auditable alongside the
conflicting assertions it resolves.

#### Scenario: Two conflicting declared assertions marked CONFLICTED
- **WHEN** two declared assertions about the same subject conflict (e.g.,
  two architects declare different, incompatible Component memberships for
  the same Module)
- **THEN** the CSM SHALL preserve both declared assertions with their
  provenance and SHALL mark the effective knowledge for that subject as
  CONFLICTED rather than selecting either arbitrarily

#### Scenario: Two conflicting observed assertions marked CONFLICTED
- **WHEN** two observed assertions about the same subject conflict (e.g.,
  two independent analyzer passes produce contradictory observed
  relationships for the same subject)
- **THEN** the CSM SHALL preserve both observed assertions with their
  provenance and SHALL mark the effective knowledge for that subject as
  CONFLICTED rather than selecting either arbitrarily

#### Scenario: Two conflicting inferred assertions marked CONFLICTED
- **WHEN** two inferred assertions about the same subject conflict (e.g.,
  two heuristics produce contradictory candidate Component groupings for
  the same underlying structure)
- **THEN** the CSM SHALL preserve both inferred assertions with their
  provenance and SHALL mark the effective knowledge for that subject as
  CONFLICTED rather than selecting either arbitrarily

#### Scenario: Conflict is not silently resolved by precedence
- **WHEN** conflicting assertions for the same subject share the same
  precedence category
- **THEN** the precedence order defined in Effective Knowledge and
  Precedence SHALL NOT be used to select between them

#### Scenario: Explicit resolution carries DECLARED provenance
- **WHEN** a subject marked CONFLICTED is explicitly resolved
- **THEN** the resolution SHALL be recorded with the declared provenance
  category, its provenance record SHALL identify the resolving actor or
  authoritative source and a timestamp, and it SHALL reference the
  conflicting assertions it resolves, remaining traceable alongside them

#### Scenario: Resolution does not introduce a fourth provenance category
- **WHEN** a conflict resolution is recorded
- **THEN** its provenance category SHALL be declared, and no provenance
  category other than observed, declared, or inferred SHALL be introduced
  or accepted as valid

#### Scenario: Resolution does not delete conflicting assertions
- **WHEN** a CONFLICTED subject is explicitly resolved
- **THEN** the previously conflicting assertions SHALL remain retrievable
  for auditability, consistent with Non-Destructive Preservation of
  Competing Knowledge

---

### Requirement: Architecture Component Representation
The CSM SHALL represent an Architecture Component as a named grouping
composed of one or more structural entities (at minimum, Modules or
Packages). An Architecture Component SHALL always carry a provenance
classification of declared or inferred, and SHALL NOT be classified as
observed.

#### Scenario: Component composed of existing structural elements
- **WHEN** an Architecture Component is defined
- **THEN** it SHALL reference one or more existing Module or Package
  elements as its composition, rather than duplicating their structure

#### Scenario: Component provenance is never observed
- **WHEN** an Architecture Component element is created by any means
- **THEN** its provenance classification SHALL be declared or inferred,
  and validation SHALL reject a component classified as observed

---

### Requirement: Architectural Boundary Representation
The CSM SHALL represent an Architectural Boundary as a relationship between
two or more Architecture Components (or an Architecture Component and an
External System) describing an intended structural constraint (e.g. "must
not depend on", "must only communicate via"). An Architectural Boundary
SHALL carry a provenance classification of declared or inferred. Evaluating
compliance with a boundary is a function of the Policy/Rule Model and SHALL
NOT be performed by the CSM itself.

#### Scenario: Boundary declared between components
- **WHEN** an architect declares that Component A must not depend on
  Component B
- **THEN** the CSM SHALL represent this as a boundary relationship between
  Component A and Component B with declared provenance

#### Scenario: CSM does not evaluate boundary compliance
- **WHEN** a boundary relationship exists in the CSM alongside an observed
  dependency that appears to violate it
- **THEN** the CSM SHALL represent both facts (the boundary and the
  observed dependency) without itself producing a violation Finding

---

### Requirement: Business Context as a Mapping Layer
The CSM SHALL represent business context (Business Capability, Domain,
Ownership, System Criticality) as distinct elements connected to technical
elements (Architecture Component, Module) through explicit mapping
relationships, rather than as attributes fused directly onto technical
elements' core identity. Business context elements SHALL carry declared
provenance.

#### Scenario: Capability mapped to realizing components
- **WHEN** a Business Capability is declared as realized by a set of
  Architecture Components
- **THEN** the CSM SHALL represent this via a mapping relationship, and the
  Architecture Components' own identity and structure SHALL remain
  unaffected by the mapping

#### Scenario: Business context change does not invalidate technical knowledge
- **WHEN** the ownership or criticality mapping for a Module changes
- **THEN** the observed structural and relational knowledge for that
  Module SHALL remain unaffected and SHALL NOT be re-derived as a result

---

### Requirement: External System Representation
The CSM SHALL represent an External System as a boundary node with an
opaque interior and an attributed exterior (at minimum: name, kind,
criticality, integration protocol, and owner, where known). The CSM SHALL
NOT attempt to represent the internal structure of an External System.

#### Scenario: Outbound integration represented without modeling internals
- **WHEN** a Module integrates with a third-party system
- **THEN** the CSM SHALL represent the third-party system as an External
  System boundary node connected via an integration relationship, without
  containing any Type, Method, or Package elements for that third-party
  system

#### Scenario: External system attributes without internals
- **WHEN** criticality or ownership information is available for an
  External System
- **THEN** the CSM SHALL represent that information as an attribute of the
  External System boundary node

---

### Requirement: Architecture Constraints Excluded from the CSM
Architecture Constraints and Governance Policies SHALL NOT be represented
as CSM content. They SHALL be represented in a separate Policy/Rule Model
that is expressed in terms of CSM vocabulary (entity kinds, relationship
types, and specific element identities) and is evaluated against a CSM
snapshot.

#### Scenario: Constraint references CSM vocabulary without being stored in the CSM
- **WHEN** a governance policy is defined referencing "any Module must not
  depend on any External System without an approved integration boundary"
- **THEN** the policy definition SHALL be stored in the Policy/Rule Model,
  and the CSM SHALL contain no element representing the policy itself

#### Scenario: CSM snapshot remains unaffected by policy changes
- **WHEN** a governance policy is added, modified, or removed
- **THEN** no existing CSM element or relationship SHALL be altered as a
  direct result of that policy change

---

### Requirement: CSM Relationship to the Repository Evidence Model
The CSM SHALL depend on the Repository Evidence Model as its sole upstream
source of evidence and SHALL NOT depend on the Policy/Rule Model or the
Runtime Model for its own construction. A CSM SHALL be considered valid and
usable even when no policies or runtime data exist for the represented
system.

#### Scenario: CSM buildable without policy or runtime data
- **WHEN** a repository has evidence available but no governance policies
  or runtime telemetry configured
- **THEN** a CSM SHALL still be constructible and valid for that repository

---

### Requirement: CSM Relationship to the Runtime Model
The CSM SHALL relate to the Runtime Model only through a stable
runtime-identity reference on relevant CSM elements and through optional
enrichment of existing CSM relationships with observational confirmation.
The CSM SHALL NOT store raw runtime telemetry.

#### Scenario: Runtime confirmation enriches an existing relationship
- **WHEN** an observed static dependency between two Components is also
  confirmed by runtime call data
- **THEN** the CSM MAY record that the existing dependency relationship was
  confirmed at runtime, without storing the underlying trace or metric data

#### Scenario: Runtime data alone does not create CSM structure
- **WHEN** runtime telemetry reveals a call pattern with no corresponding
  static dependency in the CSM
- **THEN** the CSM SHALL NOT automatically create a new dependency
  relationship solely from runtime telemetry without a defined
  transformation producing it as knowledge with appropriate provenance

---

### Requirement: Language Independence
The CSM's structural vocabulary SHALL remain unchanged when a new
programming language's analyzer is introduced. Support for a new language
SHALL be achieved by introducing a new analyzer and a new evidence-to-CSM
mapping, and SHALL NOT require altering the CSM's entity kinds or
relationship types.

#### Scenario: New language analyzer introduced
- **WHEN** an analyzer for a previously unsupported programming language is
  added
- **THEN** the CSM's defined entity kinds and relationship types SHALL
  remain unchanged, and the new analyzer SHALL map its language's
  constructs onto the existing vocabulary

#### Scenario: Two languages produce comparable CSM elements
- **WHEN** two different languages both define an equivalent structural
  concept (e.g. an interface-like construct)
- **THEN** both SHALL be represented using the same CSM entity kind, such
  that a consumer of the CSM does not need to know which language produced
  either element

---

### Requirement: Multi-Repository Scope
The CSM SHALL be scoped to a single repository as its unit of identity,
construction, and versioning. Cross-repository relationships SHALL be
represented, from each repository's own CSM, as External System boundary
references at minimum. The CSM specification SHALL NOT require a
multi-repository "System" root to be valid.

#### Scenario: Single-repository CSM is independently valid
- **WHEN** a system is composed of multiple repositories
- **THEN** each repository SHALL have its own independently valid,
  independently versioned CSM

#### Scenario: Cross-repository dependency represented as external reference
- **WHEN** a Module in one repository depends on a service implemented in
  another repository
- **THEN** the depending repository's CSM SHALL represent the other
  repository's service as an External System boundary node at minimum

---

### Requirement: Incremental Model Updates
Every CSM element SHALL carry a stable identity that persists across
re-analysis runs as long as the underlying structure it represents has not
changed. The CSM construction process SHALL support scoping re-derivation
to the subset of the model affected by changed evidence, without requiring
full re-derivation of unaffected elements, and without invalidating
business context or other declared mappings that do not depend on the
changed evidence.

#### Scenario: Unchanged element retains identity across re-analysis
- **WHEN** a repository is re-analyzed and a given Module's underlying code
  has not changed
- **THEN** the CSM element representing that Module SHALL retain the same
  identity it had prior to re-analysis

#### Scenario: Change-scoped re-derivation
- **WHEN** only a subset of files in a repository have changed since the
  last analysis
- **THEN** the CSM construction process SHALL be able to update only the
  CSM elements affected by that subset, rather than reconstructing the
  entire CSM

#### Scenario: Business mapping unaffected by unrelated code change
- **WHEN** a code change affects a Module unrelated to a previously
  declared Business Capability mapping
- **THEN** the existing Business Capability mapping SHALL remain valid and
  SHALL NOT be invalidated by that code change

---

### Requirement: CSM Versioning and Evolution
The CSM specification (its vocabulary of entity kinds, relationship types,
and required attributes) SHALL be versioned. Additive changes (new entity
kind, new relationship type, new optional attribute) SHALL be considered
backward-compatible. Any change that alters the meaning or identity
semantics of existing entity kinds, relationship types, or required
attributes SHALL be considered a breaking change and SHALL require an
explicit change proposal. Each CSM snapshot for a repository SHALL be
individually identifiable (versioned or timestamped) rather than
overwritten in place.

#### Scenario: Additive vocabulary change does not break existing snapshots
- **WHEN** a new optional entity attribute is added to the CSM
  specification
- **THEN** existing CSM snapshots SHALL remain valid without requiring
  re-derivation

#### Scenario: Breaking change requires explicit proposal
- **WHEN** the meaning of an existing relationship type is changed
- **THEN** the change SHALL be introduced through an explicit change
  proposal rather than as an implicit modification

#### Scenario: Snapshots are individually identifiable
- **WHEN** a CSM is rebuilt for a repository
- **THEN** the resulting snapshot SHALL be distinguishable from prior
  snapshots for the same repository (e.g. by version or timestamp), rather
  than silently replacing prior state with no record of change

---

### Requirement: CSM Validation Expectations
A CSM, or any element within it, SHALL be considered invalid unless it
satisfies, at minimum: every element belongs to a defined entity kind or
relationship type from the core vocabulary (or a validly versioned
extension of it); every element carries a valid, structured provenance
record; every element classified as inferred carries a confidence level of
HIGH, MEDIUM, or LOW; every Architecture Component and Architectural
Boundary carries a provenance classification of declared or inferred
(never observed); every element derived from evidence retains a traceable
reference to that evidence; the absence of Method-level elements SHALL
NOT, by itself, be treated as invalid; and a subject's effective knowledge
being marked CONFLICTED SHALL NOT, by itself, be treated as invalid.

#### Scenario: Element with unknown entity kind is invalid
- **WHEN** a CSM element is classified using an entity kind not present in
  the core vocabulary or a validly versioned extension of it
- **THEN** the element SHALL be considered invalid CSM content

#### Scenario: Element missing required provenance is invalid
- **WHEN** a CSM element lacks a provenance record
- **THEN** the element SHALL be considered invalid CSM content

#### Scenario: Inferred element missing confidence is invalid
- **WHEN** a CSM element is classified as inferred but lacks a confidence
  level, or carries a value other than HIGH, MEDIUM, or LOW
- **THEN** the element SHALL be considered invalid CSM content

#### Scenario: Absence of Method-level elements does not invalidate the CSM
- **WHEN** a CSM contains only Type-level elements for a given language's
  evidence, with no Method-level elements present
- **THEN** the CSM SHALL NOT be considered invalid on that basis alone

#### Scenario: CONFLICTED effective knowledge does not invalidate the CSM
- **WHEN** a subject's effective knowledge is marked CONFLICTED because of
  unresolved same-category conflicting assertions
- **THEN** the CSM SHALL NOT be considered invalid on that basis alone,
  provided each conflicting assertion carries a valid provenance record

#### Scenario: Validation is performed before analysis
- **WHEN** analysis, rule evaluation, or AI reasoning is about to be
  performed against a CSM snapshot
- **THEN** the CSM snapshot SHALL be validated against these expectations
  before being used as an analysis input
