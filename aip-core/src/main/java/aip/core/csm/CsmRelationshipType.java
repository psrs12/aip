package aip.core.csm;

/**
 * The Canonical Software Model's closed, versioned set of core
 * relationship types, per the {@code CSM Conceptual Vocabulary}
 * requirement: "The core relationship types SHALL include, at
 * minimum: containment, dependency, implementation/extension,
 * invocation, exposure/consumption, integration, composition, and
 * boundary/constraint relationships."
 *
 * <p>This enum is exhaustive as of the currently archived
 * specification. Introducing a new relationship type requires a
 * deliberate, versioned specification change — never a silent
 * addition here.
 */
public enum CsmRelationshipType {
  CONTAINMENT,
  DEPENDENCY,
  IMPLEMENTATION_EXTENSION,
  INVOCATION,
  EXPOSURE_CONSUMPTION,
  INTEGRATION,
  COMPOSITION,
  BOUNDARY_CONSTRAINT
}
