package aip.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.analysis.test.fixtures.CsmSnapshotSourceBuilder;
import aip.core.csm.AnalysisView;
import aip.core.csm.CsmElementId;
import aip.core.csm.CsmEntityKind;
import aip.core.csm.CsmScope;
import aip.core.csm.CsmScopeInstance;
import aip.core.csm.NativeAttributes;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * {@link AnalysisScopeEvaluator} tests, per {@code Analysis Scope
 * Declaration}, {@code Kind-Based Analyzer Applicability}, and {@code
 * Native-Attribute Applicability Refinement}.
 */
class AnalysisScopeEvaluatorTest {

  @Test
  void unanchoredScopeEnumeratesExactlyOneWholeRepositoryInstance() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    builder.module("moduleA");
    AnalysisView view = AnalysisView.from(builder.build());

    CsmScope scope = CsmScope.ofEntityKinds(Set.of(CsmEntityKind.MODULE));
    List<CsmScopeInstance> instances = AnalysisScopeEvaluator.enumerateInstances(view, scope);

    assertEquals(List.of(CsmScopeInstance.wholeRepository()), instances);
  }

  @Test
  void anchoredScopeEnumeratesOneInstancePerMatchingContainedElement() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    CsmElementId m1 = builder.module("moduleA");
    CsmElementId m2 = builder.module("moduleB");
    AnalysisView view = AnalysisView.from(builder.build());

    CsmScope scope = CsmScope.ofEntityKinds(Set.of(CsmEntityKind.TYPE)).anchoredAt(CsmEntityKind.MODULE);
    List<CsmScopeInstance> instances = AnalysisScopeEvaluator.enumerateInstances(view, scope);

    assertEquals(
        Set.of(CsmScopeInstance.anchoredAt(m1), CsmScopeInstance.anchoredAt(m2)), Set.copyOf(instances));
  }

  @Test
  void skipsWhenNoDeclaredKindIsPresent() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    builder.module("moduleA");
    AnalysisView view = AnalysisView.from(builder.build());

    CsmScope scope = CsmScope.ofEntityKinds(Set.of(CsmEntityKind.PACKAGE));
    assertFalse(AnalysisScopeEvaluator.isApplicable(view, scope, CsmScopeInstance.wholeRepository()));
  }

  @Test
  void invokesWhenADeclaredKindIsPresent() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    builder.module("moduleA");
    AnalysisView view = AnalysisView.from(builder.build());

    CsmScope scope = CsmScope.ofEntityKinds(Set.of(CsmEntityKind.MODULE));
    assertTrue(AnalysisScopeEvaluator.isApplicable(view, scope, CsmScopeInstance.wholeRepository()));
  }

  @Test
  void anchoredApplicabilityIsScopedToTheAnchorsOwnContainmentClosure() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    CsmElementId m1 = builder.module("moduleA");
    CsmElementId m2 = builder.module("moduleB");
    CsmElementId t1 = builder.type("TypeInModuleA");
    builder.containment(m1, t1);
    AnalysisView view = AnalysisView.from(builder.build());

    CsmScope scope = CsmScope.ofEntityKinds(Set.of(CsmEntityKind.TYPE)).anchoredAt(CsmEntityKind.MODULE);

    assertTrue(AnalysisScopeEvaluator.isApplicable(view, scope, CsmScopeInstance.anchoredAt(m1)));
    assertFalse(AnalysisScopeEvaluator.isApplicable(view, scope, CsmScopeInstance.anchoredAt(m2)));
  }

  @Test
  void nativeAttributeRefinementSkipsWhenNoMatchingAttributeIsPresent() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    builder.module("moduleA");
    AnalysisView view = AnalysisView.from(builder.build());

    CsmScope scope =
        CsmScope.ofEntityKinds(Set.of(CsmEntityKind.MODULE))
            .withNativeAttributePredicate(attrs -> attrs.get("ecosystem").filter("maven"::equals).isPresent());

    assertFalse(AnalysisScopeEvaluator.isApplicable(view, scope, CsmScopeInstance.wholeRepository()));
  }

  @Test
  void nativeAttributeRefinementInvokesWhenTheAttributeMatches() {
    CsmSnapshotSourceBuilder builder = CsmSnapshotSourceBuilder.forRepository("repo");
    builder.module("moduleA", NativeAttributes.of(java.util.Map.of("ecosystem", "maven")));
    AnalysisView view = AnalysisView.from(builder.build());

    CsmScope scope =
        CsmScope.ofEntityKinds(Set.of(CsmEntityKind.MODULE))
            .withNativeAttributePredicate(attrs -> attrs.get("ecosystem").filter("maven"::equals).isPresent());

    assertTrue(AnalysisScopeEvaluator.isApplicable(view, scope, CsmScopeInstance.wholeRepository()));
  }
}
