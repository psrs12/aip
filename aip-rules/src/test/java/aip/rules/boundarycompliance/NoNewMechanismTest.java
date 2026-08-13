package aip.rules.boundarycompliance;

import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.core.csm.AnalysisView;
import aip.core.csm.CsmElementId;
import aip.core.csm.DependencyKind;
import aip.core.csm.RuleEvaluationResult;
import aip.core.csm.ValidationResult;
import aip.rules.Rule;
import aip.rules.RuleEvaluationOrchestrator;
import aip.rules.RuleEvaluationResultValidator;
import aip.rules.RuleRegistry;
import aip.rules.RuleTypeRegistry;
import aip.rules.test.fixtures.CsmSnapshotSourceBuilder;
import aip.rules.test.fixtures.InMemoryAnalysisResultSource;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Confirms this capability introduces no new artifact, identity,
 * traceability, or validation mechanism — every output is an ordinary
 * {@link RuleEvaluationResult}, validated through {@code aip-rules}'s
 * own already-established {@link RuleEvaluationResultValidator}
 * unmodified, per spec {@code Architecture Compliance Agent Introduces
 * No New Artifact or Mechanism} and {@code Boundary-Compliance Rule
 * Type Registration}.
 */
class NoNewMechanismTest {

  @Test
  void registrationRequiresNoCoreMechanismChangeAndProducesAnOrdinaryValidatableResult() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    CsmElementId moduleA = builder.module("moduleA");
    CsmElementId moduleB = builder.module("moduleB");
    CsmElementId componentA = builder.architectureComponent("ComponentA", List.of(moduleA));
    CsmElementId componentB = builder.architectureComponent("ComponentB", List.of(moduleB));
    builder.mustNotDependOnConstraint(componentA, componentB);
    builder.dependency(componentA, componentB, DependencyKind.COMPILE_TIME);

    AnalysisView view = AnalysisView.from(builder.build());
    RuleTypeRegistry ruleTypeRegistry = new RuleTypeRegistry();
    ruleTypeRegistry.register(new BoundaryComplianceRuleType());
    RuleRegistry ruleRegistry = new RuleRegistry(ruleTypeRegistry);
    ruleRegistry.register(new Rule("rule.boundary-compliance", 1, BoundaryComplianceRuleType.IDENTIFIER, Map.of()));

    List<RuleEvaluationResult> results =
        new RuleEvaluationOrchestrator(ruleTypeRegistry, ruleRegistry, new InMemoryAnalysisResultSource()).run(view);
    assertTrue(!results.isEmpty());

    // Every produced RuleEvaluationResult validates through
    // RuleEvaluationResultValidator's own already-established,
    // unmodified logic - no new validation mechanism exists for this
    // Rule Type.
    for (RuleEvaluationResult result : results) {
      ValidationResult validation =
          RuleEvaluationResultValidator.validate(
              result, view, new InMemoryAnalysisResultSource(), ruleTypeRegistry, ruleRegistry);
      assertTrue(validation.valid(), () -> "expected valid for " + result.id() + ": " + validation.violations());
    }
  }
}
