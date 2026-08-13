package aip.analysis;

import aip.core.csm.AnalysisView;
import aip.core.csm.CsmScope;
import aip.core.csm.CsmScopeInstance;

/**
 * A registered Analyzer, per {@code Analyzer Contract}: "A registered
 * Analyzer SHALL declare a stable identifier, a monotonically
 * versioned identifier, and an explicit Analysis Scope... Given an
 * Analysis View and an Analysis Scope instance it is applicable to, an
 * Analyzer SHALL deterministically produce exactly one Analysis
 * Result for that instance."
 *
 * <p>{@link #analyze} returns only the Analyzer's own opaque payload —
 * {@link AnalysisOrchestrator} wraps it into a full {@link
 * aip.core.csm.AnalysisResult}, computing identity/traceability from
 * the invocation context, not from anything the Analyzer itself
 * supplies. This keeps an Analyzer implementation from ever needing to
 * duplicate identity-derivation logic.
 *
 * <p>Per {@code Analyzer Independence}, an implementation SHALL NOT
 * read another Analyzer's {@link aip.core.csm.AnalysisResult}, and
 * SHALL NOT depend on any other Analyzer having been registered,
 * invoked, or having produced a result — nothing in this interface's
 * own shape provides a way to do either, so this is enforced
 * structurally, not merely documented.
 */
public interface Analyzer {

  /** This Analyzer's stable identifier. */
  String identifier();

  /** This Analyzer's monotonically versioned identifier. */
  int version();

  /** This Analyzer's declared Analysis Scope. */
  CsmScope scope();

  /**
   * Deterministically computes this Analyzer's opaque analytical
   * payload for {@code scopeInstance}, using only {@code view}
   * content — no AI/LLM call, no heuristic or probabilistic scoring
   * step, per {@code Deterministic, Snapshot-Driven Analysis Only}.
   */
  Object analyze(AnalysisView view, CsmScopeInstance scopeInstance);
}
