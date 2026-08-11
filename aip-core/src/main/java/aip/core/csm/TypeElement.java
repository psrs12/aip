package aip.core.csm;

import java.util.Objects;
import java.util.Optional;

/**
 * A CSM {@code Type} element, contained by a {@link PackageElement}
 * (see {@code Containment Relationships Between Structural Entities}).
 * The minimum required capability for any language analyzer's
 * contribution to the CSM (see {@code Method-Level Representation
 * Capability}).
 *
 * @param sourceLocation an optional source-location reference (see
 *     {@link SourceLocation} and {@code Source File Representation}) —
 *     never a relationship to a Source File entity, since Source File
 *     is not part of {@link CsmEntityKind}.
 */
public record TypeElement(
    CsmElementId id,
    String name,
    ProvenanceRecord provenance,
    NativeAttributes nativeAttributes,
    Optional<SourceLocation> sourceLocation)
    implements CsmElement {

  public TypeElement {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(name, "name");
    if (name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    Objects.requireNonNull(provenance, "provenance");
    Objects.requireNonNull(nativeAttributes, "nativeAttributes");
    Objects.requireNonNull(sourceLocation, "sourceLocation");
  }

  @Override
  public CsmEntityKind kind() {
    return CsmEntityKind.TYPE;
  }
}
