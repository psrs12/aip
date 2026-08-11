package aip.csmbuilder.mapping;

import aip.core.evidence.DiscoveryOutcomeStatus;
import aip.core.evidence.EvidenceItem;

/**
 * The single test for whether an Evidence Item is eligible to
 * contribute CSM content at all, per {@code Failed-Evidence
 * Non-Construction}: "CSM Builder SHALL NOT construct a CSM element
 * from a Repository Evidence Item with a discovery outcome of {@code
 * failed}." A {@code partial} discovery outcome is eligible — see
 * {@code Partial-Evidence Construction} — construction from it simply
 * uses whatever structure was successfully captured; no Mapper or
 * relationship builder in this codebase treats {@code partial}
 * differently from {@code complete}, so no separate check exists for
 * it here.
 *
 * <p>Applied both at {@link MappingOrchestrator}'s per-item Mapper
 * dispatch (tasks.md 14.2) and by the relationship builders that read
 * "fact" Evidence Items ({@code ManifestDependencyEdge}, {@code
 * ImportEdge}) directly rather than through a registered Mapper ({@link
 * DependencyRelationshipBuilder}, {@link ExternalSystemRelationshipBuilder})
 * — a {@code failed} discovery outcome disqualifies an Evidence Item
 * from contributing to CSM content everywhere it might otherwise
 * appear, not only the specific case of a Mapper-driven element.
 */
final class FailedEvidenceFilter {

  private FailedEvidenceFilter() {}

  static boolean isEligible(EvidenceItem item) {
    return item.discoveryOutcome().status() != DiscoveryOutcomeStatus.FAILED;
  }
}
