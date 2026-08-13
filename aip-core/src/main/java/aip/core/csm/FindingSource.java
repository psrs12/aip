package aip.core.csm;

import java.util.Optional;
import java.util.Set;

/**
 * A read-only view over published {@link Finding}s, per {@code
 * Finding Source Shape}: "A Finding Source SHALL expose, at minimum: a
 * Finding retrievable by its Evaluation Identity, and the set of
 * Findings sharing a given Logical Finding Identity or a given source
 * CSM Snapshot identity."
 *
 * <p>Agent Framework's own {@code aip-core} contribution
 * (`define-agent-framework/design.md` Decision 8), not Finding
 * Model's — mirroring {@link RuleEvaluationResultSource}'s own shape.
 * Since {@link Finding} already lives in {@code aip-core}, this
 * contract's return types require no import from {@code aip-findings}
 * at all; {@code aip-ai} depends on this interface only, never on
 * {@code aip-findings}'s own {@code FindingStore}.
 */
public interface FindingSource {

  /** The Finding with the given Evaluation Identity, if one has been published. */
  Optional<Finding> read(EvaluationIdentity id);

  /** Every Finding sharing the given Logical Finding Identity, if any. */
  Set<Finding> listByLogicalFindingIdentity(LogicalFindingIdentity id);

  /** Every Finding produced against the given source CSM Snapshot identity, if any. */
  Set<Finding> listBySourceSnapshotId(CsmSnapshotId snapshotId);
}
