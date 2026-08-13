package aip.ai;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.ai.test.fixtures.FindingFixtures;
import aip.ai.test.fixtures.InMemoryFindingSource;
import aip.ai.test.fixtures.StubAgent;
import aip.core.csm.Finding;
import aip.core.csm.ValidationResult;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * {@link RecommendationValidator} tests, per {@code Recommendation
 * Validation Before Publication} and {@code Validation Does Not
 * Determine Substantive Correctness}.
 */
class RecommendationValidatorTest {

  private static final GenerationProvenance VALID_PROVENANCE =
      new GenerationProvenance("provider", "v1", Map.of(), Instant.parse("2026-01-01T00:00:00Z"));

  @Test
  void validRecommendationPassesValidation() {
    Finding finding = FindingFixtures.of("rule.a", "csm:element:m1");
    InMemoryFindingSource findingSource = new InMemoryFindingSource().with(finding);
    AgentRegistry agentRegistry = new AgentRegistry();
    StubAgent agent = StubAgent.succeeding("agent.a", 1);
    agentRegistry.register(agent);

    Recommendation recommendation = RecommendationConstructor.construct(agent, finding).orElseThrow();

    ValidationResult validation = RecommendationValidator.validate(recommendation, findingSource, agentRegistry);
    assertTrue(validation.valid());
  }

  @Test
  void recommendationReferencingAnUnpublishedFindingIsRejected() {
    Finding finding = FindingFixtures.of("rule.a", "csm:element:m1");
    InMemoryFindingSource emptyFindingSource = new InMemoryFindingSource(); // finding not published here
    AgentRegistry agentRegistry = new AgentRegistry();
    StubAgent agent = StubAgent.succeeding("agent.a", 1);
    agentRegistry.register(agent);

    Recommendation recommendation = RecommendationConstructor.construct(agent, finding).orElseThrow();

    ValidationResult validation = RecommendationValidator.validate(recommendation, emptyFindingSource, agentRegistry);
    assertFalse(validation.valid());
  }

  @Test
  void recommendationFromAnUnregisteredAgentIsRejected() {
    Finding finding = FindingFixtures.of("rule.a", "csm:element:m1");
    InMemoryFindingSource findingSource = new InMemoryFindingSource().with(finding);
    AgentRegistry emptyRegistry = new AgentRegistry(); // agent not registered

    Recommendation recommendation =
        Recommendation.of(
            "agent.unknown",
            1,
            finding.id(),
            finding.logicalFindingIdentity(),
            GenerationIdentifier.generate(),
            "content",
            0.5,
            VALID_PROVENANCE);

    ValidationResult validation = RecommendationValidator.validate(recommendation, findingSource, emptyRegistry);
    assertFalse(validation.valid());
  }

  @Test
  void recommendationFromAnUnregisteredAgentVersionIsRejected() {
    Finding finding = FindingFixtures.of("rule.a", "csm:element:m1");
    InMemoryFindingSource findingSource = new InMemoryFindingSource().with(finding);
    AgentRegistry agentRegistry = new AgentRegistry();
    agentRegistry.register(StubAgent.succeeding("agent.a", 2)); // registered at version 2

    Recommendation recommendation =
        Recommendation.of(
            "agent.a",
            1, // declares version 1
            finding.id(),
            finding.logicalFindingIdentity(),
            GenerationIdentifier.generate(),
            "content",
            0.5,
            VALID_PROVENANCE);

    ValidationResult validation = RecommendationValidator.validate(recommendation, findingSource, agentRegistry);
    assertFalse(validation.valid());
  }

  @Test
  void recommendationWithIncompleteGenerationProvenanceIsRejected() {
    Finding finding = FindingFixtures.of("rule.a", "csm:element:m1");
    InMemoryFindingSource findingSource = new InMemoryFindingSource().with(finding);
    AgentRegistry agentRegistry = new AgentRegistry();
    agentRegistry.register(StubAgent.succeeding("agent.a", 1));

    Recommendation recommendation =
        Recommendation.of(
            "agent.a",
            1,
            finding.id(),
            finding.logicalFindingIdentity(),
            GenerationIdentifier.generate(),
            "content",
            0.5,
            new GenerationProvenance(" ", "v1", Map.of(), Instant.parse("2026-01-01T00:00:00Z")));

    ValidationResult validation = RecommendationValidator.validate(recommendation, findingSource, agentRegistry);
    assertFalse(validation.valid());
  }

  @Test
  void wellFormedButSubstantivelyQuestionableRecommendationStillPassesValidation() {
    Finding finding = FindingFixtures.of("rule.a", "csm:element:m1");
    InMemoryFindingSource findingSource = new InMemoryFindingSource().with(finding);
    AgentRegistry agentRegistry = new AgentRegistry();
    StubAgent agent = StubAgent.succeedingWithConfidence("agent.a", 1, 0.01); // deliberately low Confidence

    agentRegistry.register(agent);
    Recommendation recommendation = RecommendationConstructor.construct(agent, finding).orElseThrow();

    ValidationResult validation = RecommendationValidator.validate(recommendation, findingSource, agentRegistry);
    assertTrue(validation.valid(), "validation SHALL NOT reject a low-Confidence Recommendation on that basis alone");
  }
}
