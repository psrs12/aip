package aip.csmbuilder.mapping;

import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.core.evidence.DiscoveryOutcome;
import aip.core.evidence.EvidenceAttributes;
import aip.core.evidence.EvidenceId;
import aip.core.evidence.EvidenceItem;
import aip.core.evidence.EvidenceKind;
import aip.core.evidence.ExtractionMethod;
import aip.core.evidence.RepositoryEvidenceModel;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Construction-level coverage for {@code Exclusion of Configuration
 * Reference Representation} (closing a traceability gap found in
 * Section 23: {@link EvidenceKindMapperRegistryTest} only proves no
 * Mapper can ever be <em>registered</em> for these kinds — this proves
 * that running them through {@link MappingOrchestrator} produces no
 * CSM content, the scenario the requirement actually describes).
 */
class ConfigurationExclusionTest {

  private static final Clock FIXED_CLOCK = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);

  @Test
  void configFileEvidenceProducesNoCsmElementOrRelationship() {
    MappingResult result = construct(EvidenceKind.CONFIG_FILE, "application.yml");
    assertTrue(result.elements().isEmpty());
    assertTrue(result.relationships().isEmpty());
  }

  @Test
  void configReferenceEvidenceProducesNoCsmElementOrRelationship() {
    MappingResult result = construct(EvidenceKind.CONFIG_REFERENCE, "DB_PASSWORD");
    assertTrue(result.elements().isEmpty());
    assertTrue(result.relationships().isEmpty());
  }

  private MappingResult construct(EvidenceKind kind, String scopeKey) {
    EvidenceItem item =
        new EvidenceItem(
            new EvidenceId("repo-1", kind, scopeKey),
            EvidenceAttributes.empty(),
            Optional.empty(),
            DiscoveryOutcome.complete(),
            ExtractionMethod.FULL_PARSE);
    RepositoryEvidenceModel model = RepositoryEvidenceModel.of(List.of(item), List.of());
    // An empty registry - no Mapper is ever registered for either kind
    // (EvidenceKindMapperRegistry rejects the attempt outright).
    return new MappingOrchestrator(new EvidenceKindMapperRegistry(), FIXED_CLOCK).construct(model);
  }
}
