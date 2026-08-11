package aip.core.csm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * {@link SubjectConflictMarker} tests, per {@code Same-Category
 * Conflict Marking and Resolution} and {@code Non-Destructive
 * Preservation of Competing Knowledge}.
 */
class SubjectConflictMarkerTest {

  private static final CsmElementId MODULE_A = new CsmElementId("csm:module-a");
  private static final CsmElementId MODULE_B = new CsmElementId("csm:module-b");

  @Test
  void twoConflictingObservedAssertionsForTheSameSubjectAreBothPreservedAndMarkedConflicted() {
    CsmRelationship compileTime =
        relationship("rel:1", DependencyKind.COMPILE_TIME);
    CsmRelationship runtime = relationship("rel:2", DependencyKind.RUNTIME);

    Map<Subject, SubjectConflictMarker.Classification<CsmRelationship>> classified =
        SubjectConflictMarker.classify(List.of(compileTime, runtime), this::subjectOf);

    assertEquals(1, classified.size(), "both relationships share one subject");
    SubjectConflictMarker.Classification<CsmRelationship> classification =
        classified.values().iterator().next();
    assertEquals(EffectiveKnowledgeStatus.CONFLICTED, classification.status());
    assertEquals(2, classification.assertions().size());
    assertTrue(classification.assertions().contains(compileTime), "the first assertion is preserved");
    assertTrue(classification.assertions().contains(runtime), "the second assertion is preserved, not overwritten");
  }

  @Test
  void aSingleAssertionForASubjectIsEffective() {
    CsmRelationship only = relationship("rel:1", DependencyKind.COMPILE_TIME);

    Map<Subject, SubjectConflictMarker.Classification<CsmRelationship>> classified =
        SubjectConflictMarker.classify(List.of(only), this::subjectOf);

    SubjectConflictMarker.Classification<CsmRelationship> classification =
        classified.values().iterator().next();
    assertEquals(EffectiveKnowledgeStatus.EFFECTIVE, classification.status());
    assertEquals(List.of(only), classification.assertions());
  }

  @Test
  void assertionsAboutDifferentSubjectsDoNotCompete() {
    CsmRelationship towardB = relationship("rel:1", DependencyKind.COMPILE_TIME);
    CsmRelationship towardOther =
        new CsmRelationship(
            new CsmElementId("rel:2"),
            CsmRelationshipType.DEPENDENCY,
            MODULE_A,
            Optional.of(new CsmElementId("csm:module-c")),
            ProvenanceRecord.observed("evidence:2", Instant.EPOCH),
            NativeAttributes.empty(),
            Optional.of(DependencyKind.COMPILE_TIME));

    Map<Subject, SubjectConflictMarker.Classification<CsmRelationship>> classified =
        SubjectConflictMarker.classify(List.of(towardB, towardOther), this::subjectOf);

    assertEquals(2, classified.size(), "different targets are different subjects");
    assertTrue(classified.values().stream().allMatch(c -> c.status() == EffectiveKnowledgeStatus.EFFECTIVE));
  }

  @Test
  void classificationRejectsConflictedWithFewerThanTwoAssertions() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new SubjectConflictMarker.Classification<>(
                EffectiveKnowledgeStatus.CONFLICTED, List.of("only-one")));
  }

  private Subject subjectOf(CsmRelationship relationship) {
    return Subject.forRelationship(relationship.sourceId(), relationship.type(), relationship.targetId());
  }

  private static CsmRelationship relationship(String id, DependencyKind kind) {
    return new CsmRelationship(
        new CsmElementId(id),
        CsmRelationshipType.DEPENDENCY,
        MODULE_A,
        Optional.of(MODULE_B),
        ProvenanceRecord.observed("evidence:" + id, Instant.EPOCH),
        NativeAttributes.empty(),
        Optional.of(kind));
  }
}
