package aip.csmbuilder.mapper;

import aip.core.evidence.DiscoveryOutcome;
import aip.core.evidence.EvidenceAttributes;
import aip.core.evidence.EvidenceId;
import aip.core.evidence.EvidenceItem;
import aip.core.evidence.EvidenceKind;
import aip.core.evidence.ExtractionMethod;
import java.util.Map;
import java.util.Optional;

/**
 * Minimal Evidence Item construction helpers for this section's own
 * per-Mapper unit tests — small and scoped to these tests only. The
 * broader, traced fixture-scenario library is Section 21's job, not
 * this one's.
 */
final class TestEvidenceItems {

  private TestEvidenceItems() {}

  static EvidenceItem of(EvidenceKind kind, String scopeKey) {
    return of(kind, scopeKey, Map.of());
  }

  static EvidenceItem of(EvidenceKind kind, String scopeKey, Map<String, String> attributes) {
    return new EvidenceItem(
        new EvidenceId("repo-1", kind, scopeKey),
        EvidenceAttributes.of(attributes),
        Optional.empty(),
        DiscoveryOutcome.complete(),
        ExtractionMethod.MANIFEST_DECLARED);
  }
}
