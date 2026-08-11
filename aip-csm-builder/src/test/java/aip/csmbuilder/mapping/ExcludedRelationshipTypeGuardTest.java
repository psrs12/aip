package aip.csmbuilder.mapping;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import aip.core.csm.CsmElementId;
import aip.core.csm.CsmRelationship;
import aip.core.csm.CsmRelationshipType;
import aip.core.csm.NativeAttributes;
import aip.core.csm.ProvenanceRecord;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Guard tests (tasks.md 13.2): CSM Builder never constructs an {@code
 * implementation/extension} or {@code invocation} relationship, per
 * {@code Exclusion of Relationship Types Without Corresponding
 * Evidence}.
 */
class ExcludedRelationshipTypeGuardTest {

  private static final ProvenanceRecord PROVENANCE = ProvenanceRecord.observed("evidence:1", Instant.EPOCH);
  private static final CsmElementId SOURCE = new CsmElementId("csm:source");
  private static final CsmElementId TARGET = new CsmElementId("csm:target");

  @Test
  void implementationExtensionRelationshipIsRejected() {
    MappingResult result =
        MappingResult.ofRelationship(relationship(CsmRelationshipType.IMPLEMENTATION_EXTENSION));

    assertThrows(IllegalStateException.class, () -> ExcludedRelationshipTypeGuard.verify(result));
  }

  @Test
  void invocationRelationshipIsRejected() {
    MappingResult result = MappingResult.ofRelationship(relationship(CsmRelationshipType.INVOCATION));

    assertThrows(IllegalStateException.class, () -> ExcludedRelationshipTypeGuard.verify(result));
  }

  @Test
  void everyOtherRelationshipTypeIsAccepted() {
    for (CsmRelationshipType type : CsmRelationshipType.values()) {
      if (type == CsmRelationshipType.IMPLEMENTATION_EXTENSION || type == CsmRelationshipType.INVOCATION) {
        continue;
      }
      MappingResult result = MappingResult.ofRelationship(relationship(type));
      assertDoesNotThrow(() -> ExcludedRelationshipTypeGuard.verify(result), () -> type + " should be accepted");
    }
  }

  @Test
  void emptyResultIsAccepted() {
    assertDoesNotThrow(() -> ExcludedRelationshipTypeGuard.verify(new MappingResult(List.of(), List.of())));
  }

  private static CsmRelationship relationship(CsmRelationshipType type) {
    return CsmRelationship.of(
        new CsmElementId("csm:rel:1"), type, SOURCE, TARGET, PROVENANCE, NativeAttributes.empty());
  }
}
