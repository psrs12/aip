package aip.core.csm;

import java.util.Objects;
import java.util.Optional;

/**
 * The specific unit a {@link CsmScope}-declaring consumer (an
 * Analyzer, or later a Rule Type) is invoked against, per {@code
 * Unanchored Analyzer is invoked once per repository} and {@code
 * Anchored Analyzer is invoked once per matching contained element}.
 *
 * <p>Distinct from {@link CsmScope} itself: a {@code CsmScope} is a
 * static, registration-time declaration ("I read {@code Module} and
 * {@code Package} elements, anchored at {@code Module}"); a {@code
 * CsmScopeInstance} is one concrete invocation of that declaration
 * ("this specific Module"). This is the value {@link
 * aip.core.csm.AnalysisResultId}-shaped identity schemes fold in — a
 * hashable, equality-comparable record with no predicate or other
 * non-comparable content, unlike {@link CsmScope} itself.
 */
public record CsmScopeInstance(Optional<CsmElementId> anchorElementId) {

  public CsmScopeInstance {
    Objects.requireNonNull(anchorElementId, "anchorElementId");
  }

  /** The unanchored instance: covers the whole repository. */
  public static CsmScopeInstance wholeRepository() {
    return new CsmScopeInstance(Optional.empty());
  }

  /** The instance anchored at the contained element {@code elementId}. */
  public static CsmScopeInstance anchoredAt(CsmElementId elementId) {
    Objects.requireNonNull(elementId, "elementId");
    return new CsmScopeInstance(Optional.of(elementId));
  }

  public boolean isAnchored() {
    return anchorElementId.isPresent();
  }

  @Override
  public String toString() {
    return anchorElementId.map(CsmElementId::toString).orElse("<whole-repository>");
  }
}
