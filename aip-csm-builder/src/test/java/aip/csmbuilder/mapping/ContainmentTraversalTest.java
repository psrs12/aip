package aip.csmbuilder.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.core.csm.CsmElement;
import aip.core.csm.CsmElementId;
import aip.core.csm.CsmRelationship;
import aip.core.csm.CsmRelationshipType;
import aip.core.evidence.DiscoveryOutcome;
import aip.core.evidence.EvidenceAttributes;
import aip.core.evidence.EvidenceId;
import aip.core.evidence.EvidenceItem;
import aip.core.evidence.EvidenceKind;
import aip.core.evidence.EvidenceRelationship;
import aip.core.evidence.EvidenceRelationshipType;
import aip.core.evidence.ExtractionMethod;
import aip.core.evidence.RepositoryEvidenceModel;
import aip.csmbuilder.identity.ElementIdentityDeriver;
import aip.csmbuilder.mapper.MethodMapper;
import aip.csmbuilder.mapper.ModuleMapper;
import aip.csmbuilder.mapper.PackageMapper;
import aip.csmbuilder.mapper.ProjectMapper;
import aip.csmbuilder.mapper.RepositoryMapper;
import aip.csmbuilder.mapper.TypeMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Containment-traversal tests (tasks.md 8.2): the full
 * Repository→Project→Module→Package→Type→Method chain, the reserved
 * unmanaged Project and default Module cases, and graceful omission
 * when an endpoint has no registered Mapper.
 */
class ContainmentTraversalTest {

