package aip.csmbuilder.snapshot;

import aip.core.csm.CsmElementId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A CSM snapshot's Snapshot Manifest: an index of {@link
 * SnapshotManifestEntry}, one per constructed CSM element, keyed by
 * that element's identity, per {@code design.md} Decision 2.
 */
public final class SnapshotManifest {

  private final Map<CsmElementId, SnapshotManifestEntry> byElementId;

  public SnapshotManifest(List<SnapshotManifestEntry> entries) {
    Objects.requireNonNull(entries, "entries");
    Map<CsmElementId, SnapshotManifestEntry> byId = new LinkedHashMap<>();
    for (SnapshotManifestEntry entry : entries) {
      if (byId.put(entry.elementId(), entry) != null) {
        throw new IllegalArgumentException(
            "duplicate Snapshot Manifest entry for element " + entry.elementId());
      }
    }
    this.byElementId = Map.copyOf(byId);
  }

  public List<SnapshotManifestEntry> entries() {
    return List.copyOf(byElementId.values());
  }

  public Optional<SnapshotManifestEntry> find(CsmElementId elementId) {
    Objects.requireNonNull(elementId, "elementId");
    return Optional.ofNullable(byElementId.get(elementId));
  }

  public int size() {
    return byElementId.size();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof SnapshotManifest other && byElementId.equals(other.byElementId);
  }

  @Override
  public int hashCode() {
    return byElementId.hashCode();
  }

  @Override
  public String toString() {
    return "SnapshotManifest" + byElementId.values();
  }
}
