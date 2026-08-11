package aip.core.evidence;

import java.util.Objects;
import java.util.Optional;

/**
 * A single Repository Evidence Item, per the {@code Repository
 * Evidence Model Shape} requirement: "typed Evidence Items, each
 * carrying: a stable identity, an Evidence kind, ... kind-specific
 * attributes ..., a current source location (where applicable), a
 * discovery outcome status, an extraction-method tag."
 *
 * <p>This type intentionally does not carry a {@link ChangeStatus} or
 * {@link LifecycleState} — both are per-discovery-run classifications
 * of an Evidence Item's identity over time, not a static property of
 * one snapshot instance (see {@link ClassifiedEvidenceItem} for the
 * pairing).
 *
 * @param currentLocation present when a physical file location applies
 *     to this kind of evidence ("where applicable" — e.g. absent for
 *     evidence with no single originating file).
 */
public record EvidenceItem(
    EvidenceId id,
    EvidenceAttributes attributes,
    Optional<EvidenceLocation> currentLocation,
    DiscoveryOutcome discoveryOutcome,
    ExtractionMethod extractionMethod) {

  public EvidenceItem {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(attributes, "attributes");
    Objects.requireNonNull(currentLocation, "currentLocation");
    Objects.requireNonNull(discoveryOutcome, "discoveryOutcome");
    Objects.requireNonNull(extractionMethod, "extractionMethod");
  }

  /** This item's Evidence kind — a convenience accessor onto {@link #id()}. */
  public EvidenceKind kind() {
    return id.kind();
  }
}
