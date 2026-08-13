## Scope Notes — Explicitly Deferred (Not Tasks in This Change)

The following are deliberately out of scope and listed here for
traceability only — not as checkbox tasks:

- The Architecture Compliance Agent, or any other specific agent from
  `project.md` §5's catalogue (`define-agent-framework/design.md`
  Context item 1).
- A persisted, identified AI Reasoning artifact, and exposure or
  persistence of internal reasoning/chain-of-thought (`define-agent-
  framework/design.md` Decision 1).
- Proposed Change, code generation, remediation execution, deployment,
  remediation verification, and human-approval workflow (`define-
  agent-framework/design.md` Decision 9).
- Recommendation conflict resolution, precedence, ranking, or
  deduplication (`define-agent-framework/design.md` Decision 5).
- Concrete `RecommendationStore` persistence technology, and its
  promotion to `aip-core` (`define-agent-framework/design.md`
  Decision 8, Non-Goals).
- Retry or generation-failure orchestration infrastructure (spec
  `Failure Semantics`).
- A real LLM/model-provider adapter (this change's own `design.md`
  Binding Decision 4 — fixtures only).
- An automatic Finding-discovery-and-dispatch orchestrator (this
  change's own `design.md` Risks — a named, accepted absence, not a
  gap).
- Any change to `canonical-software-model`, `software-repository-
  understanding`, `csm-builder`, `define-analysis-framework`,
  `define-rule-framework`, `define-finding-model`,
  `implement-analysis-framework`, `implement-rule-framework`,
  `implement-finding-model`, or `define-agent-framework`'s own
  specification.

## 1. `aip-ai` Module Scaffolding and Dependency-Graph Guard

- [x] 1.1 Add the `aip-ai` Maven module (Java 21+) — the first
  AI-bearing module, not a sibling of and sharing no dependency
  relationship with `aip-csm-builder`/`aip-analysis`/`aip-rules`/
  `aip-findings` (`design.md` Decision 9).
- [x] 1.2 Configure `aip-ai`'s POM to depend on `aip-core` only — no
  dependency on `aip-findings`, `aip-rules`, `aip-analysis`,
  `aip-csm-builder`, or `aip-analyzer` (`design.md` Decision 9;
  `define-agent-framework/design.md` Decisions 4, 8; spec `Agent
  Consumption Is Limited to Findings Through the Finding Source`).
  Depends on: 1.1.
- [x] 1.3 Add the standard `check-module-dependencies.sh` and
  `check-fixture-package-scope.sh` guard executions. Depends on: 1.2.
- [x] 1.4 Wire four executions of the generic `scripts/check-no-
  module-reference.sh` against `aip-ai`: forbidding `aip\.findings\.`,
  `aip\.rules\.`, `aip\.analysis\.`, and `aip\.csmbuilder\.`
  (`design.md` Decision 9). Confirm the reverse guard ("no
  deterministic module depends on `aip-ai`") is already satisfied by
  each deterministic module's own existing `check-module-dependencies.sh`
  execution, requiring no new script (`design.md` Decision 9). Depends
  on: 1.3.
- [x] 1.5 Confirm `check-no-ai-heuristic-imports.sh` is deliberately
  NOT wired for `aip-ai`, and record why in this module's own
  `package-info.java` (`design.md` Decision 9; this change's own
  `proposal.md` Binding Decision 2). Depends on: 1.1.
- [x] 1.6 Set up shared test infrastructure and directory conventions
  for `aip-ai`, consistent with the deterministic modules. Depends
  on: 1.1.

## 2. `aip-core`: `FindingSource` Contract

- [x] 2.1 Define the `FindingSource` contract in `aip-core`
  (`aip.core.csm`): `read(EvaluationIdentity): Optional<Finding>`,
  `listByLogicalFindingIdentity(LogicalFindingIdentity): Set<Finding>`,
  `listBySourceSnapshotId(CsmSnapshotId): Set<Finding>` (`design.md`
  Decision 1; `define-agent-framework/design.md` Decision 8; spec
  `Finding Source Shape`). Depends on: 1.1.
- [x] 2.2 Add contract tests for `FindingSource` independent of any
  concrete implementation, mirroring `RuleEvaluationResultSource`'s
  own contract-test pattern (spec `Finding Source Shape`, scenario:
  Agent Framework is agnostic to Finding production). Depends on: 2.1.

## 3. Agent Contract and Registration

- [x] 3.1 Define the `Agent` interface in `aip-ai`: `identifier():
  String`, `version(): int`, `invoke(Finding): AgentInvocationResult`
  (`design.md` Decision 7; spec `Agent Contract`). Depends on: 1.1.
- [x] 3.2 Define `AgentInvocationResult`: `content`, `confidence`,
  `provenance` (`design.md` Decision 7). Depends on: 3.1.
- [x] 3.3 Implement `AgentRegistry`: registration API, identifier-based
  lookup, mirroring `RuleTypeRegistry`'s shape (`design.md` Decision 8;
  spec `Agent Registration and Extension Behavior`). Depends on: 3.1.
