package aip.core.csm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * {@link RuleEvaluationResult} tests, per {@code Rule Type Contract},
 * {@code Rule Evaluation Result Traceability}, and {@code Rule
 * Evaluation Result Payload Is Rule-Type-Defined}.
 */
class RuleEvaluationResultTest {

  private static final CsmSnapshotId SNAPSHOT = new CsmSnapshotId("repo", 1);
  private static final CsmScopeInstance SCOPE_INSTANCE = CsmScopeInstance.wholeRepository();
  private static final AnalysisResultId AR1 = AnalysisResultId.of("analyzer.a", 1, SNAPSHOT, SCOPE_INSTANCE);

  @Test
  void ofComputesIdentityFromTheSameTraceabilityComponents() {
    RuleEvaluationResult result =
        RuleEvaluationResult.of("rule.a", 1, SNAPSHOT, SCOPE_INSTANCE, Set.of(AR1), RuleEvaluationOutcome.PASS, "ok");
    assertEquals(
        RuleEvaluationResultId.of("rule.a", 1, SNAPSHOT, SCOPE_INSTANCE, Set.of(AR1)), result.id());
  }

  @Test
  void retainsTraceabilityToProducingRuleSourceSnapshotAndConsumedAnalysisResults() {
    RuleEvaluationResult result =
        RuleEvaluationResult.of(
            "rule.a", 2, SNAPSHOT, SCOPE_INSTANCE, Set.of(AR1), RuleEvaluationOutcome.FAIL, "violation");
    assertEquals("rule.a", result.ruleIdentifier());
    assertEquals(2, result.ruleVersion());
    assertEquals(SNAPSHOT, result.sourceSnapshotId());
    assertEquals(SCOPE_INSTANCE, result.scopeInstance());
    assertEquals(Set.of(AR1), result.consumedAnalysisResultIds());
  }

  @Test
  void outcomeAndPayloadAreRetainedUnmodified() {
    RuleEvaluationResult result =
        RuleEvaluationResult.of(
            "rule.a", 1, SNAPSHOT, SCOPE_INSTANCE, Set.of(), RuleEvaluationOutcome.NOT_APPLICABLE, "no data");
    assertEquals(RuleEvaluationOutcome.NOT_APPLICABLE, result.outcome());
    assertEquals("no data", result.payload());
  }

  @Test
  void differentlyShapedPayloadsAreBothAccepted() {
    RuleEvaluationResult stringPayload =
        RuleEvaluationResult.of("rule.a", 1, SNAPSHOT, SCOPE_INSTANCE, Set.of(), RuleEvaluationOutcome.PASS, "text");
    RuleEvaluationResult listPayload =
        RuleEvaluationResult.of(
            "rule.b", 1, SNAPSHOT, SCOPE_INSTANCE, Set.of(), RuleEvaluationOutcome.FAIL, java.util.List.of("a", "b"));
    assertEquals("text", stringPayload.payload());
    assertEquals(java.util.List.of("a", "b"), listPayload.payload());
  }

  @Test
  void emptyConsumedAnalysisResultSetIsPermitted() {
    RuleEvaluationResult result =
        RuleEvaluationResult.of("rule.a", 1, SNAPSHOT, SCOPE_INSTANCE, Set.of(), RuleEvaluationOutcome.PASS, "ok");
    assertEquals(Set.of(), result.consumedAnalysisResultIds());
  }

  @Test
  void rejectsNullOutcome() {
    assertThrows(
        NullPointerException.class,
        () ->
            new RuleEvaluationResult(
                RuleEvaluationResultId.of("rule.a", 1, SNAPSHOT, SCOPE_INSTANCE, Set.of()),
                "rule.a",
                1,
                SNAPSHOT,
                SCOPE_INSTANCE,
                Set.of(),
                null,
                "payload"));
  }

  @Test
  void rejectsBlankRuleIdentifier() {
    assertThrows(
        IllegalArgumentException.class,
        () -> RuleEvaluationResult.of(" ", 1, SNAPSHOT, SCOPE_INSTANCE, Set.of(), RuleEvaluationOutcome.PASS, "ok"));
  }
}
