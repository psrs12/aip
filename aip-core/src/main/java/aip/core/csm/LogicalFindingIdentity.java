package aip.core.csm;

import java.util.Objects;

/**
 * A {@link Finding}'s snapshot-independent identity, per {@code
 * Logical Finding Identity}: "a deterministic function of the
 * producing Rule's identifier (excluding its version) and the CSM
 * element identity the underlying Rule Scope instance's containment-
 * level anchor concerns. Logical Finding Identity SHALL NOT include
 * source CSM Snapshot identity, RuleEvaluationResult identity, Rule
 * version, or any consumed Analysis Result identity."
 *
 * <p>Two Findings sharing a {@code LogicalFindingIdentity} but
 * differing {@link EvaluationIdentity} represent the same logical
 * issue observed across two different evaluation runs — this is what
 * makes cross-CSM-Snapshot tracking possible without a mutable
 * lifecycle state (see {@code define-finding-model/design.md}
 * Decisions 3, 4).
 *
 * @param value the opaque, deterministic digest string. Never blank.
 */
public record LogicalFindingIdentity(String value) {

  public LogicalFindingIdentity {
    Objects.requireNonNull(value, "value");
    if (value.isBlank()) {
      throw new IllegalArgumentException("LogicalFindingIdentity value must not be blank");
    }
  }

  /**
   * Computes a {@code LogicalFindingIdentity} as a deterministic
   * function of exactly {@code ruleIdentifier} and {@code
   * concernedElementId} — deliberately excluding every other
   * traceability component a {@link RuleEvaluationResult} carries.
   */
  public static LogicalFindingIdentity of(String ruleIdentifier, CsmElementId concernedElementId) {
    Objects.requireNonNull(ruleIdentifier, "ruleIdentifier");
    Objects.requireNonNull(concernedElementId, "concernedElementId");
    return new LogicalFindingIdentity(DeterministicHash.of(ruleIdentifier, concernedElementId));
  }

  @Override
  public String toString() {
    return value;
  }
}
