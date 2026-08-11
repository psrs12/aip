package aip.csmbuilder.mapper;

import aip.core.csm.NativeAttributes;
import aip.core.csm.TypeElement;
import aip.core.evidence.EvidenceItem;
import aip.core.evidence.EvidenceKind;
import aip.csmbuilder.identity.ElementIdentityDeriver;
import aip.csmbuilder.mapping.EvidenceKindMapper;
import aip.csmbuilder.mapping.MappingContext;
import aip.csmbuilder.mapping.MappingResult;
import aip.csmbuilder.provenance.ObservedProvenanceFactory;
import java.util.Optional;

/**
 * Maps a {@code SourceUnit} Evidence Item to a CSM {@link TypeElement},
 * per {@code Structural Entity Mapping}: every native construct kind
 * label (class, struct, interface, record, ...) maps uniformly onto
 * {@code Type} — never a distinct CSM entity kind per label.
 *
 * <p>The native construct kind label itself is preserved as a
 * {@code Type} attribute when present (see {@code Native Construct
 * Kind Preserved as Attribute}) — its absence is not treated as an
 * error here; Repository Understanding's own completeness is not this
 * Mapper's concern to enforce.
 *
 * <p>{@link TypeElement#sourceLocation()} is left empty by this
 * Mapper — populating it from File Evidence is Section 9's concern
 * (see {@code File Evidence Becomes a Location Attribute, Not a
 * Relationship}), not this one's.
 */
public final class TypeMapper implements EvidenceKindMapper {

  private static final int MAPPER_VERSION = 1;

  @Override
  public EvidenceKind supportedKind() {
    return EvidenceKind.SOURCE_UNIT;
  }

  @Override
  public int mapperVersion() {
    return MAPPER_VERSION;
  }

  @Override
  public MappingResult map(EvidenceItem item, MappingContext context) {
    NativeAttributes attributes = NativeAttributes.empty();
    Optional<String> nativeConstructKind =
        item.attributes().get(EvidenceAttributeKeys.NATIVE_CONSTRUCT_KIND);
    if (nativeConstructKind.isPresent()) {
      attributes = attributes.with(EvidenceAttributeKeys.NATIVE_CONSTRUCT_KIND, nativeConstructKind.get());
    }

    TypeElement element =
        new TypeElement(
            ElementIdentityDeriver.fromEvidenceId(item.id()),
            item.id().scopeKey(),
            ObservedProvenanceFactory.fromEvidence(item.id(), context.constructionTimestamp()),
            attributes,
            Optional.empty());
    return MappingResult.ofElement(element);
  }
}
