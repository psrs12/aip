package aip.rules;

import aip.core.csm.AnalysisResultId;
import aip.core.csm.AnalysisResultSource;
import aip.core.csm.AnalysisView;
import aip.core.csm.CsmElement;
import aip.core.csm.CsmElementId;
import aip.core.csm.CsmEntityKind;
import aip.core.csm.CsmScope;
import aip.core.csm.CsmScopeInstance;
import aip.core.csm.RuleEvaluationResult;
import aip.core.csm.ValidationResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Validates a constructed {@link RuleEvaluationResult} before it is
 * considered usable output, per {@code Rule Evaluation Result
 * Validation Before Publication}: referential integrity of every
 * consumed Analysis Result identity and every referenced CSM element/
 * Subject identity; Rule/version consistency against the currently
 * registered Rule; and Result content staying within its Rule Type's
 * declared Rule Scope.
 *
 * <p>Trusts that a consumed {@link AnalysisResultId} resolving via
 * {@link AnalysisResultSource#read} was itself already validated by
 * {@code aip-analysis}'s own {@code AnalysisResultValidator} — this
 * validator confirms the identity is <em>resolvable</em>, it does not
 * re-check that Analysis Result's own referential integrity against
 * CSM content, mirroring the same defense-in-depth-without-re-
 * verifying-the-whole-chain posture every prior validator in this
 * project has held to.
 */
public final class RuleEvaluationResultValidator {

  private RuleEvaluationResultValidator() {}

  public static ValidationResult validate(
      RuleEvaluationResult result,
      AnalysisView view,
      AnalysisResultSource analysisResultSource,
      RuleTypeRegistry ruleTypeRegistry,
      RuleRegistry ruleRegistry) {
    Objects.requireNonNull(result, "result");
    Objects.requireNonNull(view, "view");
    Objects.requireNonNull(analysisResultSource, "analysisResultSource");
    Objects.requireNonNull(ruleTypeRegistry, "ruleTypeRegistry");
    Objects.requireNonNull(ruleRegistry, "ruleRegistry");

    List<String> violations = new ArrayList<>();

    if (!result.sourceSnapshotId().equals(view.sourceSnapshotId())) {
      violations.add(
          "Rule Evaluation Result " + result.id() + " declares source CSM Snapshot " + result.sourceSnapshotId()
              + " but was validated against a view of " + view.sourceSnapshotId());
    }

    for (AnalysisResultId consumedId : result.consumedAnalysisResultIds()) {
      if (analysisResultSource.read(consumedId).isEmpty()) {
        violations.add(
            "Rule Evaluation Result " + result.id() + " references Analysis Result " + consumedId
                + " which does not exist");
      }
    }

    CsmScopeInstance scopeInstance = result.scopeInstance();
    if (scopeInstance.isAnchored()) {
      CsmElementId anchorId = scopeInstance.anchorElementId().orElseThrow();
      Optional<CsmElement> anchorElement = view.element(anchorId);
      if (anchorElement.isEmpty()) {
        violations.add(
            "Rule Evaluation Result " + result.id() + " references CSM element " + anchorId
                + " which does not exist within the source CSM Snapshot it claims to be computed"
                + " against");
      }
    }

    Optional<Rule> rule = ruleRegistry.lookup(result.ruleIdentifier());
    if (rule.isEmpty()) {
      violations.add(
          "Rule Evaluation Result " + result.id() + " declares producing Rule " + result.ruleIdentifier()
              + " which is not currently registered");
    } else if (rule.get().version() != result.ruleVersion()) {
      violations.add(
          "Rule Evaluation Result " + result.id() + " declares Rule version " + result.ruleVersion()
              + " but the currently registered version of " + result.ruleIdentifier() + " is "
              + rule.get().version());
    } else {
      Optional<RuleType> ruleType = ruleTypeRegistry.lookup(rule.get().ruleTypeIdentifier());
      if (ruleType.isEmpty()) {
        violations.add(
            "Rule Evaluation Result " + result.id() + "'s Rule references Rule Type "
                + rule.get().ruleTypeIdentifier() + " which is not currently registered");
      } else {
        CsmScope declaredScope = ruleType.get().scope();
        if (declaredScope.containmentAnchor().isPresent() != scopeInstance.isAnchored()) {
          violations.add(
              "Rule Evaluation Result " + result.id() + "'s Scope instance anchoring does not match its"
                  + " Rule Type's declared Rule Scope");
        } else if (declaredScope.containmentAnchor().isPresent()) {
          CsmEntityKind declaredAnchorKind = declaredScope.containmentAnchor().orElseThrow();
          CsmElementId anchorId = scopeInstance.anchorElementId().orElseThrow();
          Optional<CsmElement> anchorElement = view.element(anchorId);
          if (anchorElement.isPresent() && anchorElement.get().kind() != declaredAnchorKind) {
            violations.add(
                "Rule Evaluation Result " + result.id() + "'s Scope instance is anchored at a "
                    + anchorElement.get().kind() + " element, but its Rule Type declares an anchor of "
                    + declaredAnchorKind);
          }
        }
      }
    }

    return violations.isEmpty() ? ValidationResult.success() : ValidationResult.failure(violations);
  }
}
