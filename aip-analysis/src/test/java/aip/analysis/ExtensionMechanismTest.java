package aip.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.analysis.test.fixtures.CsmSnapshotSourceBuilder;
import aip.analysis.test.fixtures.InMemoryAnalysisResultStore;
import aip.analysis.test.fixtures.StubAnalyzer;
import aip.core.csm.AnalysisResult;
import aip.core.csm.AnalysisView;
import aip.core.csm.CsmEntityKind;
import aip.core.csm.CsmRelationshipType;
import aip.core.csm.CsmScope;
import aip.core.csm.CsmScopeInstance;
import aip.core.csm.ValidationResult;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Confirms registering a new Analyzer for a previously-unused Analysis
 * Scope requires no change to Analysis View construction, Result
 * identity/traceability, or validation mechanisms — mirroring {@code
 * implement-csm-builder}'s own Extension Mechanism Verification.
 */
class ExtensionMechanismTest {

  @Test
  void aStructurallyDistinctSecondAnalyzerUsesTheSameCoreMechanismsUnmodified() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    builder.module("moduleA");
    AnalysisView view = AnalysisView.from(builder.build());

    AnalyzerRegistry registry = new AnalyzerRegistry();
    // First Analyzer: unanchored, entity-kind scope.
    registry.register(new StubAnalyzer("analyzer.first", 1, CsmScope.ofEntityKinds(Set.of(CsmEntityKind.MODULE))));
    // Second Analyzer: structurally distinct — relationship-type scope,
    // anchored at a different containment level than the first even
    // declares.
    registry.register(
        new StubAnalyzer(
            "analyzer.second",
            1,
            CsmScope.ofRelationshipTypes(Set.of(CsmRelationshipType.DEPENDENCY)).anchoredAt(CsmEntityKind.MODULE)));

    AnalysisOrchestrator orchestrator = new AnalysisOrchestrator(registry);
    List<AnalysisResult> results = orchestrator.run(view);

    // The first Analyzer's own result is unaffected by the second
    // Analyzer's registration (Analyzer Independence, re-exercised here
    // specifically for a newly-added, structurally distinct Analyzer).
    AnalysisResult firstResult =
        results.stream().filter(r -> r.analyzerIdentifier().equals("analyzer.first")).findFirst().orElseThrow();
    assertEquals(CsmScopeInstance.wholeRepository(), firstResult.scopeInstance());

    // Identity, traceability, and validation mechanisms apply unchanged
    // to the new Analyzer's own results too.
    InMemoryAnalysisResultStore store = new InMemoryAnalysisResultStore();
    for (AnalysisResult result : results) {
      ValidationResult validation = AnalysisResultValidator.validate(result, view, registry);
      assertTrue(validation.valid(), () -> "expected valid: " + validation.violations());
      store.write(result);
      assertEquals(result, store.read(result.id()).orElseThrow());
    }
  }
}
