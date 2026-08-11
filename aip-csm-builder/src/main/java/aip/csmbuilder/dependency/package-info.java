/**
 * Deterministic, versioned, per-build-system native dependency-scope
 * classification, per {@code Dependency Kind Classification} and
 * {@code openspec/changes/implement-csm-builder/design.md} Decision 3.
 *
 * <p>Every value {@link DependencyKindClassifier} can return is one of
 * {@code aip.core.csm.DependencyKind}'s already-approved CSM values —
 * this package introduces no new CSM vocabulary, only a mechanism for
 * mapping build-system-native strings onto it.
 */
package aip.csmbuilder.dependency;
