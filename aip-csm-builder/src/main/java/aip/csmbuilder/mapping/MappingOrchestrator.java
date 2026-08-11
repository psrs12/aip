package aip.csmbuilder.mapping;

import aip.core.csm.CsmElementId;
import aip.core.csm.CsmRelationship;
import aip.core.evidence.EvidenceId;
import aip.core.evidence.EvidenceItem;
import aip.core.evidence.RepositoryEvidenceModel;
import aip.csmbuilder.dependency.DependencyKindClassifier;
import aip.csmbuilder.dependency.ResourceDependencyKindClassifier;
import aip.csmbuilder.provenance.ProvenanceGuard;
import java.time.Clock;
import java.time.Instant;
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
 *
 * <p>After every Evidence Item has been dispatched, {@link
 * ContainmentRelationshipBuilder}, {@link DependencyRelationshipBuilder},
 * and {@link ExternalSystemRelationshipBuilder} construct CSM
 * {@code CONTAINMENT}, {@code dependency}, and {@code integration}/
 * {@code External System} content from the Evidence Model's own
 * structure (tasks.md 8.1, 10.4, 11.1-11.2) — a distinct step from
 * per-item Mapper dispatch, since each is derived from relationships
 * between (or grouped across) Evidence Items, not an Evidence Item in
 * its own right. {@link ExternalSystemRelationshipBuilder} determines
 * "unresolved" independently, from the same {@code resolvedIds} map
 * Mapper dispatch populated — it does not depend on
 * {@link DependencyRelationshipBuilder}'s output, though it
 * conventionally runs after it.
 *
 * <p>{@link ExcludedRelationshipTypeGuard} verifies the final
 * accumulated result before it is returned, guarding the {@code
 * implementation/extension}/{@code invocation} relationship-type
 * exclusion (tasks.md 13.2) the same way {@link EvidenceKindMapperRegistry}
 * guards the {@code ConfigFile}/{@code ConfigReference} exclusion at
 * registration time (tasks.md 13.1) — two different points in this
 * class's lifecycle, since one is a registration-time property and the
 * other is a property of what was actually constructed.
 */
public final class MappingOrchestrator {

  private final EvidenceKindMapperRegistry registry;
  private final Clock clock;
  private final DependencyKindClassifier dependencyKindClassifier;

  public MappingOrchestrator(EvidenceKindMapperRegistry registry) {
    this(registry, Clock.systemUTC());
  }

  public MappingOrchestrator(EvidenceKindMapperRegistry registry, Clock clock) {
    this(registry, clock, ResourceDependencyKindClassifier.loadDefaults());
  }

  public MappingOrchestrator(
      EvidenceKindMapperRegistry registry, Clock clock, DependencyKindClassifier dependencyKindClassifier) {
    this.registry = Objects.requireNonNull(registry, "registry");
    this.clock = Objects.requireNonNull(clock, "clock");
    this.dependencyKindClassifier =
        Objects.requireNonNull(dependencyKindClassifier, "dependencyKindClassifier");
  }

  public MappingResult construct(RepositoryEvidenceModel evidenceModel) {
    Objects.requireNonNull(evidenceModel, "evidenceModel");

    List<EvidenceItem> orderedItems =
        evidenceModel.items().values().stream()
            .sorted(Comparator.comparing(item -> item.id().toString()))
            .toList();

    Instant constructionTimestamp = clock.instant();
    Map<EvidenceId, CsmElementId> resolvedIds = new LinkedHashMap<>();
    MutableMappingContext context =
        new MutableMappingContext(evidenceModel, resolvedIds, constructionTimestamp);

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

    List<CsmRelationship> containmentRelationships =
        ContainmentRelationshipBuilder.build(evidenceModel, resolvedIds, constructionTimestamp);
    accumulated = accumulated.merge(new MappingResult(List.of(), containmentRelationships));

    List<CsmRelationship> dependencyRelationships =
        DependencyRelationshipBuilder.build(
            evidenceModel, resolvedIds, dependencyKindClassifier, constructionTimestamp);
    accumulated = accumulated.merge(new MappingResult(List.of(), dependencyRelationships));

    MappingResult externalSystemResult =
        ExternalSystemRelationshipBuilder.build(evidenceModel, resolvedIds, constructionTimestamp);
    accumulated = accumulated.merge(externalSystemResult);

    ExcludedRelationshipTypeGuard.verify(accumulated);

    return accumulated;
  }

  private record MutableMappingContext(
      RepositoryEvidenceModel evidenceModel,
      Map<EvidenceId, CsmElementId> resolvedIds,
      Instant constructionTimestamp)
      implements MappingContext {

    @Override
    public Optional<CsmElementId> resolvedElementId(EvidenceId evidenceId) {
      return Optional.ofNullable(resolvedIds.get(evidenceId));
    }
  }
}
