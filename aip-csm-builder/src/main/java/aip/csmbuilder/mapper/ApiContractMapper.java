package aip.csmbuilder.mapper;

import aip.core.csm.CsmElementId;
import aip.core.csm.CsmRelationship;
import aip.core.csm.CsmRelationshipType;
import aip.core.csm.NativeAttributes;
import aip.core.csm.ProvenanceRecord;
import aip.core.evidence.EvidenceId;
import aip.core.evidence.EvidenceItem;
import aip.core.evidence.EvidenceKind;
import aip.core.evidence.EvidenceRelationshipType;
import aip.csmbuilder.identity.ElementIdentityDeriver;
import aip.csmbuilder.mapping.EvidenceAttributeKeys;
import aip.csmbuilder.mapping.EvidenceKindMapper;
import aip.csmbuilder.mapping.EvidenceRelationshipLookup;
import aip.csmbuilder.mapping.MappingContext;
import aip.csmbuilder.mapping.MappingResult;
import aip.csmbuilder.provenance.ObservedProvenanceFactory;
import java.util.Optional;

/**
 * Maps an {@code ApiContractDeclaration} Evidence Item to a CSM
 * {@code exposure/consumption} relationship attached to its declaring
 * {@code Type}/{@code Method}, per {@code API Contract Relationship
 * Construction}.
 *
 * <p>Unlike {@link aip.csmbuilder.mapping.DependencyRelationshipBuilder}
 * and its siblings, this fits the ordinary per-item {@link
 * EvidenceKindMapper} dispatch model: each {@code ApiContractDeclaration}
 * Evidence Item stands entirely on its own — it does not need to be
 * grouped with any other Evidence Item to be transformed.
 *
 * <p>The declaring {@code Type}/{@code Method} is found via the {@code
 * REFERENCE} relationship an {@code ApiContractDeclaration} Evidence
 * Item carries to it — the same relationship kind (though opposite
 * direction) {@link FileLocationResolver} uses to find a {@code
 * SourceUnit}/Method-level Evidence Item's File Evidence Item. Its CSM
 * identity is re-derived directly from that Evidence identity ({@link
 * ElementIdentityDeriver#fromEvidenceId}) rather than looked up via
 * {@link MappingContext#resolvedElementId}, so this Mapper's result
 * does not depend on {@link aip.csmbuilder.mapping.MappingOrchestrator}'s
 * dispatch order (see {@link EvidenceRelationshipLookup}'s own
 * javadoc).
 *
 * <p>No consumer entity is ever constructed here: {@code
 * ApiContractDeclaration} evidence carries no fact identifying a
 * consumer (see {@code openspec/changes/implement-csm-builder/design.md}
 * Decision 9), so the constructed relationship's target is always
 * {@link CsmRelationship#withUnevidencedTarget} — never invented, never
 * a self-reference back to the declaring element.
 */
public final class ApiContractMapper implements EvidenceKindMapper {

  private static final int MAPPER_VERSION = 1;

  @Override
  public EvidenceKind supportedKind() {
    return EvidenceKind.API_CONTRACT_DECLARATION;
  }

  @Override
  public int mapperVersion() {
    return MAPPER_VERSION;
  }

  @Override
  public MappingResult map(EvidenceItem item, MappingContext context) {
    Optional<EvidenceId> declaringEvidenceId =
        EvidenceRelationshipLookup.findSingleTarget(
            context.evidenceModel(), EvidenceRelationshipType.REFERENCE, item.id());
    if (declaringEvidenceId.isEmpty()) {
      // No declaring Type/Method evidenced for this contract - nothing
      // to attach the relationship to.
      return MappingResult.empty();
    }

    CsmElementId declaringElementId = ElementIdentityDeriver.fromEvidenceId(declaringEvidenceId.get());
    CsmElementId relationshipId = ElementIdentityDeriver.forApiContractExposure(item.id());
    ProvenanceRecord provenance =
        ObservedProvenanceFactory.fromEvidence(item.id(), context.constructionTimestamp());
    NativeAttributes attributes = structuralSummaryAttributes(item);

    CsmRelationship relationship =
        CsmRelationship.withUnevidencedTarget(
            relationshipId, CsmRelationshipType.EXPOSURE_CONSUMPTION, declaringElementId, provenance, attributes);
    return MappingResult.ofRelationship(relationship);
  }

  private static NativeAttributes structuralSummaryAttributes(EvidenceItem item) {
    return item.attributes()
        .get(EvidenceAttributeKeys.API_STRUCTURAL_SUMMARY)
        .map(summary -> NativeAttributes.empty().with(EvidenceAttributeKeys.API_STRUCTURAL_SUMMARY, summary))
        .orElse(NativeAttributes.empty());
  }
}
