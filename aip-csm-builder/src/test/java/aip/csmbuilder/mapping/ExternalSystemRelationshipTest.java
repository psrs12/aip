package aip.csmbuilder.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.core.csm.CsmElement;
import aip.core.csm.CsmRelationship;
import aip.core.csm.CsmRelationshipType;
import aip.core.csm.ExternalSystemElement;
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
 * External System construction tests (tasks.md 11.1-11.4): a {@code
 * ManifestDependencyEdge} whose target does not resolve to a known
 * Project/Module Evidence Item produces a CSM {@code External System}
 * element and an {@code integration} relationship from the source
 * Module, per {@code External System Construction for Unresolved
 * Dependencies}.
 */
class ExternalSystemRelationshipTest {

  private static final Clock FIXED_CLOCK = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);
  private static final String MODULE_A = "com.acme:module-a";
  private static final String MODULE_B = "com.acme:module-b";
  private static final String EXTERNAL_TARGET = "com.thirdparty:some-library";

  @Test
  void unresolvedManifestTargetProducesExternalSystemAndIntegrationRelationship() {
    EvidenceItem moduleA = module(MODULE_A);
    EvidenceItem manifestEdge =
        manifestDependencyEdge("edge-1", MODULE_A, EXTERNAL_TARGET, "maven", "compile");
    RepositoryEvidenceModel model = RepositoryEvidenceModel.of(List.of(moduleA, manifestEdge), List.of());

    MappingResult result = construct(model);

    List<ExternalSystemElement> externalSystems = externalSystemElements(result);
    assertEquals(1, externalSystems.size());
    assertEquals(EXTERNAL_TARGET, externalSystems.get(0).name());

    List<CsmRelationship> integrations = relationshipsOfType(result, CsmRelationshipType.INTEGRATION);
    assertEquals(1, integrations.size());
    CsmRelationship integration = integrations.get(0);
    assertEquals(csmId(moduleA), integration.sourceId());
    assertEquals(Optional.of(externalSystems.get(0).id()), integration.targetId());
  }

  @Test
  void unresolvedTargetIsConnectedByIntegrationNeverDependency() {
    // Regression test guarding the defect corrected in stakeholder
    // review: External System uses `integration`, not `dependency`.
    EvidenceItem moduleA = module(MODULE_A);
    EvidenceItem manifestEdge =
        manifestDependencyEdge("edge-1", MODULE_A, EXTERNAL_TARGET, "maven", "compile");
    RepositoryEvidenceModel model = RepositoryEvidenceModel.of(List.of(moduleA, manifestEdge), List.of());

    MappingResult result = construct(model);

    assertTrue(
        result.relationships().stream().noneMatch(rel -> rel.type() == CsmRelationshipType.DEPENDENCY));
    assertEquals(1, relationshipsOfType(result, CsmRelationshipType.INTEGRATION).size());
  }

  @Test
  void externalSystemAttributesLeftUnset() {
    EvidenceItem moduleA = module(MODULE_A);
    EvidenceItem manifestEdge =
        manifestDependencyEdge("edge-1", MODULE_A, EXTERNAL_TARGET, "maven", "compile");
    RepositoryEvidenceModel model = RepositoryEvidenceModel.of(List.of(moduleA, manifestEdge), List.of());

    ExternalSystemElement externalSystem = externalSystemElements(construct(model)).get(0);

    assertTrue(externalSystem.systemKind().isEmpty());
    assertTrue(externalSystem.criticality().isEmpty());
    assertTrue(externalSystem.integrationProtocol().isEmpty());
    assertTrue(externalSystem.owner().isEmpty());
  }

  @Test
  void resolvedTargetProducesNoExternalSystem() {
    EvidenceItem moduleA = module(MODULE_A);
    EvidenceItem moduleB = module(MODULE_B);
    EvidenceItem manifestEdge = manifestDependencyEdge("edge-1", MODULE_A, MODULE_B, "maven", "compile");
    RepositoryEvidenceModel model =
        RepositoryEvidenceModel.of(List.of(moduleA, moduleB, manifestEdge), List.of());

    MappingResult result = construct(model);

    assertTrue(externalSystemElements(result).isEmpty());
    assertTrue(relationshipsOfType(result, CsmRelationshipType.INTEGRATION).isEmpty());
  }

  @Test
  void importEdgeAloneToAnUnresolvedTargetProducesNoExternalSystem() {
    // Only a ManifestDependencyEdge's target is an artifact
    // coordinate; an ImportEdge's target is source-level only.
    EvidenceItem moduleA = module(MODULE_A);
    EvidenceItem importEdge = importEdge("edge-1", MODULE_A, EXTERNAL_TARGET);
    RepositoryEvidenceModel model = RepositoryEvidenceModel.of(List.of(moduleA, importEdge), List.of());

    MappingResult result = construct(model);

    assertTrue(externalSystemElements(result).isEmpty());
    assertTrue(relationshipsOfType(result, CsmRelationshipType.INTEGRATION).isEmpty());
  }

  @Test
  void sameExternalTargetFromTwoModulesProducesOneElementAndTwoRelationships() {
    EvidenceItem moduleA = module(MODULE_A);
    EvidenceItem moduleB = module(MODULE_B);
    EvidenceItem edgeFromA = manifestDependencyEdge("edge-1", MODULE_A, EXTERNAL_TARGET, "maven", "compile");
    EvidenceItem edgeFromB = manifestDependencyEdge("edge-2", MODULE_B, EXTERNAL_TARGET, "maven", "compile");
    RepositoryEvidenceModel model =
        RepositoryEvidenceModel.of(List.of(moduleA, moduleB, edgeFromA, edgeFromB), List.of());

    MappingResult result = construct(model);

    assertEquals(1, externalSystemElements(result).size(), "one External System per distinct coordinate");
    assertEquals(2, relationshipsOfType(result, CsmRelationshipType.INTEGRATION).size());
  }

  @Test
  void externalSystemIdentityIsStableAcrossRepeatedConstruction() {
    EvidenceItem moduleA = module(MODULE_A);
    EvidenceItem manifestEdge =
        manifestDependencyEdge("edge-1", MODULE_A, EXTERNAL_TARGET, "maven", "compile");
    RepositoryEvidenceModel model = RepositoryEvidenceModel.of(List.of(moduleA, manifestEdge), List.of());

    ExternalSystemElement first = externalSystemElements(construct(model)).get(0);
    ExternalSystemElement second = externalSystemElements(construct(model)).get(0);

    assertEquals(first.id(), second.id());
    assertEquals(
        ElementIdentityDeriver.forExternalSystem("repo-1", EXTERNAL_TARGET), first.id());
  }

  private MappingResult construct(RepositoryEvidenceModel model) {
    EvidenceKindMapperRegistry registry = new EvidenceKindMapperRegistry();
    registry.register(new ModuleMapper());
    return new MappingOrchestrator(registry, FIXED_CLOCK).construct(model);
  }

  private static List<ExternalSystemElement> externalSystemElements(MappingResult result) {
    return result.elements().stream()
        .filter(ExternalSystemElement.class::isInstance)
        .map(ExternalSystemElement.class::cast)
        .toList();
  }

  private static List<CsmRelationship> relationshipsOfType(
      MappingResult result, CsmRelationshipType type) {
    return result.relationships().stream().filter(rel -> rel.type() == type).toList();
  }

  private static aip.core.csm.CsmElementId csmId(EvidenceItem item) {
    return ElementIdentityDeriver.fromEvidenceId(item.id());
  }

  private static EvidenceItem module(String scopeKey) {
    return new EvidenceItem(
        new EvidenceId("repo-1", EvidenceKind.MODULE, scopeKey),
        EvidenceAttributes.empty(),
        java.util.Optional.empty(),
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
        java.util.Optional.empty(),
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
        java.util.Optional.empty(),
        DiscoveryOutcome.complete(),
        ExtractionMethod.FULL_PARSE);
  }
}
