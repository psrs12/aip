package aip.core.csm;

import java.util.Optional;
import java.util.Set;

/**
 * A read-only view over published {@link AnalysisResult}s, per {@code
 * Analysis Result Source Shape}: "An Analysis Result Source SHALL
 * expose, at minimum: an Analysis Result retrievable by its own
 * identity, and the set of Analysis Results produced by a given
 * Analyzer against a given CSM Snapshot identity."
 *
 * <p>Rule Framework's own {@code aip-core} contribution
 * (`define-rule-framework/design.md` Decision 7), not Analysis
 * Framework's — mirroring {@link CsmSnapshotSource}'s own shape.
 * Since {@link AnalysisResult} already lives in {@code aip-core},
 * this contract's return types require no import from {@code
 * aip-analysis} at all; {@code aip-rules} depends on this interface
 * only, never on {@code aip-analysis}'s own {@code
 * AnalysisResultStore}.
 */
public interface AnalysisResultSource {

  /** The Analysis Result with the given identity, if one has been published. */
  Optional<AnalysisResult> read(AnalysisResultId id);

  /** Every Analysis Result {@code analyzerIdentifier} produced against {@code snapshotId}, if any. */
  Set<AnalysisResult> list(String analyzerIdentifier, CsmSnapshotId snapshotId);
}
