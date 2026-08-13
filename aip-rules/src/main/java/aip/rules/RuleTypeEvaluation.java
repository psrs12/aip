package aip.rules;

import aip.core.csm.RuleEvaluationOutcome;
import java.util.Objects;

/**
 * A Rule Type's own evaluation outcome for one applicable Rule Scope
 * instance, per {@code Rule Evaluation Outcome Semantics}: an exact
 * {@link RuleEvaluationOutcome} plus the opaque, Rule-Type-defined
 * diagnostic payload {@link RuleEvaluationOrchestrator} wraps into a
 * full {@link aip.core.csm.RuleEvaluationResult}, computing identity/
 * traceability from the invocation context, not from anything the
 * Rule Type itself supplies.
 */
public record RuleTypeEvaluation(RuleEvaluationOutcome outcome, Object payload) {

  public RuleTypeEvaluation {
    Objects.requireNonNull(outcome, "outcome");
    Objects.requireNonNull(payload, "payload");
  }

  public static RuleTypeEvaluation pass(Object payload) {
    return new RuleTypeEvaluation(RuleEvaluationOutcome.PASS, payload);
  }

  public static RuleTypeEvaluation fail(Object payload) {
    return new RuleTypeEvaluation(RuleEvaluationOutcome.FAIL, payload);
  }

  public static RuleTypeEvaluation notApplicable(Object payload) {
    return new RuleTypeEvaluation(RuleEvaluationOutcome.NOT_APPLICABLE, payload);
  }
}
