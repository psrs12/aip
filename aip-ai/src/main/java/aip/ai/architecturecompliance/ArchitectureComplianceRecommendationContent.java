package aip.ai.architecturecompliance;

import aip.core.csm.CsmElementId;
import java.util.Objects;

/**
 * The Architecture Compliance Agent's Recommendation content, per spec
 * {@code Architecture Compliance Agent Recommendation Content}: an
 * opaque payload to the generic Agent Framework, but this Agent's own
 * content shape, per {@code implement-architecture-compliance-agent/
 * design.md} Decision 7.
 *
 * @param violatingComponentId the concerned Architecture Component —
 *     copied from {@code Finding.concernedElementId()}.
 * @param violationDescription identification of the dependency target
 *     and violated boundary relationship — copied from {@code
 *     Finding.description()} unmodified (already contains this
 *     content, per {@code implement-architecture-compliance-agent/
 *     design.md} Decision 5).
 * @param remediationGuidance a general remediation approach expressed
 *     as guidance content — never generated code, a diff, or a
 *     Proposed Change, per spec {@code Architecture Compliance Agent
 *     Recommendation Excludes Code and Proposed Changes}.
 */
public record ArchitectureComplianceRecommendationContent(
    CsmElementId violatingComponentId, String violationDescription, String remediationGuidance) {

  public ArchitectureComplianceRecommendationContent {
    Objects.requireNonNull(violatingComponentId, "violatingComponentId");
    Objects.requireNonNull(violationDescription, "violationDescription");
    Objects.requireNonNull(remediationGuidance, "remediationGuidance");
  }
}
