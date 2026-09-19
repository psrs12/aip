package aip.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.analysis.test.fixtures.CsmSnapshotSourceBuilder;
import aip.analysis.test.fixtures.InMemoryAnalysisResultStore;
import aip.analysis.test.fixtures.StubAnalyzer;
import aip.core.csm.AnalysisResult;
import aip.core.csm.AnalysisView;
import aip.core.csm.CsmElementId;
import aip.core.csm.CsmEntityKind;
import aip.core.csm.CsmScope;
import aip.core.csm.CsmScopeInstance;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * {@link AnalysisResultPublisher} tests, per {@code Analysis Result
 * Validation Before Publication} — covering all three validation
 * failure categories through the publish-then-query-the-store path
 * (referential integrity, Analyzer/version consistency, scope
 * containment; {@link AnalysisResultValidatorTest} covers the same
 * three at the validator level directly), plus the store-level {@code
 * AnalysisResultStore never holds a Result that failed validation}
 * scenario.
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

  @Test
  void resultReferencingAnAbsentCsmIdentityIsNeverWritten() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    builder.module("moduleA");
    AnalysisView view = AnalysisView.from(builder.build());

    AnalyzerRegistry registry = new AnalyzerRegistry();
    CsmScope anchoredScope = MODULE_SCOPE.anchoredAt(CsmEntityKind.MODULE);
    registry.register(new StubAnalyzer("analyzer.a", 1, anchoredScope));
    InMemoryAnalysisResultStore store = new InMemoryAnalysisResultStore();

    CsmScopeInstance nonExistentAnchor = CsmScopeInstance.anchoredAt(new CsmElementId("repo:element:absent"));
    AnalysisResult result = AnalysisResult.of("analyzer.a", 1, view.sourceSnapshotId(), nonExistentAnchor, "x");

    AnalysisResultPublisher.PublicationOutcome outcome =
        AnalysisResultPublisher.publish(store, result, view, registry);

    assertFalse(outcome.published());
    assertTrue(store.read(result.id()).isEmpty());
  }

  @Test
  void resultExceedingItsAnalyzersDeclaredScopeIsNeverWritten() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    CsmElementId m1 = builder.module("moduleA");
    AnalysisView view = AnalysisView.from(builder.build());

    AnalyzerRegistry registry = new AnalyzerRegistry();
    // Declared unanchored, but the constructed Result claims an anchored scope instance.
    registry.register(new StubAnalyzer("analyzer.a", 1, MODULE_SCOPE));
    InMemoryAnalysisResultStore store = new InMemoryAnalysisResultStore();

    AnalysisResult result =
        AnalysisResult.of("analyzer.a", 1, view.sourceSnapshotId(), CsmScopeInstance.anchoredAt(m1), "x");

    AnalysisResultPublisher.PublicationOutcome outcome =
        AnalysisResultPublisher.publish(store, result, view, registry);

    assertFalse(outcome.published());
    assertTrue(store.read(result.id()).isEmpty());
  }

  @Test
  void storeNeverHoldsAResultThatFailedValidation() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    CsmElementId m1 = builder.module("moduleA");
    AnalysisView view = AnalysisView.from(builder.build());

    AnalyzerRegistry registry = new AnalyzerRegistry();
    registry.register(new StubAnalyzer("analyzer.valid", 1, MODULE_SCOPE));
    // analyzer.invalid is deliberately never registered.
    InMemoryAnalysisResultStore store = new InMemoryAnalysisResultStore();

    AnalysisResult validResult =
        AnalysisResult.of("analyzer.valid", 1, view.sourceSnapshotId(), CsmScopeInstance.wholeRepository(), "ok");
    AnalysisResult unregisteredAnalyzerResult =
        AnalysisResult.of("analyzer.invalid", 1, view.sourceSnapshotId(), CsmScopeInstance.wholeRepository(), "bad");
    AnalysisResult outOfScopeResult =
        AnalysisResult.of("analyzer.valid", 1, view.sourceSnapshotId(), CsmScopeInstance.anchoredAt(m1), "bad");

    AnalysisResultPublisher.publish(store, validResult, view, registry);
    AnalysisResultPublisher.publish(store, unregisteredAnalyzerResult, view, registry);
    AnalysisResultPublisher.publish(store, outOfScopeResult, view, registry);

    // Query by identity: only the valid Result is ever retrievable.
    assertTrue(store.read(validResult.id()).isPresent());
    assertTrue(store.read(unregisteredAnalyzerResult.id()).isEmpty());
    assertTrue(store.read(outOfScopeResult.id()).isEmpty());

    // Query by listing: only the valid Result is ever surfaced.
    assertEquals(
        java.util.List.of(validResult),
        store.listByAnalyzerAndSnapshot("analyzer.valid", view.sourceSnapshotId()));
    assertTrue(store.listByAnalyzerAndSnapshot("analyzer.invalid", view.sourceSnapshotId()).isEmpty());
  }
}
