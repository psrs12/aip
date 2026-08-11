package aip.csmbuilder.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.core.evidence.EvidenceKind;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EvidenceKindMapperRegistryTest {

  @Test
  void registeredMapperIsFoundByItsSupportedKind() {
    EvidenceKindMapperRegistry registry = new EvidenceKindMapperRegistry();
    RecordingTestMapper mapper = new RecordingTestMapper(EvidenceKind.MODULE, 1);

    registry.register(mapper);

    assertEquals(Optional.of(mapper), registry.lookup(EvidenceKind.MODULE));
    assertEquals(Set.of(EvidenceKind.MODULE), registry.registeredKinds());
  }

  @Test
  void unregisteredKindIsAbsent() {
    EvidenceKindMapperRegistry registry = new EvidenceKindMapperRegistry();
    assertTrue(registry.lookup(EvidenceKind.PACKAGE).isEmpty());
  }

  @Test
  void registeringTwoMappersForTheSameKindIsRejected() {
    EvidenceKindMapperRegistry registry = new EvidenceKindMapperRegistry();
    registry.register(new RecordingTestMapper(EvidenceKind.MODULE, 1));

    assertThrows(
        IllegalStateException.class,
        () -> registry.register(new RecordingTestMapper(EvidenceKind.MODULE, 2)));
  }
}
