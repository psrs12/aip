package aip.core.csm;

/**
 * The reconciliation outcome for one {@link Subject}'s competing
 * knowledge assertions, per {@code Effective Knowledge and Precedence}
 * and {@code Same-Category Conflict Marking and Resolution}.
 *
 * <p>{@link #EFFECTIVE} covers both "exactly one assertion exists for
 * this subject" and "multiple assertions exist, but a higher-precedence
 * one — {@code DECLARED} over {@code OBSERVED} over {@code INFERRED} —
 * determines the effective view." This implementation does not yet
 * compute the second case: nothing in this codebase currently produces
 * {@code declared} or {@code inferred} CSM content to reconcile against
 * {@code observed} content, so cross-category precedence is deliberately
 * not built here (see {@code
 * openspec/changes/implement-csm-builder/design.md} Decision 10). {@link
 * SubjectConflictMarker} only ever computes {@link #CONFLICTED} for
 * multiple assertions <em>within the same provenance category</em> —
 * the one case CSM Builder can actually exercise, since it only ever
 * constructs {@code observed} content.
 */
public enum EffectiveKnowledgeStatus {
  EFFECTIVE,
  CONFLICTED
}
