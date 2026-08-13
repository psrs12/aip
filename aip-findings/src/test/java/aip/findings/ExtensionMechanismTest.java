package aip.findings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.core.csm.CsmScopeInstance;
import aip.core.csm.Finding;
import aip.core.csm.RuleEvaluationOutcome;
import aip.core.csm.RuleEvaluationResult;
import aip.core.csm.ValidationResult;
import aip.findings.test.fixtures.InMemoryRuleEvaluationResultSource;
import aip.findings.test.fixtures.RuleEvaluationResultFixtures;
import aip.findings.test.fixtures.StubFindingMetadata;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Confirms a second, structurally distinct example Rule Type's
 * output — a different declared Category/Severity, a different
 * (unanchored) Rule Scope shape — uses Finding construction, identity,
 * traceability, and validation mechanisms unmodified, per {@code
 * Extension Behavior — New Rule Types Require No Finding Model
 * Changes}.
 */
class ExtensionMechanismTest {

  @Test
  void aStructurallyDistinctSecondRuleTypesOutputUsesTheSameCoreMechanismsUnmodified() {
    // First: an anchored Rule Type example, "architecture-boundary"
    // Category, "high" Severity.
    RuleEvaluationResult first =
        RuleEvaluationResultFixtures.anchoredFailing("ruletype.first", new aip.core.csm.CsmElementId("csm:element:m1"));

    // Second: a structurally distinct example - unanchored Rule
    // Scope, a different declared Category/Severity entirely.
    RuleEvaluationResult second =
        RuleEvaluationResult.of(
            "ruletype.second",
            1,
            RuleEvaluationResultFixtures.SNAPSHOT,
            CsmScopeInstance.wholeRepository(),
            Set.of(),
            RuleEvaluationOutcome.FAIL,
            StubFindingMetadata.of("license-compliance", "critical"));

    Finding firstFinding = FindingConstructor.construct(first).orElseThrow();
    Finding secondFinding = FindingConstructor.construct(second).orElseThrow();

    assertEquals("category", firstFinding.category());
    assertEquals("license-compliance", secondFinding.category());
    assertEquals("critical", secondFinding.severity());

    InMemoryRuleEvaluationResultSource source =
        new InMemoryRuleEvaluationResultSource().with(first).with(second);

    for (Finding finding : Set.of(firstFinding, secondFinding)) {
      ValidationResult validation = FindingValidator.validate(finding, source);
      assertTrue(validation.valid(), () -> "expected valid for " + finding.id() + ": " + validation.violations());
    }
  }
}
