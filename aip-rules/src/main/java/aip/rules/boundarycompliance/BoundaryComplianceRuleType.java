package aip.rules.boundarycompliance;

import aip.core.csm.AnalysisResult;
import aip.core.csm.AnalysisView;
import aip.core.csm.CsmElementId;
import aip.core.csm.CsmEntityKind;
import aip.core.csm.CsmRelationship;
import aip.core.csm.CsmRelationshipType;
import aip.core.csm.CsmScope;
import aip.core.csm.CsmScopeInstance;
import aip.rules.Rule;
import aip.rules.RuleType;
import aip.rules.RuleTypeEvaluation;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The first concrete Rule Type: detects Architectural Boundary
 * violations, per {@code Boundary-Compliance Condition —
 * Must-Not-Depend-On Violations}. Configuration of Rule Framework's
 * already-specified extension mechanism — introduces no new Rule
 * Framework mechanism (`define-architecture-compliance-agent/design.md`
 * Decisions 1, 2, 9).
 *
 * <p>Rule Scope: {@code DEPENDENCY} and {@code BOUNDARY_CONSTRAINT}
 * relationship kinds, anchored at {@code ARCHITECTURE_COMPONENT}. No
 * required Analyzer inputs — both relationship kinds are read directly
 * from {@link AnalysisView}.
 *
 * <p>For a given anchored Architecture Component, only {@code
 * BOUNDARY_CONSTRAINT} relationships carrying {@link
 * BoundaryConstraintKind#MUST_NOT_DEPEND_ON} are applicable
 * constraints (per {@code implement-architecture-compliance-agent/
 * design.md} Decision 2) — any other constraint shape (e.g. "must only
 * communicate via") is never evaluated, per spec {@code Must-Only-
 * Communicate-Via Constraints Are Out of Scope}.
 */
public final class BoundaryComplianceRuleType implements RuleType {

  public static final String IDENTIFIER = "rule-type.architecture-compliance.boundary-compliance";
  private static final int VERSION = 1;

  private static final CsmScope SCOPE =
      CsmScope.ofRelationshipTypes(Set.of(CsmRelationshipType.DEPENDENCY, CsmRelationshipType.BOUNDARY_CONSTRAINT))
          .anchoredAt(CsmEntityKind.ARCHITECTURE_COMPONENT);

  @Override
  public String identifier() {
    return IDENTIFIER;
  }

  @Override
  public int version() {
    return VERSION;
  }

  @Override
  public Set<String> requiredAnalyzerIdentifiers() {
    return Set.of();
  }

  @Override
  public CsmScope scope() {
    return SCOPE;
  }

  @Override
  public RuleTypeEvaluation evaluate(
      Rule rule, AnalysisView view, CsmScopeInstance scopeInstance, Map<String, Set<AnalysisResult>> consumedAnalysisResults) {
    CsmElementId anchor = scopeInstance.anchorElementId().orElseThrow();

    Set<CsmRelationship> applicableConstraints =
        view.relationshipsOfType(CsmRelationshipType.BOUNDARY_CONSTRAINT).stream()
            .filter(r -> r.sourceId().equals(anchor))
            .filter(BoundaryConstraintKind::isMustNotDependOn)
            .filter(r -> r.targetId().isPresent())
            .collect(Collectors.toSet());

    if (applicableConstraints.isEmpty()) {
      return RuleTypeEvaluation.notApplicable(
          "Architecture Component " + anchor + " has no applicable 'must not depend on' boundary constraint");
    }

    Set<CsmElementId> dependencyTargets =
        view.relationshipsOfType(CsmRelationshipType.DEPENDENCY).stream()
            .filter(r -> r.sourceId().equals(anchor))
            .flatMap(r -> r.targetId().stream())
            .collect(Collectors.toSet());

    for (CsmRelationship constraint : applicableConstraints) {
      CsmElementId target = constraint.targetId().orElseThrow();
      if (dependencyTargets.contains(target)) {
        return RuleTypeEvaluation.fail(new BoundaryComplianceDiagnostic(anchor, target, constraint.id()));
      }
    }

    return RuleTypeEvaluation.pass(
        "Architecture Component " + anchor + " violates no applicable 'must not depend on' boundary constraint");
  }
}
