package aip.core.csm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

/** {@link CsmSnapshotId} tests, per {@code CSM Snapshot Source Shape}. */
class CsmSnapshotIdTest {

  @Test
  void rejectsBlankRepositoryIdentifier() {
    assertThrows(IllegalArgumentException.class, () -> new CsmSnapshotId("  ", 1));
  }

  @Test
  void rejectsNonPositiveSequenceNumber() {
    assertThrows(IllegalArgumentException.class, () -> new CsmSnapshotId("repo", 0));
    assertThrows(IllegalArgumentException.class, () -> new CsmSnapshotId("repo", -1));
  }

  @Test
  void equalRepositoryAndSequenceAreEqual() {
    assertEquals(new CsmSnapshotId("repo", 1), new CsmSnapshotId("repo", 1));
  }

  @Test
  void differentSequenceNumbersAreDistinguishable() {
    assertNotEquals(new CsmSnapshotId("repo", 1), new CsmSnapshotId("repo", 2));
  }
}
