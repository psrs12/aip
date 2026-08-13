## Purpose

The Finding Model interprets Rule Framework's RuleEvaluationResults
into Findings — durable, traceable, consumer-facing artifacts carrying
the structured shape a developer, architect, or CI pipeline consumes,
with an identity model that supports recognizing the same logical
issue across CSM Snapshots without weakening this system's established
deterministic-identity and immutable-artifact discipline.

The archived Canonical Software Model specification describes its
Evidence-Knowledge-Analysis-Findings pipeline at the conceptual level:
a policy is evaluated against CSM knowledge and produces a Finding.
This capability, together with Analysis Framework and Rule Framework,
preserves that meaning at the pipeline level while refining its
architecture into explicit layers: Analysis Framework performs
policy-independent computation over a CSM Snapshot, producing Analysis
Results; Rule Framework evaluates declared Rules against Analysis View
and Analysis Result content, producing RuleEvaluationResults for every
outcome; the Finding Model, specified here, is where a
RuleEvaluationResult becomes the "structured output of Analysis" the
archived specification's own Finding concept describes. Every Finding
this specification defines therefore traces to at least one
RuleEvaluationResult and never itself becomes a Recommendation or a
Remediation.

## Requirements

### Requirement: Deterministic, Declarative Finding Construction Only
The Finding Model SHALL construct every Finding by deterministic
interpretation of its referenced RuleEvaluationResult(s) only. It
SHALL NOT use AI/LLM reasoning, heuristic scoring, or probabilistic
inference to construct a Finding or compute its identity in this
version. A Finding's identity SHALL NOT be generated randomly or by
any other non-reproducible means.

#### Scenario: No AI or heuristic judgment used to construct a Finding
- **WHEN** the Finding Model constructs a Finding from one or more
  RuleEvaluationResults
- **THEN** the resulting Finding, and both of its identities, SHALL be
  derivable by deterministic computation alone, with no AI/LLM call or
  heuristic scoring step involved

#### Scenario: Repeated construction over unchanged input is identical
- **WHEN** the Finding Model constructs a Finding twice from the same
  set of RuleEvaluationResult identities
- **THEN** both constructions SHALL produce a Finding with identical
  Evaluation Identity and identical Logical Finding Identity

---

### Requirement: Finding Provenance Requires At Least One RuleEvaluationResult
A Finding SHALL reference a non-empty set of RuleEvaluationResult
identities. A Finding SHALL NOT exist independently of at least one
RuleEvaluationResult in this version. The Finding Model SHALL NOT
provide a mechanism for AI-generated or manually-authored Findings in
this version; such a mechanism, if introduced, is a separate future
capability and SHALL NOT be represented as an extension of this
requirement.

#### Scenario: Finding construction requires at least one RuleEvaluationResult
- **WHEN** the Finding Model constructs a Finding
- **THEN** that Finding SHALL reference at least one RuleEvaluationResult
  identity, and construction SHALL fail if no RuleEvaluationResult is
  supplied

#### Scenario: No mechanism exists for provenance-independent Findings
- **WHEN** the Finding Model is used as specified in this version
- **THEN** it SHALL offer no operation that produces a Finding without
  at least one referenced RuleEvaluationResult

---

### Requirement: Finding Is a Distinct Artifact From RuleEvaluationResult
A Finding SHALL be represented as an artifact distinct from any
RuleEvaluationResult it references. A RuleEvaluationResult SHALL NOT
be treated as a Finding, relabeled as a Finding, or exposed to a
consumer as if it were a Finding, without passing through the Finding
Model's own construction, identity computation, and validation.

#### Scenario: A RuleEvaluationResult is not itself a Finding
- **WHEN** a RuleEvaluationResult is produced by Rule Framework
- **THEN** it SHALL NOT be treated as a Finding until the Finding Model
  constructs a Finding that references it

---

### Requirement: Finding-to-RuleEvaluationResult Reference Set and V1 Multiplicity
A Finding's RuleEvaluationResult reference SHALL be a non-empty set,
capable of holding more than one RuleEvaluationResult identity. In
this version, the Finding Model's construction behavior SHALL populate
that set with exactly one RuleEvaluationResult identity per Finding;
this version SHALL NOT perform aggregation of multiple
RuleEvaluationResults into a single Finding (see Aggregation Is Not
Performed in This Version).

