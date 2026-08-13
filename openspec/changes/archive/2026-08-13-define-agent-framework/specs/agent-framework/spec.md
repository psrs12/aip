## Purpose

The Agent Framework evaluates Findings using specialized, AI-assisted
Agents and produces Recommendations — durable, traceable,
consumer-facing guidance that references a Finding without mutating
it, with a deterministic artifact identity that does not, and cannot,
claim the generated guidance itself is reproducible.

The archived Canonical Software Model specification describes
Recommendations as "AI- or rule-generated guidance attached to a
Finding," part of its own Evidence-Knowledge-Analysis-Findings-
Recommendations-Remediation pipeline. `project.md` §6's AI Philosophy
separately names an `AI Reasoning -> Recommendations` pipeline stage.
This capability, together with Analysis Framework, Rule Framework, and
Finding Model, preserves that meaning while completing the refined
architecture those capabilities already established: Analysis
Framework performs policy-independent computation over a CSM Snapshot;
Rule Framework evaluates declared policy, producing
RuleEvaluationResults; Finding Model interprets those into Findings —
the first consumer-facing, deterministic artifact in the pipeline; the
Agent Framework, specified here, is the layer where AI reasoning is
finally introduced, producing Recommendations from Findings. This
capability specifies the generic Agent contract, consumption boundary,
and Recommendation artifact — not the Architecture Compliance Agent or
any other specific agent from `project.md` §5's catalogue, and not
Proposed Change, remediation execution, or any capability downstream
of Recommendation.

## ADDED Requirements

### Requirement: Agent Framework Specifies the Generic Framework Only
This specification SHALL define the generic Agent contract, Agent
registration, Finding consumption boundary, and Recommendation
artifact only. It SHALL NOT define, and this version SHALL NOT
require, the reasoning logic, prompt content, or behavior of any
specific agent (including the Architecture Compliance Agent or any
other agent named in `project.md` §5).

#### Scenario: No specific agent's reasoning logic is specified
- **WHEN** this specification is read for what constitutes a
  conforming Agent
- **THEN** it SHALL impose no requirement on how any specific agent
  constructs its reasoning, prompts, or output content — only on the
  contract shape every Agent SHALL satisfy

---

### Requirement: Agent Contract
An Agent SHALL declare a stable identifier and a monotonically
versioned identifier. An Agent SHALL consume exactly one Finding per
invocation, obtained through the Finding Source, and SHALL produce
exactly one Recommendation per invocation. An Agent SHALL declare no
input beyond Finding consumption in this version.

#### Scenario: Agent declares identifier and version
- **WHEN** an Agent is registered with the Agent Framework
- **THEN** it SHALL carry a stable identifier and a version

#### Scenario: Agent invocation consumes one Finding and produces one Recommendation
- **WHEN** an Agent is invoked
- **THEN** it SHALL consume exactly one Finding, obtained through the
  Finding Source, and SHALL produce exactly one Recommendation as the
  result of that invocation

---

### Requirement: Agent Consumption Is Limited to Findings Through the Finding Source
An Agent SHALL obtain its Finding exclusively through the Finding
Source. An Agent SHALL NOT read CSM content, Analysis Results, or
RuleEvaluationResults directly, and SHALL NOT depend on the module
that produced the Finding it consumes.

#### Scenario: Finding is obtained only through the Finding Source
- **WHEN** an Agent is invoked
- **THEN** it SHALL obtain its Finding exclusively through the Finding
  Source, never through a direct dependency on the module that
  produced it

#### Scenario: No direct access to CSM, Analysis Results, or RuleEvaluationResults
- **WHEN** an Agent is invoked
- **THEN** it SHALL complete that invocation without reading CSM
  content, Analysis Results, or RuleEvaluationResults directly — only
  the content already retained on the Finding itself, as exposed by
  the Finding Source

---

### Requirement: No Arbitrary Repository or Application Access
The Agent Framework SHALL NOT provide an Agent with access to
Repository Evidence, source repository content, or any other AIP
service or application state beyond what the Finding Source exposes.

#### Scenario: No Repository Evidence or arbitrary application access
- **WHEN** an Agent is invoked
- **THEN** the Agent Framework SHALL NOT provide it any means of
  reading Repository Evidence, source repository content, or any AIP
  service or application state other than the Finding obtained through
  the Finding Source

