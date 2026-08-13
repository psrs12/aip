package aip.rules;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Registration and identifier-based lookup of {@link Rule}s, per
 * {@code Rule configures a Rule Type without code} and {@code
 * Extension Mechanism for New Rule Types}. Rejects a {@link Rule}
 * whose {@link Rule#ruleTypeIdentifier()} does not correspond to a
 * currently registered {@link RuleType}, per {@code Rule Evaluation
 * Result Validation Before Publication}'s own analogous rejection —
 * applied here at registration time, the earliest point the mistake
 * can be caught.
 */
public final class RuleRegistry {

  private final RuleTypeRegistry ruleTypeRegistry;
  private final TreeMap<String, Rule> byIdentifier = new TreeMap<>();

  public RuleRegistry(RuleTypeRegistry ruleTypeRegistry) {
    this.ruleTypeRegistry = Objects.requireNonNull(ruleTypeRegistry, "ruleTypeRegistry");
  }

  public void register(Rule rule) {
    Objects.requireNonNull(rule, "rule");
    if (ruleTypeRegistry.lookup(rule.ruleTypeIdentifier()).isEmpty()) {
      throw new IllegalArgumentException(
          "Rule " + rule.identifier() + " references Rule Type " + rule.ruleTypeIdentifier()
              + " which is not currently registered");
    }
    if (byIdentifier.containsKey(rule.identifier())) {
      throw new IllegalStateException(
          "a Rule is already registered for identifier " + rule.identifier() + "; registering a second"
              + " Rule for the same identifier is not permitted");
    }
    byIdentifier.put(rule.identifier(), rule);
  }

  public Optional<Rule> lookup(String identifier) {
    Objects.requireNonNull(identifier, "identifier");
    return Optional.ofNullable(byIdentifier.get(identifier));
  }

  /** Every registered Rule, ordered by identifier. */
  public Collection<Rule> registered() {
    return byIdentifier.values();
  }
}
