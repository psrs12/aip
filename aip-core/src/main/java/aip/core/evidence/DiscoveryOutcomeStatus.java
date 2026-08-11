package aip.core.evidence;

/**
 * An Evidence Item's discovery outcome status, per
 * {@code Partial-Analysis Outcome Reporting}: "Every discovery attempt
 * against an Evidence Item SHALL record a discovery outcome status of
 * exactly one of: {@code complete}, {@code partial}, or {@code failed}."
 */
public enum DiscoveryOutcomeStatus {
  COMPLETE,
  PARTIAL,
  FAILED
}
