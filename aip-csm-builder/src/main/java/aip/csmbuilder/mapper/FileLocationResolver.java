package aip.csmbuilder.mapper;

import aip.core.csm.SourceLocation;
import aip.core.evidence.EvidenceId;
import aip.core.evidence.EvidenceItem;
import aip.core.evidence.EvidenceLocation;
import aip.core.evidence.EvidenceRelationshipType;
import aip.csmbuilder.mapping.EvidenceRelationshipLookup;
import aip.csmbuilder.mapping.MappingContext;
import java.util.Optional;

/**
 * Resolves a {@code Type}/{@code Method} element's source-location
 * attribute from its File Evidence Item, per {@code File Evidence
 * Becomes a Location Attribute, Not a Relationship}: the File Evidence
 * Item — found via the {@code REFERENCE} relationship a {@code SourceUnit}
 * or Method-level Evidence Item carries back to it (see
 * {@code File Evidence and Kind-Specific Layering}) — is the
 * authoritative source of location data, not any location an Evidence
 * Item might otherwise carry on itself directly.
 *
 * <p>Shared by {@link TypeMapper} and {@link MethodMapper} so both stay
 * consistent rather than each re-implementing this traversal.
 */
final class FileLocationResolver {

  private FileLocationResolver() {}

  static Optional<SourceLocation> resolve(EvidenceItem item, MappingContext context) {
    Optional<EvidenceId> fileEvidenceId =
        EvidenceRelationshipLookup.findSingleTarget(
            context.evidenceModel(), EvidenceRelationshipType.REFERENCE, item.id());
    if (fileEvidenceId.isEmpty()) {
      return Optional.empty();
    }

    Optional<EvidenceItem> fileItem = context.evidenceModel().find(fileEvidenceId.get());
    if (fileItem.isEmpty()) {
      return Optional.empty();
    }

    Optional<EvidenceLocation> location = fileItem.get().currentLocation();
    return location.map(loc -> new SourceLocation(loc.filePath(), loc.position()));
  }
}
