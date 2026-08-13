package aip.core.csm;

import java.util.Objects;

/**
 * The durable output of one Analyzer invocation against one Analysis
 * Scope instance, per {@code Analyzer Contract}: "Given an Analysis
 * View and an Analysis Scope instance it is applicable to, an Analyzer
 * SHALL deterministically produce exactly one Analysis Result for that
 * instance."
 *
 * <p>A canonical {@code aip-core} type, per {@code
 * implement-analysis-framework/design.md} Decision 1 — one logical
 * artifact, not split into separate identity/traceability/payload
 * types — mirroring {@link CsmElement}'s own placement: many
 * producers (Analyzers), at least one already-named future consumer
 * ({@code AnalysisResultSource}, Rule Framework's own {@code aip-core}
 * contribution). Final, not sealed: there is no closed set of "kinds"
 * the way {@link CsmElement} has eight; this is one artifact kind with
 * producer-defined variation only in its {@link #payload()}.
 *
 * <p>The payload is deliberately typed {@code Object}, not generic —
 * see {@code implement-analysis-framework/design.md} Decision 1,
 * Alternatives Considered. The framework never constrains, interprets,
 * or validates it beyond the traceability/referential-integrity
 * expectations {@link AnalysisResultValidator} (in {@code
 * aip-analysis}) checks, per {@code Analysis Result Payload Is
 * Analyzer-Defined}.
 */
public final class AnalysisResult {

  private final AnalysisResultId id;
  private final String analyzerIdentifier;
  private final int analyzerVersion;
  private final CsmSnapshotId sourceSnapshotId;
  private final CsmScopeInstance scopeInstance;
  private final Object payload;

  public AnalysisResult(
      AnalysisResultId id,
      String analyzerIdentifier,
      int analyzerVersion,
      CsmSnapshotId sourceSnapshotId,
      CsmScopeInstance scopeInstance,
      Object payload) {
    this.id = Objects.requireNonNull(id, "id");
    this.analyzerIdentifier = Objects.requireNonNull(analyzerIdentifier, "analyzerIdentifier");
    if (analyzerIdentifier.isBlank()) {
      throw new IllegalArgumentException("analyzerIdentifier must not be blank");
    }
    this.analyzerVersion = analyzerVersion;
    this.sourceSnapshotId = Objects.requireNonNull(sourceSnapshotId, "sourceSnapshotId");
    this.scopeInstance = Objects.requireNonNull(scopeInstance, "scopeInstance");
    this.payload = Objects.requireNonNull(payload, "payload");
  }

  /**
   * Constructs an {@code AnalysisResult}, computing its {@link
   * AnalysisResultId} from the same four traceability components
   * (per {@code Deterministic Analysis Result Identity}) rather than
   * requiring a caller to compute it separately and risk the two
   * drifting apart.
   */
  public static AnalysisResult of(
      String analyzerIdentifier,
      int analyzerVersion,
      CsmSnapshotId sourceSnapshotId,
      CsmScopeInstance scopeInstance,
      Object payload) {
    AnalysisResultId id =
        AnalysisResultId.of(analyzerIdentifier, analyzerVersion, sourceSnapshotId, scopeInstance);
    return new AnalysisResult(id, analyzerIdentifier, analyzerVersion, sourceSnapshotId, scopeInstance, payload);
  }

  public AnalysisResultId id() {
    return id;
  }

  public String analyzerIdentifier() {
    return analyzerIdentifier;
  }

  public int analyzerVersion() {
    return analyzerVersion;
  }

  public CsmSnapshotId sourceSnapshotId() {
    return sourceSnapshotId;
  }

  public CsmScopeInstance scopeInstance() {
    return scopeInstance;
  }

  /** The opaque, Analyzer-defined analytical content — never constrained or interpreted by the framework. */
  public Object payload() {
    return payload;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof AnalysisResult other)) {
      return false;
    }
    return id.equals(other.id)
        && analyzerIdentifier.equals(other.analyzerIdentifier)
        && analyzerVersion == other.analyzerVersion
        && sourceSnapshotId.equals(other.sourceSnapshotId)
        && scopeInstance.equals(other.scopeInstance)
        && payload.equals(other.payload);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, analyzerIdentifier, analyzerVersion, sourceSnapshotId, scopeInstance, payload);
  }

  @Override
  public String toString() {
    return "AnalysisResult[id="
        + id
        + ", analyzerIdentifier="
        + analyzerIdentifier
        + ", analyzerVersion="
        + analyzerVersion
        + ", sourceSnapshotId="
        + sourceSnapshotId
        + ", scopeInstance="
        + scopeInstance
        + "]";
  }
}
