package aip.core.evidence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Confirms the Repository Evidence contract's vocabulary is closed to
 * exactly what {@code software-repository-understanding/spec.md}
 * defines (tasks.md 3.6), and that its enforced invariants (discovery
 * outcome / failure reason pairing; change-status / lifecycle-state
 * pairing; relationship referential integrity) hold. The broader,
 * source-scan-level enforcement of "no duplicate Evidence type exists
 * anywhere else in the codebase" (invariant 3) is tasks.md task 24.2 —
 * this test covers the vocabulary-closure and validation half of that
 * invariant, not the full-codebase scan.
 */
class EvidenceVocabularyClosureTest {

  @Test
  void evidenceKindsMatchExactlyWhatTheArchivedSpecDefines() {
    assertEquals(
        Set.of(
            EvidenceKind.REPOSITORY,
            EvidenceKind.PROJECT,
            EvidenceKind.MODULE,
            EvidenceKind.PACKAGE,
            EvidenceKind.FILE,
            EvidenceKind.SOURCE_UNIT,
            EvidenceKind.METHOD,
            EvidenceKind.MANIFEST_DEPENDENCY_EDGE,
            EvidenceKind.IMPORT_EDGE,
            EvidenceKind.API_CONTRACT_DECLARATION,
            EvidenceKind.CONFIG_FILE,
            EvidenceKind.CONFIG_REFERENCE),
        Set.of(EvidenceKind.values()),
        "EvidenceKind must contain exactly the 12 Evidence kinds the archived RU specification"
            + " defines — no more, no fewer");
  }

  @Test
  void failureReasonsMatchExactlyWhatTheArchivedSpecDefines() {
    assertEquals(
        Set.of(
            FailureReason.PARSE_ERROR,
            FailureReason.UNSUPPORTED_CONSTRUCT,
            FailureReason.UNSUPPORTED_LANGUAGE,
            FailureReason.SIZE_LIMIT_EXCEEDED,
            FailureReason.TIMEOUT,
            FailureReason.BINARY_OR_NON_TEXT,
            FailureReason.EXCLUDED_BY_CONFIGURATION,
            FailureReason.CONFLICTING_OVERRIDE),
        Set.of(FailureReason.values()));
  }

  @Test
  void extractionMethodsMatchExactlyWhatTheArchivedSpecDefines() {
    assertEquals(
        Set.of(
            ExtractionMethod.FULL_PARSE,
            ExtractionMethod.HEURISTIC_SCAN,
            ExtractionMethod.MANIFEST_DECLARED,
            ExtractionMethod.EXTERNALLY_DECLARED),
        Set.of(ExtractionMethod.values()));
  }

  @Test
  void changeStatusesMatchExactlyWhatTheArchivedSpecDefines() {
    assertEquals(
        Set.of(ChangeStatus.ADDED, ChangeStatus.UNCHANGED, ChangeStatus.MODIFIED, ChangeStatus.REMOVED),
        Set.of(ChangeStatus.values()));
  }

  @Test
  void lifecycleStatesMatchExactlyWhatTheArchivedSpecDefines() {
    assertEquals(
        Set.of(LifecycleState.PRESENT, LifecycleState.TOMBSTONED, LifecycleState.PURGED),
        Set.of(LifecycleState.values()));
  }

  @Test
  void changeStatusAndLifecycleStateAreDistinctTypes() {
    // These two vocabularies SHALL NOT be used interchangeably.
    assertTrue(!ChangeStatus.class.equals(LifecycleState.class));
  }

  @Test
  void partialOutcomeWithoutFailureReasonIsRejected() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new DiscoveryOutcome(DiscoveryOutcomeStatus.PARTIAL, Optional.empty()));
  }

  @Test
  void completeOutcomeWithFailureReasonIsRejected() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new DiscoveryOutcome(
                DiscoveryOutcomeStatus.COMPLETE, Optional.of(FailureReason.PARSE_ERROR)));
  }

  @Test
  void addedChangeStatusMustPairWithPresentLifecycleState() {
    EvidenceItem item = sampleItem();
    assertThrows(
        IllegalArgumentException.class,
        () -> new ClassifiedEvidenceItem(item, ChangeStatus.ADDED, LifecycleState.TOMBSTONED));
  }

  @Test
  void removedChangeStatusMustPairWithTombstonedOrPurgedLifecycleState() {
    EvidenceItem item = sampleItem();
    assertThrows(
        IllegalArgumentException.class,
        () -> new ClassifiedEvidenceItem(item, ChangeStatus.REMOVED, LifecycleState.PRESENT));
    // Both valid pairings do not throw.
    new ClassifiedEvidenceItem(item, ChangeStatus.REMOVED, LifecycleState.TOMBSTONED);
    new ClassifiedEvidenceItem(item, ChangeStatus.REMOVED, LifecycleState.PURGED);
  }

  @Test
  void repositoryEvidenceModelRejectsDanglingRelationshipEndpoints() {
    EvidenceItem module = sampleItem();
    EvidenceId danglingTarget = new EvidenceId("repo-1", EvidenceKind.PACKAGE, "com.acme.missing");
    EvidenceRelationship relationship =
        new EvidenceRelationship(EvidenceRelationshipType.CONTAINMENT, module.id(), danglingTarget);

    assertThrows(
        IllegalArgumentException.class,
        () -> RepositoryEvidenceModel.of(List.of(module), List.of(relationship)));
  }

  @Test
  void repositoryEvidenceModelFindsItemsByIdAndKind() {
    EvidenceItem repository =
        new EvidenceItem(
            new EvidenceId("repo-1", EvidenceKind.REPOSITORY, "repo-1"),
            EvidenceAttributes.empty(),
            Optional.empty(),
            DiscoveryOutcome.complete(),
            ExtractionMethod.EXTERNALLY_DECLARED);
    EvidenceItem module = sampleItem();
    EvidenceRelationship containment =
        new EvidenceRelationship(EvidenceRelationshipType.CONTAINMENT, repository.id(), module.id());

    RepositoryEvidenceModel model =
        RepositoryEvidenceModel.of(List.of(repository, module), List.of(containment));

    assertEquals(Optional.of(repository), model.find(repository.id()));
    assertEquals(Optional.of(repository), model.repository());
  }

  private static EvidenceItem sampleItem() {
    return new EvidenceItem(
        new EvidenceId("repo-1", EvidenceKind.MODULE, "com.acme:module-a"),
        EvidenceAttributes.empty(),
        Optional.of(EvidenceLocation.of("module-a/pom.xml")),
        DiscoveryOutcome.complete(),
        ExtractionMethod.MANIFEST_DECLARED);
  }
}
