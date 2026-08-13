package aip.analysis.test.fixtures;

import aip.analysis.AnalysisResultStore;
import aip.core.csm.AnalysisResult;
import aip.core.csm.AnalysisResultId;
import aip.core.csm.CsmSnapshotId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A test-only, in-memory {@link AnalysisResultStore} implementation —
 * concrete persistence technology is deliberately deferred (per {@code
 * define-analysis-framework/design.md} Decision 4, Non-Goals), so
 * production code never chooses one; this exists only so this module's
 * own tests can exercise the {@link AnalysisResultStore} abstraction
 * end to end.
 */
public final class InMemoryAnalysisResultStore implements AnalysisResultStore {

  private final Map<AnalysisResultId, AnalysisResult> byId = new LinkedHashMap<>();

  @Override
  public AnalysisResult write(AnalysisResult result) {
    Objects.requireNonNull(result, "result");
    if (byId.containsKey(result.id())) {
      throw new IllegalStateException(
          "an Analysis Result is already stored for identity " + result.id() + "; a store SHALL NOT"
              + " overwrite a previously written result");
    }
    byId.put(result.id(), result);
    return result;
  }

  @Override
  public Optional<AnalysisResult> read(AnalysisResultId id) {
    Objects.requireNonNull(id, "id");
    return Optional.ofNullable(byId.get(id));
  }

  @Override
  public List<AnalysisResult> listByAnalyzerAndSnapshot(String analyzerIdentifier, CsmSnapshotId snapshotId) {
    Objects.requireNonNull(analyzerIdentifier, "analyzerIdentifier");
    Objects.requireNonNull(snapshotId, "snapshotId");
    List<AnalysisResult> result = new ArrayList<>();
    for (AnalysisResult candidate : byId.values()) {
      if (candidate.analyzerIdentifier().equals(analyzerIdentifier) && candidate.sourceSnapshotId().equals(snapshotId)) {
        result.add(candidate);
      }
    }
    return result;
  }
}
