package aip.rules;

import java.util.Map;
import java.util.Objects;

/**
 * A declarative, version-controlled configuration binding exactly one
 * registered {@link RuleType} to specific CSM vocabulary, per {@code
 * Rule Declaration}: "A Rule SHALL be a declarative, version-
 * controlled configuration binding exactly one Rule Type to specific
 * CSM vocabulary — entity kinds, relationship types, element
 * identities, and/or native-attribute predicate values — without
 * itself containing executable logic. A Rule SHALL NOT be defined as,
 * or require, source code."
 *
 * <p>Carries its own identifier and version, distinct from its
 * referenced Rule Type's own identifier/version — both are folded into
 * {@link aip.core.csm.RuleEvaluationResultId} per {@code Deterministic
 * Rule Evaluation Result Identity}'s "the producing Rule's identifier,
 * the Rule's version." {@link #configuration()} is a plain,
 * unconstrained value bag (entity kinds, relationship types, element
 * identities, native-attribute predicate values, or whatever else a
 * concrete Rule Type's own condition needs) — never executable code,
 * only data a {@link RuleType#evaluate} implementation reads.
 */
public record Rule(String identifier, int version, String ruleTypeIdentifier, Map<String, Object> configuration) {

  public Rule {
    Objects.requireNonNull(identifier, "identifier");
    if (identifier.isBlank()) {
      throw new IllegalArgumentException("Rule identifier must not be blank");
    }
    Objects.requireNonNull(ruleTypeIdentifier, "ruleTypeIdentifier");
    if (ruleTypeIdentifier.isBlank()) {
      throw new IllegalArgumentException("ruleTypeIdentifier must not be blank");
    }
    Objects.requireNonNull(configuration, "configuration");
    configuration = Map.copyOf(configuration);
  }
}
