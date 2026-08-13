# Requirement/Scenario Traceability Matrix

tasks.md 20.1: every requirement and scenario in
`openspec/changes/define-agent-framework/specs/agent-framework/spec.md`
(27 requirements, ~50 scenarios), mapped to the automated test(s)
exercising it. Test references are `ClassName.methodName`; unqualified
class names live in `aip-ai/src/test/java/aip/ai/...` unless marked
`(aip-core)`, which live in `aip-core/src/test/java/aip/core/csm/...`.
Where a requirement is structurally guaranteed rather than actively
tested, that is stated explicitly rather than pointing at a test that
would always trivially pass.

## Agent Framework Specifies the Generic Framework Only

| Scenario | Test(s) |
|---|---|
| No specific agent's reasoning logic is specified | Structural: `Agent`'s own interface has no method constraining reasoning, prompt, or output-content logic — only `identifier()`, `version()`, `invoke(Finding)`. No specific agent (Architecture Compliance or otherwise) is implemented anywhere in this change. |

## Agent Contract

| Scenario | Test(s) |
|---|---|
| Agent declares identifier and version | `AgentRegistryTest.registersAndLooksUpByIdentifier` |
| Agent invocation consumes one Finding and produces one Recommendation | `RecommendationConstructorTest.oneInvocationProducesExactlyOneRecommendation` |

## Agent Consumption Is Limited to Findings Through the Finding Source

| Scenario | Test(s) |
|---|---|
| Finding is obtained only through the Finding Source | `DependencyAndBoundaryTest.agentReadsOnlyFromAFindingItIsGivenDirectly`; `check-no-aip-findings-adapter` CI guard |
| No direct access to CSM, Analysis Results, or RuleEvaluationResults | Architecturally guaranteed: `Agent.invoke`'s own signature accepts only a `Finding` — no `AnalysisView`, `CsmSnapshotSource`, `AnalysisResultSource`, or `RuleEvaluationResultSource` parameter exists; `aip-ai`'s dependency graph permits only `aip-core`. |

## No Arbitrary Repository or Application Access

| Scenario | Test(s) |
|---|---|
| No Repository Evidence or arbitrary application access | Architecturally guaranteed: `Agent.invoke`'s signature has no such parameter; `check-no-aip-csmbuilder-adapter` CI guard (the only module that could reach Repository Evidence). |

## Finding Source Shape

| Scenario | Test(s) |
|---|---|
| Finding Source exposes retrieval by Evaluation Identity and by Logical Finding Identity or snapshot | `FindingSourceTest` (aip-core, both tests) |
| Agent Framework is agnostic to Finding production | `FindingSourceTest.agnosticToFindingProduction` (aip-core) |

## No Intermediate AI Reasoning Artifact

| Scenario | Test(s) |
|---|---|
| No separate reasoning artifact is produced | `DependencyAndBoundaryTest.noSeparateReasoningArtifactExists`; structurally, no reasoning-artifact type exists anywhere in `aip-ai`. |

## Internal Reasoning Content Is Not Exposed or Persisted

| Scenario | Test(s) |
|---|---|
| Chain-of-thought is neither required nor exposed | Structural: `AgentInvocationResult` and `Recommendation` expose only `content`/`confidence`/`provenance` fields — no reasoning-content field or accessor exists on any `aip-ai` type. |

## Recommendation Is a Distinct, Immutable Artifact From Finding

| Scenario | Test(s) |
|---|---|
| Producing a Recommendation does not alter the referenced Finding | `DependencyAndBoundaryTest.producingARecommendationDoesNotAlterTheReferencedFinding` |
| No operation modifies an existing Recommendation | Architecturally guaranteed: `Recommendation` (aip-ai) exposes only accessor methods — no setter, no mutator, all fields `final`. |

## Recommendation References Its Source Finding by Both Identities

| Scenario | Test(s) |
|---|---|
| Both Finding identities are directly retained | `RecommendationTest.retainsBothFindingIdentitiesDirectly` |

## Recommendation Multiplicity — One Invocation, One Recommendation

