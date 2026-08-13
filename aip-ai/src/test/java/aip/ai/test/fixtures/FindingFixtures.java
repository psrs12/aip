package aip.ai.test.fixtures;

import aip.core.csm.Confidence;
import aip.core.csm.CsmElementId;
import aip.core.csm.CsmSnapshotId;
import aip.core.csm.Finding;
import aip.core.csm.RuleEvaluationResultId;
import aip.core.csm.CsmScopeInstance;
import java.util.Set;

/**
 * Fixture {@code Finding} construction, used throughout this module's
 * own tests. Agent Framework never reads CSM content itself
 * (`define-agent-framework/design.md` Decision 4), so no
 * CSM-element-graph builder is needed here — only enough {@code
 * Finding.of(...)} arguments to construct representative Findings
 * directly, mirroring `implement-finding-model/design.md` Decision
 * 10's identical reasoning one layer down.
 */
public final class FindingFixtures {

  public static final CsmSnapshotId SNAPSHOT = new CsmSnapshotId("repo", 1);

  private FindingFixtures() {}

  public static Finding of(String ruleIdentifier, String concernedElementValue) {
    CsmElementId concernedElement = new CsmElementId(concernedElementValue);
    RuleEvaluationResultId rer =
        RuleEvaluationResultId.of(ruleIdentifier, 1, SNAPSHOT, CsmScopeInstance.anchoredAt(concernedElement), Set.of());
    return Finding.of(
        ruleIdentifier,
        SNAPSHOT,
        concernedElement,
        Set.of(rer),
        "category",
        "high",
        Confidence.HIGH,
        "description",
        "impact");
  }
}
