## 1. Module and Contract Foundations

- [ ] 1.1 Create the `aip-declared-knowledge` module (Java 21+, Maven), depending on `aip-core` only, per `design.md` Decision 1.
- [ ] 1.2 Add a dependency-graph CI check (`scripts/check-module-dependencies.sh aip-declared-knowledge aip:aip-core`) that fails the build if `aip-declared-knowledge` gains any non-test dependency outside `aip-core` — in particular, no dependency on `aip-csm-builder`, `aip-analysis`, `aip-rules`, `aip-findings`, or `aip-ai`. Depends on: 1.1.
- [ ] 1.3 Define the `Declaration` value types in `aip.declaredknowledge`: an Architecture Component Declaration (name + non-empty set of `CsmElementId`s) and a Boundary Constraint Declaration (source identity, target identity, constraint kind), per `design.md` Decision 3 and `spec.md`'s `Architecture Component Declaration Shape`/`Boundary Constraint Declaration Shape` requirements. Depends on: 1.1.

## 2. Architecture Component Construction

- [ ] 2.1 Implement construction of an `ArchitectureComponentElement` from a valid Architecture Component Declaration, referencing the declared composition by identity, per `spec.md`'s `Architecture Component Declaration Shape` requirement. Depends on: 1.3.
- [ ] 2.2 Implement referential-integrity checking against a supplied baseline `CsmSnapshotSource`: every composition identity SHALL exist within the baseline, per `design.md` Decision 4 and `spec.md`'s `CSM Baseline as Referential Integrity Input` requirement. Depends on: 2.1.
- [ ] 2.3 Add tests for: valid composition declaration constructs a component; declaration referencing a nonexistent baseline identity is rejected; construction is agnostic to which `CsmSnapshotSource` implementation supplies the baseline (fixture vs. any other conforming implementation). Depends on: 2.1, 2.2.

## 3. Boundary Constraint Construction

- [ ] 3.1 Implement construction of a `BOUNDARY_CONSTRAINT` `CsmRelationship` from a valid Boundary Constraint Declaration, checking that its source/target identities correspond to Architecture Components constructed in the same batch or present in the baseline, per `spec.md`'s `Boundary Constraint Declaration Shape` requirement. Depends on: 1.3, 2.1.
- [ ] 3.2 Implement the `constraint-kind` = `must-not-depend-on` native-attribute encoding for a "must not depend on" declaration, reusing `aip-rules`'s existing convention verbatim (do not introduce a second encoding) — per `design.md` Decision 5 and `spec.md`'s `Must-Not-Depend-On Constraint Vocabulary Reuse` requirement. Depends on: 3.1.
- [ ] 3.3 Add tests for: valid boundary declaration constructs a boundary relationship; declaration referencing an unpublished component identity is rejected; a constructed must-not-depend-on relationship carries the exact `constraint-kind`/`must-not-depend-on` attribute `aip.rules.boundarycompliance.BoundaryConstraintKind` reads. Depends on: 3.1, 3.2.

## 4. Provenance and Determinism

- [ ] 4.1 Ensure every constructed `ArchitectureComponentElement`/`BOUNDARY_CONSTRAINT` relationship carries `declared` provenance exclusively — reuse `ProvenanceRecord.declared(...)`, never `observed(...)` or `inferred(...)` — per `spec.md`'s `Declared Provenance on Every Constructed Element` and `Declared Knowledge Scope Excludes Inferred Construction` requirements. Depends on: 2.1, 3.1.
- [ ] 4.2 Add tests/guards confirming no AI/LLM call, heuristic scoring, or probabilistic-inference code path exists anywhere in `aip-declared-knowledge`, per `spec.md`'s `Declared Knowledge Construction Is Deterministic` requirement (mirroring `check-no-ai-heuristic-imports.sh`'s existing pattern for other modules). Depends on: 1.1.
- [ ] 4.3 Add a test confirming repeated construction over the same baseline and Declarations produces identical constructed content, per the same requirement's second scenario. Depends on: 4.1.

## 5. Declared Knowledge Validation and Publishing

- [ ] 5.1 Implement `DeclaredKnowledgeValidator`: referential integrity (Section 2/3's checks) and provenance-classification correctness (Section 4.1's guarantee, re-checked defensively), per `design.md` Decision 7 and `spec.md`'s `Declared Knowledge Validation Before Publication` requirement. Depends on: 2.2, 3.1, 4.1.
- [ ] 5.2 Implement `DeclaredKnowledgePublisher`: gates writes so content that fails validation is never written at all, mirroring `AnalysisResultPublisher`/`SnapshotPublisher`'s existing gate pattern. Depends on: 5.1.
- [ ] 5.3 Add tests for a passing outcome and for each failing-validation case (nonexistent baseline identity, unpublished boundary-constraint endpoint), confirming a failing batch is never published and never written to the store. Depends on: 5.1, 5.2.

## 6. Declared Knowledge Artifact Identity, Traceability, and Store