- [x] 3.4 Add tests confirming a new Agent registers with a stable
  identifier and version (spec: Agent declares identifier and
  version). Depends on: 3.3.

## 4. Finding Consumption Boundary Enforcement

- [x] 4.1 Implement Agent invocation as reading its Finding exclusively
  through `FindingSource` — no direct dependency on `aip-findings`,
  CSM content, `AnalysisResult`s, or `RuleEvaluationResult`s
  (`design.md` Decision 7; spec `Agent Consumption Is Limited to
  Findings Through the Finding Source`). Depends on: 2.1, 3.1.
- [x] 4.2 Enforce no arbitrary Repository Evidence or application-state
  access — `Agent.invoke`'s own signature has no such parameter (spec
  `No Arbitrary Repository or Application Access`). Depends on: 4.1.
- [x] 4.3 Add a dependency-graph test proving `aip-ai` has no reachable
  dependency on `aip-findings`, `aip-rules`, `aip-analysis`, or
  `aip-csm-builder` (spec `Agent Consumption Is Limited to Findings
  Through the Finding Source`, both scenarios). Depends on: 4.1, 1.4.

## 5. Recommendation Domain/Artifact Model

- [x] 5.1 Define `Recommendation` as a final, non-generic `aip-ai`
  type: Artifact Identity, producing Agent identifier/version,
  referenced Finding's Evaluation Identity and Logical Finding
  Identity, Generation Identifier, opaque content, Confidence,
  Generation Provenance — no field representing a claimed mutation of
  any deterministic artifact (`design.md` Decision 2; spec
  `Recommendation Is a Distinct, Immutable Artifact From Finding`).
  Depends on: 2.1.
- [x] 5.2 Implement Recommendation as an artifact distinct from
  Finding — no shared representation, no relabeling; producing a
  Recommendation never alters the referenced Finding (spec, scenario:
  Producing a Recommendation does not alter the referenced Finding).
  Depends on: 5.1.
- [x] 5.3 Implement direct retention of the referenced Finding's
  Evaluation Identity and Logical Finding Identity — determinable
  without resolving the Finding first (spec `Recommendation References
  Its Source Finding by Both Identities`). Depends on: 5.1.
- [x] 5.4 Confirm `Recommendation` offers no operation that alters any
  field after construction (spec, scenario: No operation modifies an
  existing Recommendation). Depends on: 5.1.

## 6. No Intermediate AI Reasoning Artifact / Reasoning Isolation

- [x] 6.1 Confirm no separate, identified reasoning artifact type
  exists anywhere in `aip-ai` — an Agent invocation produces exactly
  one Recommendation as its only durable output (`design.md` Decision
  6; `define-agent-framework/design.md` Decision 1; spec `No
  Intermediate AI Reasoning Artifact`). Depends on: 5.1.
- [x] 6.2 Confirm `aip-ai`'s contracts expose no operation returning an
  Agent's internal reasoning/chain-of-thought content, and require no
  such content to be persisted (spec `Internal Reasoning Content Is
  Not Exposed or Persisted`). Depends on: 3.1.

## 7. Recommendation Artifact Identity

