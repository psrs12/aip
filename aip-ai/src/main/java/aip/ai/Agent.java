package aip.ai;

import aip.core.csm.Finding;

/**
 * The generic Agent contract, per {@code Agent Contract}: "An Agent
 * SHALL declare a stable identifier and a monotonically versioned
 * identifier. An Agent SHALL consume exactly one Finding per
 * invocation, obtained through the Finding Source, and SHALL produce
 * exactly one Recommendation per invocation."
 *
 * <p>{@link #invoke(Finding)} takes an already-resolved {@link
 * Finding} directly — the caller (never the Agent itself) is
 * responsible for obtaining it through {@code
 * aip.core.csm.FindingSource}, per {@code Agent Consumption Is Limited
 * to Findings Through the Finding Source}. An Agent's own logic SHALL
 * NOT read CSM content, Analysis Results, or RuleEvaluationResults
 * directly, and SHALL NOT depend on the module that produced the
 * Finding — this interface's own signature makes that structurally
 * impossible: no such parameter exists.
 *
 * <p>{@link #invoke(Finding)} MAY throw an unchecked exception to
 * signal a generation failure (e.g. an underlying model call failing
 * or timing out), per {@code Failure Semantics}. {@link
 * RecommendationConstructor} treats any such exception as "no
 * Recommendation produced for this invocation," never as a partially-
 * constructed or invalid Recommendation.
 */
public interface Agent {

  /** A stable identifier for this Agent. */
  String identifier();

  /** This Agent's monotonically increasing version. */
  int version();

  /**
   * Consumes {@code finding} and produces the Agent-declared portion
   * of a Recommendation. MAY throw an unchecked exception to signal a
   * generation failure.
   */
  AgentInvocationResult invoke(Finding finding);
}
