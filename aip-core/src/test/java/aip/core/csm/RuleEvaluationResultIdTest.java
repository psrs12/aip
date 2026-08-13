package aip.core.csm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * {@link RuleEvaluationResultId} tests, per {@code Deterministic Rule
 * Evaluation Result Identity}.
 */
class RuleEvaluationResultIdTest {

  private static final CsmSnapshotId SNAPSHOT = new CsmSnapshotId("repo", 1);
  private static final CsmScopeInstance SCOPE_INSTANCE = CsmScopeInstance.wholeRepository();
  private static final AnalysisResultId AR1 = AnalysisResultId.of("analyzer.a", 1, SNAPSHOT, SCOPE_INSTANCE);
  private static final AnalysisResultId AR2 =
      AnalysisResultId.of("analyzer.b", 1, SNAPSHOT, CsmScopeInstance.anchoredAt(new CsmElementId("m1")));

  @Test
  void identicalInputsProduceIdenticalIdentity() {
    RuleEvaluationResultId first =
        RuleEvaluationResultId.of("rule.a", 1, SNAPSHOT, SCOPE_INSTANCE, Set.of(AR1));
    RuleEvaluationResultId second =
        RuleEvaluationResultId.of("rule.a", 1, SNAPSHOT, SCOPE_INSTANCE, Set.of(AR1));
    assertEquals(first, second);
  }

  @Test
  void differentConsumedAnalysisResultsYieldDistinguishableIdentity() {
    RuleEvaluationResultId withOne =
        RuleEvaluationResultId.of("rule.a", 1, SNAPSHOT, SCOPE_INSTANCE, Set.of(AR1));
    RuleEvaluationResultId withBoth =
        RuleEvaluationResultId.of("rule.a", 1, SNAPSHOT, SCOPE_INSTANCE, Set.of(AR1, AR2));
    assertNotEquals(withOne, withBoth);
  }

  @Test
  void differentRuleVersionYieldsDistinguishableIdentity() {
    RuleEvaluationResultId v1 = RuleEvaluationResultId.of("rule.a", 1, SNAPSHOT, SCOPE_INSTANCE, Set.of(AR1));
    RuleEvaluationResultId v2 = RuleEvaluationResultId.of("rule.a", 2, SNAPSHOT, SCOPE_INSTANCE, Set.of(AR1));
    assertNotEquals(v1, v2);
  }

  @Test
  void differentRuleIdentifierYieldsDistinguishableIdentity() {
    RuleEvaluationResultId a = RuleEvaluationResultId.of("rule.a", 1, SNAPSHOT, SCOPE_INSTANCE, Set.of(AR1));
    RuleEvaluationResultId b = RuleEvaluationResultId.of("rule.b", 1, SNAPSHOT, SCOPE_INSTANCE, Set.of(AR1));
    assertNotEquals(a, b);
  }

  @Test
  void differentSourceSnapshotYieldsDistinguishableIdentity() {
    RuleEvaluationResultId first = RuleEvaluationResultId.of("rule.a", 1, SNAPSHOT, SCOPE_INSTANCE, Set.of(AR1));
    RuleEvaluationResultId second =
        RuleEvaluationResultId.of("rule.a", 1, new CsmSnapshotId("repo", 2), SCOPE_INSTANCE, Set.of(AR1));
    assertNotEquals(first, second);
  }

  @Test
  void emptyConsumedSetIsPermittedAndDeterministic() {
    RuleEvaluationResultId first = RuleEvaluationResultId.of("rule.a", 1, SNAPSHOT, SCOPE_INSTANCE, Set.of());
    RuleEvaluationResultId second = RuleEvaluationResultId.of("rule.a", 1, SNAPSHOT, SCOPE_INSTANCE, Set.of());
    assertEquals(first, second);
  }

  @Test
  void consumedSetOrderDoesNotAffectIdentity() {
    // Set.of's own iteration order is unspecified across JVM runs;
    // confirm two logically-equal-but-differently-constructed sets
    // still produce the same identity.
    RuleEvaluationResultId first = RuleEvaluationResultId.of("rule.a", 1, SNAPSHOT, SCOPE_INSTANCE, Set.of(AR1, AR2));
    RuleEvaluationResultId second = RuleEvaluationResultId.of("rule.a", 1, SNAPSHOT, SCOPE_INSTANCE, Set.of(AR2, AR1));
    assertEquals(first, second);
  }
}