#### Scenario: Reference set shape supports multiple RuleEvaluationResults
- **WHEN** a Finding's RuleEvaluationResult reference set is inspected
- **THEN** its shape SHALL be a set capable of holding more than one
  RuleEvaluationResult identity, independent of how many it currently
  holds

#### Scenario: V1 construction produces exactly one reference per Finding
- **WHEN** the Finding Model constructs a Finding in this version
- **THEN** the resulting Finding's RuleEvaluationResult reference set
  SHALL contain exactly one RuleEvaluationResult identity

---

### Requirement: Evaluation Identity
A Finding's Evaluation Identity SHALL be a deterministic function of
the complete set of its referenced RuleEvaluationResult identities.
Two Findings constructed from the same complete RuleEvaluationResult
identity set SHALL have the same Evaluation Identity. Two Findings
constructed from different RuleEvaluationResult identity sets SHALL
have different Evaluation Identities. Evaluation Identity is
snapshot-bound: because each referenced RuleEvaluationResult identity
already encodes a source CSM Snapshot identity, Evaluation Identity
changes whenever the underlying evaluation run changes, even if the
concerned CSM element is unchanged.

#### Scenario: Identical RuleEvaluationResult sets produce identical Evaluation Identity
- **WHEN** two Findings are each constructed from the same complete set
  of RuleEvaluationResult identities
- **THEN** both Findings SHALL have the same Evaluation Identity

#### Scenario: Different RuleEvaluationResult sets produce different Evaluation Identity
- **WHEN** two Findings are constructed from different
  RuleEvaluationResult identity sets
- **THEN** the two Findings' Evaluation Identities SHALL be
  distinguishable from one another

---

### Requirement: Logical Finding Identity
A Finding SHALL also carry a Logical Finding Identity: a deterministic
function of the producing Rule's identifier (excluding its version)
and the CSM element identity the underlying Rule Scope instance's
containment-level anchor concerns. Logical Finding Identity SHALL NOT
include source CSM Snapshot identity, RuleEvaluationResult identity,
Rule version, or any consumed Analysis Result identity. For an
unanchored Rule Scope instance, the concerned CSM element identity
SHALL be the Repository CSM element's own identity. A Rule Scope
instance's containment-level anchor is always a CSM element identity;
Logical Finding Identity SHALL NOT be computed from a relationship
identity, even when the Rule Type's declared Rule Scope includes
relationship types among the content it reads within that anchor's
invocation.

#### Scenario: Logical Finding Identity excludes source CSM Snapshot identity
- **WHEN** the same Rule is evaluated against the same concerned CSM
  element across two different CSM Snapshots, in each case producing a
  FAIL RuleEvaluationResult
- **THEN** the two resulting Findings SHALL have the same Logical
  Finding Identity, regardless of the two CSM Snapshots' differing
  identities

#### Scenario: Logical Finding Identity excludes Rule version
- **WHEN** the same Rule, at two different versions, is evaluated
  against the same concerned CSM element, in each case producing a
  FAIL RuleEvaluationResult
- **THEN** the two resulting Findings SHALL have the same Logical
  Finding Identity

#### Scenario: Different concerned CSM elements yield different Logical Finding Identity
- **WHEN** the same Rule is evaluated against two different concerned
  CSM elements
- **THEN** the two resulting Findings' Logical Finding Identities SHALL
  be distinguishable from one another

#### Scenario: Unanchored Rule Scope concerns the Repository element
- **WHEN** a Finding is constructed from a RuleEvaluationResult whose
  Rule Scope instance is unanchored
- **THEN** that Finding's Logical Finding Identity SHALL be computed
  using the Repository CSM element's own identity as the concerned
  element

#### Scenario: A relationship-reading Rule Type resolves to its anchor element, not a relationship identity
- **WHEN** a Rule Type's declared Rule Scope includes a CSM
  relationship type, and its RuleEvaluationResult's outcome concerns a
  specific relationship read within one anchored invocation
- **THEN** the resulting Finding's Logical Finding Identity SHALL be
  computed from that invocation's containment-level anchor element
  identity, not from the identity of the specific relationship

---

### Requirement: Evaluation Identity and Logical Finding Identity Are Distinct
Evaluation Identity and Logical Finding Identity SHALL be computed
independently, SHALL be retained on a Finding as two separate values,
and SHALL NOT be conflated, merged into a single value, or treated as
interchangeable by any Finding Model operation.

