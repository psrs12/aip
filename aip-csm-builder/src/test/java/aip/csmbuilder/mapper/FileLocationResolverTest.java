package aip.csmbuilder.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.core.csm.CsmElement;
import aip.core.csm.TypeElement;
import aip.core.evidence.DiscoveryOutcome;
import aip.core.evidence.EvidenceAttributes;
import aip.core.evidence.EvidenceId;
import aip.core.evidence.EvidenceItem;
import aip.core.evidence.EvidenceKind;
import aip.core.evidence.EvidenceLocation;
import aip.core.evidence.EvidenceRelationship;
import aip.core.evidence.EvidenceRelationshipType;
import aip.core.evidence.ExtractionMethod;
import aip.core.evidence.RepositoryEvidenceModel;
import aip.csmbuilder.mapping.EvidenceAttributeKeys;
import aip.csmbuilder.mapping.EvidenceKindMapperRegistry;
import aip.csmbuilder.mapping.MappingOrchestrator;
import aip.csmbuilder.mapping.MappingResult;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * File-location resolution tests (tasks.md 9.1, 9.2), exercised
 * through {@link TypeMapper} since {@link FileLocationResolver} has no
 * public API of its own — both Type and Method Mappers share it.
 */
class FileLocationResolverTest {

  private static final Clock FIXED_CLOCK = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);

  @Test
  void sourceLocationIsResolvedFromTheReferencedFileEvidenceItem() {
    EvidenceItem file =
        new EvidenceItem(
            new EvidenceId("repo-1", EvidenceKind.FILE, "src/main/java/com/acme/pkg/MyType.java"),
            EvidenceAttributes.empty(),
            Optional.of(EvidenceLocation.of("src/main/java/com/acme/pkg/MyType.java", "1:1-42:1")),
            DiscoveryOutcome.complete(),
            ExtractionMethod.FULL_PARSE);
    EvidenceItem type =
        new EvidenceItem(
            new EvidenceId("repo-1", EvidenceKind.SOURCE_UNIT, "com.acme.pkg.MyType"),
            EvidenceAttributes.of(Map.of(EvidenceAttributeKeys.NATIVE_CONSTRUCT_KIND, "class")),
            Optional.empty(),
            DiscoveryOutcome.complete(),
            ExtractionMethod.FULL_PARSE);
    EvidenceRelationship reference =
        new EvidenceRelationship(EvidenceRelationshipType.REFERENCE, type.id(), file.id());
    RepositoryEvidenceModel model =
        RepositoryEvidenceModel.of(List.of(type, file), List.of(reference));

    EvidenceKindMapperRegistry registry = new EvidenceKindMapperRegistry();
    registry.register(new TypeMapper());
    MappingResult result = new MappingOrchestrator(registry, FIXED_CLOCK).construct(model);

    TypeElement element = (TypeElement) result.elements().get(0);
    assertTrue(element.sourceLocation().isPresent());
    assertEquals("src/main/java/com/acme/pkg/MyType.java", element.sourceLocation().get().filePath());
    assertEquals(Optional.of("1:1-42:1"), element.sourceLocation().get().position());
  }

  @Test
  void sourceLocationIsEmptyWhenNoFileReferenceExists() {
    EvidenceItem type =
        new EvidenceItem(
            new EvidenceId("repo-1", EvidenceKind.SOURCE_UNIT, "com.acme.pkg.MyType"),
            EvidenceAttributes.empty(),
            Optional.empty(),
            DiscoveryOutcome.complete(),
            ExtractionMethod.FULL_PARSE);
    RepositoryEvidenceModel model = RepositoryEvidenceModel.of(List.of(type), List.of());

    EvidenceKindMapperRegistry registry = new EvidenceKindMapperRegistry();
    registry.register(new TypeMapper());
    MappingResult result = new MappingOrchestrator(registry, FIXED_CLOCK).construct(model);

    TypeElement element = (TypeElement) result.elements().get(0);
    assertTrue(element.sourceLocation().isEmpty());
  }

  @Test
  void noCsmRelationshipIsEverConstructedToOrFromAFileEvidenceItem() {
    // 9.2: File Evidence Becomes a Location Attribute, Not a
    // Relationship. Even with a REFERENCE relationship present (the
    // legitimate Evidence-side mechanism) and, hypothetically, a
    // CONTAINMENT relationship pointing at the File (malformed input,
    // but must still not produce a CSM relationship, since File never
    // resolves to a CSM element in the first place — there is no
    // FileMapper registered anywhere in this codebase).
    EvidenceItem file =
        new EvidenceItem(
            new EvidenceId("repo-1", EvidenceKind.FILE, "src/main/java/com/acme/pkg/MyType.java"),
            EvidenceAttributes.empty(),
            Optional.of(EvidenceLocation.of("src/main/java/com/acme/pkg/MyType.java")),
            DiscoveryOutcome.complete(),
            ExtractionMethod.FULL_PARSE);
    EvidenceItem type =
        new EvidenceItem(
            new EvidenceId("repo-1", EvidenceKind.SOURCE_UNIT, "com.acme.pkg.MyType"),
            EvidenceAttributes.empty(),
            Optional.empty(),
            DiscoveryOutcome.complete(),
            ExtractionMethod.FULL_PARSE);
    EvidenceItem module =
        new EvidenceItem(
            new EvidenceId("repo-1", EvidenceKind.MODULE, "com.acme:module-a"),
            EvidenceAttributes.empty(),
            Optional.empty(),
            DiscoveryOutcome.complete(),
            ExtractionMethod.MANIFEST_DECLARED);
    List<EvidenceRelationship> relationships =
        List.of(
            new EvidenceRelationship(EvidenceRelationshipType.REFERENCE, type.id(), file.id()),
            // Hypothetical/malformed: a CONTAINMENT edge naming File as
            // a target, as if it were a structural container's child.
            new EvidenceRelationship(EvidenceRelationshipType.CONTAINMENT, module.id(), file.id()));
    RepositoryEvidenceModel model =
        RepositoryEvidenceModel.of(List.of(type, file, module), relationships);

    EvidenceKindMapperRegistry registry = new EvidenceKindMapperRegistry();
    registry.register(new TypeMapper());
    registry.register(new ModuleMapper());
    // Deliberately no Mapper registered for EvidenceKind.FILE anywhere.

    MappingResult result = new MappingOrchestrator(registry, FIXED_CLOCK).construct(model);

    assertTrue(
        result.elements().stream().map(CsmElement::name).noneMatch(name -> name.contains("MyType.java")),
        "no CSM element should ever represent a File Evidence Item");
    assertTrue(
        result.relationships().isEmpty(),
        "no CSM relationship should reference a File Evidence Item, malformed CONTAINMENT included");
  }
}
