## 1. Build and Module Scaffolding

- [x] 1.1 Create the parent Maven project (Java 21+) with `aip-core` and `aip-csm-builder` modules, per `project.md` §10 and `design.md` Decision 1.
- [x] 1.2 Configure `aip-csm-builder`'s POM to depend on `aip-core` only — no dependency on `aip-analyzer` or any Repository Understanding implementation module (invariants 1, 2). Depends on: 1.1.
- [x] 1.3 Add a dependency-graph CI check that fails the build if `aip-csm-builder` gains any non-test dependency outside `aip-core` (invariants 1, 2). Depends on: 1.2.
- [x] 1.4 Set up shared test infrastructure and directory conventions for both modules. Depends on: 1.1.

## 2. `aip-core`: CSM Domain Model

- [x] 2.1 Implement the `aip.core.csm` package: CSM entity kinds (`Repository`, `Project`, `Module`, `Package`, `Type`, `Method`, `Architecture Component`, `External System`) per `canonical-software-model/spec.md`'s vocabulary. Depends on: 1.1.
- [x] 2.2 Implement the CSM relationship-type vocabulary (containment, dependency, implementation/extension, invocation, exposure/consumption, integration, composition, boundary/constraint) as a closed, versioned set. Depends on: 2.1.
- [x] 2.3 Implement the Structured Provenance Record type and the closed provenance-category enumeration (`observed`/`declared`/`inferred`). Depends on: 2.1.
- [x] 2.4 Implement the confidence representation (`HIGH`/`MEDIUM`/`LOW`), reserved for `inferred` elements — present in the domain model for completeness, not exercised by CSM Builder itself. Depends on: 2.3.
- [x] 2.5 Implement CSM element/relationship identity types and the native-evidence-attribute-bag mechanism. Depends on: 2.1, 2.2.
- [x] 2.6 Add tests confirming the CSM domain model's vocabulary is closed to exactly what `canonical-software-model/spec.md` defines — no accidental extra entity or relationship kind. Depends on: 2.1, 2.2.

## 3. `aip-core`: Repository Evidence Contract

- [x] 3.1 Implement the `aip.core.evidence` package: the Evidence Item base type and the Evidence-kind enumeration (`Repository`, `Project`, `Module`, `Package`, `SourceUnit`, Method-level, `ManifestDependencyEdge`, `ImportEdge`, `ApiContractDeclaration`, `ConfigFile`, `ConfigReference`, `File`), per `software-repository-understanding/spec.md`. Depends on: 1.1.
- [x] 3.2 Implement the Evidence identity type (repository identifier, Evidence kind, scope key) and the source-location type, kept structurally distinct per the archived RU spec. Depends on: 3.1.
- [x] 3.3 Implement the discovery-outcome-status vocabulary (`complete`/`partial`/`failed`) and the failure-reason taxonomy. Depends on: 3.1.
- [x] 3.4 Implement the extraction-method-tag vocabulary. Depends on: 3.1.
- [x] 3.5 Implement the change-status vocabulary (`ADDED`/`UNCHANGED`/`MODIFIED`/`REMOVED`) and the lifecycle-state vocabulary (`PRESENT`/`TOMBSTONED`/`PURGED`) as two distinct, non-interchangeable types. Depends on: 3.1.
- [x] 3.6 Add tests confirming this is the single, authoritative Repository Evidence contract — no duplicate or parallel Evidence type exists anywhere else in the codebase (invariant 3). Depends on: 3.1–3.5.

## 4. CSM Builder Foundational Architecture

- [x] 4.1 Define the Evidence-Kind Mapper contract/interface in `aip-csm-builder`, depending only on `aip.core.evidence` (input) and `aip.core.csm` (output) types. Depends on: 2.5, 3.6.
- [x] 4.2 Implement the Evidence-Kind Mapper Registry (registration API, kind-based lookup). Depends on: 4.1.
- [x] 4.3 Implement the Mapping Orchestrator: consumes a Repository Evidence Model (or incremental delta, see Section 16) and dispatches Evidence Items to registered Mappers in deterministic, sorted-by-identity order — sequential execution only, no concurrency infrastructure (invariant 7, `design.md` Decision 4). Depends on: 4.2.
- [x] 4.4 Add contract tests for the Mapper interface itself, independent of any specific Mapper implementation. Depends on: 4.1.

## 5. Deterministic Identity Derivation

