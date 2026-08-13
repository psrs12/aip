package aip.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.analysis.test.fixtures.CsmSnapshotSourceBuilder;
import aip.core.csm.AnalysisResult;
import aip.core.csm.AnalysisView;
import aip.core.csm.CsmElementId;
import aip.core.csm.CsmEntityKind;
import aip.core.csm.CsmScope;
import aip.core.csm.CsmScopeInstance;
import aip.core.csm.CsmSnapshotSource;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * {@link AnalysisOrchestrator} tests, per {@code Analyzer produces one
 * Result per applicable scope instance}, {@code Analyzer
 * Independence}, {@code Analyzer Execution Order Independence}, and
 * {@code Repeated analysis over unchanged input is identical}.
 */
class AnalysisOrchestratorTest {

  @Test
  void producesOneResultPerApplicableScopeInstance() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    CsmElementId m1 = builder.module("moduleA");
    CsmElementId m2 = builder.module("moduleB");
    CsmSnapshotSource source = builder.build();

    AnalyzerRegistry registry = new AnalyzerRegistry();
    registry.register(
        new aip.analysis.test.fixtures.StubAnalyzer(
            "analyzer.per-module", 1, CsmScope.ofEntityKinds(Set.of(CsmEntityKind.MODULE)).anchoredAt(CsmEntityKind.MODULE)));

    List<AnalysisResult> results = new AnalysisOrchestrator(registry).run(source);

    assertEquals(2, results.size());
    Set<CsmScopeInstance> instances = Set.of(results.get(0).scopeInstance(), results.get(1).scopeInstance());
    assertEquals(Set.of(CsmScopeInstance.anchoredAt(m1), CsmScopeInstance.anchoredAt(m2)), instances);
  }

  @Test
  void inapplicableAnalyzerProducesNoResult() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    builder.module("moduleA");
    CsmSnapshotSource source = builder.build();

    AnalyzerRegistry registry = new AnalyzerRegistry();
    registry.register(
        new aip.analysis.test.fixtures.StubAnalyzer(
            "analyzer.packages-only", 1, CsmScope.ofEntityKinds(Set.of(CsmEntityKind.PACKAGE))));

    List<AnalysisResult> results = new AnalysisOrchestrator(registry).run(source);
    assertTrue(results.isEmpty());
  }

  @Test
  void analyzerResultIsUnaffectedByWhichOtherAnalyzersAreRegistered() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    builder.module("moduleA");
    CsmSnapshotSource source = builder.build();

    AnalyzerRegistry alone = new AnalyzerRegistry();
    alone.register(analyzerCountingModules("analyzer.counter", 1));
    AnalysisResult aloneResult = new AnalysisOrchestrator(alone).run(source).get(0);

    AnalyzerRegistry withOthers = new AnalyzerRegistry();
    withOthers.register(analyzerCountingModules("analyzer.counter", 1));
    withOthers.register(analyzerCountingModules("analyzer.other", 1));
    List<AnalysisResult> withOthersResults = new AnalysisOrchestrator(withOthers).run(source);
    AnalysisResult sameAnalyzerResult =
        withOthersResults.stream().filter(r -> r.analyzerIdentifier().equals("analyzer.counter")).findFirst().orElseThrow();

    assertEquals(aloneResult.payload(), sameAnalyzerResult.payload());
    assertEquals(aloneResult.id(), sameAnalyzerResult.id());
  }

  @Test
  void repeatedAnalysisOverUnchangedInputIsIdentical() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    builder.module("moduleA");
    CsmSnapshotSource source = builder.build();

    AnalyzerRegistry registry = new AnalyzerRegistry();
    registry.register(analyzerCountingModules("analyzer.counter", 1));

    List<AnalysisResult> first = new AnalysisOrchestrator(registry).run(source);
    List<AnalysisResult> second = new AnalysisOrchestrator(registry).run(source);

    assertEquals(first.size(), second.size());
    assertEquals(first.get(0).id(), second.get(0).id());
    assertEquals(first.get(0).payload(), second.get(0).payload());
  }

  @Test
  void runAcceptsAnAlreadyConstructedAnalysisView() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    builder.module("moduleA");
    AnalysisView view = AnalysisView.from(builder.build());

    AnalyzerRegistry registry = new AnalyzerRegistry();
    registry.register(analyzerCountingModules("analyzer.counter", 1));

    List<AnalysisResult> results = new AnalysisOrchestrator(registry).run(view);
    assertEquals(1, results.size());
  }

  private static aip.analysis.test.fixtures.StubAnalyzer analyzerCountingModules(String identifier, int version) {
    return new aip.analysis.test.fixtures.StubAnalyzer(
        identifier,
        version,
        CsmScope.ofEntityKinds(Set.of(CsmEntityKind.MODULE)),
        (view, instance) -> view.elementsOfKind(CsmEntityKind.MODULE).size());
  }
}
