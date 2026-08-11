package aip.csmbuilder.provenance;

import aip.core.csm.ProvenanceRecord;
import aip.core.evidence.EvidenceId;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Constructs the {@link ProvenanceRecord} every Mapper attaches to
 * every CSM element or relationship it produces, per {@code Provenance
 * Assignment for Constructed Knowledge}: category always
 * {@code observed}, source reference identifying the originating
 * Repository Evidence Item(s) by their evidence identity, timestamp
 * reflecting when CSM Builder constructed or last re-derived it.
 *
 * <p>This is the only place in CSM Builder that constructs a
 * {@link ProvenanceRecord}. Because it only ever calls
 * {@link ProvenanceRecord#observed}, there is no code path here through
 * which a confidence level could be attached — the {@code observed}
 * factory method's signature admits none. See {@link ProvenanceGuard}
 * for the additional, defense-in-depth runtime check applied to every
 * element/relationship a {@code MappingOrchestrator} run produces.
 */
public final class ObservedProvenanceFactory {

  private ObservedProvenanceFactory() {}

  /** Provenance sourced from a single originating Evidence Item. */
  public static ProvenanceRecord fromEvidence(EvidenceId evidenceId, Instant timestamp) {
    Objects.requireNonNull(evidenceId, "evidenceId");
    Objects.requireNonNull(timestamp, "timestamp");
    return ProvenanceRecord.observed(evidenceId.toString(), timestamp);
  }

  /**
   * Provenance sourced from more than one originating Evidence Item
   * (e.g. a relationship corroborated by more than one Evidence kind).
   * Evidence identities are sorted before joining so the resulting
   * source reference is itself deterministic regardless of input
   * ordering.
   */
  public static ProvenanceRecord fromEvidence(List<EvidenceId> evidenceIds, Instant timestamp) {
    Objects.requireNonNull(evidenceIds, "evidenceIds");
    Objects.requireNonNull(timestamp, "timestamp");
    if (evidenceIds.isEmpty()) {
      throw new IllegalArgumentException("at least one originating Evidence identity is required");
    }
    String sourceReference =
        evidenceIds.stream().map(EvidenceId::toString).sorted().collect(Collectors.joining(","));
    return ProvenanceRecord.observed(sourceReference, timestamp);
  }
}
