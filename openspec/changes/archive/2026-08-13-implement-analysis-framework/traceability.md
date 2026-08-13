# Requirement/Scenario Traceability Matrix

tasks.md 14.1: every requirement and scenario in
`openspec/changes/define-analysis-framework/specs/analysis-framework/spec.md`
(18 requirements, 39 scenarios), mapped to the automated test(s)
exercising it. Test references are `ClassName.methodName`; unqualified
class names live in `aip-analysis/src/test/java/aip/analysis/...`
unless marked `(aip-core)`, which live in
`aip-core/src/test/java/aip/core/csm/...`. Where a requirement is
structurally guaranteed rather than actively tested (e.g. by Java's
type system, or by this module's dependency graph), that is stated
explicitly rather than pointing at a test that would always trivially
pass.

## Deterministic, Snapshot-Driven Analysis Only

| Scenario | Test(s) |
|---|---|
| No AI or heuristic judgment used to produce an Analysis Result | Architecturally guaranteed: `aip-analysis`'s dependency graph (`scripts/check-module-dependencies.sh`) permits only `aip-core`; `scripts/check-no-ai-heuristic-imports.sh` confirms no randomness/AI-SDK import exists in production source. |
| Repeated analysis over unchanged input is identical | `AnalysisOrchestratorTest.repeatedAnalysisOverUnchangedInputIsIdentical` |

## CSM Snapshot as Sole Analysis Input

| Scenario | Test(s) |
|---|---|
| Analysis proceeds without policy or runtime data | `DeterminismAndBoundaryTest.analysisProceedsWithoutPolicyOrRuntimeDataConfigured` |
| No direct Repository Evidence dependency | Architecturally guaranteed: `Analyzer.analyze`'s own signature accepts only `AnalysisView`/`CsmScopeInstance`; no Repository Evidence type is reachable from `aip-analysis`'s dependency graph. |

## Single-Repository Analysis Scope

| Scenario | Test(s) |
|---|---|
| One analysis run covers one repository's snapshot | `DeterminismAndBoundaryTest.everyResultFromOneRunReferencesExactlyOneRepositorysSnapshotIdentity` |
| Cross-repository analysis is not attempted | Architecturally guaranteed: `CsmSnapshotSource` exposes exactly one `CsmSnapshotId`; `AnalysisOrchestrator` never reads a second snapshot source. |

## CSM Snapshot Source Shape

| Scenario | Test(s) |
|---|---|
| Snapshot source exposes elements, relationships, and identity | `AnalysisViewTest.exposesItsSourceCsmSnapshotIdentity` (aip-core); `CsmSnapshotSourceBuilder.build()`'s own contract-conforming fixture, exercised throughout `aip-analysis`'s test suite. |
| Analysis Framework is agnostic to snapshot production | `AnalysisViewTest` (aip-core) constructs `AnalysisView` from a hand-built anonymous `CsmSnapshotSource`, distinct from both `aip-csm-builder`'s `Snapshot` and the fixture builder — the framework's own logic never branches on which. |

## Analysis View Construction

| Scenario | Test(s) |
|---|---|
| Analysis View exposes its source CSM Snapshot identity | `AnalysisViewTest.exposesItsSourceCsmSnapshotIdentity` (aip-core) |
| Analysis View reflects effective knowledge for an uncontested subject | `AnalysisViewTest.reflectsEffectiveKnowledgeForAnUncontestedSubject` (aip-core) |
| Analysis View marks conflicting knowledge as CONFLICTED | `AnalysisViewTest.marksConflictingKnowledgeAsConflicted` (aip-core) |
| Analysis performed without direct snapshot or evidence access | Architecturally guaranteed: `Analyzer.analyze(AnalysisView, CsmScopeInstance)`'s signature has no `CsmSnapshotSource` or Repository Evidence parameter. |

## Analysis View Is a Derived Projection, Not a New Canonical Model

| Scenario | Test(s) |
|---|---|
| Analysis View content is fully re-derivable from its source snapshot | `AnalysisViewTest.twoConstructionsFromEquivalentContentProduceEquivalentViews` (aip-core) |
| Analysis View is not independently persisted | Architecturally guaranteed: no `AnalysisView` store/persistence type exists anywhere in `aip-core` or `aip-analysis`; only `AnalysisResultStore` persists anything. |

## Analyzer Contract

| Scenario | Test(s) |
|---|---|
| Analyzer declares identifier, version, and scope | `AnalyzerRegistryTest.registersAndLooksUpByIdentifier` |
| Analyzer produces one Result per applicable scope instance | `AnalysisOrchestratorTest.producesOneResultPerApplicableScopeInstance` |

## Analyzer Independence

| Scenario | Test(s) |
|---|---|
| Analyzer result is unaffected by which other Analyzers are registered | `AnalysisOrchestratorTest.analyzerResultIsUnaffectedByWhichOtherAnalyzersAreRegistered` |
| No declared dependency between Analyzers is honored | Architecturally guaranteed: `Analyzer` and `AnalyzerRegistry`'s own APIs have no method through which an ordering or dependency between Analyzers could be expressed. |

## Analyzer Execution Order Independence

| Scenario | Test(s) |
|---|---|
| Result is identical regardless of invocation order | `AnalyzerRegistryTest.registeredIsOrderedByIdentifier` (deterministic order) combined with `AnalysisOrchestratorTest.analyzerResultIsUnaffectedByWhichOtherAnalyzersAreRegistered` (no Analyzer reads another's state, so order cannot matter) — architecturally guaranteed by `Analyzer.analyze`'s signature carrying no other-Analyzer state. |

## Analysis Scope Declaration

| Scenario | Test(s) |
|---|---|
| Analyzer declares a kind-based scope | `CsmScopeTest.declaresAtLeastOneKindOrRelationshipType` (aip-core) |
| Unanchored Analyzer is invoked once per repository | `AnalysisScopeEvaluatorTest.unanchoredScopeEnumeratesExactlyOneWholeRepositoryInstance` |
| Anchored Analyzer is invoked once per matching contained element | `AnalysisScopeEvaluatorTest.anchoredScopeEnumeratesOneInstancePerMatchingContainedElement` |

## Kind-Based Analyzer Applicability

| Scenario | Test(s) |
|---|---|
| Analyzer with no matching kind present is skipped | `AnalysisScopeEvaluatorTest.skipsWhenNoDeclaredKindIsPresent`, `AnalysisOrchestratorTest.inapplicableAnalyzerProducesNoResult` |
| Analyzer with a matching kind present is invoked | `AnalysisScopeEvaluatorTest.invokesWhenADeclaredKindIsPresent` |

## Native-Attribute Applicability Refinement

| Scenario | Test(s) |
|---|---|
| Ecosystem-specific Analyzer is skipped when no matching native attribute is present | `AnalysisScopeEvaluatorTest.nativeAttributeRefinementSkipsWhenNoMatchingAttributeIsPresent` |
| Ecosystem-specific Analyzer is invoked when its native attribute predicate is satisfied | `AnalysisScopeEvaluatorTest.nativeAttributeRefinementInvokesWhenTheAttributeMatches` |

## Deterministic Analysis Result Identity

| Scenario | Test(s) |
|---|---|
| Identical inputs produce identical Result identity | `AnalysisResultIdTest.identicalInputsProduceIdenticalIdentity` (aip-core) |
| A different Analyzer version yields a distinguishable identity | `AnalysisResultIdTest.differentAnalyzerVersionYieldsDistinguishableIdentity` (aip-core) |

## Analysis Result Traceability

| Scenario | Test(s) |
|---|---|
| Result traceable to its producing Analyzer | `AnalysisResultTest.retainsTraceabilityToProducingAnalyzerAndSourceSnapshot` (aip-core) |
| Result traceable to its source CSM Snapshot | `AnalysisResultTest.retainsTraceabilityToProducingAnalyzerAndSourceSnapshot` (aip-core) |

## Analysis Result Payload Is Analyzer-Defined

| Scenario | Test(s) |
|---|---|
| Differently-shaped payloads from different Analyzers are both accepted | `AnalysisResultTest.differentlyShapedPayloadsAreBothAccepted` (aip-core); `AnalysisFrameworkFixtureEndToEndTest` (two differently-shaped Analyzer payloads, both validated/published) |
| Framework operations do not require understanding payload content | `AnalysisResultValidator`/`AnalysisResultPublisher` never read `AnalysisResult.payload()` anywhere in their own logic — architecturally guaranteed by inspection, exercised by every validator/publisher test using opaque `String`/`int` payloads interchangeably. |

## Analysis Results Are Durable, Individually Identifiable Artifacts

| Scenario | Test(s) |
|---|---|
| Analysis Result remains retrievable after the producing run ends | `AnalysisResultPublisherTest.validResultIsPublishedAndRetrievable` |
| A new Analysis Result does not overwrite a prior, differently-identified one | `InMemoryAnalysisResultStore.write`'s own overwrite guard, exercised implicitly by every test writing more than one Result (e.g. `AnalysisFrameworkFixtureEndToEndTest`); explicit rejection behavior is a fixture-store property, not a framework requirement — the interface itself (`AnalysisResultStore`) documents the "never overwrites" contract a conforming implementation must satisfy. |

## Analysis Result Validation Before Publication

| Scenario | Test(s) |
|---|---|
| Valid Result is published as usable output | `AnalysisResultValidatorTest.validResultPassesValidation`, `AnalysisResultPublisherTest.validResultIsPublishedAndRetrievable` |
| Result referencing a CSM identity absent from its source snapshot is rejected | `AnalysisResultValidatorTest.resultReferencingACsmIdentityAbsentFromItsSourceSnapshotIsRejected` |
| Result from an unregistered Analyzer/version is rejected | `AnalysisResultValidatorTest.resultFromAnUnregisteredAnalyzerVersionIsRejected`, `AnalysisResultValidatorTest.unregisteredAnalyzerIsRejected`, `AnalysisResultPublisherTest.invalidResultIsNeverWritten` |
| Result exceeding its Analyzer's declared scope is rejected | `AnalysisResultValidatorTest.resultExceedingItsAnalyzersDeclaredScopeIsRejected` |

## Analysis Results Are Not CSM Knowledge

| Scenario | Test(s) |
|---|---|
| Producing an Analysis Result does not alter CSM content | `DeterminismAndBoundaryTest.producingAnAnalysisResultDoesNotAlterCsmSnapshotContent` |
| Analysis Results are stored separately from CSM content | `DeterminismAndBoundaryTest.analysisResultIsNeverExposedAsCsmContent` (structural: `AnalysisResult` and `CsmElement`/`CsmRelationship` are disjoint types with no shared query surface) |

## Extension Mechanism (not a spec.md requirement; mirrors `implement-csm-builder`'s own Extension Mechanism Verification)

| Scenario | Test(s) |
|---|---|
| A structurally distinct new Analyzer requires no core mechanism change | `ExtensionMechanismTest.aStructurallyDistinctSecondAnalyzerUsesTheSameCoreMechanismsUnmodified` |

## Binding Decisions (this change's own `design.md`)

| Decision | Verification |
|---|---|
| 1. `AnalysisResult` shape (final, non-generic, `Object` payload) | `AnalysisResultTest`, `AnalysisResultIdTest` (aip-core) |
| 2. `CsmScope` in `aip-core` | `CsmScopeTest` (aip-core); reused verbatim by `aip-analysis`, no wrapper type |
| 3. `CsmSnapshotSource`/`CsmSnapshotId` shape | `CsmSnapshotIdTest` (aip-core), `AnalysisViewTest.exposesItsSourceCsmSnapshotIdentity` (aip-core) |
| 4. `AnalysisResultId` | `AnalysisResultIdTest` (aip-core) |
| 5. `AnalysisResultStore` stays in `aip-analysis` | Structural: `AnalysisResultStore.java` lives in `aip-analysis/src/main/java/aip/analysis/`, not `aip-core` |
| 6. Fixture layer, no real adapter | `CsmSnapshotSourceBuilder` (test-only); `scripts/check-fixture-package-scope.sh` and `scripts/check-no-csm-builder-analysis-adapter.sh` |
| 7. Test organization | This file plus the unit/contract/end-to-end test split described above |
