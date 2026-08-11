package aip.core.csm;

import java.util.Objects;

/**
 * A CSM {@code Module} element, contained by a {@link ProjectElement}
 * (see {@code Containment Relationships Between Structural Entities}).
 */
public record ModuleElement(
    CsmElementId id, String name, ProvenanceRecord provenance, NativeAttributes nativeAttributes)
    implements CsmElement {

  public ModuleElement {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(name, "name");
    if (name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    Objects.requireNonNull(provenance, "provenance");
    Objects.requireNonNull(nativeAttributes, "nativeAttributes");
  }

  @Override
  public CsmEntityKind kind() {
    return CsmEntityKind.MODULE;
  }
}
