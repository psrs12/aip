package aip.core.csm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** {@link CsmValidator} tests (tasks.md 19.1, 19.3). */
class CsmValidatorTest {

  @Test
  void emptyContentIsValid() {
    ValidationResult result = CsmValidator.validate(List.of(), List.of());
    assertTrue(result.valid());
    assertTrue(result.violations().isEmpty());
  }

  @Test
  void ordinaryObservedElementsAndRelationshipsAreValid() {
    CsmElementId moduleId = new CsmElementId("csm:module-a");
    CsmElementId typeId = new CsmElementId("csm:type-a");
    ModuleElement module =
        new ModuleElement(
            moduleId, "module-a", ProvenanceRecord.observed("evidence:1", Instant.EPOCH), NativeAttributes.empty());
    CsmRelationship containment =
        CsmRelationship.of(
            new CsmElementId("csm:rel:1"),
            CsmRelationshipType.CONTAINMENT,
            moduleId,
            typeId,
            ProvenanceRecord.observed("evidence:1", Instant.EPOCH),
            NativeAttributes.empty());

    ValidationResult result = CsmValidator.validate(List.of(module), List.of(containment));

    assertTrue(result.valid());
  }

  @Test
  void observedBoundaryConstraintRelationshipIsInvalid() {
    CsmRelationship boundary =
        CsmRelationship.of(
            new CsmElementId("csm:rel:1"),
            CsmRelationshipType.BOUNDARY_CONSTRAINT,
            new CsmElementId("csm:module-a"),
            new CsmElementId("csm:module-b"),
            ProvenanceRecord.observed("evidence:1", Instant.EPOCH),
            NativeAttributes.empty());

    ValidationResult result = CsmValidator.validate(List.of(), List.of(boundary));

    assertFalse(result.valid());
    assertEquals(1, result.violations().size());
    assertTrue(result.violations().get(0).contains("Architectural Boundary"));
  }

  @Test
  void declaredBoundaryConstraintRelationshipIsValid() {
    CsmRelationship boundary =
        CsmRelationship.of(
            new CsmElementId("csm:rel:1"),
            CsmRelationshipType.BOUNDARY_CONSTRAINT,
            new CsmElementId("csm:module-a"),
            new CsmElementId("csm:module-b"),
            ProvenanceRecord.declared("architecture-doc:1", Instant.EPOCH),
            NativeAttributes.empty());

    ValidationResult result = CsmValidator.validate(List.of(), List.of(boundary));

    assertTrue(result.valid());
  }

  @Test
  void inferredBoundaryConstraintRelationshipIsValid() {
    CsmRelationship boundary =
        CsmRelationship.of(
            new CsmElementId("csm:rel:1"),
            CsmRelationshipType.BOUNDARY_CONSTRAINT,
            new CsmElementId("csm:module-a"),
            new CsmElementId("csm:module-b"),
            ProvenanceRecord.inferred("heuristic:1", Instant.EPOCH, Confidence.HIGH),
            NativeAttributes.empty());

    ValidationResult result = CsmValidator.validate(List.of(), List.of(boundary));

    assertTrue(result.valid());
  }

  @Test
  void conflictedSubjectsDoNotInvalidateOnTheirOwn() {
    // CONFLICTED effective knowledge SHALL NOT, by itself, invalidate
    // the CSM - modeled here as two otherwise-valid, competing
    // relationships for the same subject; the validator has no
    // conflict-awareness at all, so it simply has nothing to object to.
    CsmElementId source = new CsmElementId("csm:module-a");
    CsmElementId target = new CsmElementId("csm:module-b");
    CsmRelationship compileTime =
        new CsmRelationship(
            new CsmElementId("rel:1"),
            CsmRelationshipType.DEPENDENCY,
            source,
            Optional.of(target),
            ProvenanceRecord.observed("evidence:1", Instant.EPOCH),
            NativeAttributes.empty(),
            Optional.of(DependencyKind.COMPILE_TIME));
    CsmRelationship runtime =
        new CsmRelationship(
            new CsmElementId("rel:2"),
            CsmRelationshipType.DEPENDENCY,
            source,
            Optional.of(target),
            ProvenanceRecord.observed("evidence:2", Instant.EPOCH),
            NativeAttributes.empty(),
            Optional.of(DependencyKind.RUNTIME));

    ValidationResult result = CsmValidator.validate(List.of(), List.of(compileTime, runtime));

    assertTrue(result.valid());
  }

  @Test
  void multipleViolationsAreAllReported() {
    CsmRelationship boundaryOne =
        CsmRelationship.of(
            new CsmElementId("csm:rel:1"),
            CsmRelationshipType.BOUNDARY_CONSTRAINT,
            new CsmElementId("csm:module-a"),
            new CsmElementId("csm:module-b"),
            ProvenanceRecord.observed("evidence:1", Instant.EPOCH),
            NativeAttributes.empty());
    CsmRelationship boundaryTwo =
        CsmRelationship.of(
            new CsmElementId("csm:rel:2"),
            CsmRelationshipType.BOUNDARY_CONSTRAINT,
            new CsmElementId("csm:module-c"),
            new CsmElementId("csm:module-d"),
            ProvenanceRecord.observed("evidence:2", Instant.EPOCH),
            NativeAttributes.empty());

    ValidationResult result = CsmValidator.validate(List.of(), List.of(boundaryOne, boundaryTwo));

    assertFalse(result.valid());
    assertEquals(2, result.violations().size());
  }
}