- [x] 5.1 Implement CSM element identity derivation for 1:1-evidence entities as a pure function of Evidence identity. Depends on: 4.1.
- [x] 5.2 Implement CSM element identity derivation for `Package` (function of containing Module identity + native namespace name, independent of member `SourceUnit` evidence). Depends on: 5.1.
- [x] 5.3 Implement CSM relationship identity derivation (source entity identity, target entity identity, relationship type — excluding the dependency-kind qualifier). Depends on: 5.1.
- [x] 5.4 Add identity-stability tests covering unchanged-evidence stability, Package identity survival across member changes, and dependency-relationship identity stability across a kind-qualifier change. Depends on: 5.1–5.3.

## 6. Provenance and Traceability

- [x] 6.1 Implement Structured Provenance Record construction: `observed` always, source reference = originating Evidence identity/identities, construction/re-derivation timestamp. Depends on: 5.1, 2.3.
- [x] 6.2 Add a construction-time guard rejecting any attempt to attach a confidence level to CSM Builder-constructed content. Depends on: 6.1.
- [x] 6.3 Implement traceability-reference preservation on every constructed element/relationship. Depends on: 6.1.

## 7. Structural Entity Mappers

- [x] 7.1 Implement the `Repository` → CSM `Repository` Mapper. Depends on: 4.1, 5.1, 6.1.
- [x] 7.2 Implement the `Project` → CSM `Project` Mapper, including the reserved unmanaged Project case. Depends on: 7.1.
- [x] 7.3 Implement the `Module` → CSM `Module` Mapper, including RU's default-Module case for sub-module-less Projects. Depends on: 7.2.
- [x] 7.4 Implement the `Package` → CSM `Package` Mapper. Depends on: 7.3, 5.2.
- [x] 7.5 Implement the `SourceUnit` → CSM `Type` Mapper, mapping every native construct kind label uniformly onto `Type`. Depends on: 7.4.
- [x] 7.6 Implement native construct kind label preservation as a `Type` attribute. Depends on: 7.5.
- [x] 7.7 Implement the Method-level Evidence → CSM `Method` Mapper. Depends on: 7.5.

## 8. Structural Containment Construction

- [x] 8.1 Implement containment-relationship construction across the full chain (Repository→Project→Module→Package→Type[→Method]) directly from Repository Evidence's own containment. Depends on: 7.1–7.7.
- [x] 8.2 Add containment-traversal tests, including the unmanaged-Project and default-Module cases. Depends on: 8.1.

## 9. File-Location Handling

- [x] 9.1 Implement File Evidence → source-location attribute mapping on the corresponding `Type`/`Method` element. Depends on: 7.5, 7.7.
- [x] 9.2 Add a guard/test proving no CSM relationship or containment edge is ever constructed to a File Evidence Item. Depends on: 9.1.

## 10. Dependency Mapping and Kind-Classification Abstraction

- [x] 10.1 Define the Dependency-Kind Classifier abstraction (interface), decoupled from any specific mapping-table implementation (invariant 6, `design.md` Decision 3). Depends on: 4.1.
- [x] 10.2 Implement the versioned, declarative per-build-system mapping-resource format and loader behind that abstraction. Depends on: 10.1.
- [x] 10.3 Populate initial mapping resources with starter entries (exhaustive coverage is reference data, extended incrementally, not blocking). Depends on: 10.2.
- [x] 10.4 Implement `ManifestDependencyEdge`/`ImportEdge` → single CSM `dependency` relationship construction, covering the manifest-only, source-only, and both-present cases. Depends on: 7.3.
- [x] 10.5 Wire the Dependency-Kind Classifier into dependency relationship construction; an unmapped native scope string SHALL yield no kind qualifier, never a guess. Depends on: 10.2, 10.4.
- [x] 10.6 Add tests for manifest-only, source-only, both-present, mappable scope, unmappable scope, and confirm no corroboration attribute is recorded (per the corrected specification). Depends on: 10.4, 10.5.

## 11. External System Construction

- [ ] 11.1 Implement the mechanical unresolved-target lookup (does a manifest-declared target correspond to a known Project/Module identity?). Depends on: 7.2, 7.3.
- [ ] 11.2 Implement `External System` element construction connected by an `integration` relationship — not `dependency` — from the source `Module`. Depends on: 11.1.
- [ ] 11.3 Leave criticality, integration-protocol, and owner attributes unset on constructed `External System` elements. Depends on: 11.2.
- [ ] 11.4 Add a regression test asserting the constructed relationship type is `integration`, guarding the defect corrected in stakeholder review. Depends on: 11.2.

