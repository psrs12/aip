package aip.analysis;

import aip.core.csm.AnalysisResult;
import aip.core.csm.AnalysisView;
import aip.core.csm.CsmScope;
import aip.core.csm.CsmScopeEvaluator;
import aip.core.csm.CsmScopeInstance;
import aip.core.csm.CsmSnapshotSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Constructs exactly one {@link AnalysisView} per analysis run before
 * invoking any Analyzer, then dispatches every applicable (Analyzer,
 * Analysis Scope instance) pair, per {@code Analysis View
 * Construction} and {@code Analyzer Contract}.
 *
 * <p>v1 dispatch is a simple sequential loop — no concurrency
 * infrastructure (thread pool, executor service) is introduced, per
 * {@code define-analysis-framework/design.md} Decision 6. {@link
 * AnalyzerRegistry#registered()}'s own deterministic (identifier-order)
 * iteration, combined with each Analyzer's own independence (per
 * {@code Analyzer Independence}), is what makes {@code Analyzer
 * Execution Order Independence} hold: no Analyzer's own result depends
 * on this loop's iteration order, since {@link Analyzer#analyze} never
 * receives anything about other Analyzers or their results.
 */
public final class AnalysisOrchestrator {

  private final AnalyzerRegistry registry;

  public AnalysisOrchestrator(AnalyzerRegistry registry) {
    this.registry = Objects.requireNonNull(registry, "registry");
  }

  /**
   * Runs every registered, applicable Analyzer against {@code source},
   * producing one {@link AnalysisResult} per applicable (Analyzer,
   * Scope instance) pair — an inapplicable pair is skipped without
   * error, per {@code Kind-Based Analyzer Applicability}.
   */
  public List<AnalysisResult> run(CsmSnapshotSource source) {
    Objects.requireNonNull(source, "source");
    AnalysisView view = AnalysisView.from(source);
    return run(view);
  }

  /** Runs against an already-constructed {@link AnalysisView} — used when a caller has already built one. */
  public List<AnalysisResult> run(AnalysisView view) {
    Objects.requireNonNull(view, "view");

    List<AnalysisResult> results = new ArrayList<>();
    for (Analyzer analyzer : registry.registered()) {
      CsmScope scope = analyzer.scope();
      for (CsmScopeInstance instance : CsmScopeEvaluator.enumerateInstances(view, scope)) {
        if (!CsmScopeEvaluator.isApplicable(view, scope, instance)) {
          continue;
        }
        Object payload = analyzer.analyze(view, instance);
        AnalysisResult result =
            AnalysisResult.of(analyzer.identifier(), analyzer.version(), view.sourceSnapshotId(), instance, payload);
        results.add(result);
      }
    }
    return results;
  }
}
