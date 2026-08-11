package aip.csmbuilder.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import aip.core.csm.CsmEntityKind;
import aip.core.csm.PackageElement;
import aip.core.csm.ProvenanceCategory;
import aip.core.evidence.EvidenceItem;
import aip.core.evidence.EvidenceKind;
import aip.core.evidence.EvidenceRelationship;
import aip.core.evidence.EvidenceRelationshipType;
import aip.core.evidence.RepositoryEvidenceModel;
import aip.csmbuilder.identity.ElementIdentityDeriver;
import aip.csmbuilder.mapping.EvidenceAttributeKeys;
import aip.csmbuilder.mapping.EvidenceKindMapperRegistry;
import aip.csmbuilder.mapping.MappingOrchestrator;
import aip.csmbuilder.mapping.MappingResult;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PackageMapperTest {

  private static final Clock FIXED_CLOCK = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);

  @Test
  void mapsPackageToAPackageElementIdentifiedByItsContainingModuleAndNamespace() {
    EvidenceItem module = TestEvidenceItems.of(EvidenceKind.MODULE, "com.acme:module-a");
    EvidenceItem pkg =
        TestEvidenceItems.of(
            EvidenceKind.PACKAGE,
            "com.acme:module-a:com.acme.pkg",
            Map.of(EvidenceAttributeKeys.NAMESPACE_NAME, "com.acme.pkg"));
    EvidenceRelationship containment =
        new EvidenceRelationship(EvidenceRelationshipType.CONTAINMENT, module.id(), pkg.id());
    RepositoryEvidenceModel model =
        RepositoryEvidenceModel.of(List.of(module, pkg), List.of(containment));

    EvidenceKindMapperRegistry registry = new EvidenceKindMapperRegistry();
    registry.register(new PackageMapper());
    MappingResult result = new MappingOrchestrator(registry, FIXED_CLOCK).construct(model);

    assertEquals(1, result.elements().size());
    PackageElement element = assertInstanceOf(PackageElement.class, result.elements().get(0));
    assertEquals(CsmEntityKind.PACKAGE, element.kind());
    assertEquals("com.acme.pkg", element.name());
    assertEquals(
        ElementIdentityDeriver.forPackage(
            ElementIdentityDeriver.fromEvidenceId(module.id()), "com.acme.pkg"),
        element.id());
    assertEquals(ProvenanceCategory.OBSERVED, element.provenance().category());
    assertEquals(pkg.id().toString(), element.provenance().sourceReference());
  }

  @Test
  void missingContainingModuleRelationshipIsRejected() {
    EvidenceItem pkg =
        TestEvidenceItems.of(
            EvidenceKind.PACKAGE,
            "com.acme:module-a:com.acme.pkg",
            Map.of(EvidenceAttributeKeys.NAMESPACE_NAME, "com.acme.pkg"));
    RepositoryEvidenceModel model = RepositoryEvidenceModel.of(List.of(pkg), List.of());

    EvidenceKindMapperRegistry registry = new EvidenceKindMapperRegistry();
    registry.register(new PackageMapper());

    assertThrows(
        IllegalStateException.class,
        () -> new MappingOrchestrator(registry, FIXED_CLOCK).construct(model));
  }

  @Test
  void missingNamespaceNameAttributeIsRejected() {
    EvidenceItem module = TestEvidenceItems.of(EvidenceKind.MODULE, "com.acme:module-a");
    EvidenceItem pkg = TestEvidenceItems.of(EvidenceKind.PACKAGE, "com.acme:module-a:com.acme.pkg");
    EvidenceRelationship containment =
        new EvidenceRelationship(EvidenceRelationshipType.CONTAINMENT, module.id(), pkg.id());
    RepositoryEvidenceModel model =
        RepositoryEvidenceModel.of(List.of(module, pkg), List.of(containment));

    EvidenceKindMapperRegistry registry = new EvidenceKindMapperRegistry();
    registry.register(new PackageMapper());

    assertThrows(
        IllegalStateException.class,
        () -> new MappingOrchestrator(registry, FIXED_CLOCK).construct(model));
  }

  @Test
  void packageIdentityIsIndependentOfDispatchOrderRelativeToItsModule() {
    // PackageMapper derives the containing Module's CSM identity via
    // pure re-derivation from the Evidence Model's own CONTAINMENT
    // relationship, never via MappingContext.resolvedElementId — so
    // it must produce the same result whether or not a ModuleMapper is
    // even registered (i.e. regardless of processing order or whether
    // the Module was "already mapped").
    EvidenceItem module = TestEvidenceItems.of(EvidenceKind.MODULE, "com.acme:module-a");
    EvidenceItem pkg =
        TestEvidenceItems.of(
            EvidenceKind.PACKAGE,
            "com.acme:module-a:com.acme.pkg",
            Map.of(EvidenceAttributeKeys.NAMESPACE_NAME, "com.acme.pkg"));
    EvidenceRelationship containment =
        new EvidenceRelationship(EvidenceRelationshipType.CONTAINMENT, module.id(), pkg.id());
    RepositoryEvidenceModel model =
        RepositoryEvidenceModel.of(List.of(module, pkg), List.of(containment));

    EvidenceKindMapperRegistry withoutModuleMapper = new EvidenceKindMapperRegistry();
    withoutModuleMapper.register(new PackageMapper());
    MappingResult resultWithoutModuleMapper =
        new MappingOrchestrator(withoutModuleMapper, FIXED_CLOCK).construct(model);

    EvidenceKindMapperRegistry withModuleMapper = new EvidenceKindMapperRegistry();
    withModuleMapper.register(new ModuleMapper());
    withModuleMapper.register(new PackageMapper());
    MappingResult resultWithModuleMapper =
        new MappingOrchestrator(withModuleMapper, FIXED_CLOCK).construct(model);

    PackageElement packageOnly =
        (PackageElement)
            resultWithoutModuleMapper.elements().stream()
                .filter(PackageElement.class::isInstance)
                .findFirst()
                .orElseThrow();
    PackageElement packageWithModule =
        (PackageElement)
            resultWithModuleMapper.elements().stream()
                .filter(PackageElement.class::isInstance)
                .findFirst()
                .orElseThrow();

    assertEquals(packageOnly.id(), packageWithModule.id());
  }
}