## 12. API Contract Mapping

- [ ] 12.1 Implement `ApiContractDeclaration` → `exposure/consumption` relationship construction, attached to the declaring `Type`/`Method`. Depends on: 7.5, 7.7.
- [ ] 12.2 Enforce that no consumer entity is invented unless one is already evidenced elsewhere in CSM content. Depends on: 12.1.
- [ ] 12.3 Add tests for both the exposure relationship construction and the no-invented-consumer guarantee. Depends on: 12.1, 12.2.

## 13. Explicit Exclusions

- [ ] 13.1 Verify no Mapper is ever registered for `ConfigFile`/`ConfigReference` Evidence kinds; add a construction-time guard that fails loudly if one is. Depends on: 4.2.
- [ ] 13.2 Add a guard/test proving CSM Builder never constructs an `implementation/extension` or `invocation` relationship under current evidence coverage. Depends on: 4.3.
- [ ] 13.3 Document, in code and in the traceability matrix (Section 23), the "future evidence coverage extends via new Mapper registration only" path. Depends on: 13.1, 13.2.

## 14. Partial and Failed Evidence Handling

- [ ] 14.1 Implement CSM element construction from `partial` Repository Evidence using whatever structure was successfully captured, with `observed` provenance and no incompleteness marker. Depends on: 6.1.
- [ ] 14.2 Implement non-construction for `failed` Repository Evidence Items. Depends on: 4.3.
- [ ] 14.3 Add tests confirming a `partial` evidence item yields a CSM element indistinguishable in shape/provenance from one built from `complete` evidence, and a `failed` evidence item yields none. Depends on: 14.1, 14.2.

## 15. Snapshot Persistence Abstraction and Filesystem Implementation

- [ ] 15.1 Define a Snapshot Store abstraction (interface) so the persistence mechanism can be replaced later without changing CSM Builder's construction logic (invariant 5, `design.md` Decision 2). Depends on: 4.3.
- [ ] 15.2 Implement the filesystem-based Snapshot Store: one immutable directory per snapshot, keyed by repository identifier and a monotonically increasing sequence number. Depends on: 15.1.
- [ ] 15.3 Implement the Snapshot Manifest (CSM element identity → Mapper/Mapper-version/originating-evidence-identity/content-location index). Depends on: 15.2.
- [ ] 15.4 Add tests, written against the Snapshot Store abstraction (not the filesystem implementation directly), confirming multiple runs produce distinguishable snapshots and prior snapshots remain retrievable and untouched. Depends on: 15.1, 15.2, 15.3.

## 16. Incremental Re-derivation

- [ ] 16.1 Implement per-run construction scoping keyed to Repository Evidence's change-status classification: `UNCHANGED`-derived elements carried forward without re-invoking their Mapper; `ADDED`/`MODIFIED`-derived elements (re)constructed. Depends on: 15.3.
- [ ] 16.2 Implement omission of a CSM element from the new snapshot once its sole originating evidence's change status is `REMOVED`, without waiting for the `PURGED` lifecycle state. Depends on: 16.1.
- [ ] 16.3 Add tests covering unchanged, changed, removed, and `TOMBSTONED`-but-not-yet-`PURGED` cases. Depends on: 16.1, 16.2.

## 17. CSM Builder Mapper Versioning

- [ ] 17.1 Implement an internal, monotonically versioned identifier per Evidence-Kind Mapper. Depends on: 4.2.
- [ ] 17.2 Implement re-derivation eligibility when a Mapper's version differs from the version recorded against a previously constructed element, independent of evidence change status. Depends on: 17.1, 16.1.
- [ ] 17.3 Add a test simulating a Mapper version change against unchanged evidence, confirming re-derivation occurs. Depends on: 17.2.

## 18. Conflict and Precedence Integration

- [ ] 18.1 Integrate with the CSM domain model's existing Subject Identification and Same-Category Conflict Marking mechanism (Section 2) for conflicting `observed` assertions CSM Builder constructs. Depends on: 6.1, 2.3.
- [ ] 18.2 Verify CSM Builder performs no conflict arbitration of its own outside that existing mechanism. Depends on: 18.1.
- [ ] 18.3 Add a test constructing two conflicting `observed` assertions about the same subject, confirming both are preserved and the subject is marked `CONFLICTED`. Depends on: 18.1, 18.2.

## 19. Snapshot Validation

