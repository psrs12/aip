package aip.core.csm;

/**
 * The Canonical Software Model's qualitative confidence levels, per
 * the {@code Confidence for Inferred Knowledge} requirement: every CSM
 * element or relationship classified as {@link ProvenanceCategory#INFERRED}
 * SHALL carry a confidence level expressed using one of exactly three
 * qualitative levels — no other value is valid.
 *
 * <p>Elements classified as {@link ProvenanceCategory#OBSERVED} are
 * not required to carry a confidence level. {@link ProvenanceCategory#DECLARED}
 * elements MAY carry one when sourced from an unverified external
 * source.
 */
public enum Confidence {
  HIGH,
  MEDIUM,
  LOW
}
