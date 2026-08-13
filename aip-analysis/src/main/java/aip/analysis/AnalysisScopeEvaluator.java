package aip.analysis;

import aip.core.csm.AnalysisView;
import aip.core.csm.CsmElement;
import aip.core.csm.CsmElementId;
import aip.core.csm.CsmRelationship;
import aip.core.csm.CsmScope;
import aip.core.csm.CsmScopeInstance;
import aip.core.csm.NativeAttributes;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Analysis Scope instance enumeration and applicability, per {@code
 * Analysis Scope Declaration}, {@code Kind-Based Analyzer
 * Applicability}, and {@code Native-Attribute Applicability
 * Refinement}.
 */
public final class AnalysisScopeEvaluator {

  private AnalysisScopeEvaluator() {}

  /**
   * Every Scope instance {@code scope} is invoked against, per {@code
   * Unanchored Analyzer is invoked once per repository} (a single
   * {@link CsmScopeInstance#wholeRepository()}) and {@code Anchored
   * Analyzer is invoked once per matching contained element} (one
   * instance per element of the anchor kind present in {@code view}).
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
   * Whether {@code scope} is applicable to {@code instance}, per
   * {@code Kind-Based Analyzer Applicability} ("the corresponding CSM
   * content contains at least one element or relationship of a kind
   * within that Analyzer's declared Analysis Scope") and, when
   * declared, {@code Native-Attribute Applicability Refinement}.
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
