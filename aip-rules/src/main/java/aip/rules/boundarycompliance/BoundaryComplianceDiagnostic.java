package aip.rules.boundarycompliance;

import aip.core.csm.CsmElementId;
import aip.core.csm.FindingMetadata;
import java.util.Objects;

/**
 * The {@code FAIL}-outcome payload for {@link BoundaryComplianceRuleType},
 * implementing {@link FindingMetadata} so a {@code FAIL} outcome
 * qualifies for Finding construction, per {@code
 * implement-finding-model/design.md} Decision 4.
 *
 * <p>{@code category()}/{@code severity()} are fixed constants, applied
 * uniformly across every instance, per spec {@code Boundary-Compliance
 * Category and Severity}. {@code description()}/{@code impact()} are
 * computed dynamically from this record's own three {@link
 * CsmElementId} fields — the concrete mechanism resolving {@code
 * implement-architecture-compliance-agent/proposal.md}'s Binding
 * Decision 3: since Finding construction copies these values onto
 * {@code Finding.description()}/{@code Finding.impact()} unmodified,
 * the violating dependency target and violated boundary relationship
 * become directly readable from the Finding alone, with no new {@code
 * aip-core} contract and no {@code aip-ai} dependency on {@code
 * RuleEvaluationResultSource} (see {@code implement-architecture-
 * compliance-agent/design.md} Decision 5).
 */
public record BoundaryComplianceDiagnostic(
    CsmElementId violatingComponentId, CsmElementId dependencyTargetId, CsmElementId violatedBoundaryRelationshipId)
    implements FindingMetadata {

  static final String CATEGORY = "architecture-boundary-violation";
  static final String SEVERITY = "high";

  public BoundaryComplianceDiagnostic {
    Objects.requireNonNull(violatingComponentId, "violatingComponentId");
    Objects.requireNonNull(dependencyTargetId, "dependencyTargetId");
    Objects.requireNonNull(violatedBoundaryRelationshipId, "violatedBoundaryRelationshipId");
  }

  @Override
  public String category() {
    return CATEGORY;
  }

  @Override
  public String severity() {
    return SEVERITY;
  }

  @Override
  public String description() {
    return "Architecture Component "
        + violatingComponentId
        + " depends on "
        + dependencyTargetId
        + ", violating boundary constraint "
        + violatedBoundaryRelationshipId
        + ".";
  }

  @Override
  public String impact() {
    return "This dependency crosses a declared or inferred architectural boundary and may introduce"
        + " unwanted coupling.";
  }
}