| Scenario | Test(s) |
|---|---|
| One invocation produces exactly one Recommendation | `RecommendationConstructorTest.oneInvocationProducesExactlyOneRecommendation` |
| A Finding may be referenced by multiple Recommendations | `RecommendationConstructorTest.aFindingMayBeReferencedByMultipleRecommendationsFromDifferentAgents`; `AgentFrameworkFixtureEndToEndTest` (two Recommendations from the same Agent, both retained) |

## No Conflict or Precedence Semantics Among Recommendations

| Scenario | Test(s) |
|---|---|
| No Recommendation is marked as primary or authoritative | Structural: no ranking/priority/primary field exists anywhere in `Recommendation` or `RecommendationStore`. |
| Disagreeing Recommendations are both retained | `AgentFrameworkFixtureEndToEndTest` (two independently-produced Recommendations for the same Finding, both published and independently retrievable) |

## Recommendation Artifact Identity

| Scenario | Test(s) |
|---|---|
| Same invocation identity components resolve to the same Recommendation Artifact Identity | `RecommendationArtifactIdentityTest.sameIdentityComponentsResolveToTheSameArtifactIdentity` |
| A different Generation Identifier yields a distinguishable identity | `RecommendationArtifactIdentityTest.differentGenerationIdentifierYieldsDistinguishableIdentity` |

## Recommendation Artifact Identity Excludes Generated Content

| Scenario | Test(s) |
|---|---|
| Identity computation does not read generated content | Architecturally guaranteed: `RecommendationArtifactIdentity.of`'s own signature accepts only `(agentIdentifier, agentVersion, findingEvaluationIdentity, generationIdentifier)` — no content, Confidence, or provenance parameter exists for it to read. |

## Generation Identifier Uniqueness and Non-Content-Derivation

| Scenario | Test(s) |
|---|---|
| Generation Identifier is unique per invocation | `GenerationIdentifierTest.generateProducesDistinctTokensAcrossCalls`; `RecommendationConstructorTest.repeatedInvocationsAgainstTheSameFindingProduceIndependentlyIdentifiedRecommendations` |
| Generation Identifier is not derived from generated content | Architecturally guaranteed: `RecommendationConstructor` calls `GenerationIdentifier.generate()` **before** invoking `Agent.invoke` — the token exists prior to any content being generated. |

## Deterministic Recommendation Lookup, Not Deterministic Generation

| Scenario | Test(s) |
|---|---|
| Same invocation identity resolves to the same artifact, not necessarily the same content across separate invocations | `RecommendationConstructorTest.repeatedInvocationsAgainstTheSameFindingProduceIndependentlyIdentifiedRecommendations` |
| Repeated retrieval of the same Recommendation is identical | `RecommendationPublisherTest.validRecommendationIsPublishedAndRetrievable` (retrieval returns the identical, previously-constructed Recommendation, including its generated content) |

## Recommendation Content Is Agent-Defined and Opaque to the Framework

| Scenario | Test(s) |
|---|---|
| Differently-shaped guidance content from different Agents is accepted | `RecommendationTest.differentlyShapedContentFromDifferentAgentsIsBothAccepted` |

## Recommendation Confidence Is Agent-Produced and Mandatory

| Scenario | Test(s) |
|---|---|
| Confidence is present on every Recommendation | `AgentInvocationResult`'s own constructor requires it (aip-ai); `RecommendationConstructorTest.confidenceIsPreservedFromTheAgentsOwnDeclaration` |
| Framework does not compute or override Confidence | `RecommendationConstructorTest.confidenceIsPreservedFromTheAgentsOwnDeclaration`; architecturally guaranteed by `RecommendationConstructor`'s own logic never computing a Confidence value, only forwarding `AgentInvocationResult.confidence()`. |

## Recommendation Confidence Is Distinct From Finding Confidence

| Scenario | Test(s) |
|---|---|
| Recommendation Confidence is independent of Finding Confidence | `RecommendationConstructorTest.confidenceIsPreservedFromTheAgentsOwnDeclaration` (Recommendation's `double` Confidence is structurally distinct from Finding's `Confidence` enum — different types entirely, never compared or constrained against each other anywhere in `aip-ai`). |

