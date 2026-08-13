package aip.analysis.test.fixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.analysis.AnalysisOrchestrator;
import aip.analysis.AnalysisResultPublisher;
import aip.analysis.AnalysisResultValidator;
import aip.analysis.Analyzer;
import aip.analysis.AnalyzerRegistry;
import aip.core.csm.AnalysisResult;
import aip.core.csm.AnalysisView;
import aip.core.csm.CsmElementId;
import aip.core.csm.CsmEntityKind;
import aip.core.csm.CsmRelationshipType;
import aip.core.csm.CsmScope;
import aip.core.csm.CsmSnapshotSource;
import aip.core.csm.DependencyKind;
import aip.core.csm.ValidationResult;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * A representative, multi-Analyzer, multi-scope-instance end-to-end
 * scenario: construct a fixture CSM Snapshot, run the full Analysis
 * Orchestrator, validate and publish every Result, then retrieve each
 * by identity — mirroring {@code aip-csm-builder}'s own {@code
 * CsmBuilderFixtureEndToEndTest}.
 */
class AnalysisFrameworkFixtureEndToEndTest {

  @Test
  void multiAnalyzerMultiScopeInstanceRunProducesValidatedPublishedRetrievableResults() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    CsmElementId moduleA = builder.module("moduleA");
    CsmElementId moduleB = builder.module("moduleB");
    CsmElementId typeInA = builder.type("TypeInA");
    builder.containment(moduleA, typeInA);
    builder.dependency(moduleA, moduleB, DependencyKind.COMPILE_TIME);
    CsmSnapshotSource source = builder.build();

    AnalyzerRegistry registry = new AnalyzerRegistry();
    // Repository-wide Analyzer: counts every Module in the snapshot.
    Analyzer moduleCounter =
        new StubAnalyzer(
            "analyzer.module-counter",
            1,
            CsmScope.ofEntityKinds(Set.of(CsmEntityKind.MODULE)),
            (view, instance) -> view.elementsOfKind(CsmEntityKind.MODULE).size());
    // Per-Module Analyzer: counts each Module's own outgoing dependencies.
    Analyzer perModuleDependencyCounter =
        new StubAnalyzer(
            "analyzer.dependency-counter",
            1,
            CsmScope.ofRelationshipTypes(Set.of(CsmRelationshipType.DEPENDENCY)).anchoredAt(CsmEntityKind.MODULE),
            (view, instance) ->
                (int)
                    view.relationships().stream()
                        .filter(r -> r.type() == CsmRelationshipType.DEPENDENCY)
                        .filter(r -> r.sourceId().equals(instance.anchorElementId().orElseThrow()))
                        .count());
    registry.register(moduleCounter);
    registry.register(perModuleDependencyCounter);

    AnalysisView view = AnalysisView.from(source);
    List<AnalysisResult> results = new AnalysisOrchestrator(registry).run(view);

    // moduleCounter: one whole-repository Result. dependencyCounter: one
    // Result per Module present (moduleA, moduleB) - moduleA is
    // applicable (it has an outgoing dependency); moduleB has none, but
    // is still applicable because it is anchored per matching contained
    // element regardless of its own dependency count (dependency
    // relationships targeting it, not from it, don't affect its own
    // applicability here since the Scope also declares no entity kind -
    // applicability is kind-based on relationship presence anywhere
    // reachable from the anchor's own closure, which for moduleB (no
    // outgoing or contained dependency edges) is empty, so moduleB is
    // correctly skipped).
    assertEquals(1, results.stream().filter(r -> r.analyzerIdentifier().equals("analyzer.module-counter")).count());

    InMemoryAnalysisResultStore store = new InMemoryAnalysisResultStore();
    for (AnalysisResult result : results) {
      ValidationResult validation = AnalysisResultValidator.validate(result, view, registry);
      assertTrue(validation.valid(), () -> "expected valid for " + result.id() + ": " + validation.violations());

      AnalysisResultPublisher.PublicationOutcome outcome =
          AnalysisResultPublisher.publish(store, result, view, registry);
      assertTrue(outcome.published());
      assertEquals(result.id(), store.read(result.id()).orElseThrow().id());
    }
  }
}
