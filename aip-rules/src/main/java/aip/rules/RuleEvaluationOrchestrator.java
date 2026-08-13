package aip.rules;

import aip.core.csm.AnalysisResult;
import aip.core.csm.AnalysisResultId;
import aip.core.csm.AnalysisResultSource;
import aip.core.csm.AnalysisView;
import aip.core.csm.CsmScope;
import aip.core.csm.CsmScopeEvaluator;
import aip.core.csm.CsmScopeInstance;
import aip.core.csm.RuleEvaluationResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Dispatches every applicable (Rule, Rule Scope instance) pair, per
 * {@code Rule Type Contract} and the binding distinction between "not
 * applicable, no Result at all" (kind-based/native-attribute
 * applicability failure — Section 8) and "applicable and evaluated,
 * {@code NOT_APPLICABLE} Result" (the Rule Type's own {@link
 * RuleTypeEvaluation} decides this, once invoked).
 *
 * <p>Obtains the source CSM Snapshot identity from {@code view}
 * itself ({@link AnalysisView#sourceSnapshotId()}) — never a direct
 * {@code CsmSnapshotSource} dependency, per {@code define-rule-
 * framework/design.md} Decision 4. Consumes {@link AnalysisResult}s
 * exclusively through {@link AnalysisResultSource}, never a direct
 * dependency on the module that produced them.
 *
 * <p>For a given (Rule, Scope instance) pair, every {@link
 * AnalysisResult} each of that Rule's Rule Type's declared Analyzer
 * identifiers produced against the current snapshot is resolved and
 * passed to {@link RuleType#evaluate} — not pre-filtered to the
 * current Scope instance's own anchor, since {@link
 * AnalysisResultSource}'s own contract has no scope-instance-level
 * filtering to offer (list is by Analyzer identifier and snapshot
 * identity only). The complete union of resolved identities becomes
 * the Result's own consumed-Analysis-Result set, per {@code
 * Deterministic Rule Evaluation Result Identity}.
 *
 * <p>Sequential v1 dispatch — mirrors {@code aip-analysis}'s own
 * {@code AnalysisOrchestrator}; no concurrency infrastructure.
 */
public final class RuleEvaluationOrchestrator {

  private final RuleTypeRegistry ruleTypeRegistry;
  private final RuleRegistry ruleRegistry;
  private final AnalysisResultSource analysisResultSource;

  public RuleEvaluationOrchestrator(
      RuleTypeRegistry ruleTypeRegistry, RuleRegistry ruleRegistry, AnalysisResultSource analysisResultSource) {
    this.ruleTypeRegistry = Objects.requireNonNull(ruleTypeRegistry, "ruleTypeRegistry");
    this.ruleRegistry = Objects.requireNonNull(ruleRegistry, "ruleRegistry");
    this.analysisResultSource = Objects.requireNonNull(analysisResultSource, "analysisResultSource");
  }

  public List<RuleEvaluationResult> run(AnalysisView view) {
    Objects.requireNonNull(view, "view");

    List<RuleEvaluationResult> results = new ArrayList<>();
    for (Rule rule : ruleRegistry.registered()) {
      RuleType ruleType =
          ruleTypeRegistry
              .lookup(rule.ruleTypeIdentifier())
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "Rule " + rule.identifier() + " references unregistered Rule Type "
                              + rule.ruleTypeIdentifier() + " — RuleRegistry SHALL have rejected this at"
                              + " registration time"));

      CsmScope scope = ruleType.scope();
      for (CsmScopeInstance instance : CsmScopeEvaluator.enumerateInstances(view, scope)) {
        if (!CsmScopeEvaluator.isApplicable(view, scope, instance)) {
          // Kind-based/native-attribute applicability failure: the Rule
          // Type is never invoked for this instance, and no
          // RuleEvaluationResult of any outcome is produced.
          continue;
        }

        Map<String, Set<AnalysisResult>> consumedByAnalyzer = new LinkedHashMap<>();
        Set<AnalysisResultId> consumedIds = new LinkedHashSet<>();
        for (String analyzerIdentifier : ruleType.requiredAnalyzerIdentifiers()) {
          Set<AnalysisResult> analyzerResults = analysisResultSource.list(analyzerIdentifier, view.sourceSnapshotId());
          consumedByAnalyzer.put(analyzerIdentifier, analyzerResults);
          for (AnalysisResult analysisResult : analyzerResults) {
            consumedIds.add(analysisResult.id());
          }
        }

        RuleTypeEvaluation evaluation = ruleType.evaluate(rule, view, instance, consumedByAnalyzer);
        RuleEvaluationResult result =
            RuleEvaluationResult.of(
                rule.identifier(),
                rule.version(),
                view.sourceSnapshotId(),
                instance,
                consumedIds,
                evaluation.outcome(),
                evaluation.payload());
        results.add(result);
      }
    }
    return results;
  }
}
