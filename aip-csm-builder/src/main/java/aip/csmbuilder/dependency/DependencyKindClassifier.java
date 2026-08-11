package aip.csmbuilder.dependency;

import aip.core.csm.DependencyKind;
import java.util.Optional;

/**
 * Classifies a build system's native dependency scope string into
 * CSM's {@link DependencyKind} vocabulary, per {@code Dependency Kind
 * Classification}: "Where a {@code ManifestDependencyEdge}'s native
 * scope string maps to CSM's compile-time, runtime, or test-only
 * dependency-kind vocabulary via a versioned, build-system-specific
 * mapping, CSM Builder SHALL qualify the constructed {@code dependency}
 * relationship with that kind. Where the native scope string does not
 * map ..., CSM Builder SHALL construct the {@code dependency}
 * relationship without a kind qualifier, rather than guessing a kind."
 *
 * <p>Deliberately an abstraction decoupled from any specific
 * mapping-table implementation (invariant 6;
 * {@code openspec/changes/implement-csm-builder/design.md} Decision 3)
 * — {@link ResourceDependencyKindClassifier} is the default
 * implementation, but nothing in {@code aip.csmbuilder.mapping} depends
 * on that concrete class.
 */
public interface DependencyKindClassifier {

  /**
   * Classifies {@code nativeScope} for {@code buildSystem}. Empty when
   * either the build system is unrecognized or the build system's own
   * table has no entry for that scope string — both are legitimate,
   * honest outcomes, never a guess.
   */
  Optional<DependencyKind> classify(String buildSystem, String nativeScope);
}
