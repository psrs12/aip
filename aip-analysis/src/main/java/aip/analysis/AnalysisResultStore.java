package aip.analysis;

import aip.core.csm.AnalysisResult;
import aip.core.csm.AnalysisResultId;
import aip.core.csm.CsmSnapshotId;
import java.util.List;
import java.util.Optional;

/**
 * The Analysis Result Store abstraction: writes and retrieves {@link
 * AnalysisResult}s, per {@code Analysis Results Are Durable,
 * Individually Identifiable Artifacts} — "so the persistence mechanism
 * can be replaced later without changing Analysis Framework's own
 * orchestration logic," mirroring {@code aip-csm-builder}'s own {@code
 * SnapshotStore} abstraction (per {@code
 * implement-analysis-framework/design.md} Decision 5,
 * <strong>not</strong> promoted to {@code aip-core} — the artifact
 * moved, the store abstraction did not, the same way {@code
 * SnapshotStore} never moved when {@code CsmElement} was promoted).
 *
 * <p>Concrete persistence technology is deliberately not chosen here —
 * this interface only fixes the shape a future implementation must
 * satisfy.
 */
public interface AnalysisResultStore {

  /** Writes {@code result} as a new, immutable artifact. Never overwrites a previously written result. */
  AnalysisResult write(AnalysisResult result);

  /** The result with the given identity, if one was written. */
  Optional<AnalysisResult> read(AnalysisResultId id);

  /** Every result produced by {@code analyzerIdentifier} against {@code snapshotId}, if any. */
  List<AnalysisResult> listByAnalyzerAndSnapshot(String analyzerIdentifier, CsmSnapshotId snapshotId);
}
