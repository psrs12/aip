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

  @Test
  void registeringAMapperForConfigFileIsRejected() {
    // 13.1: ConfigFile/ConfigReference are permanently excluded from
    // CSM representation - a Mapper mistakenly written for either kind
    // must fail loudly at registration time, not silently produce
    // excluded CSM content.
    EvidenceKindMapperRegistry registry = new EvidenceKindMapperRegistry();

    assertThrows(
        IllegalArgumentException.class,
        () -> registry.register(new RecordingTestMapper(EvidenceKind.CONFIG_FILE, 1)));
  }

  @Test
  void registeringAMapperForConfigReferenceIsRejected() {
    EvidenceKindMapperRegistry registry = new EvidenceKindMapperRegistry();

    assertThrows(
        IllegalArgumentException.class,
        () -> registry.register(new RecordingTestMapper(EvidenceKind.CONFIG_REFERENCE, 1)));
  }
}
