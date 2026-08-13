package aip.core.csm;

/**
 * A structural contract a {@link RuleEvaluationResult}'s opaque {@link
 * RuleEvaluationResult#payload()} MAY implement to declare the static
 * Category/Severity/Description/Impact configuration Finding
 * construction copies onto a {@link Finding} unmodified, per {@code
 * Severity and Category Are Rule-Type-Declared Configuration}.
 *
 * <p>Resolves a gap {@code define-finding-model/design.md} Decision 5
 * left at the conceptual level ("taken directly from static
 * configuration declared by the producing Rule Type") without naming a
 * concrete mechanism — see {@code implement-finding-model/design.md}
 * Decision 3. A Rule Type whose {@code FAIL} outcomes are meant to
 * become Findings populates its evaluation payload with a value
 * implementing this interface; a Rule Type that never intends its
 * outcomes to become Findings simply does not, and Finding
 * construction treats that {@link RuleEvaluationResult} as not
 * qualifying (see {@code implement-finding-model/design.md} Decision
 * 4) rather than as an error.
 *
 * <p>{@code aip-core} does not require {@link RuleEvaluationResult}'s
 * payload to implement this interface — {@code payload()}'s declared
 * type remains a plain, unconstrained {@code Object}. Finding
 * construction reads Category/Severity/Description/Impact through
 * this interface only, never by branching on a payload's concrete
 * Rule-Type-specific class, so registering a new Rule Type requires no
 * Finding Model change (per {@code Extension Behavior — New Rule Types
 * Require No Finding Model Changes}).
 */
public interface FindingMetadata {

  /** The static classification a Finding constructed from this outcome carries, unmodified. */
  String category();

  /** The static severity a Finding constructed from this outcome carries, unmodified. */
  String severity();

  /** Static, Rule-configuration-parameterizable description text. */
  String description();

  /** Static, Rule-configuration-parameterizable impact text. */
  String impact();
}
