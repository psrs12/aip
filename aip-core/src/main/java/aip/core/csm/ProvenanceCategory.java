package aip.core.csm;

/**
 * The Canonical Software Model's closed provenance classification, per
 * the {@code Provenance Classification of Knowledge} requirement:
 * every CSM element SHALL carry a provenance classification of exactly
 * one of observed, declared, or inferred.
 *
 * <ul>
 *   <li>{@link #OBSERVED} — mechanically derived from evidence.
 *   <li>{@link #DECLARED} — asserted by a human or an authoritative
 *       external source.
 *   <li>{@link #INFERRED} — produced by AIP's own heuristics or AI
 *       reasoning.
 * </ul>
 *
 * <p>Precedence between competing knowledge in the same subject is
 * {@code DECLARED > OBSERVED > INFERRED} (see
 * {@code Effective Knowledge and Precedence}) — that ordering is a
 * property of how this enumeration's values are used, not of the
 * enum's declaration order, and is implemented by the reconciliation
 * logic that consumes this type, not by this type itself.
 */
public enum ProvenanceCategory {
  OBSERVED,
  DECLARED,
  INFERRED
}