---

### Requirement: Finding Source Shape
A Finding Source SHALL expose, at minimum: a Finding retrievable by
its Evaluation Identity, and the set of Findings sharing a given
Logical Finding Identity or a given source CSM Snapshot identity. The
Agent Framework SHALL depend only on this shape to consume Findings,
independent of which module produced or how it persisted the
underlying Findings.

#### Scenario: Finding Source exposes retrieval by Evaluation Identity and by Logical Finding Identity or snapshot
- **WHEN** the Agent Framework reads a Finding Source
- **THEN** it SHALL be able to retrieve a specific Finding by its
  Evaluation Identity, and SHALL be able to retrieve every Finding
  sharing a named Logical Finding Identity or a named source CSM
  Snapshot identity

#### Scenario: Agent Framework is agnostic to Finding production
- **WHEN** two different Finding Sources expose equivalent Finding
  content, produced by different upstream mechanisms
- **THEN** the Agent Framework SHALL invoke Agents against both
  without requiring any change to its own behavior

---

### Requirement: No Intermediate AI Reasoning Artifact
The Agent Framework SHALL NOT define, persist, or expose a separate,
identified artifact representing an Agent's internal reasoning process
between Finding consumption and Recommendation production. An Agent's
internal reasoning is not a tracked artifact of this framework.

#### Scenario: No separate reasoning artifact is produced
- **WHEN** an Agent completes an invocation
- **THEN** the Agent Framework SHALL have produced exactly one
  Recommendation as the durable output — no separate, independently
  identified reasoning artifact SHALL exist

---

### Requirement: Internal Reasoning Content Is Not Exposed or Persisted
The Agent Framework SHALL NOT require an Agent's internal
reasoning content (including any model-produced chain-of-thought) to
be persisted, and SHALL NOT expose such content through any contract
this specification defines.

#### Scenario: Chain-of-thought is neither required nor exposed
- **WHEN** an underlying model produces internal reasoning content as
  part of generating a Recommendation
- **THEN** the Agent Framework SHALL NOT require that content to be
  stored, and SHALL NOT provide any operation that returns it

---

### Requirement: Recommendation Is a Distinct, Immutable Artifact From Finding
A Recommendation SHALL be represented as an artifact distinct from any
Finding it references. Producing a Recommendation SHALL NOT alter the
Finding it references, or any other CSM content, Analysis Result, or
RuleEvaluationResult. A Recommendation SHALL be immutable once
constructed and validated — the Agent Framework SHALL NOT provide an
operation that modifies a Recommendation's identity, referenced
Finding, or any other field after construction.

#### Scenario: Producing a Recommendation does not alter the referenced Finding
- **WHEN** the Agent Framework constructs a Recommendation
- **THEN** the Finding it references SHALL remain unchanged as a
  direct result of that construction

#### Scenario: No operation modifies an existing Recommendation
- **WHEN** a Recommendation has been constructed and validated
- **THEN** the Agent Framework SHALL offer no operation that alters
  any of its fields after construction

---

### Requirement: Recommendation References Its Source Finding by Both Identities
A Recommendation SHALL directly retain the referenced Finding's
Evaluation Identity and Logical Finding Identity. A consumer SHALL be
able to determine both directly from the Recommendation, without
resolving the referenced Finding first.

#### Scenario: Both Finding identities are directly retained
- **WHEN** a Recommendation is inspected
- **THEN** it SHALL be possible to determine the referenced Finding's
  Evaluation Identity and Logical Finding Identity directly from the
  Recommendation, without resolving that Finding first

---

### Requirement: Recommendation Multiplicity — One Invocation, One Recommendation
Every Agent invocation SHALL produce exactly one Recommendation. A
Finding MAY be referenced by any number of Recommendations, produced
by the same Agent invoked more than once or by different registered
Agents.

#### Scenario: One invocation produces exactly one Recommendation
- **WHEN** an Agent is invoked once
- **THEN** exactly one Recommendation SHALL be produced as a direct
  result

#### Scenario: A Finding may be referenced by multiple Recommendations
- **WHEN** the same Finding is referenced by Recommendations from more
  than one invocation, whether from the same Agent or different
  registered Agents
