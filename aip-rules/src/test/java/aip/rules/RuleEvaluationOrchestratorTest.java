package aip.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.core.csm.AnalysisResult;
import aip.core.csm.AnalysisView;
import aip.core.csm.CsmElementId;
import aip.core.csm.CsmEntityKind;
import aip.core.csm.CsmScope;
import aip.core.csm.CsmScopeInstance;
import aip.core.csm.CsmSnapshotSource;
import aip.core.csm.RuleEvaluationOutcome;
import aip.core.csm.RuleEvaluationResult;
import aip.rules.test.fixtures.CsmSnapshotSourceBuilder;
import aip.rules.test.fixtures.InMemoryAnalysisResultSource;
import aip.rules.test.fixtures.StubRuleType;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * {@link RuleEvaluationOrchestrator} tests, per {@code Rule Type
 * declares a kind-based scope} / applicability semantics, {@code No
 * Rule-to-Rule Composition}, {@code Deterministic, Declarative Rule
 * Evaluation Only}, and {@code Single-Repository Rule Evaluation
 * Scope}.
 */
class RuleEvaluationOrchestratorTest {

  private static final CsmScope MODULE_SCOPE = CsmScope.ofEntityKinds(Set.of(CsmEntityKind.MODULE));

  @Test
  void producesOneResultPerApplicableScopeInstance() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    CsmElementId m1 = builder.module("moduleA");
    CsmElementId m2 = builder.module("moduleB");
    AnalysisView view = AnalysisView.from(builder.build());

    RuleTypeRegistry ruleTypeRegistry = new RuleTypeRegistry();
    ruleTypeRegistry.register(new StubRuleType("ruletype.per-module", 1, MODULE_SCOPE.anchoredAt(CsmEntityKind.MODULE)));
    RuleRegistry ruleRegistry = new RuleRegistry(ruleTypeRegistry);
    ruleRegistry.register(new Rule("rule.a", 1, "ruletype.per-module", Map.of()));

    RuleEvaluationOrchestrator orchestrator =
        new RuleEvaluationOrchestrator(ruleTypeRegistry, ruleRegistry, new InMemoryAnalysisResultSource());
    List<RuleEvaluationResult> results = orchestrator.run(view);

