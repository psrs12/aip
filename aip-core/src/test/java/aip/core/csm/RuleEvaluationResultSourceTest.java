package aip.core.csm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * {@link RuleEvaluationResultSource} contract tests, per {@code Rule
 * Evaluation Result Source Shape}.
 */
class RuleEvaluationResultSourceTest {

  private static final CsmSnapshotId SNAPSHOT = new CsmSnapshotId("repo", 1);
  private static final CsmScopeInstance SCOPE_INSTANCE = CsmScopeInstance.wholeRepository();

  @Test
  void exposesRetrievalByIdentityAndByRuleAndSnapshot() {
    RuleEvaluationResult result =
        RuleEvaluationResult.of("rule.a", 1, SNAPSHOT, SCOPE_INSTANCE, Set.of(), RuleEvaluationOutcome.FAIL, "payload");
    RuleEvaluationResultSource source = fixtureSource(Set.of(result));

    assertEquals(result, source.read(result.id()).orElseThrow());
    assertEquals(Set.of(result), source.list("rule.a", SNAPSHOT));
    assertTrue(source.list("rule.unknown", SNAPSHOT).isEmpty());
  }

  @Test
  void agnosticToRuleEvaluationResultProduction() {
    RuleEvaluationResult result =
        RuleEvaluationResult.of("rule.a", 1, SNAPSHOT, SCOPE_INSTANCE, Set.of(), RuleEvaluationOutcome.PASS, "ok");
    RuleEvaluationResultSource first = fixtureSource(Set.of(result));
    RuleEvaluationResultSource second = fixtureSource(Set.of(result));

    assertEquals(first.read(result.id()), second.read(result.id()));
    assertEquals(first.list("rule.a", SNAPSHOT), second.list("rule.a", SNAPSHOT));
  }

  private static RuleEvaluationResultSource fixtureSource(Set<RuleEvaluationResult> results) {
    Map<RuleEvaluationResultId, RuleEvaluationResult> byId = new LinkedHashMap<>();
    for (RuleEvaluationResult result : results) {
      byId.put(result.id(), result);
    }
    return new RuleEvaluationResultSource() {
      @Override
      public Optional<RuleEvaluationResult> read(RuleEvaluationResultId id) {
        return Optional.ofNullable(byId.get(id));
      }

      @Override
      public Set<RuleEvaluationResult> list(String ruleIdentifier, CsmSnapshotId snapshotId) {
        return byId.values().stream()
            .filter(r -> r.ruleIdentifier().equals(ruleIdentifier) && r.sourceSnapshotId().equals(snapshotId))
            .collect(Collectors.toUnmodifiableSet());
      }
    };
  }
}
