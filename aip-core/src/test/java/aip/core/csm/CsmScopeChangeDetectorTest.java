package aip.core.csm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * {@link CsmScopeChangeDetector} tests, per {@code Incremental
 * Analysis Is Architecturally Supported}.
 */
class CsmScopeChangeDetectorTest {

  @Test
  void reportsNoDifferenceWhenScopedContentIsUnchanged() {
    AnalysisView earlier = viewOf(1, Set.of(module("m1", "moduleA")), Set.of());
    AnalysisView later = viewOf(2, Set.of(module("m1", "moduleA")), Set.of());
    CsmScope scope = CsmScope.ofEntityKinds(Set.of(CsmEntityKind.MODULE));

    assertFalse(CsmScopeChangeDetector.scopeContentDiffers(earlier, later, scope));
  }

  @Test
  void reportsADifferenceWhenScopedContentChanged() {
    AnalysisView earlier = viewOf(1, Set.of(module("m1", "moduleA")), Set.of());
    AnalysisView later = viewOf(2, Set.of(module("m1", "moduleA"), module("m2", "moduleB")), Set.of());
    CsmScope scope = CsmScope.ofEntityKinds(Set.of(CsmEntityKind.MODULE));

    assertTrue(CsmScopeChangeDetector.scopeContentDiffers(earlier, later, scope));
  }

  @Test
  void ignoresContentOutsideTheDeclaredScope() {
    ModuleElement m1 = module("m1", "moduleA");
    AnalysisView earlier = viewOf(1, Set.of(m1), Set.of());
    // Only a PACKAGE is added between snapshots; the scope below only reads MODULE.
    AnalysisView later = viewOf(2, Set.of(m1, pkg("p1", "pkgA")), Set.of());
    CsmScope scope = CsmScope.ofEntityKinds(Set.of(CsmEntityKind.MODULE));

    assertFalse(CsmScopeChangeDetector.scopeContentDiffers(earlier, later, scope));
  }

  @Test
  void detectsRelationshipTypeScopedDifferences() {
    ModuleElement m1 = module("m1", "moduleA");
    ModuleElement m2 = module("m2", "moduleB");
    CsmRelationship dependency = dependency("d1", m1.id(), m2.id());

    AnalysisView earlier = viewOf(1, Set.of(m1, m2), Set.of());
    AnalysisView later = viewOf(2, Set.of(m1, m2), Set.of(dependency));
    CsmScope scope = CsmScope.ofRelationshipTypes(Set.of(CsmRelationshipType.DEPENDENCY));

    assertTrue(CsmScopeChangeDetector.scopeContentDiffers(earlier, later, scope));
  }

  private static ModuleElement module(String id, String name) {
    return new ModuleElement(
        new CsmElementId(id), name, ProvenanceRecord.observed("fixture", Instant.EPOCH), NativeAttributes.empty());
  }

  private static PackageElement pkg(String id, String name) {
    return new PackageElement(
        new CsmElementId(id), name, ProvenanceRecord.observed("fixture", Instant.EPOCH), NativeAttributes.empty());
  }

  private static CsmRelationship dependency(String id, CsmElementId sourceId, CsmElementId targetId) {
    return new CsmRelationship(
        new CsmElementId(id),
        CsmRelationshipType.DEPENDENCY,
        sourceId,
        java.util.Optional.of(targetId),
        ProvenanceRecord.observed("fixture", Instant.EPOCH),
        NativeAttributes.empty(),
        java.util.Optional.of(DependencyKind.COMPILE_TIME));
  }

  private static AnalysisView viewOf(long sequenceNumber, Set<CsmElement> elements, Set<CsmRelationship> relationships) {
    CsmSnapshotId id = new CsmSnapshotId("repo", sequenceNumber);
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
            return id;
          }
        });
  }
}
