package aip.rules;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.core.csm.AnalysisResultId;
import aip.core.csm.AnalysisView;
import aip.core.csm.CsmElementId;
import aip.core.csm.CsmEntityKind;
import aip.core.csm.CsmScope;
import aip.core.csm.CsmScopeInstance;
import aip.core.csm.CsmSnapshotId;
import aip.core.csm.RuleEvaluationOutcome;
import aip.core.csm.RuleEvaluationResult;
import aip.core.csm.RuleEvaluationResultId;
import aip.core.csm.ValidationResult;
import aip.rules.test.fixtures.CsmSnapshotSourceBuilder;
import aip.rules.test.fixtures.InMemoryAnalysisResultSource;
import aip.rules.test.fixtures.StubRuleType;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * {@link RuleEvaluationResultValidator} tests, per {@code Rule
 * Evaluation Result Validation Before Publication}.
 */
class RuleEvaluationResultValidatorTest {

  private static final CsmScope MODULE_SCOPE = CsmScope.ofEntityKinds(Set.of(CsmEntityKind.MODULE));

  @Test
  void validResultPassesValidation() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    builder.module("moduleA");
    AnalysisView view = AnalysisView.from(builder.build());

    RuleTypeRegistry ruleTypeRegistry = new RuleTypeRegistry();
    ruleTypeRegistry.register(new StubRuleType("ruletype.a", 1, MODULE_SCOPE));
    RuleRegistry ruleRegistry = new RuleRegistry(ruleTypeRegistry);
    ruleRegistry.register(new Rule("rule.a", 1, "ruletype.a", Map.of()));

    RuleEvaluationResult result =
        RuleEvaluationResult.of(
            "rule.a", 1, view.sourceSnapshotId(), CsmScopeInstance.wholeRepository(), Set.of(), RuleEvaluationOutcome.PASS, "ok");

