package aip.core.csm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * {@link AnalysisView} tests, per {@code Analysis View Construction}
 * and {@code Analysis View Is a Derived Projection, Not a New
 * Canonical Model}.
 */
class AnalysisViewTest {

  private static final CsmSnapshotId SNAPSHOT_ID = new CsmSnapshotId("repo", 1);

  @Test
  void exposesItsSourceCsmSnapshotIdentity() {
    AnalysisView view = AnalysisView.from(fixtureSource(Set.of(), Set.of()));
    assertEquals(SNAPSHOT_ID, view.sourceSnapshotId());
  }

  @Test
  void reflectsEffectiveKnowledgeForAnUncontestedSubject() {
    ModuleElement module = module("csm:m1", "moduleA");
    AnalysisView view = AnalysisView.from(fixtureSource(Set.of(module), Set.of()));

    Optional<EffectiveKnowledgeStatus> status = view.statusOf(Subject.forElementIdentity(module.id()));
    assertEquals(EffectiveKnowledgeStatus.EFFECTIVE, status.orElseThrow());
    assertTrue(view.elements().contains(module));
  }

  @Test
  void marksConflictingKnowledgeAsConflicted() {
    CsmElementId source = new CsmElementId("csm:m1");
    CsmElementId target = new CsmElementId("csm:m2");
    CsmRelationship compileTime = dependency("rel:1", source, target, DependencyKind.COMPILE_TIME);
    CsmRelationship runtime = dependency("rel:2", source, target, DependencyKind.RUNTIME);

    AnalysisView view = AnalysisView.from(fixtureSource(Set.of(), Set.of(compileTime, runtime)));

    Subject subject = Subject.forRelationship(source, CsmRelationshipType.DEPENDENCY, Optional.of(target));
    SubjectConflictMarker.Classification<CsmRelationship> classification =
        view.relationshipClassifications().get(subject);
    assertEquals(EffectiveKnowledgeStatus.CONFLICTED, classification.status());
    assertEquals(2, classification.assertions().size());
  }

  @Test
  void twoConstructionsFromEquivalentContentProduceEquivalentViews() {
    ModuleElement module = module("csm:m1", "moduleA");
    AnalysisView first = AnalysisView.from(fixtureSource(Set.of(module), Set.of()));
    AnalysisView second = AnalysisView.from(fixtureSource(Set.of(module), Set.of()));
    assertEquals(first.elements(), second.elements());
    assertEquals(first.sourceSnapshotId(), second.sourceSnapshotId());
  }

  @Test
  void elementsOfKindFiltersByEntityKind() {
    ModuleElement module = module("csm:m1", "moduleA");
    AnalysisView view = AnalysisView.from(fixtureSource(Set.of(module), Set.of()));
    assertEquals(Set.of(module), view.elementsOfKind(CsmEntityKind.MODULE));
    assertEquals(Set.of(), view.elementsOfKind(CsmEntityKind.PACKAGE));
  }

  private static ModuleElement module(String id, String name) {
    return new ModuleElement(
        new CsmElementId(id), name, ProvenanceRecord.observed("src:" + id, Instant.EPOCH), NativeAttributes.empty());
  }

  private static CsmRelationship dependency(
      String id, CsmElementId sourceId, CsmElementId targetId, DependencyKind kind) {
    return new CsmRelationship(
        new CsmElementId(id),
        CsmRelationshipType.DEPENDENCY,
        sourceId,
        Optional.of(targetId),
        ProvenanceRecord.observed("src:" + id, Instant.EPOCH),
        NativeAttributes.empty(),
        Optional.of(kind));
  }

  private static CsmSnapshotSource fixtureSource(Set<CsmElement> elements, Set<CsmRelationship> relationships) {
    return new CsmSnapshotSource() {
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
    };
  }
}
