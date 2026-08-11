package aip.csmbuilder.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.core.csm.CsmEntityKind;
import aip.core.csm.ProvenanceCategory;
import aip.core.csm.RepositoryElement;
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

class RepositoryMapperTest {

  private static final Clock FIXED_CLOCK = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);

  @Test
  void mapsRepositoryEvidenceToARepositoryElement() {
    EvidenceItem repository = TestEvidenceItems.of(EvidenceKind.REPOSITORY, "repo-1");

    EvidenceKindMapperRegistry registry = new EvidenceKindMapperRegistry();
    registry.register(new RepositoryMapper());
    MappingResult result =
        new MappingOrchestrator(registry, FIXED_CLOCK)
            .construct(RepositoryEvidenceModel.of(List.of(repository), List.of()));

    assertEquals(1, result.elements().size());
    RepositoryElement element = assertInstanceOf(RepositoryElement.class, result.elements().get(0));
    assertEquals(CsmEntityKind.REPOSITORY, element.kind());
    assertEquals(ElementIdentityDeriver.fromEvidenceId(repository.id()), element.id());
    assertEquals("repo-1", element.name());
    assertEquals(ProvenanceCategory.OBSERVED, element.provenance().category());
    assertEquals(repository.id().toString(), element.provenance().sourceReference());
    assertEquals(Instant.EPOCH, element.provenance().timestamp());
    assertTrue(element.provenance().confidence().isEmpty());
  }
}
