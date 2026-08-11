package aip.csmbuilder.snapshot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.core.csm.CsmElement;
import aip.core.csm.CsmElementId;
import aip.core.csm.CsmRelationship;
import aip.core.csm.CsmRelationshipType;
import aip.core.csm.ModuleElement;
import aip.core.csm.NativeAttributes;
import aip.core.csm.ProvenanceRecord;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** {@link SnapshotPublisher} tests (tasks.md 19.3): a passing and a failing validation outcome. */
class SnapshotPublisherTest {

  private static final Instant TIMESTAMP = Instant.parse("2026-03-01T00:00:00Z");

  @TempDir Path tempDir;

  @Test
  void validSnapshotIsPublished() {
    SnapshotStore store = new FilesystemSnapshotStore(tempDir);
    CsmElementId moduleId = new CsmElementId("csm:module-a");
    CsmElement module =
        new ModuleElement(
            moduleId, "module-a", ProvenanceRecord.observed("evidence:1", TIMESTAMP), NativeAttributes.empty());
    SnapshotContent content =
        new SnapshotContent(
            List.of(module), List.of(), Map.of(moduleId, new MapperAttribution("moduleMapper", 1)));

    SnapshotPublisher.PublicationOutcome outcome = SnapshotPublisher.publish(store, "repo-1", content, TIMESTAMP);

    assertTrue(outcome.published());
    assertTrue(outcome.validation().valid());
    assertTrue(outcome.snapshot().isPresent());
    assertEquals(List.of(1L), store.sequenceNumbers("repo-1"), "the valid snapshot was actually written");
  }

  @Test
  void invalidSnapshotIsNotPublished() {
    SnapshotStore store = new FilesystemSnapshotStore(tempDir);
    CsmRelationship observedBoundary =
        CsmRelationship.of(
            new CsmElementId("csm:rel:1"),
            CsmRelationshipType.BOUNDARY_CONSTRAINT,
            new CsmElementId("csm:module-a"),
            new CsmElementId("csm:module-b"),
            ProvenanceRecord.observed("evidence:1", TIMESTAMP),
            NativeAttributes.empty());
    SnapshotContent content = new SnapshotContent(List.of(), List.of(observedBoundary), Map.of());

    SnapshotPublisher.PublicationOutcome outcome = SnapshotPublisher.publish(store, "repo-1", content, TIMESTAMP);

    assertFalse(outcome.published());
    assertFalse(outcome.validation().valid());
    assertTrue(outcome.snapshot().isEmpty());
    assertTrue(store.sequenceNumbers("repo-1").isEmpty(), "the invalid snapshot was never written to the store");
  }
}