- [x] 7.1 Define `GenerationIdentifier` in `aip-ai`: an opaque token,
  minted via `generate()` using `UUID.randomUUID()` — the only
  non-deterministic call site in the entire codebase (`design.md`
  Decision 3; spec `Generation Identifier Uniqueness and
  Non-Content-Derivation`). Depends on: 1.1.
- [x] 7.2 Define `RecommendationArtifactIdentity` in `aip-ai`: a
  deterministic function of producing Agent identifier, Agent version,
  referenced Finding's Evaluation Identity, and Generation Identifier
  — never reading generated content, Confidence, or Generation
  Provenance (`design.md` Decision 3; spec `Recommendation Artifact
  Identity`, `Recommendation Artifact Identity Excludes Generated
  Content`). Depends on: 7.1.
- [x] 7.3 Add tests: identical identity components resolve to the same
  Artifact Identity; a differing Generation Identifier yields a
  distinguishable Artifact Identity, with everything else unchanged
  (spec `Recommendation Artifact Identity`, both scenarios). Depends
  on: 7.2.
- [x] 7.4 Add a test confirming Artifact Identity computation never
  reads generated content, Confidence, or Generation Provenance (spec
  `Recommendation Artifact Identity Excludes Generated Content`).
  Depends on: 7.2.
- [x] 7.5 Add tests confirming Generation Identifiers assigned across
  repeated invocations of the same Agent against the same Finding are
  distinct, and that assignment never depends on subsequently
  generated content (spec `Generation Identifier Uniqueness and
  Non-Content-Derivation`, both scenarios). Depends on: 7.1.
- [x] 7.6 Add a test confirming repeated retrieval of the same
  Recommendation Artifact Identity returns the identical Recommendation
  (including its generated content), while two separate invocations
  against the same Finding are never required to produce similar
  content (spec `Deterministic Recommendation Lookup, Not Deterministic
  Generation`, both scenarios). Depends on: 7.2, 14.1.

## 8. Generation Provenance

- [x] 8.1 Define `GenerationProvenance`: model/provider identifier and
  version, opaque generation configuration, generation timestamp
  (`design.md` Decision 5; spec `Generation Provenance`). Depends
  on: 1.1.
- [x] 8.2 Implement Generation Provenance as excluded from
  Recommendation Artifact Identity computation, while remaining fully
  inspectable directly on the Recommendation (spec, both scenarios).
  Depends on: 8.1, 7.2.

## 9. AI-Content Isolation

- [x] 9.1 Confirm no CSM element, `AnalysisResult`, `RuleEvaluationResult`,
  or `Finding` type carries prompt content, model/provider details, or
  generation configuration of any kind — structurally, by inspection
  of those already-implemented types (`design.md` Decision 5's own
  `GenerationProvenance` confinement; `define-agent-framework/design.md`
  Decision 10; spec `AI-Specific Content Isolation From Deterministic
  Artifacts`). Depends on: 8.1.
- [x] 9.2 Add a test confirming AI-specific content is confined to
  `GenerationProvenance` alone (spec, scenario: AI-specific content is
  confined to Generation Provenance). Depends on: 9.1.

## 10. Recommendation Confidence

- [x] 10.1 Implement Recommendation Confidence as an Agent-declared,
  mandatory `double` in `[0.0, 1.0]`, never computed, derived, or
  overridden by the framework (`design.md` Decision 4; spec
  `Recommendation Confidence Is Agent-Produced and Mandatory`).
  Depends on: 5.1.
- [x] 10.2 Confirm Recommendation Confidence is never copied from,
  constrained by, or required to match its referenced Finding's own
  Confidence value (spec `Recommendation Confidence Is Distinct From
  Finding Confidence`). Depends on: 10.1.
- [x] 10.3 Add tests: Confidence is present on every Recommendation;
  the framework never alters an Agent's declared value; Recommendation
  Confidence is independent of Finding Confidence (spec, all three
  scenarios across both Confidence requirements). Depends on: 10.1,
  10.2.

## 11. Recommendation Multiplicity and Invocation Independence

