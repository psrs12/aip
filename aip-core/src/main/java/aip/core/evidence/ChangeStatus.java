package aip.core.evidence;

/**
 * An Evidence Item's per-run change-status classification, per
 * {@code Change Detection Classification}: "Repository Understanding
 * SHALL classify each Evidence Item's change status as exactly one of:
 * {@code ADDED}, {@code UNCHANGED}, {@code MODIFIED}, or {@code REMOVED}."
 *
 * <p>Distinct from, and never interchangeable with, {@link LifecycleState}
 * — change status describes what happened in a given discovery run;
 * lifecycle state describes an Evidence Item's current standing. See
 * that requirement's own text: "These two vocabularies SHALL NOT be
 * used interchangeably."
 */
public enum ChangeStatus {
  ADDED,
  UNCHANGED,
  MODIFIED,
  REMOVED
}
