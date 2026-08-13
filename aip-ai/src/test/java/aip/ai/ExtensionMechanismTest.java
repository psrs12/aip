package aip.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.ai.test.fixtures.FindingFixtures;
import aip.ai.test.fixtures.InMemoryFindingSource;
import aip.ai.test.fixtures.StubAgent;
import aip.core.csm.Finding;
import aip.core.csm.ValidationResult;
import org.junit.jupiter.api.Test;

/**
 * Confirms registering a new, structurally distinct Agent requires no
 * change to Recommendation construction, identity, traceability, or
 * validation mechanisms — mirroring {@code aip-findings}'s own
 * Extension Mechanism Verification, per {@code Agent Registration and
 * Extension Behavior}.
 */
class ExtensionMechanismTest {

  @Test
  void aStructurallyDistinctSecondAgentUsesTheSameCoreMechanismsUnmodified() {
    Finding finding = FindingFixtures.of("rule.a", "csm:element:m1");
    InMemoryFindingSource findingSource = new InMemoryFindingSource().with(finding);

    AgentRegistry agentRegistry = new AgentRegistry();
    // First: a low-confidence, plain-text-content Agent.
    StubAgent first = StubAgent.succeedingWithConfidence("agent.first", 1, 0.4);
    // Second: structurally distinct - different Confidence, different
    // version.
    StubAgent second = StubAgent.succeedingWithConfidence("agent.second", 7, 0.95);
    agentRegistry.register(first);
    agentRegistry.register(second);

    Recommendation fromFirst = RecommendationConstructor.construct(first, finding).orElseThrow();
    Recommendation fromSecond = RecommendationConstructor.construct(second, finding).orElseThrow();

    assertEquals(7, fromSecond.agentVersion());
    assertTrue(
        RecommendationValidator.validate(fromFirst, findingSource, agentRegistry).valid());
    ValidationResult secondValidation = RecommendationValidator.validate(fromSecond, findingSource, agentRegistry);
    assertTrue(secondValidation.valid(), () -> "expected valid: " + secondValidation.violations());
  }
}
