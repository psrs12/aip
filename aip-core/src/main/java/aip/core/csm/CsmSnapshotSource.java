package aip.core.csm;

import java.util.Set;

/**
 * A read-only view over a constructed CSM Snapshot's content, per
 * {@code CSM Snapshot Source Shape}: "A CSM Snapshot Source SHALL
 * expose, at minimum: the CSM elements and relationships constructed
 * for a repository as of a specific construction run, and a stable
 * identity for that snapshot... The Analysis Framework SHALL depend
 * only on this shape, independent of which capability produced or how
 * it persisted the underlying snapshot."
 *
 * <p>Deliberately exposes only already-{@code aip-core}-native content
 * ({@link CsmElement}, {@link CsmRelationship}, {@link CsmSnapshotId})
 * rather than a producer-owned wrapper type — {@code aip-csm-builder}'s
 * own richer {@code Snapshot} type (construction timestamp, manifest)
 * is never exposed here, and no consumer of this interface ever needs
 * to depend on {@code aip-csm-builder} to use it. A conforming
 * implementation may be a hand-built test fixture or a real adapter
 * over a producer's own snapshot type — per {@code Analysis Framework
 * is agnostic to snapshot production}, both analyze identically.
 */
public interface CsmSnapshotSource {

  /** This snapshot's CSM elements. */
  Set<CsmElement> elements();

  /** This snapshot's CSM relationships. */
  Set<CsmRelationship> relationships();

  /** This snapshot's own stable identity. */
  CsmSnapshotId id();
}
