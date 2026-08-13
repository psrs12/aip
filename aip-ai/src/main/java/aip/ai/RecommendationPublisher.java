package aip.ai;

import aip.core.csm.FindingSource;
import aip.core.csm.ValidationResult;
import java.util.Objects;
import java.util.Optional;

/**
 * The Agent Framework's publication gate, per {@code Recommendation
 * Publishing}: "A Recommendation Publisher SHALL call the
 * Recommendation persistence mechanism's write operation only for a
 * Recommendation that has passed validation" — mirroring {@code
 * aip-findings}'s own {@code FindingPublisher} pattern.
 */
public final class RecommendationPublisher {

  private RecommendationPublisher() {}

  public static PublicationOutcome publish(
      RecommendationStore store, Recommendation recommendation, FindingSource findingSource, AgentRegistry agentRegistry) {
    Objects.requireNonNull(store, "store");
    Objects.requireNonNull(recommendation, "recommendation");
    Objects.requireNonNull(findingSource, "findingSource");
    Objects.requireNonNull(agentRegistry, "agentRegistry");

    ValidationResult validation = RecommendationValidator.validate(recommendation, findingSource, agentRegistry);
    if (!validation.valid()) {
      return new PublicationOutcome(Optional.empty(), validation);
    }

    Recommendation written = store.write(recommendation);
    return new PublicationOutcome(Optional.of(written), validation);
  }

  /** @param recommendation present only when {@code validation} passed and the Recommendation was actually written. */
  public record PublicationOutcome(Optional<Recommendation> recommendation, ValidationResult validation) {

    public PublicationOutcome {
      Objects.requireNonNull(recommendation, "recommendation");
      Objects.requireNonNull(validation, "validation");
      if (recommendation.isPresent() != validation.valid()) {
        throw new IllegalArgumentException("a recommendation is present if, and only if, validation passed");
      }
    }

    public boolean published() {
      return recommendation.isPresent();
    }
  }
}
