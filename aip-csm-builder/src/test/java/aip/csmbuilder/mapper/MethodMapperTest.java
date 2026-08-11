package aip.csmbuilder.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.core.csm.CsmEntityKind;
import aip.core.csm.MethodElement;
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

class MethodMapperTest {

  private static final Clock FIXED_CLOCK = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);

  @Test
  void mapsMethodEvidenceToAMethodElement() {
    EvidenceItem method = TestEvidenceItems.of(EvidenceKind.METHOD, "com.acme.pkg.MyType#doThing()");

    EvidenceKindMapperRegistry registry = new EvidenceKindMapperRegistry();
    registry.register(new MethodMapper());
    MappingResult result =
        new MappingOrchestrator(registry, FIXED_CLOCK)
            .construct(RepositoryEvidenceModel.of(List.of(method), List.of()));

    assertEquals(1, result.elements().size());
    MethodElement element = assertInstanceOf(MethodElement.class, result.elements().get(0));
    assertEquals(CsmEntityKind.METHOD, element.kind());
    assertEquals(ElementIdentityDeriver.fromEvidenceId(method.id()), element.id());
    assertEquals(method.id().scopeKey(), element.name());
    assertEquals(ProvenanceCategory.OBSERVED, element.provenance().category());
    assertTrue(element.sourceLocation().isEmpty(), "source location is populated in Section 9, not here");
  }
}
