package aip.core.csm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * {@link CsmScopeEvaluator} tests, per {@code Analysis Scope
 * Declaration}/{@code Rule Scope Declaration}, {@code Kind-Based
 * Analyzer Applicability}/{@code Kind-Based Rule Applicability}, and
 * {@code Native-Attribute Applicability Refinement}/{@code
 * Native-Attribute Rule Applicability Refinement} — the general
 * mechanism both `aip-analysis` and `aip-rules` are built on.
 */
class CsmScopeEvaluatorTest {

  private static final CsmSnapshotId SNAPSHOT_ID = new CsmSnapshotId("repo", 1);

  @Test
  void unanchoredScopeEnumeratesExactlyOneWholeRepositoryInstance() {
    AnalysisView view = viewOf(Set.of(module("m1", "moduleA")), Set.of());
    CsmScope scope = CsmScope.ofEntityKinds(Set.of(CsmEntityKind.MODULE));
    assertEquals(List.of(CsmScopeInstance.wholeRepository()), CsmScopeEvaluator.enumerateInstances(view, scope));
  }

  @Test
  void anchoredScopeEnumeratesOneInstancePerMatchingContainedElement() {
    ModuleElement m1 = module("m1", "moduleA");
    ModuleElement m2 = module("m2", "moduleB");
    AnalysisView view = viewOf(Set.of(m1, m2), Set.of());
    CsmScope scope = CsmScope.ofEntityKinds(Set.of(CsmEntityKind.TYPE)).anchoredAt(CsmEntityKind.MODULE);

    List<CsmScopeInstance> instances = CsmScopeEvaluator.enumerateInstances(view, scope);
    assertEquals(
        Set.of(CsmScopeInstance.anchoredAt(m1.id()), CsmScopeInstance.anchoredAt(m2.id())), Set.copyOf(instances));
  }

  @Test
  void skipsWhenNoDeclaredKindIsPresent() {
    AnalysisView view = viewOf(Set.of(module("m1", "moduleA")), Set.of());
    CsmScope scope = CsmScope.ofEntityKinds(Set.of(CsmEntityKind.PACKAGE));
    assertFalse(CsmScopeEvaluator.isApplicable(view, scope, CsmScopeInstance.wholeRepository()));
  }

  @Test
  void invokesWhenADeclaredKindIsPresent() {
    AnalysisView view = viewOf(Set.of(module("m1", "moduleA")), Set.of());
    CsmScope scope = CsmScope.ofEntityKinds(Set.of(CsmEntityKind.MODULE));
    assertTrue(CsmScopeEvaluator.isApplicable(view, scope, CsmScopeInstance.wholeRepository()));
  }

  @Test
  void anchoredApplicabilityIsScopedToTheAnchorsOwnContainmentClosure() {
    ModuleElement m1 = module("m1", "moduleA");
    ModuleElement m2 = module("m2", "moduleB");
    TypeElement t1 = type("t1", "TypeInModuleA");
    CsmRelationship containment = containment("c1", m1.id(), t1.id());
    AnalysisView view = viewOf(Set.of(m1, m2, t1), Set.of(containment));

    CsmScope scope = CsmScope.ofEntityKinds(Set.of(CsmEntityKind.TYPE)).anchoredAt(CsmEntityKind.MODULE);

    assertTrue(CsmScopeEvaluator.isApplicable(view, scope, CsmScopeInstance.anchoredAt(m1.id())));
    assertFalse(CsmScopeEvaluator.isApplicable(view, scope, CsmScopeInstance.anchoredAt(m2.id())));
  }

  @Test
  void nativeAttributeRefinementSkipsWhenNoMatchingAttributeIsPresent() {
    AnalysisView view = viewOf(Set.of(module("m1", "moduleA")), Set.of());
    CsmScope scope =
        CsmScope.ofEntityKinds(Set.of(CsmEntityKind.MODULE))
            .withNativeAttributePredicate(attrs -> attrs.get("ecosystem").filter("maven"::equals).isPresent());
    assertFalse(CsmScopeEvaluator.isApplicable(view, scope, CsmScopeInstance.wholeRepository()));
  }

  @Test
  void nativeAttributeRefinementInvokesWhenTheAttributeMatches() {
    ModuleElement m1 =
        new ModuleElement(
            new CsmElementId("m1"),
            "moduleA",
            ProvenanceRecord.observed("fixture", Instant.EPOCH),
            NativeAttributes.of(Map.of("ecosystem", "maven")));
    AnalysisView view = viewOf(Set.of(m1), Set.of());
    CsmScope scope =
        CsmScope.ofEntityKinds(Set.of(CsmEntityKind.MODULE))
            .withNativeAttributePredicate(attrs -> attrs.get("ecosystem").filter("maven"::equals).isPresent());
    assertTrue(CsmScopeEvaluator.isApplicable(view, scope, CsmScopeInstance.wholeRepository()));
  }

  private static ModuleElement module(String id, String name) {
    return new ModuleElement(
        new CsmElementId(id), name, ProvenanceRecord.observed("fixture", Instant.EPOCH), NativeAttributes.empty());
  }

  private static TypeElement type(String id, String name) {
    return new TypeElement(
        new CsmElementId(id),
        name,
        ProvenanceRecord.observed("fixture", Instant.EPOCH),
        NativeAttributes.empty(),
        Optional.empty());
  }

  private static CsmRelationship containment(String id, CsmElementId sourceId, CsmElementId targetId) {
    return CsmRelationship.of(
        new CsmElementId(id),
        CsmRelationshipType.CONTAINMENT,
        sourceId,
        targetId,
        ProvenanceRecord.observed("fixture", Instant.EPOCH),
        NativeAttributes.empty());
  }

  private static AnalysisView viewOf(Set<CsmElement> elements, Set<CsmRelationship> relationships) {
    return AnalysisView.from(
        new CsmSnapshotSource() {
          @Override
          public Set<CsmElement> elements() {
            return elements;
          }

          @Override
          public Set<CsmRelationship> relationships() {
            return relationships;
          }

          @Override
          public CsmSnapshotId id() {
            return SNAPSHOT_ID;
          }
        });
  }
}
