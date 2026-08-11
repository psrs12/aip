package aip.csmbuilder.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import aip.core.evidence.LifecycleState;
import aip.core.evidence.RepositoryEvidenceModel;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Incremental re-derivation tests (tasks.md 16.3): unchanged, changed
 * (added/modified), removed, and TOMBSTONED-but-not-yet-PURGED cases,
 * per {@code Incremental Snapshot Scope} and {@code Evidence Lifecycle
 * Interaction}.
 */
class IncrementalConstructionTest {

  private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-02-01T00:00:00Z"), ZoneOffset.UTC);
  private static final EvidenceId MODULE_ID = new EvidenceId("repo-1", EvidenceKind.MODULE, "com.acme:module-a");

  @Test
  void unchangedItemIsCarriedForwardWithoutInvokingItsMapper() {
    RecordingTestMapper mapper = new RecordingTestMapper(EvidenceKind.MODULE, 1);
    CsmElement priorElement = priorElement();
    PriorElementLookup priorElements = priorLookup(priorElement, 1);

    MappingResult result = construct(mapper, ChangeStatus.UNCHANGED, priorElements);

    assertTrue(mapper.invocations.isEmpty(), "Mapper SHALL NOT be invoked for an UNCHANGED item with a known prior element");
    assertEquals(1, result.elements().size());
    assertSame(priorElement, result.elements().get(0), "the exact prior element is carried forward, not reconstructed");
  }

  @Test
  void addedItemIsConstructedFresh() {
    RecordingTestMapper mapper = new RecordingTestMapper(EvidenceKind.MODULE, 1);

    MappingResult result = construct(mapper, ChangeStatus.ADDED, PriorElementLookup.none());

    assertEquals(1, mapper.invocations.size());
    assertEquals(1, result.elements().size());
  }

  @Test
  void modifiedItemIsConstructedFresh() {
    RecordingTestMapper mapper = new RecordingTestMapper(EvidenceKind.MODULE, 1);

    MappingResult result = construct(mapper, ChangeStatus.MODIFIED, PriorElementLookup.none());

    assertEquals(1, mapper.invocations.size());
    assertEquals(1, result.elements().size());
  }

  @Test
  void removedItemIsOmittedAndItsMapperIsNeverInvoked() {
    RecordingTestMapper mapper = new RecordingTestMapper(EvidenceKind.MODULE, 1);

    MappingResult result = construct(mapper, ChangeStatus.REMOVED, PriorElementLookup.none());

    assertTrue(mapper.invocations.isEmpty());
    assertTrue(result.elements().isEmpty());
  }

  @Test
  void tombstonedButNotYetPurgedIsOmittedWithoutWaitingForPurge() {
    // Evidence Lifecycle Interaction: change status REMOVED is
    // sufficient to omit, regardless of which lifecycle state
    // (TOMBSTONED or PURGED) accompanies it - construct a
    // ClassifiedEvidenceItem explicitly pairing REMOVED with
    // TOMBSTONED to demonstrate this isn't waiting for PURGED.
    var classified =
        new aip.core.evidence.ClassifiedEvidenceItem(moduleItem(), ChangeStatus.REMOVED, LifecycleState.TOMBSTONED);
    RecordingTestMapper mapper = new RecordingTestMapper(EvidenceKind.MODULE, 1);

    MappingResult result = construct(mapper, classified.changeStatus(), PriorElementLookup.none());

    assertTrue(mapper.invocations.isEmpty());
    assertTrue(result.elements().isEmpty());
  }

  @Test
  void purgedIsOmittedTheSameWayAsTombstoned() {
    var classified =
        new aip.core.evidence.ClassifiedEvidenceItem(moduleItem(), ChangeStatus.REMOVED, LifecycleState.PURGED);
    RecordingTestMapper mapper = new RecordingTestMapper(EvidenceKind.MODULE, 1);

    MappingResult result = construct(mapper, classified.changeStatus(), PriorElementLookup.none());

    assertTrue(mapper.invocations.isEmpty());
    assertTrue(result.elements().isEmpty());
  }

  @Test
  void unchangedItemWithNoKnownPriorElementFallsBackToFreshConstruction() {
    // No prior snapshot at all (or this identity simply isn't in it) -
    // PriorElementLookup.none() always misses, so construction proceeds
    // as if this were ADDED/MODIFIED rather than silently producing
    // nothing.
    RecordingTestMapper mapper = new RecordingTestMapper(EvidenceKind.MODULE, 1);

    MappingResult result = construct(mapper, ChangeStatus.UNCHANGED, PriorElementLookup.none());

    assertEquals(1, mapper.invocations.size());
    assertEquals(1, result.elements().size());
  }

  @Test
  void unspecifiedChangeStatusDefaultsToAdded() {
    RecordingTestMapper mapper = new RecordingTestMapper(EvidenceKind.MODULE, 1);
    EvidenceKindMapperRegistry registry = new EvidenceKindMapperRegistry();
    registry.register(mapper);
    RepositoryEvidenceModel model = RepositoryEvidenceModel.of(java.util.List.of(moduleItem()), java.util.List.of());

    MappingResult result =
        new MappingOrchestrator(registry, FIXED_CLOCK).construct(model, Map.of(), PriorElementLookup.none());

    assertEquals(1, mapper.invocations.size());
    assertEquals(1, result.elements().size());
  }

  static PriorElementLookup priorLookup(EvidenceId evidenceId, CsmElement element, int mapperVersion) {
    PriorElementLookup.PriorElement priorElement = new PriorElementLookup.PriorElement(element, mapperVersion);
    return candidateId -> Optional.of(priorElement).filter(ignored -> candidateId.equals(evidenceId));
  }

  private static PriorElementLookup priorLookup(CsmElement element, int mapperVersion) {
    return priorLookup(MODULE_ID, element, mapperVersion);
  }

  private MappingResult construct(RecordingTestMapper mapper, ChangeStatus changeStatus, PriorElementLookup priorElements) {
    EvidenceKindMapperRegistry registry = new EvidenceKindMapperRegistry();
    registry.register(mapper);
    RepositoryEvidenceModel model = RepositoryEvidenceModel.of(java.util.List.of(moduleItem()), java.util.List.of());

    return new MappingOrchestrator(registry, FIXED_CLOCK)
        .construct(model, Map.of(MODULE_ID, changeStatus), priorElements);
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
