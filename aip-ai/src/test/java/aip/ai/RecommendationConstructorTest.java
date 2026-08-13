package aip.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.ai.test.fixtures.FindingFixtures;
import aip.ai.test.fixtures.StubAgent;
import aip.core.csm.Finding;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * {@link RecommendationConstructor} tests, per {@code Agent Contract},
 * {@code Recommendation Multiplicity — One Invocation, One
 * Recommendation}, {@code Failure Semantics}, and {@code Deterministic
 * Recommendation Lookup, Not Deterministic Generation}.
 */
class RecommendationConstructorTest {

  @Test
  void oneInvocationProducesExactlyOneRecommendation() {
    Finding finding = FindingFixtures.of("rule.a", "csm:element:m1");
    StubAgent agent = StubAgent.succeeding("agent.a", 1);
    Optional<Recommendation> recommendation = RecommendationConstructor.construct(agent, finding);
    assertTrue(recommendation.isPresent());
  }

  @Test
  void failingAgentInvocationProducesNoRecommendation() {
    Finding finding = FindingFixtures.of("rule.a", "csm:element:m1");
    StubAgent agent = StubAgent.failing("agent.failing", 1);
    assertEquals(Optional.empty(), RecommendationConstructor.construct(agent, finding));
  }

  @Test
  void repeatedInvocationsAgainstTheSameFindingProduceIndependentlyIdentifiedRecommendations() {
    Finding finding = FindingFixtures.of("rule.a", "csm:element:m1");
    StubAgent agent = StubAgent.succeeding("agent.a", 1);
    Recommendation first = RecommendationConstructor.construct(agent, finding).orElseThrow();
    Recommendation second = RecommendationConstructor.construct(agent, finding).orElseThrow();

    // Same Agent, same version, same Finding - yet two independently
    // identified Recommendations, since each invocation mints its own
    // Generation Identifier.
    assertNotEquals(first.id(), second.id());
    assertNotEquals(first.generationIdentifier(), second.generationIdentifier());
  }

  @Test
  void constructedRecommendationTracesToTheInvokingAgentAndFinding() {
    Finding finding = FindingFixtures.of("rule.a", "csm:element:m1");
    StubAgent agent = StubAgent.succeeding("agent.a", 3);
    Recommendation recommendation = RecommendationConstructor.construct(agent, finding).orElseThrow();

    assertEquals("agent.a", recommendation.agentIdentifier());
    assertEquals(3, recommendation.agentVersion());
    assertEquals(finding.id(), recommendation.findingEvaluationIdentity());
    assertEquals(finding.logicalFindingIdentity(), recommendation.findingLogicalFindingIdentity());
  }

  @Test
  void confidenceIsPreservedFromTheAgentsOwnDeclaration() {
    Finding finding = FindingFixtures.of("rule.a", "csm:element:m1");
    StubAgent agent = StubAgent.succeedingWithConfidence("agent.a", 1, 0.33);
    Recommendation recommendation = RecommendationConstructor.construct(agent, finding).orElseThrow();
    assertEquals(0.33, recommendation.confidence());
  }

  @Test
  void aFindingMayBeReferencedByMultipleRecommendationsFromDifferentAgents() {
    Finding finding = FindingFixtures.of("rule.a", "csm:element:m1");
    Recommendation fromAgentA = RecommendationConstructor.construct(StubAgent.succeeding("agent.a", 1), finding).orElseThrow();
    Recommendation fromAgentB = RecommendationConstructor.construct(StubAgent.succeeding("agent.b", 1), finding).orElseThrow();

    assertEquals(finding.id(), fromAgentA.findingEvaluationIdentity());
    assertEquals(finding.id(), fromAgentB.findingEvaluationIdentity());
    assertFalse(fromAgentA.id().equals(fromAgentB.id()));
  }
}
