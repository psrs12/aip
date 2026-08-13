package aip.rules;

import aip.core.csm.CsmSnapshotId;
import aip.core.csm.RuleEvaluationResult;
import aip.core.csm.RuleEvaluationResultId;
import java.util.List;
import java.util.Optional;

/**
 * The Rule Evaluation Result Store abstraction: writes and retrieves
 * {@link RuleEvaluationResult}s, per {@code Rule Evaluation Results
 * Are Durable, Individually Identifiable Artifacts} — mirroring {@code
 * aip-analysis}'s own {@code AnalysisResultStore} precedent (per
 * {@code implement-rule-framework/design.md} Decision 6,
 * <strong>not</strong> promoted to {@code aip-core} — the artifact
 * moved, the store abstraction did not).
 *
 * <p>Concrete persistence technology is deliberately not chosen here.
 */
public interface RuleEvaluationResultStore {

  /** Writes {@code result} as a new, immutable artifact. Never overwrites a previously written result. */
  RuleEvaluationResult write(RuleEvaluationResult result);

  /** The result with the given identity, if one was written. */
  Optional<RuleEvaluationResult> read(RuleEvaluationResultId id);

  /** Every result produced by {@code ruleIdentifier} against {@code snapshotId}, if any. */
  List<RuleEvaluationResult> listByRuleAndSnapshot(String ruleIdentifier, CsmSnapshotId snapshotId);
}
