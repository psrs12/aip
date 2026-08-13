package aip.ai;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * A record of what produced a {@link Recommendation}, per {@code
 * Generation Provenance}: "the producing model or provider identifier
 * and version, the generation configuration used (as opaque,
 * Agent-defined values not interpreted by the framework), and a
 * generation timestamp."
 *
 * <p>Explicitly excluded from {@link RecommendationArtifactIdentity}
 * computation ({@code Recommendation Artifact Identity Excludes
 * Generated Content}) — this is what makes a Recommendation
 * explainable without claiming its content is reproducible. All
 * AI-specific mechanics (prompt template identifiers, model/provider
 * details, generation configuration) live exclusively here, per {@code
 * AI-Specific Content Isolation From Deterministic Artifacts}.
 *
 * @param modelProviderIdentifier the producing model/provider's
 *     identifier. Never blank.
 * @param modelVersion the producing model/provider's version. Never
 *     blank.
 * @param generationConfiguration opaque, Agent-defined key/value
 *     configuration (e.g. {@code "temperature" -> "0.2"}), never
 *     interpreted by the framework.
 * @param generationTimestamp when this Recommendation was generated.
 */
public record GenerationProvenance(
    String modelProviderIdentifier,
    String modelVersion,
    Map<String, String> generationConfiguration,
    Instant generationTimestamp) {

  public GenerationProvenance {
    // Non-null, but deliberately NOT non-blank-enforced here: spec
    // `Recommendation Validation Before Publication` frames "a
    // non-empty model/provider identifier" as a *validation* check
    // (RecommendationValidator), not a construction-time invariant -
    // an Agent implementation defect producing a blank identifier
    // SHALL fail validation, not throw during construction, so a
    // RecommendationValidatorTest can exercise the rejection path
    // directly against an otherwise-well-formed Recommendation.
    Objects.requireNonNull(modelProviderIdentifier, "modelProviderIdentifier");
    Objects.requireNonNull(modelVersion, "modelVersion");
    Objects.requireNonNull(generationConfiguration, "generationConfiguration");
    generationConfiguration = Map.copyOf(generationConfiguration);
    Objects.requireNonNull(generationTimestamp, "generationTimestamp");
  }
}
