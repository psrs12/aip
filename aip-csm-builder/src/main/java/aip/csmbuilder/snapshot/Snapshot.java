package aip.csmbuilder.snapshot;

import aip.core.csm.CsmElement;
import aip.core.csm.CsmRelationship;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * One individually identifiable CSM snapshot, as read back from a
 * {@link SnapshotStore}, per {@code Snapshot-Based CSM Construction}.
 * Immutable: once written, a snapshot is never modified — {@link
 * #sequenceNumber()} is unique per {@link #repositoryIdentifier()} and
 * monotonically increasing across writes.
 */
public record Snapshot(
    String repositoryIdentifier,
    long sequenceNumber,
    Instant constructionTimestamp,
    List<CsmElement> elements,
    List<CsmRelationship> relationships,
    SnapshotManifest manifest) {

  public Snapshot {
    Objects.requireNonNull(repositoryIdentifier, "repositoryIdentifier");
    if (repositoryIdentifier.isBlank()) {
      throw new IllegalArgumentException("repositoryIdentifier must not be blank");
    }
    if (sequenceNumber < 1) {
      throw new IllegalArgumentException("sequenceNumber must be at least 1");
    }
    Objects.requireNonNull(constructionTimestamp, "constructionTimestamp");
    Objects.requireNonNull(elements, "elements");
    Objects.requireNonNull(relationships, "relationships");
    Objects.requireNonNull(manifest, "manifest");
    elements = List.copyOf(elements);
    relationships = List.copyOf(relationships);
  }
}
