package aip.csmbuilder.mapper;

import aip.core.csm.MethodElement;
import aip.core.csm.NativeAttributes;
import aip.core.evidence.EvidenceItem;
import aip.core.evidence.EvidenceKind;
import aip.csmbuilder.identity.ElementIdentityDeriver;
import aip.csmbuilder.mapping.EvidenceKindMapper;
import aip.csmbuilder.mapping.MappingContext;
import aip.csmbuilder.mapping.MappingResult;
import aip.csmbuilder.provenance.ObservedProvenanceFactory;
import java.util.Optional;

/**
 * Maps a Method-level Evidence Item to a CSM {@link MethodElement}, per
 * {@code Structural Entity Mapping}. Invoked only for Evidence Items
 * Repository Understanding actually produced — Method-level evidence
 * is itself optional per RU's own specification, so the absence of any
 * such Evidence Items simply means this Mapper is never invoked for a
 * given Type, which is valid (see {@code Optional Method-Level
 * Representation}).
 *
 * <p>{@link MethodElement#sourceLocation()} is left empty by this
 * Mapper — see {@link TypeMapper}'s javadoc for why (Section 9's
 * concern, not this one's).
 */
public final class MethodMapper implements EvidenceKindMapper {

  private static final int MAPPER_VERSION = 1;

  @Override
  public EvidenceKind supportedKind() {
    return EvidenceKind.METHOD;
  }

  @Override
  public int mapperVersion() {
    return MAPPER_VERSION;
  }

  @Override
  public MappingResult map(EvidenceItem item, MappingContext context) {
    MethodElement element =
        new MethodElement(
            ElementIdentityDeriver.fromEvidenceId(item.id()),
            item.id().scopeKey(),
            ObservedProvenanceFactory.fromEvidence(item.id(), context.constructionTimestamp()),
            NativeAttributes.empty(),
            Optional.empty());
    return MappingResult.ofElement(element);
  }
}
