/**
 * The Finding Model: deterministic interpretation of Rule Framework's
 * {@link aip.core.csm.RuleEvaluationResult}s into durable, traceable
 * {@link aip.core.csm.Finding}s.
 *
 * <p>Depends on {@code aip-core} only, per {@code
 * define-finding-model/design.md} Decisions 10, 11 — this package
 * SHALL NOT depend on {@code aip-rules}, {@code aip-analysis}, {@code
 * aip-csm-builder}, or {@code aip-analyzer}.
 *
 * <p>{@link aip.core.csm.RuleEvaluationResultSource}, {@link
 * aip.core.csm.Finding}, {@link aip.core.csm.EvaluationIdentity},
 * {@link aip.core.csm.LogicalFindingIdentity}, and {@link
 * aip.core.csm.FindingMetadata} all live in {@code aip-core}, per
 * {@code implement-finding-model/design.md} Decisions 1, 2, 3, 7 —
 * this package supplies construction ({@link
 * aip.findings.FindingConstructor}) and the persistence/validation
 * gate ({@link aip.findings.FindingStore}, {@link
 * aip.findings.FindingValidator}, {@link
 * aip.findings.FindingPublisher}) around those {@code aip-core} types,
 * never redefining them. Finding Model has no Scope concept of its
 * own ({@code define-finding-model/design.md} Decision 8) — no
 * wrapper or applicability type exists in this package.
 */
package aip.findings;
