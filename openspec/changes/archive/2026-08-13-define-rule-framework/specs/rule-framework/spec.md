## Purpose

The Rule Framework evaluates organization-defined policy — a Rule,
each a declarative configuration of a versioned Rule Type — against
Analysis Framework's Analysis View and Analysis Results, producing
durable, traceable Rule Evaluation Results that a future Finding Model
formalizes into structured findings.

The archived Canonical Software Model specification describes its
Evidence-Knowledge-Analysis-Findings pipeline at the conceptual level:
Analysis evaluates policy or heuristics against CSM knowledge and
produces Findings. This capability, together with Analysis Framework,
preserves that meaning at the pipeline level while refining its
architecture into explicit layers: Analysis Framework performs
policy-independent computation over a CSM Snapshot, producing Analysis
Results; the Rule Framework is where policy interpretation actually
occurs, evaluating declared Rules against Analysis View and Analysis
Result content; a future Finding Model formalizes Rule Framework's
output into the richer structure the archived specification's own
Finding concept implies. Every Rule Evaluation Result this
specification defines is therefore a pre-Finding policy-evaluation
artifact, never itself a Finding.

## ADDED Requirements

### Requirement: Deterministic, Declarative Rule Evaluation Only
The Rule Framework SHALL produce every Rule Evaluation Result by
deterministic evaluation of a Rule's declared inputs only. It SHALL
NOT use AI/LLM reasoning, heuristic scoring, or probabilistic inference
to produce a Rule Evaluation Result in this version.

#### Scenario: No AI or heuristic judgment used to produce a Rule Evaluation Result
- **WHEN** the Rule Framework evaluates any registered Rule
- **THEN** the resulting Rule Evaluation Result SHALL be derivable by
  deterministic evaluation alone, with no AI/LLM call or heuristic
  scoring step involved in producing it

#### Scenario: Repeated evaluation over unchanged input is identical
- **WHEN** the Rule Framework evaluates the same Rule twice against the
  same Analysis View and the same set of consumed Analysis Results
- **THEN** both evaluations SHALL produce Rule Evaluation Results with
  identical identity and identical outcome

---

### Requirement: Rule Type Contract
A Rule Type SHALL declare a stable identifier, a monotonically
versioned identifier, the set of Analyzer identifiers whose Analysis
Results it consumes, and a Rule Scope (see Rule Scope Declaration) for
direct Analysis View reads. A Rule Type SHALL be evaluable given
exactly these declared inputs, and SHALL NOT read any CSM or Analysis
content outside them.

#### Scenario: Rule Type declares identifier, version, and inputs
- **WHEN** a Rule Type is registered with the Rule Framework
- **THEN** it SHALL carry a stable identifier, a version, a declared
  set of required Analyzer identifiers (which MAY be empty), and a Rule
  Scope declaration

#### Scenario: Rule Type reads no undeclared content
- **WHEN** a Rule Type is evaluated
- **THEN** it SHALL be able to complete that evaluation using only its
  declared Analysis Result inputs and its declared Rule Scope's
  Analysis View content, without reading any other Analyzer's Analysis
  Results or any Analysis View content outside its declared scope

---

### Requirement: Rule Declaration
A Rule SHALL be a declarative, version-controlled configuration binding
exactly one Rule Type to specific CSM vocabulary — entity kinds,
relationship types, element identities, and/or native-attribute
predicate values — without itself containing executable logic. A Rule
SHALL NOT be defined as, or require, source code.

#### Scenario: Rule configures a Rule Type without code
- **WHEN** an organization defines a Rule
- **THEN** that Rule SHALL consist of a reference to a registered Rule
  Type plus configuration values, and SHALL NOT require compiling or
  deploying new source code to take effect

#### Scenario: One Rule Type supports multiple independently configured Rules
- **WHEN** two Rules both reference the same Rule Type with different
  configuration values
- **THEN** the Rule Framework SHALL evaluate each Rule independently,
  using only that Rule's own configuration

---

### Requirement: No Rule-to-Rule Composition
A Rule SHALL NOT consume another Rule's Rule Evaluation Result, and
SHALL NOT depend on any other Rule having been registered, evaluated,
or having produced a result. The Rule Framework SHALL NOT provide a
mechanism for declaring an ordering or dependency between Rules in this
version.