  private static final Clock FIXED_CLOCK = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);

  @Test
  void fullChainProducesContainmentRelationshipsAtEveryLevel() {
    EvidenceItem repository = evidenceItem(EvidenceKind.REPOSITORY, "repo-1", Map.of());
    EvidenceItem project = evidenceItem(EvidenceKind.PROJECT, "com.acme:parent", Map.of());
    EvidenceItem module = evidenceItem(EvidenceKind.MODULE, "com.acme:module-a", Map.of());
    EvidenceItem pkg =
        evidenceItem(
            EvidenceKind.PACKAGE,
            "com.acme:module-a:com.acme.pkg",
            Map.of(EvidenceAttributeKeys.NAMESPACE_NAME, "com.acme.pkg"));
    EvidenceItem type =
        evidenceItem(
            EvidenceKind.SOURCE_UNIT,
            "com.acme.pkg.MyType",
            Map.of(EvidenceAttributeKeys.NATIVE_CONSTRUCT_KIND, "class"));
    EvidenceItem method = evidenceItem(EvidenceKind.METHOD, "com.acme.pkg.MyType#doThing()", Map.of());

    List<EvidenceItem> items = List.of(repository, project, module, pkg, type, method);
    List<EvidenceRelationship> containment =
        List.of(
            relationship(repository, project),
            relationship(project, module),
            relationship(module, pkg),
            relationship(pkg, type),
            relationship(type, method));
    RepositoryEvidenceModel model = RepositoryEvidenceModel.of(items, containment);

    EvidenceKindMapperRegistry registry = new EvidenceKindMapperRegistry();
    registry.register(new RepositoryMapper());
    registry.register(new ProjectMapper());
    registry.register(new ModuleMapper());
    registry.register(new PackageMapper());
    registry.register(new TypeMapper());
    registry.register(new MethodMapper());

    MappingResult result = new MappingOrchestrator(registry, FIXED_CLOCK).construct(model);

    assertEquals(6, result.elements().size(), "one CSM element per Evidence Item");
    assertEquals(5, result.relationships().size(), "one CONTAINMENT relationship per adjacent pair");

    for (CsmRelationship rel : result.relationships()) {
      assertEquals(CsmRelationshipType.CONTAINMENT, rel.type());
    }

    // Spot-check one edge end-to-end: Module -> Package.
    CsmElementId moduleId = elementId(result, module.id());
    CsmElementId packageId =
        ElementIdentityDeriver.forPackage(ElementIdentityDeriver.fromEvidenceId(module.id()), "com.acme.pkg");
    boolean foundModuleToPackage =
        result.relationships().stream()
            .anyMatch(rel -> rel.sourceId().equals(moduleId) && rel.targetId().equals(Optional.of(packageId)));
    assertTrue(foundModuleToPackage, "expected a CONTAINMENT relationship from Module to Package");
  }

  @Test
  void unmanagedProjectContainmentIsConstructedLikeAnyOther() {
    EvidenceItem repository = evidenceItem(EvidenceKind.REPOSITORY, "repo-1", Map.of());
    EvidenceItem unmanagedProject = evidenceItem(EvidenceKind.PROJECT, "unmanaged", Map.of());
    RepositoryEvidenceModel model =
        RepositoryEvidenceModel.of(
            List.of(repository, unmanagedProject), List.of(relationship(repository, unmanagedProject)));

    EvidenceKindMapperRegistry registry = new EvidenceKindMapperRegistry();
    registry.register(new RepositoryMapper());
    registry.register(new ProjectMapper());

    MappingResult result = new MappingOrchestrator(registry, FIXED_CLOCK).construct(model);

    assertEquals(1, result.relationships().size());
    CsmRelationship rel = result.relationships().get(0);
    assertEquals(elementId(result, repository.id()), rel.sourceId());
    assertEquals(Optional.of(elementId(result, unmanagedProject.id())), rel.targetId());
  }

  @Test
  void defaultModuleContainmentIsConstructedLikeAnyOther() {
    EvidenceItem project = evidenceItem(EvidenceKind.PROJECT, "com.acme:parent", Map.of());
    EvidenceItem defaultModule = evidenceItem(EvidenceKind.MODULE, "com.acme:parent:default", Map.of());
    RepositoryEvidenceModel model =
        RepositoryEvidenceModel.of(
            List.of(project, defaultModule), List.of(relationship(project, defaultModule)));

    EvidenceKindMapperRegistry registry = new EvidenceKindMapperRegistry();
    registry.register(new ProjectMapper());
    registry.register(new ModuleMapper());

    MappingResult result = new MappingOrchestrator(registry, FIXED_CLOCK).construct(model);

    assertEquals(1, result.relationships().size());
    CsmRelationship rel = result.relationships().get(0);
    assertEquals(elementId(result, project.id()), rel.sourceId());
    assertEquals(Optional.of(elementId(result, defaultModule.id())), rel.targetId());
  }

  @Test
  void containmentToAnUnmappedKindIsOmittedNotAnError() {
    // No MethodMapper registered - the Type -> Method edge has no
    // resolvable target, so it is silently omitted, matching the
    // "absence of Method-level elements is not invalid" precedent.
    EvidenceItem type =
        evidenceItem(
            EvidenceKind.SOURCE_UNIT,
            "com.acme.pkg.MyType",
            Map.of(EvidenceAttributeKeys.NATIVE_CONSTRUCT_KIND, "class"));
    EvidenceItem method = evidenceItem(EvidenceKind.METHOD, "com.acme.pkg.MyType#doThing()", Map.of());
    RepositoryEvidenceModel model =
        RepositoryEvidenceModel.of(List.of(type, method), List.of(relationship(type, method)));

    EvidenceKindMapperRegistry registry = new EvidenceKindMapperRegistry();
    registry.register(new TypeMapper());
    // MethodMapper deliberately not registered.

    MappingResult result = new MappingOrchestrator(registry, FIXED_CLOCK).construct(model);

    assertEquals(1, result.elements().size(), "only the Type element, since Method has no Mapper");
    assertEquals(0, result.relationships().size(), "no CONTAINMENT relationship to an unresolved target");
  }

  private static CsmElementId elementId(MappingResult result, EvidenceId evidenceId) {
    CsmElementId expected = ElementIdentityDeriver.fromEvidenceId(evidenceId);
    for (CsmElement element : result.elements()) {
      if (element.id().equals(expected)) {
        return element.id();
      }
    }
    throw new AssertionError("no element found for " + evidenceId);
  }

  private static EvidenceRelationship relationship(EvidenceItem source, EvidenceItem target) {
    return new EvidenceRelationship(EvidenceRelationshipType.CONTAINMENT, source.id(), target.id());
  }

  private static EvidenceItem evidenceItem(EvidenceKind kind, String scopeKey, Map<String, String> attributes) {
    return new EvidenceItem(
        new EvidenceId("repo-1", kind, scopeKey),
        EvidenceAttributes.of(attributes),
        Optional.empty(),
        DiscoveryOutcome.complete(),
        ExtractionMethod.MANIFEST_DECLARED);
  }
}