- [ ] 6.1 Implement deterministic declared-knowledge artifact identity as a pure function of (repository identity, baseline `CsmSnapshotSource` identity, sequence value) — never random — per `design.md` Decision 7 and `spec.md`'s `Deterministic Declared Knowledge Artifact Identity` requirement. Depends on: 5.1.
- [ ] 6.2 Define a `DeclaredKnowledgeStore` abstraction (write/read-by-identity/list) mirroring `SnapshotStore`/`AnalysisResultStore`'s own shape; concrete persistence technology deferred. Depends on: 6.1.
- [ ] 6.3 Add identity-stability and durability tests: identical inputs produce identical artifact identity across two runs; a published artifact remains retrievable after the producing process exits. Depends on: 6.1, 6.2.

## 7. Publication as a CSM Snapshot Source

- [ ] 7.1 Implement a `CsmSnapshotSource`-conforming wrapper over a published declared-knowledge artifact's constructed elements/relationships, tied to the same repository identity as its baseline, per `spec.md`'s `Declared Knowledge Is Published as a CSM Snapshot Source` requirement. Depends on: 5.2, 6.1.
- [ ] 7.2 Add a test confirming the published artifact conforms to the `CsmSnapshotSource` shape (elements, relationships, stable identity). Depends on: 7.1.

## 8. Combining Observed and Declared Content (`aip-core`)

- [ ] 8.1 Implement `CompositeCsmSnapshotSource` in `aip-core` (`aip.core.csm`, alongside `AnalysisView`/`CsmScopeEvaluator` — same general-mechanism placement precedent): given two or more `CsmSnapshotSource` instances of the same repository identity, expose the union of their elements and relationships as one combined `CsmSnapshotSource`, per `design.md` Decision 6 and `spec.md`'s `Combining Observed and Declared CSM Content` requirement. Depends on: existing `CsmSnapshotSource`.
- [ ] 8.2 Reject combining `CsmSnapshotSource` instances with differing repository identities, per that requirement's third scenario. Depends on: 8.1.
- [ ] 8.3 Add tests for: combined source exposes both sources' content; combining requires no dependency between the two producer modules (verified by `aip-declared-knowledge` and `aip-csm-builder` fixtures from two independent test packages); mismatched-repository combination is rejected. Depends on: 8.1, 8.2.

## 9. Determinism and Boundary Enforcement

- [ ] 9.1 Add a test/guard confirming CSM Builder's own construction packages (`aip.csmbuilder.mapper`/`mapping`) remain unaffected and still forbidden from constructing an Architecture Component or Boundary Constraint relationship (re-run `scripts/check-no-excluded-construction.sh`), per `spec.md`'s `Declared Knowledge Is Not Constructed by CSM Builder` requirement. Depends on: none (regression check against existing `aip-csm-builder` code).
- [ ] 9.2 Confirm `aip-declared-knowledge` never depends on `aip-csm-builder`, and `aip-csm-builder` never depends on `aip-declared-knowledge` or references `CsmSnapshotSource`/`CompositeCsmSnapshotSource` (re-run `scripts/check-no-csm-builder-analysis-adapter.sh`, extended to cover the new type name). Depends on: 1.2, 8.1.

## 10. Fixture Layer

- [ ] 10.1 Implement a test-only fixture-building API for baseline `CsmSnapshotSource` content and `Declaration` values, analogous to `aip-analysis`'s/`aip-rules`'s own `CsmSnapshotSourceBuilder`, using `aip-core` types exclusively. Depends on: 1.3.
- [ ] 10.2 Add a build/lint check confirming the fixture package is test-scope only and is never imported by `aip-declared-knowledge`'s production code, mirroring every prior module's own `check-fixture-package-scope` pattern. Depends on: 10.1.

## 11. End-to-End Verification

- [ ] 11.1 Add a fixture-based end-to-end test: declare an Architecture Component composition and a "must not depend on" Boundary Constraint against a fixture baseline, publish, combine with a separate `observed`-only fixture `CsmSnapshotSource` via `CompositeCsmSnapshotSource`, and confirm the combined source is exactly what `BoundaryComplianceRuleType` (`aip-rules`, unmodified) needs to evaluate — demonstrating the full gap this change closes without modifying any existing Rule Type or Agent. Depends on: 5.2, 7.1, 8.1.
- [ ] 11.2 Build a traceability matrix mapping every requirement/scenario in `specs/declared-knowledge-construction/spec.md` to its automated test(s), mirroring `implement-analysis-framework`'s own precedent. Depends on: 2.3, 3.3, 4.2, 4.3, 5.3, 6.3, 7.2, 8.3, 9.1, 9.2, 11.1.

## 12. Final Gate

- [ ] 12.1 Confirm every Decision in `design.md` (Decisions 1–7) holds as implemented, that `canonical-software-model`, `csm-builder`, `analysis-framework`, `rule-framework`, `finding-model`, `agent-framework`, and `architecture-compliance-agent` remain unmodified, and that this change implements no real `aip-csm-builder` → `aip-analysis` adapter, no concrete `Analyzer`, and no `*Store` persistence technology (all explicitly deferred per `proposal.md`). Depends on: all preceding sections.
