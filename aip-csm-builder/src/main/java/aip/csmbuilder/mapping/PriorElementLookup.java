package aip.csmbuilder.mapping;

import aip.core.csm.CsmElement;
import aip.core.evidence.EvidenceId;
import java.util.Optional;

/**
 * A lookup from an Evidence Item's identity to the CSM element a prior
 * construction run produced from it, if any — the one fact {@code
 * Incremental Snapshot Scope} needs about a prior snapshot in order to
 * carry an {@code UNCHANGED} Evidence Item's element forward "without
 * re-invoking their Mapper" (tasks.md 16.1), expressed as the smallest
 * possible interface rather than a dependency on {@code
 * aip.csmbuilder.snapshot} — that package's {@code
 * package-info.java} deliberately states this package never depends on
 * it. A future CLI/application entry point (or, within {@code
 * aip.csmbuilder.snapshot} itself) bridges a real persisted {@code
 * Snapshot} into this interface; {@link MappingOrchestrator}'s own
 * tests supply a trivial in-memory implementation directly.
 */
public interface PriorElementLookup {

  Optional<CsmElement> find(EvidenceId originatingEvidenceId);

  /** No prior snapshot exists — every lookup is empty. Used for ordinary, non-incremental construction. */
  static PriorElementLookup none() {
    return evidenceId -> Optional.empty();
  }
}
