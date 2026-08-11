package aip.csmbuilder.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.core.csm.CsmEntityKind;
import aip.core.csm.TypeElement;
import aip.core.evidence.EvidenceItem;
import aip.core.evidence.EvidenceKind;
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

class TypeMapperTest {

  private static final Clock FIXED_CLOCK = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);

  @Test
  void everyNativeConstructKindMapsUniformlyOntoType() {
    for (String nativeKind : List.of("class", "interface", "struct", "record", "enum")) {
      EvidenceItem sourceUnit =
          TestEvidenceItems.of(
              EvidenceKind.SOURCE_UNIT,
              "com.acme.pkg.MyType",
              Map.of(EvidenceAttributeKeys.NATIVE_CONSTRUCT_KIND, nativeKind));

      EvidenceKindMapperRegistry registry = new EvidenceKindMapperRegistry();
      registry.register(new TypeMapper());
      MappingResult result =
          new MappingOrchestrator(registry, FIXED_CLOCK)
              .construct(RepositoryEvidenceModel.of(List.of(sourceUnit), List.of()));

      TypeElement element = assertInstanceOf(TypeElement.class, result.elements().get(0));
      assertEquals(
          CsmEntityKind.TYPE, element.kind(), "native kind '" + nativeKind + "' must still map to TYPE");
      assertEquals(nativeKind, element.nativeAttributes().get(EvidenceAttributeKeys.NATIVE_CONSTRUCT_KIND).get());
    }
  }

  @Test
  void identityAndNameDeriveFromEvidence() {
    EvidenceItem sourceUnit =
        TestEvidenceItems.of(
            EvidenceKind.SOURCE_UNIT,
            "com.acme.pkg.MyType",
            Map.of(EvidenceAttributeKeys.NATIVE_CONSTRUCT_KIND, "class"));

    EvidenceKindMapperRegistry registry = new EvidenceKindMapperRegistry();
    registry.register(new TypeMapper());
    MappingResult result =
        new MappingOrchestrator(registry, FIXED_CLOCK)
            .construct(RepositoryEvidenceModel.of(List.of(sourceUnit), List.of()));

    TypeElement element = (TypeElement) result.elements().get(0);
    assertEquals(ElementIdentityDeriver.fromEvidenceId(sourceUnit.id()), element.id());
    assertEquals("com.acme.pkg.MyType", element.name());
    assertTrue(element.sourceLocation().isEmpty(), "source location is populated in Section 9, not here");
  }

  @Test
  void missingNativeConstructKindAttributeIsNotAnError() {
    EvidenceItem sourceUnit = TestEvidenceItems.of(EvidenceKind.SOURCE_UNIT, "com.acme.pkg.MyType");

    EvidenceKindMapperRegistry registry = new EvidenceKindMapperRegistry();
    registry.register(new TypeMapper());
    MappingResult result =
        new MappingOrchestrator(registry, FIXED_CLOCK)
            .construct(RepositoryEvidenceModel.of(List.of(sourceUnit), List.of()));

    TypeElement element = (TypeElement) result.elements().get(0);
    assertTrue(element.nativeAttributes().isEmpty());
  }
}