#### Scenario: Both identities are independently present on a Finding
- **WHEN** a Finding is inspected
- **THEN** it SHALL be possible to read its Evaluation Identity and its
  Logical Finding Identity as two distinct values, neither derivable
  from the other alone

#### Scenario: Same Logical Finding Identity, different Evaluation Identity, across two runs
- **WHEN** the Finding Model constructs a Finding for the same
  concerned CSM element and Rule in two different evaluation runs
- **THEN** the two Findings SHALL share the same Logical Finding
  Identity while having different Evaluation Identities

---

### Requirement: Cross-Snapshot Comparison via Logical Finding Identity
A consumer SHALL be able to determine whether a given Logical Finding
Identity is present among the Findings of one evaluation run and
absent among the Findings of a different evaluation run, using
Logical Finding Identity alone, without needing to compare Evaluation
Identities or resolve either run's RuleEvaluationResults or CSM
Snapshot content. The Finding Model SHALL NOT itself interpret this
comparison as a lifecycle state transition; it SHALL only ensure the
comparison is possible.

#### Scenario: Logical Finding Identity presence is comparable across two runs
- **WHEN** a consumer holds the set of Logical Finding Identities
  produced by two different evaluation runs against the same
  repository
- **THEN** the consumer SHALL be able to determine, using Logical
  Finding Identity values alone, which identities are present in both
  runs, present in only one, or present in only the other

#### Scenario: Finding Model does not itself label a comparison as resolved or new
- **WHEN** the Finding Model constructs Findings for an evaluation run
- **THEN** it SHALL NOT attach an open, resolved, new, or any other
  lifecycle-state label to a Finding as part of construction

---

### Requirement: Finding Immutability
A Finding SHALL be immutable once constructed and validated. The
Finding Model SHALL NOT provide an operation that modifies a Finding's
identity, referenced RuleEvaluationResult set, or any other field
after construction. A Finding that would represent a changed
evaluation outcome SHALL be represented as a new, separately
identified Finding, never as a modification of a prior one.

#### Scenario: No operation modifies an existing Finding
- **WHEN** a Finding has been constructed and validated
- **THEN** the Finding Model SHALL offer no operation that alters any
  of its fields, its Evaluation Identity, or its Logical Finding
  Identity

#### Scenario: A changed outcome produces a new Finding, not a mutation
- **WHEN** re-evaluation produces a RuleEvaluationResult with a
  different outcome for the same concerned CSM element and Rule
- **THEN** the Finding Model SHALL construct a new Finding with a new
  Evaluation Identity, sharing the prior Finding's Logical Finding
  Identity, rather than modifying the prior Finding

---

### Requirement: No Mutable Lifecycle State or Lifecycle Event Artifact in This Version
The Finding Model SHALL NOT introduce a mutable lifecycle-state field
on Finding (such as open, resolved, or suppressed) in this version. It
SHALL NOT introduce a separate lifecycle-event artifact (such as a
transition or status-change record) in this version.

#### Scenario: Finding carries no mutable lifecycle-state field
- **WHEN** a Finding's shape is inspected
- **THEN** it SHALL NOT contain a field whose value is mutated in place
  to reflect a lifecycle-state change

#### Scenario: No lifecycle-event artifact is produced
- **WHEN** the Finding Model completes an evaluation run
- **THEN** it SHALL NOT produce any artifact representing a lifecycle
  transition or status change, distinct from the Findings themselves

---

### Requirement: Finding Lifecycle Presentation Is Out of Scope
Any mechanism that presents, queries, aggregates, or otherwise
surfaces derived lifecycle information (such as an open/resolved
dashboard or a suppression workflow) to a consumer SHALL NOT be part
of this version of the Finding Model. This version's obligation is
limited to ensuring Logical Finding Identity and Evaluation Identity
are each correctly computed and retained (see Cross-Snapshot
Comparison via Logical Finding Identity).

#### Scenario: No lifecycle presentation mechanism is provided
- **WHEN** the Finding Model is used as specified in this version
- **THEN** it SHALL offer no query, view, or workflow that presents
  lifecycle state to a consumer beyond exposing Logical Finding
  Identity and Evaluation Identity on each Finding

---

### Requirement: Finding Shape
A Finding SHALL carry, at minimum: a Finding ID (its Evaluation
Identity), its Logical Finding Identity, the producing Rule's
identifier, a Category, a Severity, a fixed Confidence value, a
Location (the concerned CSM element identity), an
Evidence reference (see Finding Evidence Means Traceability Content,
Not Repository Evidence), and a Description and Impact. A Finding
SHALL NOT carry a Recommendation field or a Remediation-availability
field populated with content in this version.

