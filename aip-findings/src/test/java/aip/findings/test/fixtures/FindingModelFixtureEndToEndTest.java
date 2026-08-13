package aip.findings.test.fixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.core.csm.CsmElementId;
import aip.core.csm.Finding;
import aip.core.csm.LogicalFindingIdentity;
import aip.core.csm.RuleEvaluationResult;
import aip.core.csm.ValidationResult;
import aip.findings.FindingConstructor;
import aip.findings.FindingPublisher;
import aip.findings.FindingValidator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * A representative end-to-end scenario: construct, validate, and
 * publish a Finding from a fixture qualifying {@code
 * RuleEvaluationResult}, then retrieve it by Evaluation Identity and
 * independently recompute its Logical Finding Identity; also confirm
 * every non-qualifying outcome (PASS, NOT_APPLICABLE, FAIL without
 * FindingMetadata) produces no Finding in the same run. Mirrors {@code
 * aip-rules}'s own {@code RuleFrameworkFixtureEndToEndTest}.
 */
class FindingModelFixtureEndToEndTest {

  @Test
  void constructsValidatesAndPublishesFindingsForOnlyTheQualifyingResults() {
    CsmElementId moduleFail = new CsmElementId("csm:element:moduleFail");
    CsmElementId modulePass = new CsmElementId("csm:element:modulePass");
    CsmElementId moduleNotApplicable = new CsmElementId("csm:element:moduleNotApplicable");
    CsmElementId moduleNoMetadata = new CsmElementId("csm:element:moduleNoMetadata");

    List<RuleEvaluationResult> results =
        List.of(
            RuleEvaluationResultFixtures.anchoredFailing("rule.boundary", moduleFail),
            RuleEvaluationResultFixtures.anchoredPassing("rule.boundary", modulePass),
            RuleEvaluationResultFixtures.anchoredNotApplicable("rule.boundary", moduleNotApplicable),
            RuleEvaluationResultFixtures.anchoredFailingWithoutMetadata("rule.boundary", moduleNoMetadata));

    InMemoryRuleEvaluationResultSource source = new InMemoryRuleEvaluationResultSource();
    for (RuleEvaluationResult result : results) {
      source.with(result);
    }

    InMemoryFindingStore store = new InMemoryFindingStore();
    int published = 0;
    for (RuleEvaluationResult result : results) {
      Optional<Finding> maybeFinding = FindingConstructor.construct(result);
      if (maybeFinding.isEmpty()) {
        continue;
      }
      Finding finding = maybeFinding.get();

      ValidationResult validation = FindingValidator.validate(finding, source);
      assertTrue(validation.valid(), () -> "expected valid for " + finding.id() + ": " + validation.violations());

      FindingPublisher.PublicationOutcome outcome = FindingPublisher.publish(store, finding, source);
      assertTrue(outcome.published());
      published++;

      Finding retrieved = store.read(finding.id()).orElseThrow();
      assertEquals(finding.id(), retrieved.id());
      assertEquals(LogicalFindingIdentity.of("rule.boundary", moduleFail), retrieved.logicalFindingIdentity());
    }

    // Only the single FAIL-with-FindingMetadata Result qualified;
    // PASS, NOT_APPLICABLE, and FAIL-without-FindingMetadata each
    // produced no Finding at all.
    assertEquals(1, published);
  }
}