## Generation Provenance

| Scenario | Test(s) |
|---|---|
| Generation Provenance is present on every Recommendation | `RecommendationValidatorTest.validRecommendationPassesValidation`; `Recommendation`'s own constructor requires a non-null `GenerationProvenance` |
| Generation Provenance remains fully inspectable independent of identity computation | `Recommendation.provenance()`'s own accessor (aip-ai) — directly readable, no recomputation needed |

## AI-Specific Content Isolation From Deterministic Artifacts

| Scenario | Test(s) |
|---|---|
| No AI-specific content appears in a deterministic artifact | Structural: `Finding`, `RuleEvaluationResult`, `AnalysisResult`, and every CSM element/relationship type (all in `aip-core`, unmodified by this change) have no prompt/model/generation-configuration field. |
| AI-specific content is confined to Generation Provenance | `GenerationProvenance`'s own field list (aip-ai) is the sole location for `modelProviderIdentifier`/`modelVersion`/`generationConfiguration`. |

## Recommendation Validation Before Publication

| Scenario | Test(s) |
|---|---|
| Valid Recommendation is published as usable output | `RecommendationValidatorTest.validRecommendationPassesValidation`, `RecommendationPublisherTest.validRecommendationIsPublishedAndRetrievable` |
| Recommendation referencing an unpublished Finding is rejected | `RecommendationValidatorTest.recommendationReferencingAnUnpublishedFindingIsRejected` |
| Recommendation from an unregistered Agent/version is rejected | `RecommendationValidatorTest.recommendationFromAnUnregisteredAgentIsRejected`, `RecommendationValidatorTest.recommendationFromAnUnregisteredAgentVersionIsRejected` |
| Recommendation with incomplete Generation Provenance is rejected | `RecommendationValidatorTest.recommendationWithIncompleteGenerationProvenanceIsRejected` |
| Recommendation missing Confidence or content is rejected | `AgentInvocationResult`'s and `Recommendation`'s own constructors structurally guarantee Confidence/content presence at construction time; `RecommendationValidator`'s own range/non-null checks are defense-in-depth for the same properties. |

## Validation Does Not Determine Substantive Correctness

| Scenario | Test(s) |
|---|---|
| A well-formed but substantively questionable Recommendation still passes validation | `RecommendationValidatorTest.wellFormedButSubstantivelyQuestionableRecommendationStillPassesValidation` |
| Validation does not gate on Confidence value | `RecommendationValidatorTest.wellFormedButSubstantivelyQuestionableRecommendationStillPassesValidation` (deliberately uses a Confidence of `0.01`) |

## Recommendation Publishing

| Scenario | Test(s) |
|---|---|
| Only validated Recommendations are written to the persistence mechanism | `RecommendationPublisherTest.invalidRecommendationIsNeverPublished` |

## Recommendations Are Durable, Individually Identifiable Artifacts

| Scenario | Test(s) |
|---|---|
| Recommendation remains retrievable after the producing invocation ends | `RecommendationPublisherTest.validRecommendationIsPublishedAndRetrievable` |
| A new Recommendation does not overwrite a prior, differently-identified one | `InMemoryRecommendationStore.write`'s own overwrite guard, exercised implicitly by `AgentFrameworkFixtureEndToEndTest` (two Recommendations written in the same run); the interface itself (`RecommendationStore`) documents the "never overwrites" contract a conforming implementation must satisfy. |

## Agent Registration and Extension Behavior

| Scenario | Test(s) |
|---|---|
| A new Agent is registered without core mechanism changes | `ExtensionMechanismTest.aStructurallyDistinctSecondAgentUsesTheSameCoreMechanismsUnmodified` |
| Registering a new Agent requires no change to deterministic capabilities | Architecturally guaranteed: `aip-ai`'s dependency graph has no reachable dependency on `aip-csm-builder`, `aip-analysis`, `aip-rules`, or `aip-findings` — registering an Agent involves no code path that could touch those modules' own specifications or implementations. |

