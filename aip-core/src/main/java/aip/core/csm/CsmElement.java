package aip.core.csm;

/**
 * A Canonical Software Model element: one of the closed set of entity
 * kinds in {@link CsmEntityKind}.
 *
 * <p>Sealed to exactly the eight permitted implementations so that
 * exhaustive {@code switch} handling of every CSM entity kind is
 * enforced at compile time — the same closed-vocabulary discipline
 * {@link CsmEntityKind} itself expresses as an enum, expressed here at
 * the type level.
 *
 * <p>Every CSM element is {@code Knowledge}, never raw {@code Evidence}
 * (see {@code Evidence and Knowledge Distinction}): every
 * implementation therefore always carries a {@link ProvenanceRecord}.
 */
public sealed interface CsmElement
    permits
        RepositoryElement,
        ProjectElement,
        ModuleElement,
        PackageElement,
        TypeElement,
        MethodElement,
        ArchitectureComponentElement,
        ExternalSystemElement {

  CsmElementId id();

  CsmEntityKind kind();

  String name();

  ProvenanceRecord provenance();

  NativeAttributes nativeAttributes();
}
