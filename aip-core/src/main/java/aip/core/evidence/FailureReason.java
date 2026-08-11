package aip.core.evidence;

/**
 * The shared, versioned failure-reason taxonomy, per
 * {@code Failure Reason Taxonomy}: accompanies a {@code partial} or
 * {@code failed} {@link DiscoveryOutcomeStatus}. "Core but
 * non-exhaustive: additional failure reasons MAY be introduced in
 * future versioned specification changes without invalidating
 * existing Evidence" — extending this taxonomy is therefore a
 * deliberate, versioned change to both the specification and this
 * enum together, not a runtime-open set.
 */
public enum FailureReason {
  PARSE_ERROR,
  UNSUPPORTED_CONSTRUCT,
  UNSUPPORTED_LANGUAGE,
  SIZE_LIMIT_EXCEEDED,
  TIMEOUT,
  BINARY_OR_NON_TEXT,
  EXCLUDED_BY_CONFIGURATION,
  CONFLICTING_OVERRIDE
}
