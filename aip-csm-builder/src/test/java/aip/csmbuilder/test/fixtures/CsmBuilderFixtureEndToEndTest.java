package aip.csmbuilder.test.fixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.core.csm.CsmElement;
import aip.core.csm.CsmElementId;
import aip.core.csm.CsmRelationship;
import aip.core.csm.CsmRelationshipType;
import aip.core.csm.DependencyKind;
import aip.core.csm.ExternalSystemElement;
import aip.core.evidence.ChangeStatus;
import aip.core.evidence.EvidenceId;
import aip.core.evidence.EvidenceKind;
import aip.core.evidence.RepositoryEvidenceModel;
import aip.csmbuilder.mapper.ApiContractMapper;
import aip.csmbuilder.mapper.MethodMapper;
import aip.csmbuilder.mapper.ModuleMapper;
import aip.csmbuilder.mapper.PackageMapper;
import aip.csmbuilder.mapper.ProjectMapper;
import aip.csmbuilder.mapper.RepositoryMapper;
import aip.csmbuilder.mapper.TypeMapper;
import aip.csmbuilder.mapping.EvidenceKindMapperRegistry;
import aip.csmbuilder.mapping.MappingOrchestrator;
import aip.csmbuilder.mapping.MappingResult;
import aip.csmbuilder.mapping.PriorElementLookup;
import aip.csmbuilder.snapshot.FilesystemSnapshotStore;
import aip.csmbuilder.snapshot.MapperAttribution;
import aip.csmbuilder.snapshot.Snapshot;
import aip.csmbuilder.snapshot.SnapshotContent;
import aip.csmbuilder.snapshot.SnapshotPublisher;
import aip.csmbuilder.snapshot.SnapshotStore;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * End-to-end fixture-based tests (tasks.md 23.2): the full Mapping
 * Orchestrator run against {@link
 * FixtureScenarios#multiLanguageRepositoryWithDependenciesAndApiContracts()},
 * a representative multi-language, multi-Module scenario composing
 * structural containment (Section 8), dependency and External System
 * construction (Sections 10-11), API contract exposure (Section 12),
 * configuration exclusion (Section 13), snapshot persistence and
 * validation (Sections 15, 19), and incremental re-derivation (Section
 * 16) together — proving these mechanisms compose correctly, not only
 * that each passes in isolation.
 */
class CsmBuilderFixtureEndToEndTest {

  private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneOffset.UTC);

  @TempDir Path tempDir;

  @Test
  void constructsExpectedStructuralDependencyAndApiContractContent() {
    MappingResult result = construct(FixtureScenarios.multiLanguageRepositoryWithDependenciesAndApiContracts());

    // Structural containment (Section 8): Repository, Project, two
    // Modules, two Packages, two Types, one Method (nine elements),
    // plus one External System (Section 11) for the unresolved
    // dependency target - ten elements total, none for the ConfigFile
    // evidence.
    assertEquals(10, result.elements().size());
    assertTrue(result.elements().stream().noneMatch(e -> e.name().equals("application.yml")));

    // One internal DEPENDENCY relationship, kind-qualified.
    long dependencyCount =
        result.relationships().stream().filter(r -> r.type() == CsmRelationshipType.DEPENDENCY).count();
    assertEquals(1, dependencyCount);
    CsmRelationship dependency =
        result.relationships().stream().filter(r -> r.type() == CsmRelationshipType.DEPENDENCY).findFirst().get();
    assertEquals(Optional.of(DependencyKind.COMPILE_TIME), dependency.dependencyKind());

    // One External System element and one INTEGRATION relationship for
    // the unresolved target - never DEPENDENCY (the stakeholder-review
    // regression).
    long externalSystemCount = result.elements().stream().filter(e -> e instanceof ExternalSystemElement).count();
    assertEquals(1, externalSystemCount);
    long integrationCount =
        result.relationships().stream().filter(r -> r.type() == CsmRelationshipType.INTEGRATION).count();
    assertEquals(1, integrationCount);

    // One EXPOSURE_CONSUMPTION relationship for the API contract, with
    // an absent (never invented) target.
    long exposureCount =
        result.relationships().stream().filter(r -> r.type() == CsmRelationshipType.EXPOSURE_CONSUMPTION).count();
    assertEquals(1, exposureCount);
    CsmRelationship exposure =
        result.relationships().stream()
            .filter(r -> r.type() == CsmRelationshipType.EXPOSURE_CONSUMPTION)
            .findFirst()
            .get();
    assertTrue(exposure.targetId().isEmpty());

    // Structural containment: eight CONTAINMENT edges (Repository->Project,
    // Project->ModuleA, Project->ModuleB, ModuleA->PackageA, PackageA->TypeA,
    // TypeA->MethodA, ModuleB->PackageB, PackageB->TypeB).
    long containmentCount =
        result.relationships().stream().filter(r -> r.type() == CsmRelationshipType.CONTAINMENT).count();
    assertEquals(8, containmentCount);
  }

  @Test
  void constructedContentPublishesAsAValidRetrievableSnapshot() {
    RepositoryEvidenceModel model = FixtureScenarios.multiLanguageRepositoryWithDependenciesAndApiContracts();
    MappingResult result = construct(model);

    Map<CsmElementId, MapperAttribution> attributions = new LinkedHashMap<>();
    for (CsmElement element : result.elements()) {
      attributions.put(element.id(), new MapperAttribution(element.kind().name(), 1));
    }
    SnapshotContent content = new SnapshotContent(result.elements(), result.relationships(), attributions);

    SnapshotStore store = new FilesystemSnapshotStore(tempDir);
    SnapshotPublisher.PublicationOutcome outcome =
        SnapshotPublisher.publish(store, "repo-1", content, FIXED_CLOCK.instant());

    assertTrue(outcome.published(), "an ordinary, all-OBSERVED snapshot satisfies CSM Validation Expectations");
    Snapshot snapshot = outcome.snapshot().orElseThrow();
    Optional<Snapshot> readBack = store.read("repo-1", snapshot.sequenceNumber());
    assertTrue(readBack.isPresent());
    assertEquals(result.elements().size(), readBack.get().elements().size());
    assertEquals(result.relationships().size(), readBack.get().relationships().size());
  }

  @Test
  void secondRunCarriesUnchangedModulesForwardAndOmitsARemovedOne() {
    RepositoryEvidenceModel firstRunModel =
        FixtureScenarios.multiLanguageRepositoryWithDependenciesAndApiContracts();
    EvidenceKindMapperRegistry registry = fullRegistry();
    MappingOrchestrator orchestrator = new MappingOrchestrator(registry, FIXED_CLOCK);
    MappingResult firstRun = orchestrator.construct(firstRunModel);

    CsmElement moduleBFirstRun =
        firstRun.elements().stream().filter(e -> e.name().equals("com.acme:module-b")).findFirst().orElseThrow();

    // Second run: Module B is UNCHANGED (carried forward via a
    // PriorElementLookup built from the first run's own output - no
    // production Snapshot-backed adapter exists yet, see design.md
    // Decision 9's scope note); Module A is REMOVED.
    EvidenceId moduleAId = new EvidenceId("repo-1", EvidenceKind.MODULE, "com.acme:module-a");
    EvidenceId moduleBId = new EvidenceId("repo-1", EvidenceKind.MODULE, "com.acme:module-b");
    Map<EvidenceId, ChangeStatus> secondRunChangeStatuses =
        Map.of(moduleAId, ChangeStatus.REMOVED, moduleBId, ChangeStatus.UNCHANGED);
    PriorElementLookup priorElements =
        evidenceId ->
            evidenceId.equals(moduleBId)
                ? Optional.of(new PriorElementLookup.PriorElement(moduleBFirstRun, 1))
                : Optional.empty();

    MappingResult secondRun = orchestrator.construct(firstRunModel, secondRunChangeStatuses, priorElements);

    assertTrue(
        secondRun.elements().stream().noneMatch(e -> e.name().equals("com.acme:module-a")),
        "REMOVED omits Module A from the second run");
    CsmElement moduleBSecondRun =
        secondRun.elements().stream().filter(e -> e.name().equals("com.acme:module-b")).findFirst().orElseThrow();
    assertSame(moduleBFirstRun, moduleBSecondRun, "UNCHANGED Module B is carried forward, not reconstructed");
  }

  private MappingResult construct(RepositoryEvidenceModel model) {
    return new MappingOrchestrator(fullRegistry(), FIXED_CLOCK).construct(model);
  }

  private static EvidenceKindMapperRegistry fullRegistry() {
    EvidenceKindMapperRegistry registry = new EvidenceKindMapperRegistry();
    registry.register(new RepositoryMapper());
    registry.register(new ProjectMapper());
    registry.register(new ModuleMapper());
    registry.register(new PackageMapper());
    registry.register(new TypeMapper());
    registry.register(new MethodMapper());
    registry.register(new ApiContractMapper());
    return registry;
  }
}
