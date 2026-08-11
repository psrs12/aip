package aip.core.csm;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Computes {@link EffectiveKnowledgeStatus} per {@link Subject}, per
 * {@code Same-Category Conflict Marking and Resolution}: "When multiple
 * knowledge assertions apply to the same subject, share the same
 * precedence category ..., and conflict with one another, the CSM
 * SHALL NOT arbitrarily select one assertion as effective... The
 * effective knowledge for that subject SHALL be marked as CONFLICTED."
 *
 * <p>The caller supplies only assertions that already share one
 * provenance category (e.g. all {@code observed}) — this type does not
 * itself inspect provenance, since doing so would require the general
 * {@code DECLARED > OBSERVED > INFERRED} precedence computation this
 * change deliberately does not build (see {@link EffectiveKnowledgeStatus}
 * and {@code design.md} Decision 10). {@link #classify} never discards
 * an assertion — every input value is preserved in the returned index,
 * satisfying {@code Non-Destructive Preservation of Competing Knowledge}
 * by construction rather than by a separate check.
 */
public final class SubjectConflictMarker {

  private SubjectConflictMarker() {}

  /**
   * Groups {@code sameCategoryAssertions} by the {@link Subject} {@code
   * subjectOf} derives for each, and classifies every resulting group:
   * {@link EffectiveKnowledgeStatus#CONFLICTED} when more than one
   * assertion shares a subject, {@link EffectiveKnowledgeStatus#EFFECTIVE}
   * when exactly one does.
   */
  public static <T> Map<Subject, Classification<T>> classify(
      List<T> sameCategoryAssertions, Function<T, Subject> subjectOf) {
    Objects.requireNonNull(sameCategoryAssertions, "sameCategoryAssertions");
    Objects.requireNonNull(subjectOf, "subjectOf");

    Map<Subject, List<T>> bySubject =
        sameCategoryAssertions.stream()
            .collect(Collectors.groupingBy(subjectOf, LinkedHashMap::new, Collectors.toList()));

    Map<Subject, Classification<T>> result = new LinkedHashMap<>();
    for (Map.Entry<Subject, List<T>> entry : bySubject.entrySet()) {
      List<T> assertions = List.copyOf(entry.getValue());
      EffectiveKnowledgeStatus status =
          assertions.size() > 1 ? EffectiveKnowledgeStatus.CONFLICTED : EffectiveKnowledgeStatus.EFFECTIVE;
      result.put(entry.getKey(), new Classification<>(status, assertions));
    }
    return result;
  }

  /** One {@link Subject}'s reconciliation outcome, with every competing assertion preserved. */
  public record Classification<T>(EffectiveKnowledgeStatus status, List<T> assertions) {

    public Classification {
      Objects.requireNonNull(status, "status");
      Objects.requireNonNull(assertions, "assertions");
      assertions = List.copyOf(assertions);
      if (assertions.isEmpty()) {
        throw new IllegalArgumentException("assertions must not be empty");
      }
      if (status == EffectiveKnowledgeStatus.CONFLICTED && assertions.size() < 2) {
        throw new IllegalArgumentException("CONFLICTED requires at least two competing assertions");
      }
      if (status == EffectiveKnowledgeStatus.EFFECTIVE && assertions.size() != 1) {
        throw new IllegalArgumentException(
            "EFFECTIVE with more than one assertion requires cross-category precedence, which this"
                + " type does not compute (see EffectiveKnowledgeStatus)");
      }
    }
  }
}
