package aip.ai;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Registration and identifier-based lookup of {@link Agent}s, per
 * {@code Agent Registration and Extension Behavior} — mirroring {@code
 * aip-rules}'s own {@code RuleTypeRegistry}: at most one Agent per
 * identifier, {@link #registered()} ordered by identifier for
 * deterministic dispatch.
 */
public final class AgentRegistry {

  private final TreeMap<String, Agent> byIdentifier = new TreeMap<>();

  public void register(Agent agent) {
    Objects.requireNonNull(agent, "agent");
    String identifier = agent.identifier();
    Objects.requireNonNull(identifier, "agent.identifier()");
    if (identifier.isBlank()) {
      throw new IllegalArgumentException("Agent identifier must not be blank");
    }
    if (byIdentifier.containsKey(identifier)) {
      throw new IllegalStateException(
          "an Agent is already registered for identifier " + identifier + "; registering a second Agent"
              + " for the same identifier is not permitted");
    }
    byIdentifier.put(identifier, agent);
  }

  public Optional<Agent> lookup(String identifier) {
    Objects.requireNonNull(identifier, "identifier");
    return Optional.ofNullable(byIdentifier.get(identifier));
  }

  /** Every registered Agent, ordered by identifier. */
  public Collection<Agent> registered() {
    return byIdentifier.values();
  }
}
