package aip.core.csm;

import java.util.Objects;
import java.util.Set;

/**
 * The durable output of evaluating one Rule against one Rule Scope
 * instance, per {@code Rule Type Contract} and {@code Rule Evaluation
 * Outcome Semantics}.
 *
 * <p>A canonical {@code aip-core} type, per {@code
 * implement-rule-framework/design.md} Decision 1 — mirroring {@link
 * AnalysisResult}'s own placement field-for-field where the underlying
 * concept matches, extended with the complete consumed {@link
 * AnalysisResultId} set ({@code define-rule-framework/design.md}
 * Decision 4) and a {@link RuleEvaluationOutcome}. Final, not sealed:
 * one artifact kind, with Rule-Type-defined variation only in its
 * {@link #payload()}.
 *
 * <p>The source CSM Snapshot identity is obtained from {@link
 * AnalysisView} during evaluation, never from a direct {@link
 * CsmSnapshotSource} dependency — {@code aip-rules} has no reachable
 * dependency on {@code aip-csm-builder} or {@link CsmSnapshotSource}
 * at all (per {@code define-rule-framework/design.md} Decision 4).
 */
public final class RuleEvaluationResult {

  private final RuleEvaluationResultId id;
  private final String ruleIdentifier;
  private final int ruleVersion;
  private final CsmSnapshotId sourceSnapshotId;
  private final CsmScopeInstance scopeInstance;
  private final Set<AnalysisResultId> consumedAnalysisResultIds;
  private final RuleEvaluationOutcome outcome;
  private final Object payload;

  public RuleEvaluationResult(
      RuleEvaluationResultId id,
      String ruleIdentifier,
      int ruleVersion,
      CsmSnapshotId sourceSnapshotId,
      CsmScopeInstance scopeInstance,
      Set<AnalysisResultId> consumedAnalysisResultIds,
      RuleEvaluationOutcome outcome,
      Object payload) {
    this.id = Objects.requireNonNull(id, "id");
    this.ruleIdentifier = Objects.requireNonNull(ruleIdentifier, "ruleIdentifier");
    if (ruleIdentifier.isBlank()) {
      throw new IllegalArgumentException("ruleIdentifier must not be blank");
    }
    this.ruleVersion = ruleVersion;
    this.sourceSnapshotId = Objects.requireNonNull(sourceSnapshotId, "sourceSnapshotId");
    this.scopeInstance = Objects.requireNonNull(scopeInstance, "scopeInstance");
    Objects.requireNonNull(consumedAnalysisResultIds, "consumedAnalysisResultIds");
    this.consumedAnalysisResultIds = Set.copyOf(consumedAnalysisResultIds);
    this.outcome = Objects.requireNonNull(outcome, "outcome");
    this.payload = Objects.requireNonNull(payload, "payload");
  }

  /**
   * Constructs a {@code RuleEvaluationResult}, computing its {@link
   * RuleEvaluationResultId} from the same five traceability components
   * (per {@code Deterministic Rule Evaluation Result Identity}) rather
   * than requiring a caller to compute it separately and risk the two
   * drifting apart.
   */
  public static RuleEvaluationResult of(
      String ruleIdentifier,
      int ruleVersion,
      CsmSnapshotId sourceSnapshotId,
      CsmScopeInstance scopeInstance,
      Set<AnalysisResultId> consumedAnalysisResultIds,
      RuleEvaluationOutcome outcome,
      Object payload) {
    RuleEvaluationResultId id =
        RuleEvaluationResultId.of(ruleIdentifier, ruleVersion, sourceSnapshotId, scopeInstance, consumedAnalysisResultIds);
    return new RuleEvaluationResult(
        id, ruleIdentifier, ruleVersion, sourceSnapshotId, scopeInstance, consumedAnalysisResultIds, outcome, payload);
  }

  public RuleEvaluationResultId id() {
    return id;
  }

  public String ruleIdentifier() {
    return ruleIdentifier;
  }

  public int ruleVersion() {
    return ruleVersion;
  }

  public CsmSnapshotId sourceSnapshotId() {
    return sourceSnapshotId;
  }

  public CsmScopeInstance scopeInstance() {
    return scopeInstance;
  }

  /** The complete set of Analysis Result identities this evaluation consumed. Never empty is not guaranteed — a Rule Type MAY declare no Analyzer inputs. */
  public Set<AnalysisResultId> consumedAnalysisResultIds() {
    return consumedAnalysisResultIds;
  }

  public RuleEvaluationOutcome outcome() {
    return outcome;
  }

  /** The opaque, Rule-Type-defined diagnostic content — never constrained or interpreted by the framework. */
  public Object payload() {
    return payload;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof RuleEvaluationResult other)) {
      return false;
    }
    return id.equals(other.id)
        && ruleIdentifier.equals(other.ruleIdentifier)
        && ruleVersion == other.ruleVersion
        && sourceSnapshotId.equals(other.sourceSnapshotId)
        && scopeInstance.equals(other.scopeInstance)
        && consumedAnalysisResultIds.equals(other.consumedAnalysisResultIds)
        && outcome == other.outcome
        && payload.equals(other.payload);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        id, ruleIdentifier, ruleVersion, sourceSnapshotId, scopeInstance, consumedAnalysisResultIds, outcome, payload);
  }

  @Override
  public String toString() {
    return "RuleEvaluationResult[id="
        + id
        + ", ruleIdentifier="
        + ruleIdentifier
        + ", ruleVersion="
        + ruleVersion
        + ", sourceSnapshotId="
        + sourceSnapshotId
        + ", scopeInstance="
        + scopeInstance
        + ", outcome="
        + outcome
        + "]";
  }
}
