package aip.csmbuilder.mapper;

import aip.core.csm.CsmElementId;
import aip.core.csm.NativeAttributes;
import aip.core.csm.PackageElement;
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
 * Maps a {@code Package} Evidence Item to a CSM {@link PackageElement},
 * per {@code Structural Entity Mapping}.
 *
 * <p>Unlike the other structural entity Mappers, a Package's identity
 * is not a 1:1 function of its own Evidence identity alone (see
 * {@code CSM Element Identity Derivation}): it also depends on its
 * containing Module's CSM identity. This Mapper finds that Module by
 * traversing the Evidence Model's own {@code CONTAINMENT} relationship
 * pointing at this Package's Evidence identity — never via
 * {@link MappingContext#resolvedElementId}, so this Mapper's result
 * does not depend on whether the containing Module has already been
 * processed earlier in the current orchestration run (see
 * {@link EvidenceRelationshipLookup}).
 */
public final class PackageMapper implements EvidenceKindMapper {

  private static final int MAPPER_VERSION = 1;

  @Override
  public EvidenceKind supportedKind() {
    return EvidenceKind.PACKAGE;
  }

  @Override
  public int mapperVersion() {
    return MAPPER_VERSION;
  }

  @Override
  public MappingResult map(EvidenceItem item, MappingContext context) {
    EvidenceId containingModuleEvidenceId =
        EvidenceRelationshipLookup.findSingleSource(
                context.evidenceModel(), EvidenceRelationshipType.CONTAINMENT, item.id())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Package Evidence Item " + item.id() + " has no containing Module"
                            + " CONTAINMENT relationship (Package Evidence and Containment)"));
    CsmElementId containingModuleId = ElementIdentityDeriver.fromEvidenceId(containingModuleEvidenceId);

    String namespaceName =
        item.attributes()
            .get(EvidenceAttributeKeys.NAMESPACE_NAME)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Package Evidence Item " + item.id() + " is missing its '"
                            + EvidenceAttributeKeys.NAMESPACE_NAME
                            + "' attribute"));

    PackageElement element =
        new PackageElement(
            ElementIdentityDeriver.forPackage(containingModuleId, namespaceName),
            namespaceName,
            ObservedProvenanceFactory.fromEvidence(item.id(), context.constructionTimestamp()),
            NativeAttributes.empty());
    return MappingResult.ofElement(element);
  }
}
