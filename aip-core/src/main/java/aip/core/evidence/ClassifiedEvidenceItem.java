package aip.core.evidence;

import java.util.Objects;
import java.util.Set;

/**
 * An {@link EvidenceItem} paired with its {@link ChangeStatus} and
 * {@link LifecycleState} classification for one discovery run, per
 * {@code Change Detection Classification} and {@code Evidence
 * Lifecycle States}: an {@code ADDED}/{@code UNCHANGED}/{@code MODIFIED}
 * change status always pairs with the {@code PRESENT} lifecycle state;
 * a {@code REMOVED} change status pairs with {@code TOMBSTONED} (or,
 * once the retention window elapses in a later run, {@code PURGED}).
 * This invariant is enforced here, not left to callers to remember.
 */
public record ClassifiedEvidenceItem(
    EvidenceItem item, ChangeStatus changeStatus, LifecycleState lifecycleState) {

  private static final Set<ChangeStatus> PRESENT_STATUSES =
      Set.of(ChangeStatus.ADDED, ChangeStatus.UNCHANGED, ChangeStatus.MODIFIED);

  public ClassifiedEvidenceItem {
    Objects.requireNonNull(item, "item");
    Objects.requireNonNull(changeStatus, "changeStatus");
    Objects.requireNonNull(lifecycleState, "lifecycleState");

    if (PRESENT_STATUSES.contains(changeStatus) && lifecycleState != LifecycleState.PRESENT) {
      throw new IllegalArgumentException(
          "an ADDED/UNCHANGED/MODIFIED change status SHALL pair with the PRESENT lifecycle state,"
              + " not "
              + lifecycleState);
    }
    if (changeStatus == ChangeStatus.REMOVED
        && lifecycleState != LifecycleState.TOMBSTONED
        && lifecycleState != LifecycleState.PURGED) {
      throw new IllegalArgumentException(
          "a REMOVED change status SHALL pair with the TOMBSTONED or PURGED lifecycle state, not "
              + lifecycleState);
    }
  }
}
