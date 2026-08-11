package aip.csmbuilder.mapping;

import aip.core.evidence.EvidenceId;
import aip.core.evidence.EvidenceRelationship;
import aip.core.evidence.EvidenceRelationshipType;
import aip.core.evidence.RepositoryEvidenceModel;
import java.util.Objects;
import java.util.Optional;

/**
 * A small traversal utility over {@link RepositoryEvidenceModel}'s
 * relationships, used by Mappers that need to find a related Evidence
 * Item's identity — e.g. a Package's containing Module — without
 * depending on {@link MappingOrchestrator}'s processing order.
 * Deliberately a pure query against the Evidence Model, not against
 * {@link MappingContext#resolvedElementId}: since CSM identity
 * derivation is a pure function of Evidence identity (see
 * {@code aip.csmbuilder.identity.ElementIdentityDeriver}), a Mapper
 * can always independently re-derive a related element's CSM identity
 * once it knows the related Evidence Item's identity — it never needs
 * that related item to have already been processed.
 */
public final class EvidenceRelationshipLookup {

  private EvidenceRelationshipLookup() {}

  /**
   * The single source Evidence identity of a {@code type} relationship
   * whose target is {@code targetId}, if exactly one exists.
   *
   * @throws IllegalStateException if more than one such relationship
   *     exists — an Evidence Item is expected to have at most one
   *     container of a given relationship type.
   */
  public static Optional<EvidenceId> findSingleSource(
      RepositoryEvidenceModel model, EvidenceRelationshipType type, EvidenceId targetId) {
    Objects.requireNonNull(model, "model");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(targetId, "targetId");

    Optional<EvidenceId> found = Optional.empty();
    for (EvidenceRelationship relationship : model.relationships()) {
      if (relationship.type() == type && relationship.targetId().equals(targetId)) {
        if (found.isPresent()) {
          throw new IllegalStateException(
              "more than one " + type + " relationship targets " + targetId);
        }
        found = Optional.of(relationship.sourceId());
      }
    }
    return found;
  }

  /**
   * The single target Evidence identity of a {@code type} relationship
   * whose source is {@code sourceId}, if exactly one exists — the
   * mirror of {@link #findSingleSource}. Used, for example, by a
   * {@code SourceUnit}/Method-level Evidence Item to find its own
   * {@code File} Evidence Item via a {@code REFERENCE} relationship
   * (see {@code File Evidence and Kind-Specific Layering}).
   *
   * @throws IllegalStateException if more than one such relationship
   *     exists.
   */
  public static Optional<EvidenceId> findSingleTarget(
      RepositoryEvidenceModel model, EvidenceRelationshipType type, EvidenceId sourceId) {
    Objects.requireNonNull(model, "model");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(sourceId, "sourceId");

    Optional<EvidenceId> found = Optional.empty();
    for (EvidenceRelationship relationship : model.relationships()) {
      if (relationship.type() == type && relationship.sourceId().equals(sourceId)) {
        if (found.isPresent()) {
          throw new IllegalStateException(
              "more than one " + type + " relationship originates from " + sourceId);
        }
        found = Optional.of(relationship.targetId());
      }
    }
    return found;
  }
}
