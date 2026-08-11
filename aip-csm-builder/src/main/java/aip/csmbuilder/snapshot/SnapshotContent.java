package aip.csmbuilder.snapshot;

import aip.core.csm.CsmElement;
import aip.core.csm.CsmElementId;
import aip.core.csm.CsmRelationship;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The full content of one CSM snapshot, as supplied to {@link
 * SnapshotStore#write} before it has been given a repository
 * identifier, sequence number, or content location — those are the
 * store's own concerns.
 *
 * @param attributions every element in {@code elements}' {@link
 *     MapperAttribution}, keyed by {@link CsmElement#id()}. Every
 *     element SHALL have an entry; relationships have none, since the
 *     Snapshot Manifest is scoped to CSM elements only (per {@code
 *     design.md} Decision 2).
 */
public record SnapshotContent(
    List<CsmElement> elements, List<CsmRelationship> relationships, Map<CsmElementId, MapperAttribution> attributions) {

  public SnapshotContent {
    Objects.requireNonNull(elements, "elements");
    Objects.requireNonNull(relationships, "relationships");
    Objects.requireNonNull(attributions, "attributions");
    elements = List.copyOf(elements);
    relationships = List.copyOf(relationships);
    attributions = Map.copyOf(attributions);
    for (CsmElement element : elements) {
      if (!attributions.containsKey(element.id())) {
        throw new IllegalArgumentException(
            "element " + element.id() + " has no MapperAttribution entry; every element in a"
                + " SnapshotContent must have one");
      }
    }
  }
}
