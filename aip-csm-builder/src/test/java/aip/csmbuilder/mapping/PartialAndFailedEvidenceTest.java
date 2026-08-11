package aip.csmbuilder.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.core.csm.CsmElement;
import aip.core.evidence.DiscoveryOutcome;
import aip.core.evidence.EvidenceAttributes;
import aip.core.evidence.EvidenceId;
import aip.core.evidence.EvidenceItem;
import aip.core.evidence.EvidenceKind;
import aip.core.evidence.ExtractionMethod;
import aip.core.evidence.FailureReason;
import aip.core.evidence.RepositoryEvidenceModel;
import aip.csmbuilder.mapper.ModuleMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Partial- and failed-evidence handling tests (tasks.md 14.3), per
 * {@code Partial-Evidence Construction} and {@code Failed-Evidence
 * Non-Construction}.
 */
class PartialAndFailedEvidenceTest {

  private static final Clock FIXED_CLOCK = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);

  @Test
  void partialEvidenceProducesAnElementIndistinguishableFromComplete() {
    EvidenceItem completeModule = module("com.acme:module-a", DiscoveryOutcome.complete());
    EvidenceItem partialModule =
        module("com.acme:module-b", DiscoveryOutcome.partial(FailureReason.PARSE_ERROR));

    CsmElement fromComplete = construct(completeModule).elements().get(0);
    CsmElement fromPartial = construct(partialModule).elements().get(0);

    // Only the identity differs (different originating scope key) - the
    // shape (type, name-derivation) and provenance category/absence of
    // confidence are identical regardless of discovery outcome.
    assertEquals(fromComplete.getClass(), fromPartial.getClass());
    assertEquals(fromComplete.provenance().category(), fromPartial.provenance().category());
    assertTrue(fromPartial.provenance().confidence().isEmpty());
    assertTrue(
        fromPartial.nativeAttributes().asMap().keySet().stream()
            .noneMatch(key -> key.toLowerCase().contains("partial") || key.toLowerCase().contains("incomplete")),
        "no incompleteness marker is attached");
  }

  @Test
  void failedEvidenceProducesNoElement() {
    EvidenceItem failedModule =
        module("com.acme:module-a", DiscoveryOutcome.failed(FailureReason.TIMEOUT));

    MappingResult result = construct(failedModule);

    assertTrue(result.elements().isEmpty());
  }

  @Test
  void failedManifestDependencyEdgeProducesNoDependencyRelationship() {
    EvidenceItem moduleA = module("com.acme:module-a", DiscoveryOutcome.complete());
    EvidenceItem moduleB = module("com.acme:module-b", DiscoveryOutcome.complete());
    EvidenceItem failedEdge =
        new EvidenceItem(
            new EvidenceId("repo-1", EvidenceKind.MANIFEST_DEPENDENCY_EDGE, "edge-1"),
            EvidenceAttributes.of(
                Map.of(
                    EvidenceAttributeKeys.DEPENDENCY_SOURCE_MODULE, "com.acme:module-a",
                    EvidenceAttributeKeys.DEPENDENCY_TARGET, "com.acme:module-b",
                    EvidenceAttributeKeys.BUILD_SYSTEM, "maven",
                    EvidenceAttributeKeys.NATIVE_SCOPE, "compile")),
            Optional.empty(),
            DiscoveryOutcome.failed(FailureReason.PARSE_ERROR),
            ExtractionMethod.MANIFEST_DECLARED);
    RepositoryEvidenceModel model =
        RepositoryEvidenceModel.of(List.of(moduleA, moduleB, failedEdge), List.of());

    EvidenceKindMapperRegistry registry = new EvidenceKindMapperRegistry();
    registry.register(new ModuleMapper());
    MappingResult result = new MappingOrchestrator(registry, FIXED_CLOCK).construct(model);

    assertTrue(result.relationships().isEmpty());
  }

  @Test
  void failedManifestDependencyEdgeToAnUnresolvedTargetProducesNoExternalSystem() {
    EvidenceItem moduleA = module("com.acme:module-a", DiscoveryOutcome.complete());
    EvidenceItem failedEdge =
        new EvidenceItem(
            new EvidenceId("repo-1", EvidenceKind.MANIFEST_DEPENDENCY_EDGE, "edge-1"),
            EvidenceAttributes.of(
                Map.of(
                    EvidenceAttributeKeys.DEPENDENCY_SOURCE_MODULE, "com.acme:module-a",
                    EvidenceAttributeKeys.DEPENDENCY_TARGET, "com.thirdparty:some-library",
                    EvidenceAttributeKeys.BUILD_SYSTEM, "maven",
                    EvidenceAttributeKeys.NATIVE_SCOPE, "compile")),
            Optional.empty(),
            DiscoveryOutcome.failed(FailureReason.PARSE_ERROR),
            ExtractionMethod.MANIFEST_DECLARED);
    RepositoryEvidenceModel model = RepositoryEvidenceModel.of(List.of(moduleA, failedEdge), List.of());

    EvidenceKindMapperRegistry registry = new EvidenceKindMapperRegistry();
    registry.register(new ModuleMapper());
    MappingResult result = new MappingOrchestrator(registry, FIXED_CLOCK).construct(model);

    assertTrue(result.relationships().isEmpty());
    assertTrue(result.elements().stream().noneMatch(e -> e.name().equals("com.thirdparty:some-library")));
  }

  private MappingResult construct(EvidenceItem item) {
    EvidenceKindMapperRegistry registry = new EvidenceKindMapperRegistry();
    registry.register(new ModuleMapper());
    RepositoryEvidenceModel model = RepositoryEvidenceModel.of(List.of(item), List.of());
    return new MappingOrchestrator(registry, FIXED_CLOCK).construct(model);
  }

  private static EvidenceItem module(String scopeKey, DiscoveryOutcome discoveryOutcome) {
    return new EvidenceItem(
        new EvidenceId("repo-1", EvidenceKind.MODULE, scopeKey),
        EvidenceAttributes.empty(),
        Optional.empty(),
        discoveryOutcome,
        ExtractionMethod.MANIFEST_DECLARED);
  }
}
