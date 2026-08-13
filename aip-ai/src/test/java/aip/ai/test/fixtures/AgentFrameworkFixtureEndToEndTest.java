package aip.ai.test.fixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.ai.Agent;
import aip.ai.AgentRegistry;
import aip.ai.Recommendation;
import aip.ai.RecommendationConstructor;
import aip.ai.RecommendationPublisher;
import aip.ai.RecommendationValidator;
import aip.core.csm.Finding;
import aip.core.csm.ValidationResult;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * A representative end-to-end scenario: invoke a fixture Agent against
 * a fixture Finding, construct/validate/publish the resulting
 * Recommendation, retrieve it by Artifact Identity, confirm a second
 * invocation against the same Finding produces a second,
 * independently-identified Recommendation, and confirm a failing
 * fixture Agent invocation produces no Recommendation in the same run.
 * Mirrors {@code aip-findings}'s own {@code
 * FindingModelFixtureEndToEndTest}.
 */
class AgentFrameworkFixtureEndToEndTest {

  @Test
  void invokesConstructsValidatesAndPublishesRecommendationsAcrossMultipleInvocations() {
    Finding finding = FindingFixtures.of("rule.boundary", "csm:element:moduleA");

    InMemoryFindingSource findingSource = new InMemoryFindingSource().with(finding);
    AgentRegistry agentRegistry = new AgentRegistry();
    Agent succeedingAgent = StubAgent.succeeding("agent.succeeding", 1);
    Agent failingAgent = StubAgent.failing("agent.failing", 1);
    agentRegistry.register(succeedingAgent);
    agentRegistry.register(failingAgent);
    InMemoryRecommendationStore store = new InMemoryRecommendationStore();

    // First invocation: succeeds.
    Recommendation firstRecommendation = RecommendationConstructor.construct(succeedingAgent, finding).orElseThrow();
    ValidationResult firstValidation = RecommendationValidator.validate(firstRecommendation, findingSource, agentRegistry);
    assertTrue(firstValidation.valid(), () -> "expected valid: " + firstValidation.violations());
    RecommendationPublisher.PublicationOutcome firstOutcome =
        RecommendationPublisher.publish(store, firstRecommendation, findingSource, agentRegistry);
    assertTrue(firstOutcome.published());

    // Second invocation, same Agent, same Finding: independently
    // identified, both retained.
    Recommendation secondRecommendation = RecommendationConstructor.construct(succeedingAgent, finding).orElseThrow();
    RecommendationPublisher.PublicationOutcome secondOutcome =
        RecommendationPublisher.publish(store, secondRecommendation, findingSource, agentRegistry);
    assertTrue(secondOutcome.published());
    assertNotEquals(firstRecommendation.id(), secondRecommendation.id());

    // Third invocation: a failing Agent produces no Recommendation at all.
    Optional<Recommendation> thirdRecommendation = RecommendationConstructor.construct(failingAgent, finding);
    assertEquals(Optional.empty(), thirdRecommendation);

    // Both successful Recommendations remain independently retrievable.
    assertEquals(firstRecommendation.id(), store.read(firstRecommendation.id()).orElseThrow().id());
    assertEquals(secondRecommendation.id(), store.read(secondRecommendation.id()).orElseThrow().id());
  }
}
