package aip.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.analysis.test.fixtures.CsmSnapshotSourceBuilder;
import aip.analysis.test.fixtures.InMemoryAnalysisResultStore;
import aip.analysis.test.fixtures.StubAnalyzer;
import aip.core.csm.AnalysisResult;
import aip.core.csm.AnalysisView;
import aip.core.csm.CsmEntityKind;
import aip.core.csm.CsmScope;
import aip.core.csm.CsmScopeInstance;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * {@link AnalysisResultPublisher} tests, per {@code Valid Result is
 * published as usable output} and {@code Result from an unregistered
 * Analyzer/version is rejected} (representative of the rejection
 * cases {@link AnalysisResultValidatorTest} already covers directly).
 */
class AnalysisResultPublisherTest {

  private static final CsmScope MODULE_SCOPE = CsmScope.ofEntityKinds(Set.of(CsmEntityKind.MODULE));

  @Test
  void validResultIsPublishedAndRetrievable() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    builder.module("moduleA");
    AnalysisView view = AnalysisView.from(builder.build());

    AnalyzerRegistry registry = new AnalyzerRegistry();
    registry.register(new StubAnalyzer("analyzer.a", 1, MODULE_SCOPE));
    InMemoryAnalysisResultStore store = new InMemoryAnalysisResultStore();

    AnalysisResult result =
        AnalysisResult.of("analyzer.a", 1, view.sourceSnapshotId(), CsmScopeInstance.wholeRepository(), "ok");

    AnalysisResultPublisher.PublicationOutcome outcome =
        AnalysisResultPublisher.publish(store, result, view, registry);

    assertTrue(outcome.published());
    assertEquals(result.id(), outcome.result().orElseThrow().id());
    assertEquals(result, store.read(result.id()).orElseThrow());
  }

  @Test
  void invalidResultIsNeverWritten() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    builder.module("moduleA");
    AnalysisView view = AnalysisView.from(builder.build());

    AnalyzerRegistry registry = new AnalyzerRegistry(); // analyzer.a is not registered
    InMemoryAnalysisResultStore store = new InMemoryAnalysisResultStore();

    AnalysisResult result =
        AnalysisResult.of("analyzer.a", 1, view.sourceSnapshotId(), CsmScopeInstance.wholeRepository(), "bad");

    AnalysisResultPublisher.PublicationOutcome outcome =
        AnalysisResultPublisher.publish(store, result, view, registry);

    assertFalse(outcome.published());
    assertTrue(store.read(result.id()).isEmpty());
  }
}
