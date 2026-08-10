## 1. Foundational Architecture

- [ ] 1.1 Define the Evidence-Kind Mapper contract: per Repository Evidence kind, an input (Evidence Item(s) of that kind) and output (zero or more CSM elements/relationships, each carrying a derived identity and a provenance record) shape, per design.md Decision 1.
- [ ] 1.2 Implement the CSM element/relationship output model, constrained to entity kinds and relationship types already defined by the Canonical Software Model specification only — no new vocabulary of any kind.
- [ ] 1.3 Implement the Evidence-Kind Mapper Registry: registration API and kind-based lookup, mirroring the registry pattern already established in `software-repository-understanding` and `canonical-software-model`. Depends on: 1.1.
- [ ] 1.4 Implement the Mapping Orchestrator: consumes a Repository Evidence Model (or an incremental delta, see 13.1) and dispatches each Evidence Item to its registered Mapper. Depends on: 1.2, 1.3.

## 2. Deterministic Identity Derivation

- [ ] 2.1 Implement CSM element identity derivation for entities with a 1:1 evidence relationship (Repository, Project, Module, Type, Method) as a pure function of the originating Evidence Item's identity. Depends on: 1.1.
- [ ] 2.2 Implement CSM element identity derivation for Package as a function of its containing Module's identity and its native namespace name, independent of which member `SourceUnit` Evidence Items currently populate it. Depends on: 1.1.
- [ ] 2.3 Implement CSM relationship identity derivation as a function of source entity identity, target entity identity, and relationship type, explicitly excluding the dependency-kind qualifier from a dependency relationship's identity. Depends on: 2.1.
- [ ] 2.4 Add identity-stability tests: identity is unchanged across runs when evidence identity is unchanged; Package identity survives member `SourceUnit` add/remove; dependency relationship identity is stable across a kind-qualifier change. Depends on: 2.1, 2.2, 2.3.

## 3. Provenance and Traceability

- [ ] 3.1 Implement Structured Provenance Record construction for every CSM element/relationship: provenance category always `observed`, source reference = originating Repository Evidence Item identity/identities, timestamp = construction/re-derivation time. Depends on: 2.1, 2.2, 2.3.
- [ ] 3.2 Add a construction-time guard that no confidence level of any kind can be attached to a CSM Builder-constructed element or relationship. Depends on: 3.1.
- [ ] 3.3 Implement traceability preservation: every constructed element/relationship carries a reference sufficient to trace it back to its specific originating Repository Evidence Item(s). Depends on: 3.1.

## 4. Structural Entity Mappers

- [ ] 4.1 Implement the `Repository` Evidence Item → CSM `Repository` element Mapper. Depends on: 1.1, 2.1, 3.1.
- [ ] 4.2 Implement the `Project` Evidence Item → CSM `Project` element Mapper, including the reserved unmanaged Project Evidence Item → `observed` CSM `Project` element case, using the same reserved identity. Depends on: 4.1.
- [ ] 4.3 Implement the `Module` Evidence Item → CSM `Module` element Mapper, including RU's single default Module for a sub-module-less Project. Depends on: 4.2.
- [ ] 4.4 Implement the `Package` Evidence Item → CSM `Package` element Mapper. Depends on: 4.3, 2.2.
- [ ] 4.5 Implement the `SourceUnit` Evidence Item → CSM `Type` element Mapper, mapping every native construct kind label (class, struct, interface, record, ...) uniformly onto `Type`. Depends on: 4.4.
- [ ] 4.6 Implement native construct kind label preservation as an attribute on the resulting `Type` element, via the Canonical Software Model's existing native-evidence-attribute mechanism. Depends on: 4.5.
- [ ] 4.7 Implement the Method-level Evidence Item → CSM `Method` element Mapper, invoked only where RU provides Method-level evidence for a given construct. Depends on: 4.5.

## 5. Structural Containment Construction

- [ ] 5.1 Implement containment-relationship construction (Repository→Project→Module→Package→Type[→Method]) directly from Repository Evidence's own structural containment, introducing no containment not present in evidence. Depends on: 4.1–4.7.
- [ ] 5.2 Add containment-traversal tests confirming CSM containment mirrors Repository Evidence's containment 1:1, including the unmanaged-Project and default-Module cases.

## 6. File-Location Handling

- [ ] 6.1 Implement File Evidence Item → source-location attribute mapping on the corresponding `Type`/`Method` element. Depends on: 4.5, 4.7.
- [ ] 6.2 Add a guard and a test proving CSM Builder never constructs a CSM relationship to, or containment involving, a File Evidence Item. Depends on: 6.1.

