package aip.csmbuilder.snapshot;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * The Snapshot Store abstraction: writes and retrieves CSM snapshots,
 * per {@code Snapshot-Based CSM Construction} and {@code design.md}
 * Decision 2 (tasks.md 15.1) — "so the persistence mechanism can be
 * replaced later without changing CSM Builder's construction logic"
 * (invariant 5). {@link FilesystemSnapshotStore} is the only
 * implementation this change provides; nothing in {@code
 * aip.csmbuilder.mapping} depends on either this interface or that
 * implementation.
 *
 * <p>Every method is scoped to one repository identifier — cross-
 * repository snapshot listing/comparison is not a CSM Builder concern.
 */
public interface SnapshotStore {

  /**
   * Writes {@code content} as a new, immutable snapshot for {@code
   * repositoryIdentifier}, assigning it the next sequence number for
   * that repository (starting at 1). Never overwrites or mutates a
   * previously written snapshot.
   */
  Snapshot write(String repositoryIdentifier, SnapshotContent content, Instant constructionTimestamp);

  /**
   * Every sequence number written so far for {@code
   * repositoryIdentifier}, ascending. Empty if none has been written.
   */
  List<Long> sequenceNumbers(String repositoryIdentifier);

  /** The snapshot at {@code sequenceNumber} for {@code repositoryIdentifier}, if one was written. */
  Optional<Snapshot> read(String repositoryIdentifier, long sequenceNumber);

  /** The most recently written snapshot for {@code repositoryIdentifier}, if any. */
  default Optional<Snapshot> readLatest(String repositoryIdentifier) {
    List<Long> sequenceNumbers = sequenceNumbers(repositoryIdentifier);
    if (sequenceNumbers.isEmpty()) {
      return Optional.empty();
    }
    return read(repositoryIdentifier, sequenceNumbers.get(sequenceNumbers.size() - 1));
  }
}
