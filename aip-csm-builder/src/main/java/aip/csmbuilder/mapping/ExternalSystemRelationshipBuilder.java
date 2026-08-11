package aip.csmbuilder.mapping;

import aip.core.csm.CsmElement;
import aip.core.csm.CsmElementId;
import aip.core.csm.CsmRelationship;
import aip.core.csm.CsmRelationshipType;
import aip.core.csm.ExternalSystemElement;
import aip.core.csm.NativeAttributes;
import aip.core.csm.ProvenanceRecord;
import aip.core.evidence.EvidenceId;
import aip.core.evidence.EvidenceItem;
import aip.core.evidence.EvidenceKind;
import aip.core.evidence.RepositoryEvidenceModel;
import aip.csmbuilder.identity.ElementIdentityDeriver;
import aip.csmbuilder.provenance.ObservedProvenanceFactory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Constructs CSM {@code External System} elements and the {@code
 * integration} relationships connecting them to their source
 * {@code Module}, from {@code ManifestDependencyEdge} Evidence Items
 * whose target artifact coordinate does not correspond to any
 * {@code Project} or {@code Module} Evidence Item in the repository's
 * Repository Evidence Model, per {@code External System Construction
 * for Unresolved Dependencies}.
 *
 * <p>Only {@code ManifestDependencyEdge} evidence is considered here —
 * an {@code ImportEdge}'s target is a source-level reference, not an
 * artifact coordinate, so it carries no information about an external
 * system's identity.
 *
 * <p>Like {@link DependencyRelationshipBuilder}, this does not fit the
 * per-item {@link EvidenceKindMapper} dispatch model — recognizing a
 * target as "unresolved" requires the full set of resolved internal
 * identities, and two edges may name the same external artifact
 * coordinate and must resolve to the same External System element even
 * when they come from different source Modules. {@link
 * MappingOrchestrator} invokes this directly, once, after every
 * Evidence Item has been dispatched.
 */
final class ExternalSystemRelationshipBuilder {

  private ExternalSystemRelationshipBuilder() {}

  static MappingResult build(
      RepositoryEvidenceModel evidenceModel,
      Map<EvidenceId, CsmElementId> resolvedElementIds,
      Instant constructionTimestamp) {

    List<EvidenceItem> unresolvedEdges = unresolvedManifestEdges(evidenceModel, resolvedElementIds);

    List<CsmElement> elements =
        buildExternalSystemElements(unresolvedEdges, constructionTimestamp);
    List<CsmRelationship> relationships =
        buildIntegrationRelationships(unresolvedEdges, resolvedElementIds, constructionTimestamp);

    return new MappingResult(elements, relationships);
  }

  /**
   * One {@code External System} element per distinct (repository,
   * coordinate) — independent of which, or how many, source Modules
   * depend on it — with provenance sourced from every unresolved edge
   * naming that coordinate.
   */
  private static List<CsmElement> buildExternalSystemElements(
      List<EvidenceItem> unresolvedEdges, Instant constructionTimestamp) {
    Map<ExternalCoordinate, List<EvidenceItem>> byCoordinate = new LinkedHashMap<>();
    for (EvidenceItem edge : unresolvedEdges) {
      byCoordinate.computeIfAbsent(coordinateOf(edge), key -> new ArrayList<>()).add(edge);
    }

    List<CsmElement> elements = new ArrayList<>();
    for (Map.Entry<ExternalCoordinate, List<EvidenceItem>> entry : byCoordinate.entrySet()) {
      ExternalCoordinate coordinate = entry.getKey();
      List<EvidenceId> evidenceIds = entry.getValue().stream().map(EvidenceItem::id).toList();
      ProvenanceRecord provenance =
          ObservedProvenanceFactory.fromEvidence(evidenceIds, constructionTimestamp);
      CsmElementId id =
          ElementIdentityDeriver.forExternalSystem(
              coordinate.repositoryIdentifier(), coordinate.artifactCoordinate());
      elements.add(ExternalSystemElement.unresolved(id, coordinate.artifactCoordinate(), provenance));
    }
    return elements;
  }

