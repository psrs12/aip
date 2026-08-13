package aip.rules.test.fixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.core.csm.AnalysisView;
import aip.core.csm.CsmElementId;
import aip.core.csm.CsmEntityKind;
import aip.core.csm.CsmRelationshipType;
import aip.core.csm.CsmScope;
import aip.core.csm.RuleEvaluationOutcome;
import aip.core.csm.RuleEvaluationResult;
import aip.core.csm.ValidationResult;
import aip.rules.Rule;
import aip.rules.RuleEvaluationOrchestrator;
import aip.rules.RuleEvaluationResultPublisher;
import aip.rules.RuleEvaluationResultValidator;
import aip.rules.RuleRegistry;
import aip.rules.RuleType;
import aip.rules.RuleTypeEvaluation;
import aip.rules.RuleTypeRegistry;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * A representative end-to-end scenario exercising a boundary-
 * compliance-shaped Rule Type — "a Module must not depend on a Module
 * it has a `BOUNDARY_CONSTRAINT` relationship toward" — against three
 * Modules, covering all three outcomes ({@code PASS}, {@code FAIL},
 * {@code NOT_APPLICABLE}) in one run, then validating and publishing
 * every Result. Mirrors {@code aip-analysis}'s own {@code
 * AnalysisFrameworkFixtureEndToEndTest}.
 */
class RuleFrameworkFixtureEndToEndTest {

  @Test
  void boundaryComplianceShapedRuleTypeProducesAllThreeOutcomesAcrossThreeModules() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    // moduleFail: has a boundary constraint toward moduleTarget AND an
    // observed dependency violating it -> FAIL.
    CsmElementId moduleFail = builder.module("moduleFail");
    CsmElementId moduleTarget = builder.module("moduleTarget");
    builder.boundaryConstraint(moduleFail, moduleTarget);
    builder.dependency(moduleFail, moduleTarget, aip.core.csm.DependencyKind.COMPILE_TIME);

    // modulePass: has the same kind of boundary constraint, but no
    // violating dependency -> PASS.
    CsmElementId modulePass = builder.module("modulePass");
    CsmElementId otherTarget = builder.module("otherTarget");
    builder.boundaryConstraint(modulePass, otherTarget);

    // moduleNotApplicable: has an observed dependency but no boundary
    // constraint declared at all -> the Rule Type is invoked (kind-based
    // applicability passes on the DEPENDENCY relationship alone) but has
    // no constraint to check -> NOT_APPLICABLE.
    CsmElementId moduleNotApplicable = builder.module("moduleNotApplicable");
    CsmElementId someTarget = builder.module("someTarget");
    builder.dependency(moduleNotApplicable, someTarget, aip.core.csm.DependencyKind.RUNTIME);

    AnalysisView view = AnalysisView.from(builder.build());

    RuleType boundaryComplianceRuleType =
        new RuleType() {
          @Override
          public String identifier() {
            return "ruletype.boundary-compliance";
          }

          @Override
          public int version() {
            return 1;
          }

          @Override
          public Set<String> requiredAnalyzerIdentifiers() {
            return Set.of();
          }

          @Override
          public CsmScope scope() {
            return CsmScope.ofRelationshipTypes(Set.of(CsmRelationshipType.DEPENDENCY, CsmRelationshipType.BOUNDARY_CONSTRAINT))
                .anchoredAt(CsmEntityKind.MODULE);
          }

          @Override
          public RuleTypeEvaluation evaluate(
              Rule rule,
              AnalysisView view,
              aip.core.csm.CsmScopeInstance scopeInstance,
              java.util.Map<String, Set<aip.core.csm.AnalysisResult>> consumedAnalysisResults) {
            CsmElementId anchor = scopeInstance.anchorElementId().orElseThrow();
            Set<CsmElementId> boundaryTargets =
                view.relationshipsOfType(CsmRelationshipType.BOUNDARY_CONSTRAINT).stream()
                    .filter(r -> r.sourceId().equals(anchor))
                    .flatMap(r -> r.targetId().stream())
                    .collect(java.util.stream.Collectors.toSet());
            if (boundaryTargets.isEmpty()) {
              return RuleTypeEvaluation.notApplicable("no boundary constraint declared for " + anchor);
            }
            boolean violated =
                view.relationshipsOfType(CsmRelationshipType.DEPENDENCY).stream()
                    .filter(r -> r.sourceId().equals(anchor))
                    .anyMatch(r -> r.targetId().map(boundaryTargets::contains).orElse(false));
            return violated
                ? RuleTypeEvaluation.fail("violates declared boundary")
                : RuleTypeEvaluation.pass("complies with declared boundary");
          }
        };

    RuleTypeRegistry ruleTypeRegistry = new RuleTypeRegistry();
    ruleTypeRegistry.register(boundaryComplianceRuleType);
    RuleRegistry ruleRegistry = new RuleRegistry(ruleTypeRegistry);
    ruleRegistry.register(new Rule("rule.boundary-compliance", 1, "ruletype.boundary-compliance", java.util.Map.of()));

    InMemoryAnalysisResultSource analysisResultSource = new InMemoryAnalysisResultSource();
    List<RuleEvaluationResult> results =
        new RuleEvaluationOrchestrator(ruleTypeRegistry, ruleRegistry, analysisResultSource).run(view);

    // Six Module elements total (moduleFail, moduleTarget, modulePass,
    // otherTarget, moduleNotApplicable, someTarget) - the Rule Scope is
    // anchored at MODULE, and kind-based applicability is satisfied for
    // a Module playing either the source or target role in a
    // DEPENDENCY/BOUNDARY_CONSTRAINT relationship, so every Module here
    // is applicable and gets its own evaluated Result: FAIL
    // (moduleFail), PASS (modulePass), and NOT_APPLICABLE for the four
    // Modules that are only ever a relationship's target, never a
    // BOUNDARY_CONSTRAINT source themselves (moduleTarget, otherTarget,
    // moduleNotApplicable, someTarget).
    assertEquals(6, results.size());
    Set<RuleEvaluationOutcome> outcomes = results.stream().map(RuleEvaluationResult::outcome).collect(java.util.stream.Collectors.toSet());
    assertEquals(Set.of(RuleEvaluationOutcome.PASS, RuleEvaluationOutcome.FAIL, RuleEvaluationOutcome.NOT_APPLICABLE), outcomes);

    InMemoryRuleEvaluationResultStore store = new InMemoryRuleEvaluationResultStore();
    for (RuleEvaluationResult result : results) {
      ValidationResult validation =
          RuleEvaluationResultValidator.validate(result, view, analysisResultSource, ruleTypeRegistry, ruleRegistry);
      assertTrue(validation.valid(), () -> "expected valid for " + result.id() + ": " + validation.violations());

      RuleEvaluationResultPublisher.PublicationOutcome outcome =
          RuleEvaluationResultPublisher.publish(store, result, view, analysisResultSource, ruleTypeRegistry, ruleRegistry);
      assertTrue(outcome.published());
      assertEquals(result.id(), store.read(result.id()).orElseThrow().id());
    }
  }
}
