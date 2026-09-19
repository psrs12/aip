package aip.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.analysis.test.fixtures.CsmSnapshotSourceBuilder;
import aip.analysis.test.fixtures.StubAnalyzer;
import aip.core.csm.AnalysisView;
import aip.core.csm.CsmEntityKind;
import aip.core.csm.CsmScope;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * {@link IncrementalAnalysis} tests, per {@code Incremental Analysis
 * Is Architecturally Supported}.
 */
class IncrementalAnalysisTest {

  private static final CsmScope MODULE_SCOPE = CsmScope.ofEntityKinds(Set.of(CsmEntityKind.MODULE));
  private static final CsmScope PACKAGE_SCOPE = CsmScope.ofEntityKinds(Set.of(CsmEntityKind.PACKAGE));

  @Test
  void twoViewsOfTheSameRepositoryCanBeComparedForAGivenAnalyzersScope() {
    AnalysisView earlier = moduleView("repo", 1, "moduleA");
    AnalysisView later = moduleView("repo", 2, "moduleA");

    // Comparability itself: no exception, a boolean answer either way.
    boolean differs =
        aip.core.csm.CsmScopeChangeDetector.scopeContentDiffers(earlier, later, MODULE_SCOPE);
    assertFalse(differs);
  }

  @Test
  void analyzerIsACandidateWhenItsDeclaredScopeContentChanged() {
    AnalyzerRegistry registry = new AnalyzerRegistry();
    Analyzer moduleAnalyzer = new StubAnalyzer("analyzer.module", 1, MODULE_SCOPE);
    registry.register(moduleAnalyzer);

    AnalysisView earlier = moduleView("repo", 1, "moduleA");
    AnalysisView later = moduleViewWithTwoModules("repo", 2, "moduleA", "moduleB");

    Set<Analyzer> candidates = IncrementalAnalysis.reexecutionCandidates(registry, earlier, later);

    assertEquals(Set.of(moduleAnalyzer), candidates);
  }

  @Test
  void analyzerIsNotACandidateWhenOnlyOutOfScopeContentChanged() {
    AnalyzerRegistry registry = new AnalyzerRegistry();
    Analyzer moduleAnalyzer = new StubAnalyzer("analyzer.module", 1, MODULE_SCOPE);
    registry.register(moduleAnalyzer);

    AnalysisView earlier = moduleView("repo", 1, "moduleA");
    // Only a PACKAGE is added between snapshots; analyzer.module reads MODULE only.
    AnalysisView later = moduleAndPackageView("repo", 2, "moduleA", "pkgA");

    Set<Analyzer> candidates = IncrementalAnalysis.reexecutionCandidates(registry, earlier, later);

    assertTrue(candidates.isEmpty());
  }

  @Test
  void noDeclaredDependencyBetweenAnalyzersIsRequiredOrHonored() {
    AnalyzerRegistry registry = new AnalyzerRegistry();
    Analyzer moduleAnalyzer = new StubAnalyzer("analyzer.module", 1, MODULE_SCOPE);
    Analyzer packageAnalyzer = new StubAnalyzer("analyzer.package", 1, PACKAGE_SCOPE);
    registry.register(moduleAnalyzer);
    registry.register(packageAnalyzer);

    AnalysisView earlier = moduleView("repo", 1, "moduleA");
    // Only MODULE content changes; PACKAGE-scoped analyzer must not be
    // affected by, or need to know about, the MODULE-scoped one.
    AnalysisView later = moduleViewWithTwoModules("repo", 2, "moduleA", "moduleB");

    Set<Analyzer> candidates = IncrementalAnalysis.reexecutionCandidates(registry, earlier, later);

    assertEquals(Set.of(moduleAnalyzer), candidates);
  }

  private static AnalysisView moduleView(String repo, long sequenceNumber, String moduleName) {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository(repo, sequenceNumber);
    builder.module(moduleName);
    return AnalysisView.from(builder.build());
  }

  private static AnalysisView moduleViewWithTwoModules(
      String repo, long sequenceNumber, String moduleName1, String moduleName2) {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository(repo, sequenceNumber);
    builder.module(moduleName1);
    builder.module(moduleName2);
    return AnalysisView.from(builder.build());
  }

  private static AnalysisView moduleAndPackageView(
      String repo, long sequenceNumber, String moduleName, String packageName) {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository(repo, sequenceNumber);
    builder.module(moduleName);
    builder.pkg(packageName);
    return AnalysisView.from(builder.build());
  }
}
