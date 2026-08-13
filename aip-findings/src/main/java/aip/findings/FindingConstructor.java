package aip.findings;

import aip.core.csm.Confidence;
import aip.core.csm.Finding;
import aip.core.csm.FindingMetadata;
import aip.core.csm.RuleEvaluationOutcome;
import aip.core.csm.RuleEvaluationResult;
import java.util.Optional;
import java.util.Set;

/**
 * Deterministic Finding construction from one {@code
 * RuleEvaluationResult}, per {@code Deterministic, Declarative Finding
 * Construction Only} and {@code implement-finding-model/design.md}
 * Decisions 4, 6, 8.
 *
 * <p>A pure function: no AI/LLM call, no heuristic or probabilistic
 * step, no I/O, no randomness. Every field it produces is copied
 * unmodified from its input or computed by one of the deterministic
 * identity functions in {@code aip-core}.
 */
public final class FindingConstructor {

  private FindingConstructor() {}

  /**
   * {@code true} iff {@code result} qualifies for Finding
   * construction: its outcome is {@code FAIL} and its payload
   * implements {@link FindingMetadata} (`design.md` Decision 4). Every
   * other combination — {@code PASS}, {@code NOT_APPLICABLE}, or a
   * {@code FAIL} outcome whose payload does not implement {@code
   * FindingMetadata} — does not qualify.
   */
  public static boolean qualifies(RuleEvaluationResult result) {
    return result.outcome() == RuleEvaluationOutcome.FAIL && result.payload() instanceof FindingMetadata;
  }

  /**
   * Constructs exactly one {@code Finding} from {@code result} if it
   * qualifies (per {@link #qualifies(RuleEvaluationResult)}), or
   * {@link Optional#empty()} otherwise — silently, not as an error
   * (`design.md` Decision 4's own reasoning). The constructed
   * Finding's referenced-Result set contains exactly {@code result}'s
   * own identity, per v1's no-aggregation construction behavior
   * (`define-finding-model/design.md` Decisions 2, 7).
   */
  public static Optional<Finding> construct(RuleEvaluationResult result) {
    if (!qualifies(result)) {
      return Optional.empty();
    }
    FindingMetadata metadata = (FindingMetadata) result.payload();
    var concernedElementId = ConcernedElementResolver.resolve(result.scopeInstance(), result.sourceSnapshotId());
    Finding finding =
        Finding.of(
            result.ruleIdentifier(),
            result.sourceSnapshotId(),
            concernedElementId,
            Set.of(result.id()),
            metadata.category(),
            metadata.severity(),
            Confidence.HIGH,
            metadata.description(),
            metadata.impact());
    return Optional.of(finding);
  }
}
