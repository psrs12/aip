package aip.core.csm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * {@link FindingSource} contract tests, per {@code Finding Source
 * Shape}.
 */
class FindingSourceTest {

  private static final CsmSnapshotId SNAPSHOT = new CsmSnapshotId("repo", 1);
  private static final CsmElementId CONCERNED_ELEMENT = new CsmElementId("csm:element:m1");
  private static final RuleEvaluationResultId RER =
      RuleEvaluationResultId.of("rule.a", 1, SNAPSHOT, CsmScopeInstance.wholeRepository(), Set.of());

  @Test
  void exposesRetrievalByEvaluationIdentityAndByLogicalFindingIdentityAndSnapshot() {
    Finding finding =
        Finding.of("rule.a", SNAPSHOT, CONCERNED_ELEMENT, Set.of(RER), "cat", "sev", Confidence.HIGH, "d", "i");
    FindingSource source = fixtureSource(Set.of(finding));

    assertEquals(finding, source.read(finding.id()).orElseThrow());
    assertEquals(Set.of(finding), source.listByLogicalFindingIdentity(finding.logicalFindingIdentity()));
    assertEquals(Set.of(finding), source.listBySourceSnapshotId(SNAPSHOT));
    assertTrue(source.listBySourceSnapshotId(new CsmSnapshotId("other-repo", 1)).isEmpty());
  }

  @Test
  void agnosticToFindingProduction() {
    Finding finding =
        Finding.of("rule.a", SNAPSHOT, CONCERNED_ELEMENT, Set.of(RER), "cat", "sev", Confidence.HIGH, "d", "i");
    FindingSource first = fixtureSource(Set.of(finding));
    FindingSource second = fixtureSource(Set.of(finding));

    assertEquals(first.read(finding.id()), second.read(finding.id()));
    assertEquals(
        first.listByLogicalFindingIdentity(finding.logicalFindingIdentity()),
        second.listByLogicalFindingIdentity(finding.logicalFindingIdentity()));
  }

  private static FindingSource fixtureSource(Set<Finding> findings) {
    Map<EvaluationIdentity, Finding> byId = new LinkedHashMap<>();
    for (Finding finding : findings) {
      byId.put(finding.id(), finding);
    }
    return new FindingSource() {
      @Override
      public Optional<Finding> read(EvaluationIdentity id) {
        return Optional.ofNullable(byId.get(id));
      }

      @Override
      public Set<Finding> listByLogicalFindingIdentity(LogicalFindingIdentity id) {
        return byId.values().stream()
            .filter(f -> f.logicalFindingIdentity().equals(id))
            .collect(Collectors.toUnmodifiableSet());
      }

      @Override
      public Set<Finding> listBySourceSnapshotId(CsmSnapshotId snapshotId) {
        return byId.values().stream()
            .filter(f -> f.sourceSnapshotId().equals(snapshotId))
            .collect(Collectors.toUnmodifiableSet());
      }
    };
  }
}
