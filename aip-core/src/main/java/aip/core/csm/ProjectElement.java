package aip.core.csm;

import java.util.Objects;

/**
 * A CSM {@code Project} element, contained by a {@link RepositoryElement}
 * (see {@code Containment Relationships Between Structural Entities}).
 */
public record ProjectElement(
    CsmElementId id, String name, ProvenanceRecord provenance, NativeAttributes nativeAttributes)
    implements CsmElement {

  public ProjectElement {
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
    return CsmEntityKind.PROJECT;
  }
}
