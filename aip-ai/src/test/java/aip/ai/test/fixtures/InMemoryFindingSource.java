package aip.ai.test.fixtures;

import aip.core.csm.CsmSnapshotId;
import aip.core.csm.EvaluationIdentity;
import aip.core.csm.Finding;
import aip.core.csm.FindingSource;
import aip.core.csm.LogicalFindingIdentity;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A test-only, in-memory {@link FindingSource} — this change's own
 * fixture layer, per {@code implement-agent-framework/design.md}
 * Decision 10 and this change's own Binding Decision 4: no real
 * {@code aip-findings} adapter, fixtures only.
 */
public final class InMemoryFindingSource implements FindingSource {

  private final Map<EvaluationIdentity, Finding> byId = new LinkedHashMap<>();

  public InMemoryFindingSource with(Finding finding) {
    Objects.requireNonNull(finding, "finding");
    byId.put(finding.id(), finding);
    return this;
  }

  @Override
  public Optional<Finding> read(EvaluationIdentity id) {
    Objects.requireNonNull(id, "id");
    return Optional.ofNullable(byId.get(id));
  }

  @Override
  public Set<Finding> listByLogicalFindingIdentity(LogicalFindingIdentity id) {
    Objects.requireNonNull(id, "id");
    return byId.values().stream()
        .filter(f -> f.logicalFindingIdentity().equals(id))
        .collect(Collectors.toUnmodifiableSet());
  }

  @Override
  public Set<Finding> listBySourceSnapshotId(CsmSnapshotId snapshotId) {
    Objects.requireNonNull(snapshotId, "snapshotId");
    return byId.values().stream()
        .filter(f -> f.sourceSnapshotId().equals(snapshotId))
        .collect(Collectors.toUnmodifiableSet());
  }
}
