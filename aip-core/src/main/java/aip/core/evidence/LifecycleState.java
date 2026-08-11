package aip.core.evidence;

/**
 * An Evidence Item's lifecycle state, per {@code Evidence Lifecycle
 * States}: "exactly one of: {@code PRESENT}, {@code TOMBSTONED}, or
 * {@code PURGED}."
 *
 * <p>An Evidence Item enters or remains {@link #PRESENT} when
 * classified {@link ChangeStatus#ADDED}, {@link ChangeStatus#UNCHANGED},
 * or {@link ChangeStatus#MODIFIED} in a given run, and transitions to
 * {@link #TOMBSTONED} when classified {@link ChangeStatus#REMOVED}. A
 * {@link #TOMBSTONED} item remains retrievable for at least one
 * subsequent discovery run before becoming eligible for {@link #PURGED}.
 * This mapping is process behavior (owned by whatever computes it, not
 * by this enum) — this type only names the three states.
 *
 * <p>Distinct from, and never interchangeable with, {@link ChangeStatus}
 * — see that type's own javadoc.
 */
public enum LifecycleState {
  PRESENT,
  TOMBSTONED,
  PURGED
}
