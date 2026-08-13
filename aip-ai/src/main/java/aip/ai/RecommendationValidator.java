package aip.ai;

import aip.core.csm.FindingSource;
import aip.core.csm.ValidationResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Validates a constructed {@link Recommendation} before it is
 * considered usable output, per {@code Recommendation Validation
 * Before Publication}: referential integrity of the referenced
 * Finding; Agent/version consistency against the currently registered
 * Agent; Generation Provenance completeness; required-field presence;
 * and isolation from deterministic facts.
 *
 * <p><strong>Explicitly does NOT, and structurally cannot, determine
 * substantive correctness</strong> — per {@code Validation Does Not
 * Determine Substantive Correctness}, this is a deliberate, named
 * boundary: a well-formed but substantively questionable Recommendation
 * still passes, and a low-Confidence Recommendation is never rejected
 * on that basis alone. Trusts that the referenced Finding's own
 * upstream chain (consumed RuleEvaluationResults, CSM content) was
 * already validated by {@code aip-findings}'s own {@code
 * FindingValidator} — this validator confirms the Finding identity is
 * <em>resolvable and published</em>, it does not re-check that
 * Finding's own referential integrity (`implement-agent-framework/
 * design.md` Decision 8's trust-boundary reasoning).
 */
public final class RecommendationValidator {

  private RecommendationValidator() {}

  public static ValidationResult validate(
      Recommendation recommendation, FindingSource findingSource, AgentRegistry agentRegistry) {
    Objects.requireNonNull(recommendation, "recommendation");
    Objects.requireNonNull(findingSource, "findingSource");
    Objects.requireNonNull(agentRegistry, "agentRegistry");

    List<String> violations = new ArrayList<>();

    if (findingSource.read(recommendation.findingEvaluationIdentity()).isEmpty()) {
      violations.add(
          "Recommendation " + recommendation.id() + " references Finding "
              + recommendation.findingEvaluationIdentity() + " which does not exist or has not been published");
    }

    Optional<Agent> agent = agentRegistry.lookup(recommendation.agentIdentifier());
    if (agent.isEmpty()) {
      violations.add(
          "Recommendation " + recommendation.id() + " declares producing Agent " + recommendation.agentIdentifier()
              + " which is not currently registered");
    } else if (agent.get().version() != recommendation.agentVersion()) {
      violations.add(
          "Recommendation " + recommendation.id() + " declares Agent version " + recommendation.agentVersion()
              + " but the currently registered version of " + recommendation.agentIdentifier() + " is "
              + agent.get().version());
    }

    GenerationProvenance provenance = recommendation.provenance();
    if (provenance.modelProviderIdentifier().isBlank()) {
      violations.add("Recommendation " + recommendation.id() + " has an empty Generation Provenance model/provider identifier");
    }
    if (provenance.generationTimestamp() == null) {
      violations.add("Recommendation " + recommendation.id() + " has no Generation Provenance timestamp");
    }

    double confidence = recommendation.confidence();
    if (Double.isNaN(confidence) || confidence < 0.0 || confidence > 1.0) {
      violations.add("Recommendation " + recommendation.id() + " has a Confidence value outside [0.0, 1.0]: " + confidence);
    }

    if (recommendation.content() == null) {
      violations.add("Recommendation " + recommendation.id() + " has no content");
    }

    // Isolation from deterministic facts (spec's own "SHALL carry no
    // field representing a claimed mutation" clause) is structurally
    // guaranteed by Recommendation's own field list, per
    // implement-agent-framework/design.md Decision 2 - no such field
    // exists for this check to inspect.

    return violations.isEmpty() ? ValidationResult.success() : ValidationResult.failure(violations);
  }
}