#### Scenario: Rule Evaluation Result is unaffected by which other Rules are registered
- **WHEN** a Rule is evaluated
- **THEN** its Rule Evaluation Result SHALL be the same regardless of
  which other Rules are also registered and evaluated in that run

#### Scenario: No declared dependency between Rules is honored
- **WHEN** the Rule Framework registers a set of Rules
- **THEN** it SHALL NOT accept, require, or act on a declared
  execution-order or output dependency between any two of them

---

### Requirement: Rule Scope Declaration
A Rule Type's Rule Scope SHALL be a declared, non-empty set of CSM
entity kinds and/or CSM relationship types, with an optional
containment-level anchor and an optional native-attribute predicate
refinement — the identical declaration shape Analysis Scope already
establishes for an Analyzer. A Rule Type with no declared anchor SHALL
be invoked once for the whole repository; a Rule Type with a declared
anchor SHALL be invoked once per matching contained element present in
the CSM Snapshot.

#### Scenario: Rule Type declares a kind-based scope
- **WHEN** a Rule Type is registered
- **THEN** its Rule Scope SHALL identify at least one CSM entity kind
  or CSM relationship type it reads

#### Scenario: Unanchored Rule Type is invoked once per repository
- **WHEN** a Rule Type declares no containment-level anchor
- **THEN** the Rule Framework SHALL evaluate it exactly once per
  analysis run, covering the whole repository's Analysis View

#### Scenario: Anchored Rule Type is invoked once per matching contained element
- **WHEN** a Rule Type declares a containment-level anchor
- **THEN** the Rule Framework SHALL evaluate it once for each element
  at that containment level present in the CSM Snapshot, each
  evaluation covering that element's own Rule Scope instance

---

### Requirement: Kind-Based Rule Applicability
The Rule Framework SHALL determine a Rule's applicability to a given
Rule Scope instance by whether the corresponding CSM content contains
at least one element or relationship of a kind within that Rule Type's
declared Rule Scope. A Rule Scope instance with no matching kind
present SHALL NOT be evaluated at all — no Rule Evaluation Result of
any outcome SHALL be produced for it.

#### Scenario: Rule Scope instance with no matching kind is not evaluated
- **WHEN** none of a Rule Type's declared Rule Scope kinds are present
  in a given Rule Scope instance's CSM content
- **THEN** the Rule Framework SHALL NOT evaluate that Rule for that
  instance, and SHALL NOT produce a Rule Evaluation Result for it

#### Scenario: Rule Scope instance with a matching kind is evaluated
- **WHEN** at least one of a Rule Type's declared Rule Scope kinds is
  present in a given Rule Scope instance's CSM content
- **THEN** the Rule Framework SHALL evaluate that Rule for that
  instance, producing a Rule Evaluation Result

---

### Requirement: Native-Attribute Rule Applicability Refinement
A Rule Type MAY further restrict its own applicability using a
predicate over native-evidence-attribute content already present on
in-scope CSM elements or relationships. This refinement SHALL NOT
require the Rule Framework to introduce a separate scope-declaration or
registration mechanism for it.

#### Scenario: Ecosystem-specific Rule Type is not evaluated when no matching native attribute is present
- **WHEN** a Rule Type's applicability predicate requires a native
  attribute value that is absent from every element/relationship within
  its Rule Scope instance
- **THEN** the Rule Framework SHALL NOT evaluate that Rule Type for
  that instance, and SHALL NOT produce a Rule Evaluation Result for it

#### Scenario: Ecosystem-specific Rule Type is evaluated when its native attribute predicate is satisfied
- **WHEN** a Rule Type's applicability predicate matches a native
  attribute value present within its declared, kind-based Rule Scope
- **THEN** the Rule Framework SHALL evaluate that Rule Type for that
  instance

---

### Requirement: Analysis Result Source Shape
An Analysis Result Source SHALL expose, at minimum: an Analysis Result
retrievable by its own identity, and the set of Analysis Results
produced by a given Analyzer against a given CSM Snapshot identity. The
Rule Framework SHALL depend only on this shape to consume Analysis
Results, independent of which module produced or how it persisted the
underlying Analysis Results.

#### Scenario: Analysis Result Source exposes retrieval by identity and by Analyzer/snapshot
- **WHEN** the Rule Framework reads an Analysis Result Source
- **THEN** it SHALL be able to retrieve a specific Analysis Result by
  its identity, and SHALL be able to retrieve every Analysis Result a
  named Analyzer produced against a named CSM Snapshot identity