- **THEN** the Agent Framework SHALL retain each Recommendation
  independently, without limiting how many Recommendations may
  reference that Finding

---

### Requirement: No Conflict or Precedence Semantics Among Recommendations
The Agent Framework SHALL NOT rank, mark as primary, deduplicate, or
otherwise resolve conflict or precedence among multiple Recommendations
referencing the same Finding. Every Recommendation remains
independently valid and retrievable regardless of how many other
Recommendations reference the same Finding.

#### Scenario: No Recommendation is marked as primary or authoritative
- **WHEN** multiple Recommendations reference the same Finding
- **THEN** the Agent Framework SHALL NOT designate any one of them as
  primary, authoritative, or preferred over the others

#### Scenario: Disagreeing Recommendations are both retained
- **WHEN** two Recommendations referencing the same Finding contain
  differing or conflicting guidance
- **THEN** both SHALL remain independently retrievable, unmodified by
  the presence of the other

---

### Requirement: Recommendation Artifact Identity
A Recommendation's Artifact Identity SHALL be a deterministic function
of: the producing Agent's identifier, the Agent's version, the
referenced Finding's Evaluation Identity, and an opaque, invocation-
scoped Generation Identifier. Random or otherwise non-reproducible
Recommendation Artifact Identity generation SHALL NOT be used — the
identity computation itself, given the same recorded identity
components, SHALL always resolve to the same Recommendation Artifact
Identity.

#### Scenario: Same invocation identity components resolve to the same Recommendation Artifact Identity
- **WHEN** a Recommendation's identity is computed twice from the same
  recorded Agent identifier, Agent version, source Finding Evaluation
  Identity, and Generation Identifier
- **THEN** both computations SHALL produce the same Recommendation
  Artifact Identity

#### Scenario: A different Generation Identifier yields a distinguishable identity
- **WHEN** two Recommendations share the same Agent identifier, Agent
  version, and source Finding Evaluation Identity, but differ in their
  Generation Identifier
- **THEN** the two Recommendations' Artifact Identities SHALL be
  distinguishable from one another

---

### Requirement: Recommendation Artifact Identity Excludes Generated Content
Recommendation Artifact Identity computation SHALL NOT read, hash, or
otherwise derive from the Recommendation's generated content,
Confidence value, or Generation Provenance. Two Recommendations with
identical Artifact Identity components but differing generated content
SHALL NOT occur, because the Generation Identifier component is
assigned per invocation, not derived from content.

#### Scenario: Identity computation does not read generated content
- **WHEN** a Recommendation's Artifact Identity is computed
- **THEN** that computation SHALL NOT read the Recommendation's
  generated content, Confidence value, or Generation Provenance as an
  input

---

### Requirement: Generation Identifier Uniqueness and Non-Content-Derivation
Every Agent invocation SHALL be assigned a Generation Identifier that
is unique among that Agent's invocations and is not derived from the
generated content of the Recommendation it identifies. This
specification does not prescribe a specific generation mechanism for
the Generation Identifier (e.g. a random token or a monotonic
sequence) beyond this uniqueness and non-content-derivation
requirement.

#### Scenario: Generation Identifier is unique per invocation
- **WHEN** the same Agent is invoked more than once against the same
  Finding
- **THEN** each invocation SHALL be assigned a distinct Generation
  Identifier

#### Scenario: Generation Identifier is not derived from generated content
- **WHEN** a Generation Identifier is assigned to an invocation
- **THEN** that assignment SHALL NOT depend on, or be computed from,
  the content the invocation subsequently generates

---

### Requirement: Deterministic Recommendation Lookup, Not Deterministic Generation
The Agent Framework's determinism guarantee SHALL apply to
Recommendation Artifact Identity, lookup, and Generation Provenance
only. It SHALL NOT claim, and no requirement in this specification
SHALL be interpreted as claiming, that invoking the same Agent version
against the same Finding produces identical, or even similar,
Recommendation content across separate invocations.

#### Scenario: Same invocation identity resolves to the same artifact, not necessarily the same content across separate invocations
- **WHEN** the same Agent identifier, Agent version, and source Finding
  Evaluation Identity are used across two separate invocations, each
  with its own Generation Identifier
