## Context

`aip-analysis` and its `aip-core` support types (`CsmSnapshotSource`,
`AnalysisView`, `AnalysisResult`, `CsmScope`/`CsmScopeInstance`/
`CsmScopeEvaluator`, `AnalyzerRegistry`, `AnalysisOrchestrator`,
`AnalysisResultStore`/`Validator`/`Publisher`) are already implemented
and merged (the archived `2026-08-13-implement-analysis-framework`
change). This `tasks.md` covers only the delta introduced by the
`define-analysis-framework` Review pass's two REQUIRED CHANGE edits —
it does not re-implement anything already built:

1. **ADDED** requirement `Incremental Analysis Is Architecturally
   Supported` — no corresponding mechanism exists yet in the codebase.
   This is genuinely new work (Sections 1 and 3 below).
2. **MODIFIED** requirement `Analysis Result Validation Before
   Publication` (store-gating strengthening) — `AnalysisResultPublisher`
   **already** validates before calling `AnalysisResultStore.write`
   (see `AnalysisResultPublisher.publish`), and
   `AnalysisResultPublisherTest.invalidResultIsNeverWritten` already
   covers one failure case end-to-end. This delta's work is closing
   remaining test-coverage gaps and adding an explicit regression
   guard, not new production code (Section 2 below).

## 1. Incremental Analysis — Scope-Based Snapshot Comparison (`aip-core`)

- [x] 1.1 Implement a scope-based content comparator in `aip-core`
      (`aip.core.csm`, alongside `CsmScopeEvaluator` — same placement
      precedent as `AnalysisView`: a general CSM capability, not an
      `aip-analysis`-local one) that, given two `AnalysisView`s of the
      same repository and a `CsmScope`, determines whether the CSM
      elements/relationships within that scope differ between the two
      views. Reuse `CsmScopeEvaluator.contentElements`/
      `contentRelationships` rather than re-deriving scope content
      matching. Per `define-analysis-framework/specs/analysis-framework/spec.md`'s
      `Incremental Analysis Is Architecturally Supported` requirement.
      Depends on: existing `CsmScopeEvaluator`.
- [x] 1.2 Implement a re-execution-candidate query — given an
      `AnalyzerRegistry` and two `AnalysisView`s of the same
      repository, return the subset of registered Analyzers whose
      declared `CsmScope` shows a difference per 1.1. This determination
      SHALL use only each Analyzer's declared Analysis Scope and the
      two views' CSM content — no declared Analyzer-to-Analyzer
      dependency or ordering, and no per-element/fine-grained diffing
      beyond "some scope-relevant content differs." Depends on: 1.1.
- [x] 1.3 Do **not** introduce a declared-dependency or execution-order
      mechanism between Analyzers, and do **not** implement a
      fine-grained (e.g., per-Subject or per-relationship-instance)
      invalidation graph — a whole-scope-content difference is
      sufficient grounds for candidacy, per Design Decision 7's "coarse
      determination... is sufficient for v1." Confirm by review that
      1.1/1.2's API surface has no way to declare such a dependency.
      Depends on: 1.2.
- [x] 1.4 Add tests for each scenario in the `Incremental Analysis Is
      Architecturally Supported` requirement:
      - two `AnalysisView`s of the same repository can be compared for
        a given Analyzer's declared scope;
      - an Analyzer is flagged a re-execution candidate when
        scope-relevant content differs between the two views;
      - an Analyzer is **not** flagged when only content outside its
        declared scope differs;
      - the comparator/candidate-query never requires or accepts a
        declared dependency between two Analyzers;
      - a candidate determination is accepted on any scope-relevant
        difference without requiring a more precise, fine-grained
        determination within that scope.
      Depends on: 1.1, 1.2, 1.3.

## 2. Validation-Gate Coverage Completion (`aip-analysis`)

- [x] 2.1 Add `AnalysisResultPublisherTest` cases for the two
      currently-untested failure categories at the Publisher level:
      referential integrity failure (Result references a CSM element
      absent from the source snapshot) and scope-containment failure
      (Result's scope instance anchoring/kind mismatches its Analyzer's
      declared scope) — today only the Analyzer/version-mismatch case
      is exercised through `AnalysisResultPublisher` directly
      (`AnalysisResultValidatorTest` covers all three at the validator
      level, but not through the publish-then-query-the-store path).
      For each, assert the Result is neither published nor retrievable
      from the store. Depends on: existing `AnalysisResultValidator`,
      `AnalysisResultPublisher`.
- [x] 2.2 Add a test for the new `AnalysisResultStore never holds a
      Result that failed validation` scenario: publish a mix of valid
      and invalid Results against one store, then assert
      `store.listByAnalyzerAndSnapshot` and `store.read` never surface
      any Result from the invalid set, only the valid ones. Depends on:
      2.1.
- [x] 2.3 Add a source-scan/architectural check (mirroring the existing
      dependency-graph and fixture-scope CI checks) confirming
      `AnalysisResultStore.write` is called only from
      `AnalysisResultPublisher.publish`, after a successful
      `AnalysisResultValidator.validate` call — so a future code change
      cannot silently reintroduce an unvalidated write path. Depends
      on: 2.1, 2.2.

## 3. Traceability and Verification

- [x] 3.1 Extend the Analysis Framework traceability matrix (or add a
      delta traceability record for this change) mapping the two
      updated spec deltas — ADDED `Incremental Analysis Is
      Architecturally Supported`, MODIFIED `Analysis Result Validation
      Before Publication` — and their scenarios to the tests added in
      Sections 1 and 2. Depends on: 1.4, 2.1, 2.2.
- [x] 3.2 Run `mvn verify` from the repository root; confirm all
      previously-passing tests remain green and the new tests in
      Sections 1 and 2 pass. Depends on: 1.4, 2.1, 2.2, 2.3.
- [x] 3.3 Confirm architectural invariants are unaffected: the new
      scope-comparison mechanism (1.1/1.2) lives in `aip-core`, not
      `aip-analysis`; `aip-analysis` still depends on `aip-core` only;
      no concurrency infrastructure is introduced. Re-run the existing
      dependency-graph CI check. Depends on: 1.1, 1.2, 3.2.
- [x] 3.4 Final gate: confirm this change implements only the two
      `define-analysis-framework` spec deltas, modifies no other
      approved requirement or design decision, and adds no Rule
      Framework, Finding Model, or Agent Framework code. Depends on:
      all preceding sections.
