package aip.csmbuilder.mapping;

import aip.core.csm.CsmElement;
import aip.core.csm.CsmElementId;
import aip.core.csm.CsmRelationship;
import aip.core.evidence.ChangeStatus;
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
 * <p>An Evidence Item whose discovery outcome is {@code failed} is
 * likewise skipped before dispatch, per {@link FailedEvidenceFilter}
 * (tasks.md 14.2) — a {@code partial} discovery outcome is dispatched
 * normally, since {@code Partial-Evidence Construction} requires
 * constructing from whatever structure was successfully captured, with
 * ordinary {@code observed} provenance and no incompleteness marker;
 * no Mapper reads {@code discoveryOutcome} at all, so this falls out
 * for free once {@code failed} items are excluded upstream.
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
 *
 * <p>{@link #construct(RepositoryEvidenceModel, Map, PriorElementLookup)}
 * adds incremental scoping on top of the same dispatch loop (tasks.md
 * Section 16): an Evidence Item classified {@code REMOVED} is skipped
 * unconditionally; one classified {@code UNCHANGED} is carried forward
 * from {@link PriorElementLookup} without invoking its Mapper, when a
 * prior element is known. {@link #construct(RepositoryEvidenceModel)}
 * is the ordinary, non-incremental case: every item defaults to {@code
 * ADDED} and no prior element is ever known.
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

  /**
   * Ordinary, non-incremental construction: every Evidence Item is
   * treated as {@link ChangeStatus#ADDED} and no prior snapshot exists
   * (see {@link #construct(RepositoryEvidenceModel, Map, PriorElementLookup)}).
   */
  public MappingResult construct(RepositoryEvidenceModel evidenceModel) {
    return construct(evidenceModel, Map.of(), PriorElementLookup.none());
  }

  /**
   * Incremental construction, per {@code Incremental Snapshot Scope}
   * and {@code Evidence Lifecycle Interaction} (tasks.md 16.1, 16.2).
   *
   * @param changeStatuses each Evidence Item's per-run change-status
   *     classification, by identity. An identity absent from this map
   *     is treated as {@link ChangeStatus#ADDED} — the same as ordinary
   *     construction's default — so a caller only needs to populate
   *     entries for items whose status is meaningful to report.
   * @param priorElements the previous construction run's per-Evidence-Item
   *     elements, consulted only for {@link ChangeStatus#UNCHANGED}
   *     items. Pass {@link PriorElementLookup#none()} when no prior
   *     snapshot exists (equivalent to full construction).
   */
  public MappingResult construct(
      RepositoryEvidenceModel evidenceModel,
      Map<EvidenceId, ChangeStatus> changeStatuses,
      PriorElementLookup priorElements) {
    Objects.requireNonNull(evidenceModel, "evidenceModel");
    Objects.requireNonNull(changeStatuses, "changeStatuses");
    Objects.requireNonNull(priorElements, "priorElements");

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
      if (!FailedEvidenceFilter.isEligible(item)) {
        // Failed-Evidence Non-Construction (tasks.md 14.2): a `failed`
        // discovery outcome yields no CSM element, unconditionally -
        // not even to a Mapper that would otherwise be found for it.
        continue;
      }

      ChangeStatus changeStatus = changeStatuses.getOrDefault(item.id(), ChangeStatus.ADDED);
      if (changeStatus == ChangeStatus.REMOVED) {
        // Evidence Lifecycle Interaction (tasks.md 16.2): omitted from
        // this snapshot unconditionally - whether the Evidence Item is
        // now TOMBSTONED or already PURGED makes no difference, since
        // this branch never even inspects lifecycle state. Omission is
        // simply "never added to `accumulated`" - no explicit deletion
        // is needed.
        continue;
      }

      Optional<EvidenceKindMapper> mapper = registry.lookup(item.kind());
      if (mapper.isEmpty()) {
        continue;
      }

      if (changeStatus == ChangeStatus.UNCHANGED) {
        Optional<CsmElement> carried = priorElements.find(item.id());
        if (carried.isPresent()) {
          // Incremental Snapshot Scope (tasks.md 16.1): carried forward
          // exactly as previously constructed, without invoking the
          // Mapper again.
          resolvedIds.put(item.id(), carried.get().id());
          accumulated = accumulated.merge(MappingResult.ofElement(carried.get()));
          continue;
        }
        // No prior element is known for this identity (e.g. no prior
        // snapshot at all) - fall through and construct fresh, the
        // same as ADDED/MODIFIED. This also covers a Mapper that
        // produces no element at all (e.g. ApiContractMapper, which
        // only ever produces a relationship): PriorElementLookup never
        // has an entry for it, so it is always (re)invoked regardless
        // of change status - Incremental Snapshot Scope's guarantee is
        // scoped to CSM elements, not relationships.
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
