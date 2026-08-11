package aip.csmbuilder.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.core.evidence.DiscoveryOutcome;
import aip.core.evidence.EvidenceAttributes;
import aip.core.evidence.EvidenceId;
import aip.core.evidence.EvidenceItem;
import aip.core.evidence.EvidenceKind;
import aip.core.evidence.ExtractionMethod;
import aip.core.evidence.RepositoryEvidenceModel;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Contract tests for {@link MappingOrchestrator}, independent of any
 * real domain Mapper (tasks.md 4.4) — uses {@link RecordingTestMapper}
 * only.
 */
class MappingOrchestratorTest {

  @Test
  void emptyModelProducesEmptyResult() {
    MappingOrchestrator orchestrator = new MappingOrchestrator(new EvidenceKindMapperRegistry());
    MappingResult result = orchestrator.construct(RepositoryEvidenceModel.empty());
    assertEquals(MappingResult.empty(), result);
  }

  @Test
  void itemWithRegisteredMapperIsDispatched() {
    EvidenceKindMapperRegistry registry = new EvidenceKindMapperRegistry();
    RecordingTestMapper mapper = new RecordingTestMapper(EvidenceKind.MODULE, 1);
    registry.register(mapper);

    EvidenceItem module = item(EvidenceKind.MODULE, "com.acme:module-a");
    RepositoryEvidenceModel model = RepositoryEvidenceModel.of(List.of(module), List.of());

    MappingResult result = new MappingOrchestrator(registry).construct(model);

    assertEquals(1, mapper.invocations.size());
    assertEquals(module, mapper.invocations.get(0));
    assertEquals(1, result.elements().size());
  }

  @Test
  void itemWithNoRegisteredMapperIsSkippedWithoutError() {
    // An empty registry means no Evidence kind has a Mapper — this
    // must not throw, matching how the ConfigReference/implementation-
    // extension/invocation exclusions are ultimately realized (nothing
    // is registered for them, per MappingOrchestrator's own javadoc).
    EvidenceKindMapperRegistry registry = new EvidenceKindMapperRegistry();
    EvidenceItem module = item(EvidenceKind.MODULE, "com.acme:module-a");
    RepositoryEvidenceModel model = RepositoryEvidenceModel.of(List.of(module), List.of());

    MappingResult result = new MappingOrchestrator(registry).construct(model);

    assertEquals(MappingResult.empty(), result);
  }

  @Test
  void itemsAreDispatchedInDeterministicSortedOrder() {
    EvidenceKindMapperRegistry registry = new EvidenceKindMapperRegistry();
    RecordingTestMapper mapper = new RecordingTestMapper(EvidenceKind.MODULE, 1);
    registry.register(mapper);

    EvidenceItem moduleC = item(EvidenceKind.MODULE, "com.acme:module-c");
    EvidenceItem moduleA = item(EvidenceKind.MODULE, "com.acme:module-a");
    EvidenceItem moduleB = item(EvidenceKind.MODULE, "com.acme:module-b");
    // Deliberately constructed out of order to prove the orchestrator
    // sorts, rather than preserving input iteration order.
    RepositoryEvidenceModel model =
        RepositoryEvidenceModel.of(List.of(moduleC, moduleA, moduleB), List.of());

    new MappingOrchestrator(registry).construct(model);

    assertEquals(List.of(moduleA, moduleB, moduleC), mapper.invocations);
  }

  @Test
  void repeatedRunsAgainstUnchangedInputProduceIdenticalDispatchOrder() {
    EvidenceKindMapperRegistry registry1 = new EvidenceKindMapperRegistry();
    RecordingTestMapper mapper1 = new RecordingTestMapper(EvidenceKind.MODULE, 1);
    registry1.register(mapper1);

    EvidenceKindMapperRegistry registry2 = new EvidenceKindMapperRegistry();
    RecordingTestMapper mapper2 = new RecordingTestMapper(EvidenceKind.MODULE, 1);
    registry2.register(mapper2);

    List<EvidenceItem> items =
        List.of(
            item(EvidenceKind.MODULE, "com.acme:module-z"),
            item(EvidenceKind.MODULE, "com.acme:module-y"));
    RepositoryEvidenceModel model = RepositoryEvidenceModel.of(items, List.of());

    new MappingOrchestrator(registry1).construct(model);
    new MappingOrchestrator(registry2).construct(model);

    assertEquals(mapper1.invocations, mapper2.invocations);
  }

  @Test
  void laterItemsCanResolveEarlierItemsProducedElementId() {
    EvidenceKindMapperRegistry registry = new EvidenceKindMapperRegistry();
    EvidenceItem moduleA = item(EvidenceKind.MODULE, "com.acme:module-a");

    // A second Mapper, registered for a different kind, that asserts
    // it can see module-a's already-resolved element id — proving
    // MappingContext propagates state across the orchestration run in
    // dispatch order.
    EvidenceItem packageEvidence = item(EvidenceKind.PACKAGE, "com.acme:module-a:com.acme.pkg");
    boolean[] sawResolvedId = {false};
    EvidenceKindMapper packageMapper =
        new EvidenceKindMapper() {
          @Override
          public EvidenceKind supportedKind() {
            return EvidenceKind.PACKAGE;
          }

          @Override
          public int mapperVersion() {
            return 1;
          }

          @Override
          public MappingResult map(EvidenceItem item, MappingContext context) {
            Optional<aip.core.csm.CsmElementId> resolved =
                context.resolvedElementId(moduleA.id());
            sawResolvedId[0] = resolved.isPresent();
            return MappingResult.empty();
          }
        };

    registry.register(new RecordingTestMapper(EvidenceKind.MODULE, 1));
    registry.register(packageMapper);

    RepositoryEvidenceModel model =
        RepositoryEvidenceModel.of(List.of(moduleA, packageEvidence), List.of());

    new MappingOrchestrator(registry).construct(model);

    assertTrue(sawResolvedId[0], "PACKAGE Mapper should see MODULE's already-resolved element id");
  }

  private static EvidenceItem item(EvidenceKind kind, String scopeKey) {
    return new EvidenceItem(
        new EvidenceId("repo-1", kind, scopeKey),
        EvidenceAttributes.empty(),
        Optional.empty(),
        DiscoveryOutcome.complete(),
        ExtractionMethod.MANIFEST_DECLARED);
  }
}
