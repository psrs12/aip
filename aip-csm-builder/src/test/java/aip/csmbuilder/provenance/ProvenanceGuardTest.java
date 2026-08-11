package aip.csmbuilder.provenance;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import aip.core.csm.CsmElementId;
import aip.core.csm.ModuleElement;
import aip.core.csm.NativeAttributes;
import aip.core.csm.ProvenanceRecord;
import aip.core.evidence.DiscoveryOutcome;
import aip.core.evidence.EvidenceAttributes;
import aip.core.evidence.EvidenceId;
import aip.core.evidence.EvidenceItem;
import aip.core.evidence.EvidenceKind;
import aip.core.evidence.ExtractionMethod;
import aip.csmbuilder.mapping.MappingResult;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProvenanceGuardTest {

  private static final EvidenceItem DRIVING_ITEM =
      new EvidenceItem(
          new EvidenceId("repo-1", EvidenceKind.MODULE, "com.acme:module-a"),
          EvidenceAttributes.empty(),
          Optional.empty(),
          DiscoveryOutcome.complete(),
          ExtractionMethod.MANIFEST_DECLARED);

  @Test
  void wellFormedResultPassesVerification() {
    ModuleElement element =
        new ModuleElement(
            new CsmElementId("csm:module-a"),
            "module-a",
            ObservedProvenanceFactory.fromEvidence(DRIVING_ITEM.id(), Instant.EPOCH),
            NativeAttributes.empty());

    assertDoesNotThrow(() -> ProvenanceGuard.verify(MappingResult.ofElement(element), DRIVING_ITEM));
  }

  @Test
  void declaredProvenanceIsRejected() {
    ModuleElement element =
        new ModuleElement(
            new CsmElementId("csm:module-a"),
            "module-a",
            ProvenanceRecord.declared(DRIVING_ITEM.id().toString(), Instant.EPOCH),
            NativeAttributes.empty());

    assertThrows(
        IllegalStateException.class,
        () -> ProvenanceGuard.verify(MappingResult.ofElement(element), DRIVING_ITEM));
  }

  @Test
  void confidenceOnDeclaredProvenanceIsRejected() {
    // Even though aip.core.csm.ProvenanceRecord permits a DECLARED
    // record to carry confidence in general, CSM Builder's own guard
    // rejects DECLARED outright before confidence is even considered
    // (CSM Builder only ever constructs OBSERVED knowledge) — this
    // test exercises that a record carrying confidence is caught by
    // the guard regardless of which check catches it first.
    ModuleElement element =
        new ModuleElement(
            new CsmElementId("csm:module-a"),
            "module-a",
            ProvenanceRecord.declared(
                DRIVING_ITEM.id().toString(), Instant.EPOCH, aip.core.csm.Confidence.HIGH),
            NativeAttributes.empty());

    assertThrows(
        IllegalStateException.class,
        () -> ProvenanceGuard.verify(MappingResult.ofElement(element), DRIVING_ITEM));
  }

  @Test
  void mismatchedSourceReferenceIsRejected() {
    ModuleElement element =
        new ModuleElement(
            new CsmElementId("csm:module-a"),
            "module-a",
            ObservedProvenanceFactory.fromEvidence(
                new EvidenceId("repo-1", EvidenceKind.MODULE, "com.acme:some-other-module"),
                Instant.EPOCH),
            NativeAttributes.empty());

    assertThrows(
        IllegalStateException.class,
        () -> ProvenanceGuard.verify(MappingResult.ofElement(element), DRIVING_ITEM));
  }

  @Test
  void joinedMultiEvidenceSourceReferenceContainingTheDrivingItemIsTraceable() {
    EvidenceId other = new EvidenceId("repo-1", EvidenceKind.MODULE, "com.acme:module-z");
    ModuleElement element =
        new ModuleElement(
            new CsmElementId("csm:module-a"),
            "module-a",
            ObservedProvenanceFactory.fromEvidence(List.of(DRIVING_ITEM.id(), other), Instant.EPOCH),
            NativeAttributes.empty());

    assertDoesNotThrow(() -> ProvenanceGuard.verify(MappingResult.ofElement(element), DRIVING_ITEM));
  }
}
