package aip.core.csm;

/**
 * The exhaustive outcome vocabulary for an evaluated Rule Scope
 * instance, per {@code Rule Evaluation Outcome Semantics}: "{@code
 * PASS} (the Rule's declared condition was evaluated and satisfied),
 * {@code FAIL} (the Rule's declared condition was evaluated and not
 * satisfied), or {@code NOT_APPLICABLE} (the Rule Scope instance was
 * evaluated, but content the Rule's condition specifically depends on
 * was not present, so no PASS/FAIL determination could be made)."
 *
 * <p>{@code NOT_APPLICABLE} is a produced {@link RuleEvaluationResult},
 * not the absence of one — distinct from a Rule Scope instance that
 * failed kind-based or native-attribute applicability, for which no
 * {@link RuleEvaluationResult} is produced at all. This is why this
 * enum has exactly three values, not four: there is deliberately no
 * fourth value (e.g. a {@code NOT_EVALUATED} sentinel) representing
 * the no-Result case — see {@code
 * implement-rule-framework/design.md} Decision 4, Alternatives
 * Considered.
 */
public enum RuleEvaluationOutcome {
  PASS,
  FAIL,
  NOT_APPLICABLE
}
