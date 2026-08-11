package aip.csmbuilder.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import aip.core.csm.CsmEntityKind;
import aip.core.csm.ModuleElement;
import aip.core.csm.ProvenanceCategory;
import aip.core.evidence.EvidenceItem;
import aip.core.evidence.EvidenceKind;
import aip.core.evidence.RepositoryEvidenceModel;
import aip.csmbuilder.identity.ElementIdentityDeriver;
import aip.csmbuilder.mapping.EvidenceKindMapperRegistry;
import aip.csmbuilder.mapping.MappingOrchestrator;
import aip.csmbuilder.mapping.MappingResult;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModuleMapperTest {

  private static final Clock FIXED_CLOCK = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);

  @Test
  void mapsADeclaredSubModuleToAModuleElement() {
    EvidenceItem module = TestEvidenceItems.of(EvidenceKind.MODULE, "com.acme:module-a");
    assertMapsCleanly(module);
  }

  @Test
  void mapsTheDefaultModuleForASubModuleLessProjectWithNoSpecialCasing() {
    // Per Structural Entity Mapping: RU's synthetic default Module
    // (single-module Project case) is handled identically to any
    // declared sub-module.
    EvidenceItem defaultModule = TestEvidenceItems.of(EvidenceKind.MODULE, "com.acme:parent:default");
    assertMapsCleanly(defaultModule);
  }

  private void assertMapsCleanly(EvidenceItem module) {
    EvidenceKindMapperRegistry registry = new EvidenceKindMapperRegistry();
    registry.register(new ModuleMapper());
    MappingResult result =
        new MappingOrchestrator(registry, FIXED_CLOCK)
            .construct(RepositoryEvidenceModel.of(List.of(module), List.of()));

    assertEquals(1, result.elements().size());
    ModuleElement element = assertInstanceOf(ModuleElement.class, result.elements().get(0));
    assertEquals(CsmEntityKind.MODULE, element.kind());
    assertEquals(ElementIdentityDeriver.fromEvidenceId(module.id()), element.id());
    assertEquals(module.id().scopeKey(), element.name());
    assertEquals(ProvenanceCategory.OBSERVED, element.provenance().category());
  }
}
