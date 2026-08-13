## Scope Notes — Explicitly Deferred (Not Tasks in This Change)

The following are deliberately out of scope for `implement-agent-framework`
(the future change these tasks describe the design/spec surface for)
and are listed here for traceability only — not as checkbox tasks,
since nothing below is meant to become "done":

- The Architecture Compliance Agent, or any other specific agent from
  `project.md` §5's catalogue (`design.md` Context item 1; spec
  `Agent Framework Specifies the Generic Framework Only`).
- A persisted, identified AI Reasoning artifact, and exposure or
  persistence of internal reasoning/chain-of-thought (`design.md`
  Decision 1; spec `No Intermediate AI Reasoning Artifact`, `Internal
  Reasoning Content Is Not Exposed or Persisted`).
- Proposed Change, code generation, remediation execution, deployment,
  remediation verification, and human-approval workflow (`design.md`
  Decision 9; spec `Output Boundary Excludes Remediation and Beyond`).
- Recommendation conflict resolution, precedence, ranking, or
  deduplication (`design.md` Decision 5; spec `No Conflict or
  Precedence Semantics Among Recommendations`).
- Concrete `RecommendationStore` persistence technology, and its
  promotion to `aip-core` (`design.md` Decision 8, Non-Goals).
- Retry or generation-failure orchestration infrastructure (spec
  `Failure Semantics`).
- Any change to `canonical-software-model`, `define-analysis-framework`,
  `define-rule-framework`, or `define-finding-model` — all fixed
  inputs (`design.md` Context; Cross-Capability Impacts, which found
  no gap requiring one).

## 1. `aip-ai` Module Scaffolding and Dependency-Graph Guard

- [ ] 1.1 Add the `aip-ai` Maven module (Java 21+), depending on
  `aip-core` only, per `design.md` Decision 8's dependency diagram —
  the first AI-bearing module, not a sibling of the five deterministic
  modules and not a dependency of any of them.
- [ ] 1.2 Configure `aip-ai`'s POM to depend on `aip-core` only — no
  dependency on `aip-findings`, `aip-rules`, `aip-analysis`,
  `aip-csm-builder`, or `aip-analyzer` (`design.md` Decisions 4, 8;
  spec `Agent Consumption Is Limited to Findings Through the Finding
  Source`). Depends on: 1.1.
- [ ] 1.3 Add a dependency-graph CI check that fails the build if
  `aip-ai` gains any non-test dependency outside `aip-core`, and a
  second check confirming no deterministic module (`aip-csm-builder`,
  `aip-analysis`, `aip-rules`, `aip-findings`) depends on `aip-ai`
  (`design.md` Decision 8). Depends on: 1.2.
- [ ] 1.4 Set up shared test infrastructure and directory conventions
  for `aip-ai`, consistent with the deterministic modules. Depends
  on: 1.1.

## 2. `aip-core`: `FindingSource` Contract

- [ ] 2.1 Define the `FindingSource` contract in `aip-core`: retrieval
  of a Finding by its Evaluation Identity, and retrieval of every
  Finding sharing a given Logical Finding Identity or a given source
  CSM Snapshot identity (`design.md` Decision 8; spec `Finding Source
  Shape`). Depends on: 1.1 (module present; contract itself lives in
  `aip-core` and has no `aip-ai` dependency).
- [ ] 2.2 Add contract tests for `FindingSource` independent of any
  concrete implementation, mirroring `RuleEvaluationResultSource`'s
  own contract-test pattern (spec `Finding Source Shape`, scenario:
  Agent Framework is agnostic to Finding production). Depends on: 2.1.

## 3. Agent Contract and Registration

- [ ] 3.1 Define the Agent contract: stable identifier, monotonic
  version, and the constraint that an Agent declares no input beyond
  Finding consumption in this version (`design.md` Decisions 1, 4;
  spec `Agent Contract`). Depends on: 2.1.
- [ ] 3.2 Implement an `AgentRegistry` (registration API,
  identifier-based lookup) scoped entirely within `aip-ai`, mirroring
  `AnalyzerRegistry`'s and the Rule Type registry's shape (`design.md`
  Decision 8; spec `Agent Registration and Extension Behavior`).
  Depends on: 3.1.
- [ ] 3.3 Confirm registering a new Agent requires no change to
  Recommendation construction, identity, traceability, or validation
  mechanisms, and no change to any deterministic capability's own
  specification or implementation (`design.md` Decision 8's
  extensibility framing; spec `Agent Registration and Extension
  Behavior`, both scenarios). Depends on: 3.2.
