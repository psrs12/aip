package aip.analysis;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.analysis.test.fixtures.CsmSnapshotSourceBuilder;
import aip.analysis.test.fixtures.StubAnalyzer;
import aip.core.csm.AnalysisResult;
import aip.core.csm.AnalysisResultId;
import aip.core.csm.AnalysisView;
import aip.core.csm.CsmElementId;
import aip.core.csm.CsmEntityKind;
import aip.core.csm.CsmScope;
import aip.core.csm.CsmScopeInstance;
import aip.core.csm.CsmSnapshotId;
import aip.core.csm.ValidationResult;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * {@link AnalysisResultValidator} tests, per {@code Analysis Result
 * Validation Before Publication}.
 */
class AnalysisResultValidatorTest {

  private static final CsmScope MODULE_SCOPE = CsmScope.ofEntityKinds(Set.of(CsmEntityKind.MODULE));

  @Test
  void validResultPassesValidation() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    builder.module("moduleA");
    AnalysisView view = AnalysisView.from(builder.build());

    AnalyzerRegistry registry = new AnalyzerRegistry();
    registry.register(new StubAnalyzer("analyzer.a", 1, MODULE_SCOPE));

    AnalysisResult result =
        AnalysisResult.of("analyzer.a", 1, view.sourceSnapshotId(), CsmScopeInstance.wholeRepository(), "ok");

    ValidationResult validation = AnalysisResultValidator.validate(result, view, registry);
    assertTrue(validation.valid());
  }

  @Test
  void resultReferencingACsmIdentityAbsentFromItsSourceSnapshotIsRejected() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    builder.module("moduleA");
    AnalysisView view = AnalysisView.from(builder.build());

    AnalyzerRegistry registry = new AnalyzerRegistry();
    CsmScope anchoredScope = MODULE_SCOPE.anchoredAt(CsmEntityKind.MODULE);
    registry.register(new StubAnalyzer("analyzer.a", 1, anchoredScope));

    CsmScopeInstance nonExistentAnchor = CsmScopeInstance.anchoredAt(new CsmElementId("repo:element:absent"));
    AnalysisResult result = AnalysisResult.of("analyzer.a", 1, view.sourceSnapshotId(), nonExistentAnchor, "x");

    ValidationResult validation = AnalysisResultValidator.validate(result, view, registry);
    assertFalse(validation.valid());
  }

  @Test
  void resultFromAnUnregisteredAnalyzerVersionIsRejected() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    builder.module("moduleA");
    AnalysisView view = AnalysisView.from(builder.build());

    AnalyzerRegistry registry = new AnalyzerRegistry();
    registry.register(new StubAnalyzer("analyzer.a", 2, MODULE_SCOPE));

    AnalysisResult result =
        AnalysisResult.of("analyzer.a", 1, view.sourceSnapshotId(), CsmScopeInstance.wholeRepository(), "x");

    ValidationResult validation = AnalysisResultValidator.validate(result, view, registry);
    assertFalse(validation.valid());
  }

  @Test
  void unregisteredAnalyzerIsRejected() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    builder.module("moduleA");
    AnalysisView view = AnalysisView.from(builder.build());

    AnalyzerRegistry registry = new AnalyzerRegistry();
    AnalysisResult result =
        AnalysisResult.of("analyzer.unknown", 1, view.sourceSnapshotId(), CsmScopeInstance.wholeRepository(), "x");

    ValidationResult validation = AnalysisResultValidator.validate(result, view, registry);
    assertFalse(validation.valid());
  }

  @Test
  void resultExceedingItsAnalyzersDeclaredScopeIsRejected() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    CsmElementId m1 = builder.module("moduleA");
    AnalysisView view = AnalysisView.from(builder.build());

    AnalyzerRegistry registry = new AnalyzerRegistry();
    // Declared unanchored, but the constructed Result claims an anchored scope instance.
    registry.register(new StubAnalyzer("analyzer.a", 1, MODULE_SCOPE));

    AnalysisResult result =
        AnalysisResult.of("analyzer.a", 1, view.sourceSnapshotId(), CsmScopeInstance.anchoredAt(m1), "x");

    ValidationResult validation = AnalysisResultValidator.validate(result, view, registry);
    assertFalse(validation.valid());
  }

  @Test
  void resultDeclaringAMismatchedSourceSnapshotIsRejected() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    builder.module("moduleA");
    AnalysisView view = AnalysisView.from(builder.build());

    AnalyzerRegistry registry = new AnalyzerRegistry();
    registry.register(new StubAnalyzer("analyzer.a", 1, MODULE_SCOPE));

    AnalysisResultId id =
        AnalysisResultId.of("analyzer.a", 1, new CsmSnapshotId("repo", 99), CsmScopeInstance.wholeRepository());
    AnalysisResult result =
        new AnalysisResult(
            id, "analyzer.a", 1, new CsmSnapshotId("repo", 99), CsmScopeInstance.wholeRepository(), "x");

    ValidationResult validation = AnalysisResultValidator.validate(result, view, registry);
    assertFalse(validation.valid());
  }
}