  /**
   * One {@code integration} relationship per distinct (source Module,
   * coordinate) pair — merging multiple edges evidencing the same pair
   * into one relationship, mirroring {@link DependencyRelationshipBuilder}.
   */
  private static List<CsmRelationship> buildIntegrationRelationships(
      List<EvidenceItem> unresolvedEdges,
      Map<EvidenceId, CsmElementId> resolvedElementIds,
      Instant constructionTimestamp) {
    Map<SourceAndCoordinate, List<EvidenceItem>> bySourceAndCoordinate = new LinkedHashMap<>();
    for (EvidenceItem edge : unresolvedEdges) {
      SourceAndCoordinate key = new SourceAndCoordinate(sourceModuleIdOf(edge), coordinateOf(edge));
      bySourceAndCoordinate.computeIfAbsent(key, k -> new ArrayList<>()).add(edge);
    }

    List<CsmRelationship> relationships = new ArrayList<>();
    for (Map.Entry<SourceAndCoordinate, List<EvidenceItem>> entry : bySourceAndCoordinate.entrySet()) {
      SourceAndCoordinate key = entry.getKey();
      Optional<CsmElementId> sourceId = Optional.ofNullable(resolvedElementIds.get(key.sourceModuleId()));
      if (sourceId.isEmpty()) {
        // Source Module itself was never mapped - nothing to connect.
        continue;
      }

      List<EvidenceId> evidenceIds = entry.getValue().stream().map(EvidenceItem::id).toList();
      ProvenanceRecord provenance =
          ObservedProvenanceFactory.fromEvidence(evidenceIds, constructionTimestamp);
      CsmElementId externalSystemId =
          ElementIdentityDeriver.forExternalSystem(
              key.coordinate().repositoryIdentifier(), key.coordinate().artifactCoordinate());
      CsmElementId relationshipId =
          ElementIdentityDeriver.forRelationship(
              sourceId.get(), externalSystemId, CsmRelationshipType.INTEGRATION);

      relationships.add(
          CsmRelationship.of(
              relationshipId,
              CsmRelationshipType.INTEGRATION,
              sourceId.get(),
              externalSystemId,
              provenance,
              NativeAttributes.empty()));
    }
    return relationships;
  }

  private static List<EvidenceItem> unresolvedManifestEdges(
      RepositoryEvidenceModel evidenceModel, Map<EvidenceId, CsmElementId> resolvedElementIds) {
    List<EvidenceItem> unresolved = new ArrayList<>();
    for (EvidenceItem item : evidenceModel.items().values()) {
      if (item.kind() != EvidenceKind.MANIFEST_DEPENDENCY_EDGE) {
        continue;
      }
      String repositoryIdentifier = item.id().repositoryIdentifier();
      String targetScopeKey = requireAttribute(item, EvidenceAttributeKeys.DEPENDENCY_TARGET);
      if (!resolvesInternally(repositoryIdentifier, targetScopeKey, resolvedElementIds)) {
        unresolved.add(item);
      }
    }
    return unresolved;
  }

  /**
   * Mechanically checks whether {@code targetScopeKey} corresponds to
   * a known {@code Project} or {@code Module} Evidence Item that was
   * itself mapped to a CSM element — the "unresolved-target lookup"
   * of tasks.md 11.1.
   */
  private static boolean resolvesInternally(
      String repositoryIdentifier,
      String targetScopeKey,
      Map<EvidenceId, CsmElementId> resolvedElementIds) {
    return resolvedElementIds.containsKey(
            new EvidenceId(repositoryIdentifier, EvidenceKind.MODULE, targetScopeKey))
        || resolvedElementIds.containsKey(
            new EvidenceId(repositoryIdentifier, EvidenceKind.PROJECT, targetScopeKey));
  }

  private static EvidenceId sourceModuleIdOf(EvidenceItem edge) {
    String sourceModuleScopeKey = requireAttribute(edge, EvidenceAttributeKeys.DEPENDENCY_SOURCE_MODULE);
    return new EvidenceId(edge.id().repositoryIdentifier(), EvidenceKind.MODULE, sourceModuleScopeKey);
  }

  private static ExternalCoordinate coordinateOf(EvidenceItem edge) {
    String targetScopeKey = requireAttribute(edge, EvidenceAttributeKeys.DEPENDENCY_TARGET);
    return new ExternalCoordinate(edge.id().repositoryIdentifier(), targetScopeKey);
  }

  private static String requireAttribute(EvidenceItem item, String key) {
    return item.attributes()
        .get(key)
        .orElseThrow(
            () ->
                new IllegalStateException(
                    item.kind() + " Evidence Item " + item.id() + " is missing its '" + key + "' attribute"));
  }

  /** A distinct external artifact coordinate, scoped to its repository. */
  private record ExternalCoordinate(String repositoryIdentifier, String artifactCoordinate) {}

  /** A (source Module, external artifact coordinate) pair. */
  private record SourceAndCoordinate(EvidenceId sourceModuleId, ExternalCoordinate coordinate) {}
}
