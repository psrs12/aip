package aip.csmbuilder.mapper;

import aip.core.csm.NativeAttributes;
import aip.core.csm.ProjectElement;
import aip.core.evidence.EvidenceItem;
import aip.core.evidence.EvidenceKind;
import aip.csmbuilder.identity.ElementIdentityDeriver;
import aip.csmbuilder.mapping.EvidenceKindMapper;
import aip.csmbuilder.mapping.MappingContext;
import aip.csmbuilder.mapping.MappingResult;
import aip.csmbuilder.provenance.ObservedProvenanceFactory;

/**
 * Maps a {@code Project} Evidence Item to a CSM {@link ProjectElement},
 * per {@code Structural Entity Mapping}.
 *
 * <p>This includes Repository Understanding's reserved unmanaged
 * Project Evidence Item (see {@code Unmanaged Evidence Mapping}): from
 * CSM Builder's perspective it is structurally identical to any other
 * Project Evidence Item — it carries its own stable identity like any
 * other, so no special-case logic is needed here. The "reserved
 * identity" requirement is satisfied simply by deriving this Mapper's
 * output identity from whatever identity Repository Understanding
 * already assigned, the same as for every other Project.
 */
public final class ProjectMapper implements EvidenceKindMapper {

  private static final int MAPPER_VERSION = 1;

  @Override
  public EvidenceKind supportedKind() {
    return EvidenceKind.PROJECT;
  }

  @Override
  public int mapperVersion() {
    return MAPPER_VERSION;
  }

  @Override
  public MappingResult map(EvidenceItem item, MappingContext context) {
    ProjectElement element =
        new ProjectElement(
            ElementIdentityDeriver.fromEvidenceId(item.id()),
            item.id().scopeKey(),
            ObservedProvenanceFactory.fromEvidence(item.id(), context.constructionTimestamp()),
            NativeAttributes.empty());
    return MappingResult.ofElement(element);
  }
}
