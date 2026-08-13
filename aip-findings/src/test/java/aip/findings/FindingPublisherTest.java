package aip.findings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.core.csm.CsmElementId;
import aip.core.csm.Finding;
import aip.core.csm.RuleEvaluationResult;
import aip.findings.test.fixtures.InMemoryFindingStore;
import aip.findings.test.fixtures.InMemoryRuleEvaluationResultSource;
import aip.findings.test.fixtures.RuleEvaluationResultFixtures;
import org.junit.jupiter.api.Test;

/**
 * {@link FindingPublisher} tests, per {@code Valid Finding published}/
 * {@code invalid Finding never published}.
 */
class FindingPublisherTest {

  private static final CsmElementId ELEMENT = new CsmElementId("csm:element:m1");

  @Test
  void validFindingIsPublishedAndRetrievable() {
    RuleEvaluationResult result = RuleEvaluationResultFixtures.anchoredFailing("rule.a", ELEMENT);
    Finding finding = FindingConstructor.construct(result).orElseThrow();
    InMemoryRuleEvaluationResultSource source = new InMemoryRuleEvaluationResultSource().with(result);
    InMemoryFindingStore store = new InMemoryFindingStore();

    FindingPublisher.PublicationOutcome outcome = FindingPublisher.publish(store, finding, source);

    assertTrue(outcome.published());
    assertEquals(finding.id(), store.read(finding.id()).orElseThrow().id());
  }

  @Test
  void invalidFindingIsNeverPublished() {
    RuleEvaluationResult result = RuleEvaluationResultFixtures.anchoredFailing("rule.a", ELEMENT);
    Finding finding = FindingConstructor.construct(result).orElseThrow();
    // Source deliberately omits the referenced Result -> invalid.
    InMemoryRuleEvaluationResultSource emptySource = new InMemoryRuleEvaluationResultSource();
    InMemoryFindingStore store = new InMemoryFindingStore();

    FindingPublisher.PublicationOutcome outcome = FindingPublisher.publish(store, finding, emptySource);

    assertFalse(outcome.published());
    assertTrue(store.read(finding.id()).isEmpty());
  }
}
