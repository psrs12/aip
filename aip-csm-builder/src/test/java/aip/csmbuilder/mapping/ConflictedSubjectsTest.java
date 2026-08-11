package aip.csmbuilder.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.core.csm.CsmElementId;
import aip.core.csm.CsmRelationship;
import aip.core.csm.CsmRelationshipType;
import aip.core.csm.DependencyKind;
import aip.core.csm.EffectiveKnowledgeStatus;
import aip.core.csm.NativeAttributes;
import aip.core.csm.ProvenanceRecord;
import aip.core.csm.Subject;
import aip.core.csm.SubjectConflictMarker;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Conflict and Precedence Integration tests (tasks.md 18.3): two
 * conflicting {@code observed} assertions CSM Builder constructs are
 * both preserved, and their shared subject is marked {@code CONFLICTED},
 * using the Canonical Software Model's own conflict-marking mechanism
 * rather than any arbitration of CSM Builder's own (18.1, 18.2).
 */
class ConflictedSubjectsTest {

  private static final CsmElementId MODULE_A = new CsmElementId("csm:module-a");
  private static final CsmElementId MODULE_B = new CsmElementId("csm:module-b");

  @Test
  void twoConflictingObservedRelationshipsAreBothPreservedAndTheirSubjectIsConflicted() {
    // Simulates two independent observed facts about the same
    // (source, target, DEPENDENCY) subject disagreeing on kind - the
    // archived spec's own example ("two independent analyzer passes
    // produce contradictory observed relationships for the same
    // subject").
    CsmRelationship compileTime = dependency("rel:compile-time", DependencyKind.COMPILE_TIME);
    CsmRelationship runtime = dependency("rel:runtime", DependencyKind.RUNTIME);
    MappingResult resultFromPassOne = MappingResult.ofRelationship(compileTime);
    MappingResult resultFromPassTwo = MappingResult.ofRelationship(runtime);

    // 18.2: MappingResult.merge - the only place CSM Builder combines
    // per-item results - never arbitrates; it simply concatenates.
    MappingResult merged = resultFromPassOne.merge(resultFromPassTwo);
    assertEquals(2, merged.relationships().size(), "both conflicting relationships are preserved by construction");
    assertTrue(merged.relationships().contains(compileTime));
    assertTrue(merged.relationships().contains(runtime));

    // 18.1: applying the CSM's own conflict-marking mechanism reports
    // the shared subject as CONFLICTED, with both assertions retained.
    Map<Subject, SubjectConflictMarker.Classification<CsmRelationship>> classified =
        ConflictedSubjects.classify(merged);

    assertEquals(1, classified.size());
    SubjectConflictMarker.Classification<CsmRelationship> classification = classified.values().iterator().next();
    assertEquals(EffectiveKnowledgeStatus.CONFLICTED, classification.status());
    assertEquals(List.of(compileTime, runtime), classification.assertions());
  }

  @Test
  void anUncontestedRelationshipIsEffective() {
    CsmRelationship only = dependency("rel:1", DependencyKind.COMPILE_TIME);
    MappingResult result = MappingResult.ofRelationship(only);

    Map<Subject, SubjectConflictMarker.Classification<CsmRelationship>> classified =
        ConflictedSubjects.classify(result);

    assertEquals(EffectiveKnowledgeStatus.EFFECTIVE, classified.values().iterator().next().status());
  }

  @Test
  void noRelationshipsClassifyToAnEmptyMap() {
    assertTrue(ConflictedSubjects.classify(MappingResult.empty()).isEmpty());
  }

  private static CsmRelationship dependency(String id, DependencyKind kind) {
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
