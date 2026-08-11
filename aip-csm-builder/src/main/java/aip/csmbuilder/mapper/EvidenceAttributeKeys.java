package aip.csmbuilder.mapper;

/**
 * Well-known {@code EvidenceAttributes} keys Mapper implementations
 * read from. The archived RU specification deliberately leaves each
 * Evidence kind's exact attribute-schema keys as an implementation
 * detail (a per-kind versioned schema, not fixed by the specification
 * itself) — these constants are CSM Builder's own documented
 * convention for that schema, centralized here so every Mapper agrees
 * on the same key names rather than each hand-rolling string literals.
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
}
