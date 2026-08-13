# Requirement/Scenario Traceability Matrix

tasks.md 18.1: every requirement and scenario in
`openspec/changes/define-finding-model/specs/finding-model/spec.md`
(25 requirements, 52 scenarios), mapped to the automated test(s)
exercising it. Test references are `ClassName.methodName`; unqualified
class names live in `aip-findings/src/test/java/aip/findings/...`
unless marked `(aip-core)`, which live in
`aip-core/src/test/java/aip/core/csm/...`. Where a requirement is
structurally guaranteed rather than actively tested (e.g. by Java's
type system, or by this module's dependency graph), that is stated
explicitly rather than pointing at a test that would always trivially
pass.

## Deterministic, Declarative Finding Construction Only

| Scenario | Test(s) |
|---|---|
| No AI or heuristic judgment used to construct a Finding | Architecturally guaranteed: `aip-findings`'s dependency graph (`check-module-dependencies.sh`) permits only `aip-core`; `check-no-ai-heuristic-imports.sh` confirms no randomness/AI-SDK import exists in production source; `FindingConstructor` reads only its input `RuleEvaluationResult`'s own fields. |
| Repeated construction over unchanged input is identical | `FindingConstructorTest.repeatedConstructionOverUnchangedInputIsIdentical` |

## Finding Provenance Requires At Least One RuleEvaluationResult

| Scenario | Test(s) |
|---|---|
| Finding construction requires at least one RuleEvaluationResult | `FindingTest.rejectsEmptyReferencedRuleEvaluationResultSet` (aip-core); `FindingConstructor.construct` always populates a singleton set from its single input Result |
| No mechanism exists for provenance-independent Findings | Architecturally guaranteed: `FindingConstructor`'s only entry point (`construct`) takes a `RuleEvaluationResult` as its sole argument — no overload or alternate path constructs a Finding from anything else. |

## Finding Is a Distinct Artifact From RuleEvaluationResult

| Scenario | Test(s) |
|---|---|
| A RuleEvaluationResult is not itself a Finding | `DependencyAndBoundaryTest.findingIsNeverExposedAsCsmContentOrAnalysisOrRuleEvaluationResult`; `DependencyAndBoundaryTest.findingsAreStoredSeparatelyFromRuleEvaluationResults` |

## Finding-to-RuleEvaluationResult Reference Set and V1 Multiplicity

| Scenario | Test(s) |
|---|---|
| Reference set shape supports multiple RuleEvaluationResults | `Finding`'s `referencedRuleEvaluationResultIds()` is `Set<RuleEvaluationResultId>`, structurally unbounded (aip-core) |
| V1 construction produces exactly one reference per Finding | `FindingConstructorTest.constructedFindingReferencesExactlyTheSourceResult`; `DependencyAndBoundaryTest.noAggregationOccursAcrossMultipleQualifyingResults` |

## Evaluation Identity

| Scenario | Test(s) |
|---|---|
| Identical RuleEvaluationResult sets produce identical Evaluation Identity | `EvaluationIdentityTest.identicalReferencedSetsProduceIdenticalIdentity` (aip-core) |
| Different RuleEvaluationResult sets produce different Evaluation Identity | `EvaluationIdentityTest.differentReferencedSetsProduceDistinguishableIdentity` (aip-core) |

## Logical Finding Identity

| Scenario | Test(s) |
|---|---|
| Logical Finding Identity excludes source CSM Snapshot identity | `FindingConstructorTest.changedOutcomeAcrossRunsSharesLogicalFindingIdentityButDiffersInEvaluationIdentity`; `LogicalFindingIdentityTest.sameRuleAndElementAcrossTwoSnapshotsYieldsTheSameIdentity` (aip-core) |
| Logical Finding Identity excludes Rule version | `LogicalFindingIdentityTest.ruleVersionIsNotAnInputSoDifferingVersionsYieldTheSameIdentity` (aip-core) |
| Different concerned CSM elements yield different Logical Finding Identity | `LogicalFindingIdentityTest.differentConcernedElementsYieldDistinguishableIdentity` (aip-core); `FindingConstructorTest.differentConcernedElementsYieldDifferentLogicalFindingIdentity` |
| Unanchored Rule Scope concerns the Repository element | `FindingConstructorTest.unanchoredResultResolvesConcernedElementToTheSynthesizedRepositorySubject`; `ConcernedElementResolverTest.unanchoredScopeInstanceResolvesToADeterministicSyntheticSubjectDerivedFromTheRepositoryIdentifier` — see `implement-finding-model/design.md` Decision 5 for the named, accepted deviation from the literal `aip-csm-builder`-constructed Repository element identity |
| A relationship-reading Rule Type resolves to its anchor element, not a relationship identity | Architecturally guaranteed: `ConcernedElementResolver.resolve` reads only `CsmScopeInstance.anchorElementId()`, never any relationship identity or payload content; `FindingConstructorTest.anchoredResultResolvesConcernedElementToItsAnchor` |

## Evaluation Identity and Logical Finding Identity Are Distinct

| Scenario | Test(s) |
|---|---|
| Both identities are independently present on a Finding | `FindingTest.ofComputesBothIdentitiesFromTheSameTraceabilityComponents` (aip-core) — `EvaluationIdentity` and `LogicalFindingIdentity` are distinct record types, structurally non-interchangeable |
| Same Logical Finding Identity, different Evaluation Identity, across two runs | `FindingConstructorTest.changedOutcomeAcrossRunsSharesLogicalFindingIdentityButDiffersInEvaluationIdentity` |

## Cross-Snapshot Comparison via Logical Finding Identity

| Scenario | Test(s) |
|---|---|
| Logical Finding Identity presence is comparable across two runs | `FindingConstructorTest.changedOutcomeAcrossRunsSharesLogicalFindingIdentityButDiffersInEvaluationIdentity` demonstrates two independently-constructed Findings' Logical Finding Identities are directly `.equals()`-comparable `record` values |
| Finding Model does not itself label a comparison as resolved or new | Architecturally guaranteed: `Finding`, `FindingConstructor`, `FindingValidator`, and `FindingPublisher` expose no lifecycle-state field, method, or return value anywhere in `aip-findings`. |

## Finding Immutability

| Scenario | Test(s) |
|---|---|
| No operation modifies an existing Finding | Architecturally guaranteed: `Finding` (aip-core) exposes only accessor methods — no setter, no mutator, all fields `final`. |
| A changed outcome produces a new Finding, not a mutation | `FindingConstructorTest.changedOutcomeAcrossRunsSharesLogicalFindingIdentityButDiffersInEvaluationIdentity` |

## No Mutable Lifecycle State or Lifecycle Event Artifact in This Version

| Scenario | Test(s) |
|---|---|
| Finding carries no mutable lifecycle-state field | Structural: `Finding`'s field list (aip-core) has no lifecycle-state field of any kind. |
| No lifecycle-event artifact is produced | Structural: no lifecycle-event/transition type exists anywhere in `aip-findings` or `aip-core`. |

## Finding Lifecycle Presentation Is Out of Scope

| Scenario | Test(s) |
|---|---|
| No lifecycle presentation mechanism is provided | Structural: `aip-findings` exposes only `FindingConstructor`, `FindingValidator`, `FindingPublisher`, `FindingStore` — no query, view, or dashboard type. |

## Finding Shape

| Scenario | Test(s) |
|---|---|
| A constructed Finding carries the complete v1 field set | `FindingTest.retainsTraceabilityToProducingRuleSourceSnapshotAndConcernedElement`, `FindingTest.categorySeverityDescriptionAndImpactAreRetainedUnmodified` (aip-core) |
| Confidence is fixed for every v1 Finding | `FindingConstructorTest.confidenceIsAlwaysFixedToHigh`; `FindingTest.confidenceIsRetainedAsProvided` (aip-core) |
| Recommendation and remediation availability are not populated | Structural: `Finding` (aip-core) has no such field at all — not defaulted, absent by construction; `FindingConstructorTest.constructedFindingCopiesCategorySeverityDescriptionAndImpactUnmodified` exercises the complete populated field set, confirming nothing else exists. |

## Severity and Category Are Rule-Type-Declared Configuration

| Scenario | Test(s) |
|---|---|
| Severity and Category are copied from Rule Type configuration | `FindingConstructorTest.constructedFindingCopiesCategorySeverityDescriptionAndImpactUnmodified` |
| Finding Model does not compute Severity from context | Architecturally guaranteed: `FindingConstructor.construct` reads `category()`/`severity()` directly from the payload's `FindingMetadata` implementation, with no other computation path; `ExtensionMechanismTest` confirms two structurally distinct Category/Severity pairs both pass through unmodified. |

## Finding Evidence Means Traceability Content, Not Repository Evidence

| Scenario | Test(s) |
|---|---|
| Finding Evidence references CSM provenance, not raw Repository Evidence | Architecturally guaranteed: `Finding`'s Evidence-bearing fields are `concernedElementId` (a `CsmElementId`) and `referencedRuleEvaluationResultIds` — no Repository Evidence type is reachable from `aip-findings`'s dependency graph at all. |
| Finding Model does not read Repository Evidence | `DependencyAndBoundaryTest.findingModelReadsOnlyFromARuleEvaluationResultSource`; `check-no-aip-csmbuilder-adapter` CI guard (the only module that could reach Repository Evidence). |

## Finding Traceability

| Scenario | Test(s) |
|---|---|
| Source CSM Snapshot identity is directly retained | `FindingTest.retainsTraceabilityToProducingRuleSourceSnapshotAndConcernedElement` (aip-core) |
| Concerned CSM element identity is directly retained | `FindingTest.retainsTraceabilityToProducingRuleSourceSnapshotAndConcernedElement` (aip-core) |
| Producing Rule and consumed RuleEvaluationResults are traceable | `FindingTest.retainsTraceabilityToProducingRuleSourceSnapshotAndConcernedElement` (aip-core) |

## Aggregation Is Not Performed in This Version

| Scenario | Test(s) |
|---|---|
| No grouping of multiple RuleEvaluationResults occurs | `DependencyAndBoundaryTest.noAggregationOccursAcrossMultipleQualifyingResults`; `FindingModelFixtureEndToEndTest.constructsValidatesAndPublishesFindingsForOnlyTheQualifyingResults` |

## Finding Model Has No Scope Concept

| Scenario | Test(s) |
|---|---|
| Finding Model requires no Scope declaration to operate | `DependencyAndBoundaryTest.findingConstructionRequiresNoScopeDeclarationOfAnyKind`; structurally, no Scope-declaring type exists anywhere in `aip-findings`. |
| Finding targeting is determined by RuleEvaluationResult content alone | `ConcernedElementResolverTest` (all three tests); `DependencyAndBoundaryTest.findingConstructionRequiresNoScopeDeclarationOfAnyKind` |

## Finding Validation Before Publication

| Scenario | Test(s) |
|---|---|
| Valid Finding is published as usable output | `FindingValidatorTest.validFindingPassesValidation`; `FindingPublisherTest.validFindingIsPublishedAndRetrievable` |
| Finding referencing an unpublished RuleEvaluationResult is rejected | `FindingValidatorTest.findingReferencingAnUnavailableRuleEvaluationResultIsRejected` |
| Finding with an empty RuleEvaluationResult reference set is rejected | `FindingTest.rejectsEmptyReferencedRuleEvaluationResultSet` (aip-core, structurally guaranteed by `Finding`'s own constructor) plus `FindingValidatorTest.findingWithAnEmptyReferenceSetIsRejected` (validator-level defense in depth against a dangling reference) |
| Finding with inconsistent identity is rejected | `FindingValidatorTest.findingWithInconsistentIdentityIsRejected`, `FindingValidatorTest.findingDeclaringAMismatchedConcernedElementIsRejected`, `FindingValidatorTest.findingDeclaringAMismatchedSourceSnapshotIsRejected` |
| Validation does not re-verify the upstream Analysis-to-Rule chain | `FindingValidator`'s own logic never calls into `AnalysisResultSource` or reads a `RuleEvaluationResult`'s own `consumedAnalysisResultIds()` — architecturally guaranteed by inspection; `aip-findings` has no dependency on `aip-rules`'s `RuleEvaluationResultValidator` at all. |

## Findings Are Durable, Individually Identifiable Artifacts

| Scenario | Test(s) |
|---|---|
| Finding remains retrievable after the producing run ends | `FindingPublisherTest.validFindingIsPublishedAndRetrievable` |
| A new Finding does not overwrite a prior, differently-identified one | `InMemoryFindingStore.write`'s own overwrite guard, exercised implicitly by every test writing more than one Finding (e.g. `FindingModelFixtureEndToEndTest`); the interface itself (`FindingStore`) documents the "never overwrites" contract a conforming implementation must satisfy. |

## Rule Evaluation Result Source Shape

| Scenario | Test(s) |
|---|---|
| Rule Evaluation Result Source exposes retrieval by identity and by Rule/snapshot | `RuleEvaluationResultSourceTest` (aip-core, both tests) |
| Finding Model is agnostic to RuleEvaluationResult production | `RuleEvaluationResultSourceTest.agnosticToRuleEvaluationResultProduction` (aip-core) |

## Rule Evaluation Result Consumption Through the Rule Evaluation Result Source Only

| Scenario | Test(s) |
|---|---|
| RuleEvaluationResults are read only through the Rule Evaluation Result Source | `DependencyAndBoundaryTest.findingModelReadsOnlyFromARuleEvaluationResultSource`; `check-no-aip-rules-adapter` CI guard |

## CSM and Rule Evaluation Content Reached Only Through Established Contracts

| Scenario | Test(s) |
|---|---|
| No direct Repository Evidence or Runtime Model dependency | `check-no-aip-csmbuilder-adapter` CI guard; `aip-findings`'s dependency graph (`check-module-dependencies.sh`) permits only `aip-core`. |
| No dependency on the producing module | `check-no-aip-rules-adapter`, `check-no-aip-analysis-adapter`, `check-no-aip-csmbuilder-adapter` CI guards (all three producing modules across the full pipeline). |

## Findings Are a Distinct Concept From CSM Knowledge, Analysis Results, and Rule Evaluation Results

| Scenario | Test(s) |
|---|---|
| Producing a Finding does not alter upstream content | `DependencyAndBoundaryTest.producingAFindingDoesNotAlterUpstreamContent` |
| Findings are stored separately from upstream content | `DependencyAndBoundaryTest.findingsAreStoredSeparatelyFromRuleEvaluationResults`; `DependencyAndBoundaryTest.findingIsNeverExposedAsCsmContentOrAnalysisOrRuleEvaluationResult` |

## Single-Repository Finding Scope

| Scenario | Test(s) |
|---|---|
| One construction run covers one repository's snapshot | `DependencyAndBoundaryTest.everyFindingFromOneConstructionRunReferencesExactlyOneRepositorysSnapshotIdentity` |

## Extension Behavior — New Rule Types Require No Finding Model Changes

| Scenario | Test(s) |
|---|---|
| A new Rule Type's RuleEvaluationResults are consumed without Finding Model changes | `ExtensionMechanismTest.aStructurallyDistinctSecondRuleTypesOutputUsesTheSameCoreMechanismsUnmodified` |

## Binding Decisions (this change's own `design.md`)

| Decision | Verification |
|---|---|
| 1. `Finding` shape (final, non-generic, no Recommendation/Remediation field) | `FindingTest` (aip-core) |
| 2. `EvaluationIdentity`/`LogicalFindingIdentity` as two distinct record types | `EvaluationIdentityTest`, `LogicalFindingIdentityTest` (aip-core) |
| 3. `FindingMetadata` contract | `FindingConstructorTest`, `ExtensionMechanismTest` |
| 4. Outcome qualification (FAIL + FindingMetadata only) | `FindingConstructorTest` (four qualification tests) |
| 5. Unanchored-scope synthetic concerned element | `ConcernedElementResolverTest` |
| 6. `Confidence` reuse (fixed to HIGH) | `FindingConstructorTest.confidenceIsAlwaysFixedToHigh` |
| 7. `RuleEvaluationResultSource` contract shape | `RuleEvaluationResultSourceTest` (aip-core) |
| 8. `FindingStore`/`FindingValidator`/`FindingPublisher` in `aip-findings` | Structural: all three live in `aip-findings/src/main/java/aip/findings/`, not `aip-core`; `FindingValidatorTest`, `FindingPublisherTest` |
| 9. Module scaffolding, `check-no-module-reference.sh` reuse | `check-module-dependencies`, `check-no-aip-rules-adapter`, `check-no-aip-analysis-adapter`, `check-no-aip-csmbuilder-adapter` CI guard executions |
| 10. Fixture layer (no CSM-graph builder needed) | `RuleEvaluationResultFixtures`, `InMemoryRuleEvaluationResultSource`, `InMemoryFindingStore`, `StubFindingMetadata` (test-only); `check-fixture-package-scope` CI guard |
