package aip.csmbuilder.mapping;

import aip.core.csm.CsmElementId;
import aip.core.evidence.EvidenceId;
import aip.core.evidence.EvidenceItem;
import aip.core.evidence.RepositoryEvidenceModel;
import aip.csmbuilder.provenance.ProvenanceGuard;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Consumes a {@link RepositoryEvidenceModel} and dispatches each
 * Evidence Item to its registered {@link EvidenceKindMapper}, per
 * {@code Deterministic, Evidence-Driven Transformation Only} and
 * {@code openspec/changes/implement-csm-builder/design.md} Decision 4:
 * sequential execution only, in a fixed, deterministic order (sorted
 * by Evidence identity), so two runs against unchanged input produce
 * byte-identical construction order — not merely identical final
 * content. No concurrency infrastructure is used (invariant 7).
 *
 * <p>An Evidence Item whose kind has no registered Mapper is skipped —
 * not an error. This is how the {@code ConfigFile}/{@code ConfigReference}
 * exclusion (Section 13) and the {@code implementation/extension}/
 * {@code invocation} relationship-type exclusion are ultimately
 * realized: by simply never registering a Mapper for those kinds,
 * rather than this class special-casing them.
 *
 * <p>Every Mapper's result is verified by {@link ProvenanceGuard}
 * before being merged into the accumulated result — this holds
 * regardless of which Mapper produced it, so an individual Mapper
 * implementation being careless does not compromise the
 * {@code observed}-only and traceability guarantees CSM Builder as a
 * whole makes (tasks.md 6.2, 6.3).
 */
public final class MappingOrchestrator {

  private final EvidenceKindMapperRegistry registry;

  public MappingOrchestrator(EvidenceKindMapperRegistry registry) {
    this.registry = Objects.requireNonNull(registry, "registry");
  }

  public MappingResult construct(RepositoryEvidenceModel evidenceModel) {
    Objects.requireNonNull(evidenceModel, "evidenceModel");

    List<EvidenceItem> orderedItems =
        evidenceModel.items().values().stream()
            .sorted(Comparator.comparing(item -> item.id().toString()))
            .toList();

    Map<EvidenceId, CsmElementId> resolvedIds = new LinkedHashMap<>();
    MutableMappingContext context = new MutableMappingContext(evidenceModel, resolvedIds);

    MappingResult accumulated = MappingResult.empty();
    for (EvidenceItem item : orderedItems) {
      Optional<EvidenceKindMapper> mapper = registry.lookup(item.kind());
      if (mapper.isEmpty()) {
        continue;
      }
      MappingResult result = mapper.get().map(item, context);
      ProvenanceGuard.verify(result, item);
      if (!result.elements().isEmpty()) {
        resolvedIds.put(item.id(), result.elements().get(0).id());
      }
      accumulated = accumulated.merge(result);
    }

    return accumulated;
  }

  private record MutableMappingContext(
      RepositoryEvidenceModel evidenceModel, Map<EvidenceId, CsmElementId> resolvedIds)
      implements MappingContext {

    @Override
    public Optional<CsmElementId> resolvedElementId(EvidenceId evidenceId) {
      return Optional.ofNullable(resolvedIds.get(evidenceId));
    }
  }
}
