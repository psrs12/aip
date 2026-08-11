package aip.csmbuilder.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import aip.core.csm.CsmEntityKind;
import aip.core.csm.ProjectElement;
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

class ProjectMapperTest {

  private static final Clock FIXED_CLOCK = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);

  @Test
  void mapsADeclaredProjectToAProjectElement() {
    EvidenceItem project = TestEvidenceItems.of(EvidenceKind.PROJECT, "com.acme:parent");
    assertMapsCleanly(project);
  }

  @Test
  void mapsTheReservedUnmanagedProjectWithNoSpecialCasing() {
    // Per Unmanaged Evidence Mapping: CSM Builder treats the reserved
    // unmanaged Project Evidence Item exactly like any other Project.
    EvidenceItem unmanagedProject = TestEvidenceItems.of(EvidenceKind.PROJECT, "unmanaged");
    assertMapsCleanly(unmanagedProject);
  }

  private void assertMapsCleanly(EvidenceItem project) {
    EvidenceKindMapperRegistry registry = new EvidenceKindMapperRegistry();
    registry.register(new ProjectMapper());
    MappingResult result =
        new MappingOrchestrator(registry, FIXED_CLOCK)
            .construct(RepositoryEvidenceModel.of(List.of(project), List.of()));

    assertEquals(1, result.elements().size());
    ProjectElement element = assertInstanceOf(ProjectElement.class, result.elements().get(0));
    assertEquals(CsmEntityKind.PROJECT, element.kind());
    assertEquals(ElementIdentityDeriver.fromEvidenceId(project.id()), element.id());
    assertEquals(project.id().scopeKey(), element.name());
    assertEquals(ProvenanceCategory.OBSERVED, element.provenance().category());
    assertEquals(project.id().toString(), element.provenance().sourceReference());
  }
}
