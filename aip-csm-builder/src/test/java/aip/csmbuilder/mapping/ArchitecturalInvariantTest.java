package aip.csmbuilder.mapping;

import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.core.csm.ArchitectureComponentElement;
import aip.core.csm.CsmRelationshipType;
import aip.csmbuilder.mapper.ApiContractMapper;
import aip.csmbuilder.mapper.MethodMapper;
import aip.csmbuilder.mapper.ModuleMapper;
import aip.csmbuilder.mapper.PackageMapper;
import aip.csmbuilder.mapper.ProjectMapper;
import aip.csmbuilder.mapper.RepositoryMapper;
import aip.csmbuilder.mapper.TypeMapper;
import aip.csmbuilder.test.fixtures.FixtureScenarios;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Architectural invariant tests (tasks.md 24.4, 24.5) — runtime
 * complements to {@code scripts/check-no-excluded-construction.sh}
 * (24.4's static, source-scan half). See {@code
 * aip.csmbuilder.mapping.package-info}/{@code CsmElementCodec}'s own
 * javadoc for why {@code ArchitectureComponentElement} construction is
 * legitimate in {@code aip.csmbuilder.snapshot}'s decode path and
 * therefore out of scope for both this test and that script.
 */
class ArchitecturalInvariantTest {

  private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-01T00:00:00Z"), ZoneOffset.UTC);

  @Test
  void comprehensiveFixtureNeverProducesAnArchitectureComponentOrArchitecturalBoundary() {
    // tasks.md 24.4: run the richest available fixture (Section 23's
    // multi-language, multi-Module, multi-mechanism scenario) through
    // every registered production Mapper and confirm neither excluded
    // kind ever appears in the result.
    EvidenceKindMapperRegistry registry = new EvidenceKindMapperRegistry();
    registry.register(new RepositoryMapper());
    registry.register(new ProjectMapper());
    registry.register(new ModuleMapper());
    registry.register(new PackageMapper());
    registry.register(new TypeMapper());
    registry.register(new MethodMapper());
    registry.register(new ApiContractMapper());

    MappingResult result =
        new MappingOrchestrator(registry, FIXED_CLOCK)
            .construct(FixtureScenarios.multiLanguageRepositoryWithDependenciesAndApiContracts());

    assertTrue(
        result.elements().stream().noneMatch(ArchitectureComponentElement.class::isInstance),
        "no Architecture Component element was constructed");
    assertTrue(
        result.relationships().stream().noneMatch(r -> r.type() == CsmRelationshipType.BOUNDARY_CONSTRAINT),
        "no Architectural Boundary (BOUNDARY_CONSTRAINT) relationship was constructed");
  }

  @Test
  void mappingOrchestratorApiReferencesNoPolicyRuleOrRuntimeModelType() {
    // tasks.md 24.5: no Policy/Rule Model or Runtime Model type exists
    // anywhere in aip-core at all (confirmed: nothing under
    // aip.core.* matches "Policy", "Rule", or "RuntimeModel"), so
    // depending on one is structurally impossible today. This test
    // guards the API surface itself, so a future accidental
    // reintroduction of such a dependency into MappingOrchestrator's
    // public methods fails loudly rather than silently compiling.
    Set<String> forbiddenNameFragments = Set.of("Policy", "Rule", "RuntimeModel");

    for (Method method : MappingOrchestrator.class.getMethods()) {
      if (method.getDeclaringClass() != MappingOrchestrator.class) {
        continue; // skip inherited Object methods
      }
      assertNoForbiddenType(method.getReturnType().getName(), forbiddenNameFragments, method);
      for (Parameter parameter : method.getParameters()) {
        assertNoForbiddenType(parameter.getType().getName(), forbiddenNameFragments, method);
      }
    }
  }

  private static void assertNoForbiddenType(String typeName, Set<String> forbiddenNameFragments, Method method) {
    for (String fragment : forbiddenNameFragments) {
      assertTrue(
          !typeName.contains(fragment),
          "MappingOrchestrator." + method.getName() + " references " + typeName
              + ", which looks like a Policy/Rule/Runtime Model dependency");
    }
  }
}