- [ ] 3.4 Confirm this specification, and the Agent contract itself,
  impose no requirement on any specific agent's reasoning, prompt
  content, or behavior — only the contract shape every Agent SHALL
  satisfy (`design.md` Context item 1; spec `Agent Framework Specifies
  the Generic Framework Only`). Depends on: 3.1.

## 4. Finding Consumption Boundary Enforcement

- [ ] 4.1 Implement Agent invocation to consume exactly one Finding
  per invocation, obtained exclusively through `FindingSource`
  (`design.md` Decision 4; spec `Agent Contract`, `Agent Consumption
  Is Limited to Findings Through the Finding Source`). Depends
  on: 2.1, 3.1.
- [ ] 4.2 Enforce that an Agent invocation has no means of reading CSM
  content, Analysis Results, or RuleEvaluationResults directly, or any
  Repository Evidence, source repository content, or other AIP service
  or application state (`design.md` Decision 4; spec `Agent
  Consumption Is Limited to Findings Through the Finding Source`, `No
  Arbitrary Repository or Application Access`). Depends on: 4.1.
- [ ] 4.3 Add a dependency-graph test proving `aip-ai` has no reachable
  dependency on `aip-findings`, `aip-rules`, `aip-analysis`, or
  `aip-csm-builder` (`design.md` Decision 4, 8). Depends on: 4.2, 1.3.

## 5. Recommendation Domain/Artifact Model

- [ ] 5.1 Define the Recommendation artifact type: Artifact Identity
  (Section 7), referenced Finding's Evaluation Identity and Logical
  Finding Identity, producing Agent identifier/version, Generation
  Provenance (Section 8), Confidence (Section 10), and Agent-defined
  content (`design.md` Decisions 2, 3; spec `Recommendation Is a
  Distinct, Immutable Artifact From Finding`, `Recommendation
  References Its Source Finding by Both Identities`). Depends
  on: 3.1.
- [ ] 5.2 Implement Recommendation as an artifact distinct from
  Finding — no shared representation, no mutation of the referenced
  Finding, no writing into CSM content, Analysis Results, or
  RuleEvaluationResults as a side effect of construction (`design.md`
  Decision 2; spec `Recommendation Is a Distinct, Immutable Artifact
  From Finding`, scenario: Producing a Recommendation does not alter
  the referenced Finding). Depends on: 5.1.
- [ ] 5.3 Implement Recommendation immutability: no operation modifies
  any field after construction and validation; a changed outcome from
  re-invocation is a new, separately identified Recommendation
  (`design.md` Decision 2; spec `Recommendation Is a Distinct,
  Immutable Artifact From Finding`, scenario: No operation modifies an
  existing Recommendation). Depends on: 5.1.
- [ ] 5.4 Implement direct retention, on every Recommendation, of the
  referenced Finding's Evaluation Identity and Logical Finding Identity
  — each determinable without resolving the referenced Finding first
  (`design.md` Decision 2; spec `Recommendation References Its Source
  Finding by Both Identities`). Depends on: 5.1.
- [ ] 5.5 Implement a Recommendation referencing exactly one Finding in
  v1 — no compound, multi-Finding Recommendation shape (`design.md`
  Decision 2, Alternatives Considered). Depends on: 5.1.
