package aip.csmbuilder.provenance;

import aip.core.csm.CsmElement;
import aip.core.csm.CsmRelationship;
import aip.core.csm.ProvenanceCategory;
import aip.core.evidence.EvidenceItem;
import aip.csmbuilder.mapping.MappingResult;

/**
 * Defense-in-depth runtime checks applied to every CSM element and
 * relationship a Mapper produces, independent of whether the Mapper
 * implementation itself was careful:
 *
 * <ul>
 *   <li>{@code observed}-only, never confidence (tasks.md 6.2; see
 *       {@code Provenance Assignment for Constructed Knowledge}: "CSM
 *       Builder SHALL NOT assign a confidence level to any element or
 *       relationship it constructs").
 *   <li>traceability: the produced content's provenance source
 *       reference must match the originating Evidence Item that was
 *       actually passed to the Mapper (tasks.md 6.3; see
 *       {@code Evidence Traceability Preservation}).
 * </ul>
 *
 * <p>{@link aip.core.csm.ProvenanceRecord} itself (in {@code aip-core})
 * deliberately allows {@code declared} provenance to carry confidence,
 * since the general Canonical Software Model permits that — this guard
 * enforces CSM Builder's own, stricter rule on top of that general
 * type, without weakening the general domain model to do it.
 */
public final class ProvenanceGuard {

  private ProvenanceGuard() {}

  /**
   * Verifies every element and relationship in {@code result} against
   * the Evidence Item that produced it. Throws {@link IllegalStateException}
   * on any violation.
   */
  public static void verify(MappingResult result, EvidenceItem drivingItem) {
    String expectedSourceReference = drivingItem.id().toString();
    for (CsmElement element : result.elements()) {
      requireObservedNoConfidence(element.provenance().category(), element.provenance().confidence().isPresent(), element);
      requireTraceableTo(element.provenance().sourceReference(), expectedSourceReference, element);
    }
    for (CsmRelationship relationship : result.relationships()) {
      requireObservedNoConfidence(
          relationship.provenance().category(), relationship.provenance().confidence().isPresent(), relationship);
      requireTraceableTo(relationship.provenance().sourceReference(), expectedSourceReference, relationship);
    }
  }

  private static void requireObservedNoConfidence(
      ProvenanceCategory category, boolean hasConfidence, Object producedContent) {
    if (category != ProvenanceCategory.OBSERVED) {
      throw new IllegalStateException(
          "CSM Builder SHALL only construct OBSERVED provenance, not " + category + ": "
              + producedContent);
    }
    if (hasConfidence) {
      throw new IllegalStateException(
          "CSM Builder SHALL NOT assign a confidence level to constructed content: "
              + producedContent);
    }
  }

  private static void requireTraceableTo(
      String actualSourceReference, String expectedSourceReference, Object producedContent) {
    boolean traceable =
        actualSourceReference.equals(expectedSourceReference)
            || isPresentInJoinedReference(actualSourceReference, expectedSourceReference);
    if (!traceable) {
      throw new IllegalStateException(
          "constructed content's provenance source reference ('"
              + actualSourceReference
              + "') does not trace back to the Evidence Item that produced it ('"
              + expectedSourceReference
              + "'): "
              + producedContent);
    }
  }

  private static boolean isPresentInJoinedReference(String joined, String candidate) {
    for (String part : joined.split(",")) {
      if (part.equals(candidate)) {
        return true;
      }
    }
    return false;
  }
}
