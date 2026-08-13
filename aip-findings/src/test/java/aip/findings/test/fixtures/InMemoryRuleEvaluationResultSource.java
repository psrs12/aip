package aip.findings.test.fixtures;

import aip.core.csm.CsmSnapshotId;
import aip.core.csm.RuleEvaluationResult;
import aip.core.csm.RuleEvaluationResultId;
import aip.core.csm.RuleEvaluationResultSource;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A test-only, in-memory {@link RuleEvaluationResultSource} — this
 * change's own fixture layer, per {@code
 * implement-finding-model/design.md} Decision 10 and its own Binding
 * Decision 5: no real {@code aip-rules} adapter, fixtures only.
 */
public final class InMemoryRuleEvaluationResultSource implements RuleEvaluationResultSource {

  private final Map<RuleEvaluationResultId, RuleEvaluationResult> byId = new LinkedHashMap<>();

  public InMemoryRuleEvaluationResultSource with(RuleEvaluationResult result) {
    Objects.requireNonNull(result, "result");
    byId.put(result.id(), result);
    return this;
  }

  @Override
  public Optional<RuleEvaluationResult> read(RuleEvaluationResultId id) {
    Objects.requireNonNull(id, "id");
    return Optional.ofNullable(byId.get(id));
  }

  @Override
  public Set<RuleEvaluationResult> list(String ruleIdentifier, CsmSnapshotId snapshotId) {
    Objects.requireNonNull(ruleIdentifier, "ruleIdentifier");
    Objects.requireNonNull(snapshotId, "snapshotId");
    return byId.values().stream()
        .filter(r -> r.ruleIdentifier().equals(ruleIdentifier) && r.sourceSnapshotId().equals(snapshotId))
        .collect(Collectors.toUnmodifiableSet());
  }
}