- [ ] 19.1 Implement a validator applying the CSM domain model's validation expectations to a constructed snapshot before it is considered usable. Depends on: 15.2, 2.1–2.5.
- [ ] 19.2 Implement non-publication of a snapshot that fails validation. Depends on: 19.1.
- [ ] 19.3 Add tests for both a passing and a failing validation outcome. Depends on: 19.1, 19.2.

## 20. Extension Mechanism Verification

- [ ] 20.1 Confirm and document that registering a new Evidence-Kind Mapper requires no change to identity-derivation, provenance-construction, or incremental-scoping mechanisms. Depends on: 4.2, 5.1–5.3, 6.1, 16.1.
- [ ] 20.2 Add a test registering a stub/no-op Mapper for a hypothetical new Evidence kind, confirming core mechanisms are unaffected. Depends on: 20.1.

## 21. Fixture Layer

- [ ] 21.1 Implement the `RepositoryEvidenceModelBuilder` fixture-building API in `aip-csm-builder`'s test sources, using `aip.core.evidence` types exclusively (invariants 3, 4). Depends on: 3.1–3.5.
- [ ] 21.2 Add a build/lint check confirming the fixture package is test-scope only and is never imported by CSM Builder's production code (invariant 4). Depends on: 21.1.
- [ ] 21.3 Build a named fixture-scenario library (single Module, multi-module Project, unmanaged files, partial evidence, mappable/unmappable dependency scope, removed/tombstoned evidence across two runs, etc.), each traced to its originating RU specification requirement/scenario. Depends on: 21.1.

## 22. Test Organization, Naming, and Reserved Integration Seam

- [ ] 22.1 Establish package/naming conventions: unit tests, `EvidenceKindMapperContractTest`-style contract tests, and `*FixtureTest`/`*FixtureEndToEndTest` fixture-based end-to-end tests. Depends on: 21.1.
- [ ] 22.2 Add a naming-convention lint/CI check rejecting any test name containing "RepositoryUnderstanding" or "RU" in a way that implies real-pipeline integration coverage (invariant 8). Depends on: 22.1.
- [ ] 22.3 Reserve the future `RepositoryToCsmPipelineIntegrationTest` name and location with a documented pending-work marker (no test body), referencing the future `implement-software-repository-understanding` change (invariants 8, 9). Depends on: 22.1.

## 23. Requirement/Scenario Traceability and Coverage

- [ ] 23.1 Build a traceability matrix mapping each of the 29 requirements / 52 scenarios in `openspec/specs/csm-builder/spec.md` to its automated test(s).
- [ ] 23.2 Add end-to-end fixture-based tests running the full Mapping Orchestrator against representative multi-language, multi-Module fixture scenarios. Depends on: 21.3, 8.1, 10.4–10.6, 11.1–11.4, 12.1–12.3, 15.1–15.4, 16.1–16.3.
- [ ] 23.3 Add regression tests specifically for the two defects corrected in stakeholder review: External System uses `integration` (not `dependency`), and API Contract construction never invents a consumer. Depends on: 11.4, 12.3.
- [ ] 23.4 Add a CI check confirming no test or implementation source references a CSM entity kind, relationship type, provenance category, or confidence level outside the archived vocabulary. Depends on: 2.1–2.5.

## 24. Architectural Invariant Enforcement

- [ ] 24.1 Finalize the dependency-graph CI check enforcing invariants 1 and 2 (`aip-csm-builder` depends on `aip-core` only; never `aip-analyzer` or a Repository Understanding implementation module). Depends on: 1.3.
- [ ] 24.2 Add a source-scan or module-boundary check enforcing invariant 3 (no duplicate/parallel Evidence DTOs outside `aip.core.evidence`). Depends on: 3.6.
- [ ] 24.3 Add tests confirming no AI/LLM call, heuristic scoring, or probabilistic-inference code path exists anywhere in `aip-csm-builder`. Depends on: 1.1.
- [ ] 24.4 Add tests confirming CSM Builder never constructs an Architecture Component, Architectural Boundary, or Business Context element under any input. Depends on: 2.1.
- [ ] 24.5 Add tests confirming CSM Builder construction never reads from, or depends on, a Policy/Rule Model or Runtime Model. Depends on: 4.3.
- [ ] 24.6 Final gate: confirm (via review checklist and CI) that this change contains no Repository Understanding implementation code, that both archived specifications remain unmodified, and that all invariants 1–10 hold. Depends on: all preceding sections.
