package aip.findings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.core.csm.CsmElementId;
import aip.core.csm.CsmSnapshotId;
import aip.core.csm.EvaluationIdentity;
import aip.core.csm.Finding;
import aip.core.csm.LogicalFindingIdentity;
import aip.core.csm.RuleEvaluationResult;
import aip.findings.test.fixtures.RuleEvaluationResultFixtures;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * {@link FindingConstructor} tests, per {@code Deterministic,
 * Declarative Finding Construction Only}, {@code Rule Evaluation
 * Outcome} qualification (`implement-finding-model/design.md`
 * Decision 4), and {@code Finding-to-RuleEvaluationResult Reference
 * Set and V1 Multiplicity}.
 */
class FindingConstructorTest {

  private static final CsmElementId ELEMENT = new CsmElementId("csm:element:m1");

  @Test
  void failingResultWithFindingMetadataQualifies() {
    RuleEvaluationResult result = RuleEvaluationResultFixtures.anchoredFailing("rule.a", ELEMENT);
    assertTrue(FindingConstructor.qualifies(result));
    assertTrue(FindingConstructor.construct(result).isPresent());
  }

  @Test
  void passingResultNeverQualifies() {
    RuleEvaluationResult result = RuleEvaluationResultFixtures.anchoredPassing("rule.a", ELEMENT);
    assertFalse(FindingConstructor.qualifies(result));
    assertEquals(Optional.empty(), FindingConstructor.construct(result));
  }

  @Test
  void notApplicableResultNeverQualifies() {
    RuleEvaluationResult result = RuleEvaluationResultFixtures.anchoredNotApplicable("rule.a", ELEMENT);
    assertFalse(FindingConstructor.qualifies(result));
    assertEquals(Optional.empty(), FindingConstructor.construct(result));
  }

  @Test
  void failingResultWithoutFindingMetadataPayloadDoesNotQualify() {
    RuleEvaluationResult result = RuleEvaluationResultFixtures.anchoredFailingWithoutMetadata("rule.a", ELEMENT);
    assertFalse(FindingConstructor.qualifies(result));
    assertEquals(Optional.empty(), FindingConstructor.construct(result));
  }

  @Test
  void constructedFindingReferencesExactlyTheSourceResult() {
    RuleEvaluationResult result = RuleEvaluationResultFixtures.anchoredFailing("rule.a", ELEMENT);
    Finding finding = FindingConstructor.construct(result).orElseThrow();
    assertEquals(Set.of(result.id()), finding.referencedRuleEvaluationResultIds());
    assertEquals(EvaluationIdentity.of(Set.of(result.id())), finding.id());
  }

  @Test
  void constructedFindingCopiesCategorySeverityDescriptionAndImpactUnmodified() {
    RuleEvaluationResult result = RuleEvaluationResultFixtures.anchoredFailing("rule.a", ELEMENT);
    Finding finding = FindingConstructor.construct(result).orElseThrow();
    assertEquals("category", finding.category());
    assertEquals("high", finding.severity());
    assertEquals("stub description", finding.description());
    assertEquals("stub impact", finding.impact());
  }

  @Test
  void anchoredResultResolvesConcernedElementToItsAnchor() {
    RuleEvaluationResult result = RuleEvaluationResultFixtures.anchoredFailing("rule.a", ELEMENT);
    Finding finding = FindingConstructor.construct(result).orElseThrow();
    assertEquals(ELEMENT, finding.concernedElementId());
    assertEquals(LogicalFindingIdentity.of("rule.a", ELEMENT), finding.logicalFindingIdentity());
  }

  @Test
  void unanchoredResultResolvesConcernedElementToTheSynthesizedRepositorySubject() {
    RuleEvaluationResult result = RuleEvaluationResultFixtures.unanchoredFailing("rule.a");
    Finding finding = FindingConstructor.construct(result).orElseThrow();
    // Same repository -> same synthesized subject, deterministically,
    // across two separately-constructed unanchored Results.
    RuleEvaluationResult second = RuleEvaluationResultFixtures.unanchoredFailing("rule.b");
    Finding secondFinding = FindingConstructor.construct(second).orElseThrow();
    assertEquals(finding.concernedElementId(), secondFinding.concernedElementId());
  }

  @Test
  void repeatedConstructionOverUnchangedInputIsIdentical() {
    RuleEvaluationResult result = RuleEvaluationResultFixtures.anchoredFailing("rule.a", ELEMENT);
    Finding first = FindingConstructor.construct(result).orElseThrow();
    Finding second = FindingConstructor.construct(result).orElseThrow();
    assertEquals(first.id(), second.id());
    assertEquals(first.logicalFindingIdentity(), second.logicalFindingIdentity());
  }

  @Test
  void confidenceIsAlwaysFixedToHigh() {
    RuleEvaluationResult result = RuleEvaluationResultFixtures.anchoredFailing("rule.a", ELEMENT);
    Finding finding = FindingConstructor.construct(result).orElseThrow();
    assertEquals(aip.core.csm.Confidence.HIGH, finding.confidence());
  }

  @Test
  void differentConcernedElementsYieldDifferentLogicalFindingIdentity() {
    RuleEvaluationResult forA = RuleEvaluationResultFixtures.anchoredFailing("rule.a", ELEMENT);
    RuleEvaluationResult forB =
        RuleEvaluationResultFixtures.anchoredFailing("rule.a", new CsmElementId("csm:element:m2"));
    Finding findingA = FindingConstructor.construct(forA).orElseThrow();
    Finding findingB = FindingConstructor.construct(forB).orElseThrow();
    assertFalse(findingA.logicalFindingIdentity().equals(findingB.logicalFindingIdentity()));
  }

  @Test
  void changedOutcomeAcrossRunsSharesLogicalFindingIdentityButDiffersInEvaluationIdentity() {
    RuleEvaluationResult firstRunFailure = RuleEvaluationResultFixtures.anchoredFailing("rule.a", ELEMENT);
    // A different snapshot -> a different RuleEvaluationResultId (a
    // different evaluation run), same Rule and same concerned element.
    RuleEvaluationResult secondRunFailure =
        RuleEvaluationResult.of(
            "rule.a",
            1,
            new CsmSnapshotId("repo", 2),
            aip.core.csm.CsmScopeInstance.anchoredAt(ELEMENT),
            Set.of(),
            aip.core.csm.RuleEvaluationOutcome.FAIL,
            aip.findings.test.fixtures.StubFindingMetadata.of("category", "high"));

    Finding first = FindingConstructor.construct(firstRunFailure).orElseThrow();
    Finding second = FindingConstructor.construct(secondRunFailure).orElseThrow();

    assertEquals(first.logicalFindingIdentity(), second.logicalFindingIdentity());
    assertFalse(first.id().equals(second.id()));
  }
}
