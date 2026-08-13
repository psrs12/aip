package aip.rules.boundarycompliance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.core.csm.AnalysisView;
import aip.core.csm.CsmElementId;
import aip.core.csm.CsmEntityKind;
import aip.core.csm.CsmScopeInstance;
import aip.core.csm.DependencyKind;
import aip.core.csm.RuleEvaluationOutcome;
import aip.core.csm.RuleEvaluationResult;
import aip.rules.Rule;
import aip.rules.RuleEvaluationOrchestrator;
import aip.rules.RuleRegistry;
import aip.rules.RuleTypeRegistry;
import aip.rules.test.fixtures.CsmSnapshotSourceBuilder;
import aip.rules.test.fixtures.InMemoryAnalysisResultSource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * {@link BoundaryComplianceRuleType} tests, per {@code Boundary-
 * Compliance Condition — Must-Not-Depend-On Violations}, {@code
 * NOT_APPLICABLE Semantics for Boundary-Compliance Checking}, {@code
 * Boundary-Compliance Outcome Determination}, and {@code Must-Only-
 * Communicate-Via Constraints Are Out of Scope}.
 */
class BoundaryComplianceRuleTypeTest {

  private static List<RuleEvaluationResult> evaluate(CsmSnapshotSourceBuilder builder) {
    AnalysisView view = AnalysisView.from(builder.build());
    RuleTypeRegistry ruleTypeRegistry = new RuleTypeRegistry();
    ruleTypeRegistry.register(new BoundaryComplianceRuleType());
    RuleRegistry ruleRegistry = new RuleRegistry(ruleTypeRegistry);
    ruleRegistry.register(new Rule("rule.boundary-compliance", 1, BoundaryComplianceRuleType.IDENTIFIER, Map.of()));
    return new RuleEvaluationOrchestrator(ruleTypeRegistry, ruleRegistry, new InMemoryAnalysisResultSource()).run(view);
  }

  @Test
  void violatingDependencyProducesFail() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    CsmElementId moduleA = builder.module("moduleA");
    CsmElementId moduleB = builder.module("moduleB");
    CsmElementId componentA = builder.architectureComponent("ComponentA", List.of(moduleA));
    CsmElementId componentB = builder.architectureComponent("ComponentB", List.of(moduleB));
    builder.mustNotDependOnConstraint(componentA, componentB);
    builder.dependency(componentA, componentB, DependencyKind.COMPILE_TIME);

