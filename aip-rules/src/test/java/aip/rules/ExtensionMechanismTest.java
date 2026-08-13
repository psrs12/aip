package aip.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.core.csm.AnalysisView;
import aip.core.csm.CsmElementId;
import aip.core.csm.CsmEntityKind;
import aip.core.csm.CsmRelationshipType;
import aip.core.csm.CsmScope;
import aip.core.csm.CsmScopeInstance;
import aip.core.csm.DependencyKind;
import aip.core.csm.RuleEvaluationResult;
import aip.core.csm.ValidationResult;
import aip.rules.test.fixtures.CsmSnapshotSourceBuilder;
import aip.rules.test.fixtures.InMemoryAnalysisResultSource;
import aip.rules.test.fixtures.InMemoryRuleEvaluationResultStore;
import aip.rules.test.fixtures.StubRuleType;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Confirms registering a new Rule Type for a previously-unused Rule
 * Scope requires no change to Rule Evaluation Result identity,
 * traceability, or validation mechanisms — mirroring {@code
 * aip-analysis}'s own Extension Mechanism Verification.
 */
class ExtensionMechanismTest {

  @Test
  void aStructurallyDistinctSecondRuleTypeUsesTheSameCoreMechanismsUnmodified() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    CsmElementId moduleA = builder.module("moduleA");
    CsmElementId moduleB = builder.module("moduleB");
    builder.dependency(moduleA, moduleB, DependencyKind.COMPILE_TIME);
    AnalysisView view = AnalysisView.from(builder.build());

    RuleTypeRegistry ruleTypeRegistry = new RuleTypeRegistry();
    // First Rule Type: unanchored, entity-kind scope, no Analyzer inputs.
    ruleTypeRegistry.register(new StubRuleType("ruletype.first", 1, CsmScope.ofEntityKinds(Set.of(CsmEntityKind.MODULE))));
    // Second Rule Type: structurally distinct - relationship-type
    // scope, anchored, with a declared Analyzer input the first
    // doesn't have.
    ruleTypeRegistry.register(
        new StubRuleType(
            "ruletype.second",
            1,
            Set.of("analyzer.a"),
            CsmScope.ofRelationshipTypes(Set.of(CsmRelationshipType.DEPENDENCY)).anchoredAt(CsmEntityKind.MODULE),
            ctx -> RuleTypeEvaluation.pass("checked")));

    RuleRegistry ruleRegistry = new RuleRegistry(ruleTypeRegistry);
    ruleRegistry.register(new Rule("rule.first", 1, "ruletype.first", Map.of()));
    ruleRegistry.register(new Rule("rule.second", 1, "ruletype.second", Map.of()));

    InMemoryAnalysisResultSource analysisResultSource = new InMemoryAnalysisResultSource();
    RuleEvaluationOrchestrator orchestrator = new RuleEvaluationOrchestrator(ruleTypeRegistry, ruleRegistry, analysisResultSource);
    List<RuleEvaluationResult> results = orchestrator.run(view);

    RuleEvaluationResult firstResult =
        results.stream().filter(r -> r.ruleIdentifier().equals("rule.first")).findFirst().orElseThrow();
    assertEquals(CsmScopeInstance.wholeRepository(), firstResult.scopeInstance());
    assertTrue(
        results.stream().anyMatch(r -> r.ruleIdentifier().equals("rule.second")),
        "the second, structurally distinct Rule Type SHALL also be evaluated and produce a Result");

    InMemoryRuleEvaluationResultStore store = new InMemoryRuleEvaluationResultStore();
    for (RuleEvaluationResult result : results) {
      ValidationResult validation =
          RuleEvaluationResultValidator.validate(result, view, analysisResultSource, ruleTypeRegistry, ruleRegistry);
      assertTrue(validation.valid(), () -> "expected valid: " + validation.violations());
      store.write(result);
      assertEquals(result, store.read(result.id()).orElseThrow());
    }
  }
}
