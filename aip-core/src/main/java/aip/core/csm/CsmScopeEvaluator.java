package aip.core.csm;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * {@link CsmScope} instance enumeration and applicability — the
 * general mechanism both Analysis Scope and Rule Scope are defined in
 * terms of, per {@code Analysis Scope Declaration}/{@code Rule Scope
 * Declaration}, {@code Kind-Based Analyzer Applicability}/{@code
 * Kind-Based Rule Applicability}, and {@code Native-Attribute
 * Applicability Refinement}/{@code Native-Attribute Rule Applicability
 * Refinement} — all four requirement pairs worded identically across
 * the two specifications.
 *
 * <p>Promoted to {@code aip-core}, per {@code
 * implement-rule-framework/design.md}'s own extension of the reasoning
 * `CsmScope` and `AnalysisResult` were already promoted under: this is
 * one general mechanism two sibling modules (`aip-analysis`,
 * `aip-rules`) each need identically, with the second consumer already
 * concretely present, not speculative — originally implemented as
 * `aip-analysis`'s own {@code AnalysisScopeEvaluator}, moved here
 * rather than duplicated once Rule Framework's implementation needed
 * the identical logic, avoiding exactly the "second, subtly
 * incompatible mechanism" risk this project has repeatedly avoided
 * elsewhere.
 */
public final class CsmScopeEvaluator {

  private CsmScopeEvaluator() {}

  /**
   * Every Scope instance {@code scope} is invoked against: a single
   * {@link CsmScopeInstance#wholeRepository()} when unanchored, or one
   * instance per element of the anchor kind present in {@code view}
   * when anchored.
   */
  public static List<CsmScopeInstance> enumerateInstances(AnalysisView view, CsmScope scope) {
    Objects.requireNonNull(view, "view");
    Objects.requireNonNull(scope, "scope");

    if (scope.containmentAnchor().isEmpty()) {
      return List.of(CsmScopeInstance.wholeRepository());
    }
    return view.elementsOfKind(scope.containmentAnchor().get()).stream()
        .map(CsmElement::id)
        .map(CsmScopeInstance::anchoredAt)
        .toList();
  }

  /**
   * Whether {@code scope} is applicable to {@code instance}: the
   * corresponding CSM content contains at least one element or
   * relationship of a declared kind, and, when a native-attribute
   * predicate is declared, at least one element or relationship also
   * satisfies it.
   */
  public static boolean isApplicable(AnalysisView view, CsmScope scope, CsmScopeInstance instance) {
    Objects.requireNonNull(view, "view");
    Objects.requireNonNull(scope, "scope");
    Objects.requireNonNull(instance, "instance");

    Set<CsmElement> elements = contentElements(view, instance);
    Set<CsmRelationship> relationships = contentRelationships(view, instance);

    boolean kindPresent =
        elements.stream().anyMatch(element -> scope.declaresEntityKind(element.kind()))
            || relationships.stream().anyMatch(relationship -> scope.declaresRelationshipType(relationship.type()));
    if (!kindPresent) {
      return false;
    }

    if (scope.nativeAttributePredicate().isEmpty()) {
      return true;
    }
    Predicate<NativeAttributes> predicate = scope.nativeAttributePredicate().get();
    return elements.stream().anyMatch(element -> predicate.test(element.nativeAttributes()))
        || relationships.stream().anyMatch(relationship -> predicate.test(relationship.nativeAttributes()));
  }

  /** The CSM elements within {@code instance}'s own scope — the whole view when unanchored, else {@code instance}'s containment closure. */
  public static Set<CsmElement> contentElements(AnalysisView view, CsmScopeInstance instance) {
    Objects.requireNonNull(view, "view");
    Objects.requireNonNull(instance, "instance");
    if (instance.isAnchored()) {
      return ContainmentClosure.elementsWithin(view, instance.anchorElementId().orElseThrow());
    }
    return view.elements();
  }

  /** The CSM relationships within {@code instance}'s own scope — either endpoint within {@link #contentElements}. */
  public static Set<CsmRelationship> contentRelationships(AnalysisView view, CsmScopeInstance instance) {
    Objects.requireNonNull(view, "view");
    Objects.requireNonNull(instance, "instance");
    if (!instance.isAnchored()) {
      return view.relationships();
    }
    Set<CsmElementId> ids = new LinkedHashSet<>();
    for (CsmElement element : contentElements(view, instance)) {
      ids.add(element.id());
    }
    Set<CsmRelationship> result = new LinkedHashSet<>();
    for (CsmRelationship relationship : view.relationships()) {
      boolean sourceWithin = ids.contains(relationship.sourceId());
      boolean targetWithin = relationship.targetId().map(ids::contains).orElse(false);
      if (sourceWithin || targetWithin) {
        result.add(relationship);
      }
    }
    return result;
  }
}
