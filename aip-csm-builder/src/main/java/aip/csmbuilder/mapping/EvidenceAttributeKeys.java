package aip.csmbuilder.mapping;

/**
 * Well-known {@code EvidenceAttributes} keys read by Mappers
 * ({@code aip.csmbuilder.mapper}) and relationship builders in this
 * package. The archived RU specification deliberately leaves each
 * Evidence kind's exact attribute-schema keys as an implementation
 * detail (a per-kind versioned schema, not fixed by the specification
 * itself) — these constants are CSM Builder's own documented
 * convention for that schema, centralized here (rather than in
 * {@code aip.csmbuilder.mapper}) so that {@code mapping}'s own
 * relationship builders (e.g. {@link DependencyRelationshipBuilder})
 * can use them too, without creating a package dependency cycle
 * between {@code mapping} and {@code mapper}.
 */
public final class EvidenceAttributeKeys {

  private EvidenceAttributeKeys() {}

  /**
   * A {@code Package} Evidence Item's native namespace name (e.g. a
   * Java package name), distinct from its scope key — see
   * {@code Package Evidence and Containment}.
   */
  public static final String NAMESPACE_NAME = "namespaceName";

  /**
   * A {@code SourceUnit} Evidence Item's native construct kind label
   * (e.g. "class", "interface", "struct", "record") — see
   * {@code Evidence Kind Vocabulary Uses Native Terms} and
   * {@code Native Construct Kind Preserved as Attribute}.
   */
  public static final String NATIVE_CONSTRUCT_KIND = "nativeConstructKind";

  /**
   * A {@code ManifestDependencyEdge} or {@code ImportEdge} Evidence
   * Item's source Module scope key — see {@code Dependency Evidence}.
   */
  public static final String DEPENDENCY_SOURCE_MODULE = "sourceModule";

  /**
   * A {@code ManifestDependencyEdge} or {@code ImportEdge} Evidence
   * Item's dependency target scope key — either another Module's scope
   * key (an internal dependency) or an external artifact coordinate
   * (resolved to an {@code External System} in a future section) — see
   * {@code Dependency Evidence}.
   */
  public static final String DEPENDENCY_TARGET = "target";

  /**
   * A {@code ManifestDependencyEdge}'s native scope/kind string exactly
   * as declared by the build manifest (e.g. Maven's {@code compile},
   * {@code test}) — see {@code Dependency Kind Classification}. Never
   * present on an {@code ImportEdge}.
   */
  public static final String NATIVE_SCOPE = "nativeScope";

  /**
   * A {@code ManifestDependencyEdge}'s originating build system (e.g.
   * {@code maven}, {@code npm}) — selects which
   * {@code DependencyKindClassifier} mapping table applies to its
   * {@link #NATIVE_SCOPE}.
   */
  public static final String BUILD_SYSTEM = "buildSystem";

  /**
   * An {@code ApiContractDeclaration} Evidence Item's lightweight
   * structural summary (e.g. operation names, paths, verbs) — see
   * {@code Deterministic API Relationship Discovery}. Preserved
   * verbatim as the constructed {@code exposure/consumption}
   * relationship's own native attribute, per {@code API Contract
   * Relationship Construction}: "carrying the Evidence Item's
   * structural summary as an attribute."
   */
  public static final String API_STRUCTURAL_SUMMARY = "structuralSummary";
}
