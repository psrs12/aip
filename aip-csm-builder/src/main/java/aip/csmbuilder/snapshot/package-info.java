/**
 * Snapshot persistence: the {@link aip.csmbuilder.snapshot.SnapshotStore}
 * abstraction and its filesystem implementation, per {@code
 * Snapshot-Based CSM Construction} and {@code
 * openspec/changes/implement-csm-builder/design.md} Decision 2 (tasks.md
 * Section 15).
 *
 * <p>This package depends only on {@code aip.core.csm} and {@code
 * aip.core.evidence} — the same boundary every other CSM Builder
 * package holds to (invariants 1, 2). Nothing in {@code
 * aip.csmbuilder.mapping} depends on this package: constructing CSM
 * content and persisting it are deliberately separate concerns, joined
 * only by whatever calls both (a future CLI/application entry point).
 */
package aip.csmbuilder.snapshot;
