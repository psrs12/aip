package aip.ai;

import aip.core.csm.EvaluationIdentity;
import aip.core.csm.LogicalFindingIdentity;
import java.util.Objects;

/**
 * A durable, consumer-facing suggestion an {@link Agent} produces from
 * a single {@link aip.core.csm.Finding}, per {@code Recommendation Is
 * a Distinct, Immutable Artifact From Finding}.
 *
 * <p>An {@code aip-ai}-local type, per {@code
 * implement-agent-framework/design.md} Decision 2 — deliberately
 * <strong>not</strong> promoted to {@code aip-core}, unlike {@code
 * AnalysisResult}/{@code RuleEvaluationResult}/{@code Finding} (each
 * promoted for a specific, already-named future consumer). No field
 * on this type represents a claimed mutation of CSM content, an
 * Analysis Result, a RuleEvaluationResult, or the referenced Finding —
 * structurally guaranteed by this field list, per {@code
 * Deterministic-Layer Protection}.
 *
 * <p>Immutable once constructed: every field is {@code final}, and no
 * setter or mutator method exists anywhere on this type, per {@code
 * Recommendation Is a Distinct, Immutable Artifact From Finding}'s own
 * "no operation modifies an existing Recommendation" requirement.
 */
public final class Recommendation {

  private final RecommendationArtifactIdentity id;
  private final String agentIdentifier;
  private final int agentVersion;
  private final EvaluationIdentity findingEvaluationIdentity;
  private final LogicalFindingIdentity findingLogicalFindingIdentity;
  private final GenerationIdentifier generationIdentifier;
  private final Object content;
  private final double confidence;
  private final GenerationProvenance provenance;

  public Recommendation(
      RecommendationArtifactIdentity id,
      String agentIdentifier,
      int agentVersion,
      EvaluationIdentity findingEvaluationIdentity,
      LogicalFindingIdentity findingLogicalFindingIdentity,
      GenerationIdentifier generationIdentifier,
      Object content,
      double confidence,
      GenerationProvenance provenance) {
    this.id = Objects.requireNonNull(id, "id");
    this.agentIdentifier = Objects.requireNonNull(agentIdentifier, "agentIdentifier");
    if (agentIdentifier.isBlank()) {
      throw new IllegalArgumentException("agentIdentifier must not be blank");
    }
    this.agentVersion = agentVersion;
    this.findingEvaluationIdentity = Objects.requireNonNull(findingEvaluationIdentity, "findingEvaluationIdentity");
    this.findingLogicalFindingIdentity =
        Objects.requireNonNull(findingLogicalFindingIdentity, "findingLogicalFindingIdentity");
    this.generationIdentifier = Objects.requireNonNull(generationIdentifier, "generationIdentifier");
    this.content = Objects.requireNonNull(content, "content");
    if (Double.isNaN(confidence) || confidence < 0.0 || confidence > 1.0) {
      throw new IllegalArgumentException("confidence must be within [0.0, 1.0], was: " + confidence);
    }
    this.confidence = confidence;
    this.provenance = Objects.requireNonNull(provenance, "provenance");
  }

  /**
   * Constructs a {@code Recommendation}, computing its {@link
   * RecommendationArtifactIdentity} from the same identity components
   * rather than requiring a caller to compute it separately and risk
   * the two drifting apart.
   */
  public static Recommendation of(
      String agentIdentifier,
      int agentVersion,
      EvaluationIdentity findingEvaluationIdentity,
      LogicalFindingIdentity findingLogicalFindingIdentity,
      GenerationIdentifier generationIdentifier,
      Object content,
      double confidence,
      GenerationProvenance provenance) {
    RecommendationArtifactIdentity id =
        RecommendationArtifactIdentity.of(agentIdentifier, agentVersion, findingEvaluationIdentity, generationIdentifier);
    return new Recommendation(
        id,
        agentIdentifier,
        agentVersion,
        findingEvaluationIdentity,
        findingLogicalFindingIdentity,
        generationIdentifier,
        content,
        confidence,
        provenance);
  }

  public RecommendationArtifactIdentity id() {
    return id;
  }

  public String agentIdentifier() {
    return agentIdentifier;
  }

  public int agentVersion() {
    return agentVersion;
  }

  /** The referenced Finding's Evaluation Identity — the precise snapshot-bound artifact this Agent actually read. */
  public EvaluationIdentity findingEvaluationIdentity() {
    return findingEvaluationIdentity;
  }

  /** The referenced Finding's Logical Finding Identity, carried forward for cross-snapshot traceability. */
  public LogicalFindingIdentity findingLogicalFindingIdentity() {
    return findingLogicalFindingIdentity;
  }

  public GenerationIdentifier generationIdentifier() {
    return generationIdentifier;
  }

  /** The opaque, Agent-defined guidance content — never constrained or interpreted by the framework. */
  public Object content() {
    return content;
  }

  /** The Agent's own declared Confidence, in {@code [0.0, 1.0]} — never computed or overridden by the framework. */
  public double confidence() {
    return confidence;
  }

  public GenerationProvenance provenance() {
    return provenance;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Recommendation other)) {
      return false;
    }
    return id.equals(other.id)
        && agentIdentifier.equals(other.agentIdentifier)
        && agentVersion == other.agentVersion
        && findingEvaluationIdentity.equals(other.findingEvaluationIdentity)
        && findingLogicalFindingIdentity.equals(other.findingLogicalFindingIdentity)
        && generationIdentifier.equals(other.generationIdentifier)
        && content.equals(other.content)
        && Double.compare(confidence, other.confidence) == 0
        && provenance.equals(other.provenance);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        id,
        agentIdentifier,
        agentVersion,
        findingEvaluationIdentity,
        findingLogicalFindingIdentity,
        generationIdentifier,
        content,
        confidence,
        provenance);
  }

  @Override
  public String toString() {
    return "Recommendation[id="
        + id
        + ", agentIdentifier="
        + agentIdentifier
        + ", agentVersion="
        + agentVersion
        + ", findingEvaluationIdentity="
        + findingEvaluationIdentity
        + ", generationIdentifier="
        + generationIdentifier
        + ", confidence="
        + confidence
        + "]";
  }
}
