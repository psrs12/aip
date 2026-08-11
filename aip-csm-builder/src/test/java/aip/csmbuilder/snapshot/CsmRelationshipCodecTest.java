package aip.csmbuilder.snapshot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import aip.core.csm.CsmElementId;
import aip.core.csm.CsmRelationship;
import aip.core.csm.CsmRelationshipType;
import aip.core.csm.DependencyKind;
import aip.core.csm.NativeAttributes;
import aip.core.csm.ProvenanceRecord;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Encode/decode round-trip coverage for {@link CsmRelationship} (tasks.md 15.3). */
class CsmRelationshipCodecTest {

  private static final Instant TIMESTAMP = Instant.parse("2026-01-01T00:00:00Z");
  private static final ProvenanceRecord OBSERVED = ProvenanceRecord.observed("repo-1:MODULE:x", TIMESTAMP);
  private static final CsmElementId SOURCE = new CsmElementId("csm:source");
  private static final CsmElementId TARGET = new CsmElementId("csm:target");

  @Test
  void relationshipWithConcreteTargetRoundTrips() {
    assertRoundTrips(
        CsmRelationship.of(
            new CsmElementId("csm:rel:1"),
            CsmRelationshipType.CONTAINMENT,
            SOURCE,
            TARGET,
            OBSERVED,
            NativeAttributes.empty()));
  }

  @Test
  void relationshipWithUnevidencedTargetRoundTrips() {
    assertRoundTrips(
        CsmRelationship.withUnevidencedTarget(
            new CsmElementId("csm:rel:1"),
            CsmRelationshipType.EXPOSURE_CONSUMPTION,
            SOURCE,
            OBSERVED,
            NativeAttributes.empty()));
  }

  @Test
  void dependencyRelationshipWithKindQualifierRoundTrips() {
    assertRoundTrips(
        new CsmRelationship(
            new CsmElementId("csm:rel:1"),
            CsmRelationshipType.DEPENDENCY,
            SOURCE,
            java.util.Optional.of(TARGET),
            OBSERVED,
            NativeAttributes.empty(),
            java.util.Optional.of(DependencyKind.COMPILE_TIME)));
  }

  @Test
  void dependencyRelationshipWithoutKindQualifierRoundTrips() {
    assertRoundTrips(
        CsmRelationship.of(
            new CsmElementId("csm:rel:1"),
            CsmRelationshipType.DEPENDENCY,
            SOURCE,
            TARGET,
            OBSERVED,
            NativeAttributes.empty()));
  }

  private static void assertRoundTrips(CsmRelationship relationship) {
    String encoded = CsmRelationshipCodec.encode(relationship);
    CsmRelationship decoded = CsmRelationshipCodec.decode(encoded);
    assertEquals(relationship, decoded);
  }
}