#### Scenario: A constructed Finding carries the complete v1 field set
- **WHEN** the Finding Model constructs a Finding
- **THEN** the resulting Finding SHALL carry a Finding ID, Logical
  Finding Identity, Rule identifier, Category, Severity, Confidence,
  Location, Evidence reference, Description, and Impact

#### Scenario: Confidence is fixed for every v1 Finding
- **WHEN** any Finding is constructed in this version
- **THEN** its Confidence value SHALL equal the single, fixed, maximal
  value this version defines, regardless of which Rule or CSM content
  it concerns

#### Scenario: Recommendation and remediation availability are not populated
- **WHEN** a Finding is constructed in this version
- **THEN** it SHALL NOT carry populated Recommendation content or
  populated Remediation-availability content

---

### Requirement: Severity and Category Are Rule-Type-Declared Configuration
A Finding's Severity and Category SHALL be taken directly from static
configuration declared by the producing Rule Type, unchanged by the
Finding Model. The Finding Model SHALL NOT compute, infer, or
otherwise derive a Severity or Category value from Finding content,
aggregation, or any heuristic.

#### Scenario: Severity and Category are copied from Rule Type configuration
- **WHEN** the Finding Model constructs a Finding from a
  RuleEvaluationResult
- **THEN** the Finding's Severity and Category SHALL equal the values
  statically declared by that RuleEvaluationResult's producing Rule
  Type, unmodified

#### Scenario: Finding Model does not compute Severity from context
- **WHEN** the Finding Model constructs a Finding
- **THEN** it SHALL NOT vary that Finding's Severity based on the
  number of referenced RuleEvaluationResults, aggregated content, or
  any other run-time computation

---

### Requirement: Finding Evidence Means Traceability Content, Not Repository Evidence
A Finding's Evidence reference SHALL consist of the CSM element and
relationship identities, and their provenance references, that the
Finding concerns — reachable through the CSM's own Structured
Provenance Record mechanism. The Finding Model SHALL NOT read
Repository Evidence directly, and a Finding's Evidence reference
SHALL NOT contain raw Repository Evidence content.

#### Scenario: Finding Evidence references CSM provenance, not raw Repository Evidence
- **WHEN** a Finding's Evidence reference is inspected
- **THEN** it SHALL consist of CSM element/relationship identities and
  provenance references, and SHALL NOT contain raw Repository Evidence
  content

#### Scenario: Finding Model does not read Repository Evidence
- **WHEN** the Finding Model constructs a Finding
- **THEN** it SHALL do so without reading Repository Evidence directly

---

### Requirement: Finding Traceability
Every Finding SHALL directly retain: the source CSM Snapshot identity
its referenced RuleEvaluationResult(s) were evaluated against, the
concerned CSM element identity, the producing Rule's identifier, and
the complete set of referenced RuleEvaluationResult identities. A
consumer SHALL be able to determine each of these directly from the
Finding, without resolving any referenced RuleEvaluationResult first.

#### Scenario: Source CSM Snapshot identity is directly retained
- **WHEN** a Finding is inspected
- **THEN** it SHALL be possible to determine the source CSM Snapshot
  identity it concerns without resolving any referenced
  RuleEvaluationResult

#### Scenario: Concerned CSM element identity is directly retained
- **WHEN** a Finding is inspected
- **THEN** it SHALL be possible to determine the CSM element identity
  it concerns directly from the Finding

#### Scenario: Producing Rule and consumed RuleEvaluationResults are traceable
- **WHEN** a Finding is inspected
- **THEN** it SHALL be possible to determine the producing Rule's
  identifier and the complete set of RuleEvaluationResult identities it
  references

---

### Requirement: Aggregation Is Not Performed in This Version
The Finding Model SHALL NOT group multiple RuleEvaluationResults into
a single Finding in this version. Any future aggregation or
deduplication behavior is a distinct, separately specified capability
and SHALL NOT be introduced as an undocumented side effect of Finding
construction in this version.

#### Scenario: No grouping of multiple RuleEvaluationResults occurs
- **WHEN** the Finding Model processes a set of RuleEvaluationResults
  produced by one evaluation run
- **THEN** it SHALL construct one Finding per RuleEvaluationResult that
  qualifies for Finding construction, never combining more than one
  RuleEvaluationResult into a single Finding

