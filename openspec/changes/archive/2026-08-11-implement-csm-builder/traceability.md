# Requirement/Scenario Traceability Matrix

tasks.md 23.1: every requirement and scenario in
`openspec/specs/csm-builder/spec.md` (29 requirements, 52 scenarios),
mapped to the automated test(s) exercising it. Test references are
`ClassName.methodName`; unqualified class names live in
`aip-csm-builder/src/test/java/aip/csmbuilder/...` unless marked
`(aip-core)`. Where a requirement is structurally guaranteed rather
than actively tested (e.g. by Java's closed-enum/sealed-type system,
or by this module's dependency graph), that is stated explicitly rather
than pointing at a test that would always trivially pass.

Kept as a standalone file (not inlined into `tasks.md`) since it is a
coverage artifact to maintain going forward, not a one-time task
checklist entry.

## Deterministic, Evidence-Driven Transformation Only

| Scenario | Test(s) |
|---|---|
| No AI or heuristic judgment used to construct CSM content | Architecturally guaranteed: `aip-csm-builder`'s dependency graph (`scripts/check-module-dependencies.sh`) permits only `aip-core`, so no AI/ML/heuristic library is even reachable. See also tasks.md 24.3 (pending, Section 24). |
| Repeated construction from unchanged evidence is identical | `MappingOrchestratorTest.repeatedRunsAgainstUnchangedInputProduceIdenticalDispatchOrder` |

## Repository Evidence as Sole Construction Input

| Scenario | Test(s) |
|---|---|
| Construction proceeds without policy or runtime data | Architecturally guaranteed: `MappingOrchestrator.construct`'s signature accepts only `RepositoryEvidenceModel`/`ChangeStatus`/`PriorElementLookup`; no Policy/Rule Model or Runtime Model type exists anywhere in `aip-core` for it to depend on. See also tasks.md 24.5 (pending, Section 24). |
| No policy or runtime dependency in construction | Same as above. |

## Exclusion of Architectural Inference and Declared-Knowledge Construction

| Scenario | Test(s) |
|---|---|
| No Architecture Component constructed | Pending tasks.md 24.4 (Section 24) for an exhaustive, all-inputs test. Partially structural today: no Mapper or relationship builder in `aip.csmbuilder.mapper`/`aip.csmbuilder.mapping` ever constructs an `ArchitectureComponentElement`. |
| No Architectural Boundary constructed | Pending tasks.md 24.4. Structural today: no construction path ever produces a `BOUNDARY_CONSTRAINT` relationship. |
| No Business Context element constructed | Structurally impossible: Business Capability/Domain/Ownership/Criticality are not CSM entity kinds at all (see `aip.core.csm.package-info`'s own "out of scope" note) — there is no type to construct. |
| No declared or inferred provenance ever assigned | `ProvenanceGuardTest.declaredProvenanceIsRejected`, `ProvenanceGuardTest.confidenceOnDeclaredProvenanceIsRejected`, `MappingOrchestratorTest.orchestratorRejectsAMisbehavingMapperRegardlessOfImplementation` |

## Exclusion of New CSM Vocabulary

| Scenario | Test(s) |
|---|---|
| Only existing CSM vocabulary used | `CsmVocabularyClosureTest.entityKindsMatchExactlyWhatTheArchivedSpecDefines`, `.relationshipTypesMatchExactlyWhatTheArchivedSpecDefines` (aip-core); structurally reinforced by `CsmElement`/`CsmEntityKind`/`CsmRelationshipType` being sealed/closed. See also tasks.md 23.4. |
| Unmappable evidence does not create new vocabulary | `NativeAttributes`-based escape hatch is what every Mapper uses instead — see `TypeMapperTest.everyNativeConstructKindMapsUniformlyOntoType` (native construct kind preserved as an attribute, never a new entity kind). |

## Structural Entity Mapping

| Scenario | Test(s) |
|---|---|
| Structural evidence maps to corresponding CSM entities | `RepositoryMapperTest.mapsRepositoryEvidenceToARepositoryElement`, `ProjectMapperTest.mapsADeclaredProjectToAProjectElement`, `ModuleMapperTest.mapsADeclaredSubModuleToAModuleElement`, `PackageMapperTest.mapsPackageToAPackageElementIdentifiedByItsContainingModuleAndNamespace`, `TypeMapperTest.identityAndNameDeriveFromEvidence`, `MethodMapperTest.mapsMethodEvidenceToAMethodElement` |
| Every native source-construct kind maps to Type | `TypeMapperTest.everyNativeConstructKindMapsUniformlyOntoType` |

## Native Construct Kind Preserved as Attribute

| Scenario | Test(s) |
|---|---|
| Native kind label retained on the Type element | `TypeMapperTest.everyNativeConstructKindMapsUniformlyOntoType` |

## Unmanaged Evidence Mapping

| Scenario | Test(s) |
|---|---|
| Unmanaged Project evidence produces a CSM Project element | `ProjectMapperTest.mapsTheReservedUnmanagedProjectWithNoSpecialCasing`, `FixtureScenariosTest.unmanagedFilesConstructUnderTheUnmanagedProject` |

## CSM Element Identity Derivation

| Scenario | Test(s) |
|---|---|
| CSM identity stable when evidence identity is stable | `ElementIdentityDeriverTest.sameEvidenceIdAlwaysProducesTheSameCsmIdentity` |
| Package identity survives member evidence changes | `ElementIdentityDeriverTest.packageIdentitySurvivesMemberEvidenceChanges`, `.packageIdentityDependsOnContainingModuleAndNamespaceOnly`, `PackageMapperTest.packageIdentityIsIndependentOfDispatchOrderRelativeToItsModule` |

## CSM Relationship Identity Derivation

| Scenario | Test(s) |
|---|---|
| Dependency relationship identity stable when kind qualifier changes | `ElementIdentityDeriverTest.dependencyRelationshipIdentityIsStableAcrossAKindQualifierChange`, `DependencyRelationshipTest.dependencyRelationshipIdentityExcludesTheKindQualifier` |

## Provenance Assignment for Constructed Knowledge

| Scenario | Test(s) |
|---|---|
| Constructed element carries an observed provenance record | `ObservedProvenanceFactoryTest.singleEvidenceProvenanceIsAlwaysObservedWithNoConfidence`; every `*MapperTest` also asserts `ProvenanceCategory.OBSERVED` on its own element. |
| No confidence assigned to constructed knowledge | `ObservedProvenanceFactoryTest.singleEvidenceProvenanceIsAlwaysObservedWithNoConfidence` (the factory's signature admits none), `ProvenanceGuardTest.confidenceOnDeclaredProvenanceIsRejected` (defense-in-depth) |

## Structural Containment Construction

| Scenario | Test(s) |
|---|---|
| Evidence containment produces corresponding CSM containment | `ContainmentTraversalTest.fullChainProducesContainmentRelationshipsAtEveryLevel`, `.unmanagedProjectContainmentIsConstructedLikeAnyOther`, `.defaultModuleContainmentIsConstructedLikeAnyOther` |

## File Evidence Becomes a Location Attribute, Not a Relationship

| Scenario | Test(s) |
|---|---|
| File reference becomes a source-location attribute | `FileLocationResolverTest.sourceLocationIsResolvedFromTheReferencedFileEvidenceItem`, `.sourceLocationIsEmptyWhenNoFileReferenceExists`, `.noCsmRelationshipIsEverConstructedToOrFromAFileEvidenceItem` |

## Dependency Relationship Construction

| Scenario | Test(s) |
|---|---|
| Manifest-only evidence produces a dependency relationship | `DependencyRelationshipTest.manifestOnlyEdgeWithMappableScopeProducesAQualifiedDependency` |
| Source-level-only evidence produces a dependency relationship | `DependencyRelationshipTest.sourceOnlyEdgeProducesAnUnqualifiedDependency` |
| Both evidence kinds produce a single relationship | `DependencyRelationshipTest.bothEdgesPresentProduceExactlyOneRelationshipWithTheManifestKind` |

## Dependency Kind Classification

| Scenario | Test(s) |
|---|---|
| Mappable native scope produces a kind-qualified relationship | `DependencyRelationshipTest.manifestOnlyEdgeWithMappableScopeProducesAQualifiedDependency`, `ResourceDependencyKindClassifierTest.mappableMavenScopesClassifyCorrectly`, `.mappableNpmScopesClassifyCorrectly` |
| Unmappable native scope produces an unqualified relationship | `DependencyRelationshipTest.unmappableNativeScopeYieldsNoKindQualifierNeverAGuess`, `ResourceDependencyKindClassifierTest.unmappableScopeYieldsEmptyNeverAGuess` |

## External System Construction for Unresolved Dependencies

| Scenario | Test(s) |
|---|---|
| Unresolved dependency target produces an External System element | `ExternalSystemRelationshipTest.unresolvedManifestTargetProducesExternalSystemAndIntegrationRelationship` |
| External System attributes left unset when unknown | `ExternalSystemRelationshipTest.externalSystemAttributesLeftUnset` |

## Exclusion of Configuration Reference Representation

| Scenario | Test(s) |
|---|---|
| No CSM element constructed from configuration evidence | `ConfigurationExclusionTest.configFileEvidenceProducesNoCsmElementOrRelationship`, `.configReferenceEvidenceProducesNoCsmElementOrRelationship`, `EvidenceKindMapperRegistryTest.registeringAMapperForConfigFileIsRejected`, `.registeringAMapperForConfigReferenceIsRejected` |
| Configuration evidence remains valid without CSM representation | `ConfigurationExclusionTest` (both methods) — a `CONFIG_FILE`/`CONFIG_REFERENCE` item runs through `RepositoryEvidenceModel.of`/`MappingOrchestrator.construct` without error, producing simply no CSM content. |

## API Contract Relationship Construction

| Scenario | Test(s) |
|---|---|
| API contract evidence produces an exposure relationship | `ApiContractMapperTest.apiContractEvidenceProducesAnExposureRelationshipOnTheDeclaringType` |
| No invented consumer for an API contract | `ApiContractMapperTest.noConsumerEntityIsEverInvented` (regression test, tasks.md 23.3 — see design.md Decision 7's "no invented consumer" correction) |

## Exclusion of Relationship Types Without Corresponding Evidence

| Scenario | Test(s) |
|---|---|
| No implementation/extension relationship constructed | `ExcludedRelationshipTypeGuardTest.implementationExtensionRelationshipIsRejected` |
| No invocation relationship constructed | `ExcludedRelationshipTypeGuardTest.invocationRelationshipIsRejected` |
| Future evidence coverage extends without redesign | `ExtensionMechanismTest.registeringANewMapperLeavesExistingConstructionUnaffected`, `.incrementalScopingAppliesToTheNewKindWithoutAnyChange` |

## Partial-Evidence Construction

| Scenario | Test(s) |
|---|---|
| Partial evidence still produces a CSM element | `PartialAndFailedEvidenceTest.partialEvidenceProducesAnElementIndistinguishableFromComplete`, `FixtureScenariosTest.partialEvidenceStillConstructsAnElement` |
| No incompleteness marker attached to the CSM element | `PartialAndFailedEvidenceTest.partialEvidenceProducesAnElementIndistinguishableFromComplete` |

## Failed-Evidence Non-Construction

| Scenario | Test(s) |
|---|---|
| Failed evidence produces no CSM element | `PartialAndFailedEvidenceTest.failedEvidenceProducesNoElement`, `.failedManifestDependencyEdgeProducesNoDependencyRelationship`, `.failedManifestDependencyEdgeToAnUnresolvedTargetProducesNoExternalSystem` |

## Snapshot-Based CSM Construction

| Scenario | Test(s) |
|---|---|
| Each run produces a distinguishable snapshot | `FilesystemSnapshotStoreTest.multipleRunsProduceDistinguishableSnapshots` |
| Prior snapshots remain retrievable | `FilesystemSnapshotStoreTest.priorSnapshotsRemainRetrievableAndUnaffectedByANewWrite` |

## Incremental Snapshot Scope

| Scenario | Test(s) |
|---|---|
| Unchanged evidence is carried forward without re-derivation | `IncrementalConstructionTest.unchangedItemIsCarriedForwardWithoutInvokingItsMapper` |
| Changed evidence triggers re-derivation | `IncrementalConstructionTest.addedItemIsConstructedFresh`, `.modifiedItemIsConstructedFresh` |

## Evidence Lifecycle Interaction

| Scenario | Test(s) |
|---|---|
| Removed evidence omits the corresponding element | `IncrementalConstructionTest.removedItemIsOmittedAndItsMapperIsNeverInvoked`, `FixtureScenariosTest.removedAndTombstonedAcrossTwoRunsOmitsTheModuleFromTheSecondSnapshot` |
| CSM Builder does not wait for purge | `IncrementalConstructionTest.tombstonedButNotYetPurgedIsOmittedWithoutWaitingForPurge`, `.purgedIsOmittedTheSameWayAsTombstoned` |

## CSM Builder Mapper Versioning

| Scenario | Test(s) |
|---|---|
| Mapper version change triggers re-derivation | `MapperVersioningTest.mapperVersionChangeTriggersReDerivationOfUnchangedEvidence` |

## Conflict Marking Reuse

| Scenario | Test(s) |
|---|---|
| Conflicting observed assertions are preserved and marked | `ConflictedSubjectsTest.twoConflictingObservedRelationshipsAreBothPreservedAndTheirSubjectIsConflicted`, `SubjectConflictMarkerTest.twoConflictingObservedAssertionsForTheSameSubjectAreBothPreservedAndMarkedConflicted` (aip-core) |
| CSM Builder does not arbitrate conflicts | `ConflictedSubjectsTest.twoConflictingObservedRelationshipsAreBothPreservedAndTheirSubjectIsConflicted` (its `MappingResult.merge` assertions prove no arbitration occurs at the one place CSM Builder combines results) |

## Snapshot Validation Before Use

| Scenario | Test(s) |
|---|---|
| Valid snapshot is usable | `SnapshotPublisherTest.validSnapshotIsPublished`, `CsmValidatorTest.ordinaryObservedElementsAndRelationshipsAreValid` (aip-core) |
| Invalid snapshot is not published | `SnapshotPublisherTest.invalidSnapshotIsNotPublished`, `CsmValidatorTest.observedBoundaryConstraintRelationshipIsInvalid` (aip-core) |

## CSM Element Identity Stability Across Snapshots

| Scenario | Test(s) |
|---|---|
| Element identity persists across snapshots | `ElementIdentityDeriverTest.sameEvidenceIdAlwaysProducesTheSameCsmIdentity` — identity derivation is a pure function of Evidence identity with no time-varying state, so stability at a single point in time and stability across any number of snapshots are the same property. |

## Extension Mechanism for New Evidence Kinds

| Scenario | Test(s) |
|---|---|
| New evidence kind added without core mechanism changes | `ExtensionMechanismTest.registeringANewMapperLeavesExistingConstructionUnaffected`, `.incrementalScopingAppliesToTheNewKindWithoutAnyChange` |

## Evidence Traceability Preservation

| Scenario | Test(s) |
|---|---|
| CSM element traceable to its originating evidence | `ProvenanceGuardTest.mismatchedSourceReferenceIsRejected`, `.joinedMultiEvidenceSourceReferenceContainingTheDrivingItemIsTraceable`, `ObservedProvenanceFactoryTest.multiEvidenceProvenanceJoinsSortedIdentitiesDeterministically` |

---

## Gaps found and closed while building this matrix

- **Exclusion of Configuration Reference Representation**: only
  registration-time rejection (`EvidenceKindMapperRegistryTest`) existed;
  no test exercised the actual scenario text ("CSM Builder processes a
  `ConfigFile`/`ConfigReference` Evidence Item... SHALL NOT construct
  any corresponding CSM element or relationship"). Closed with the new
  `ConfigurationExclusionTest`.

## Gaps intentionally left open, tracked elsewhere

- "No Architecture Component constructed" / "No Architectural Boundary
  constructed" — exhaustive, all-inputs coverage is tasks.md 24.4
  (Section 24), not yet implemented.
- "No AI or heuristic judgment used" / "no policy or runtime dependency"
  — currently architectural-only (dependency-graph guard); tasks.md
  24.3 and 24.5 (Section 24) add direct tests.
