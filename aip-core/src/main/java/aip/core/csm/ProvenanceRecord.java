package aip.core.csm;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A structured provenance record, per the {@code Structured Provenance
 * Record} requirement: "Each CSM element's provenance SHALL be
 * recorded as a structured record containing, at minimum: the
 * provenance category ..., a stable reference to the source (the
 * originating evidence, the declaring actor or document, or the
 * inference process), and a timestamp indicating when the knowledge
 * was established."
 *
 * @param category the provenance classification; exactly one of
 *     {@link ProvenanceCategory#OBSERVED}, {@link ProvenanceCategory#DECLARED},
 *     or {@link ProvenanceCategory#INFERRED}.
 * @param sourceReference a stable, non-blank reference to the
 *     originating evidence, declaring actor/document, or inference
 *     process. This type deliberately does not constrain the
 *     reference's shape beyond "stable and non-blank" — a CSM Builder
 *     populates it with a Repository Evidence identity's string form,
 *     but this general domain model also serves declared and inferred
 *     provenance, whose sources are not evidence identities at all.
 * @param timestamp when this knowledge was established.
 * @param confidence required and present when {@code category} is
 *     {@link ProvenanceCategory#INFERRED} (see {@code Confidence for
 *     Inferred Knowledge}); optional otherwise.
 */
public record ProvenanceRecord(
    ProvenanceCategory category,
    String sourceReference,
    Instant timestamp,
    Optional<Confidence> confidence) {

  public ProvenanceRecord {
    Objects.requireNonNull(category, "category");
    Objects.requireNonNull(sourceReference, "sourceReference");
    if (sourceReference.isBlank()) {
      throw new IllegalArgumentException("sourceReference must not be blank");
    }
    Objects.requireNonNull(timestamp, "timestamp");
    Objects.requireNonNull(confidence, "confidence");
    if (category == ProvenanceCategory.INFERRED && confidence.isEmpty()) {
      throw new IllegalArgumentException(
          "an INFERRED provenance record SHALL carry a confidence level (Confidence for Inferred"
              + " Knowledge)");
    }
  }

  /** Convenience factory for an {@link ProvenanceCategory#OBSERVED} record, which never carries confidence. */
  public static ProvenanceRecord observed(String sourceReference, Instant timestamp) {
    return new ProvenanceRecord(
        ProvenanceCategory.OBSERVED, sourceReference, timestamp, Optional.empty());
  }

  /** Convenience factory for a {@link ProvenanceCategory#DECLARED} record with no confidence attached. */
  public static ProvenanceRecord declared(String sourceReference, Instant timestamp) {
    return new ProvenanceRecord(
        ProvenanceCategory.DECLARED, sourceReference, timestamp, Optional.empty());
  }

  /** Convenience factory for a {@link ProvenanceCategory#DECLARED} record sourced from an unverified external source. */
  public static ProvenanceRecord declared(
      String sourceReference, Instant timestamp, Confidence confidence) {
    return new ProvenanceRecord(
        ProvenanceCategory.DECLARED, sourceReference, timestamp, Optional.of(confidence));
  }

  /** Convenience factory for an {@link ProvenanceCategory#INFERRED} record, which always requires confidence. */
  public static ProvenanceRecord inferred(
      String sourceReference, Instant timestamp, Confidence confidence) {
    return new ProvenanceRecord(
        ProvenanceCategory.INFERRED, sourceReference, timestamp, Optional.of(confidence));
  }
}
