package aip.findings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.core.csm.CsmElement;
import aip.core.csm.CsmElementId;
import aip.core.csm.CsmRelationship;
import aip.core.csm.CsmSnapshotId;
import aip.core.csm.Finding;
import aip.core.csm.RuleEvaluationResult;
import aip.findings.test.fixtures.InMemoryFindingStore;
import aip.findings.test.fixtures.InMemoryRuleEvaluationResultSource;
import aip.findings.test.fixtures.RuleEvaluationResultFixtures;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Dependency and architectural invariant tests, per {@code CSM and
 * Rule Evaluation Content Reached Only Through Established
 * Contracts}, {@code Findings Are a Distinct Concept From CSM
 * Knowledge, Analysis Results, and Rule Evaluation Results}, {@code
 * Aggregation Is Not Performed in This Version}, {@code
 * Single-Repository Finding Scope}, and {@code Finding Model Has No
 * Scope Concept}.
 */
class DependencyAndBoundaryTest {

  private static final CsmElementId ELEMENT_A = new CsmElementId("csm:element:m1");
  private static final CsmElementId ELEMENT_B = new CsmElementId("csm:element:m2");

  @Test
  void findingModelReadsOnlyFromARuleEvaluationResultSource() {
    // FindingConstructor.construct's own signature has no
    // AnalysisView, CsmSnapshotSource, or any CSM-content-bearing
    // parameter at all - its only input is a single
    // RuleEvaluationResult, already resolved through a
    // RuleEvaluationResultSource by the caller.
    RuleEvaluationResult result = RuleEvaluationResultFixtures.anchoredFailing("rule.a", ELEMENT_A);
    assertTrue(FindingConstructor.construct(result).isPresent());
  }

  @Test
  void producingAFindingDoesNotAlterUpstreamContent() {
    RuleEvaluationResult result = RuleEvaluationResultFixtures.anchoredFailing("rule.a", ELEMENT_A);
    InMemoryRuleEvaluationResultSource source = new InMemoryRuleEvaluationResultSource().with(result);
    RuleEvaluationResult before = source.read(result.id()).orElseThrow();

    Finding finding = FindingConstructor.construct(result).orElseThrow();
    FindingValidator.validate(finding, source);

    assertEquals(before, source.read(result.id()).orElseThrow());
  }

  @Test
  void findingIsNeverExposedAsCsmContentOrAnalysisOrRuleEvaluationResult() {
    // Finding, CsmElement/CsmRelationship, and RuleEvaluationResult
    // are disjoint types - no shared query surface exists through
    // which one could be returned in place of another.
    assertTrue(!CsmElement.class.isAssignableFrom(Finding.class));
    assertTrue(!CsmRelationship.class.isAssignableFrom(Finding.class));
    assertTrue(!RuleEvaluationResult.class.isAssignableFrom(Finding.class));
  }

  @Test
  void findingsAreStoredSeparatelyFromRuleEvaluationResults() {
    RuleEvaluationResult result = RuleEvaluationResultFixtures.anchoredFailing("rule.a", ELEMENT_A);
    InMemoryRuleEvaluationResultSource source = new InMemoryRuleEvaluationResultSource().with(result);
    Finding finding = FindingConstructor.construct(result).orElseThrow();
    InMemoryFindingStore findingStore = new InMemoryFindingStore();
    findingStore.write(finding);

    // Querying the RuleEvaluationResultSource never returns Findings.
    assertEquals(Set.of(result), source.list("rule.a", RuleEvaluationResultFixtures.SNAPSHOT));
  }

  @Test
  void noAggregationOccursAcrossMultipleQualifyingResults() {
    RuleEvaluationResult first = RuleEvaluationResultFixtures.anchoredFailing("rule.a", ELEMENT_A);
    RuleEvaluationResult second = RuleEvaluationResultFixtures.anchoredFailing("rule.a", ELEMENT_B);

    Finding firstFinding = FindingConstructor.construct(first).orElseThrow();
    Finding secondFinding = FindingConstructor.construct(second).orElseThrow();

    // Two Results -> two Findings, each referencing exactly its own
    // source Result - never combined into one.
    assertEquals(Set.of(first.id()), firstFinding.referencedRuleEvaluationResultIds());
    assertEquals(Set.of(second.id()), secondFinding.referencedRuleEvaluationResultIds());
  }

  @Test
  void everyFindingFromOneConstructionRunReferencesExactlyOneRepositorysSnapshotIdentity() {
    List<RuleEvaluationResult> results =
        List.of(
            RuleEvaluationResultFixtures.anchoredFailing("rule.a", ELEMENT_A),
            RuleEvaluationResultFixtures.anchoredFailing("rule.b", ELEMENT_B));
    for (RuleEvaluationResult result : results) {
      Finding finding = FindingConstructor.construct(result).orElseThrow();
      assertEquals(RuleEvaluationResultFixtures.SNAPSHOT, finding.sourceSnapshotId());
    }
  }

  @Test
  void findingConstructionRequiresNoScopeDeclarationOfAnyKind() {
    // FindingConstructor exposes no Scope-declaring type, no
    // registration mechanism, and no configuration input at all -
    // qualifies/construct each take only a single RuleEvaluationResult.
    RuleEvaluationResult result = RuleEvaluationResultFixtures.anchoredFailing("rule.a", ELEMENT_A);
    Finding finding = FindingConstructor.construct(result).orElseThrow();
    // Targeting is determined solely from the Result's own Rule Scope
    // instance content (its anchor), never from any independently
    // evaluated applicability condition.
    assertEquals(ELEMENT_A, finding.concernedElementId());
  }
}
