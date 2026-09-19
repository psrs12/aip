package aip.analysis;

import aip.core.csm.AnalysisView;
import aip.core.csm.CsmScopeChangeDetector;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Determines which registered {@link Analyzer}s may need re-execution
 * between two {@link AnalysisView}s of the same repository, per {@code
 * Incremental Analysis Is Architecturally Supported}: "sufficient to
 * determine... which registered Analyzers may need re-execution
 * because CSM content within their declared Analysis Scope differs
 * between those two snapshots... using only each Analyzer's declared
 * Analysis Scope... and the CSM content of the two snapshots being
 * compared."
 *
 * <p>This is orchestration-level integration only — the actual
 * scope-content comparison is {@link CsmScopeChangeDetector}'s job (an
 * {@code aip-core} mechanism, per that type's own placement rationale),
 * mirroring how {@link AnalysisOrchestrator} itself only integrates
 * {@code aip-core}'s {@link aip.core.csm.CsmScopeEvaluator} rather than
 * re-deriving scope-instance enumeration or applicability itself.
 *
 * <p>No declared dependency or ordering between Analyzers is read,
 * accepted, or required — {@link AnalyzerRegistry#registered()} is
 * iterated only to ask, independently for each Analyzer, whether its
 * own declared Scope's content differs between the two views. Per
 * Design Decision 7's "coarse determination... is sufficient for v1,"
 * this never attempts a more precise, fine-grained invalidation
 * determination within an affected Analyzer's Scope.
 */
public final class IncrementalAnalysis {

  private IncrementalAnalysis() {}

  /**
   * The subset of {@code registry}'s registered Analyzers whose
   * declared Analysis Scope's content differs between {@code earlier}
   * and {@code later} — candidates for re-execution against {@code
   * later}. An Analyzer whose declared Scope shows no content
   * difference is omitted, even when unrelated, out-of-scope CSM
   * content differs between the two views.
   */
  public static Set<Analyzer> reexecutionCandidates(
      AnalyzerRegistry registry, AnalysisView earlier, AnalysisView later) {
    Objects.requireNonNull(registry, "registry");
    Objects.requireNonNull(earlier, "earlier");
    Objects.requireNonNull(later, "later");

    Set<Analyzer> candidates = new LinkedHashSet<>();
    for (Analyzer analyzer : registry.registered()) {
      if (CsmScopeChangeDetector.scopeContentDiffers(earlier, later, analyzer.scope())) {
        candidates.add(analyzer);
      }
    }
    return candidates;
  }
}
