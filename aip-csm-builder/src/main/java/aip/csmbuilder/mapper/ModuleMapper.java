package aip.csmbuilder.mapper;

import aip.core.csm.ModuleElement;
import aip.core.csm.NativeAttributes;
import aip.core.evidence.EvidenceItem;
import aip.core.evidence.EvidenceKind;
import aip.csmbuilder.identity.ElementIdentityDeriver;
import aip.csmbuilder.mapping.EvidenceKindMapper;
import aip.csmbuilder.mapping.MappingContext;
import aip.csmbuilder.mapping.MappingResult;
import aip.csmbuilder.provenance.ObservedProvenanceFactory;

/**
 * Maps a {@code Module} Evidence Item to a CSM {@link ModuleElement},
 * per {@code Structural Entity Mapping}.
 *
 * <p>This includes Repository Understanding's default Module Evidence
 * Item for a sub-module-less Project: from CSM Builder's perspective
 * it is structurally identical to any declared sub-module — it carries
 * its own stable identity like any other, so no special-case logic is
 * needed here.
 */
public final class ModuleMapper implements EvidenceKindMapper {

  private static final int MAPPER_VERSION = 1;

  @Override
  public EvidenceKind supportedKind() {
    return EvidenceKind.MODULE;
  }

  @Override
  public int mapperVersion() {
    return MAPPER_VERSION;
  }

  @Override
  public MappingResult map(EvidenceItem item, MappingContext context) {
    ModuleElement element =
        new ModuleElement(
            ElementIdentityDeriver.fromEvidenceId(item.id()),
            item.id().scopeKey(),
            ObservedProvenanceFactory.fromEvidence(item.id(), context.constructionTimestamp()),
            NativeAttributes.empty());
    return MappingResult.ofElement(element);
  }
}
