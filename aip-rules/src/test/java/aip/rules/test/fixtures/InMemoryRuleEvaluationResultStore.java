package aip.rules.test.fixtures;

import aip.core.csm.CsmSnapshotId;
import aip.core.csm.RuleEvaluationResult;
import aip.core.csm.RuleEvaluationResultId;
import aip.rules.RuleEvaluationResultStore;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A test-only, in-memory {@link RuleEvaluationResultStore}
 * implementation — concrete persistence technology is deliberately
 * deferred, so production code never chooses one; this exists only so
 * this module's own tests can exercise the {@link
 * RuleEvaluationResultStore} abstraction end to end.
 */
public final class InMemoryRuleEvaluationResultStore implements RuleEvaluationResultStore {

  private final Map<RuleEvaluationResultId, RuleEvaluationResult> byId = new LinkedHashMap<>();

  @Override
  public RuleEvaluationResult write(RuleEvaluationResult result) {
    Objects.requireNonNull(result, "result");
    if (byId.containsKey(result.id())) {
      throw new IllegalStateException(
          "a Rule Evaluation Result is already stored for identity " + result.id() + "; a store SHALL"
              + " NOT overwrite a previously written result");
    }
    byId.put(result.id(), result);
    return result;
  }

  @Override
  public Optional<RuleEvaluationResult> read(RuleEvaluationResultId id) {
    Objects.requireNonNull(id, "id");
    return Optional.ofNullable(byId.get(id));
  }

  @Override
  public List<RuleEvaluationResult> listByRuleAndSnapshot(String ruleIdentifier, CsmSnapshotId snapshotId) {
    Objects.requireNonNull(ruleIdentifier, "ruleIdentifier");
    Objects.requireNonNull(snapshotId, "snapshotId");
    List<RuleEvaluationResult> result = new ArrayList<>();
    for (RuleEvaluationResult candidate : byId.values()) {
      if (candidate.ruleIdentifier().equals(ruleIdentifier) && candidate.sourceSnapshotId().equals(snapshotId)) {
        result.add(candidate);
      }
    }
    return result;
  }
}
