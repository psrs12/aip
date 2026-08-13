package aip.core.csm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * {@link Finding} tests, per {@code Finding Shape}, {@code Finding
 * Traceability}, and {@code Finding-to-RuleEvaluationResult Reference
 * Set and V1 Multiplicity}.
 */
class FindingTest {

  private static final CsmSnapshotId SNAPSHOT = new CsmSnapshotId("repo", 1);
  private static final CsmScopeInstance SCOPE_INSTANCE = CsmScopeInstance.wholeRepository();
  private static final CsmElementId CONCERNED_ELEMENT = new CsmElementId("csm:element:m1");
  private static final RuleEvaluationResultId RER =
      RuleEvaluationResultId.of("rule.a", 1, SNAPSHOT, SCOPE_INSTANCE, Set.of());

  @Test
  void ofComputesBothIdentitiesFromTheSameTraceabilityComponents() {
    Finding finding =
        Finding.of(
            "rule.a", SNAPSHOT, CONCERNED_ELEMENT, Set.of(RER), "coupling", "high", Confidence.HIGH, "desc", "impact");
    assertEquals(EvaluationIdentity.of(Set.of(RER)), finding.id());
    assertEquals(LogicalFindingIdentity.of("rule.a", CONCERNED_ELEMENT), finding.logicalFindingIdentity());
  }

  @Test
  void retainsTraceabilityToProducingRuleSourceSnapshotAndConcernedElement() {
    Finding finding =
        Finding.of(
            "rule.a", SNAPSHOT, CONCERNED_ELEMENT, Set.of(RER), "coupling", "high", Confidence.HIGH, "desc", "impact");
    assertEquals("rule.a", finding.ruleIdentifier());
    assertEquals(SNAPSHOT, finding.sourceSnapshotId());
    assertEquals(CONCERNED_ELEMENT, finding.concernedElementId());
    assertEquals(Set.of(RER), finding.referencedRuleEvaluationResultIds());
  }

  @Test
  void categorySeverityDescriptionAndImpactAreRetainedUnmodified() {
    Finding finding =
        Finding.of(
            "rule.a",
            SNAPSHOT,
            CONCERNED_ELEMENT,
            Set.of(RER),
            "security",
            "critical",
            Confidence.HIGH,
            "a forbidden dependency exists",
            "may expose internal data");
    assertEquals("security", finding.category());
    assertEquals("critical", finding.severity());
    assertEquals("a forbidden dependency exists", finding.description());
    assertEquals("may expose internal data", finding.impact());
  }

  @Test
  void confidenceIsRetainedAsProvided() {
    Finding finding =
        Finding.of(
            "rule.a", SNAPSHOT, CONCERNED_ELEMENT, Set.of(RER), "cat", "sev", Confidence.HIGH, "d", "i");
    assertEquals(Confidence.HIGH, finding.confidence());
  }

  @Test
  void rejectsEmptyReferencedRuleEvaluationResultSet() {
    assertThrows(
        IllegalArgumentException.class,
        () -> Finding.of("rule.a", SNAPSHOT, CONCERNED_ELEMENT, Set.of(), "cat", "sev", Confidence.HIGH, "d", "i"));
  }

  @Test
  void rejectsBlankRuleIdentifier() {
    assertThrows(
        IllegalArgumentException.class,
        () -> Finding.of(" ", SNAPSHOT, CONCERNED_ELEMENT, Set.of(RER), "cat", "sev", Confidence.HIGH, "d", "i"));
  }

  @Test
  void differentlyShapedCategoryValuesFromDifferentRuleTypesAreBothAccepted() {
    Finding architectural =
        Finding.of(
            "rule.a", SNAPSHOT, CONCERNED_ELEMENT, Set.of(RER), "architecture-boundary", "high", Confidence.HIGH, "d", "i");
    Finding security =
        Finding.of(
            "rule.b", SNAPSHOT, CONCERNED_ELEMENT, Set.of(RER), "security-secret-exposure", "critical", Confidence.HIGH, "d", "i");
    assertEquals("architecture-boundary", architectural.category());
    assertEquals("security-secret-exposure", security.category());
  }
}
