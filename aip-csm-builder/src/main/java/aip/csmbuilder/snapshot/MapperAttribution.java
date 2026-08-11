package aip.csmbuilder.snapshot;

import java.util.Objects;

/**
 * CSM Builder's own internal bookkeeping about which producer (an
 * Evidence-Kind Mapper, or an equivalent internal construction step
 * such as {@code ExternalSystemRelationshipBuilder}, which produces
 * {@code External System} elements without going through the
 * per-Evidence-Item Mapper dispatch model) constructed a given CSM
 * element, and at what version — per {@code design.md} Decision 2's
 * Snapshot Manifest: "the Evidence-Kind Mapper and Mapper version that
 * produced it." This is not CSM content and not new CSM vocabulary; it
 * exists purely so a future snapshot can determine, for a previously
 * constructed element, whether the thing that built it has since
 * changed (see {@code CSM Builder Mapper Versioning}, Section 17).
 *
 * @param mapperIdentifier a stable identifier for the producer — by
 *     convention, a registered {@link aip.csmbuilder.mapping.EvidenceKindMapper}
 *     uses its {@link aip.csmbuilder.mapping.EvidenceKindMapper#supportedKind()}
 *     name, and a batch construction step uses its own fixed class-like
 *     name (e.g. {@code "EXTERNAL_SYSTEM"}). Deliberately a plain
 *     string, not tied to {@code EvidenceKind}, since not every
 *     producer of CSM elements is a registered per-item Mapper.
 * @param mapperVersion the producer's monotonically increasing version
 *     at the time it constructed this element.
 */
public record MapperAttribution(String mapperIdentifier, int mapperVersion) {

  public MapperAttribution {
    Objects.requireNonNull(mapperIdentifier, "mapperIdentifier");
    if (mapperIdentifier.isBlank()) {
      throw new IllegalArgumentException("mapperIdentifier must not be blank");
    }
    if (mapperVersion < 1) {
      throw new IllegalArgumentException("mapperVersion must be at least 1");
    }
  }
}
