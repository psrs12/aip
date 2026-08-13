package aip.core.csm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * {@link EvaluationIdentity} tests, per {@code Evaluation Identity}.
 */
class EvaluationIdentityTest {

  private static final CsmSnapshotId SNAPSHOT = new CsmSnapshotId("repo", 1);
  private static final CsmScopeInstance SCOPE_INSTANCE = CsmScopeInstance.wholeRepository();
  private static final RuleEvaluationResultId RER1 =
      RuleEvaluationResultId.of("rule.a", 1, SNAPSHOT, SCOPE_INSTANCE, Set.of());
  private static final RuleEvaluationResultId RER2 =
      RuleEvaluationResultId.of("rule.b", 1, SNAPSHOT, SCOPE_INSTANCE, Set.of());

  @Test
  void identicalReferencedSetsProduceIdenticalIdentity() {
    EvaluationIdentity first = EvaluationIdentity.of(Set.of(RER1));
    EvaluationIdentity second = EvaluationIdentity.of(Set.of(RER1));
    assertEquals(first, second);
  }

  @Test
  void differentReferencedSetsProduceDistinguishableIdentity() {
    EvaluationIdentity withOne = EvaluationIdentity.of(Set.of(RER1));
    EvaluationIdentity withBoth = EvaluationIdentity.of(Set.of(RER1, RER2));
    assertNotEquals(withOne, withBoth);
  }

  @Test
  void referencedSetOrderDoesNotAffectIdentity() {
    EvaluationIdentity first = EvaluationIdentity.of(Set.of(RER1, RER2));
    EvaluationIdentity second = EvaluationIdentity.of(Set.of(RER2, RER1));
    assertEquals(first, second);
  }

  @Test
  void changesWheneverTheUnderlyingEvaluationRunChanges() {
    // Same rule/version, differing only in source snapshot (a
    // different evaluation run) -> a different RuleEvaluationResultId
    // -> a different EvaluationIdentity, even though "the concerned
    // element" would be unchanged in both runs.
    RuleEvaluationResultId runOne =
        RuleEvaluationResultId.of("rule.a", 1, SNAPSHOT, SCOPE_INSTANCE, Set.of());
    RuleEvaluationResultId runTwo =
        RuleEvaluationResultId.of("rule.a", 1, new CsmSnapshotId("repo", 2), SCOPE_INSTANCE, Set.of());
    assertNotEquals(EvaluationIdentity.of(Set.of(runOne)), EvaluationIdentity.of(Set.of(runTwo)));
  }
}
