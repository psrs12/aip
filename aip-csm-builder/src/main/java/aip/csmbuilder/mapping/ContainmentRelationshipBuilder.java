package aip.csmbuilder.mapping;

import aip.core.csm.CsmElementId;
import aip.core.csm.CsmRelationship;
import aip.core.csm.CsmRelationshipType;
import aip.core.csm.NativeAttributes;
import aip.core.csm.ProvenanceRecord;
import aip.core.evidence.EvidenceId;
import aip.core.evidence.EvidenceRelationship;
import aip.core.evidence.EvidenceRelationshipType;
import aip.core.evidence.RepositoryEvidenceModel;
import aip.csmbuilder.identity.ElementIdentityDeriver;
import aip.csmbuilder.provenance.ObservedProvenanceFactory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Constructs CSM {@code CONTAINMENT} relationships
 * (Repository→Project→Module→Package→Type[→Method]) directly from the
 * Repository Evidence Model's own {@code CONTAINMENT} relationships,
 * per {@code Structural Containment Construction} and
 * {@code openspec/changes/implement-csm-builder/design.md} Decision 5:
 * "close to mechanical: no interpretation is required, since both
 * containment chains were deliberately engineered to align."
 *
 * <p>Unlike the entity Mappers in {@code aip.csmbuilder.mapper}, this
 * is not registered against an {@code EvidenceKind} — containment is a
 * relationship between Evidence Items, not an Evidence Item in its own
 * right, so it does not fit the {@link EvidenceKindMapper} dispatch
 * model. {@link MappingOrchestrator} invokes this directly, once, after
 * every Evidence Item has been dispatched.
 *
 * <p>Both endpoints are resolved via the {@link MappingOrchestrator}'s
 * already-produced identity index (which entity Mapper produced a CSM
 * element for which Evidence identity) rather than re-deriving each
 * endpoint's identity independently — unlike
 * {@code aip.csmbuilder.mapper.PackageMapper}'s own containing-Module
 * lookup, a generic containment builder cannot know, for an arbitrary
 * endpoint, which of {@link ElementIdentityDeriver}'s several
 * derivation functions applies (e.g. {@code Package} needs its own
 * containing Module id and namespace name, not just its Evidence
 * identity) — but every entity Mapper already resolved that correctly
 * when it ran, so reusing that result is both simpler and correct.
 *
 * <p>An Evidence-side containment edge whose source or target Evidence
 * Item produced no CSM element (no Mapper was registered for its kind,
 * or the kind is not yet supported) is silently omitted — not an
 * error. This is the same "absence is valid" precedent already
 * established for Method-level elements at the CSM specification
 * level.
 */
final class ContainmentRelationshipBuilder {

  private ContainmentRelationshipBuilder() {}

  static List<CsmRelationship> build(
      RepositoryEvidenceModel evidenceModel,
      Map<EvidenceId, CsmElementId> resolvedElementIds,
      Instant constructionTimestamp) {
    List<CsmRelationship> relationships = new ArrayList<>();

    for (EvidenceRelationship evidenceRelationship : evidenceModel.relationships()) {
      if (evidenceRelationship.type() != EvidenceRelationshipType.CONTAINMENT) {
        continue;
      }

      Optional<CsmElementId> sourceId =
          Optional.ofNullable(resolvedElementIds.get(evidenceRelationship.sourceId()));
      Optional<CsmElementId> targetId =
          Optional.ofNullable(resolvedElementIds.get(evidenceRelationship.targetId()));
      if (sourceId.isEmpty() || targetId.isEmpty()) {
        continue;
      }

      CsmElementId relationshipId =
          ElementIdentityDeriver.forRelationship(sourceId.get(), targetId.get(), CsmRelationshipType.CONTAINMENT);
      ProvenanceRecord provenance =
          ObservedProvenanceFactory.fromEvidence(
              List.of(evidenceRelationship.sourceId(), evidenceRelationship.targetId()),
              constructionTimestamp);

      relationships.add(
          new CsmRelationship(
              relationshipId,
              CsmRelationshipType.CONTAINMENT,
              sourceId.get(),
              targetId.get(),
              provenance,
              NativeAttributes.empty(),
              Optional.empty()));
    }

    return relationships;
  }
}
