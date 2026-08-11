package aip.core.csm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Confirms the CSM domain model's vocabulary is closed to exactly what
 * {@code openspec/specs/canonical-software-model/spec.md} defines
 * (tasks.md 2.6) — no accidental extra entity or relationship kind,
 * and the closed-vocabulary invariants (Architecture Component never
 * observed, inferred requires confidence) are enforced, not merely
 * documented.
 */
class CsmVocabularyClosureTest {

  @Test
  void entityKindsMatchExactlyWhatTheArchivedSpecDefines() {
    // CSM Conceptual Vocabulary: "Repository, Project, Module, Package,
    // Type, Architecture Component, and External System. Method is
    // also part of the core vocabulary..."
    assertEquals(
        Set.of(
            CsmEntityKind.REPOSITORY,
            CsmEntityKind.PROJECT,
            CsmEntityKind.MODULE,
            CsmEntityKind.PACKAGE,
            CsmEntityKind.TYPE,
            CsmEntityKind.METHOD,
            CsmEntityKind.ARCHITECTURE_COMPONENT,
            CsmEntityKind.EXTERNAL_SYSTEM),
        Set.of(CsmEntityKind.values()),
        "CsmEntityKind must contain exactly the 8 core structural entity kinds the archived CSM"
            + " specification defines — no more, no fewer");
  }

  @Test
  void relationshipTypesMatchExactlyWhatTheArchivedSpecDefines() {
    // CSM Conceptual Vocabulary: "containment, dependency,
    // implementation/extension, invocation, exposure/consumption,
    // integration, composition, and boundary/constraint relationships."
    assertEquals(
        Set.of(
            CsmRelationshipType.CONTAINMENT,
            CsmRelationshipType.DEPENDENCY,
            CsmRelationshipType.IMPLEMENTATION_EXTENSION,
            CsmRelationshipType.INVOCATION,
            CsmRelationshipType.EXPOSURE_CONSUMPTION,
            CsmRelationshipType.INTEGRATION,
            CsmRelationshipType.COMPOSITION,
            CsmRelationshipType.BOUNDARY_CONSTRAINT),
        Set.of(CsmRelationshipType.values()),
        "CsmRelationshipType must contain exactly the 8 core relationship types the archived CSM"
            + " specification defines — no more, no fewer");
  }

  @Test
  void provenanceCategoriesMatchExactlyWhatTheArchivedSpecDefines() {
    assertEquals(
        Set.of(ProvenanceCategory.OBSERVED, ProvenanceCategory.DECLARED, ProvenanceCategory.INFERRED),
        Set.of(ProvenanceCategory.values()));
  }

  @Test
  void confidenceLevelsMatchExactlyWhatTheArchivedSpecDefines() {
    assertEquals(Set.of(Confidence.HIGH, Confidence.MEDIUM, Confidence.LOW), Set.of(Confidence.values()));
  }

  @Test
  void everySealedCsmElementSubtypeMapsToADistinctEntityKind() {
    // Exercises every permitted CsmElement subtype's kind() so the
    // sealed hierarchy and the enum stay in lockstep — a missing or
    // duplicated mapping here would mean the two vocabularies have
    // drifted apart.
    ProvenanceRecord observed = ProvenanceRecord.observed("evidence:1", Instant.EPOCH);
    NativeAttributes attrs = NativeAttributes.empty();
    CsmElementId id = new CsmElementId("id:1");

    Set<CsmEntityKind> kinds =
        Set.of(
            new RepositoryElement(id, "repo", observed, attrs).kind(),
            new ProjectElement(id, "proj", observed, attrs).kind(),
            new ModuleElement(id, "mod", observed, attrs).kind(),
            new PackageElement(id, "pkg", observed, attrs).kind(),
            new TypeElement(id, "Type", observed, attrs, Optional.empty()).kind(),
            new MethodElement(id, "method", observed, attrs, Optional.empty()).kind(),
            new ArchitectureComponentElement(
                    id,
                    "component",
                    ProvenanceRecord.declared("human:architect", Instant.EPOCH),
                    attrs,
                    List.of(id))
                .kind(),
            ExternalSystemElement.unresolved(id, "external", observed).kind());

    assertEquals(Set.of(CsmEntityKind.values()), kinds);
  }

  @Test
  void architectureComponentRejectsObservedProvenance() {
    // Component provenance is never observed.
    ProvenanceRecord observed = ProvenanceRecord.observed("evidence:1", Instant.EPOCH);
    CsmElementId id = new CsmElementId("id:1");
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ArchitectureComponentElement(
                id, "component", observed, NativeAttributes.empty(), List.of(id)));
  }

  @Test
  void inferredProvenanceWithoutConfidenceIsRejected() {
    // Inferred element missing confidence is invalid.
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ProvenanceRecord(
                ProvenanceCategory.INFERRED, "heuristic:1", Instant.EPOCH, Optional.empty()));
  }

  @Test
  void dependencyKindOnlyMeaningfulForDependencyRelationships() {
    CsmElementId a = new CsmElementId("a");
    CsmElementId b = new CsmElementId("b");
    ProvenanceRecord observed = ProvenanceRecord.observed("evidence:1", Instant.EPOCH);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new CsmRelationship(
                new CsmElementId("rel:1"),
                CsmRelationshipType.CONTAINMENT,
                a,
                b,
                observed,
                NativeAttributes.empty(),
                Optional.of(DependencyKind.COMPILE_TIME)));
  }
}