    assertEquals(2, results.size());
    Set<CsmScopeInstance> instances = Set.of(results.get(0).scopeInstance(), results.get(1).scopeInstance());
    assertEquals(Set.of(CsmScopeInstance.anchoredAt(m1), CsmScopeInstance.anchoredAt(m2)), instances);
  }

  @Test
  void inapplicableScopeInstanceProducesNoResult() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    builder.module("moduleA");
    AnalysisView view = AnalysisView.from(builder.build());

    RuleTypeRegistry ruleTypeRegistry = new RuleTypeRegistry();
    ruleTypeRegistry.register(new StubRuleType("ruletype.packages-only", 1, CsmScope.ofEntityKinds(Set.of(CsmEntityKind.PACKAGE))));
    RuleRegistry ruleRegistry = new RuleRegistry(ruleTypeRegistry);
    ruleRegistry.register(new Rule("rule.a", 1, "ruletype.packages-only", Map.of()));

    RuleEvaluationOrchestrator orchestrator =
        new RuleEvaluationOrchestrator(ruleTypeRegistry, ruleRegistry, new InMemoryAnalysisResultSource());
    assertTrue(orchestrator.run(view).isEmpty());
  }

  @Test
  void nativeAttributeRefinementSkipsWhenNoMatchingAttributeIsPresent() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    builder.module("moduleA");
    AnalysisView view = AnalysisView.from(builder.build());

    CsmScope scope =
        MODULE_SCOPE.withNativeAttributePredicate(attrs -> attrs.get("ecosystem").filter("maven"::equals).isPresent());
    RuleTypeRegistry ruleTypeRegistry = new RuleTypeRegistry();
    ruleTypeRegistry.register(new StubRuleType("ruletype.ecosystem-specific", 1, scope));
    RuleRegistry ruleRegistry = new RuleRegistry(ruleTypeRegistry);
    ruleRegistry.register(new Rule("rule.a", 1, "ruletype.ecosystem-specific", Map.of()));

    RuleEvaluationOrchestrator orchestrator =
        new RuleEvaluationOrchestrator(ruleTypeRegistry, ruleRegistry, new InMemoryAnalysisResultSource());
    assertTrue(orchestrator.run(view).isEmpty());
  }

  @Test
  void ruleTypeCanProduceNotApplicableForAnApplicableInstanceWithMissingConditionSpecificContent() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    builder.module("moduleA");
    AnalysisView view = AnalysisView.from(builder.build());

    RuleTypeRegistry ruleTypeRegistry = new RuleTypeRegistry();
    ruleTypeRegistry.register(
        new StubRuleType(
            "ruletype.needs-boundary",
            1,
            Set.of(),
            MODULE_SCOPE,
            ctx -> RuleTypeEvaluation.notApplicable("no boundary declared")));
    RuleRegistry ruleRegistry = new RuleRegistry(ruleTypeRegistry);
    ruleRegistry.register(new Rule("rule.a", 1, "ruletype.needs-boundary", Map.of()));

    RuleEvaluationOrchestrator orchestrator =
        new RuleEvaluationOrchestrator(ruleTypeRegistry, ruleRegistry, new InMemoryAnalysisResultSource());
    List<RuleEvaluationResult> results = orchestrator.run(view);

    assertEquals(1, results.size());
    assertEquals(RuleEvaluationOutcome.NOT_APPLICABLE, results.get(0).outcome());
  }

  @Test
  void consumedAnalysisResultsAreResolvedAndIncludedInTheResultsOwnIdentitySet() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    builder.module("moduleA");
    CsmSnapshotSource source = builder.build();
    AnalysisView view = AnalysisView.from(source);

    AnalysisResult analysisResult =
        AnalysisResult.of("analyzer.a", 1, source.id(), CsmScopeInstance.wholeRepository(), "analysis-payload");
    InMemoryAnalysisResultSource analysisResultSource = new InMemoryAnalysisResultSource().with(analysisResult);

    RuleTypeRegistry ruleTypeRegistry = new RuleTypeRegistry();
    ruleTypeRegistry.register(
        new StubRuleType(
            "ruletype.consumer",
            1,
            Set.of("analyzer.a"),
            MODULE_SCOPE,
            ctx -> RuleTypeEvaluation.pass(ctx.consumedAnalysisResults().get("analyzer.a").size())));
    RuleRegistry ruleRegistry = new RuleRegistry(ruleTypeRegistry);
    ruleRegistry.register(new Rule("rule.a", 1, "ruletype.consumer", Map.of()));

    RuleEvaluationOrchestrator orchestrator = new RuleEvaluationOrchestrator(ruleTypeRegistry, ruleRegistry, analysisResultSource);
    List<RuleEvaluationResult> results = orchestrator.run(view);

    assertEquals(1, results.size());
    assertEquals(Set.of(analysisResult.id()), results.get(0).consumedAnalysisResultIds());
    assertEquals(1, results.get(0).payload());
  }

  @Test
  void ruleResultIsUnaffectedByWhichOtherRulesAreRegistered() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    builder.module("moduleA");
    AnalysisView view = AnalysisView.from(builder.build());

    RuleTypeRegistry ruleTypeRegistryAlone = new RuleTypeRegistry();
    ruleTypeRegistryAlone.register(countingRuleType());
    RuleRegistry ruleRegistryAlone = new RuleRegistry(ruleTypeRegistryAlone);
    ruleRegistryAlone.register(new Rule("rule.counter", 1, "ruletype.counter", Map.of()));
    RuleEvaluationResult aloneResult =
        new RuleEvaluationOrchestrator(ruleTypeRegistryAlone, ruleRegistryAlone, new InMemoryAnalysisResultSource())
            .run(view)
            .get(0);

    RuleTypeRegistry ruleTypeRegistryWithOthers = new RuleTypeRegistry();
    ruleTypeRegistryWithOthers.register(countingRuleType());
    ruleTypeRegistryWithOthers.register(new StubRuleType("ruletype.other", 1, MODULE_SCOPE));
    RuleRegistry ruleRegistryWithOthers = new RuleRegistry(ruleTypeRegistryWithOthers);
    ruleRegistryWithOthers.register(new Rule("rule.counter", 1, "ruletype.counter", Map.of()));
    ruleRegistryWithOthers.register(new Rule("rule.other", 1, "ruletype.other", Map.of()));
    List<RuleEvaluationResult> withOthersResults =
        new RuleEvaluationOrchestrator(ruleTypeRegistryWithOthers, ruleRegistryWithOthers, new InMemoryAnalysisResultSource())
            .run(view);
    RuleEvaluationResult sameRuleResult =
        withOthersResults.stream().filter(r -> r.ruleIdentifier().equals("rule.counter")).findFirst().orElseThrow();

    assertEquals(aloneResult.id(), sameRuleResult.id());
    assertEquals(aloneResult.payload(), sameRuleResult.payload());
  }

  @Test
  void repeatedEvaluationOverUnchangedInputIsIdentical() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    builder.module("moduleA");
    AnalysisView view = AnalysisView.from(builder.build());

    RuleTypeRegistry ruleTypeRegistry = new RuleTypeRegistry();
    ruleTypeRegistry.register(countingRuleType());
    RuleRegistry ruleRegistry = new RuleRegistry(ruleTypeRegistry);
    ruleRegistry.register(new Rule("rule.counter", 1, "ruletype.counter", Map.of()));

    RuleEvaluationOrchestrator orchestrator =
        new RuleEvaluationOrchestrator(ruleTypeRegistry, ruleRegistry, new InMemoryAnalysisResultSource());
    List<RuleEvaluationResult> first = orchestrator.run(view);
    List<RuleEvaluationResult> second = orchestrator.run(view);

    assertEquals(first.get(0).id(), second.get(0).id());
    assertEquals(first.get(0).outcome(), second.get(0).outcome());
  }

  @Test
  void everyResultFromOneRunReferencesExactlyOneRepositorysSnapshotIdentity() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    builder.module("moduleA");
    builder.module("moduleB");
    CsmSnapshotSource source = builder.build();
    AnalysisView view = AnalysisView.from(source);

    RuleTypeRegistry ruleTypeRegistry = new RuleTypeRegistry();
    ruleTypeRegistry.register(new StubRuleType("ruletype.per-module", 1, MODULE_SCOPE.anchoredAt(CsmEntityKind.MODULE)));
    RuleRegistry ruleRegistry = new RuleRegistry(ruleTypeRegistry);
    ruleRegistry.register(new Rule("rule.a", 1, "ruletype.per-module", Map.of()));

    List<RuleEvaluationResult> results =
        new RuleEvaluationOrchestrator(ruleTypeRegistry, ruleRegistry, new InMemoryAnalysisResultSource()).run(view);
    assertEquals(2, results.size());
    for (RuleEvaluationResult result : results) {
      assertEquals(source.id(), result.sourceSnapshotId());
    }
  }

  private static StubRuleType countingRuleType() {
    return new StubRuleType(
        "ruletype.counter",
        1,
        Set.of(),
        MODULE_SCOPE,
        ctx -> RuleTypeEvaluation.pass(ctx.view().elementsOfKind(CsmEntityKind.MODULE).size()));
  }
}