    List<RuleEvaluationResult> results = evaluate(builder);
    RuleEvaluationResult resultForA = resultForAnchor(results, componentA);
    assertEquals(RuleEvaluationOutcome.FAIL, resultForA.outcome());
    BoundaryComplianceDiagnostic diagnostic = (BoundaryComplianceDiagnostic) resultForA.payload();
    assertEquals(componentA, diagnostic.violatingComponentId());
    assertEquals(componentB, diagnostic.dependencyTargetId());
  }

  @Test
  void noViolatingDependencySatisfiesTheConditionAndProducesPass() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    CsmElementId moduleA = builder.module("moduleA");
    CsmElementId moduleB = builder.module("moduleB");
    CsmElementId componentA = builder.architectureComponent("ComponentA", List.of(moduleA));
    CsmElementId componentB = builder.architectureComponent("ComponentB", List.of(moduleB));
    builder.mustNotDependOnConstraint(componentA, componentB);
    // no dependency relationship at all - vacuous compliance

    List<RuleEvaluationResult> results = evaluate(builder);
    assertEquals(RuleEvaluationOutcome.PASS, resultForAnchor(results, componentA).outcome());
  }

  @Test
  void componentWithNoRelevantRelationshipsProducesNoResult() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    CsmElementId moduleA = builder.module("moduleA");
    CsmElementId componentA = builder.architectureComponent("ComponentA", List.of(moduleA));
    // no BOUNDARY_CONSTRAINT, no DEPENDENCY relationship at all

    List<RuleEvaluationResult> results = evaluate(builder);
    assertTrue(resultsForAnchor(results, componentA).isEmpty());
  }

  @Test
  void dependencyPresentWithNoApplicableConstraintProducesNotApplicable() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    CsmElementId moduleA = builder.module("moduleA");
    CsmElementId moduleB = builder.module("moduleB");
    CsmElementId componentA = builder.architectureComponent("ComponentA", List.of(moduleA));
    CsmElementId componentB = builder.architectureComponent("ComponentB", List.of(moduleB));
    builder.dependency(componentA, componentB, DependencyKind.COMPILE_TIME);
    // a DEPENDENCY relationship exists, but no applicable "must not depend on" constraint

    List<RuleEvaluationResult> results = evaluate(builder);
    assertEquals(RuleEvaluationOutcome.NOT_APPLICABLE, resultForAnchor(results, componentA).outcome());
  }

  @Test
  void nonMustNotDependOnConstraintIsNeverEvaluated() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    CsmElementId moduleA = builder.module("moduleA");
    CsmElementId moduleB = builder.module("moduleB");
    CsmElementId componentA = builder.architectureComponent("ComponentA", List.of(moduleA));
    CsmElementId componentB = builder.architectureComponent("ComponentB", List.of(moduleB));
    // A "must only communicate via" constraint exists, but is not a
    // "must not depend on" constraint - so it is never evaluated, and
    // the violating dependency below produces NOT_APPLICABLE (no
    // applicable constraint), not FAIL.
    builder.mustOnlyCommunicateViaConstraint(componentA, componentB);
    builder.dependency(componentA, componentB, DependencyKind.COMPILE_TIME);

    List<RuleEvaluationResult> results = evaluate(builder);
    assertEquals(RuleEvaluationOutcome.NOT_APPLICABLE, resultForAnchor(results, componentA).outcome());
  }

  @Test
  void categoryAndSeverityAreFixedAcrossDifferentComponents() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    CsmElementId moduleA = builder.module("moduleA");
    CsmElementId moduleB = builder.module("moduleB");
    CsmElementId moduleC = builder.module("moduleC");
    CsmElementId componentA = builder.architectureComponent("ComponentA", List.of(moduleA));
    CsmElementId componentB = builder.architectureComponent("ComponentB", List.of(moduleB));
    CsmElementId componentC = builder.architectureComponent("ComponentC", List.of(moduleC));
    builder.mustNotDependOnConstraint(componentA, componentC);
    builder.dependency(componentA, componentC, DependencyKind.COMPILE_TIME);
    builder.mustNotDependOnConstraint(componentB, componentC);
    builder.dependency(componentB, componentC, DependencyKind.RUNTIME);

    List<RuleEvaluationResult> results = evaluate(builder);
    BoundaryComplianceDiagnostic diagnosticA = (BoundaryComplianceDiagnostic) resultForAnchor(results, componentA).payload();
    BoundaryComplianceDiagnostic diagnosticB = (BoundaryComplianceDiagnostic) resultForAnchor(results, componentB).payload();
    assertEquals(diagnosticA.category(), diagnosticB.category());
    assertEquals(diagnosticA.severity(), diagnosticB.severity());
    // description differs - per-instance, per implement-architecture-compliance-agent/design.md Decision 5
    assertTrue(!diagnosticA.description().equals(diagnosticB.description()));
  }

  @Test
  void declaredAndInferredBoundaryViolationsAreDeterminedIdentically() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    CsmElementId moduleA = builder.module("moduleA");
    CsmElementId moduleB = builder.module("moduleB");
    CsmElementId moduleC = builder.module("moduleC");
    CsmElementId componentDeclared = builder.architectureComponent("ComponentDeclared", List.of(moduleA));
    CsmElementId componentInferred = builder.architectureComponentInferred("ComponentInferred", List.of(moduleB));
    CsmElementId target = builder.architectureComponent("Target", List.of(moduleC));
    builder.mustNotDependOnConstraint(componentDeclared, target);
    builder.dependency(componentDeclared, target, DependencyKind.COMPILE_TIME);
    builder.mustNotDependOnConstraintInferred(componentInferred, target);
    builder.dependency(componentInferred, target, DependencyKind.COMPILE_TIME);

    List<RuleEvaluationResult> results = evaluate(builder);
    RuleEvaluationResult declaredResult = resultForAnchor(results, componentDeclared);
    RuleEvaluationResult inferredResult = resultForAnchor(results, componentInferred);
    assertEquals(RuleEvaluationOutcome.FAIL, declaredResult.outcome());
    assertEquals(RuleEvaluationOutcome.FAIL, inferredResult.outcome());
    BoundaryComplianceDiagnostic declaredDiagnostic = (BoundaryComplianceDiagnostic) declaredResult.payload();
    BoundaryComplianceDiagnostic inferredDiagnostic = (BoundaryComplianceDiagnostic) inferredResult.payload();
    assertEquals(declaredDiagnostic.category(), inferredDiagnostic.category());
    assertEquals(declaredDiagnostic.severity(), inferredDiagnostic.severity());
  }

  private static RuleEvaluationResult resultForAnchor(List<RuleEvaluationResult> results, CsmElementId anchor) {
    return resultsForAnchor(results, anchor).stream()
        .findFirst()
        .orElseThrow(() -> new AssertionError("no result for anchor " + anchor));
  }

  private static List<RuleEvaluationResult> resultsForAnchor(List<RuleEvaluationResult> results, CsmElementId anchor) {
    return results.stream()
        .filter(r -> r.scopeInstance().equals(CsmScopeInstance.anchoredAt(anchor)))
        .collect(Collectors.toList());
  }
}
