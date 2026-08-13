package aip.ai;

import java.util.Objects;

/**
 * The three Agent-declared components of one invocation's output —
 * everything an {@link Agent} itself supplies; everything else on the
 * final {@link Recommendation} (identity, Generation Identifier, Agent
 * identifier/version, referenced Finding's two identities) is computed
 * or supplied by {@link RecommendationConstructor}, never the Agent
 * (`implement-agent-framework/design.md` Decision 7).
 *
 * @param content opaque, Agent-defined guidance content, per {@code
 *     Recommendation Content Is Agent-Defined and Opaque to the
 *     Framework}. Never null.
 * @param confidence the Agent's own declared Confidence, per {@code
 *     Recommendation Confidence Is Agent-Produced and Mandatory}. In
 *     {@code [0.0, 1.0]}.
 * @param provenance the Generation Provenance record, per {@code
 *     Generation Provenance}. Never null.
 */
public record AgentInvocationResult(Object content, double confidence, GenerationProvenance provenance) {

  public AgentInvocationResult {
    Objects.requireNonNull(content, "content");
    Objects.requireNonNull(provenance, "provenance");
    if (Double.isNaN(confidence) || confidence < 0.0 || confidence > 1.0) {
      throw new IllegalArgumentException("confidence must be within [0.0, 1.0], was: " + confidence);
    }
  }
}
