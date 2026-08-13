package aip.core.csm;

import java.util.Objects;

/**
 * An {@link AnalysisResult}'s deterministic identity, per {@code
 * Deterministic Analysis Result Identity}: "a deterministic function
 * of: the producing Analyzer's identifier, the Analyzer's version, the
 * source CSM Snapshot's identity, and the specific Analysis Scope
 * instance covered. Random or otherwise non-reproducible identity
 * generation SHALL NOT be used."
 *
 * @param value the opaque, deterministic digest string. Never blank.
 */
public record AnalysisResultId(String value) {

  public AnalysisResultId {
    Objects.requireNonNull(value, "value");
    if (value.isBlank()) {
      throw new IllegalArgumentException("AnalysisResultId value must not be blank");
    }
  }

  /**
   * Computes an {@code AnalysisResultId} as a deterministic function of
   * every component this identity depends on. Identical arguments
   * always produce an equal {@code AnalysisResultId}; a differing
   * {@code analyzerVersion} (with everything else unchanged) always
   * produces a distinguishable one, per {@code A different Analyzer
   * version yields a distinguishable identity}.
   */
  public static AnalysisResultId of(
      String analyzerIdentifier,
      int analyzerVersion,
      CsmSnapshotId sourceSnapshotId,
      CsmScopeInstance scopeInstance) {
    Objects.requireNonNull(analyzerIdentifier, "analyzerIdentifier");
    Objects.requireNonNull(sourceSnapshotId, "sourceSnapshotId");
    Objects.requireNonNull(scopeInstance, "scopeInstance");
    return new AnalysisResultId(
        DeterministicHash.of(analyzerIdentifier, analyzerVersion, sourceSnapshotId, scopeInstance));
  }

  @Override
  public String toString() {
    return value;
  }
}
