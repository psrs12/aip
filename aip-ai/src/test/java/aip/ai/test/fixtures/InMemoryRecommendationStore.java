package aip.ai.test.fixtures;

import aip.ai.Recommendation;
import aip.ai.RecommendationArtifactIdentity;
import aip.ai.RecommendationStore;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A test-only, in-memory {@link RecommendationStore} implementation —
 * concrete persistence technology is deliberately deferred, so
 * production code never chooses one; this exists only so this
 * module's own tests can exercise the {@link RecommendationStore}
 * abstraction end to end.
 */
public final class InMemoryRecommendationStore implements RecommendationStore {

  private final Map<RecommendationArtifactIdentity, Recommendation> byId = new LinkedHashMap<>();

  @Override
  public Recommendation write(Recommendation recommendation) {
    Objects.requireNonNull(recommendation, "recommendation");
    if (byId.containsKey(recommendation.id())) {
      throw new IllegalStateException(
          "a Recommendation is already stored for identity " + recommendation.id() + "; a store SHALL NOT"
              + " overwrite a previously written Recommendation");
    }
    byId.put(recommendation.id(), recommendation);
    return recommendation;
  }

  @Override
  public Optional<Recommendation> read(RecommendationArtifactIdentity id) {
    Objects.requireNonNull(id, "id");
    return Optional.ofNullable(byId.get(id));
  }
}