---

### Requirement: Finding Model Has No Scope Concept
The Finding Model SHALL NOT declare or require an Analysis-Scope- or
Rule-Scope-shaped applicability declaration. A Finding's targeting is
fully determined by its referenced RuleEvaluationResult(s) and their
already-established Rule Scope instances; the Finding Model performs
no independent applicability computation against CSM content.

#### Scenario: Finding Model requires no Scope declaration to operate
- **WHEN** the Finding Model is configured or invoked
- **THEN** it SHALL require no Scope declaration of any kind as an
  input

#### Scenario: Finding targeting is determined by RuleEvaluationResult content alone
- **WHEN** the Finding Model determines which CSM element or
  relationship a Finding concerns
- **THEN** it SHALL do so using only the concerned CSM element or
  relationship identity already present in the referenced
  RuleEvaluationResult's Rule Scope instance, without evaluating any
  applicability condition of its own

---

### Requirement: Finding Validation Before Publication
The Finding Model SHALL validate a constructed Finding before it is
considered usable output, checking at minimum: every referenced
RuleEvaluationResult identity SHALL exist and SHALL already be
published; the Finding's Evaluation Identity and Logical Finding
Identity SHALL each be independently recomputable from, and consistent
with, its referenced RuleEvaluationResult set; and the Finding's
RuleEvaluationResult reference set SHALL be non-empty. Validation
SHALL NOT re-verify the referential integrity of a referenced
RuleEvaluationResult's own consumed Analysis Results or CSM content —
it SHALL trust that RuleEvaluationResultValidator already established
that. A Finding that fails validation SHALL NOT be published as usable
output.

#### Scenario: Valid Finding is published as usable output
- **WHEN** a constructed Finding satisfies every validation check
- **THEN** it SHALL be published as usable output

#### Scenario: Finding referencing an unpublished RuleEvaluationResult is rejected
- **WHEN** a constructed Finding references a RuleEvaluationResult
  identity that does not exist or has not been published
- **THEN** it SHALL fail validation and SHALL NOT be published

#### Scenario: Finding with an empty RuleEvaluationResult reference set is rejected
- **WHEN** a constructed Finding's RuleEvaluationResult reference set
  is empty
- **THEN** it SHALL fail validation and SHALL NOT be published

#### Scenario: Finding with inconsistent identity is rejected
- **WHEN** a constructed Finding's Evaluation Identity or Logical
  Finding Identity cannot be recomputed to match its referenced
  RuleEvaluationResult set
- **THEN** it SHALL fail validation and SHALL NOT be published

#### Scenario: Validation does not re-verify the upstream Analysis-to-Rule chain
- **WHEN** the Finding Model validates a constructed Finding
- **THEN** it SHALL NOT re-check the referential integrity of any
  referenced RuleEvaluationResult's own consumed Analysis Result
  identities or CSM element/Subject identities

---

### Requirement: Findings Are Durable, Individually Identifiable Artifacts
The Finding Model SHALL treat each Finding as an individually
identifiable artifact of an evaluation run, retrievable by its
Evaluation Identity independent of the process that produced it,
analogous to how a RuleEvaluationResult is individually identifiable
and retrievable. The specific persistence mechanism is not constrained
by this requirement.

#### Scenario: Finding remains retrievable after the producing run ends
- **WHEN** an evaluation run that produced a Finding has completed and
  its process has exited
- **THEN** that Finding SHALL remain retrievable by its Evaluation
  Identity

#### Scenario: A new Finding does not overwrite a prior, differently-identified one
- **WHEN** a new evaluation run produces a Finding with an Evaluation
  Identity distinct from a prior run's Finding
- **THEN** the prior Finding SHALL remain retrievable and unaffected

---

### Requirement: Rule Evaluation Result Source Shape
A Rule Evaluation Result Source SHALL expose, at minimum: a
RuleEvaluationResult retrievable by its own identity, and the set of
RuleEvaluationResults produced by a given Rule identifier against a
given source CSM Snapshot identity. The Finding Model SHALL depend
only on this shape to consume RuleEvaluationResults, independent of
which module produced or how it persisted the underlying
RuleEvaluationResults.

#### Scenario: Rule Evaluation Result Source exposes retrieval by identity and by Rule/snapshot
- **WHEN** the Finding Model reads a Rule Evaluation Result Source
- **THEN** it SHALL be able to retrieve a specific RuleEvaluationResult
  by its identity, and SHALL be able to retrieve every
  RuleEvaluationResult a named Rule produced against a named CSM
  Snapshot identity

