package aip.core.csm;

import java.util.Objects;
import java.util.Set;

/**
 * A {@link RuleEvaluationResult}'s deterministic identity, per {@code
 * Deterministic Rule Evaluation Result Identity}: "a deterministic
 * function of: the producing Rule's identifier, the Rule's version,
 * the source CSM Snapshot's identity, the Rule Scope instance covered,
 * and the complete set of consumed Analysis Result identities... Two
 * evaluations that consumed different Analysis Result identities SHALL
 * produce different Rule Evaluation Result identities, even when the
 * Rule, its version, the source CSM Snapshot identity, and the Rule
 * Scope instance are otherwise identical."
 *
 * <p>Extends {@link AnalysisResultId}'s own established shape with one
 * more term — the complete consumed {@link AnalysisResultId} set — the
 * first identity scheme in this project whose input is multi-artifact
 * rather than derived from a single upstream source, per {@code
 * define-rule-framework/design.md} Decision 4.
 *
 * @param value the opaque, deterministic digest string. Never blank.
 */
public record RuleEvaluationResultId(String value) {

  public RuleEvaluationResultId {
    Objects.requireNonNull(value, "value");
    if (value.isBlank()) {
      throw new IllegalArgumentException("RuleEvaluationResultId value must not be blank");
    }
  }

  /**
   * Computes a {@code RuleEvaluationResultId} as a deterministic
   * function of every component this identity depends on. Identical
   * arguments always produce an equal {@code RuleEvaluationResultId};
   * a differing {@code consumedAnalysisResultIds} set, or a differing
   * {@code ruleVersion}, each with everything else unchanged, always
   * produces a distinguishable one.
   */
  public static RuleEvaluationResultId of(
      String ruleIdentifier,
      int ruleVersion,
      CsmSnapshotId sourceSnapshotId,
      CsmScopeInstance scopeInstance,
      Set<AnalysisResultId> consumedAnalysisResultIds) {
    Objects.requireNonNull(ruleIdentifier, "ruleIdentifier");
    Objects.requireNonNull(sourceSnapshotId, "sourceSnapshotId");
    Objects.requireNonNull(scopeInstance, "scopeInstance");
    Objects.requireNonNull(consumedAnalysisResultIds, "consumedAnalysisResultIds");
    // Sorted by string form so set iteration order never affects the
    // resulting digest — DeterministicHash hashes an ordered sequence,
    // and Set's own iteration order is not itself guaranteed stable
    // across equal sets of different concrete implementations.
    String consumedDigestComponent =
        consumedAnalysisResultIds.stream()
            .map(AnalysisResultId::value)
            .sorted()
            .reduce((a, b) -> a + "," + b)
            .orElse("");
    return new RuleEvaluationResultId(
        DeterministicHash.of(ruleIdentifier, ruleVersion, sourceSnapshotId, scopeInstance, consumedDigestComponent));
  }

  @Override
  public String toString() {
    return value;
  }
}
