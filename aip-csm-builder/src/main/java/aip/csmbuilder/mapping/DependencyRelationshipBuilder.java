package aip.csmbuilder.mapping;

import aip.core.csm.CsmElementId;
import aip.core.csm.CsmRelationship;
import aip.core.csm.CsmRelationshipType;
import aip.core.csm.DependencyKind;
import aip.core.csm.NativeAttributes;
import aip.core.csm.ProvenanceRecord;
import aip.core.evidence.EvidenceId;
import aip.core.evidence.EvidenceItem;
import aip.core.evidence.EvidenceKind;
import aip.core.evidence.RepositoryEvidenceModel;
import aip.csmbuilder.dependency.DependencyKindClassifier;
import aip.csmbuilder.identity.ElementIdentityDeriver;
import aip.csmbuilder.provenance.ObservedProvenanceFactory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Constructs CSM {@code dependency} relationships from
 * {@code ManifestDependencyEdge} and {@code ImportEdge} Evidence
 * Items, per {@code Dependency Relationship Construction}: "CSM
 * Builder SHALL construct exactly one CSM {@code dependency}
 * relationship per distinct (source, target) pair evidenced by a
 * {@code ManifestDependencyEdge}, an {@code ImportEdge}, or both."
 *
 * <p>Like {@link ContainmentRelationshipBuilder}, this does not fit the
 * per-item {@link EvidenceKindMapper} dispatch model — merging two
 * distinct Evidence kinds into a single relationship requires seeing
 * every {@code ManifestDependencyEdge}/{@code ImportEdge} Evidence Item
 * together, grouped by (source, target), not one item at a time.
 * {@link MappingOrchestrator} invokes this directly, once, after every
 * Evidence Item has been dispatched.
 *
 * <p>Only internal (Module-to-Module) dependencies are constructed
 * here — a target that does not resolve to a known Module (an
 * unresolved external artifact coordinate) produces no relationship in
 * this section; representing it as a CSM {@code External System} is
 * {@link ExternalSystemRelationshipBuilder}'s concern.
 */
final class DependencyRelationshipBuilder {

  private DependencyRelationshipBuilder() {}

  static List<CsmRelationship> build(
      RepositoryEvidenceModel evidenceModel,
      Map<EvidenceId, CsmElementId> resolvedElementIds,
      DependencyKindClassifier classifier,
      Instant constructionTimestamp) {

    Map<DependencyEndpoints, List<EvidenceItem>> grouped = groupByEndpoints(evidenceModel);

    List<CsmRelationship> relationships = new ArrayList<>();
    for (Map.Entry<DependencyEndpoints, List<EvidenceItem>> entry : grouped.entrySet()) {
      DependencyEndpoints endpoints = entry.getKey();
      List<EvidenceItem> evidenceGroup = entry.getValue();

      Optional<CsmElementId> sourceId =
          Optional.ofNullable(resolvedElementIds.get(endpoints.sourceModuleId()));
      Optional<CsmElementId> targetId =
          Optional.ofNullable(resolvedElementIds.get(endpoints.targetModuleId()));
      if (sourceId.isEmpty() || targetId.isEmpty()) {
        // Source not mapped, or target is not a known internal Module
        // (an unresolved external artifact) - the latter is Section
        // 11's concern, not constructed here.
        continue;
      }

      Optional<DependencyKind> kind = classifyGroup(evidenceGroup, classifier);
      List<EvidenceId> evidenceIds = evidenceGroup.stream().map(EvidenceItem::id).toList();
      ProvenanceRecord provenance =
          ObservedProvenanceFactory.fromEvidence(evidenceIds, constructionTimestamp);
      CsmElementId relationshipId =
          ElementIdentityDeriver.forRelationship(sourceId.get(), targetId.get(), CsmRelationshipType.DEPENDENCY);

      relationships.add(
          new CsmRelationship(
              relationshipId,
              CsmRelationshipType.DEPENDENCY,
              sourceId.get(),
              Optional.of(targetId.get()),
              provenance,
              NativeAttributes.empty(),
              kind));
    }

    return relationships;
  }

  private static Map<DependencyEndpoints, List<EvidenceItem>> groupByEndpoints(
      RepositoryEvidenceModel evidenceModel) {
    Map<DependencyEndpoints, List<EvidenceItem>> grouped = new LinkedHashMap<>();
    for (EvidenceItem item : evidenceModel.items().values()) {
      if (item.kind() != EvidenceKind.MANIFEST_DEPENDENCY_EDGE && item.kind() != EvidenceKind.IMPORT_EDGE) {
        continue;
      }

      String repositoryIdentifier = item.id().repositoryIdentifier();
      String sourceModuleScopeKey =
          item.attributes()
              .get(EvidenceAttributeKeys.DEPENDENCY_SOURCE_MODULE)
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          item.kind() + " Evidence Item " + item.id() + " is missing its '"
                              + EvidenceAttributeKeys.DEPENDENCY_SOURCE_MODULE
                              + "' attribute"));
      String targetScopeKey =
          item.attributes()
              .get(EvidenceAttributeKeys.DEPENDENCY_TARGET)
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          item.kind() + " Evidence Item " + item.id() + " is missing its '"
                              + EvidenceAttributeKeys.DEPENDENCY_TARGET
                              + "' attribute"));

      DependencyEndpoints endpoints =
          new DependencyEndpoints(
              new EvidenceId(repositoryIdentifier, EvidenceKind.MODULE, sourceModuleScopeKey),
              new EvidenceId(repositoryIdentifier, EvidenceKind.MODULE, targetScopeKey));
      grouped.computeIfAbsent(endpoints, key -> new ArrayList<>()).add(item);
    }
    return grouped;
  }

  private static Optional<DependencyKind> classifyGroup(
      List<EvidenceItem> group, DependencyKindClassifier classifier) {
    for (EvidenceItem item : group) {
      if (item.kind() != EvidenceKind.MANIFEST_DEPENDENCY_EDGE) {
        continue;
      }
      Optional<String> buildSystem = item.attributes().get(EvidenceAttributeKeys.BUILD_SYSTEM);
      Optional<String> nativeScope = item.attributes().get(EvidenceAttributeKeys.NATIVE_SCOPE);
      if (buildSystem.isPresent() && nativeScope.isPresent()) {
        return classifier.classify(buildSystem.get(), nativeScope.get());
      }
    }
    return Optional.empty();
  }

  /**
   * The (source Module, target) pair used to correlate a
   * {@code ManifestDependencyEdge} and an {@code ImportEdge} as
   * evidencing "the same two entities" (see {@code Dependency
   * Relationship Construction}). {@code targetModuleId} is always
   * constructed as a {@code MODULE}-kind identity even though the
   * target may in fact be an external artifact — such an identity
   * simply never appears in {@code resolvedElementIds}, so it resolves
   * to nothing and is skipped, exactly as intended.
   */
  private record DependencyEndpoints(EvidenceId sourceModuleId, EvidenceId targetModuleId) {}
}