- [x] 11.1 Implement one Agent invocation as producing exactly one
  Recommendation (`define-agent-framework/design.md` Decision 5; spec
  `Recommendation Multiplicity — One Invocation, One Recommendation`).
  Depends on: 5.1, 7.2.
- [x] 11.2 Confirm a Finding may be referenced by any number of
  independently-retained Recommendations, from the same Agent invoked
  more than once or from different registered Agents, with no
  conflict/precedence/ranking/deduplication mechanism anywhere in
  `aip-ai` (spec `No Conflict or Precedence Semantics Among
  Recommendations`, both scenarios). Depends on: 11.1.
- [x] 11.3 Confirm Agent invocations share no mutable state and depend
  on no declared ordering — `RecommendationConstructor`'s own signature
  takes only `(Agent, Finding)`, with no other-invocation-referencing
  parameter (`define-agent-framework/design.md` Decision 5's
  corollary; spec `Agent Invocation Independence`, both scenarios).
  Depends on: 11.1.

## 12. Recommendation Validation

- [x] 12.1 Implement `RecommendationValidator`, checking: the
  referenced Finding exists and is resolvable via `FindingSource.read`;
  the declared Agent identifier/version corresponds to a currently
  registered Agent; Generation Provenance is present with a non-blank
  model/provider identifier and a non-null timestamp; Confidence is
  within `[0.0, 1.0]`; content is present (`design.md` Decision 8;
  spec `Recommendation Validation Before Publication`). Depends
  on: 5.1, 3.3, 2.1, 7.2, 8.1, 10.1.
- [x] 12.2 Enforce that `RecommendationValidator` never re-verifies the
  referenced Finding's own upstream chain — trusting `FindingValidator`'s
  own already-completed work (`design.md` Decision 8's trust-boundary
  reasoning). Depends on: 12.1.
- [x] 12.3 Implement the isolation-from-deterministic-facts check —
  structurally guaranteed by `Recommendation`'s own field list carrying
  no CSM/AnalysisResult/RuleEvaluationResult/Finding-mutation-claiming
  field (spec, isolation clause). Depends on: 5.1.
- [x] 12.4 Add tests for all four validation scenarios: valid
  Recommendation published; Recommendation referencing an unpublished
  Finding rejected; Recommendation from an unregistered Agent/version
  rejected; Recommendation with incomplete Generation Provenance
  rejected; Recommendation missing Confidence/content rejected (spec
  `Recommendation Validation Before Publication`, all scenarios).
  Depends on: 12.1.
- [x] 12.5 Add tests confirming validation never determines or claims
  substantive correctness — a well-formed but substantively
  questionable Recommendation still passes, and a low-Confidence
  Recommendation is never rejected on that basis alone (spec
  `Validation Does Not Determine Substantive Correctness`, both
  scenarios). Depends on: 12.1.

## 13. Recommendation Publishing

- [x] 13.1 Implement `RecommendationPublisher`, calling
  `RecommendationStore.write` only for a Recommendation that passes
  `RecommendationValidator` (`design.md` Decision 8; spec
  `Recommendation Publishing`). Depends on: 12.1, 14.1.
- [x] 13.2 Add a test confirming a Recommendation that fails validation
  is never published. Depends on: 13.1.

## 14. Recommendation Persistence Abstraction

- [x] 14.1 Define the `RecommendationStore` abstraction (interface
  only; no concrete implementation) in `aip-ai`, not `aip-core`
  (`design.md` Decision 8; `define-agent-framework/design.md`
  Decision 8; spec `Recommendations Are Durable, Individually
  Identifiable Artifacts`). Depends on: 5.1, 7.2.
- [x] 14.2 Add tests confirming a Recommendation remains retrievable by
  its Artifact Identity after the producing invocation's process has
  exited, and that a new, differently-identified Recommendation never
  overwrites a prior one (spec, both scenarios). Depends on: 14.1.

## 15. Deterministic-Layer Protection

- [x] 15.1 Add tests confirming producing, validating, or publishing a
  Recommendation never alters CSM content, Analysis Results,
  RuleEvaluationResults, Findings, or Rule decisions (spec
  `Deterministic-Layer Protection`). Depends on: 5.2, 13.1.

## 16. Output Boundary Enforcement

