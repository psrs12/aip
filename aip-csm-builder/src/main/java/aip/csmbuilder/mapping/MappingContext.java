package aip.csmbuilder.mapping;

import aip.core.csm.CsmElementId;
import aip.core.evidence.EvidenceId;
import aip.core.evidence.RepositoryEvidenceModel;
import java.time.Instant;
import java.util.Optional;

/**
 * The context a {@link MappingOrchestrator} passes to every
 * {@link EvidenceKindMapper#map} call: the full Repository Evidence
 * Model being processed (so a Mapper can traverse relationships to
 * other Evidence Items, e.g. to find a source construct's containing
 * Module), a read-only, point-in-time view of which Evidence Items
 * have already produced a CSM element earlier in this orchestration
 * run (see {@link MappingOrchestrator} for the deterministic
 * processing order this view depends on), and the single timestamp
 * every element/relationship produced in this run shares (see
 * {@code Provenance Assignment for Constructed Knowledge}: "a
 * timestamp reflecting when CSM Builder constructed or last re-derived
 * it").
 */
public interface MappingContext {

  RepositoryEvidenceModel evidenceModel();

  /**
   * The CSM element identity already produced for {@code evidenceId},
   * if that Evidence Item has already been processed earlier in this
   * orchestration run. Empty if {@code evidenceId} has not yet been
   * processed, has no registered Mapper, or its Mapper produced no
   * primary element (see {@link MappingResult}).
   */
  Optional<CsmElementId> resolvedElementId(EvidenceId evidenceId);

  /**
   * The single timestamp shared by every element and relationship
   * constructed in this orchestration run — captured once, at the
   * start of the run, not re-read per Mapper call.
   */
  Instant constructionTimestamp();
}