- **THEN** each invocation SHALL produce its own independently
  identified Recommendation, and this specification SHALL NOT require
  their generated content to be identical or similar

#### Scenario: Repeated retrieval of the same Recommendation is identical
- **WHEN** the same Recommendation Artifact Identity is retrieved twice
- **THEN** both retrievals SHALL return the same Recommendation,
  including the same generated content recorded at construction —
  determinism applies to retrieval of an already-constructed artifact,
  not to generation itself

---

### Requirement: Recommendation Content Is Agent-Defined and Opaque to the Framework
The Agent Framework SHALL treat a Recommendation's generated guidance
content as an opaque payload whose internal shape is defined by its
producing Agent. The Agent Framework SHALL NOT constrain, interpret,
or validate that payload's internal structure beyond the traceability,
referential-integrity, and required-field expectations defined
elsewhere in this specification.

#### Scenario: Differently-shaped guidance content from different Agents is accepted
- **WHEN** two different Agents produce Recommendations with unrelated
  internal content shapes
- **THEN** the Agent Framework SHALL store and retrieve both without
  rejecting either on the basis of content shape

---

### Requirement: Recommendation Confidence Is Agent-Produced and Mandatory
Every Recommendation SHALL carry a Confidence value, declared by its
producing Agent at generation time. The Agent Framework SHALL NOT
compute, derive, or override a Recommendation's Confidence value.

#### Scenario: Confidence is present on every Recommendation
- **WHEN** a Recommendation is constructed
- **THEN** it SHALL carry a Confidence value declared by the producing
  Agent

#### Scenario: Framework does not compute or override Confidence
- **WHEN** the Agent Framework constructs, validates, or publishes a
  Recommendation
- **THEN** it SHALL NOT alter the Confidence value the producing Agent
  declared

---

### Requirement: Recommendation Confidence Is Distinct From Finding Confidence
Recommendation Confidence SHALL be treated as a distinct concept from
Finding Confidence. A Recommendation's Confidence value SHALL NOT be
copied from, constrained by, or required to match its referenced
Finding's Confidence value.

#### Scenario: Recommendation Confidence is independent of Finding Confidence
- **WHEN** a Recommendation is constructed from a Finding
- **THEN** the Recommendation's Confidence value SHALL be the value
  declared by the producing Agent, independent of the Finding's own
  fixed Confidence value

---

### Requirement: Generation Provenance
Every Recommendation SHALL retain a Generation Provenance record
containing, at minimum: the producing model or provider identifier and
version, the generation configuration used (as opaque, Agent-defined
values not interpreted by the framework), and a generation timestamp.
Generation Provenance SHALL NOT be part of Recommendation Artifact
Identity computation.

#### Scenario: Generation Provenance is present on every Recommendation
- **WHEN** a Recommendation is constructed
- **THEN** it SHALL carry a Generation Provenance record with a
  model/provider identifier and version, generation configuration, and
  a generation timestamp

#### Scenario: Generation Provenance remains fully inspectable independent of identity computation
- **WHEN** a Recommendation's Generation Provenance is inspected
- **THEN** its model/provider identifier and version, generation
  configuration, and generation timestamp SHALL each be readable
  directly from the Recommendation, without needing to recompute or
  otherwise interact with its Artifact Identity

---

### Requirement: AI-Specific Content Isolation From Deterministic Artifacts
Prompt content or template identifiers, model/provider details, and
generation configuration SHALL NOT be represented in, or leak into,
any CSM element, Analysis Result, RuleEvaluationResult, or Finding.
Such content SHALL be confined to a Recommendation's own Generation
Provenance.

#### Scenario: No AI-specific content appears in a deterministic artifact
- **WHEN** a CSM element, Analysis Result, RuleEvaluationResult, or
  Finding is inspected
- **THEN** it SHALL NOT contain prompt content, model/provider
  details, or generation configuration of any kind

#### Scenario: AI-specific content is confined to Generation Provenance
- **WHEN** an Agent's prompt or model configuration needs to be
  recorded for explainability
- **THEN** it SHALL be recorded only within the corresponding
  Recommendation's Generation Provenance

---

