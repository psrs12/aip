# Requirement/Scenario Traceability Matrix (Delta)

Covers only the two spec deltas introduced by
`define-analysis-framework`'s Review pass (ADDED `Incremental Analysis
Is Architecturally Supported`; MODIFIED `Analysis Result Validation
Before Publication`) — see the base traceability matrix in
`openspec/changes/archive/2026-08-13-implement-analysis-framework/traceability.md`
for the other 17 requirements / 39 scenarios, all unaffected by this
change.

## Incremental Analysis Is Architecturally Supported (ADDED)

| Scenario | Test(s) |
|---|---|
| CSM Snapshots of the same repository can be compared for scope-relevant differences | `CsmScopeChangeDetectorTest.reportsNoDifferenceWhenScopedContentIsUnchanged` (aip-core); `IncrementalAnalysisTest.twoViewsOfTheSameRepositoryCanBeComparedForAGivenAnalyzersScope` |
| An Analyzer is identified as a re-execution candidate when scope-relevant content changed | `CsmScopeChangeDetectorTest.reportsADifferenceWhenScopedContentChanged`, `.detectsRelationshipTypeScopedDifferences` (aip-core); `IncrementalAnalysisTest.analyzerIsACandidateWhenItsDeclaredScopeContentChanged` |
| An Analyzer is not identified as a re-execution candidate when its scope is unaffected | `CsmScopeChangeDetectorTest.ignoresContentOutsideTheDeclaredScope` (aip-core); `IncrementalAnalysisTest.analyzerIsNotACandidateWhenOnlyOutOfScopeContentChanged` |
| A cross-Analyzer dependency graph is not required | `IncrementalAnalysisTest.noDeclaredDependencyBetweenAnalyzersIsRequiredOrHonored`; structurally guaranteed: `IncrementalAnalysis.reexecutionCandidates` and `CsmScopeChangeDetector.scopeContentDiffers` accept only `AnalyzerRegistry`/`AnalysisView`/`CsmScope` parameters — no API exists to declare an Analyzer-to-Analyzer dependency or ordering. |
| Sophisticated cross-Analyzer invalidation is not required | Structurally guaranteed: `CsmScopeChangeDetector.scopeContentDiffers` reports only whole-scope set (in)equality — no per-Subject or per-relationship-instance diffing exists in `aip-core` or `aip-analysis`. |

## Analysis Result Validation Before Publication (MODIFIED)

| Scenario | Test(s) |
|---|---|
| Valid Result is published as usable output | `AnalysisResultPublisherTest.validResultIsPublishedAndRetrievable` (pre-existing) |
| Result referencing a CSM identity absent from its source snapshot is rejected | `AnalysisResultPublisherTest.resultReferencingAnAbsentCsmIdentityIsNeverWritten` (new); `AnalysisResultValidatorTest.resultReferencingACsmIdentityAbsentFromItsSourceSnapshotIsRejected` (pre-existing, validator level) |
| Result from an unregistered Analyzer/version is rejected | `AnalysisResultPublisherTest.invalidResultIsNeverWritten` (pre-existing) |
| Result exceeding its Analyzer's declared scope is rejected | `AnalysisResultPublisherTest.resultExceedingItsAnalyzersDeclaredScopeIsNeverWritten` (new); `AnalysisResultValidatorTest.resultExceedingItsAnalyzersDeclaredScopeIsRejected` (pre-existing, validator level) |
| AnalysisResultStore never holds a Result that failed validation | `AnalysisResultPublisherTest.storeNeverHoldsAResultThatFailedValidation` (new) — publishes a mix of one valid and two invalid Results against one store, asserts `read`/`listByAnalyzerAndSnapshot` never surface the invalid ones; structurally guaranteed further by `AnalysisResultPublisher.publish` being the only call site of `AnalysisResultStore.write` in `aip-analysis` production source (confirmed by source scan; see `tasks.md` 2.3), and that call site is unconditionally preceded by a `validation.valid()` check. |

## Verification Run

`mvn verify` from the repository root: **384 tests, 0 failures, 0
errors** across all six modules, including the pre-existing
`aip-analysis` "no concurrency infrastructure" and "depends only on
aip-core" architectural guards — both re-confirmed unaffected by this
change's two new `aip-core`/`aip-analysis` production types
(`CsmScopeChangeDetector`, `IncrementalAnalysis`).