    ValidationResult validation =
        RuleEvaluationResultValidator.validate(result, view, new InMemoryAnalysisResultSource(), ruleTypeRegistry, ruleRegistry);
    assertTrue(validation.valid());
  }

  @Test
  void resultReferencingAnUnavailableAnalysisResultIsRejected() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    builder.module("moduleA");
    AnalysisView view = AnalysisView.from(builder.build());

    RuleTypeRegistry ruleTypeRegistry = new RuleTypeRegistry();
    ruleTypeRegistry.register(new StubRuleType("ruletype.a", 1, MODULE_SCOPE));
    RuleRegistry ruleRegistry = new RuleRegistry(ruleTypeRegistry);
    ruleRegistry.register(new Rule("rule.a", 1, "ruletype.a", Map.of()));

    AnalysisResultId missingAnalysisResultId =
        AnalysisResultId.of("analyzer.a", 1, view.sourceSnapshotId(), CsmScopeInstance.wholeRepository());
    RuleEvaluationResult result =
        RuleEvaluationResult.of(
            "rule.a",
            1,
            view.sourceSnapshotId(),
            CsmScopeInstance.wholeRepository(),
            Set.of(missingAnalysisResultId),
            RuleEvaluationOutcome.PASS,
            "ok");

    ValidationResult validation =
        RuleEvaluationResultValidator.validate(result, view, new InMemoryAnalysisResultSource(), ruleTypeRegistry, ruleRegistry);
    assertFalse(validation.valid());
  }

  @Test
  void resultReferencingAnUnavailableCsmIdentityIsRejected() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    builder.module("moduleA");
    AnalysisView view = AnalysisView.from(builder.build());

    RuleTypeRegistry ruleTypeRegistry = new RuleTypeRegistry();
    ruleTypeRegistry.register(new StubRuleType("ruletype.a", 1, MODULE_SCOPE.anchoredAt(CsmEntityKind.MODULE)));
    RuleRegistry ruleRegistry = new RuleRegistry(ruleTypeRegistry);
    ruleRegistry.register(new Rule("rule.a", 1, "ruletype.a", Map.of()));

    CsmScopeInstance nonExistentAnchor = CsmScopeInstance.anchoredAt(new CsmElementId("repo:element:absent"));
    RuleEvaluationResult result =
        RuleEvaluationResult.of(
            "rule.a", 1, view.sourceSnapshotId(), nonExistentAnchor, Set.of(), RuleEvaluationOutcome.PASS, "ok");

    ValidationResult validation =
        RuleEvaluationResultValidator.validate(result, view, new InMemoryAnalysisResultSource(), ruleTypeRegistry, ruleRegistry);
    assertFalse(validation.valid());
  }

  @Test
  void resultFromAnUnregisteredRuleVersionIsRejected() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    builder.module("moduleA");
    AnalysisView view = AnalysisView.from(builder.build());

    RuleTypeRegistry ruleTypeRegistry = new RuleTypeRegistry();
    ruleTypeRegistry.register(new StubRuleType("ruletype.a", 1, MODULE_SCOPE));
    RuleRegistry ruleRegistry = new RuleRegistry(ruleTypeRegistry);
    ruleRegistry.register(new Rule("rule.a", 2, "ruletype.a", Map.of()));

    RuleEvaluationResult result =
        RuleEvaluationResult.of(
            "rule.a", 1, view.sourceSnapshotId(), CsmScopeInstance.wholeRepository(), Set.of(), RuleEvaluationOutcome.PASS, "ok");

    ValidationResult validation =
        RuleEvaluationResultValidator.validate(result, view, new InMemoryAnalysisResultSource(), ruleTypeRegistry, ruleRegistry);
    assertFalse(validation.valid());
  }

  @Test
  void resultFromAnUnregisteredRuleIsRejected() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    builder.module("moduleA");
    AnalysisView view = AnalysisView.from(builder.build());

    RuleTypeRegistry ruleTypeRegistry = new RuleTypeRegistry();
    RuleRegistry ruleRegistry = new RuleRegistry(ruleTypeRegistry);

    RuleEvaluationResult result =
        RuleEvaluationResult.of(
            "rule.unknown",
            1,
            view.sourceSnapshotId(),
            CsmScopeInstance.wholeRepository(),
            Set.of(),
            RuleEvaluationOutcome.PASS,
            "ok");

    ValidationResult validation =
        RuleEvaluationResultValidator.validate(result, view, new InMemoryAnalysisResultSource(), ruleTypeRegistry, ruleRegistry);
    assertFalse(validation.valid());
  }

  @Test
  void resultExceedingItsRuleTypesDeclaredScopeIsRejected() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    CsmElementId m1 = builder.module("moduleA");
    AnalysisView view = AnalysisView.from(builder.build());

    RuleTypeRegistry ruleTypeRegistry = new RuleTypeRegistry();
    // Declared unanchored, but the constructed Result claims an anchored scope instance.
    ruleTypeRegistry.register(new StubRuleType("ruletype.a", 1, MODULE_SCOPE));
    RuleRegistry ruleRegistry = new RuleRegistry(ruleTypeRegistry);
    ruleRegistry.register(new Rule("rule.a", 1, "ruletype.a", Map.of()));

    RuleEvaluationResult result =
        RuleEvaluationResult.of(
            "rule.a", 1, view.sourceSnapshotId(), CsmScopeInstance.anchoredAt(m1), Set.of(), RuleEvaluationOutcome.PASS, "ok");

    ValidationResult validation =
        RuleEvaluationResultValidator.validate(result, view, new InMemoryAnalysisResultSource(), ruleTypeRegistry, ruleRegistry);
    assertFalse(validation.valid());
  }

  @Test
  void resultDeclaringAMismatchedSourceSnapshotIsRejected() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    builder.module("moduleA");
    AnalysisView view = AnalysisView.from(builder.build());

    RuleTypeRegistry ruleTypeRegistry = new RuleTypeRegistry();
    ruleTypeRegistry.register(new StubRuleType("ruletype.a", 1, MODULE_SCOPE));
    RuleRegistry ruleRegistry = new RuleRegistry(ruleTypeRegistry);
    ruleRegistry.register(new Rule("rule.a", 1, "ruletype.a", Map.of()));

    CsmSnapshotId otherSnapshot = new CsmSnapshotId("repo", 99);
    RuleEvaluationResultId id =
        RuleEvaluationResultId.of("rule.a", 1, otherSnapshot, CsmScopeInstance.wholeRepository(), Set.of());
    RuleEvaluationResult result =
        new RuleEvaluationResult(
            id,
            "rule.a",
            1,
            otherSnapshot,
            CsmScopeInstance.wholeRepository(),
            Set.of(),
            RuleEvaluationOutcome.PASS,
            "ok");

    ValidationResult validation =
        RuleEvaluationResultValidator.validate(result, view, new InMemoryAnalysisResultSource(), ruleTypeRegistry, ruleRegistry);
    assertFalse(validation.valid());
  }
}
