package aip.core.csm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

/**
 * {@link LogicalFindingIdentity} tests, per {@code Logical Finding
 * Identity}.
 */
class LogicalFindingIdentityTest {

  private static final CsmElementId ELEMENT_A = new CsmElementId("csm:element:a");
  private static final CsmElementId ELEMENT_B = new CsmElementId("csm:element:b");

  @Test
  void sameRuleAndElementAcrossTwoSnapshotsYieldsTheSameIdentity() {
    // Logical Finding Identity does not accept a snapshot argument at
    // all - this test documents that fact by construction: the same
    // two inputs always produce the same identity, independent of
    // whatever CSM Snapshot the caller evaluated against.
    LogicalFindingIdentity fromRunOne = LogicalFindingIdentity.of("rule.a", ELEMENT_A);
    LogicalFindingIdentity fromRunTwo = LogicalFindingIdentity.of("rule.a", ELEMENT_A);
    assertEquals(fromRunOne, fromRunTwo);
  }

  @Test
  void ruleVersionIsNotAnInputSoDifferingVersionsYieldTheSameIdentity() {
    // No version parameter exists on LogicalFindingIdentity.of at
    // all - a Rule Type version bump cannot affect this identity by
    // construction.
    LogicalFindingIdentity identity = LogicalFindingIdentity.of("rule.a", ELEMENT_A);
    assertEquals(identity, LogicalFindingIdentity.of("rule.a", ELEMENT_A));
  }

  @Test
  void differentConcernedElementsYieldDistinguishableIdentity() {
    LogicalFindingIdentity forA = LogicalFindingIdentity.of("rule.a", ELEMENT_A);
    LogicalFindingIdentity forB = LogicalFindingIdentity.of("rule.a", ELEMENT_B);
    assertNotEquals(forA, forB);
  }

  @Test
  void differentRuleIdentifiersYieldDistinguishableIdentity() {
    LogicalFindingIdentity ruleA = LogicalFindingIdentity.of("rule.a", ELEMENT_A);
    LogicalFindingIdentity ruleB = LogicalFindingIdentity.of("rule.b", ELEMENT_A);
    assertNotEquals(ruleA, ruleB);
  }
}
