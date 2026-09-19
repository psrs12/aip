package aip.core.csm;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Determines, given two {@link AnalysisView}s of the same repository,
 * whether the CSM content within a declared {@link CsmScope} differs
 * between them — the mechanism {@code Incremental Analysis Is
 * Architecturally Supported} requires: "sufficient to determine...
 * which registered Analyzers may need re-execution because CSM content
 * within their declared Analysis Scope differs between those two
 * snapshots... using only each Analyzer's declared Analysis Scope...
 * and the CSM content of the two snapshots being compared."
 *
 * <p>Deliberately coarse, per that same requirement and {@code
 * define-analysis-framework/design.md} Decision 7's closing paragraph:
 * this reports only whether <em>some</em> element or relationship
 * within a Scope's whole-repository content differs, by set equality
 * over {@link CsmElement}/{@link CsmRelationship} (both already value
 * types) — never a finer-grained, per-Subject or per-instance
 * invalidation determination, and never anything keyed by a declared
 * dependency between two Scope-declaring consumers (an Analyzer, or a
 * Rule Type). No such dependency can even be expressed here; this type
 * accepts two {@link AnalysisView}s and one {@link CsmScope} only.
 *
 * <p>Hosted in {@code aip-core}, alongside {@link CsmScopeEvaluator} —
 * the same general-mechanism placement precedent {@code CsmScope}
 * itself already established, since a future Rule Framework consumer
 * needing an identical "did my declared Scope's content change between
 * two snapshots" answer would otherwise have to duplicate this logic
 * rather than reuse it.
 */
public final class CsmScopeChangeDetector {

  private CsmScopeChangeDetector() {}

  /**
   * Whether {@code scope}'s whole-repository content — every element
   * and relationship {@link CsmScopeEvaluator#contentElements}/{@code
   * contentRelationships} would return for {@link
   * CsmScopeInstance#wholeRepository()} — differs between {@code
   * earlier} and {@code later}. Anchored Scopes are evaluated over
   * their full, whole-repository content here too: this is a
   * repository-wide "might this Scope be affected at all" check, not a
   * per-anchor-instance one — the coarseness this requirement
   * explicitly permits.
   */
  public static boolean scopeContentDiffers(AnalysisView earlier, AnalysisView later, CsmScope scope) {
    Objects.requireNonNull(earlier, "earlier");
    Objects.requireNonNull(later, "later");
    Objects.requireNonNull(scope, "scope");

    Set<CsmElement> earlierElements = scopedElements(earlier, scope);
    Set<CsmElement> laterElements = scopedElements(later, scope);
    if (!earlierElements.equals(laterElements)) {
      return true;
    }

    Set<CsmRelationship> earlierRelationships = scopedRelationships(earlier, scope);
    Set<CsmRelationship> laterRelationships = scopedRelationships(later, scope);
    return !earlierRelationships.equals(laterRelationships);
  }

  private static Set<CsmElement> scopedElements(AnalysisView view, CsmScope scope) {
    Set<CsmElement> all = CsmScopeEvaluator.contentElements(view, CsmScopeInstance.wholeRepository());
    Set<CsmElement> matching = new LinkedHashSet<>();
    for (CsmElement element : all) {
      if (scope.declaresEntityKind(element.kind())) {
        matching.add(element);
      }
    }
    return matching;
  }

  private static Set<CsmRelationship> scopedRelationships(AnalysisView view, CsmScope scope) {
    Set<CsmRelationship> all = CsmScopeEvaluator.contentRelationships(view, CsmScopeInstance.wholeRepository());
    Set<CsmRelationship> matching = new LinkedHashSet<>();
    for (CsmRelationship relationship : all) {
      if (scope.declaresRelationshipType(relationship.type())) {
        matching.add(relationship);
      }
    }
    return matching;
  }
}
