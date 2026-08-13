package aip.ai.architecturecompliance;

import aip.ai.Agent;
import aip.ai.AgentInvocationResult;
import aip.ai.GenerationProvenance;
import aip.core.csm.Finding;
import java.time.Instant;
import java.util.Map;

/**
 * The first concrete Agent: consumes a boundary-violation {@link
 * Finding} and produces a Recommendation identifying the violation and
 * a general remediation approach, per spec {@code Architecture
 * Compliance Agent Recommendation Content}. Configuration of Agent
 * Framework's already-specified extension mechanism — introduces no
 * new Agent Framework mechanism (`define-architecture-compliance-
 * agent/design.md` Decisions 1, 2, 9).
 *
 * <p>Consumes exactly the {@link Finding} it is given, per {@code
 * Architecture Compliance Agent Consumption Boundary} — no other CSM,
 * Analysis, or RuleEvaluationResult content is read, and no {@code
 * RuleEvaluationResultSource} or other dependency exists on this type
 * beyond what {@link Agent}'s own contract already permits.
 *
 * <p>A deterministic, template-based Agent in v1 — {@link #invoke}
 * never calls an external model, per {@code implement-architecture-
 * compliance-agent/design.md} Decision 6. This is an honest v1
 * realization, not a placeholder: {@link Agent}'s own contract does
 * not require a model call to exist, only that its own code be
 * ordinary, versioned code.
 */
public final class ArchitectureComplianceAgent implements Agent {

  public static final String IDENTIFIER = "agent.architecture-compliance";
  private static final int VERSION = 1;
  private static final double CONFIDENCE = 0.9;
  private static final String REMEDIATION_GUIDANCE =
      "Consider removing this dependency, or introduce an approved integration boundary between"
          + " the two components.";

  @Override
  public String identifier() {
    return IDENTIFIER;
  }

  @Override
  public int version() {
    return VERSION;
  }

  @Override
  public AgentInvocationResult invoke(Finding finding) {
    ArchitectureComplianceRecommendationContent content =
        new ArchitectureComplianceRecommendationContent(
            finding.concernedElementId(), finding.description(), REMEDIATION_GUIDANCE);
    GenerationProvenance provenance =
        new GenerationProvenance("deterministic-template", "v1", Map.of(), Instant.now());
    return new AgentInvocationResult(content, CONFIDENCE, provenance);
  }
}
