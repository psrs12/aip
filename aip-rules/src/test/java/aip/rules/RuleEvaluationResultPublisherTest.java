package aip.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.core.csm.AnalysisView;
import aip.core.csm.CsmEntityKind;
import aip.core.csm.CsmScope;
import aip.core.csm.CsmScopeInstance;
import aip.core.csm.RuleEvaluationOutcome;
import aip.core.csm.RuleEvaluationResult;
import aip.rules.test.fixtures.CsmSnapshotSourceBuilder;
import aip.rules.test.fixtures.InMemoryAnalysisResultSource;
import aip.rules.test.fixtures.InMemoryRuleEvaluationResultStore;
import aip.rules.test.fixtures.StubRuleType;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * {@link RuleEvaluationResultPublisher} tests, per {@code Valid Result
 * published}/{@code invalid Result never written} — representative
 * cases, complementing {@link RuleEvaluationResultValidatorTest}'s own
 * direct rejection coverage.
 */
class RuleEvaluationResultPublisherTest {

  private static final CsmScope MODULE_SCOPE = CsmScope.ofEntityKinds(Set.of(CsmEntityKind.MODULE));

  @Test
  void validResultIsPublishedAndRetrievable() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    builder.module("moduleA");
    AnalysisView view = AnalysisView.from(builder.build());

    RuleTypeRegistry ruleTypeRegistry = new RuleTypeRegistry();
    ruleTypeRegistry.register(new StubRuleType("ruletype.a", 1, MODULE_SCOPE));
    RuleRegistry ruleRegistry = new RuleRegistry(ruleTypeRegistry);
    ruleRegistry.register(new Rule("rule.a", 1, "ruletype.a", Map.of()));
    InMemoryRuleEvaluationResultStore store = new InMemoryRuleEvaluationResultStore();
    InMemoryAnalysisResultSource analysisResultSource = new InMemoryAnalysisResultSource();

    RuleEvaluationResult result =
        RuleEvaluationResult.of(
            "rule.a", 1, view.sourceSnapshotId(), CsmScopeInstance.wholeRepository(), Set.of(), RuleEvaluationOutcome.PASS, "ok");

    RuleEvaluationResultPublisher.PublicationOutcome outcome =
        RuleEvaluationResultPublisher.publish(store, result, view, analysisResultSource, ruleTypeRegistry, ruleRegistry);

    assertTrue(outcome.published());
    assertEquals(result.id(), store.read(result.id()).orElseThrow().id());
  }

  @Test
  void invalidResultIsNeverWritten() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    builder.module("moduleA");
    AnalysisView view = AnalysisView.from(builder.build());

    RuleTypeRegistry ruleTypeRegistry = new RuleTypeRegistry(); // rule.a is not registered
    RuleRegistry ruleRegistry = new RuleRegistry(ruleTypeRegistry);
    InMemoryRuleEvaluationResultStore store = new InMemoryRuleEvaluationResultStore();
    InMemoryAnalysisResultSource analysisResultSource = new InMemoryAnalysisResultSource();

    RuleEvaluationResult result =
        RuleEvaluationResult.of(
            "rule.a", 1, view.sourceSnapshotId(), CsmScopeInstance.wholeRepository(), Set.of(), RuleEvaluationOutcome.PASS, "bad");

    RuleEvaluationResultPublisher.PublicationOutcome outcome =
        RuleEvaluationResultPublisher.publish(store, result, view, analysisResultSource, ruleTypeRegistry, ruleRegistry);

    assertFalse(outcome.published());
    assertTrue(store.read(result.id()).isEmpty());
  }
}
