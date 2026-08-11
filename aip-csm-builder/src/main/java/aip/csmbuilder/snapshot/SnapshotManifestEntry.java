package aip.csmbuilder.snapshot;

import aip.core.csm.CsmElementId;
import java.util.Objects;

/**
 * One Snapshot Manifest row, per {@code design.md} Decision 2: "a small
 * index recording, per CSM element identity: the Evidence-Kind Mapper
 * and Mapper version that produced it, the originating Repository
 * Evidence identity/identities, and a pointer into this snapshot's
 * content files."
 *
 * @param originatingEvidenceReference the originating Evidence
 *     identity/identities exactly as recorded on the element's own
 *     {@link aip.core.csm.ProvenanceRecord#sourceReference()} — this
 *     manifest does not re-derive or re-parse that reference, only
 *     indexes it alongside the element's identity for lookup.
 * @param contentLocation a pointer into this snapshot's content files
 *     (e.g. {@code "elements.tsv:14"}), assigned by the {@link
 *     SnapshotStore} implementation at write time — never supplied by
 *     the caller.
 */
public record SnapshotManifestEntry(
    CsmElementId elementId,
    String mapperIdentifier,
    int mapperVersion,
    String originatingEvidenceReference,
    String contentLocation) {

  public SnapshotManifestEntry {
    Objects.requireNonNull(elementId, "elementId");
    Objects.requireNonNull(mapperIdentifier, "mapperIdentifier");
    if (mapperIdentifier.isBlank()) {
      throw new IllegalArgumentException("mapperIdentifier must not be blank");
    }
    if (mapperVersion < 1) {
      throw new IllegalArgumentException("mapperVersion must be at least 1");
    }
    Objects.requireNonNull(originatingEvidenceReference, "originatingEvidenceReference");
    if (originatingEvidenceReference.isBlank()) {
      throw new IllegalArgumentException("originatingEvidenceReference must not be blank");
    }
    Objects.requireNonNull(contentLocation, "contentLocation");
    if (contentLocation.isBlank()) {
      throw new IllegalArgumentException("contentLocation must not be blank");
    }
  }
}