## 7. Dependency Mapping and Native-Scope Classification

- [ ] 7.1 Implement `ManifestDependencyEdge`/`ImportEdge` → single CSM `dependency` relationship construction per (source, target) pair, covering the manifest-only, source-level-only, and both-present cases. Depends on: 4.3.
- [ ] 7.2 Implement the versioned, per-build-system native-scope-string → CSM dependency-kind (`compile-time`/`runtime`/`test-only`) classification mechanism; an unmapped native scope string SHALL yield an unqualified relationship, never a guessed kind. Depends on: 7.1.
- [ ] 7.3 Populate the initial native-scope mapping table's schema and starter entries (exact, exhaustive table contents are reference data per design.md and may be extended incrementally without blocking on completeness). Depends on: 7.2.
- [ ] 7.4 Add tests for: manifest-only, source-only, both-present, mappable native scope, unmappable native scope. Confirm no source-level corroboration attribute is recorded on the relationship, per the corrected design.md Decision 6. Depends on: 7.1, 7.2, 7.3.

## 8. External System Construction

- [ ] 8.1 Implement the mechanical lookup: does a `ManifestDependencyEdge`'s target artifact coordinate correspond to any `Project`/`Module` Evidence Item discovered in this repository's evidence? Depends on: 4.2, 4.3.
- [ ] 8.2 Implement CSM `External System` element construction (named from the unresolved artifact coordinate), connected by an `integration` relationship — not `dependency` — from the source `Module`. Depends on: 8.1.
- [ ] 8.3 Ensure criticality, integration-protocol, and owner attributes are left unset on a constructed `External System` element. Depends on: 8.2.
- [ ] 8.4 Add a regression test asserting the constructed relationship type is `integration`, guarding against the relationship-type defect caught and corrected in stakeholder review. Depends on: 8.2.

## 9. API Contract Mapping

- [ ] 9.1 Implement `ApiContractDeclaration` → `exposure/consumption` relationship construction, attached to the declaring `Type`/`Method`, carrying the Evidence Item's structural summary as an attribute. Depends on: 4.5, 4.7.
- [ ] 9.2 Enforce that no consumer entity is invented for this relationship unless one already exists as CSM content from other evidence — no `External System` fallback for API contracts, per the corrected design.md Decision 9. Depends on: 9.1.
- [ ] 9.3 Add tests for both the exposure relationship construction and the no-invented-consumer guarantee. Depends on: 9.1, 9.2.

## 10. Explicit Exclusions

- [ ] 10.1 Verify no Mapper is registered for `ConfigFile`/`ConfigReference` Evidence kinds, and add a construction-time guard that fails loudly if one is ever registered accidentally. Depends on: 1.3.
- [ ] 10.2 Add a guard and test proving CSM Builder never constructs an `implementation/extension` or `invocation` relationship under current evidence coverage. Depends on: 1.4.
- [ ] 10.3 Document the "future evidence coverage extends without redesign" path (new Mapper registration only, per 17.x) so the exclusions in 10.1–10.2 are understood as scoped-to-current-evidence, not permanent.

## 11. Partial and Failed Evidence Handling

- [ ] 11.1 Implement CSM element construction from `partial` Repository Evidence using whatever structure was successfully captured, with `observed` provenance and no incompleteness marker of any kind. Depends on: 3.1.
- [ ] 11.2 Implement non-construction for `failed` Repository Evidence Items — no corresponding CSM element. Depends on: 1.4.
- [ ] 11.3 Add tests confirming a `partial` evidence item yields a CSM element indistinguishable in shape/provenance from one built from `complete` evidence, and a `failed` evidence item yields none. Depends on: 11.1, 11.2.

## 12. Versioned Snapshot Construction

- [ ] 12.1 Implement snapshot-per-run construction: each CSM Builder run produces a new, individually identifiable CSM snapshot rather than mutating a shared graph in place. Depends on: 1.4, 4.1–4.7, 5.1.
- [ ] 12.2 Implement a snapshot persistence/identification abstraction sufficient to keep prior snapshots retrievable and unaffected by new ones — the concrete storage technology remains a deferred choice per design.md; implement against the abstraction only. Depends on: 12.1.
- [ ] 12.3 Add tests confirming multiple runs produce distinguishable snapshots and that prior snapshots remain retrievable and untouched after a new snapshot is constructed. Depends on: 12.1, 12.2.

## 13. Incremental Re-derivation

