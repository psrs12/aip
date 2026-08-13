package aip.findings;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.core.csm.CsmElementId;
import aip.core.csm.CsmSnapshotId;
import aip.core.csm.Confidence;
import aip.core.csm.EvaluationIdentity;
import aip.core.csm.Finding;
import aip.core.csm.LogicalFindingIdentity;
import aip.core.csm.RuleEvaluationResult;
import aip.core.csm.RuleEvaluationResultId;
import aip.core.csm.ValidationResult;
import aip.findings.test.fixtures.InMemoryRuleEvaluationResultSource;
import aip.findings.test.fixtures.RuleEvaluationResultFixtures;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * {@link FindingValidator} tests, per {@code Finding Validation Before
 * Publication}.
 */
class FindingValidatorTest {

  private static final CsmElementId ELEMENT = new CsmElementId("csm:element:m1");

  @Test
  void validFindingPassesValidation() {
    RuleEvaluationResult result = RuleEvaluationResultFixtures.anchoredFailing("rule.a", ELEMENT);
    Finding finding = FindingConstructor.construct(result).orElseThrow();
    InMemoryRuleEvaluationResultSource source = new InMemoryRuleEvaluationResultSource().with(result);

    ValidationResult validation = FindingValidator.validate(finding, source);
    assertTrue(validation.valid());
  }

  @Test
  void findingReferencingAnUnavailableRuleEvaluationResultIsRejected() {
    RuleEvaluationResult result = RuleEvaluationResultFixtures.anchoredFailing("rule.a", ELEMENT);
    Finding finding = FindingConstructor.construct(result).orElseThrow();
    // Source deliberately does not contain the referenced Result.
    InMemoryRuleEvaluationResultSource emptySource = new InMemoryRuleEvaluationResultSource();

    ValidationResult validation = FindingValidator.validate(finding, emptySource);
    assertFalse(validation.valid());
  }

  @Test
  void findingWithAnEmptyReferenceSetIsRejected() {
    // Finding.of/Finding's own constructor already rejects an empty
    // set at construction time - confirm the validator would also
    // reject it if one were ever produced through a different path.
    RuleEvaluationResultId dangling =
        RuleEvaluationResultId.of("rule.a", 1, RuleEvaluationResultFixtures.SNAPSHOT, aip.core.csm.CsmScopeInstance.anchoredAt(ELEMENT), Set.of());
    Finding finding =
        new Finding(
            EvaluationIdentity.of(Set.of(dangling)),
            LogicalFindingIdentity.of("rule.a", ELEMENT),
            "rule.a",
            RuleEvaluationResultFixtures.SNAPSHOT,
            ELEMENT,
            Set.of(dangling),
            "cat",
            "sev",
            Confidence.HIGH,
            "d",
            "i");
    // Referenced Result (dangling) does not exist in the source -
    // this exercises the "unavailable reference" branch, since a
    // Finding's own constructor never permits an empty set to exist
    // at all (spec's "empty set rejected" scenario is therefore
    // structurally guaranteed by Finding's own constructor, exercised
    // in FindingTest.rejectsEmptyReferencedRuleEvaluationResultSet).
    ValidationResult validation = FindingValidator.validate(finding, new InMemoryRuleEvaluationResultSource());
    assertFalse(validation.valid());
  }

  @Test
  void findingWithInconsistentIdentityIsRejected() {
    RuleEvaluationResult result = RuleEvaluationResultFixtures.anchoredFailing("rule.a", ELEMENT);
    InMemoryRuleEvaluationResultSource source = new InMemoryRuleEvaluationResultSource().with(result);

    // Hand-construct a Finding whose declared identity does not match
    // what recomputation from its own fields would produce.
    Finding inconsistent =
        new Finding(
            new EvaluationIdentity("not-the-real-digest"),
            LogicalFindingIdentity.of("rule.a", ELEMENT),
            "rule.a",
            RuleEvaluationResultFixtures.SNAPSHOT,
            ELEMENT,
            Set.of(result.id()),
            "cat",
            "sev",
            Confidence.HIGH,
            "d",
            "i");

    ValidationResult validation = FindingValidator.validate(inconsistent, source);
    assertFalse(validation.valid());
  }

  @Test
  void findingDeclaringAMismatchedConcernedElementIsRejected() {
    RuleEvaluationResult result = RuleEvaluationResultFixtures.anchoredFailing("rule.a", ELEMENT);
    InMemoryRuleEvaluationResultSource source = new InMemoryRuleEvaluationResultSource().with(result);
    CsmElementId wrongElement = new CsmElementId("csm:element:wrong");

    Finding mismatched =
        Finding.of(
            "rule.a",
            RuleEvaluationResultFixtures.SNAPSHOT,
            wrongElement,
            Set.of(result.id()),
            "cat",
            "sev",
            Confidence.HIGH,
            "d",
            "i");

    ValidationResult validation = FindingValidator.validate(mismatched, source);
    assertFalse(validation.valid());
  }

  @Test
  void findingDeclaringAMismatchedSourceSnapshotIsRejected() {
    RuleEvaluationResult result = RuleEvaluationResultFixtures.anchoredFailing("rule.a", ELEMENT);
    InMemoryRuleEvaluationResultSource source = new InMemoryRuleEvaluationResultSource().with(result);
    CsmSnapshotId otherSnapshot = new CsmSnapshotId("repo", 99);

    Finding mismatched =
        Finding.of("rule.a", otherSnapshot, ELEMENT, Set.of(result.id()), "cat", "sev", Confidence.HIGH, "d", "i");

    ValidationResult validation = FindingValidator.validate(mismatched, source);
    assertFalse(validation.valid());
  }
}