#### Scenario: Finding Model is agnostic to RuleEvaluationResult production
- **WHEN** two different Rule Evaluation Result Sources expose
  equivalent RuleEvaluationResult content, produced by different
  upstream mechanisms
- **THEN** the Finding Model SHALL construct Findings from both without
  requiring any change to its own behavior

---

### Requirement: Rule Evaluation Result Consumption Through the Rule Evaluation Result Source Only
The Finding Model SHALL consume RuleEvaluationResults exclusively
through a Rule Evaluation Result Source. It SHALL NOT read
RuleEvaluationResults from, or otherwise depend directly on, the
module that produced them.

#### Scenario: RuleEvaluationResults are read only through the Rule Evaluation Result Source
- **WHEN** the Finding Model consumes a RuleEvaluationResult
- **THEN** it SHALL obtain it exclusively through the Rule Evaluation
  Result Source, never through a direct dependency on the module that
  produced it

---

### Requirement: CSM and Rule Evaluation Content Reached Only Through Established Contracts
The Finding Model SHALL obtain RuleEvaluationResult content only
through the Rule Evaluation Result Source. It SHALL NOT read
Repository Evidence directly, SHALL NOT depend on Runtime Model
content, and SHALL NOT depend on the module that produced the CSM
Snapshot, the Analysis Results, or the RuleEvaluationResults it
consumes.

#### Scenario: No direct Repository Evidence or Runtime Model dependency
- **WHEN** the Finding Model requires content to construct a Finding
- **THEN** it SHALL obtain that content from the Rule Evaluation Result
  Source alone, never by reading Repository Evidence or Runtime Model
  content directly

#### Scenario: No dependency on the producing module
- **WHEN** the Finding Model reads RuleEvaluationResult content
- **THEN** it SHALL do so without depending on the specific module that
  produced the RuleEvaluationResults, the Analysis Results they
  consumed, or the CSM Snapshot they were evaluated against

---

### Requirement: Findings Are a Distinct Concept From CSM Knowledge, Analysis Results, and Rule Evaluation Results
A Finding SHALL NOT be represented as, written into, or otherwise
treated as CSM content, an Analysis Result, or a RuleEvaluationResult.
Producing a Finding SHALL NOT alter CSM content, Analysis Results, or
RuleEvaluationResults.

#### Scenario: Producing a Finding does not alter upstream content
- **WHEN** the Finding Model constructs a Finding
- **THEN** the source CSM Snapshot's content, every consumed Analysis
  Result, and every referenced RuleEvaluationResult SHALL remain
  unchanged as a direct result of that construction

#### Scenario: Findings are stored separately from upstream content
- **WHEN** a consumer queries CSM content, Analysis Results, or
  RuleEvaluationResults for a repository
- **THEN** that query SHALL NOT return Findings as if they were CSM
  elements, Analysis Results, or RuleEvaluationResults

---

### Requirement: Single-Repository Finding Scope
Since Rule Framework, Analysis Framework, and the Canonical Software
Model beneath them are each scoped to a single repository, the Finding
Model SHALL scope one construction run to exactly one repository's CSM
Snapshot and its associated RuleEvaluationResults. Constructing a
Finding from RuleEvaluationResults spanning multiple repositories is
out of scope for this version.

#### Scenario: One construction run covers one repository's snapshot
- **WHEN** the Finding Model performs a construction run
- **THEN** every Finding produced by that run SHALL reference exactly
  one repository's CSM Snapshot identity

---

### Requirement: Extension Behavior — New Rule Types Require No Finding Model Changes
Support for a new Rule Type, registered with Rule Framework, SHALL
require no change to the Finding Model's construction, identity,
traceability, or validation mechanisms. The Finding Model SHALL
construct Findings from a new Rule Type's RuleEvaluationResults using
those mechanisms as they already exist, provided that Rule Type's
Rule declares the Category and Severity configuration this
specification requires.

#### Scenario: A new Rule Type's RuleEvaluationResults are consumed without Finding Model changes
- **WHEN** a new Rule Type is registered with Rule Framework and
  produces RuleEvaluationResults
- **THEN** the Finding Model SHALL construct Findings from those
  RuleEvaluationResults using its existing construction, identity,
  traceability, and validation mechanisms unchanged
