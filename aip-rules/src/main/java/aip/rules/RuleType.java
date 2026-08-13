package aip.rules;

import aip.core.csm.AnalysisResult;
import aip.core.csm.AnalysisView;
import aip.core.csm.CsmScope;
import aip.core.csm.CsmScopeInstance;
import java.util.Map;
import java.util.Set;

/**
 * A registered Rule Type — AIP-authored, versioned code implementing a
 * fixed evaluation contract — per {@code Rule Type Contract}: "A Rule
 * Type SHALL declare a stable identifier, a monotonically versioned
 * identifier, the set of Analyzer identifiers whose Analysis Results
 * it consumes, and a Rule Scope... for direct Analysis View reads. A
 * Rule Type SHALL be evaluable given exactly these declared inputs,
 * and SHALL NOT read any CSM or Analysis content outside them."
 *
 * <p>Organizations do not author Rule Types (that is what {@link
 * Rule}, the declarative configuration, is for) — a Rule Type
 * implements one <em>kind</em> of check, mirroring {@link
 * aip.core.csm.AnalysisResult}'s own producer, the Analyzer contract
 * in {@code aip-analysis}, one layer down.
 *
 * <p>{@link #evaluate}'s {@code consumedAnalysisResults} parameter is
 * keyed by this Rule Type's own declared {@link
 * #requiredAnalyzerIdentifiers()}, each value the complete set of
 * {@link AnalysisResult}s that Analyzer produced against the current
 * source CSM Snapshot — not pre-filtered to {@code scopeInstance}'s
 * own anchor, since {@link aip.core.csm.AnalysisResultSource}'s own
 * contract shape (list by Analyzer identifier and snapshot identity
 * only) has no scope-instance-level filtering to offer. A Rule Type
 * whose condition only cares about its own current anchor filters the
 * received set itself.
 */
public interface RuleType {

  /** This Rule Type's stable identifier. */
  String identifier();

  /** This Rule Type's monotonically versioned identifier. */
  int version();

  /** The Analyzer identifiers whose Analysis Results this Rule Type consumes. MAY be empty. */
  Set<String> requiredAnalyzerIdentifiers();

  /** This Rule Type's declared Rule Scope — {@link CsmScope}, reused verbatim, no wrapper type. */
  CsmScope scope();

  /**
   * Deterministically evaluates {@code rule}'s configuration against
   * {@code scopeInstance}, using only {@code view} content within its
   * declared Rule Scope and {@code consumedAnalysisResults} — no
   * AI/LLM call, no heuristic or probabilistic scoring step, per
   * {@code Deterministic, Declarative Rule Evaluation Only}.
   */
  RuleTypeEvaluation evaluate(
      Rule rule, AnalysisView view, CsmScopeInstance scopeInstance, Map<String, Set<AnalysisResult>> consumedAnalysisResults);
}
