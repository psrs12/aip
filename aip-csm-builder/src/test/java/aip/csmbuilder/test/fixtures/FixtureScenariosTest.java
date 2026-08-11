package aip.csmbuilder.test.fixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.core.csm.CsmRelationship;
import aip.core.csm.CsmRelationshipType;
import aip.core.csm.DependencyKind;
import aip.core.evidence.RepositoryEvidenceModel;
import aip.csmbuilder.mapper.MethodMapper;
import aip.csmbuilder.mapper.ModuleMapper;
import aip.csmbuilder.mapper.PackageMapper;
import aip.csmbuilder.mapper.ProjectMapper;
import aip.csmbuilder.mapper.RepositoryMapper;
import aip.csmbuilder.mapper.TypeMapper;
import aip.csmbuilder.mapping.EvidenceKindMapperRegistry;
import aip.csmbuilder.mapping.MappingOrchestrator;
import aip.csmbuilder.mapping.MappingResult;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Confirms every {@link FixtureScenarios} entry is well-formed and
 * actually usable against a fully-registered {@link MappingOrchestrator}
 * (tasks.md 21.3) — proof this fixture library is exercisable, not just
 * that it compiles.
 */
class FixtureScenariosTest {

  private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC);

  @Test
  void singleModuleProjectConstructsARepositoryProjectModuleTypeChain() {
    MappingResult result = construct(FixtureScenarios.singleModuleProject());

    assertEquals(4, result.elements().size(), "Repository, Project, default Module, Type");
    assertEquals(3, result.relationships().size(), "three containment edges");
  }

  @Test
  void multiModuleProjectConstructsTwoModulesUnderOneProject() {
    MappingResult result = construct(FixtureScenarios.multiModuleProject());

    assertEquals(6, result.elements().size(), "Repository, Project, two Modules, two Types");
  }

  @Test
  void unmanagedFilesConstructUnderTheUnmanagedProject() {
    MappingResult result = construct(FixtureScenarios.unmanagedFiles());

    assertEquals(4, result.elements().size(), "Repository, unmanaged Project, default Module, Type");
    assertTrue(
        result.elements().stream().anyMatch(e -> e.name().equals("unmanaged")),
        "the unmanaged Project element is present, mapped like any other Project");
  }

  @Test
  void partialEvidenceStillConstructsAnElement() {
    MappingResult result = construct(FixtureScenarios.partialEvidence());

    assertEquals(2, result.elements().size(), "Module and the partially-discovered Type");
  }

  @Test
  void mappableDependencyScopeProducesAQualifiedDependencyRelationship() {
    MappingResult result = construct(FixtureScenarios.mappableDependencyScope());

    assertEquals(1, result.relationships().size());
    CsmRelationship dependency = result.relationships().get(0);
    assertEquals(CsmRelationshipType.DEPENDENCY, dependency.type());
    assertEquals(Optional.of(DependencyKind.COMPILE_TIME), dependency.dependencyKind());
  }

  @Test
  void unmappableDependencyScopeProducesAnUnqualifiedDependencyRelationship() {
    MappingResult result = construct(FixtureScenarios.unmappableDependencyScope());

    assertEquals(1, result.relationships().size());
    assertTrue(result.relationships().get(0).dependencyKind().isEmpty());
  }

  @Test
  void removedAndTombstonedAcrossTwoRunsOmitsTheModuleFromTheSecondSnapshot() {
    FixtureScenarios.TwoRunScenario scenario = FixtureScenarios.removedAndTombstonedAcrossTwoRuns();
    EvidenceKindMapperRegistry registry = new EvidenceKindMapperRegistry();
    registry.register(new ModuleMapper());
    MappingOrchestrator orchestrator = new MappingOrchestrator(registry, FIXED_CLOCK);

    MappingResult firstRunResult = orchestrator.construct(scenario.firstRun());
    assertEquals(1, firstRunResult.elements().size(), "the Module is present in the first run");

    MappingResult secondRunResult =
        orchestrator.construct(
            scenario.secondRun(), scenario.secondRunChangeStatuses(), aip.csmbuilder.mapping.PriorElementLookup.none());
    assertTrue(secondRunResult.elements().isEmpty(), "REMOVED omits the Module from the second run's snapshot");
  }

  private MappingResult construct(RepositoryEvidenceModel model) {
    EvidenceKindMapperRegistry registry = new EvidenceKindMapperRegistry();
    registry.register(new RepositoryMapper());
    registry.register(new ProjectMapper());
    registry.register(new ModuleMapper());
    registry.register(new PackageMapper());
    registry.register(new TypeMapper());
    registry.register(new MethodMapper());
    return new MappingOrchestrator(registry, FIXED_CLOCK).construct(model);
  }
}
