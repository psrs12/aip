package aip.csmbuilder.snapshot;

import aip.core.csm.CsmValidator;
import aip.core.csm.ValidationResult;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * CSM Builder's publication gate, per {@code Snapshot Validation Before
 * Use}: "CSM Builder SHALL validate a constructed CSM snapshot against
 * the Canonical Software Model's validation expectations before that
 * snapshot is considered usable. A snapshot that fails this validation
 * SHALL NOT be published as usable CSM content" (tasks.md 19.1, 19.2).
 *
 * <p>Deliberately a layer in front of {@link SnapshotStore}, not inside
 * it: {@link SnapshotStore} stays a general, validation-agnostic
 * persistence mechanism (per {@code design.md} Decision 2); the
 * decision to withhold publication on a failed validation is CSM
 * Builder's own business rule, applied here before {@link
 * SnapshotStore#write} is ever called — an invalid snapshot is never
 * written at all, not written-then-flagged.
 */
public final class SnapshotPublisher {

  private SnapshotPublisher() {}

  public static PublicationOutcome publish(
      SnapshotStore store, String repositoryIdentifier, SnapshotContent content, Instant constructionTimestamp) {
    Objects.requireNonNull(store, "store");
    Objects.requireNonNull(repositoryIdentifier, "repositoryIdentifier");
    Objects.requireNonNull(content, "content");
    Objects.requireNonNull(constructionTimestamp, "constructionTimestamp");

    ValidationResult validation = CsmValidator.validate(content.elements(), content.relationships());
    if (!validation.valid()) {
      return new PublicationOutcome(Optional.empty(), validation);
    }

    Snapshot snapshot = store.write(repositoryIdentifier, content, constructionTimestamp);
    return new PublicationOutcome(Optional.of(snapshot), validation);
  }

  /**
   * @param snapshot present only when {@code validation} passed and the
   *     snapshot was actually written; absent when validation failed.
   */
  public record PublicationOutcome(Optional<Snapshot> snapshot, ValidationResult validation) {

    public PublicationOutcome {
      Objects.requireNonNull(snapshot, "snapshot");
      Objects.requireNonNull(validation, "validation");
      if (snapshot.isPresent() != validation.valid()) {
        throw new IllegalArgumentException(
            "a snapshot is present if, and only if, validation passed");
      }
    }

    public boolean published() {
      return snapshot.isPresent();
    }
  }
}
