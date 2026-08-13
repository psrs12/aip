package aip.analysis;

import aip.core.csm.AnalysisResult;
import aip.core.csm.AnalysisView;
import aip.core.csm.ValidationResult;
import java.util.Objects;
import java.util.Optional;

/**
 * The Analysis Framework's publication gate, per {@code Analysis
 * Result Validation Before Publication}: "An Analysis Result that
 * fails this validation SHALL NOT be published as usable output" —
 * mirroring {@code aip-csm-builder}'s own {@code SnapshotPublisher}
 * pattern: a layer in front of {@link AnalysisResultStore}, not inside
 * it, so the store itself stays a general, validation-agnostic
 * persistence mechanism.
 */
public final class AnalysisResultPublisher {

  private AnalysisResultPublisher() {}

  public static PublicationOutcome publish(
      AnalysisResultStore store, AnalysisResult result, AnalysisView view, AnalyzerRegistry registry) {
    Objects.requireNonNull(store, "store");
    Objects.requireNonNull(result, "result");
    Objects.requireNonNull(view, "view");
    Objects.requireNonNull(registry, "registry");

    ValidationResult validation = AnalysisResultValidator.validate(result, view, registry);
    if (!validation.valid()) {
      return new PublicationOutcome(Optional.empty(), validation);
    }

    AnalysisResult written = store.write(result);
    return new PublicationOutcome(Optional.of(written), validation);
  }

  /** @param result present only when {@code validation} passed and the result was actually written. */
  public record PublicationOutcome(Optional<AnalysisResult> result, ValidationResult validation) {

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
