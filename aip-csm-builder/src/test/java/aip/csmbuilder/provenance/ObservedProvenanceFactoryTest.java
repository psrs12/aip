package aip.csmbuilder.provenance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.core.csm.ProvenanceCategory;
import aip.core.csm.ProvenanceRecord;
import aip.core.evidence.EvidenceId;
import aip.core.evidence.EvidenceKind;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ObservedProvenanceFactoryTest {

  private static final EvidenceId MODULE_A =
      new EvidenceId("repo-1", EvidenceKind.MODULE, "com.acme:module-a");
  private static final EvidenceId MODULE_B =
      new EvidenceId("repo-1", EvidenceKind.MODULE, "com.acme:module-b");

  @Test
  void singleEvidenceProvenanceIsAlwaysObservedWithNoConfidence() {
    ProvenanceRecord record = ObservedProvenanceFactory.fromEvidence(MODULE_A, Instant.EPOCH);

    assertEquals(ProvenanceCategory.OBSERVED, record.category());
    assertTrue(record.confidence().isEmpty());
    assertEquals(MODULE_A.toString(), record.sourceReference());
    assertEquals(Instant.EPOCH, record.timestamp());
  }

  @Test
  void multiEvidenceProvenanceJoinsSortedIdentitiesDeterministically() {
    ProvenanceRecord recordAB =
        ObservedProvenanceFactory.fromEvidence(List.of(MODULE_A, MODULE_B), Instant.EPOCH);
    ProvenanceRecord recordBA =
        ObservedProvenanceFactory.fromEvidence(List.of(MODULE_B, MODULE_A), Instant.EPOCH);

    assertEquals(recordAB.sourceReference(), recordBA.sourceReference(), "input order must not matter");
    assertEquals(ProvenanceCategory.OBSERVED, recordAB.category());
    assertTrue(recordAB.confidence().isEmpty());
  }

  @Test
  void multiEvidenceProvenanceRequiresAtLeastOneEvidenceId() {
    assertThrows(
        IllegalArgumentException.class,
        () -> ObservedProvenanceFactory.fromEvidence(List.of(), Instant.EPOCH));
  }
}
