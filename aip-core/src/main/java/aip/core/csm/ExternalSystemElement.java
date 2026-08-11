package aip.core.csm;

import java.util.Objects;
import java.util.Optional;

/**
 * A CSM {@code External System} — a boundary node with an opaque
 * interior and an attributed exterior, per the {@code External System
 * Representation} requirement: "at minimum: name, kind, criticality,
 * integration protocol, and owner, where known." The CSM SHALL NOT
 * attempt to represent an External System's internal structure — this
 * type has no Type, Method, or Package children, by construction.
 *
 * @param systemKind the external system's kind, where known (e.g. "
 *     third-party artifact", "SaaS integration") — named
 *     {@code systemKind} rather than {@code kind} to avoid colliding
 *     with {@link CsmElement#kind()}, which always returns
 *     {@link CsmEntityKind#EXTERNAL_SYSTEM} for this type.
 */
public record ExternalSystemElement(
    CsmElementId id,
    String name,
    ProvenanceRecord provenance,
    NativeAttributes nativeAttributes,
    Optional<String> systemKind,
    Optional<String> criticality,
    Optional<String> integrationProtocol,
    Optional<String> owner)
    implements CsmElement {

  public ExternalSystemElement {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(name, "name");
    if (name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    Objects.requireNonNull(provenance, "provenance");
    Objects.requireNonNull(nativeAttributes, "nativeAttributes");
    Objects.requireNonNull(systemKind, "systemKind");
    Objects.requireNonNull(criticality, "criticality");
    Objects.requireNonNull(integrationProtocol, "integrationProtocol");
    Objects.requireNonNull(owner, "owner");
  }

  /** An External System with only a name known — every attribute left unset. */
  public static ExternalSystemElement unresolved(
      CsmElementId id, String name, ProvenanceRecord provenance) {
    return new ExternalSystemElement(
        id,
        name,
        provenance,
        NativeAttributes.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
  }

  @Override
  public CsmEntityKind kind() {
    return CsmEntityKind.EXTERNAL_SYSTEM;
  }
}
