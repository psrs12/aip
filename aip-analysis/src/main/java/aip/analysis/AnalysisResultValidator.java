package aip.analysis;

import aip.core.csm.AnalysisResult;
import aip.core.csm.AnalysisView;
import aip.core.csm.CsmElement;
import aip.core.csm.CsmElementId;
import aip.core.csm.CsmEntityKind;
import aip.core.csm.CsmScope;
import aip.core.csm.CsmScopeInstance;
import aip.core.csm.ValidationResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Validates a constructed {@link AnalysisResult} before it is
 * considered usable output, per {@code Analysis Result Validation
 * Before Publication}: referential integrity (every CSM element/
 * Subject identity the Result references SHALL exist within the
 * source CSM Snapshot), Analyzer/version consistency (the declared
 * producer SHALL correspond to a currently registered Analyzer), and
 * scope containment (the Result's content SHALL remain within its
 * Analyzer's declared Analysis Scope).
 *
 * <p>The framework does not, and cannot, interpret an Analyzer's
 * opaque payload (per {@code Analysis Result Payload Is
 * Analyzer-Defined}) — the CSM identities this validator checks are
 * exactly the ones {@link AnalysisResult} itself directly retains
 * (its Scope instance's anchor element, when anchored), not anything
 * reachable only through the payload.
 */
public final class AnalysisResultValidator {

  private AnalysisResultValidator() {}

  public static ValidationResult validate(AnalysisResult result, AnalysisView view, AnalyzerRegistry registry) {
    Objects.requireNonNull(result, "result");
    Objects.requireNonNull(view, "view");
    Objects.requireNonNull(registry, "registry");

    List<String> violations = new ArrayList<>();

    if (!result.sourceSnapshotId().equals(view.sourceSnapshotId())) {
      violations.add(
          "Analysis Result " + result.id() + " declares source CSM Snapshot " + result.sourceSnapshotId()
              + " but was validated against a view of " + view.sourceSnapshotId());
    }

    CsmScopeInstance scopeInstance = result.scopeInstance();
    if (scopeInstance.isAnchored()) {
      CsmElementId anchorId = scopeInstance.anchorElementId().orElseThrow();
      Optional<CsmElement> anchorElement = view.element(anchorId);
      if (anchorElement.isEmpty()) {
        violations.add(
            "Analysis Result " + result.id() + " references CSM element " + anchorId
                + " which does not exist within the source CSM Snapshot it claims to be computed"
                + " against");
      }
    }

    Optional<Analyzer> analyzer = registry.lookup(result.analyzerIdentifier());
    if (analyzer.isEmpty()) {
      violations.add(
          "Analysis Result " + result.id() + " declares producing Analyzer " + result.analyzerIdentifier()
              + " which is not currently registered");
    } else if (analyzer.get().version() != result.analyzerVersion()) {
      violations.add(
          "Analysis Result " + result.id() + " declares Analyzer version " + result.analyzerVersion()
              + " but the currently registered version of " + result.analyzerIdentifier() + " is "
              + analyzer.get().version());
    } else {
      CsmScope declaredScope = analyzer.get().scope();
      if (declaredScope.containmentAnchor().isPresent() != scopeInstance.isAnchored()) {
        violations.add(
            "Analysis Result " + result.id() + "'s Scope instance anchoring does not match its"
                + " Analyzer's declared Analysis Scope");
      } else if (declaredScope.containmentAnchor().isPresent()) {
        CsmEntityKind declaredAnchorKind = declaredScope.containmentAnchor().orElseThrow();
        CsmElementId anchorId = scopeInstance.anchorElementId().orElseThrow();
        Optional<CsmElement> anchorElement = view.element(anchorId);
        if (anchorElement.isPresent() && anchorElement.get().kind() != declaredAnchorKind) {
          violations.add(
              "Analysis Result " + result.id() + "'s Scope instance is anchored at a "
                  + anchorElement.get().kind() + " element, but its Analyzer declares an anchor of "
                  + declaredAnchorKind);
        }
      }
    }

    return violations.isEmpty() ? ValidationResult.success() : ValidationResult.failure(violations);
  }
}
