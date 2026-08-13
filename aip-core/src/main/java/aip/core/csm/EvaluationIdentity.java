package aip.core.csm;

import java.util.Objects;
import java.util.Set;

/**
 * A {@link Finding}'s snapshot-bound identity — a Finding's own
 * "Finding ID" — per {@code Evaluation Identity}: "a deterministic
 * function of the complete set of its referenced RuleEvaluationResult
 * identities... Evaluation Identity is snapshot-bound: because each
 * referenced RuleEvaluationResult identity already encodes a source
 * CSM Snapshot identity, Evaluation Identity changes whenever the
 * underlying evaluation run changes, even if the concerned CSM element
 * is unchanged."
 *
 * <p>Distinct from {@link LogicalFindingIdentity} — the two SHALL NOT
 * be conflated (per {@code Evaluation Identity and Logical Finding
 * Identity Are Distinct}); kept as separate record types for the same
 * reason {@link AnalysisResultId} and {@link RuleEvaluationResultId}
 * are each their own type rather than a shared, tagged wrapper (see
 * {@code implement-finding-model/design.md} Decision 2).
 *
 * @param value the opaque, deterministic digest string. Never blank.
 */
public record EvaluationIdentity(String value) {

  public EvaluationIdentity {
    Objects.requireNonNull(value, "value");
    if (value.isBlank()) {
      throw new IllegalArgumentException("EvaluationIdentity value must not be blank");
    }
  }

  /**
   * Computes an {@code EvaluationIdentity} as a deterministic function
   * of the complete set of referenced {@link RuleEvaluationResultId}
   * values. Identical sets always produce an equal {@code
   * EvaluationIdentity}; a differing set always produces a
   * distinguishable one, independent of {@code Set} iteration order.
   */
  public static EvaluationIdentity of(Set<RuleEvaluationResultId> referencedRuleEvaluationResultIds) {
    Objects.requireNonNull(referencedRuleEvaluationResultIds, "referencedRuleEvaluationResultIds");
    // Sorted by string form so Set iteration order never affects the
    // resulting digest, mirroring RuleEvaluationResultId.of's own
    // treatment of its own consumed-identity-set component.
    String referencedDigestComponent =
        referencedRuleEvaluationResultIds.stream()
            .map(RuleEvaluationResultId::value)
            .sorted()
            .reduce((a, b) -> a + "," + b)
            .orElse("");
    return new EvaluationIdentity(DeterministicHash.of(referencedDigestComponent));
  }

  @Override
  public String toString() {
    return value;
  }
}