#### Scenario: Rule Framework is agnostic to Analysis Result production
- **WHEN** two different Analysis Result Sources expose equivalent
  Analysis Result content, produced by different upstream mechanisms
- **THEN** the Rule Framework SHALL evaluate Rules against both without
  requiring any change to its own behavior

---

### Requirement: Analysis Result Consumption Through the Analysis Result Source Only
A Rule Type SHALL consume its declared Analyzer's Analysis Results
exclusively through an Analysis Result Source. The Rule Framework
SHALL NOT read Analysis Results from, or otherwise depend directly on,
the module that produced them.

#### Scenario: Analysis Results are read only through the Analysis Result Source
- **WHEN** a Rule Type consumes a declared Analyzer's output
- **THEN** it SHALL obtain that output exclusively through the
  Analysis Result Source, never through a direct dependency on the
  Analyzer's own producing module

---

### Requirement: Rule Evaluation Outcome Semantics
Every Rule Evaluation Result the Rule Framework produces SHALL carry
exactly one outcome: `PASS` (the Rule's declared condition was
evaluated and satisfied), `FAIL` (the Rule's declared condition was
evaluated and not satisfied), or `NOT_APPLICABLE` (the Rule Scope
instance was evaluated, but content the Rule's condition specifically
depends on was not present, so no PASS/FAIL determination could be
made). `NOT_APPLICABLE` is a produced Rule Evaluation Result, not the
absence of one — it is distinct from a Rule Scope instance that was
never evaluated at all because it failed kind-based or native-attribute
applicability (see Kind-Based Rule Applicability, Native-Attribute Rule
Applicability Refinement), for which no Rule Evaluation Result is
produced.

#### Scenario: Satisfied condition produces PASS
- **WHEN** a Rule's declared condition is evaluated against an
  applicable Rule Scope instance and is satisfied
- **THEN** the Rule Framework SHALL produce a Rule Evaluation Result
  with outcome `PASS`

#### Scenario: Unsatisfied condition produces FAIL
- **WHEN** a Rule's declared condition is evaluated against an
  applicable Rule Scope instance and is not satisfied
- **THEN** the Rule Framework SHALL produce a Rule Evaluation Result
  with outcome `FAIL`

#### Scenario: Missing condition-specific content produces NOT_APPLICABLE, not silence
- **WHEN** a Rule Scope instance is applicable (kind-based and
  native-attribute applicability are both satisfied) but content the
  Rule's own condition specifically depends on is absent from that
  instance
- **THEN** the Rule Framework SHALL produce a Rule Evaluation Result
  with outcome `NOT_APPLICABLE`, rather than producing no result at all

#### Scenario: Every outcome is offered for publication
- **WHEN** the Rule Framework completes evaluating an applicable Rule
  Scope instance
- **THEN** it SHALL offer the resulting Rule Evaluation Result —
  whether `PASS`, `FAIL`, or `NOT_APPLICABLE` — for validation and
  publication; it SHALL NOT discard a `PASS` or `NOT_APPLICABLE`
  outcome before that point on the basis of its outcome alone

---

### Requirement: Deterministic Rule Evaluation Result Identity
A Rule Evaluation Result's identity SHALL be a deterministic function
of: the producing Rule's identifier, the Rule's version, the source CSM
Snapshot's identity, the Rule Scope instance covered, and the complete
set of consumed Analysis Result identities. Random or otherwise
non-reproducible identity generation SHALL NOT be used. Two evaluations
that consumed different Analysis Result identities SHALL produce
different Rule Evaluation Result identities, even when the Rule, its
version, the source CSM Snapshot identity, and the Rule Scope instance
are otherwise identical.

#### Scenario: Identical inputs produce identical Result identity
- **WHEN** the same Rule, at the same version, is evaluated against the
  same Rule Scope instance derived from the same CSM Snapshot identity
  and the same set of consumed Analysis Result identities, across two
  separate evaluation runs
- **THEN** both runs SHALL produce a Rule Evaluation Result with the
  same identity

#### Scenario: Different consumed Analysis Results yield a distinguishable identity
- **WHEN** the set of consumed Analysis Result identities differs
  between two evaluations, with the Rule, its version, the source CSM
  Snapshot identity, and the Rule Scope instance otherwise unchanged
