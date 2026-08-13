package aip.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.ai.test.fixtures.StubAgent;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@link AgentRegistry} tests, per {@code Agent Registration and
 * Extension Behavior}.
 */
class AgentRegistryTest {

  @Test
  void registersAndLooksUpByIdentifier() {
    AgentRegistry registry = new AgentRegistry();
    StubAgent agent = StubAgent.succeeding("agent.a", 1);
    registry.register(agent);
    assertEquals(agent, registry.lookup("agent.a").orElseThrow());
  }

  @Test
  void unregisteredIdentifierIsEmpty() {
    AgentRegistry registry = new AgentRegistry();
    assertTrue(registry.lookup("agent.unknown").isEmpty());
  }

  @Test
  void rejectsRegisteringTwoAgentsForTheSameIdentifier() {
    AgentRegistry registry = new AgentRegistry();
    registry.register(StubAgent.succeeding("agent.a", 1));
    assertThrows(IllegalStateException.class, () -> registry.register(StubAgent.succeeding("agent.a", 2)));
  }

  @Test
  void registeredIsOrderedByIdentifier() {
    AgentRegistry registry = new AgentRegistry();
    registry.register(StubAgent.succeeding("agent.b", 1));
    registry.register(StubAgent.succeeding("agent.a", 1));
    List<String> identifiers = registry.registered().stream().map(Agent::identifier).toList();
    assertEquals(List.of("agent.a", "agent.b"), identifiers);
  }
}
