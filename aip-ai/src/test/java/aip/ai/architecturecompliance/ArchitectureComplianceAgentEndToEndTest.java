package aip.ai.architecturecompliance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.ai.AgentRegistry;
import aip.ai.Recommendation;
import aip.ai.RecommendationConstructor;
import aip.ai.RecommendationPublisher;
import aip.ai.test.fixtures.InMemoryFindingSource;
import aip.ai.test.fixtures.InMemoryRecommendationStore;
import aip.core.csm.Confidence;
import aip.core.csm.CsmElementId;
import aip.core.csm.CsmScopeInstance;
import aip.core.csm.CsmSnapshotId;
import aip.core.csm.Finding;
import aip.core.csm.RuleEvaluationResultId;
import aip.core.csm.ValidationResult;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * An end-to-end test of this capability's downstream half — a
 * boundary-violation Finding (shaped exactly as {@code
 * BoundaryComplianceDiagnostic} in {@code aip-rules} would produce,
 * since {@code aip-ai} has no dependency on {@code aip-rules} or
 * {@code aip-findings}, per {@code define-agent-framework} Decision 4
 * and `implement-architecture-compliance-agent/design.md` Decision 5)
 * through Agent invocation, Recommendation construction, validation,
 * and publication.
 *
 * <p>Together with {@code aip-rules}'s own {@code
 * BoundaryComplianceRuleTypeTest} (the upstream half: CSM Snapshot to
 * RuleEvaluationResult), this demonstrates the complete pipeline this
 * capability specifies — bridged at the Finding boundary, since no
 * single module may depend on both {@code aip-rules} and {@code
 * aip-ai} without violating the sibling-module independence every
 * deterministic and AI-bearing module in this codebase holds to. A
 * genuine cross-module wiring test would belong to a future
 * integration point (e.g. {@code aip-cli}/{@code aip-server}), not
 * built in this change.
 */
class ArchitectureComplianceAgentEndToEndTest {

  private static final CsmSnapshotId SNAPSHOT = new CsmSnapshotId("repo", 1);
  private static final CsmElementId COMPONENT = new CsmElementId("csm:element:componentA");
  private static final CsmElementId TARGET = new CsmElementId("csm:element:componentB");
  private static final CsmElementId CONSTRAINT = new CsmElementId("csm:element:constraint1");

  @Test
  void invokesConstructsValidatesAndPublishesARecommendationForABoundaryViolation() {
    RuleEvaluationResultId rer =
        RuleEvaluationResultId.of(
            "rule.boundary-compliance", 1, SNAPSHOT, CsmScopeInstance.anchoredAt(COMPONENT), Set.of());
    Finding finding =
        Finding.of(
            "rule.boundary-compliance",
            SNAPSHOT,
            COMPONENT,
            Set.of(rer),
            "architecture-boundary-violation",
            "high",
            Confidence.HIGH,
            "Architecture Component " + COMPONENT + " depends on " + TARGET + ", violating boundary constraint "
                + CONSTRAINT + ".",
            "This dependency crosses a declared or inferred architectural boundary and may introduce"
                + " unwanted coupling.");

    InMemoryFindingSource findingSource = new InMemoryFindingSource().with(finding);
    AgentRegistry agentRegistry = new AgentRegistry();
    ArchitectureComplianceAgent agent = new ArchitectureComplianceAgent();
    agentRegistry.register(agent);
    InMemoryRecommendationStore store = new InMemoryRecommendationStore();

    Recommendation recommendation = RecommendationConstructor.construct(agent, finding).orElseThrow();
    ArchitectureComplianceRecommendationContent content =
        (ArchitectureComplianceRecommendationContent) recommendation.content();
    assertEquals(COMPONENT, content.violatingComponentId());
    assertTrue(content.violationDescription().contains(TARGET.toString()));
    assertTrue(content.violationDescription().contains(CONSTRAINT.toString()));

    ValidationResult validation =
        aip.ai.RecommendationValidator.validate(recommendation, findingSource, agentRegistry);
    assertTrue(validation.valid(), () -> "expected valid: " + validation.violations());

    RecommendationPublisher.PublicationOutcome outcome =
        RecommendationPublisher.publish(store, recommendation, findingSource, agentRegistry);
    assertTrue(outcome.published());
    assertEquals(recommendation.id(), store.read(recommendation.id()).orElseThrow().id());
  }
}
