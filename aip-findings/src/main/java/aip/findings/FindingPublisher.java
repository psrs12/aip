package aip.findings;

import aip.core.csm.Finding;
import aip.core.csm.RuleEvaluationResultSource;
import aip.core.csm.ValidationResult;
import java.util.Objects;
import java.util.Optional;

/**
 * The Finding Model's publication gate, per {@code Finding Validation
 * Before Publication}: "A Finding that fails validation SHALL NOT be
 * published as usable output" — mirroring {@code aip-rules}'s own
 * {@code RuleEvaluationResultPublisher} pattern.
 */
public final class FindingPublisher {

  private FindingPublisher() {}

  public static PublicationOutcome publish(
      FindingStore store, Finding finding, RuleEvaluationResultSource ruleEvaluationResultSource) {
    Objects.requireNonNull(store, "store");
    Objects.requireNonNull(finding, "finding");
    Objects.requireNonNull(ruleEvaluationResultSource, "ruleEvaluationResultSource");

    ValidationResult validation = FindingValidator.validate(finding, ruleEvaluationResultSource);
    if (!validation.valid()) {
      return new PublicationOutcome(Optional.empty(), validation);
    }

    Finding written = store.write(finding);
    return new PublicationOutcome(Optional.of(written), validation);
  }

  /** @param finding present only when {@code validation} passed and the Finding was actually written. */
  public record PublicationOutcome(Optional<Finding> finding, ValidationResult validation) {

    public PublicationOutcome {
      Objects.requireNonNull(finding, "finding");
      Objects.requireNonNull(validation, "validation");
      if (finding.isPresent() != validation.valid()) {
        throw new IllegalArgumentException("a finding is present if, and only if, validation passed");
      }
    }

    public boolean published() {
      return finding.isPresent();
    }
  }
}
