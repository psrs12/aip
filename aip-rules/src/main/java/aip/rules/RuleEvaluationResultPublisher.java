package aip.rules;

import aip.core.csm.AnalysisResultSource;
import aip.core.csm.AnalysisView;
import aip.core.csm.RuleEvaluationResult;
import aip.core.csm.ValidationResult;
import java.util.Objects;
import java.util.Optional;

/**
 * The Rule Framework's publication gate, per {@code Rule Evaluation
 * Result Validation Before Publication}: "A Rule Evaluation Result
 * that fails this validation SHALL NOT be published as usable output"
 * — mirroring {@code aip-analysis}'s own {@code
 * AnalysisResultPublisher} pattern.
 */
public final class RuleEvaluationResultPublisher {

  private RuleEvaluationResultPublisher() {}

  public static PublicationOutcome publish(
      RuleEvaluationResultStore store,
      RuleEvaluationResult result,
      AnalysisView view,
      AnalysisResultSource analysisResultSource,
      RuleTypeRegistry ruleTypeRegistry,
      RuleRegistry ruleRegistry) {
    Objects.requireNonNull(store, "store");
    Objects.requireNonNull(result, "result");
    Objects.requireNonNull(view, "view");
    Objects.requireNonNull(analysisResultSource, "analysisResultSource");
    Objects.requireNonNull(ruleTypeRegistry, "ruleTypeRegistry");
    Objects.requireNonNull(ruleRegistry, "ruleRegistry");

    ValidationResult validation =
        RuleEvaluationResultValidator.validate(result, view, analysisResultSource, ruleTypeRegistry, ruleRegistry);
    if (!validation.valid()) {
      return new PublicationOutcome(Optional.empty(), validation);
    }

    RuleEvaluationResult written = store.write(result);
    return new PublicationOutcome(Optional.of(written), validation);
  }

  /** @param result present only when {@code validation} passed and the result was actually written. */
  public record PublicationOutcome(Optional<RuleEvaluationResult> result, ValidationResult validation) {

    public PublicationOutcome {
      Objects.requireNonNull(result, "result");
      Objects.requireNonNull(validation, "validation");
      if (result.isPresent() != validation.valid()) {
        throw new IllegalArgumentException("a result is present if, and only if, validation passed");
      }
    }

    public boolean published() {
      return result.isPresent();
    }
  }
}
