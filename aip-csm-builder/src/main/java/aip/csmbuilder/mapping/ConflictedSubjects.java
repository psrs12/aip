package aip.csmbuilder.mapping;

import aip.core.csm.CsmRelationship;
import aip.core.csm.Subject;
import aip.core.csm.SubjectConflictMarker;
import java.util.Map;
import java.util.Objects;

/**
 * CSM Builder's integration point with the Canonical Software Model's
 * existing {@link SubjectConflictMarker} mechanism, per {@code
 * Conflict Marking Reuse} (tasks.md 18.1): "CSM Builder SHALL apply the
 * Canonical Software Model's existing same-category conflict marking
 * mechanism, preserving both assertions and marking the subject's
 * effective knowledge {@code CONFLICTED}, rather than resolving the
 * conflict itself."
 *
 * <p>This class does not change what {@link MappingOrchestrator}
 * constructs or accumulates — every relationship it ever produces is
 * already preserved in {@link MappingResult} regardless of whether it
 * shares a {@link Subject} with another one (see {@link
 * MappingResult#merge}, which only ever concatenates). This class is
 * purely a query CSM Builder (or any caller) can use to ask "which
 * subjects, among these relationships, are conflicted" — CSM Builder
 * itself never arbitrates that question (tasks.md 18.2).
 *
 * <p>Only relationships are covered here, not elements: CSM Builder's
 * element identity derivation (Section 5) is itself a deduplicating
 * function of Evidence identity, so two distinct elements sharing a
 * {@link Subject#forElementIdentity} subject cannot arise from this
 * module's own construction paths as they exist today (every current
 * Mapper produces at most one element per Evidence Item). A dependency
 * relationship's kind qualifier, by contrast, is deliberately excluded
 * from relationship identity (see {@code CSM Relationship Identity
 * Derivation}), which is exactly what makes a same-subject,
 * different-qualifier conflict representable at all.
 */
public final class ConflictedSubjects {

  private ConflictedSubjects() {}

  /**
   * Classifies {@code relationships} by {@link Subject#forRelationship},
   * per {@code Conflict Marking Reuse}. Callers SHALL pass only
   * relationships sharing one provenance category (CSM Builder itself
   * only ever constructs {@code observed} relationships — see {@code
   * Provenance Assignment for Constructed Knowledge}).
   */
  public static Map<Subject, SubjectConflictMarker.Classification<CsmRelationship>> classify(
      MappingResult result) {
    Objects.requireNonNull(result, "result");
    return SubjectConflictMarker.classify(
        result.relationships(),
        relationship -> Subject.forRelationship(relationship.sourceId(), relationship.type(), relationship.targetId()));
  }
}
