package aip.core.csm;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Identifies what a knowledge assertion is <em>about</em>, per {@code
 * Subject Identification for Knowledge Assertions}: "the combination
 * of: (a) the anchored CSM entity or entities the assertion is about,
 * and (b) the specific relationship type or attribute being asserted
 * about that anchor." Two assertions compete, conflict, or reconcile
 * with one another only when both their anchors and their assertion
 * kind match exactly — assertions that share an anchor but assert
 * different things about it are never considered knowledge about the
 * same subject.
 *
 * @param anchors the CSM entity or entities the assertion is about.
 *     Never empty.
 * @param assertionKind the specific relationship type or attribute
 *     being asserted about {@code anchors} (e.g. "Architecture
 *     Component membership," "dependency-kind classification toward
 *     Module B," "criticality"). This type deliberately does not
 *     constrain its shape beyond "stable and non-blank" — see {@link
 *     #forRelationship} and {@link #forElementIdentity} for this
 *     package's own conventions.
 */
public record Subject(List<CsmElementId> anchors, String assertionKind) {

  public Subject {
    Objects.requireNonNull(anchors, "anchors");
    if (anchors.isEmpty()) {
      throw new IllegalArgumentException("anchors must not be empty");
    }
    Objects.requireNonNull(assertionKind, "assertionKind");
    if (assertionKind.isBlank()) {
      throw new IllegalArgumentException("assertionKind must not be blank");
    }
    anchors = List.copyOf(anchors);
  }

  /**
   * The subject of "what is {@code elementId}'s own identity/shape" —
   * used when an element's very existence or content, not one specific
   * relationship or attribute of it, is what could compete or
   * conflict.
   */
  public static Subject forElementIdentity(CsmElementId elementId) {
    Objects.requireNonNull(elementId, "elementId");
    return new Subject(List.of(elementId), "identity");
  }

  /**
   * The subject of "what {@code type} relationship does {@code
   * sourceId} have toward {@code targetId}" — e.g. for a {@link
   * CsmRelationshipType#DEPENDENCY} relationship, this is exactly the
   * subject of its dependency-kind classification. Two relationships
   * sharing {@code sourceId}, {@code targetId}, and {@code type} are
   * knowledge about the same subject even if they disagree about a
   * qualifier such as {@link DependencyKind} — matching {@code CSM
   * Relationship Identity Derivation}'s own choice to exclude that
   * qualifier from relationship identity.
   */
  public static Subject forRelationship(
      CsmElementId sourceId, CsmRelationshipType type, Optional<CsmElementId> targetId) {
    Objects.requireNonNull(sourceId, "sourceId");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(targetId, "targetId");
    String assertionKind = type.name() + targetId.map(id -> "->" + id).orElse("");
    return new Subject(List.of(sourceId), assertionKind);
  }
}
