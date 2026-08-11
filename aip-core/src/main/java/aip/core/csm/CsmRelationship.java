package aip.core.csm;

import java.util.Objects;
import java.util.Optional;

/**
 * A Canonical Software Model relationship between two CSM elements,
 * typed as one of the closed set in {@link CsmRelationshipType}.
 *
 * <p>Every CSM relationship is {@code Knowledge}, the same as every
 * {@link CsmElement} (see {@code Evidence and Knowledge Distinction}):
 * it always carries a {@link ProvenanceRecord}.
 *
 * @param targetId the relationship's target entity, when one is
 *     evidenced. Absent means "no target/consumer has been evidenced,"
 *     never "the target is the source itself" — a consumer entity
 *     SHALL NOT be invented merely to populate this field (see, e.g.,
 *     {@code API Contract Relationship Construction}'s "no invented
 *     consumer" rule in the {@code csm-builder} specification). Most
 *     relationship types (containment, dependency, integration, ...)
 *     are only ever constructed once both ends are known, so in
 *     practice this is absent only for relationship types whose
 *     originating evidence does not itself identify a target.
 * @param dependencyKind the dependency-kind qualifier — present only
 *     when {@code type} is {@link CsmRelationshipType#DEPENDENCY} and
 *     the kind was discoverable; see {@link DependencyKind}.
 */
public record CsmRelationship(
    CsmElementId id,
    CsmRelationshipType type,
    CsmElementId sourceId,
    Optional<CsmElementId> targetId,
    ProvenanceRecord provenance,
    NativeAttributes nativeAttributes,
    Optional<DependencyKind> dependencyKind) {

  public CsmRelationship {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(sourceId, "sourceId");
    Objects.requireNonNull(targetId, "targetId");
    Objects.requireNonNull(provenance, "provenance");
    Objects.requireNonNull(nativeAttributes, "nativeAttributes");
    Objects.requireNonNull(dependencyKind, "dependencyKind");
    if (dependencyKind.isPresent() && type != CsmRelationshipType.DEPENDENCY) {
      throw new IllegalArgumentException(
          "dependencyKind is only meaningful for a DEPENDENCY relationship, not " + type);
    }
  }

  /** Convenience factory for a relationship with a known target and no dependency-kind qualifier. */
  public static CsmRelationship of(
      CsmElementId id,
      CsmRelationshipType type,
      CsmElementId sourceId,
      CsmElementId targetId,
      ProvenanceRecord provenance,
      NativeAttributes nativeAttributes) {
    Objects.requireNonNull(targetId, "targetId");
    return new CsmRelationship(
        id, type, sourceId, Optional.of(targetId), provenance, nativeAttributes, Optional.empty());
  }

  /**
   * Convenience factory for a relationship whose target/consumer is
   * not evidenced — never used to invent a consumer entity, only to
   * represent the declaring side of a relationship whose other end is
   * genuinely unknown from current evidence.
   */
  public static CsmRelationship withUnevidencedTarget(
      CsmElementId id,
      CsmRelationshipType type,
      CsmElementId sourceId,
      ProvenanceRecord provenance,
      NativeAttributes nativeAttributes) {
    return new CsmRelationship(
        id, type, sourceId, Optional.empty(), provenance, nativeAttributes, Optional.empty());
  }
}