- [ ] 13.1 Implement per-run construction scoping keyed to Repository Evidence's `ADDED`/`UNCHANGED`/`MODIFIED`/`REMOVED` change-status classification: `UNCHANGED`-derived elements are carried forward without re-invoking their Mapper; `ADDED`/`MODIFIED`-derived elements are (re)constructed. Depends on: 12.1.
- [ ] 13.2 Implement omission of a CSM element from the new snapshot once its sole originating evidence's change status is `REMOVED`, without waiting for the `PURGED` lifecycle state. Depends on: 13.1.
- [ ] 13.3 Add tests covering: unchanged evidence not re-derived; changed evidence re-derived; removed evidence omitted immediately on `REMOVED` classification; `TOMBSTONED`-but-not-yet-`PURGED` evidence also omitted. Depends on: 13.1, 13.2.

## 14. CSM Builder Mapper Versioning

- [ ] 14.1 Implement an internal, monotonically versioned identifier per Evidence-Kind Mapper. Depends on: 1.3.
- [ ] 14.2 Implement re-derivation eligibility when a Mapper's version differs from the version recorded against a previously constructed element, independent of the underlying evidence's own change status. Depends on: 14.1, 13.1.
- [ ] 14.3 Add a test simulating a Mapper version change against unchanged evidence, confirming re-derivation occurs and the resulting provenance timestamp reflects it. Depends on: 14.2, 3.1.

## 15. Conflict and Precedence Integration

- [ ] 15.1 Integrate with the Canonical Software Model's existing Subject Identification and Same-Category Conflict Marking mechanism for the case of two `observed` assertions CSM Builder constructs about the same subject that conflict. Depends on: 3.1.
- [ ] 15.2 Verify CSM Builder performs no conflict arbitration of its own outside that existing mechanism (no CSM-Builder-invented precedence logic). Depends on: 15.1.
- [ ] 15.3 Add a test constructing two conflicting `observed` assertions about the same subject and confirming both are preserved, with the subject marked `CONFLICTED` via the existing CSM mechanism. Depends on: 15.1, 15.2.

## 16. Snapshot Validation

- [ ] 16.1 Integrate the Canonical Software Model's existing `CSM Validation Expectations` check as a gate applied to every constructed snapshot before it is considered usable. Depends on: 12.1.
- [ ] 16.2 Implement non-publication of a snapshot that fails validation. Depends on: 16.1.
- [ ] 16.3 Add tests for both a passing and a failing validation outcome. Depends on: 16.1, 16.2.

## 17. Extension Mechanism for Future Evidence Kinds

- [ ] 17.1 Confirm and document that registering a new Evidence-Kind Mapper for a currently-unsupported Repository Evidence kind requires no change to the identity-derivation (2.x), provenance-construction (3.x), or incremental-scoping (13.x) mechanisms. Depends on: 1.3, 2.1–2.3, 3.1, 13.1.
- [ ] 17.2 Add a test registering a stub/no-op Mapper for a hypothetical new Evidence kind and confirming core mechanisms are unaffected. Depends on: 17.1.

## 18. Requirement/Scenario Traceability and Test Coverage

- [ ] 18.1 Build a traceability matrix mapping each of the 29 requirements and 52 scenarios in `specs/csm-builder/spec.md` to its corresponding automated test(s).
- [ ] 18.2 Add contract tests for the Evidence-Kind Mapper interface itself (input/output shape, identity and provenance obligations), independent of any specific Mapper implementation. Depends on: 1.1.
- [ ] 18.3 Add end-to-end integration tests running the full Mapping Orchestrator against a representative multi-language, multi-Module sample Repository Evidence Model. Depends on: 1.4, 4.1–4.7, 5.1, 7.x, 8.x, 9.x, 12.1, 13.1.
- [ ] 18.4 Add regression tests specifically for the two defects caught in stakeholder review: External System construction uses `integration` (not `dependency`), and API Contract construction never invents a consumer. Depends on: 8.4, 9.3.
- [ ] 18.5 Add a CI-level check confirming no test or implementation source references a CSM entity kind, relationship type, provenance category, or confidence level outside the Canonical Software Model's already-approved vocabulary. Depends on: 1.2.

## 19. Shape A Boundary Enforcement

- [ ] 19.1 Add static/lint-level or test-level guards confirming no AI/LLM API call, heuristic scoring, or probabilistic-inference code path exists anywhere in the CSM Builder implementation.
- [ ] 19.2 Add tests confirming CSM Builder never constructs an Architecture Component, Architectural Boundary, or Business Context element under any input. Depends on: 1.2.
- [ ] 19.3 Add tests confirming CSM Builder construction never reads from, or otherwise depends on, the Policy/Rule Model or the Runtime Model. Depends on: 1.4.
