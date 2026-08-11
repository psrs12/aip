package aip.csmbuilder.mapping;

import aip.core.evidence.EvidenceKind;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The Evidence-Kind Mapper Registry: registration and kind-based
 * lookup of {@link EvidenceKindMapper}s, per
 * {@code openspec/changes/implement-csm-builder/design.md} Decision 1.
 *
 * <p>At most one Mapper may be registered per {@link EvidenceKind} —
 * registering a second Mapper for an already-registered kind is
 * rejected rather than silently overwriting the first, since a silent
 * overwrite would be exactly the kind of ambiguous, unreviewed
 * behavior change the rest of this codebase's invariants exist to
 * prevent. This also makes it straightforward for a later exclusion
 * guard (e.g. {@code ConfigFile}/{@code ConfigReference} — see
 * {@code Exclusion of Configuration Reference Representation}) to
 * assert that no Mapper is registered for a given kind at all.
 */
public final class EvidenceKindMapperRegistry {

  private final Map<EvidenceKind, EvidenceKindMapper> mappersByKind = new LinkedHashMap<>();

  public void register(EvidenceKindMapper mapper) {
    Objects.requireNonNull(mapper, "mapper");
    EvidenceKind kind = mapper.supportedKind();
    if (mappersByKind.containsKey(kind)) {
      throw new IllegalStateException(
          "a Mapper is already registered for Evidence kind " + kind + "; registering a second"
              + " Mapper for the same kind is not permitted");
    }
    mappersByKind.put(kind, mapper);
  }

  public Optional<EvidenceKindMapper> lookup(EvidenceKind kind) {
    Objects.requireNonNull(kind, "kind");
    return Optional.ofNullable(mappersByKind.get(kind));
  }

  public Set<EvidenceKind> registeredKinds() {
    return Set.copyOf(mappersByKind.keySet());
  }
}
