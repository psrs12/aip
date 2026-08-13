package aip.findings.test.fixtures;

import aip.core.csm.CsmElementId;
import aip.core.csm.CsmScopeInstance;
import aip.core.csm.CsmSnapshotId;
import aip.core.csm.RuleEvaluationOutcome;
import aip.core.csm.RuleEvaluationResult;
import java.util.Set;

/**
 * Fixture {@code RuleEvaluationResult} construction, used throughout
 * this module's own tests. Finding Model never reads CSM content
 * itself (`define-finding-model/design.md` Decision 8), so no
 * CSM-element-graph builder is needed here — only enough {@link
 * CsmElementId}/{@link CsmSnapshotId}/{@link CsmScopeInstance} values
 * to construct representative {@code RuleEvaluationResult}s directly,
 * per {@code implement-finding-model/design.md} Decision 10.
 */
public final class RuleEvaluationResultFixtures {

  public static final CsmSnapshotId SNAPSHOT = new CsmSnapshotId("repo", 1);

  private RuleEvaluationResultFixtures() {}

  /** A qualifying (FAIL + FindingMetadata payload) Result, anchored at {@code elementId}. */
  public static RuleEvaluationResult anchoredFailing(String ruleIdentifier, CsmElementId elementId) {
    return RuleEvaluationResult.of(
        ruleIdentifier,
        1,
        SNAPSHOT,
        CsmScopeInstance.anchoredAt(elementId),
        Set.of(),
        RuleEvaluationOutcome.FAIL,
        StubFindingMetadata.of("category", "high"));
  }

  /** A qualifying (FAIL + FindingMetadata payload) Result, unanchored (whole repository). */
  public static RuleEvaluationResult unanchoredFailing(String ruleIdentifier) {
    return RuleEvaluationResult.of(
        ruleIdentifier,
        1,
        SNAPSHOT,
        CsmScopeInstance.wholeRepository(),
        Set.of(),
        RuleEvaluationOutcome.FAIL,
        StubFindingMetadata.of("category", "high"));
  }

  /** A non-qualifying PASS Result. */
  public static RuleEvaluationResult anchoredPassing(String ruleIdentifier, CsmElementId elementId) {
    return RuleEvaluationResult.of(
        ruleIdentifier, 1, SNAPSHOT, CsmScopeInstance.anchoredAt(elementId), Set.of(), RuleEvaluationOutcome.PASS, "ok");
  }

  /** A non-qualifying NOT_APPLICABLE Result. */
  public static RuleEvaluationResult anchoredNotApplicable(String ruleIdentifier, CsmElementId elementId) {
    return RuleEvaluationResult.of(
        ruleIdentifier,
        1,
        SNAPSHOT,
        CsmScopeInstance.anchoredAt(elementId),
        Set.of(),
        RuleEvaluationOutcome.NOT_APPLICABLE,
        "no data");
  }

  /** A FAIL Result whose payload does NOT implement FindingMetadata - does not qualify. */
  public static RuleEvaluationResult anchoredFailingWithoutMetadata(String ruleIdentifier, CsmElementId elementId) {
    return RuleEvaluationResult.of(
        ruleIdentifier,
        1,
        SNAPSHOT,
        CsmScopeInstance.anchoredAt(elementId),
        Set.of(),
        RuleEvaluationOutcome.FAIL,
        "opaque, non-FindingMetadata payload");
  }
}
