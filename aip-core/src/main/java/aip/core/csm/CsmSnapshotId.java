package aip.core.csm;

import java.util.Objects;

/**
 * A CSM Snapshot's stable identity, per {@code CSM Snapshot Source
 * Shape}: "a stable identity for that snapshot (at minimum, the
 * repository identifier and a value distinguishing it from other
 * snapshots of the same repository)."
 *
 * <p>Structurally aligned with, but independent of, {@code
 * aip-csm-builder}'s own {@code Snapshot} identity shape
 * ({@code repositoryIdentifier}, {@code sequenceNumber}) — no
 * dependency is created by this alignment; a future adapter making a
 * real {@code Snapshot} satisfy {@link CsmSnapshotSource} would simply
 * map those same two fields onto this type's own two fields directly.
 *
 * @param repositoryIdentifier the repository this snapshot was
 *     constructed for. Never blank.
 * @param sequenceNumber the value distinguishing this snapshot from
 *     other snapshots of the same repository. At least 1.
 */
public record CsmSnapshotId(String repositoryIdentifier, long sequenceNumber) {

  public CsmSnapshotId {
    Objects.requireNonNull(repositoryIdentifier, "repositoryIdentifier");
    if (repositoryIdentifier.isBlank()) {
      throw new IllegalArgumentException("repositoryIdentifier must not be blank");
    }
    if (sequenceNumber < 1) {
      throw new IllegalArgumentException("sequenceNumber must be at least 1");
    }
  }

  @Override
  public String toString() {
    return repositoryIdentifier + "@" + sequenceNumber;
  }
}
