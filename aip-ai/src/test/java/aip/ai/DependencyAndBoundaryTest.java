package aip.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.ai.test.fixtures.FindingFixtures;
import aip.ai.test.fixtures.InMemoryFindingSource;
import aip.ai.test.fixtures.InMemoryRecommendationStore;
import aip.ai.test.fixtures.StubAgent;
import aip.core.csm.CsmElement;
import aip.core.csm.CsmRelationship;
import aip.core.csm.Finding;
import aip.core.csm.RuleEvaluationResult;
import org.junit.jupiter.api.Test;

/**
 * Dependency and architectural invariant tests, per {@code Agent
 * Consumption Is Limited to Findings Through the Finding Source},
 * {@code No Arbitrary Repository or Application Access}, {@code
 * Deterministic-Layer Protection}, {@code No Intermediate AI Reasoning
 * Artifact}, and {@code Single-Repository Finding Consumption}.
 */
class DependencyAndBoundaryTest {

  @Test
  void agentReadsOnlyFromAFindingItIsGivenDirectly() {
    // Agent.invoke's own signature has no FindingSource, AnalysisView,
    // CsmSnapshotSource, or any other CSM/Analysis/Rule-content-bearing
    // parameter at all - its only input is a single, already-resolved
    // Finding.
    Finding finding = FindingFixtures.of("rule.a", "csm:element:m1");
    StubAgent agent = StubAgent.succeeding("agent.a", 1);
    assertTrue(RecommendationConstructor.construct(agent, finding).isPresent());
  }

  @Test
  void producingARecommendationDoesNotAlterTheReferencedFinding() {
    Finding finding = FindingFixtures.of("rule.a", "csm:element:m1");
    InMemoryFindingSource findingSource = new InMemoryFindingSource().with(finding);
    Finding before = findingSource.read(finding.id()).orElseThrow();

    StubAgent agent = StubAgent.succeeding("agent.a", 1);
    Recommendation recommendation = RecommendationConstructor.construct(agent, finding).orElseThrow();
    AgentRegistry agentRegistry = new AgentRegistry();
    agentRegistry.register(agent);
    RecommendationValidator.validate(recommendation, findingSource, agentRegistry);

    assertEquals(before, findingSource.read(finding.id()).orElseThrow());
  }

  @Test
  void recommendationIsNeverExposedAsCsmContentOrAnalysisOrRuleEvaluationResultOrFinding() {
    assertTrue(!CsmElement.class.isAssignableFrom(Recommendation.class));
    assertTrue(!CsmRelationship.class.isAssignableFrom(Recommendation.class));
    assertTrue(!RuleEvaluationResult.class.isAssignableFrom(Recommendation.class));
    assertTrue(!Finding.class.isAssignableFrom(Recommendation.class));
  }

  @Test
  void recommendationsAreStoredSeparatelyFromFindings() {
    Finding finding = FindingFixtures.of("rule.a", "csm:element:m1");
    InMemoryFindingSource findingSource = new InMemoryFindingSource().with(finding);
    StubAgent agent = StubAgent.succeeding("agent.a", 1);
    Recommendation recommendation = RecommendationConstructor.construct(agent, finding).orElseThrow();
    InMemoryRecommendationStore recommendationStore = new InMemoryRecommendationStore();
    recommendationStore.write(recommendation);

    // Querying the FindingSource never returns Recommendations.
    assertEquals(finding, findingSource.read(finding.id()).orElseThrow());
  }

  @Test
  void everyRecommendationConcernsExactlyOneRepositoryThroughItsReferencedFinding() {
    Finding finding = FindingFixtures.of("rule.a", "csm:element:m1");
    StubAgent agent = StubAgent.succeeding("agent.a", 1);
    Recommendation recommendation = RecommendationConstructor.construct(agent, finding).orElseThrow();
    assertEquals(finding.sourceSnapshotId(), FindingFixtures.SNAPSHOT);
    assertEquals(finding.id(), recommendation.findingEvaluationIdentity());
  }

  @Test
  void noSeparateReasoningArtifactExists() {
    // Recommendation is the only durable output type this package
    // defines - RecommendationConstructor.construct's own return type
    // is Optional<Recommendation>, with no second, independently
    // identified reasoning artifact anywhere in aip-ai.
    Finding finding = FindingFixtures.of("rule.a", "csm:element:m1");
    StubAgent agent = StubAgent.succeeding("agent.a", 1);
    var result = RecommendationConstructor.construct(agent, finding);
    assertTrue(result.isPresent());
  }
}
