package aip.findings;

import aip.core.csm.EvaluationIdentity;
import aip.core.csm.Finding;
import aip.core.csm.LogicalFindingIdentity;
import aip.core.csm.RuleEvaluationResult;
import aip.core.csm.RuleEvaluationResultId;
import aip.core.csm.RuleEvaluationResultSource;
import aip.core.csm.ValidationResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Validates a constructed {@link Finding} before it is considered
 * usable output, per {@code Finding Validation Before Publication}:
 * every referenced Rule Evaluation Result identity SHALL exist and be
 * published; the Finding's Evaluation Identity and Logical Finding
 * Identity SHALL each be independently recomputable from, and
 * consistent with, its referenced Rule Evaluation Result set; and the
 * reference set SHALL be non-empty.
 *
 * <p>Trusts that a referenced {@link RuleEvaluationResultId} resolving
 * via {@link RuleEvaluationResultSource#read} was itself already
 * validated by {@code aip-rules}'s own {@code
 * RuleEvaluationResultValidator} — this validator confirms the
 * identity is <em>resolvable</em> and consistent with the Finding's
 * own declared fields; it does not re-check that Rule Evaluation
 * Result's own consumed Analysis Results or CSM element/Subject
 * identities (`implement-finding-model/design.md` Decision 8's
 * trust-boundary reasoning).
 */
public final class FindingValidator {

  private FindingValidator() {}

  public static ValidationResult validate(Finding finding, RuleEvaluationResultSource ruleEvaluationResultSource) {
    Objects.requireNonNull(finding, "finding");
    Objects.requireNonNull(ruleEvaluationResultSource, "ruleEvaluationResultSource");

    List<String> violations = new ArrayList<>();

    if (finding.referencedRuleEvaluationResultIds().isEmpty()) {
      violations.add("Finding " + finding.id() + " references an empty Rule Evaluation Result set");
    }

    List<RuleEvaluationResult> resolved = new ArrayList<>();
    for (RuleEvaluationResultId referencedId : finding.referencedRuleEvaluationResultIds()) {
      Optional<RuleEvaluationResult> result = ruleEvaluationResultSource.read(referencedId);
      if (result.isEmpty()) {
        violations.add(
            "Finding " + finding.id() + " references Rule Evaluation Result " + referencedId
                + " which does not exist or has not been published");
      } else {
        resolved.add(result.get());
      }
    }

    for (RuleEvaluationResult result : resolved) {
      if (!result.ruleIdentifier().equals(finding.ruleIdentifier())) {
        violations.add(
            "Finding " + finding.id() + " declares producing Rule " + finding.ruleIdentifier()
                + " but referenced Rule Evaluation Result " + result.id() + " was produced by "
                + result.ruleIdentifier());
      }
      if (!result.sourceSnapshotId().equals(finding.sourceSnapshotId())) {
        violations.add(
            "Finding " + finding.id() + " declares source CSM Snapshot " + finding.sourceSnapshotId()
                + " but referenced Rule Evaluation Result " + result.id() + " was evaluated against "
                + result.sourceSnapshotId());
      }
      var expectedConcernedElementId = ConcernedElementResolver.resolve(result.scopeInstance(), result.sourceSnapshotId());
      if (!expectedConcernedElementId.equals(finding.concernedElementId())) {
        violations.add(
            "Finding " + finding.id() + " declares concerned CSM element " + finding.concernedElementId()
                + " but referenced Rule Evaluation Result " + result.id() + "'s Rule Scope instance concerns "
                + expectedConcernedElementId);
      }
    }

    EvaluationIdentity expectedEvaluationIdentity = EvaluationIdentity.of(finding.referencedRuleEvaluationResultIds());
    if (!expectedEvaluationIdentity.equals(finding.id())) {
      violations.add(
          "Finding " + finding.id() + " declares an Evaluation Identity inconsistent with its own referenced"
              + " Rule Evaluation Result set (recomputed: " + expectedEvaluationIdentity + ")");
    }

    LogicalFindingIdentity expectedLogicalFindingIdentity =
        LogicalFindingIdentity.of(finding.ruleIdentifier(), finding.concernedElementId());
    if (!expectedLogicalFindingIdentity.equals(finding.logicalFindingIdentity())) {
      violations.add(
          "Finding " + finding.id() + " declares a Logical Finding Identity inconsistent with its own"
              + " Rule identifier and concerned element (recomputed: " + expectedLogicalFindingIdentity + ")");
    }

    return violations.isEmpty() ? ValidationResult.success() : ValidationResult.failure(violations);
  }
}
