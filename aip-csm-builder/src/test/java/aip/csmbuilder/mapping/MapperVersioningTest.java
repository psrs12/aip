package aip.csmbuilder.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.core.csm.CsmElement;
import aip.core.csm.CsmElementId;
import aip.core.csm.ModuleElement;
import aip.core.csm.NativeAttributes;
import aip.core.csm.ProvenanceRecord;
import aip.core.evidence.ChangeStatus;
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
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * CSM Builder Mapper Versioning tests (tasks.md 17.3): a Mapper version
 * change makes a previously constructed element eligible for
 * re-derivation, independent of evidence change status.
 */
class MapperVersioningTest {

  private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-02-01T00:00:00Z"), ZoneOffset.UTC);
  private static final EvidenceId MODULE_ID = new EvidenceId("repo-1", EvidenceKind.MODULE, "com.acme:module-a");

  @Test
  void mapperVersionChangeTriggersReDerivationOfUnchangedEvidence() {
    // The prior element was produced by Mapper version 1; the Mapper
    // registered for this run is version 2, though the evidence itself
    // is UNCHANGED.
    CsmElement priorElement = priorElement();
    PriorElementLookup priorElements = IncrementalConstructionTest.priorLookup(MODULE_ID, priorElement, 1);
    RecordingTestMapper mapperV2 = new RecordingTestMapper(EvidenceKind.MODULE, 2);

    MappingResult result = construct(mapperV2, priorElements);

    assertEquals(1, mapperV2.invocations.size(), "a version change re-invokes the Mapper despite UNCHANGED evidence");
    assertEquals(1, result.elements().size());
    assertNotSame(priorElement, result.elements().get(0), "the re-derived element is freshly constructed, not the carried-forward one");
  }

  @Test
  void matchingMapperVersionStillCarriesForwardWithoutInvocation() {
    // Baseline contrast: when the version matches, 16.1's carry-forward
    // behavior still applies unchanged.
    CsmElement priorElement = priorElement();
    PriorElementLookup priorElements = IncrementalConstructionTest.priorLookup(MODULE_ID, priorElement, 1);
    RecordingTestMapper mapperV1 = new RecordingTestMapper(EvidenceKind.MODULE, 1);

    MappingResult result = construct(mapperV1, priorElements);

    assertTrue(mapperV1.invocations.isEmpty());
    assertSame(priorElement, result.elements().get(0));
  }

  private MappingResult construct(RecordingTestMapper mapper, PriorElementLookup priorElements) {
    EvidenceKindMapperRegistry registry = new EvidenceKindMapperRegistry();
    registry.register(mapper);
    RepositoryEvidenceModel model = RepositoryEvidenceModel.of(List.of(moduleItem()), List.of());

    return new MappingOrchestrator(registry, FIXED_CLOCK)
        .construct(model, Map.of(MODULE_ID, ChangeStatus.UNCHANGED), priorElements);
  }

  private static EvidenceItem moduleItem() {
    return new EvidenceItem(
        MODULE_ID, EvidenceAttributes.empty(), Optional.empty(), DiscoveryOutcome.complete(), ExtractionMethod.MANIFEST_DECLARED);
  }

  private static CsmElement priorElement() {
    return new ModuleElement(
        new CsmElementId("csm:prior:module-a"),
        "module-a",
        ProvenanceRecord.observed(MODULE_ID.toString(), Instant.parse("2025-01-01T00:00:00Z")),
        NativeAttributes.empty());
  }
}