### Requirement: Recommendation Validation Before Publication
The Agent Framework SHALL validate a constructed Recommendation before
it is considered usable output, checking at minimum: the referenced
Finding (by Evaluation Identity) SHALL exist and SHALL already be
published; the declared producing Agent and its version SHALL
correspond to a currently registered Agent; the Generation Provenance
record SHALL be present and SHALL carry a non-empty model/provider
identifier and a generation timestamp; Confidence and the
Recommendation's content field SHALL be present; and the Recommendation
SHALL carry no field representing a claimed mutation of CSM content,
an Analysis Result, a RuleEvaluationResult, or the referenced Finding.
A Recommendation that fails this validation SHALL NOT be published as
usable output.

#### Scenario: Valid Recommendation is published as usable output
- **WHEN** a constructed Recommendation satisfies every validation
  check
- **THEN** it SHALL be published as usable output

#### Scenario: Recommendation referencing an unpublished Finding is rejected
- **WHEN** a constructed Recommendation references a Finding identity
  that does not exist or has not been published
- **THEN** it SHALL fail validation and SHALL NOT be published

#### Scenario: Recommendation from an unregistered Agent/version is rejected
- **WHEN** a constructed Recommendation declares a producing Agent
  identifier and version that does not correspond to a currently
  registered Agent
- **THEN** it SHALL fail validation and SHALL NOT be published

#### Scenario: Recommendation with incomplete Generation Provenance is rejected
- **WHEN** a constructed Recommendation's Generation Provenance is
  missing, or lacks a model/provider identifier or a generation
  timestamp
- **THEN** it SHALL fail validation and SHALL NOT be published

#### Scenario: Recommendation missing Confidence or content is rejected
- **WHEN** a constructed Recommendation lacks a Confidence value or its
  content field
- **THEN** it SHALL fail validation and SHALL NOT be published

---

### Requirement: Validation Does Not Determine Substantive Correctness
Recommendation validation SHALL be limited to structural integrity,
referential integrity, Agent/version consistency, Generation
Provenance completeness, required-field presence, and isolation from
deterministic facts. Recommendation validation SHALL NOT determine, or
claim to determine, whether a Recommendation's guidance is
substantively correct, useful, or of sufficient quality.

#### Scenario: A well-formed but substantively questionable Recommendation still passes validation
- **WHEN** a constructed Recommendation satisfies every structural,
  referential, and provenance check this specification defines
- **THEN** it SHALL pass validation regardless of whether its guidance
  content is substantively correct, useful, or high quality

#### Scenario: Validation does not gate on Confidence value
- **WHEN** a constructed Recommendation carries a low Confidence value
- **THEN** validation SHALL NOT reject it on that basis alone

---

### Requirement: Recommendation Publishing
A Recommendation Publisher SHALL call the Recommendation persistence
mechanism's write operation only for a Recommendation that has passed
validation. A Recommendation that fails validation SHALL NOT reach the
persistence mechanism through this operation.

#### Scenario: Only validated Recommendations are written to the persistence mechanism
- **WHEN** the Agent Framework publishes a Recommendation
- **THEN** it SHALL have already passed every validation check defined
  in Recommendation Validation Before Publication

---

### Requirement: Recommendations Are Durable, Individually Identifiable Artifacts
The Agent Framework SHALL treat each Recommendation as an individually
identifiable artifact, retrievable by its Artifact Identity independent
of the process that produced it. The specific persistence mechanism is
not constrained by this requirement, and its concrete persistence
technology is not shared as a general-purpose contract available to
other capabilities in this version.

#### Scenario: Recommendation remains retrievable after the producing invocation ends
- **WHEN** an Agent invocation that produced a Recommendation has
  completed and its process has exited
- **THEN** that Recommendation SHALL remain retrievable by its
  Artifact Identity

#### Scenario: A new Recommendation does not overwrite a prior, differently-identified one
- **WHEN** a new invocation produces a Recommendation with an Artifact
  Identity distinct from a prior invocation's Recommendation
- **THEN** the prior Recommendation SHALL remain retrievable and
  unaffected

---

