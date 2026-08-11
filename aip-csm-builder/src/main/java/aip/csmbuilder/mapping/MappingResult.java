package aip.csmbuilder.mapping;

import aip.core.csm.CsmElement;
import aip.core.csm.CsmRelationship;
import java.util.List;
import java.util.Objects;

/**
 * A set of CSM elements and relationships. Used both as a single
 * {@link EvidenceKindMapper}'s per-item output and as the
 * {@link MappingOrchestrator}'s accumulated, whole-run output — the
 * shape is the same in both cases.
 *
 * <p>By convention, when a {@link EvidenceKindMapper#map} call's
 * result contains one or more elements, {@link #elements()}'s first
 * entry is treated as the <em>primary</em> element produced from the
 * driving Evidence Item — the one {@link MappingOrchestrator} records
 * against that item's identity in {@link MappingContext#resolvedElementId}
 * for subsequently-processed items to look up. A Mapper producing only
 * relationships (e.g. from dependency-edge evidence) returns an empty
 * element list.
 */
public record MappingResult(List<CsmElement> elements, List<CsmRelationship> relationships) {

  private static final MappingResult EMPTY = new MappingResult(List.of(), List.of());

  public MappingResult {
    Objects.requireNonNull(elements, "elements");
    Objects.requireNonNull(relationships, "relationships");
    elements = List.copyOf(elements);
    relationships = List.copyOf(relationships);
  }

  public static MappingResult empty() {
    return EMPTY;
  }

  public static MappingResult ofElement(CsmElement element) {
    Objects.requireNonNull(element, "element");
    return new MappingResult(List.of(element), List.of());
  }

  public static MappingResult ofRelationship(CsmRelationship relationship) {
    Objects.requireNonNull(relationship, "relationship");
    return new MappingResult(List.of(), List.of(relationship));
  }

  /** Returns a new result combining this one with {@code other}. */
  public MappingResult merge(MappingResult other) {
    Objects.requireNonNull(other, "other");
    List<CsmElement> mergedElements =
        java.util.stream.Stream.concat(elements.stream(), other.elements.stream()).toList();
    List<CsmRelationship> mergedRelationships =
        java.util.stream.Stream.concat(relationships.stream(), other.relationships.stream())
            .toList();
    return new MappingResult(mergedElements, mergedRelationships);
  }
}
