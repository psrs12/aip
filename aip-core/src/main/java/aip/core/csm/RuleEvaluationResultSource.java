package aip.core.csm;

import java.util.Optional;
import java.util.Set;

/**
 * A read-only view over published {@link RuleEvaluationResult}s, per
 * {@code Rule Evaluation Result Source Shape}: "A Rule Evaluation
 * Result Source SHALL expose, at minimum: a RuleEvaluationResult
 * retrievable by its own identity, and the set of RuleEvaluationResults
 * produced by a given Rule identifier against a given source CSM
 * Snapshot identity."
 *
 * <p>Finding Model's own {@code aip-core} contribution
 * ({@code define-finding-model/design.md} Decision 11), not Rule
 * Framework's — mirroring {@link AnalysisResultSource}'s own shape.
 * Since {@link RuleEvaluationResult} already lives in {@code aip-core},
 * this contract's return types require no import from {@code
 * aip-rules} at all; {@code aip-findings} depends on this interface
 * only, never on {@code aip-rules}'s own {@code
 * RuleEvaluationResultStore}.
 */
public interface RuleEvaluationResultSource {

  /** The Rule Evaluation Result with the given identity, if one has been published. */
  Optional<RuleEvaluationResult> read(RuleEvaluationResultId id);

  /** Every Rule Evaluation Result {@code ruleIdentifier} produced against {@code snapshotId}, if any. */
  Set<RuleEvaluationResult> list(String ruleIdentifier, CsmSnapshotId snapshotId);
}
