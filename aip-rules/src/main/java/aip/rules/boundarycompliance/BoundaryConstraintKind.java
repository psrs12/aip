package aip.rules.boundarycompliance;

import aip.core.csm.CsmRelationship;

/**
 * The {@code constraint-kind} {@link aip.core.csm.NativeAttributes}
 * convention this Rule Type reads to distinguish a {@code
 * BOUNDARY_CONSTRAINT} relationship's constraint shape, per {@code
 * implement-architecture-compliance-agent/design.md} Decision 2.
 *
 * <p>{@code CsmRelationship} has no dedicated constraint-kind field
 * the way it does for a {@code DEPENDENCY} relationship's own {@code
 * dependencyKind} — this uses CSM's own designed native-evidence-
 * attribute escape hatch instead (per the {@code CSM Conceptual
 * Vocabulary} requirement), rather than modifying {@code
 * CsmRelationship} itself. A {@code BOUNDARY_CONSTRAINT} relationship
 * missing this attribute, or carrying any other value, is never
 * treated as an applicable "must not depend on" constraint (per spec
 * {@code Must-Only-Communicate-Via Constraints Are Out of Scope}).
 */
final class BoundaryConstraintKind {

  static final String ATTRIBUTE_KEY = "constraint-kind";
  static final String MUST_NOT_DEPEND_ON = "must-not-depend-on";

  private BoundaryConstraintKind() {}

  /** {@code true} iff {@code relationship} carries the "must not depend on" constraint-kind attribute. */
  static boolean isMustNotDependOn(CsmRelationship relationship) {
    return relationship.nativeAttributes().get(ATTRIBUTE_KEY).map(MUST_NOT_DEPEND_ON::equals).orElse(false);
  }
}
