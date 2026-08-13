package aip.ai;

import aip.core.csm.Finding;
import java.util.Optional;

/**
 * The invocation pipeline: one {@link Finding} in, one {@link
 * Recommendation} out — or none, on generation failure — per {@code
 * Agent Contract} and {@code Failure Semantics}.
 *
 * <p>Mints the {@link GenerationIdentifier} for this invocation
 * itself (the only caller of {@link GenerationIdentifier#generate()}
 * anywhere in this codebase) — an {@link Agent} never mints its own,
 * keeping "the framework assigns identity-bearing components, never
 * the Agent" a structural guarantee (`implement-agent-framework/
 * design.md` Decisions 3, 6).
 *
 * <p>If {@link Agent#invoke(Finding)} throws an unchecked exception
 * (signaling a generation failure — a model call failing or timing
 * out), this returns {@link Optional#empty()}: no partially-
 * constructed or invalid Recommendation is ever built, per {@code
 * Failure Semantics}.
 */
public final class RecommendationConstructor {

  private RecommendationConstructor() {}

  public static Optional<Recommendation> construct(Agent agent, Finding finding) {
    GenerationIdentifier generationIdentifier = GenerationIdentifier.generate();
    AgentInvocationResult result;
    try {
      result = agent.invoke(finding);
    } catch (RuntimeException generationFailure) {
      return Optional.empty();
    }
    Recommendation recommendation =
        Recommendation.of(
            agent.identifier(),
            agent.version(),
            finding.id(),
            finding.logicalFindingIdentity(),
            generationIdentifier,
            result.content(),
            result.confidence(),
            result.provenance());
    return Optional.of(recommendation);
  }
}