- **THEN** the two evaluations' Rule Evaluation Result identities SHALL
  be distinguishable from one another

#### Scenario: A different Rule version yields a distinguishable identity
- **WHEN** a Rule's version differs between two evaluations, with its
  Rule Scope instance, source CSM Snapshot identity, and consumed
  Analysis Result identities otherwise unchanged
- **THEN** the two evaluations' Rule Evaluation Result identities SHALL
  be distinguishable from one another

---

### Requirement: Rule Evaluation Result Traceability
Every Rule Evaluation Result SHALL retain a reference to its producing
Rule's identifier and version, a reference to the source CSM Snapshot's
identity, the Rule Scope instance it covers, and the complete set of
consumed Analysis Result identities, sufficient to trace the Result
back to the Analysis Results and Analysis View content it was evaluated
from.

#### Scenario: Result traceable to its producing Rule
- **WHEN** a Rule Evaluation Result is inspected
- **THEN** it SHALL be possible to determine which Rule, and which
  version of that Rule, produced it

#### Scenario: Result traceable to its consumed Analysis Results
- **WHEN** a Rule Evaluation Result is inspected
- **THEN** it SHALL be possible to determine the complete set of
  Analysis Result identities it consumed, and the source CSM Snapshot
  identity it was evaluated against

---

### Requirement: Rule Evaluation Result Payload Is Rule-Type-Defined
The Rule Framework SHALL treat a Rule Evaluation Result's diagnostic
content beyond its outcome (`PASS`/`FAIL`/`NOT_APPLICABLE`) as an
opaque payload whose internal shape is defined by its producing Rule
Type. The Rule Framework SHALL NOT constrain, interpret, or validate
that payload's internal structure beyond the traceability and
referential-integrity expectations defined elsewhere in this
specification.

#### Scenario: Differently-shaped diagnostic payloads from different Rule Types are both accepted
- **WHEN** two different Rule Types produce Rule Evaluation Results
  with unrelated internal diagnostic payload shapes
- **THEN** the Rule Framework SHALL store and retrieve both without
  rejecting either on the basis of payload shape

#### Scenario: Framework operations do not require understanding diagnostic payload content
- **WHEN** the Rule Framework stores, retrieves, or validates a Rule
  Evaluation Result
- **THEN** it SHALL be able to do so without interpreting the meaning
  of that Result's diagnostic payload content

---

### Requirement: Rule Evaluation Results Are Durable, Individually Identifiable Artifacts
The Rule Framework SHALL treat each Rule Evaluation Result as an
individually identifiable artifact of an evaluation run, retrievable
independent of the process that produced it, analogous to how an
Analysis Result is individually identifiable and retrievable. The
specific persistence mechanism is not constrained by this requirement.

#### Scenario: Rule Evaluation Result remains retrievable after the producing run ends
- **WHEN** an evaluation run that produced a Rule Evaluation Result has
  completed and its process has exited
- **THEN** that Rule Evaluation Result SHALL remain retrievable by its
  identity

#### Scenario: A new Rule Evaluation Result does not overwrite a prior, differently-identified one
- **WHEN** a new evaluation run produces a Rule Evaluation Result with
  an identity distinct from a prior run's Rule Evaluation Result
- **THEN** the prior Rule Evaluation Result SHALL remain retrievable
  and unaffected

---

### Requirement: Rule Evaluation Result Validation Before Publication
The Rule Framework SHALL validate a constructed Rule Evaluation Result
before it is considered usable output, checking at minimum: every
Analysis Result identity and CSM element/Subject identity the Result
references SHALL exist among its declared consumed Analysis Results
and within the source CSM Snapshot it was evaluated against; the
Result's declared producing Rule and version SHALL correspond to a
currently registered Rule; and the Result's content SHALL remain
within its Rule Type's declared Rule Scope. A Rule Evaluation Result
that fails this validation SHALL NOT be published as usable output.

#### Scenario: Valid Result is published as usable output
- **WHEN** a constructed Rule Evaluation Result satisfies every
  validation check
- **THEN** it SHALL be published as usable output

#### Scenario: Result referencing an unavailable Analysis Result or CSM identity is rejected
- **WHEN** a constructed Rule Evaluation Result references an Analysis
  Result identity not among its declared consumed Analysis Results, or
  a CSM element/Subject identity absent from its source CSM Snapshot
- **THEN** it SHALL fail validation and SHALL NOT be published

