/**
 * The Rule Framework: deterministic, declarative Rule Types evaluated
 * against Rules configured against them, producing durable, traceable
 * {@link aip.core.csm.RuleEvaluationResult}s.
 *
 * <p>Depends on {@code aip-core} only, per {@code
 * define-rule-framework/design.md} Decision 7 — this package SHALL
 * NOT depend on {@code aip-analysis}, {@code aip-csm-builder}, or
 * {@code aip-analyzer}.
 *
 * <p>{@link aip.core.csm.AnalysisResultSource}, {@link
 * aip.core.csm.RuleEvaluationResult}, {@link
 * aip.core.csm.RuleEvaluationResultId}, and {@link
 * aip.core.csm.RuleEvaluationOutcome} all live in {@code aip-core},
 * per {@code implement-rule-framework/design.md} Decisions 1, 2, 4,
 * 5 — this package supplies orchestration ({@link
 * aip.rules.RuleEvaluationOrchestrator}), the Rule Type/Rule extension
 * points ({@link aip.rules.RuleType}, {@link aip.rules.Rule}, {@link
 * aip.rules.RuleTypeRegistry}, {@link aip.rules.RuleRegistry}), and
 * the persistence/validation gate ({@link
 * aip.rules.RuleEvaluationResultStore}, {@link
 * aip.rules.RuleEvaluationResultValidator}, {@link
 * aip.rules.RuleEvaluationResultPublisher}) around those {@code
 * aip-core} types, never redefining them. Rule Scope is {@link
 * aip.core.csm.CsmScope}, reused verbatim — no wrapper type exists in
 * this package.
 */
package aip.rules;
