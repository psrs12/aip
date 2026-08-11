package aip.csmbuilder.mapping;

import aip.core.csm.CsmRelationship;
import aip.core.csm.CsmRelationshipType;
import java.util.EnumSet;
import java.util.Set;

/**
 * Defense-in-depth runtime check, applied by {@link MappingOrchestrator}
 * to its final accumulated result, that CSM Builder never constructs an
 * {@code implementation/extension} or {@code invocation} relationship,
 * per {@code Exclusion of Relationship Types Without Corresponding
 * Evidence}: "since the Repository Evidence Model, as currently
 * specified, provides no Evidence kind from which either relationship
 * type could be mechanically derived." No relationship-construction
 * site in this codebase currently produces either type — this guard
 * exists so that a future change accidentally introducing one (e.g. a
 * new Mapper misusing an existing relationship type) fails loudly
 * instead of silently expanding CSM Builder's construction scope
 * beyond what current evidence coverage supports (tasks.md 13.2).
 *
 * <p>Per the same requirement's "Future evidence coverage extends
 * without redesign" scenario, this exclusion is not a permanent
 * prohibition the way {@link EvidenceKindMapperRegistry}'s {@code
 * ConfigFile}/{@code ConfigReference} guard is (tasks.md 13.1): once a
 * future Evidence kind mechanically evidences one of these relationship
 * types, the Mapper registered for it is expected to produce that type,
 * and this guard's excluded set would be revisited as part of that
 * deliberate change — never silently.
 */
final class ExcludedRelationshipTypeGuard {

  private static final Set<CsmRelationshipType> EXCLUDED_UNDER_CURRENT_EVIDENCE_COVERAGE =
      EnumSet.of(CsmRelationshipType.IMPLEMENTATION_EXTENSION, CsmRelationshipType.INVOCATION);

  private ExcludedRelationshipTypeGuard() {}

  static void verify(MappingResult result) {
    for (CsmRelationship relationship : result.relationships()) {
      if (EXCLUDED_UNDER_CURRENT_EVIDENCE_COVERAGE.contains(relationship.type())) {
        throw new IllegalStateException(
            "CSM Builder SHALL NOT construct a " + relationship.type() + " relationship under"
                + " current Repository Evidence coverage (see 'Exclusion of Relationship Types"
                + " Without Corresponding Evidence'): "
                + relationship);
      }
    }
  }
}
