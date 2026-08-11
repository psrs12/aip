package aip.core.csm;

import java.util.Objects;
import java.util.Optional;

/**
 * A CSM {@code Method} element. Instantiation is optional per language
 * analyzer (see {@code Method-Level Representation Capability}):
 * absence of Method-level elements SHALL NOT, by itself, invalidate a
 * CSM containing only {@link TypeElement}s.
 *
 * <p>Where present, a Method is contained by (declared under) its
 * {@link TypeElement} (see {@code Containment Relationships Between
 * Structural Entities}).
 *
 * @param sourceLocation an optional source-location reference — see
 *     {@link TypeElement#sourceLocation()}.
 */
public record MethodElement(
    CsmElementId id,
    String name,
    ProvenanceRecord provenance,
    NativeAttributes nativeAttributes,
    Optional<SourceLocation> sourceLocation)
    implements CsmElement {

  public MethodElement {
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
    return CsmEntityKind.METHOD;
  }
}
