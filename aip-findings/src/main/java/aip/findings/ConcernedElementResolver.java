package aip.findings;

import aip.core.csm.CsmElementId;
import aip.core.csm.CsmScopeInstance;
import aip.core.csm.CsmSnapshotId;

/**
 * Resolves the CSM element identity a {@code RuleEvaluationResult}'s
 * Rule Scope instance concerns — reused by both Logical Finding
 * Identity computation and a {@code Finding}'s Location field, per
 * {@code implement-finding-model/design.md} Decisions 2, 5.
 *
 * <p>Anchored case: the Rule Scope instance's own anchor element
 * identity, present directly on {@code CsmScopeInstance}. Unanchored
 * case: since the literal CSM Repository element identity is a
 * function of its originating Repository Evidence Item's identity
 * (reachable only from {@code aip-csm-builder}, a module {@code
 * aip-findings} has no dependency on), this resolver computes a
 * Finding-Model-local, deterministic stand-in from the source CSM
 * Snapshot's own {@code repositoryIdentifier} alone — stable across
 * CSM Snapshots of the same repository, but **not** guaranteed equal
 * to the real, `aip-csm-builder`-constructed Repository element
 * identity. Named explicitly as an accepted v1 limitation in {@code
 * implement-finding-model/design.md}'s Risks/Trade-offs.
 */
final class ConcernedElementResolver {

  private static final String REPOSITORY_SUBJECT_PREFIX = "aip-findings:repository-subject:";

  private ConcernedElementResolver() {}

  /**
   * The concerned CSM element identity for {@code scopeInstance},
   * evaluated against {@code sourceSnapshotId}.
   */
  static CsmElementId resolve(CsmScopeInstance scopeInstance, CsmSnapshotId sourceSnapshotId) {
    return scopeInstance
        .anchorElementId()
        .orElseGet(() -> repositorySubject(sourceSnapshotId));
  }

  /**
   * The Finding-Model-local synthetic identity standing in for "the
   * Repository CSM element" when a Rule Scope instance is unanchored.
   * Deterministic: the same repository identifier always yields the
   * same value.
   */
  private static CsmElementId repositorySubject(CsmSnapshotId sourceSnapshotId) {
    return new CsmElementId(REPOSITORY_SUBJECT_PREFIX + sourceSnapshotId.repositoryIdentifier());
  }
}
