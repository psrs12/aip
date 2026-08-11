package aip.csmbuilder.mapper;

import aip.core.csm.NativeAttributes;
import aip.core.csm.RepositoryElement;
import aip.core.evidence.EvidenceItem;
import aip.core.evidence.EvidenceKind;
import aip.csmbuilder.identity.ElementIdentityDeriver;
import aip.csmbuilder.mapping.EvidenceKindMapper;
import aip.csmbuilder.mapping.MappingContext;
import aip.csmbuilder.mapping.MappingResult;
import aip.csmbuilder.provenance.ObservedProvenanceFactory;

/**
 * Maps a {@code Repository} Evidence Item to a CSM {@link RepositoryElement},
 * per {@code Structural Entity Mapping}.
 */
public final class RepositoryMapper implements EvidenceKindMapper {

  private static final int MAPPER_VERSION = 1;

  @Override
  public EvidenceKind supportedKind() {
    return EvidenceKind.REPOSITORY;
  }

  @Override
  public int mapperVersion() {
    return MAPPER_VERSION;
  }

  @Override
  public MappingResult map(EvidenceItem item, MappingContext context) {
    RepositoryElement element =
        new RepositoryElement(
            ElementIdentityDeriver.fromEvidenceId(item.id()),
            item.id().scopeKey(),
            ObservedProvenanceFactory.fromEvidence(item.id(), context.constructionTimestamp()),
            NativeAttributes.empty());
    return MappingResult.ofElement(element);
  }
}
