package aip.csmbuilder.mapping;

import aip.core.csm.CsmElement;
import aip.core.evidence.EvidenceId;
import java.util.Objects;
import java.util.Optional;

/**
 * A lookup from an Evidence Item's identity to the CSM element a prior
 * construction run produced from it, and the version of the Mapper
 * that produced it — the facts {@code Incremental Snapshot Scope} and
 * {@code CSM Builder Mapper Versioning} need about a prior snapshot in
 * order to carry an {@code UNCHANGED} Evidence Item's element forward
 * "without re-invoking their Mapper" (tasks.md 16.1), except when that
 * Mapper's version has since changed (tasks.md 17.2). Expressed as the
 * smallest possible interface rather than a dependency on {@code
 * aip.csmbuilder.snapshot} — that package's {@code package-info.java}
 * deliberately states this package never depends on it. A future
 * CLI/application entry point (or, within {@code aip.csmbuilder.snapshot}
 * itself) bridges a real persisted {@code Snapshot} into this
 * interface; {@link MappingOrchestrator}'s own tests supply a trivial
 * in-memory implementation directly.
 */
public interface PriorElementLookup {

  Optional<PriorElement> find(EvidenceId originatingEvidenceId);

  /** No prior snapshot exists — every lookup is empty. Used for ordinary, non-incremental construction. */
  static PriorElementLookup none() {
    return evidenceId -> Optional.empty();
  }

  /**
   * One prior construction run's result for a given Evidence identity:
   * the CSM element it produced, and the version of the Mapper that
   * produced it (see {@link aip.csmbuilder.snapshot.MapperAttribution}
   * — the same fact, named identically, that a real persisted snapshot
   * would supply via its Manifest).
   */
  record PriorElement(CsmElement element, int mapperVersion) {

    public PriorElement {
      Objects.requireNonNull(element, "element");
      if (mapperVersion < 1) {
        throw new IllegalArgumentException("mapperVersion must be at least 1");
      }
    }
  }
}