## Output Boundary Excludes Remediation and Beyond

| Scenario | Test(s) |
|---|---|
| No Proposed Change or remediation artifact is produced | Structural: `aip-ai` defines exactly one durable output type, `Recommendation` — no Proposed Change, code-generation, or remediation-artifact type exists anywhere in the module. |

## Deterministic-Layer Protection

| Scenario | Test(s) |
|---|---|
| No deterministic artifact is altered by Recommendation production | `DependencyAndBoundaryTest.producingARecommendationDoesNotAlterTheReferencedFinding`; structurally, `aip-ai` has no write access to any deterministic module's store (no dependency on `aip-findings`/`aip-rules`/`aip-analysis`/`aip-csm-builder` at all). |

## Agent Invocation Independence

| Scenario | Test(s) |
|---|---|
| Recommendation is unaffected by which other invocations occurred | `RecommendationConstructorTest.aFindingMayBeReferencedByMultipleRecommendationsFromDifferentAgents` (each invocation's Recommendation is independently computed from only its own `(Agent, Finding)` pair) |
| No declared dependency between Agent invocations is honored | Architecturally guaranteed: `RecommendationConstructor.construct`'s own signature takes only `(Agent, Finding)` — no other-invocation-referencing parameter or shared mutable state exists anywhere in `aip-ai`. |

## Failure Semantics

| Scenario | Test(s) |
|---|---|
| Missing or invalid Finding reference prevents publication | `RecommendationValidatorTest.recommendationReferencingAnUnpublishedFindingIsRejected` |
| Generation failure produces no Recommendation | `RecommendationConstructorTest.failingAgentInvocationProducesNoRecommendation`; `AgentFrameworkFixtureEndToEndTest` (a failing fixture Agent's invocation produces `Optional.empty()`) |

## Single-Repository Finding Consumption

| Scenario | Test(s) |
|---|---|
| A Recommendation concerns exactly one repository through its referenced Finding | `DependencyAndBoundaryTest.everyRecommendationConcernsExactlyOneRepositoryThroughItsReferencedFinding` |

## Binding Decisions (this change's own `design.md`)

| Decision | Verification |
|---|---|
| 1. `FindingSource` contract shape | `FindingSourceTest` (aip-core) |
| 2. `Recommendation` shape (final, non-generic, `aip-ai`-local) | `RecommendationTest` |
| 3. `RecommendationArtifactIdentity`/`GenerationIdentifier`, the one legitimate randomness call site | `RecommendationArtifactIdentityTest`, `GenerationIdentifierTest` |
| 4. Recommendation Confidence as a `double` in `[0.0, 1.0]` | `RecommendationTest.rejectsConfidenceOutsideValidRange`; `AgentInvocationResult`'s own range check |
| 5. `GenerationProvenance` shape | `RecommendationValidatorTest` (Generation Provenance completeness checks) |
| 6. `RecommendationConstructor`, failure semantics | `RecommendationConstructorTest.failingAgentInvocationProducesNoRecommendation` |
| 7. `Agent`/`AgentInvocationResult` contract shape | `AgentRegistryTest`, `RecommendationConstructorTest` |
| 8. `AgentRegistry`/`RecommendationStore`/`RecommendationValidator`/`RecommendationPublisher` | `AgentRegistryTest`, `RecommendationValidatorTest`, `RecommendationPublisherTest` |
| 9. Module scaffolding, `check-no-ai-heuristic-imports.sh` deliberately not wired | `check-module-dependencies`, `check-no-aip-findings-adapter`, `check-no-aip-rules-adapter`, `check-no-aip-analysis-adapter`, `check-no-aip-csmbuilder-adapter` CI guard executions; confirmed by inspection that `check-no-ai-heuristic-imports.sh` is absent from `aip-ai/pom.xml` |
| 10. Fixture layer (no CSM-graph builder needed) | `FindingFixtures`, `InMemoryFindingSource`, `InMemoryRecommendationStore`, `StubAgent` (test-only); `check-fixture-package-scope` CI guard |
