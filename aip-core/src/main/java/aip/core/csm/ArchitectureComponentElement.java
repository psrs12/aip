package aip.core.csm;

import java.util.List;
import java.util.Objects;

/**
 * A CSM {@code Architecture Component} — a named grouping composed of
 * one or more existing structural entities, per the
 * {@code Architecture Component Representation} requirement.
 *
 * <p>Its provenance classification SHALL always be
 * {@link ProvenanceCategory#DECLARED} or {@link ProvenanceCategory#INFERRED},
 * and SHALL NOT be {@link ProvenanceCategory#OBSERVED} — enforced here
 * at construction time, not left to a downstream validator, per
 * {@code Component provenance is never observed}: "validation SHALL
 * reject a component classified as observed."
 *
 * <p>CSM Builder (per its own specification's {@code Exclusion of
 * Architectural Inference and Declared-Knowledge Construction}
 * requirement) never constructs an instance of this type — it exists
 * in this domain model for completeness with the archived CSM
 * specification, for a future declared-knowledge or inference
 * capability to use.
 *
 * @param composition the one or more existing {@link ModuleElement} or
 *     {@link PackageElement} identities this component is composed
 *     of. Never empty — a component references existing structure
 *     rather than duplicating it (see {@code Component composed of
 *     existing structural elements}).
 */
public record ArchitectureComponentElement(
    CsmElementId id,
    String name,
    ProvenanceRecord provenance,
    NativeAttributes nativeAttributes,
    List<CsmElementId> composition)
    implements CsmElement {

  public ArchitectureComponentElement {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(name, "name");
    if (name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    Objects.requireNonNull(provenance, "provenance");
    if (provenance.category() == ProvenanceCategory.OBSERVED) {
      throw new IllegalArgumentException(
          "an Architecture Component's provenance SHALL NOT be OBSERVED (Component provenance is"
              + " never observed)");
    }
    Objects.requireNonNull(nativeAttributes, "nativeAttributes");
    Objects.requireNonNull(composition, "composition");
    if (composition.isEmpty()) {
      throw new IllegalArgumentException(
          "an Architecture Component's composition SHALL reference one or more existing"
              + " elements");
    }
    composition = List.copyOf(composition);
  }

  @Override
  public CsmEntityKind kind() {
    return CsmEntityKind.ARCHITECTURE_COMPONENT;
  }
}