- [ ] 5.6 Implement a Recommendation's generated guidance content as an
  opaque payload whose internal shape is defined by its producing
  Agent — the framework SHALL NOT constrain, interpret, or validate
  that shape beyond the traceability, referential-integrity, and
  required-field expectations Sections 7, 9, 10, and 12 define
  (`design.md` Decision 1's payload framing; spec `Recommendation
  Content Is Agent-Defined and Opaque to the Framework`). Depends
  on: 5.1.

## 6. No Intermediate AI Reasoning Artifact / Reasoning Isolation

- [ ] 6.1 Confirm Agent invocation produces exactly one durable
  artifact (a Recommendation) with no separate, independently
  identified reasoning artifact anywhere in `aip-ai` (`design.md`
  Decision 1; spec `No Intermediate AI Reasoning Artifact`). Depends
  on: 5.1.
- [ ] 6.2 Confirm no contract in this framework requires persistence
  or exposure of an Agent's internal reasoning content, including any
  model-produced chain-of-thought (`design.md` Decision 1; spec
  `Internal Reasoning Content Is Not Exposed or Persisted`). Depends
  on: 6.1.

## 7. Recommendation Artifact Identity

- [ ] 7.1 Implement Recommendation Artifact Identity as a deterministic
  function of: producing Agent identifier, Agent version, referenced
  Finding's Evaluation Identity, and an opaque, invocation-scoped
  Generation Identifier (`design.md` Decision 3; spec `Recommendation
  Artifact Identity`). Depends on: 5.1.
- [ ] 7.2 Implement Generation Identifier assignment: unique per
  invocation, not derived from generated content (`design.md`
  Decision 3; spec `Generation Identifier Uniqueness and
  Non-Content-Derivation`). This specification does not prescribe a
  specific generation mechanism beyond uniqueness and
  non-content-derivation. Depends on: 7.1.
- [ ] 7.3 Enforce that Artifact Identity computation never reads
  generated content, Confidence, or Generation Provenance as an input
  (`design.md` Decision 3; spec `Recommendation Artifact Identity
  Excludes Generated Content`). Depends on: 7.1.
- [ ] 7.4 Add tests: same recorded identity components resolve to the
  same Artifact Identity; a different Generation Identifier (all else
  equal) yields a distinguishable Artifact Identity; Generation
  Identifier is unique per invocation and not content-derived (spec
  `Recommendation Artifact Identity`, both scenarios; `Generation
  Identifier Uniqueness and Non-Content-Derivation`, both scenarios).
  Depends on: 7.1, 7.2.
- [ ] 7.5 Add a test confirming identity computation does not read
  generated content, Confidence, or Generation Provenance (spec
  `Recommendation Artifact Identity Excludes Generated Content`).
  Depends on: 7.3.
- [ ] 7.6 Add tests distinguishing deterministic *lookup* from
  non-deterministic *generation*: two separate invocations with their
  own Generation Identifiers produce independently identified
  Recommendations with no requirement that their content match; the
  same Artifact Identity, retrieved twice, returns the same
  Recommendation including the same recorded content (`design.md`
  Decision 11; spec `Deterministic Recommendation Lookup, Not
  Deterministic Generation`, both scenarios). Depends on: 7.1, 7.4.

## 8. Generation Provenance

- [ ] 8.1 Implement the Generation Provenance record: model/provider
  identifier and version, generation configuration (opaque,
  Agent-defined values), and a generation timestamp, retained on every
  Recommendation and excluded from Artifact Identity computation
  (`design.md` Decisions 3, 10; spec `Generation Provenance`).
  Depends on: 5.1, 7.3.
- [ ] 8.2 Add a test confirming Generation Provenance is present with
  all required fields on every constructed Recommendation (spec
  `Generation Provenance`, scenario: Generation Provenance is present
  on every Recommendation). Depends on: 8.1.
- [ ] 8.3 Add a test confirming Generation Provenance remains fully
  inspectable directly from a Recommendation, independent of Artifact
  Identity computation (spec `Generation Provenance`, scenario:
  Generation Provenance remains fully inspectable independent of
  identity computation). Depends on: 8.1.

## 9. AI-Content Isolation

- [ ] 9.1 Confirm no CSM element type, Analysis Result, RuleEvaluationResult,
  or Finding type carries a field capable of representing prompt
  content or template identifiers, model/provider details, or
  generation configuration (`design.md` Decision 10; spec
  `AI-Specific Content Isolation From Deterministic Artifacts`,
  scenario: No AI-specific content appears in a deterministic
  artifact). Depends on: 8.1.
- [ ] 9.2 Confirm all AI-specific mechanics needed for explainability
  are recorded exclusively within Generation Provenance (`design.md`
  Decision 10; spec `AI-Specific Content Isolation From Deterministic
  Artifacts`, scenario: AI-specific content is confined to Generation
  Provenance). Depends on: 9.1.

## 10. Recommendation Confidence

- [ ] 10.1 Implement Confidence as a mandatory field on every
  Recommendation, declared by the producing Agent at generation time,
  never computed, derived, or overridden by the framework (`design.md`
  Decision 6; spec `Recommendation Confidence Is Agent-Produced and
  Mandatory`). Depends on: 5.1.
- [ ] 10.2 Confirm Recommendation Confidence is independent of, and
  never copied from or constrained by, the referenced Finding's own
  fixed Confidence value (`design.md` Decision 6; spec `Recommendation
  Confidence Is Distinct From Finding Confidence`). Depends on: 10.1.
- [ ] 10.3 Add tests confirming Confidence is present on every
  Recommendation and is never altered by construction, validation, or
  publication (spec `Recommendation Confidence Is Agent-Produced and
  Mandatory`, both scenarios). Depends on: 10.1.

## 11. Recommendation Multiplicity and Invocation Independence

- [ ] 11.1 Implement one Agent invocation producing exactly one
  Recommendation (`design.md` Decision 5; spec `Recommendation
  Multiplicity — One Invocation, One Recommendation`, scenario: One
  invocation produces exactly one Recommendation). Depends on: 5.1.
- [ ] 11.2 Implement unlimited Recommendation multiplicity per Finding
  — no cap on how many Recommendations, from the same or different
  Agents, may reference one Finding (`design.md` Decision 5; spec
  `Recommendation Multiplicity — One Invocation, One Recommendation`,
  scenario: A Finding may be referenced by multiple Recommendations).
  Depends on: 11.1.
- [ ] 11.3 Confirm no ranking, primary-marking, deduplication, or
  conflict resolution exists anywhere in `aip-ai` for Recommendations
  referencing the same Finding (`design.md` Decision 5; spec `No
  Conflict or Precedence Semantics Among Recommendations`, both
  scenarios). Depends on: 11.2.
- [ ] 11.4 Implement Agent invocations as independent of one another —
  no invocation depends on another invocation having occurred, its
  result, or invocation order; no mechanism exists for declaring such
  a dependency (`design.md` Decision 5's concurrency corollary; spec
  `Agent Invocation Independence`, both scenarios). Depends on: 11.1.
- [ ] 11.5 Confirm the invocation contract permits concurrent or
  reordered execution without affecting correctness; a v1
  implementation MAY execute invocations sequentially (`design.md`
  Decision 5's concurrency corollary, citing
  `define-analysis-framework` Decisions 2 and 6). Depends on: 11.4.

## 12. Recommendation Validation

- [ ] 12.1 Implement `RecommendationValidator`, checking: the
  referenced Finding exists and is already published; the declared
  producing Agent and version correspond to a currently registered
  Agent; Generation Provenance is present with a non-empty
  model/provider identifier and a generation timestamp; Confidence and
  content are present; and the Recommendation carries no field
  representing a claimed mutation of CSM content, an Analysis Result,
  a RuleEvaluationResult, or the referenced Finding (`design.md`
  Decision 7; spec `Recommendation Validation Before Publication`).
  Depends on: 5.1, 7.1, 8.1, 10.1, 2.1, 3.2.
- [ ] 12.2 Enforce that `RecommendationValidator` does not, and
  structurally cannot, determine whether a Recommendation's guidance
  is substantively correct, useful, or of sufficient quality —
  including that it does not gate on Confidence value (`design.md`
  Decision 7's explicit non-goal; spec `Validation Does Not Determine
  Substantive Correctness`, both scenarios). Depends on: 12.1.
- [ ] 12.3 Add tests for all five validation-rejection scenarios: valid
  Recommendation published; unpublished-Finding reference rejected;
  unregistered Agent/version rejected; incomplete Generation Provenance
  rejected; missing Confidence or content rejected (spec
  `Recommendation Validation Before Publication`, all five scenarios).
  Depends on: 12.1.

## 13. Recommendation Publishing

- [ ] 13.1 Implement `RecommendationPublisher`, calling the
  Recommendation persistence mechanism's write operation only for a
  Recommendation that has passed `RecommendationValidator` (`design.md`
  Decision 7; spec `Recommendation Publishing`). Depends on: 12.1,
  14.1.
- [ ] 13.2 Add a test confirming a Recommendation that fails validation
  never reaches the persistence mechanism through this operation (spec
  `Recommendation Publishing`). Depends on: 13.1.

## 14. Recommendation Persistence Abstraction

- [ ] 14.1 Define a `RecommendationStore` abstraction (interface only;
  no concrete implementation), scoped within `aip-ai` and not promoted
  to `aip-core` (`design.md` Decision 8; spec `Recommendations Are
  Durable, Individually Identifiable Artifacts`). Depends on: 5.1.
- [ ] 14.2 Add tests confirming a Recommendation remains retrievable by
  its Artifact Identity after the producing invocation's process has
  exited, and that a new, differently-identified Recommendation never
  overwrites a prior one (spec `Recommendations Are Durable,
  Individually Identifiable Artifacts`, both scenarios). Depends
  on: 14.1, 7.1.

## 15. Deterministic-Layer Protection

- [ ] 15.1 Add tests confirming that producing, validating, or
  publishing a Recommendation never alters CSM content, Analysis
  Results, RuleEvaluationResults, Findings, or Rule decisions
  (`design.md` Context item 7; spec `Deterministic-Layer Protection`).
  Depends on: 5.2, 12.1, 13.1.

## 16. Output Boundary Enforcement

- [ ] 16.1 Confirm `aip-ai`'s only durable output type is
  Recommendation — no Proposed Change artifact, code generation,
  remediation execution, deployment, remediation verification, or
  human-approval workflow exists anywhere in this module (`design.md`
  Decision 9; spec `Output Boundary Excludes Remediation and Beyond`).
  Depends on: 5.1.

## 17. Failure Semantics

- [ ] 17.1 Implement rejection of Recommendation construction or
  publication (producing no usable Recommendation) when: the
  referenced Finding does not exist or is invalid; the declared Agent
  or version is not registered; Generation Provenance is incomplete;
  or a required payload field is missing (`design.md` Decision 7;
  spec `Failure Semantics`, scenario: Missing or invalid Finding
  reference prevents publication). Depends on: 12.1.
- [ ] 17.2 Implement generation-failure handling: a failed or
  incomplete underlying model call results in no Recommendation being
  produced for that invocation, never a partially-constructed or
  invalid Recommendation reaching publication (spec `Failure
  Semantics`, scenario: Generation failure produces no Recommendation).
  No retry or orchestration infrastructure is introduced for this
  version. Depends on: 17.1.

## 18. Dependency-Boundary and Architectural-Invariant Tests

- [ ] 18.1 Add a dependency-graph test proving no deterministic module
  (`aip-csm-builder`, `aip-analysis`, `aip-rules`, `aip-findings`)
  depends on `aip-ai`, and that `aip-ai` depends on `aip-core` only
  (`design.md` Decisions 4, 8). Depends on: 1.3, 4.3.
- [ ] 18.2 Add a test confirming every Finding an Agent consumes, and
  every Recommendation it produces, concerns exactly one repository's
  CSM Snapshot, inherited from the referenced Finding (`design.md`
  Context's single-repository-scope corollary; spec
  `Single-Repository Finding Consumption`). Depends on: 5.4.
- [ ] 18.3 Add a test confirming this specification's Agent contract
  imposes no requirement on any specific agent's internal reasoning,
  prompts, or behavior (spec `Agent Framework Specifies the Generic
  Framework Only`). Depends on: 3.4.

## 19. Fixture/Test Infrastructure

- [ ] 19.1 Build fixture Findings (single- and multi-RuleEvaluationResult
  provenance, varying Logical Finding Identities and Evaluation
  Identities) sufficient to exercise Sections 4–17 without depending
  on a real Finding Model implementation. Depends on: 2.1.
- [ ] 19.2 Build a fixture `FindingSource` implementation (in-memory,
  test scope) satisfying the contract from Section 2, for use by every
  other section's tests. Depends on: 2.2, 19.1.
- [ ] 19.3 Build a fixture Agent (test scope only) with deterministic,
  non-model-backed output — producing varying Confidence and content
  across invocations to exercise multiplicity and identity tests
  without depending on a real model/provider integration. Depends
  on: 3.1.
- [ ] 19.4 Build a fixture `RecommendationStore` implementation
  (in-memory, test scope) satisfying Section 14's abstraction, for use
  by Sections 12–17's tests. Depends on: 14.1.

## 20. Requirement/Scenario Traceability and End-to-End Verification

- [ ] 20.1 Build a traceability matrix mapping each of
  `specs/agent-framework/spec.md`'s 30 requirements and 51 scenarios
  to the task(s) and test(s) that implement/verify it, following
  `implement-csm-builder`'s own `traceability.md` precedent. Depends
  on: Sections 1–19.
- [ ] 20.2 Confirm every one of `design.md`'s 11 Decisions (plus its
  two corollaries — Agent invocation independence/concurrency under
  Decision 5, and single-repository consumption in Context) has at
  least one corresponding implementation task and test above; record
  any gap found rather than silently leaving it uncovered. Depends
  on: 20.1.
- [ ] 20.3 Add an end-to-end test: invoke a fixture Agent against a
  fixture Finding, construct, validate, and publish the resulting
  Recommendation, then retrieve it by Artifact Identity and
  independently confirm its retained Finding identities, Generation
  Provenance, and Confidence, confirming the full path (Sections 2–3,
  5, 7–14) is consistent end to end. Depends on: 13.1, 14.2, 19.3.
- [ ] 20.4 Run full validation (`openspec validate agent-framework
  --strict` once specs are synced, or the equivalent check available
  during implementation) and record the result in this change's
  verification artifact. Depends on: 20.1, 20.2.
