package aip.core.evidence;

/**
 * The shared, versioned extraction-method taxonomy, per
 * {@code Extraction Method Tagging}: "describes how the evidence was
 * obtained and SHALL NOT be used, or usable, as a substitute for CSM
 * confidence." Every Evidence Item produced from source or
 * build-system analysis carries exactly one of these.
 */
public enum ExtractionMethod {
  FULL_PARSE,
  HEURISTIC_SCAN,
  MANIFEST_DECLARED,
  EXTERNALLY_DECLARED
}
