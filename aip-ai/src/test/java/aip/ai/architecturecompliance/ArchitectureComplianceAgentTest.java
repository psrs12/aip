package aip.ai.architecturecompliance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.ai.AgentInvocationResult;
import aip.ai.AgentRegistry;
import aip.ai.RecommendationConstructor;
import aip.core.csm.Confidence;
import aip.core.csm.CsmElementId;
import aip.core.csm.CsmSnapshotId;
import aip.core.csm.Finding;
import aip.core.csm.RuleEvaluationResultId;
import aip.core.csm.CsmScopeInstance;
import aip.ai.Recommendation;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * {@link ArchitectureComplianceAgent} tests, per {@code Architecture
 * Compliance Agent Consumption Boundary}, {@code Architecture
 * Compliance Agent Recommendation Content}, and {@code Architecture
 * Compliance Agent Recommendation Excludes Code and Proposed Changes}.
 */
class ArchitectureComplianceAgentTest {

  private static final CsmSnapshotId SNAPSHOT = new CsmSnapshotId("repo", 1);
  private static final CsmElementId COMPONENT = new CsmElementId("csm:element:componentA");

  private static Finding boundaryViolationFinding() {
    RuleEvaluationResultId rer =
        RuleEvaluationResultId.of(
            "rule.boundary-compliance", 1, SNAPSHOT, CsmScopeInstance.anchoredAt(COMPONENT), Set.of());
    return Finding.of(
        "rule.boundary-compliance",
        SNAPSHOT,
        COMPONENT,
        Set.of(rer),
        "architecture-boundary-violation",
        "high",
        Confidence.HIGH,
        "Architecture Component csm:element:componentA depends on csm:element:componentB, violating"
            + " boundary constraint csm:element:constraint1.",
        "This dependency crosses a declared or inferred architectural boundary and may introduce"
            + " unwanted coupling.");
  }

  @Test
  void registersWithAgentFrameworksExistingRegistry() {
    AgentRegistry registry = new AgentRegistry();
    ArchitectureComplianceAgent agent = new ArchitectureComplianceAgent();
    registry.register(agent);
    assertEquals(agent, registry.lookup(ArchitectureComplianceAgent.IDENTIFIER).orElseThrow());
  }

  @Test
  void invocationConsumesOnlyTheGivenFindingAndProducesOneRecommendation() {
    Finding finding = boundaryViolationFinding();
    ArchitectureComplianceAgent agent = new ArchitectureComplianceAgent();
    var recommendation = RecommendationConstructor.construct(agent, finding);
    assertTrue(recommendation.isPresent());
  }

  @Test
  void recommendationContentIdentifiesTheComponentAndDependencyTarget() {
    Finding finding = boundaryViolationFinding();
    AgentInvocationResult result = new ArchitectureComplianceAgent().invoke(finding);
    ArchitectureComplianceRecommendationContent content = (ArchitectureComplianceRecommendationContent) result.content();
    assertEquals(COMPONENT, content.violatingComponentId());
    assertTrue(content.violationDescription().contains("csm:element:componentB"));
    assertTrue(content.violationDescription().contains("csm:element:constraint1"));
  }

  @Test
  void recommendationIncludesRemediationGuidance() {
    Finding finding = boundaryViolationFinding();
    AgentInvocationResult result = new ArchitectureComplianceAgent().invoke(finding);
    ArchitectureComplianceRecommendationContent content = (ArchitectureComplianceRecommendationContent) result.content();
    assertFalse(content.remediationGuidance().isBlank());
  }

  @Test
  void recommendationContentContainsNoCodeOrDiffContent() {
    // ArchitectureComplianceRecommendationContent's own field list has
    // no code/diff-shaped field at all - structurally guaranteed.
    Finding finding = boundaryViolationFinding();
    AgentInvocationResult result = new ArchitectureComplianceAgent().invoke(finding);
    ArchitectureComplianceRecommendationContent content = (ArchitectureComplianceRecommendationContent) result.content();
    assertFalse(content.remediationGuidance().contains("```"));
    assertFalse(content.remediationGuidance().contains("diff --git"));
  }

  @Test
  void agentReadsNoContentBeyondTheGivenFinding() {
    // ArchitectureComplianceAgent.invoke's own signature has no
    // FindingSource, AnalysisView, or any other content-bearing
    // parameter at all - its only input is the Finding itself.
    Finding finding = boundaryViolationFinding();
    Recommendation recommendation = RecommendationConstructor.construct(new ArchitectureComplianceAgent(), finding).orElseThrow();
    assertEquals(finding.id(), recommendation.findingEvaluationIdentity());
  }
}
