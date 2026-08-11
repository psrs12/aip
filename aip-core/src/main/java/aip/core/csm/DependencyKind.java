package aip.core.csm;

/**
 * The Canonical Software Model's closed dependency-kind qualifier, per
 * the {@code Dependency and Structural Relationships} requirement:
 * "Each dependency relationship SHALL be qualified by a dependency
 * kind (at minimum: compile-time, runtime, or test-only) where that
 * distinction is discoverable from evidence."
 *
 * <p>Applies only to {@link CsmRelationshipType#DEPENDENCY}
 * relationships; a dependency relationship with no discoverable kind
 * carries no {@link DependencyKind} at all rather than a guessed one
 * (see {@code Dependency Kind Classification} in
 * {@code openspec/specs/csm-builder/spec.md}).
 */
public enum DependencyKind {
  COMPILE_TIME,
  RUNTIME,
  TEST_ONLY
}
