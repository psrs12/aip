package aip.core.evidence;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A complete Repository Evidence Model: the Evidence Items discovered
 * for one repository, and the relationships between them, per
 * {@code Repository Evidence Model Shape}: "a complete, independently
 * valid, independently queryable artifact whose validity does not
 * depend on any downstream consumer existing."
 *
 * <p>Referential integrity is enforced at construction time: every
 * relationship's source and target SHALL reference an item present in
 * this model.
 */
public final class RepositoryEvidenceModel {

  private final Map<EvidenceId, EvidenceItem> items;
  private final List<EvidenceRelationship> relationships;

  private RepositoryEvidenceModel(
      Map<EvidenceId, EvidenceItem> items, List<EvidenceRelationship> relationships) {
    this.items = items;
    this.relationships = relationships;
  }

  public static RepositoryEvidenceModel of(
      List<EvidenceItem> items, List<EvidenceRelationship> relationships) {
    Objects.requireNonNull(items, "items");
    Objects.requireNonNull(relationships, "relationships");

    Map<EvidenceId, EvidenceItem> byId = new LinkedHashMap<>();
    for (EvidenceItem item : items) {
      byId.put(item.id(), item);
    }

    for (EvidenceRelationship relationship : relationships) {
      if (!byId.containsKey(relationship.sourceId())) {
        throw new IllegalArgumentException(
            "relationship source " + relationship.sourceId() + " is not a known Evidence Item");
      }
      if (!byId.containsKey(relationship.targetId())) {
        throw new IllegalArgumentException(
            "relationship target " + relationship.targetId() + " is not a known Evidence Item");
      }
    }

    return new RepositoryEvidenceModel(Map.copyOf(byId), List.copyOf(relationships));
  }

  public static RepositoryEvidenceModel empty() {
    return new RepositoryEvidenceModel(Map.of(), List.of());
  }

  public Optional<EvidenceItem> find(EvidenceId id) {
    return Optional.ofNullable(items.get(id));
  }

  public Map<EvidenceId, EvidenceItem> items() {
    return items;
  }

  public List<EvidenceRelationship> relationships() {
    return relationships;
  }

  /** The Evidence Item representing the repository itself, if present (see {@code Repository Identity}). */
  public Optional<EvidenceItem> repository() {
    return items.values().stream().filter(item -> item.kind() == EvidenceKind.REPOSITORY).findFirst();
  }
}
