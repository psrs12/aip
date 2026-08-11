package aip.csmbuilder.mapping;

import aip.core.evidence.EvidenceItem;
import aip.core.evidence.EvidenceKind;

/**
 * The contract every CSM Builder Mapper implements: given one
 * {@link EvidenceItem} of the kind it supports, produce zero or more
 * CSM elements/relationships (see {@link MappingResult}).
 *
 * <p>An {@code EvidenceKindMapper} implementation SHALL depend only on
 * {@code aip.core.evidence} (input) and {@code aip.core.csm} (output)
 * types — never on AI/LLM reasoning, heuristic inference, or
 * probabilistic judgment (see {@code Deterministic, Evidence-Driven
 * Transformation Only} in {@code openspec/specs/csm-builder/spec.md}).
 * Given the same {@link EvidenceItem} and {@link MappingContext}, a
 * call to {@link #map} SHALL always produce the same result.
 */
public interface EvidenceKindMapper {

  /** The single {@link EvidenceKind} this Mapper handles. */
  EvidenceKind supportedKind();

  /**
   * This Mapper's own monotonically increasing version, per
   * {@code CSM Builder Mapper Versioning} — incremented whenever this
   * Mapper's own transformation logic changes, independent of any
   * Repository Understanding analyzer version. Used to make a
   * previously constructed CSM element eligible for re-derivation even
   * when its originating evidence is unchanged (implemented in
   * Section 17, not by this interface itself).
   */
  int mapperVersion();

  /**
   * Transforms one Evidence Item of {@link #supportedKind()} into CSM
   * content. {@code item.kind()} SHALL equal {@link #supportedKind()}
   * — the caller (see {@link MappingOrchestrator}) is responsible for
   * that dispatch invariant; this method does not re-check it.
   */
  MappingResult map(EvidenceItem item, MappingContext context);
}
