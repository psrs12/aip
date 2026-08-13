package aip.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.core.csm.AnalysisResult;
import aip.core.csm.AnalysisView;
import aip.core.csm.CsmElement;
import aip.core.csm.CsmEntityKind;
import aip.core.csm.CsmRelationship;
import aip.core.csm.CsmScope;
import aip.core.csm.CsmScopeInstance;
import aip.core.csm.CsmSnapshotSource;
import aip.core.csm.RuleEvaluationResult;
import aip.rules.test.fixtures.CsmSnapshotSourceBuilder;
import aip.rules.test.fixtures.InMemoryAnalysisResultSource;
import aip.rules.test.fixtures.StubRuleType;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Dependency and architectural invariant tests, per {@code CSM and
 * Analysis Content Reached Only Through Established Contracts}, {@code
 * Rule Evaluation Results Are a Distinct Concept From CSM Knowledge,
 * Analysis Results, and Findings}, and {@code No Rule-to-Rule
 * Composition}.
 */
class DependencyAndBoundaryTest {

  private static final CsmScope MODULE_SCOPE = CsmScope.ofEntityKinds(Set.of(CsmEntityKind.MODULE));

  @Test
  void allCsmContentComesFromTheAnalysisViewAlone() {
    // RuleType.evaluate's own signature has no Repository Evidence or
    // Runtime Model parameter - the only CSM-content-bearing argument
    // is AnalysisView, and the only Analysis-Result-bearing argument
    // is the consumedAnalysisResults map resolved via
    // AnalysisResultSource. There is no other code path to read either
    // from.
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    builder.module("moduleA");
    AnalysisView view = AnalysisView.from(builder.build());

    RuleTypeRegistry ruleTypeRegistry = new RuleTypeRegistry();
    ruleTypeRegistry.register(new StubRuleType("ruletype.a", 1, MODULE_SCOPE));
    RuleRegistry ruleRegistry = new RuleRegistry(ruleTypeRegistry);
    ruleRegistry.register(new Rule("rule.a", 1, "ruletype.a", Map.of()));

    List<RuleEvaluationResult> results =
        new RuleEvaluationOrchestrator(ruleTypeRegistry, ruleRegistry, new InMemoryAnalysisResultSource()).run(view);
    assertEquals(1, results.size());
  }

  @Test
  void producingARuleEvaluationResultDoesNotAlterCsmOrAnalysisContent() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    builder.module("moduleA");
    CsmSnapshotSource source = builder.build();
    Set<CsmElement> elementsBefore = Set.copyOf(source.elements());
    Set<CsmRelationship> relationshipsBefore = Set.copyOf(source.relationships());

    AnalysisResult analysisResult =
        AnalysisResult.of("analyzer.a", 1, source.id(), CsmScopeInstance.wholeRepository(), "payload");
    InMemoryAnalysisResultSource analysisResultSource = new InMemoryAnalysisResultSource().with(analysisResult);

    RuleTypeRegistry ruleTypeRegistry = new RuleTypeRegistry();
    ruleTypeRegistry.register(new StubRuleType("ruletype.a", 1, Set.of("analyzer.a"), MODULE_SCOPE, ctx -> RuleTypeEvaluation.pass("ok")));
    RuleRegistry ruleRegistry = new RuleRegistry(ruleTypeRegistry);
    ruleRegistry.register(new Rule("rule.a", 1, "ruletype.a", Map.of()));

    new RuleEvaluationOrchestrator(ruleTypeRegistry, ruleRegistry, analysisResultSource).run(AnalysisView.from(source));

    assertEquals(elementsBefore, source.elements());
    assertEquals(relationshipsBefore, source.relationships());
    assertEquals(analysisResult, analysisResultSource.read(analysisResult.id()).orElseThrow());
  }

  @Test
  void ruleEvaluationResultIsNeverExposedAsCsmOrAnalysisContent() {
    // RuleEvaluationResult, CsmElement/CsmRelationship, and
    // AnalysisResult are disjoint types - no shared query surface
    // exists through which one could be returned in place of another.
    assertTrue(!CsmElement.class.isAssignableFrom(RuleEvaluationResult.class));
    assertTrue(!CsmRelationship.class.isAssignableFrom(RuleEvaluationResult.class));
    assertTrue(!AnalysisResult.class.isAssignableFrom(RuleEvaluationResult.class));
  }

  @Test
  void noRuleToRuleCompositionMechanismExists() {
    // Rule and RuleType's own APIs have no method through which a
    // declared dependency or ordering between Rules could be
    // expressed, and RuleType.evaluate never receives another Rule's
    // RuleEvaluationResult.
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    builder.module("moduleA");
    AnalysisView view = AnalysisView.from(builder.build());

    RuleTypeRegistry ruleTypeRegistry = new RuleTypeRegistry();
    ruleTypeRegistry.register(new StubRuleType("ruletype.a", 1, MODULE_SCOPE));
    ruleTypeRegistry.register(new StubRuleType("ruletype.b", 1, MODULE_SCOPE));
    RuleRegistry ruleRegistry = new RuleRegistry(ruleTypeRegistry);
    ruleRegistry.register(new Rule("rule.a", 1, "ruletype.a", Map.of()));
    ruleRegistry.register(new Rule("rule.b", 1, "ruletype.b", Map.of()));

    List<RuleEvaluationResult> results =
        new RuleEvaluationOrchestrator(ruleTypeRegistry, ruleRegistry, new InMemoryAnalysisResultSource()).run(view);
    assertEquals(2, results.size());
  }
}
