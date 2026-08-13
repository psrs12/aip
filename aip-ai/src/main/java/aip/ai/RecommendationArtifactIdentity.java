package aip.ai;

import aip.core.csm.EvaluationIdentity;
import java.util.Objects;

/**
 * A {@link Recommendation}'s deterministic Artifact Identity, per
 * {@code Recommendation Artifact Identity}: "a deterministic function
 * of: the producing Agent's identifier, the Agent's version, the
 * referenced Finding's Evaluation Identity, and an opaque,
 * invocation-scoped Generation Identifier."
 *
 * <p>Deliberately excludes generated content, Confidence, and
 * Generation Provenance (per {@code Recommendation Artifact Identity
 * Excludes Generated Content}) — this is the first identity scheme in
 * this project whose *reproducible-lookup* guarantee does not imply a
 * *reproducible-generation* guarantee: given the same recorded
 * identity components, the same Recommendation Artifact Identity
 * always results, but nothing about this computation claims that
 * regenerating from the same (Agent, version, Finding) would produce
 * similar content (`implement-agent-framework/design.md` Decision 3).
 *
 * @param value the opaque, deterministic digest string. Never blank.
 */
public record RecommendationArtifactIdentity(String value) {

  public RecommendationArtifactIdentity {
    Objects.requireNonNull(value, "value");
    if (value.isBlank()) {
      throw new IllegalArgumentException("RecommendationArtifactIdentity value must not be blank");
    }
  }

  /**
   * Computes a {@code RecommendationArtifactIdentity} as a
   * deterministic function of exactly these four components — never
   * generated content, Confidence, or Generation Provenance.
   */
  public static RecommendationArtifactIdentity of(
      String agentIdentifier,
      int agentVersion,
      EvaluationIdentity findingEvaluationIdentity,
      GenerationIdentifier generationIdentifier) {
    Objects.requireNonNull(agentIdentifier, "agentIdentifier");
    Objects.requireNonNull(findingEvaluationIdentity, "findingEvaluationIdentity");
    Objects.requireNonNull(generationIdentifier, "generationIdentifier");
    return new RecommendationArtifactIdentity(
        DeterministicHash.of(agentIdentifier, agentVersion, findingEvaluationIdentity, generationIdentifier));
  }

  @Override
  public String toString() {
    return value;
  }
}
