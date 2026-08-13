/**
 * The Agent Framework: the generic {@link aip.ai.Agent} contract,
 * registration ({@link aip.ai.AgentRegistry}), and the
 * {@link aip.ai.Recommendation} artifact it produces from a {@link
 * aip.core.csm.Finding}.
 *
 * <p>Depends on {@code aip-core} only, per {@code
 * define-agent-framework/design.md} Decisions 4, 8 — this package
 * SHALL NOT depend on {@code aip-findings}, {@code aip-rules}, {@code
 * aip-analysis}, {@code aip-csm-builder}, or {@code aip-analyzer}.
 * {@link aip.core.csm.FindingSource} is the only channel through which
 * this package reads a Finding.
 *
 * <p><strong>This is the first, and only, AI-bearing module in this
 * codebase.</strong> {@code Recommendation}, {@code
 * RecommendationArtifactIdentity}, {@code GenerationIdentifier}, and
 * {@code GenerationProvenance} are deliberately {@code aip-ai}-local,
 * not promoted to {@code aip-core} ({@code define-agent-framework/
 * design.md} Decision 8; {@code implement-agent-framework/design.md}
 * Decision 2 — no capability named anywhere in {@code project.md} or
 * this project's history yet needs to read Recommendations as an
 * upstream input, unlike {@code AnalysisResult}/{@code
 * RuleEvaluationResult}/{@code Finding}, each promoted for a specific,
 * already-named future consumer).
 *
 * <p><strong>{@code check-no-ai-heuristic-imports.sh} is deliberately
 * NOT wired against this module's POM</strong> ({@code
 * implement-agent-framework/design.md} Decision 9). That guard exists
 * to keep the five deterministic modules ({@code aip-csm-builder},
 * {@code aip-analysis}, {@code aip-rules}, {@code aip-findings}, and
 * the not-yet-built {@code aip-analyzer}) free of randomness and
 * AI/LLM SDK imports. {@link aip.ai.GenerationIdentifier#generate()}'s
 * {@code UUID.randomUUID()} call is the one legitimate,
 * deliberately-scoped exception in the entire codebase — a Generation
 * Identifier exists precisely because the same (Agent, version,
 * Finding) triple can legitimately produce more than one
 * independently-valid Recommendation across separate invocations, and
 * this token's only job is uniqueness per invocation, never
 * reproducibility. No other type or method in this package uses
 * randomness, and no deterministic module anywhere in this codebase
 * depends on {@code aip-ai} — each of their own {@code
 * check-module-dependencies.sh} executions already, structurally,
 * makes that impossible.
 */
package aip.ai;
