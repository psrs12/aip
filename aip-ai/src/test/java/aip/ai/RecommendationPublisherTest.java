package aip.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.ai.test.fixtures.FindingFixtures;
import aip.ai.test.fixtures.InMemoryFindingSource;
import aip.ai.test.fixtures.InMemoryRecommendationStore;
import aip.ai.test.fixtures.StubAgent;
import aip.core.csm.Finding;
import org.junit.jupiter.api.Test;

/**
 * {@link RecommendationPublisher} tests, per {@code Recommendation
 * Publishing}.
 */
class RecommendationPublisherTest {

  @Test
  void validRecommendationIsPublishedAndRetrievable() {
    Finding finding = FindingFixtures.of("rule.a", "csm:element:m1");
    InMemoryFindingSource findingSource = new InMemoryFindingSource().with(finding);
    AgentRegistry agentRegistry = new AgentRegistry();
    StubAgent agent = StubAgent.succeeding("agent.a", 1);
    agentRegistry.register(agent);
    InMemoryRecommendationStore store = new InMemoryRecommendationStore();

    Recommendation recommendation = RecommendationConstructor.construct(agent, finding).orElseThrow();
    RecommendationPublisher.PublicationOutcome outcome =
        RecommendationPublisher.publish(store, recommendation, findingSource, agentRegistry);

    assertTrue(outcome.published());
    assertEquals(recommendation.id(), store.read(recommendation.id()).orElseThrow().id());
  }

  @Test
  void invalidRecommendationIsNeverPublished() {
    Finding finding = FindingFixtures.of("rule.a", "csm:element:m1");
    InMemoryFindingSource emptyFindingSource = new InMemoryFindingSource();
    AgentRegistry agentRegistry = new AgentRegistry();
    StubAgent agent = StubAgent.succeeding("agent.a", 1);
    agentRegistry.register(agent);
    InMemoryRecommendationStore store = new InMemoryRecommendationStore();

    Recommendation recommendation = RecommendationConstructor.construct(agent, finding).orElseThrow();
    RecommendationPublisher.PublicationOutcome outcome =
        RecommendationPublisher.publish(store, recommendation, emptyFindingSource, agentRegistry);

    assertFalse(outcome.published());
    assertTrue(store.read(recommendation.id()).isEmpty());
  }
}