#### Scenario: Result from an unregistered Rule/version is rejected
- **WHEN** a constructed Rule Evaluation Result declares a producing
  Rule identifier and version that does not correspond to a currently
  registered Rule
- **THEN** it SHALL fail validation and SHALL NOT be published

#### Scenario: Result exceeding its Rule Type's declared scope is rejected
- **WHEN** a constructed Rule Evaluation Result's content extends
  beyond its producing Rule Type's declared Rule Scope
- **THEN** it SHALL fail validation and SHALL NOT be published

---

### Requirement: Rule Evaluation Results Are a Distinct Concept From CSM Knowledge, Analysis Results, and Findings
A Rule Evaluation Result SHALL NOT be represented as, written into, or
otherwise treated as CSM content or as an Analysis Result. It SHALL NOT
be represented as a Finding either — it is a distinct, pre-Finding
concept in the refined Analysis-Rule-Finding pipeline this
specification's Purpose describes, referencing CSM elements and
Analysis Results by identity without becoming part of either.

#### Scenario: Producing a Rule Evaluation Result does not alter CSM or Analysis content
- **WHEN** the Rule Framework produces a Rule Evaluation Result
- **THEN** the source CSM Snapshot's content and every consumed
  Analysis Result SHALL remain unchanged as a direct result of that
  production

#### Scenario: Rule Evaluation Results are stored separately from CSM and Analysis content
- **WHEN** a consumer queries CSM content or Analysis Results for a
  repository
- **THEN** that query SHALL NOT return Rule Evaluation Results as if
  they were CSM elements, CSM relationships, or Analysis Results

---

### Requirement: CSM and Analysis Content Reached Only Through Established Contracts
The Rule Framework SHALL obtain CSM content only through the Analysis
View, and Analysis Results only through the Analysis Result Source. It
SHALL NOT read Repository Evidence directly, SHALL NOT depend on
Runtime Model content, and SHALL NOT depend on the module that produced
the CSM Snapshot or the Analysis Results it consumes. This includes the
source CSM Snapshot's own identity: the Rule Framework SHALL obtain it
from the Analysis View (which exposes it, per Analysis Framework's own
Analysis View Construction requirement) rather than depending on a CSM
Snapshot Source directly.

#### Scenario: No direct Repository Evidence or Runtime Model dependency
- **WHEN** a Rule requires structural, relational, or analytical
  information to complete its evaluation
- **THEN** it SHALL obtain that information from the Analysis View or
  the Analysis Result Source alone, never by reading Repository
  Evidence or Runtime Model content directly

#### Scenario: No dependency on the producing module
- **WHEN** the Rule Framework reads CSM content or Analysis Results
- **THEN** it SHALL do so without depending on the specific module that
  constructed the CSM Snapshot or produced the Analysis Results

#### Scenario: Source CSM Snapshot identity is obtained from the Analysis View
- **WHEN** the Rule Framework needs the source CSM Snapshot's identity
  — for Rule Evaluation Result identity or traceability
- **THEN** it SHALL obtain that identity from the Analysis View, and
  SHALL NOT depend on a CSM Snapshot Source directly to obtain it

---

### Requirement: Single-Repository Rule Evaluation Scope
Since the Analysis Framework, and the Canonical Software Model beneath
it, are each scoped to a single repository, the Rule Framework SHALL
scope one evaluation run to exactly one repository's CSM Snapshot and
its associated Analysis Results. Evaluating a Rule against content
spanning multiple repositories is out of scope for this version.

#### Scenario: One evaluation run covers one repository's snapshot
- **WHEN** the Rule Framework performs an evaluation run
- **THEN** every Rule Evaluation Result produced by that run SHALL
  reference exactly one repository's CSM Snapshot identity

---

### Requirement: Extension Mechanism for New Rule Types
Support for evaluating an additional policy concern SHALL be
introduced only by registering a new Rule Type against the Rule
Framework's existing evaluation, identity, traceability, and validation
mechanisms. Registering a new Rule Type SHALL NOT require a change to
those mechanisms.

#### Scenario: New Rule Type registered without core mechanism changes
- **WHEN** a new Rule Type is registered with the Rule Framework
- **THEN** the Rule Evaluation Result identity, traceability, and
  validation mechanisms SHALL remain unchanged, and the new Rule Type
  SHALL be evaluable using them as they already exist
