package aip.csmbuilder.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.core.csm.CsmRelationship;
import aip.core.csm.CsmRelationshipType;
import aip.core.csm.DependencyKind;
import aip.core.evidence.DiscoveryOutcome;
import aip.core.evidence.EvidenceAttributes;
import aip.core.evidence.EvidenceId;
import aip.core.evidence.EvidenceItem;
import aip.core.evidence.EvidenceKind;
import aip.core.evidence.ExtractionMethod;
import aip.core.evidence.RepositoryEvidenceModel;
import aip.csmbuilder.identity.ElementIdentityDeriver;
import aip.csmbuilder.mapper.ModuleMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Dependency relationship construction tests (tasks.md 10.6):
 * manifest-only, source-only, both-present, mappable/unmappable native
 * scope, and confirming no corroboration attribute is recorded (per
 * the corrected specification — see design.md Decision 6).
 */
class DependencyRelationshipTest {

  private static final Clock FIXED_CLOCK = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);
  private static final String MODULE_A = "com.acme:module-a";
  private static final String MODULE_B = "com.acme:module-b";

  @Test
  void manifestOnlyEdgeWithMappableScopeProducesAQualifiedDependency() {
    EvidenceItem moduleA = module(MODULE_A);
    EvidenceItem moduleB = module(MODULE_B);
    EvidenceItem manifestEdge =
        manifestDependencyEdge("edge-1", MODULE_A, MODULE_B, "maven", "compile");
    RepositoryEvidenceModel model =
        RepositoryEvidenceModel.of(List.of(moduleA, moduleB, manifestEdge), List.of());

    MappingResult result = construct(model);

    assertEquals(1, result.relationships().size());
    CsmRelationship rel = result.relationships().get(0);
    assertEquals(CsmRelationshipType.DEPENDENCY, rel.type());
    assertEquals(csmId(moduleA), rel.sourceId());
    assertEquals(csmId(moduleB), rel.targetId());
    assertEquals(Optional.of(DependencyKind.COMPILE_TIME), rel.dependencyKind());
  }

  @Test
  void sourceOnlyEdgeProducesAnUnqualifiedDependency() {
    EvidenceItem moduleA = module(MODULE_A);
    EvidenceItem moduleB = module(MODULE_B);
    EvidenceItem importEdge = importEdge("edge-1", MODULE_A, MODULE_B);
    RepositoryEvidenceModel model =
        RepositoryEvidenceModel.of(List.of(moduleA, moduleB, importEdge), List.of());

    MappingResult result = construct(model);

    assertEquals(1, result.relationships().size());
    CsmRelationship rel = result.relationships().get(0);
    assertEquals(CsmRelationshipType.DEPENDENCY, rel.type());
    assertTrue(rel.dependencyKind().isEmpty(), "source-level-only evidence carries no kind qualifier");
  }

  @Test
  void bothEdgesPresentProduceExactlyOneRelationshipWithTheManifestKind() {
    EvidenceItem moduleA = module(MODULE_A);
    EvidenceItem moduleB = module(MODULE_B);
    EvidenceItem manifestEdge = manifestDependencyEdge("edge-1", MODULE_A, MODULE_B, "maven", "test");
    EvidenceItem importEdge = importEdge("edge-2", MODULE_A, MODULE_B);
    RepositoryEvidenceModel model =
        RepositoryEvidenceModel.of(List.of(moduleA, moduleB, manifestEdge, importEdge), List.of());

    MappingResult result = construct(model);

    assertEquals(1, result.relationships().size(), "manifest + import edges for the same pair merge into one relationship");
    CsmRelationship rel = result.relationships().get(0);
    assertEquals(Optional.of(DependencyKind.TEST_ONLY), rel.dependencyKind());
    assertTrue(
        rel.nativeAttributes().isEmpty(),
        "no corroboration attribute is recorded, per the corrected specification (design.md Decision 6)");
  }

  @Test
  void unmappableNativeScopeYieldsNoKindQualifierNeverAGuess() {
    EvidenceItem moduleA = module(MODULE_A);
    EvidenceItem moduleB = module(MODULE_B);
    EvidenceItem manifestEdge =
        manifestDependencyEdge("edge-1", MODULE_A, MODULE_B, "maven", "some-future-scope");
    RepositoryEvidenceModel model =
        RepositoryEvidenceModel.of(List.of(moduleA, moduleB, manifestEdge), List.of());

    MappingResult result = construct(model);

    assertEquals(1, result.relationships().size());
    assertTrue(result.relationships().get(0).dependencyKind().isEmpty());
  }

  @Test
  void unresolvedTargetProducesNoRelationshipInThisSection() {
    // No Module Evidence Item exists for "com.acme:external-artifact"
    // - External System construction is a later section's concern.
    EvidenceItem moduleA = module(MODULE_A);
    EvidenceItem manifestEdge =
        manifestDependencyEdge("edge-1", MODULE_A, "com.acme:external-artifact", "maven", "compile");
    RepositoryEvidenceModel model = RepositoryEvidenceModel.of(List.of(moduleA, manifestEdge), List.of());

    MappingResult result = construct(model);

    assertTrue(result.relationships().isEmpty());
  }

  @Test
  void dependencyRelationshipIdentityExcludesTheKindQualifier() {
    EvidenceItem moduleA = module(MODULE_A);
    EvidenceItem moduleB = module(MODULE_B);
    EvidenceItem manifestEdge = manifestDependencyEdge("edge-1", MODULE_A, MODULE_B, "maven", "compile");
    RepositoryEvidenceModel model =
        RepositoryEvidenceModel.of(List.of(moduleA, moduleB, manifestEdge), List.of());

    CsmRelationship rel = construct(model).relationships().get(0);

    assertEquals(
        ElementIdentityDeriver.forRelationship(csmId(moduleA), csmId(moduleB), CsmRelationshipType.DEPENDENCY),
        rel.id());
  }

  private MappingResult construct(RepositoryEvidenceModel model) {
    EvidenceKindMapperRegistry registry = new EvidenceKindMapperRegistry();
    registry.register(new ModuleMapper());
    return new MappingOrchestrator(registry, FIXED_CLOCK).construct(model);
  }

  private static aip.core.csm.CsmElementId csmId(EvidenceItem item) {
    return ElementIdentityDeriver.fromEvidenceId(item.id());
  }

  private static EvidenceItem module(String scopeKey) {
    return new EvidenceItem(
        new EvidenceId("repo-1", EvidenceKind.MODULE, scopeKey),
        EvidenceAttributes.empty(),
        Optional.empty(),
        DiscoveryOutcome.complete(),
        ExtractionMethod.MANIFEST_DECLARED);
  }

  private static EvidenceItem manifestDependencyEdge(
      String edgeScopeKey, String sourceModule, String target, String buildSystem, String nativeScope) {
    return new EvidenceItem(
        new EvidenceId("repo-1", EvidenceKind.MANIFEST_DEPENDENCY_EDGE, edgeScopeKey),
        EvidenceAttributes.of(
            Map.of(
                EvidenceAttributeKeys.DEPENDENCY_SOURCE_MODULE, sourceModule,
                EvidenceAttributeKeys.DEPENDENCY_TARGET, target,
                EvidenceAttributeKeys.BUILD_SYSTEM, buildSystem,
                EvidenceAttributeKeys.NATIVE_SCOPE, nativeScope)),
        Optional.empty(),
        DiscoveryOutcome.complete(),
        ExtractionMethod.MANIFEST_DECLARED);
  }

  private static EvidenceItem importEdge(String edgeScopeKey, String sourceModule, String target) {
    return new EvidenceItem(
        new EvidenceId("repo-1", EvidenceKind.IMPORT_EDGE, edgeScopeKey),
        EvidenceAttributes.of(
            Map.of(
                EvidenceAttributeKeys.DEPENDENCY_SOURCE_MODULE, sourceModule,
                EvidenceAttributeKeys.DEPENDENCY_TARGET, target)),
        Optional.empty(),
        DiscoveryOutcome.complete(),
        ExtractionMethod.FULL_PARSE);
  }
}