- [x] 16.1 Confirm `aip-ai`'s only durable output type is
  `Recommendation` — no Proposed Change, code-generation, or
  remediation-artifact type exists anywhere in the module (`design.md`
  Decision "Output boundary"; `define-agent-framework/design.md`
  Decision 9; spec `Output Boundary Excludes Remediation and Beyond`).
  Depends on: 5.1.

## 17. Failure Semantics

- [x] 17.1 Implement generation-failure propagation as "no
  Recommendation is produced" — `RecommendationConstructor` catches an
  `Agent.invoke` failure and returns `Optional.empty()`, never a
  partially-constructed or invalid Recommendation (`design.md`
  Decision 6; spec `Failure Semantics`, scenario: Generation failure
  produces no Recommendation). Depends on: 3.1, 5.1.
- [x] 17.2 Confirm a missing or invalid Finding reference fails
  validation and is never published (spec, scenario: Missing or
  invalid Finding reference prevents publication — covered jointly
  with Section 12.4's rejection scenarios). Depends on: 12.4.
- [x] 17.3 Add a test exercising a fixture Agent that throws during
  `invoke`, confirming `RecommendationConstructor` produces no
  Recommendation for that invocation. Depends on: 17.1.

## 18. Dependency-Boundary and Architectural-Invariant Tests

- [x] 18.1 Add a dependency-graph test proving `aip-ai` has no
  reachable dependency on `aip-findings`, `aip-rules`, `aip-analysis`,
  or `aip-csm-builder`, and confirming no deterministic module depends
  on `aip-ai` (`design.md` Decision 9). Depends on: 1.4.
- [x] 18.2 Add a test confirming `aip-ai` consumes Findings exclusively
  through `FindingSource` — never through a direct dependency on the
  producing module. Depends on: 18.1, 2.1.
- [x] 18.3 Add a test confirming a Recommendation is never exposed as
  CSM content, an Analysis Result, a RuleEvaluationResult, or a
  Finding, and vice versa (structural, disjoint-type check, mirroring
  every prior layer's own such test). Depends on: 5.1.
- [x] 18.4 Add a test confirming every Recommendation concerns exactly
  one repository's CSM Snapshot, inherited from its referenced Finding
  (spec `Single-Repository Finding Consumption`). Depends on: 5.3.

## 19. Fixture/Test Infrastructure

- [x] 19.1 Build `InMemoryFindingSource`, satisfying the Section 2
  contract, mirroring `InMemoryRuleEvaluationResultSource`. Depends
  on: 2.2.
- [x] 19.2 Build `InMemoryRecommendationStore`, satisfying the Section
  14 abstraction. Depends on: 14.1.
- [x] 19.3 Build `StubAgent`, a configurable `Agent` (fixed success
  output, or a failure mode), for use across test classes. Depends
  on: 3.1.

## 20. Requirement/Scenario Traceability and End-to-End Verification

- [x] 20.1 Build a traceability matrix mapping each of
  `define-agent-framework/specs/agent-framework/spec.md`'s
  requirements and scenarios to the task(s)/test(s) that implement/
  verify it, following `implement-finding-model`'s own
  `traceability.md` precedent. Depends on: Sections 1–19.
- [x] 20.2 Confirm every one of this change's own `design.md`'s 10
  Decisions, and every one of `define-agent-framework/design.md`'s 11
  Decisions, has at least one corresponding implementation task and
  test above; record any gap found rather than silently leaving it
  uncovered. Depends on: 20.1.
- [x] 20.3 Add an end-to-end fixture-based test: invoke a fixture Agent
  against a fixture Finding, construct/validate/publish the resulting
  Recommendation, retrieve it by Artifact Identity, and confirm a
  second invocation against the same Finding produces a second,
  independently-identified Recommendation; also confirm a failing
  fixture Agent invocation produces no Recommendation in the same run.
  Depends on: 13.1, 14.2, 17.3.
- [x] 20.4 Run full validation (`openspec validate agent-framework
  --strict` once specs are synced, or the equivalent check available
  during implementation) and record the result in this change's
  verification artifact. Depends on: 20.1, 20.2.
