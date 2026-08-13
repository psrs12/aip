package aip.analysis;

import aip.core.csm.AnalysisView;
import aip.core.csm.CsmElement;
import aip.core.csm.CsmElementId;
import aip.core.csm.CsmRelationship;
import aip.core.csm.CsmRelationshipType;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Computes the set of CSM elements transitively contained within a
 * given anchor element, following {@code CONTAINMENT} relationships
 * (source = containing entity, target = contained entity, per {@code
 * aip-csm-builder}'s own {@code ContainmentRelationshipBuilder}) —
 * used by {@link AnalysisScopeEvaluator} to determine what "this
 * anchored element's own Analysis Scope instance" actually contains,
 * per {@code Anchored Analyzer is invoked once per matching contained
 * element, each invocation covering that element's own Analysis Scope
 * instance}.
 */
final class ContainmentClosure {

  private ContainmentClosure() {}

  /** {@code anchorId} itself, plus every element transitively reachable from it via {@code CONTAINMENT}. */
  static Set<CsmElement> elementsWithin(AnalysisView view, CsmElementId anchorId) {
    Objects.requireNonNull(view, "view");
    Objects.requireNonNull(anchorId, "anchorId");

    Set<CsmElementId> visited = new LinkedHashSet<>();
    Deque<CsmElementId> queue = new ArrayDeque<>();
    visited.add(anchorId);
    queue.add(anchorId);

    Set<CsmRelationship> containment = view.relationshipsOfType(CsmRelationshipType.CONTAINMENT);
    while (!queue.isEmpty()) {
      CsmElementId current = queue.poll();
      for (CsmRelationship relationship : containment) {
        if (relationship.sourceId().equals(current) && relationship.targetId().isPresent()) {
          CsmElementId child = relationship.targetId().get();
          if (visited.add(child)) {
            queue.add(child);
          }
        }
      }
    }

    Set<CsmElement> result = new LinkedHashSet<>();
    for (CsmElementId id : visited) {
      view.element(id).ifPresent(result::add);
    }
    return result;
  }
}