### Requirement: Agent Registration and Extension Behavior
The Agent Framework SHALL provide an Agent registration mechanism.
Registering a new Agent SHALL require
no change to the Recommendation construction, identity, traceability,
or validation mechanisms this specification defines, and SHALL require
no change to any deterministic capability (CSM, Analysis Framework,
Rule Framework, or Finding Model).

#### Scenario: A new Agent is registered without core mechanism changes
- **WHEN** a new Agent is registered with the Agent Framework
- **THEN** the Recommendation identity, traceability, and validation
  mechanisms SHALL remain unchanged, and the new Agent SHALL be
  evaluable using them as they already exist

#### Scenario: Registering a new Agent requires no change to deterministic capabilities
- **WHEN** a new Agent is registered
- **THEN** no change SHALL be required to the CSM, Analysis Framework,
  Rule Framework, or Finding Model specifications or their
  implementations

---

### Requirement: Output Boundary Excludes Remediation and Beyond
The Agent Framework's output SHALL be limited to Recommendation. This
specification SHALL NOT define a Proposed Change artifact, code
generation, remediation execution, deployment, remediation
verification, or human-approval workflow.

#### Scenario: No Proposed Change or remediation artifact is produced
- **WHEN** the Agent Framework completes an invocation
- **THEN** its only durable output SHALL be a Recommendation — no
  Proposed Change, code change, or remediation artifact SHALL be
  produced

---

### Requirement: Deterministic-Layer Protection
The Agent Framework SHALL NOT modify CSM content, Analysis Results,
RuleEvaluationResults, Findings, or Rule decisions. Producing,
validating, or publishing a Recommendation SHALL NOT alter any of
these as a side effect.

#### Scenario: No deterministic artifact is altered by Recommendation production
- **WHEN** the Agent Framework produces, validates, or publishes a
  Recommendation
- **THEN** CSM content, Analysis Results, RuleEvaluationResults,
  Findings, and Rule decisions SHALL each remain unchanged as a direct
  result

---

### Requirement: Agent Invocation Independence
An Agent invocation SHALL NOT depend on any other Agent invocation
having occurred, on its result, or on invocation order. The Agent
Framework SHALL NOT provide a mechanism for declaring an ordering or
dependency between Agent invocations, and its contract permits
concurrent or reordered invocation without affecting correctness.

#### Scenario: Recommendation is unaffected by which other invocations occurred
- **WHEN** an Agent is invoked
- **THEN** its resulting Recommendation SHALL be unaffected by which
  other Agent invocations occurred, in what order, or whether they
  occurred concurrently

#### Scenario: No declared dependency between Agent invocations is honored
- **WHEN** the Agent Framework processes a set of Agent invocations
- **THEN** it SHALL NOT accept, require, or act on a declared
  execution-order or output dependency between any two of them

---

### Requirement: Failure Semantics
The Agent Framework SHALL reject Recommendation construction or
publication, without producing a usable Recommendation, when: the
referenced Finding does not exist or is invalid; the declared Agent or
version is not registered; Generation Provenance is incomplete; or the
Recommendation payload is missing a required field. A failure at
generation time (e.g. the underlying model call fails or times out)
SHALL result in no Recommendation being produced for that invocation,
rather than a partially-constructed or invalid Recommendation being
published. This specification does not define retry or orchestration
infrastructure for generation failures.

#### Scenario: Missing or invalid Finding reference prevents publication
- **WHEN** a Recommendation is constructed referencing a Finding that
  does not exist, or an invalid Finding reference
- **THEN** it SHALL fail validation and SHALL NOT be published

#### Scenario: Generation failure produces no Recommendation
- **WHEN** an Agent's underlying generation step fails or does not
  complete
- **THEN** the Agent Framework SHALL NOT publish a Recommendation for
  that invocation

---

### Requirement: Single-Repository Finding Consumption
Since Finding Model, Rule Framework, and the Canonical Software Model
beneath them are each scoped to a single repository, every Finding an
Agent consumes SHALL concern exactly one repository's CSM Snapshot,
and every Recommendation an Agent produces SHALL therefore concern
exactly one repository through its referenced Finding.

#### Scenario: A Recommendation concerns exactly one repository through its referenced Finding
- **WHEN** a Recommendation is inspected
- **THEN** it SHALL concern exactly one repository's CSM Snapshot,
  inherited from its referenced Finding
