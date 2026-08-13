package aip.core.csm;

import java.util.Objects;
import java.util.Set;

/**
 * The durable, consumer-facing interpretation of one or more {@link
 * RuleEvaluationResult}s — a distinct, pre-Recommendation artifact,
 * per {@code Finding Shape} and {@code Finding Is a Distinct Artifact
 * From RuleEvaluationResult}.
 *
 * <p>A canonical {@code aip-core} type, per {@code
 * implement-finding-model/design.md} Decision 1 — mirroring {@link
 * RuleEvaluationResult}'s own placement. Carries two independently-
 * computed identities ({@link #id()}, the Evaluation Identity; {@link
 * #logicalFindingIdentity()}) rather than one, per {@code
 * define-finding-model/design.md} Decision 3.
 *
 * <p>No {@code Recommendation} or {@code Remediation availability}
 * field exists — deliberately left absent, not defaulted to an empty
 * placeholder value, per {@code define-finding-model/design.md}
 * Decision 5.
 */
public final class Finding {

  private final EvaluationIdentity id;
  private final LogicalFindingIdentity logicalFindingIdentity;
  private final String ruleIdentifier;
  private final CsmSnapshotId sourceSnapshotId;
  private final CsmElementId concernedElementId;
  private final Set<RuleEvaluationResultId> referencedRuleEvaluationResultIds;
  private final String category;
  private final String severity;
  private final Confidence confidence;
  private final String description;
  private final String impact;

  public Finding(
      EvaluationIdentity id,
      LogicalFindingIdentity logicalFindingIdentity,
      String ruleIdentifier,
      CsmSnapshotId sourceSnapshotId,
      CsmElementId concernedElementId,
      Set<RuleEvaluationResultId> referencedRuleEvaluationResultIds,
      String category,
      String severity,
      Confidence confidence,
      String description,
      String impact) {
    this.id = Objects.requireNonNull(id, "id");
    this.logicalFindingIdentity = Objects.requireNonNull(logicalFindingIdentity, "logicalFindingIdentity");
    this.ruleIdentifier = Objects.requireNonNull(ruleIdentifier, "ruleIdentifier");
    if (ruleIdentifier.isBlank()) {
      throw new IllegalArgumentException("ruleIdentifier must not be blank");
    }
    this.sourceSnapshotId = Objects.requireNonNull(sourceSnapshotId, "sourceSnapshotId");
    this.concernedElementId = Objects.requireNonNull(concernedElementId, "concernedElementId");
    Objects.requireNonNull(referencedRuleEvaluationResultIds, "referencedRuleEvaluationResultIds");
    if (referencedRuleEvaluationResultIds.isEmpty()) {
      throw new IllegalArgumentException("referencedRuleEvaluationResultIds must not be empty");
    }
    this.referencedRuleEvaluationResultIds = Set.copyOf(referencedRuleEvaluationResultIds);
    this.category = Objects.requireNonNull(category, "category");
    this.severity = Objects.requireNonNull(severity, "severity");
    this.confidence = Objects.requireNonNull(confidence, "confidence");
    this.description = Objects.requireNonNull(description, "description");
    this.impact = Objects.requireNonNull(impact, "impact");
  }

  /**
   * Constructs a {@code Finding}, computing its {@link
   * EvaluationIdentity} and {@link LogicalFindingIdentity} from the
   * same components (per {@code Deterministic, Declarative Finding
   * Construction Only}) rather than requiring a caller to compute
   * them separately and risk drift.
   */
  public static Finding of(
      String ruleIdentifier,
      CsmSnapshotId sourceSnapshotId,
      CsmElementId concernedElementId,
      Set<RuleEvaluationResultId> referencedRuleEvaluationResultIds,
      String category,
      String severity,
      Confidence confidence,
      String description,
      String impact) {
    EvaluationIdentity id = EvaluationIdentity.of(referencedRuleEvaluationResultIds);
    LogicalFindingIdentity logicalFindingIdentity = LogicalFindingIdentity.of(ruleIdentifier, concernedElementId);
    return new Finding(
        id,
        logicalFindingIdentity,
        ruleIdentifier,
        sourceSnapshotId,
        concernedElementId,
        referencedRuleEvaluationResultIds,
        category,
        severity,
        confidence,
        description,
        impact);
  }

  /** The Finding ID — this Finding's Evaluation Identity. */
  public EvaluationIdentity id() {
    return id;
  }

  public LogicalFindingIdentity logicalFindingIdentity() {
    return logicalFindingIdentity;
  }

  public String ruleIdentifier() {
    return ruleIdentifier;
  }

  public CsmSnapshotId sourceSnapshotId() {
    return sourceSnapshotId;
  }

  /** The concerned CSM element identity — this Finding's Location. */
  public CsmElementId concernedElementId() {
    return concernedElementId;
  }

  /** The complete, non-empty set of Rule Evaluation Result identities this Finding references. */
  public Set<RuleEvaluationResultId> referencedRuleEvaluationResultIds() {
    return referencedRuleEvaluationResultIds;
  }

  public String category() {
    return category;
  }

  public String severity() {
    return severity;
  }

  /** Fixed, per {@code define-finding-model/design.md} Decision 5, to {@link Confidence#HIGH} in v1. */
  public Confidence confidence() {
    return confidence;
  }

  public String description() {
    return description;
  }

  public String impact() {
    return impact;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Finding other)) {
      return false;
    }
    return id.equals(other.id)
        && logicalFindingIdentity.equals(other.logicalFindingIdentity)
        && ruleIdentifier.equals(other.ruleIdentifier)
        && sourceSnapshotId.equals(other.sourceSnapshotId)
        && concernedElementId.equals(other.concernedElementId)
        && referencedRuleEvaluationResultIds.equals(other.referencedRuleEvaluationResultIds)
        && category.equals(other.category)
        && severity.equals(other.severity)
        && confidence == other.confidence
        && description.equals(other.description)
        && impact.equals(other.impact);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        id,
        logicalFindingIdentity,
        ruleIdentifier,
        sourceSnapshotId,
        concernedElementId,
        referencedRuleEvaluationResultIds,
        category,
        severity,
        confidence,
        description,
        impact);
  }

  @Override
  public String toString() {
    return "Finding[id="
        + id
        + ", logicalFindingIdentity="
        + logicalFindingIdentity
        + ", ruleIdentifier="
        + ruleIdentifier
        + ", sourceSnapshotId="
        + sourceSnapshotId
        + ", concernedElementId="
        + concernedElementId
        + ", category="
        + category
        + ", severity="
        + severity
        + "]";
  }
}
