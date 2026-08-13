package aip.rules;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Registration and identifier-based lookup of {@link RuleType}s, per
 * {@code Rule Type declares identifier, version, and inputs} —
 * mirroring {@code aip-analysis}'s own {@code AnalyzerRegistry}: at
 * most one Rule Type per identifier, {@link #registered()} ordered by
 * identifier for deterministic dispatch.
 */
public final class RuleTypeRegistry {

  private final TreeMap<String, RuleType> byIdentifier = new TreeMap<>();

  public void register(RuleType ruleType) {
    Objects.requireNonNull(ruleType, "ruleType");
    String identifier = ruleType.identifier();
    Objects.requireNonNull(identifier, "ruleType.identifier()");
    if (identifier.isBlank()) {
      throw new IllegalArgumentException("Rule Type identifier must not be blank");
    }
    if (byIdentifier.containsKey(identifier)) {
      throw new IllegalStateException(
          "a Rule Type is already registered for identifier " + identifier + "; registering a second"
              + " Rule Type for the same identifier is not permitted");
    }
    byIdentifier.put(identifier, ruleType);
  }

  public Optional<RuleType> lookup(String identifier) {
    Objects.requireNonNull(identifier, "identifier");
    return Optional.ofNullable(byIdentifier.get(identifier));
  }

  /** Every registered Rule Type, ordered by identifier. */
  public Collection<RuleType> registered() {
    return byIdentifier.values();
  }
}
