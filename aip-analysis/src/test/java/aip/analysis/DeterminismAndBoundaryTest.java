package aip.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.analysis.test.fixtures.CsmSnapshotSourceBuilder;
import aip.analysis.test.fixtures.StubAnalyzer;
import aip.core.csm.AnalysisResult;
import aip.core.csm.CsmElement;
import aip.core.csm.CsmEntityKind;
import aip.core.csm.CsmRelationship;
import aip.core.csm.CsmScope;
import aip.core.csm.CsmSnapshotSource;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Determinism and scope-boundary enforcement, per {@code CSM Snapshot
 * as Sole Analysis Input}, {@code Single-Repository Analysis Scope},
 * and {@code Analysis Results Are Not CSM Knowledge}.
 */
class DeterminismAndBoundaryTest {

  private static final CsmScope MODULE_SCOPE = CsmScope.ofEntityKinds(Set.of(CsmEntityKind.MODULE));

  @Test
  void analysisProceedsWithoutPolicyOrRuntimeDataConfigured() {
    // AnalysisOrchestrator.run's own signature takes a CsmSnapshotSource
    // (or an already-built AnalysisView) and nothing else — there is no
    // Policy/Rule Model or Runtime Model parameter to supply, so
    // "no governance policy or runtime telemetry has been configured"
    // is not a distinct code path to test, it is the only code path
    // that exists.
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    builder.module("moduleA");
    CsmSnapshotSource source = builder.build();

    AnalyzerRegistry registry = new AnalyzerRegistry();
    registry.register(new StubAnalyzer("analyzer.a", 1, MODULE_SCOPE));

    List<AnalysisResult> results = new AnalysisOrchestrator(registry).run(source);
    assertEquals(1, results.size());
  }

  @Test
  void everyResultFromOneRunReferencesExactlyOneRepositorysSnapshotIdentity() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    builder.module("moduleA");
    builder.module("moduleB");
    CsmSnapshotSource source = builder.build();

    AnalyzerRegistry registry = new AnalyzerRegistry();
    registry.register(new StubAnalyzer("analyzer.a", 1, MODULE_SCOPE.anchoredAt(CsmEntityKind.MODULE)));

    List<AnalysisResult> results = new AnalysisOrchestrator(registry).run(source);
    assertEquals(2, results.size());
    for (AnalysisResult result : results) {
      assertEquals(source.id(), result.sourceSnapshotId());
    }
  }

  @Test
  void producingAnAnalysisResultDoesNotAlterCsmSnapshotContent() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    builder.module("moduleA");
    CsmSnapshotSource source = builder.build();
    Set<CsmElement> elementsBefore = Set.copyOf(source.elements());
    Set<CsmRelationship> relationshipsBefore = Set.copyOf(source.relationships());

    AnalyzerRegistry registry = new AnalyzerRegistry();
    registry.register(new StubAnalyzer("analyzer.a", 1, MODULE_SCOPE));
    new AnalysisOrchestrator(registry).run(source);

    assertEquals(elementsBefore, source.elements());
    assertEquals(relationshipsBefore, source.relationships());
  }

  @Test
  void analysisResultIsNeverExposedAsCsmContent() {
    // AnalysisResult and CsmElement/CsmRelationship are disjoint types —
    // there is no shared query surface through which one could be
    // returned in place of the other; this is a structural guarantee,
    // not a runtime check, confirmed here for traceability.
    assertTrue(!CsmElement.class.isAssignableFrom(AnalysisResult.class));
    assertTrue(!CsmRelationship.class.isAssignableFrom(AnalysisResult.class));
  }
}
