# Requirement/Scenario Traceability Matrix

tasks.md 21.1: every requirement and scenario in
`openspec/changes/define-rule-framework/specs/rule-framework/spec.md`
(19 requirements, 42 scenarios), mapped to the automated test(s)
exercising it. Test references are `ClassName.methodName`; unqualified
class names live in `aip-rules/src/test/java/aip/rules/...` unless
marked `(aip-core)`, which live in
`aip-core/src/test/java/aip/core/csm/...`. Where a requirement is
structurally guaranteed rather than actively tested (e.g. by Java's
type system, or by this module's dependency graph), that is stated
explicitly rather than pointing at a test that would always trivially
pass.

## Deterministic, Declarative Rule Evaluation Only

| Scenario | Test(s) |
|---|---|
| No AI or heuristic judgment used to produce a Rule Evaluation Result | Architecturally guaranteed: `aip-rules`'s dependency graph (`scripts/check-module-dependencies.sh`) permits only `aip-core`; `scripts/check-no-ai-heuristic-imports.sh` confirms no randomness/AI-SDK import exists in production source. |
| Repeated evaluation over unchanged input is identical | `RuleEvaluationOrchestratorTest.repeatedEvaluationOverUnchangedInputIsIdentical` |

## Rule Type Contract

| Scenario | Test(s) |
|---|---|
| Rule Type declares identifier, version, and inputs | `RuleTypeRegistryTest.registersAndLooksUpByIdentifier` |
| Rule Type reads no undeclared content | Architecturally guaranteed: `RuleType.evaluate`'s own signature accepts only `Rule`, `AnalysisView`, `CsmScopeInstance`, and the declared-Analyzer-keyed `consumedAnalysisResults` map — no other CSM- or Analysis-content-bearing parameter exists; `DependencyAndBoundaryTest.allCsmContentComesFromTheAnalysisViewAlone` exercises this directly. |

## Rule Declaration

| Scenario | Test(s) |
|---|---|
| Rule configures a Rule Type without code | Architecturally guaranteed: `Rule` is a data-only record (identifier, version, Rule Type identifier, configuration map) with no executable-logic field or method. |
| One Rule Type supports multiple independently configured Rules | `RuleEvaluationOrchestratorTest.ruleIsIndependentOfOtherRegisteredRules` (two Rules, one Rule Type, independent results); `RuleRegistryTest` confirms independent registration. |

## No Rule-to-Rule Composition

| Scenario | Test(s) |
|---|---|
| Rule Evaluation Result is unaffected by which other Rules are registered | `DependencyAndBoundaryTest.noRuleToRuleCompositionMechanismExists` |
| No declared dependency between Rules is honored | Architecturally guaranteed: `Rule` and `RuleType`'s own APIs have no field or method through which an ordering or dependency between Rules could be expressed; `DependencyAndBoundaryTest.noRuleToRuleCompositionMechanismExists` confirms two independently registered Rule Types/Rules both produce results with no cross-reference. |

## Rule Scope Declaration

| Scenario | Test(s) |
|---|---|
| Rule Type declares a kind-based scope | `CsmScopeTest` (aip-core, reused verbatim — no `aip-rules`-local wrapper, confirmed by `RuleTypeRegistryTest`) |
| Unanchored Rule Type is invoked once per repository | `RuleEvaluationOrchestratorTest.singleRepositoryScopeUnanchoredRuleTypeProducesOneWholeRepositoryResult`; `ExtensionMechanismTest` (`rule.first`'s `CsmScopeInstance.wholeRepository()` assertion) |
| Anchored Rule Type is invoked once per matching contained element | `RuleEvaluationOrchestratorTest` (anchored Rule Type case, one Result per matching Module); `RuleFrameworkFixtureEndToEndTest` (six Module elements, six Results) |

## Kind-Based Rule Applicability

| Scenario | Test(s) |
|---|---|
| Rule Scope instance with no matching kind is not evaluated | `RuleEvaluationOrchestratorTest.inapplicableScopeInstanceProducesNoResult` |
| Rule Scope instance with a matching kind is evaluated | `RuleEvaluationOrchestratorTest.producesOneResultPerApplicableScopeInstance` |

## Native-Attribute Rule Applicability Refinement

| Scenario | Test(s) |
|---|---|
| Ecosystem-specific Rule Type is not evaluated when no matching native attribute is present | `RuleEvaluationOrchestratorTest.nativeAttributeRefinementSkipsWhenNoMatchingAttributeIsPresent` |
| Ecosystem-specific Rule Type is evaluated when its native attribute predicate is satisfied | `RuleEvaluationOrchestratorTest.nativeAttributeRefinementInvokesWhenTheAttributeMatches` |

## Analysis Result Source Shape

| Scenario | Test(s) |
|---|---|
| Analysis Result Source exposes retrieval by identity and by Analyzer/snapshot | `AnalysisResultSourceTest` (aip-core, contract tests for `read`/`list`) |
| Rule Framework is agnostic to Analysis Result production | `AnalysisResultSourceTest` (aip-core) constructs an anonymous `AnalysisResultSource` distinct from `InMemoryAnalysisResultSource`; `RuleEvaluationOrchestrator`'s own logic never branches on which implementation it is given — exercised throughout `aip-rules`'s test suite, all of which use the fixture implementation interchangeably with the contract's documented shape. |

## Analysis Result Consumption Through the Analysis Result Source Only

| Scenario | Test(s) |
|---|---|
| Analysis Results are read only through the Analysis Result Source | `DependencyAndBoundaryTest.allCsmContentComesFromTheAnalysisViewAlone` (constructor injection of `AnalysisResultSource`, no other Analysis-Result-bearing input); architecturally guaranteed by `aip-rules`'s dependency graph having no dependency on `aip-analysis`. |

## Rule Evaluation Outcome Semantics

| Scenario | Test(s) |
|---|---|
| Satisfied condition produces PASS | `RuleFrameworkFixtureEndToEndTest` (modulePass -> PASS); `RuleEvaluationOrchestratorTest` |
| Unsatisfied condition produces FAIL | `RuleFrameworkFixtureEndToEndTest` (moduleFail -> FAIL); `RuleEvaluationOrchestratorTest` |
| Missing condition-specific content produces NOT_APPLICABLE, not silence | `RuleEvaluationOrchestratorTest.notApplicableForMissingConditionSpecificContent`; `RuleFrameworkFixtureEndToEndTest` (moduleNotApplicable and untargeted Modules -> NOT_APPLICABLE) |
| Every outcome is offered for publication | `RuleFrameworkFixtureEndToEndTest` (all three outcomes each validated and published in the same loop, none discarded on the basis of outcome) |

## Deterministic Rule Evaluation Result Identity

| Scenario | Test(s) |
|---|---|
| Identical inputs produce identical Result identity | `RuleEvaluationResultIdTest.identicalInputsProduceIdenticalIdentity` (aip-core) |
| Different consumed Analysis Results yield a distinguishable identity | `RuleEvaluationResultIdTest.differentConsumedAnalysisResultsYieldDistinguishableIdentity` (aip-core); `RuleEvaluationResultIdTest.consumedSetOrderDoesNotAffectIdentity` (aip-core) confirms this holds independent of `Set` iteration order |
| A different Rule version yields a distinguishable identity | `RuleEvaluationResultIdTest.differentRuleVersionYieldsDistinguishableIdentity` (aip-core) |

## Rule Evaluation Result Traceability

| Scenario | Test(s) |
|---|---|
| Result traceable to its producing Rule | `RuleEvaluationResultTest.retainsTraceabilityToProducingRuleAndSourceSnapshot` (aip-core) |
| Result traceable to its consumed Analysis Results | `RuleEvaluationResultTest.retainsTraceabilityToProducingRuleAndSourceSnapshot` (aip-core); `DependencyAndBoundaryTest` exercises the full consumed-set surviving end to end |

## Rule Evaluation Result Payload Is Rule-Type-Defined

| Scenario | Test(s) |
|---|---|
| Differently-shaped diagnostic payloads from different Rule Types are both accepted | `RuleEvaluationResultTest.differentlyShapedPayloadsAreBothAccepted` (aip-core); `RuleFrameworkFixtureEndToEndTest` (`String` payloads from a single Rule Type) combined with `ExtensionMechanismTest` (a structurally distinct second Rule Type's payload), both validated/published |
| Framework operations do not require understanding diagnostic payload content | `RuleEvaluationResultValidator`/`RuleEvaluationResultPublisher` never read `RuleEvaluationResult.payload()` anywhere in their own logic — architecturally guaranteed by inspection, exercised by every validator/publisher test using opaque payloads. |

## Rule Evaluation Results Are Durable, Individually Identifiable Artifacts

| Scenario | Test(s) |
|---|---|
| Rule Evaluation Result remains retrievable after the producing run ends | `RuleEvaluationResultPublisherTest.validResultIsPublishedAndRetrievable` |
| A new Rule Evaluation Result does not overwrite a prior, differently-identified one | `InMemoryRuleEvaluationResultStore.write`'s own overwrite guard, exercised implicitly by every test writing more than one Result (e.g. `RuleFrameworkFixtureEndToEndTest`); explicit rejection behavior is a fixture-store property, not a framework requirement — the interface itself (`RuleEvaluationResultStore`) documents the "never overwrites" contract a conforming implementation must satisfy. |

## Rule Evaluation Result Validation Before Publication

| Scenario | Test(s) |
|---|---|
| Valid Result is published as usable output | `RuleEvaluationResultValidatorTest.validResultPassesValidation`, `RuleEvaluationResultPublisherTest.validResultIsPublishedAndRetrievable` |
| Result referencing an unavailable Analysis Result or CSM identity is rejected | `RuleEvaluationResultValidatorTest.resultReferencingAnUnavailableAnalysisResultIsRejected`, `RuleEvaluationResultValidatorTest.resultReferencingAnUnavailableCsmIdentityIsRejected` |
| Result from an unregistered Rule/version is rejected | `RuleEvaluationResultValidatorTest.resultFromAnUnregisteredRuleVersionIsRejected`, `RuleEvaluationResultValidatorTest.resultFromAnUnregisteredRuleIsRejected`, `RuleEvaluationResultPublisherTest.invalidResultIsNeverWritten` |
| Result exceeding its Rule Type's declared scope is rejected | `RuleEvaluationResultValidatorTest.resultExceedingItsRuleTypesDeclaredScopeIsRejected` |

## Rule Evaluation Results Are a Distinct Concept From CSM Knowledge, Analysis Results, and Findings

| Scenario | Test(s) |
|---|---|
| Producing a Rule Evaluation Result does not alter CSM or Analysis content | `DependencyAndBoundaryTest.producingARuleEvaluationResultDoesNotAlterCsmOrAnalysisContent` |
| Rule Evaluation Results are stored separately from CSM and Analysis content | `DependencyAndBoundaryTest.ruleEvaluationResultIsNeverExposedAsCsmOrAnalysisContent` (structural: `RuleEvaluationResult`, `CsmElement`/`CsmRelationship`, and `AnalysisResult` are disjoint types with no shared query surface) |

## CSM and Analysis Content Reached Only Through Established Contracts

| Scenario | Test(s) |
|---|---|
| No direct Repository Evidence or Runtime Model dependency | Architecturally guaranteed: `aip-rules`'s dependency graph has no reachable dependency on `aip-csm-builder` or any Repository Evidence/Runtime Model type; `DependencyAndBoundaryTest.allCsmContentComesFromTheAnalysisViewAlone` confirms the only CSM-content-bearing parameter is `AnalysisView`. |
| No dependency on the producing module | `scripts/check-no-module-reference.sh aip-rules 'aip\.analysis\.'` (wired as the `check-no-aip-analysis-adapter` build execution in `aip-rules/pom.xml`), confirming no source under `aip-rules` references `aip.analysis.` at all. |
| Source CSM Snapshot identity is obtained from the Analysis View | Architecturally guaranteed: `RuleEvaluationOrchestrator` and `RuleEvaluationResultValidator` obtain `CsmSnapshotId` exclusively via `AnalysisView.sourceSnapshotId()`, never via a `CsmSnapshotSource` parameter (`aip-rules` has no such type reachable at all). |

## Single-Repository Rule Evaluation Scope

| Scenario | Test(s) |
|---|---|
| One evaluation run covers one repository's snapshot | `RuleEvaluationOrchestratorTest.singleRepositoryScopeUnanchoredRuleTypeProducesOneWholeRepositoryResult`; architecturally guaranteed by `AnalysisView` exposing exactly one `CsmSnapshotId`, which `RuleEvaluationOrchestrator` never reads a second instance of. |

## Extension Mechanism for New Rule Types

| Scenario | Test(s) |
|---|---|
| New Rule Type registered without core mechanism changes | `ExtensionMechanismTest.aStructurallyDistinctSecondRuleTypeUsesTheSameCoreMechanismsUnmodified` |

## Binding Decisions (this change's own `design.md`)

| Decision | Verification |
|---|---|
| 1. `RuleEvaluationResult` shape (final, non-generic, `Object` payload) | `RuleEvaluationResultTest`, `RuleEvaluationResultIdTest` (aip-core) |
| 2. `RuleEvaluationResultId` — 5-component identity, set-order-independent | `RuleEvaluationResultIdTest` (aip-core), including `consumedSetOrderDoesNotAffectIdentity` |
| 3. `CsmScope`/`CsmScopeInstance` reused verbatim as Rule Scope | `CsmScopeEvaluatorTest` (aip-core); no `aip-rules`-local wrapper type exists |
| 4. `AnalysisView` as the sole CSM-content contract; source snapshot identity via `AnalysisView` | `DependencyAndBoundaryTest.allCsmContentComesFromTheAnalysisViewAlone` |
| 5. `AnalysisResultSource` contract, defined in `aip-core` | `AnalysisResultSourceTest` (aip-core) |
| 6. `RuleEvaluationResultStore` stays in `aip-rules` | Structural: `RuleEvaluationResultStore.java` lives in `aip-rules/src/main/java/aip/rules/`, not `aip-core` |
| 7. `aip-rules` depends on `aip-core` only; sibling of `aip-analysis`/`aip-csm-builder` | `scripts/check-module-dependencies.sh` (`check-module-dependencies` build execution); `scripts/check-no-module-reference.sh aip-rules 'aip\.analysis\.'` |
| 8. Fixture layer, no real `aip-analysis` adapter | `CsmSnapshotSourceBuilder`, `InMemoryAnalysisResultSource` (test-only, `aip-rules`-local); `scripts/check-fixture-package-scope.sh` |
| 9. Prerequisite gate on `implement-analysis-framework` | Confirmed complete before Section 1 began (Section 0.1); this change's own dependency on `AnalysisView`/`AnalysisResult`/`AnalysisResultId`/`CsmScope` compiles throughout. |

## Self-Directed Refactor: `CsmScopeEvaluator` Promotion to `aip-core`

Not a `define-rule-framework` requirement, but a self-directed
architectural decision made during this change's own implementation:
Rule Scope's applicability/enumeration logic (`CsmScopeEvaluator`,
`ContainmentClosure`) was promoted from `aip-analysis` (where it was
originally built, as `AnalysisScopeEvaluator`, during
`implement-analysis-framework`) into `aip-core`, since its logic is
identical for both Analysis Scope and Rule Scope and both `aip-analysis`
and `aip-rules` now need it as siblings with no cross-dependency
permitted between them.

| Aspect | Verification |
|---|---|
| Moved logic behaves identically to its original `aip-analysis` implementation | `CsmScopeEvaluatorTest` (aip-core, 7 tests reimplementing the original `AnalysisScopeEvaluatorTest` using plain `aip-core` element construction) |
| `aip-analysis` continues to function correctly using the promoted, shared logic | `AnalysisOrchestratorTest`, full `aip-analysis` suite (22 tests, all passing after the refactor) |
| `aip-rules` uses the identical promoted logic, no duplication | `RuleEvaluationOrchestrator` imports `aip.core.csm.CsmScopeEvaluator` directly; no `aip-rules`-local reimplementation of scope enumeration/applicability exists |
