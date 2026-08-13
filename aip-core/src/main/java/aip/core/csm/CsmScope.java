package aip.core.csm;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A declared Scope: a non-empty set of CSM entity kinds and/or CSM
 * relationship types, with an optional containment-level anchor and an
 * optional native-attribute predicate refinement — per {@code Analysis
 * Scope Declaration}: "a non-empty set of CSM entity kinds and/or CSM
 * relationship types the Analyzer reads... An Analyzer MAY additionally
 * declare a containment-level anchor narrowing its invocation to
 * specific contained elements... rather than the whole repository."
 *
 * <p>This is the one general Scope declaration type both Analysis
 * Scope and (per {@code define-rule-framework/design.md} Decision 3,
 * a future consumer already named) Rule Scope are defined in terms of
 * — hosted here in {@code aip-core}, per {@code
 * implement-analysis-framework/design.md} Decision 2, rather than
 * duplicated by each consumer independently. Analysis Scope <em>is</em>
 * this type, verbatim, with no {@code aip-analysis}-local wrapper.
 *
 * <p>A {@code CsmScope} describes what an Analyzer (or, later, a Rule
 * Type) declares it reads — a static, registration-time property. The
 * specific unit an Analyzer is invoked against at analysis time is a
 * separate concept, {@link CsmScopeInstance}.
 *
 * @param entityKinds the CSM entity kinds this Scope reads. May be
 *     empty only if {@code relationshipTypes} is non-empty.
 * @param relationshipTypes the CSM relationship types this Scope
 *     reads. May be empty only if {@code entityKinds} is non-empty.
 * @param containmentAnchor when present, narrows invocation to once
 *     per contained element of this kind present in the CSM Snapshot,
 *     per {@code Anchored Analyzer is invoked once per matching
 *     contained element}. Absent means unanchored — invoked once for
 *     the whole repository.
 * @param nativeAttributePredicate an optional refinement over
 *     native-evidence-attribute content already present within this
 *     Scope's kind-based content, per {@code Native-Attribute
 *     Applicability Refinement}. Deliberately not part of this
 *     record's {@code equals}/{@code hashCode} identity in any
 *     meaningful sense — a predicate is configuration, not something
 *     compared for equality; callers needing a stable, hashable
 *     representation of "what was actually evaluated" use {@link
 *     CsmScopeInstance}, not this declaration, for that purpose.
 */
public record CsmScope(
    Set<CsmEntityKind> entityKinds,
    Set<CsmRelationshipType> relationshipTypes,
    Optional<CsmEntityKind> containmentAnchor,
    Optional<Predicate<NativeAttributes>> nativeAttributePredicate) {

  public CsmScope {
    Objects.requireNonNull(entityKinds, "entityKinds");
    Objects.requireNonNull(relationshipTypes, "relationshipTypes");
    Objects.requireNonNull(containmentAnchor, "containmentAnchor");
    Objects.requireNonNull(nativeAttributePredicate, "nativeAttributePredicate");
    entityKinds = Set.copyOf(entityKinds);
    relationshipTypes = Set.copyOf(relationshipTypes);
    if (entityKinds.isEmpty() && relationshipTypes.isEmpty()) {
      throw new IllegalArgumentException(
          "a CsmScope SHALL identify at least one CSM entity kind or CSM relationship type (see"
              + " 'Analyzer declares a kind-based scope')");
    }
  }

  /** An unanchored Scope declaring the given kinds, with no native-attribute refinement. */
  public static CsmScope of(Set<CsmEntityKind> entityKinds, Set<CsmRelationshipType> relationshipTypes) {
    return new CsmScope(entityKinds, relationshipTypes, Optional.empty(), Optional.empty());
  }

  /** An unanchored Scope declaring only entity kinds. */
  public static CsmScope ofEntityKinds(Set<CsmEntityKind> entityKinds) {
    return of(entityKinds, Set.of());
  }

  /** An unanchored Scope declaring only relationship types. */
  public static CsmScope ofRelationshipTypes(Set<CsmRelationshipType> relationshipTypes) {
    return of(Set.of(), relationshipTypes);
  }

  /** This Scope, narrowed to once-per-{@code anchor} invocation. */
  public CsmScope anchoredAt(CsmEntityKind anchor) {
    Objects.requireNonNull(anchor, "anchor");
    return new CsmScope(entityKinds, relationshipTypes, Optional.of(anchor), nativeAttributePredicate);
  }

  /** This Scope, further refined by a native-attribute predicate. */
  public CsmScope withNativeAttributePredicate(Predicate<NativeAttributes> predicate) {
    Objects.requireNonNull(predicate, "predicate");
    return new CsmScope(entityKinds, relationshipTypes, containmentAnchor, Optional.of(predicate));
  }

  /** Whether {@code kind} is among this Scope's declared entity kinds. */
  public boolean declaresEntityKind(CsmEntityKind kind) {
    return entityKinds.contains(kind);
  }

  /** Whether {@code type} is among this Scope's declared relationship types. */
  public boolean declaresRelationshipType(CsmRelationshipType type) {
    return relationshipTypes.contains(type);
  }
}
