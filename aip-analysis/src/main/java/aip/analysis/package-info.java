/**
 * The Analysis Framework: deterministic, independently pluggable
 * Analyzers running against a CSM Snapshot's {@link
 * aip.core.csm.AnalysisView}, producing durable, traceable {@link
 * aip.core.csm.AnalysisResult}s.
 *
 * <p>Depends on {@code aip-core} only, per {@code
 * define-analysis-framework/design.md} Decision 1 — this package
 * SHALL NOT depend on {@code aip-csm-builder}, {@code aip-analyzer},
 * or any Policy/Rule Model module.
 *
 * <p>{@link aip.core.csm.CsmSnapshotSource}, {@link
 * aip.core.csm.AnalysisView}, {@link aip.core.csm.CsmScope}, {@link
 * aip.core.csm.AnalysisResult}, and {@link
 * aip.core.csm.AnalysisResultId} all live in {@code aip-core}, per
 * {@code implement-analysis-framework/design.md} Decisions 1-4 — this
 * package supplies orchestration ({@link
 * aip.analysis.AnalysisOrchestrator}), the Analyzer extension point
 * ({@link aip.analysis.Analyzer}, {@link aip.analysis.AnalyzerRegistry}),
 * and the persistence/validation gate ({@link
 * aip.analysis.AnalysisResultStore}, {@link
 * aip.analysis.AnalysisResultValidator}, {@link
 * aip.analysis.AnalysisResultPublisher}) around those {@code aip-core}
 * types, never redefining them.
 */
package aip.analysis;
