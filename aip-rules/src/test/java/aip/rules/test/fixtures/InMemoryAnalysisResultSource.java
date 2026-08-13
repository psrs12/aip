package aip.rules.test.fixtures;

import aip.core.csm.AnalysisResult;
import aip.core.csm.AnalysisResultId;
import aip.core.csm.AnalysisResultSource;
import aip.core.csm.CsmSnapshotId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A test-only, in-memory {@link AnalysisResultSource} — this change's
 * own fixture layer, per {@code implement-rule-framework/design.md}
 * Decision 8 and Binding Decision 2: no real {@code aip-analysis}
 * adapter, fixtures only.
 */
public final class InMemoryAnalysisResultSource implements AnalysisResultSource {

  private final Map<AnalysisResultId, AnalysisResult> byId = new LinkedHashMap<>();

  public InMemoryAnalysisResultSource with(AnalysisResult result) {
    Objects.requireNonNull(result, "result");
    byId.put(result.id(), result);
    return this;
  }

  @Override
  public Optional<AnalysisResult> read(AnalysisResultId id) {
    Objects.requireNonNull(id, "id");
    return Optional.ofNullable(byId.get(id));
  }

  @Override
  public Set<AnalysisResult> list(String analyzerIdentifier, CsmSnapshotId snapshotId) {
    Objects.requireNonNull(analyzerIdentifier, "analyzerIdentifier");
    Objects.requireNonNull(snapshotId, "snapshotId");
    return byId.values().stream()
        .filter(r -> r.analyzerIdentifier().equals(analyzerIdentifier) && r.sourceSnapshotId().equals(snapshotId))
        .collect(Collectors.toUnmodifiableSet());
  }
}
