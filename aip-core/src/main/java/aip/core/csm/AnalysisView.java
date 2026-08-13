package aip.core.csm;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A per-Subject effective-knowledge-plus-conflict-status projection of
 * a {@link CsmSnapshotSource}, per {@code Analysis View Construction}:
 * "for every Subject... present in the snapshot's content, that
 * Subject's effective knowledge... together with its conflict status
 * (EFFECTIVE or CONFLICTED)... An Analyzer SHALL be able to complete
 * its analysis using only Analysis View content, without direct access
 * to the CSM Snapshot Source."
 *
 * <p>Also exposes its source CSM Snapshot's own stable identity ({@code
 * Analysis View Construction}, as amended while specifying Rule
 * Framework), so a consumer holding only an {@code AnalysisView} can
 * still determine which CSM Snapshot it was derived from — Rule
 * Framework's own {@code AnalysisResultId}/{@code
 * RuleEvaluationResultId} both rely on obtaining that identity this
 * way, never through a direct {@link CsmSnapshotSource} dependency.
 *
 * <p>A read-oriented projection only, per {@code Analysis View Is a
 * Derived Projection, Not a New Canonical Model}: introduces no new
 * entity kind, relationship type, or persisted state, and is never
 * itself treated as a durable artifact — {@link #from} is always
 * re-derivable from the same source content, and nothing in this
 * package or {@code aip-analysis} ever writes an {@code AnalysisView}
 * to a store.
 */
public final class AnalysisView {

  private final CsmSnapshotId sourceSnapshotId;
  private final Map<Subject, SubjectConflictMarker.Classification<CsmElement>> elementClassifications;
  private final Map<Subject, SubjectConflictMarker.Classification<CsmRelationship>> relationshipClassifications;

  private AnalysisView(
      CsmSnapshotId sourceSnapshotId,
      Map<Subject, SubjectConflictMarker.Classification<CsmElement>> elementClassifications,
      Map<Subject, SubjectConflictMarker.Classification<CsmRelationship>> relationshipClassifications) {
    this.sourceSnapshotId = sourceSnapshotId;
    this.elementClassifications = elementClassifications;
    this.relationshipClassifications = relationshipClassifications;
  }

  /**
   * Constructs an {@code AnalysisView} from {@code source}'s current
   * content, classifying every element by {@link
   * Subject#forElementIdentity} and every relationship by {@link
   * Subject#forRelationship}, via the CSM's own {@link
   * SubjectConflictMarker}. Two constructions from equivalent source
   * content always produce equivalent views (per {@code Analysis View
   * content is fully re-derivable from its source snapshot}).
   */
  public static AnalysisView from(CsmSnapshotSource source) {
    Objects.requireNonNull(source, "source");

    Map<Subject, SubjectConflictMarker.Classification<CsmElement>> elements =
        SubjectConflictMarker.classify(
            java.util.List.copyOf(source.elements()), element -> Subject.forElementIdentity(element.id()));

    Map<Subject, SubjectConflictMarker.Classification<CsmRelationship>> relationships =
        SubjectConflictMarker.classify(
            java.util.List.copyOf(source.relationships()),
            relationship ->
                Subject.forRelationship(relationship.sourceId(), relationship.type(), relationship.targetId()));

    return new AnalysisView(source.id(), elements, relationships);
  }

  /** The source CSM Snapshot's own stable identity, unchanged from {@link CsmSnapshotSource#id()}. */
  public CsmSnapshotId sourceSnapshotId() {
    return sourceSnapshotId;
  }

  /** Every Subject's element-side classification (effective knowledge + conflict status). */
  public Map<Subject, SubjectConflictMarker.Classification<CsmElement>> elementClassifications() {
    return elementClassifications;
  }

  /** Every Subject's relationship-side classification (effective knowledge + conflict status). */
  public Map<Subject, SubjectConflictMarker.Classification<CsmRelationship>> relationshipClassifications() {
    return relationshipClassifications;
  }

  /** The conflict status for the given Subject, if it is present in this view. */
  public Optional<EffectiveKnowledgeStatus> statusOf(Subject subject) {
    SubjectConflictMarker.Classification<CsmElement> elementClassification = elementClassifications.get(subject);
    if (elementClassification != null) {
      return Optional.of(elementClassification.status());
    }
    SubjectConflictMarker.Classification<CsmRelationship> relationshipClassification =
        relationshipClassifications.get(subject);
    if (relationshipClassification != null) {
      return Optional.of(relationshipClassification.status());
    }
    return Optional.empty();
  }

  /** Every CSM element present in this view, across every Subject. */
  public Set<CsmElement> elements() {
    Set<CsmElement> result = new LinkedHashSet<>();
    for (SubjectConflictMarker.Classification<CsmElement> classification : elementClassifications.values()) {
      result.addAll(classification.assertions());
    }
    return result;
  }

  /** Every CSM relationship present in this view, across every Subject. */
  public Set<CsmRelationship> relationships() {
    Set<CsmRelationship> result = new LinkedHashSet<>();
    for (SubjectConflictMarker.Classification<CsmRelationship> classification :
        relationshipClassifications.values()) {
      result.addAll(classification.assertions());
    }
    return result;
  }

  /** Every element of the given kind present in this view. */
  public Set<CsmElement> elementsOfKind(CsmEntityKind kind) {
    Objects.requireNonNull(kind, "kind");
    Set<CsmElement> result = new LinkedHashSet<>();
    for (CsmElement element : elements()) {
      if (element.kind() == kind) {
        result.add(element);
      }
    }
    return result;
  }

  /** Every relationship of the given type present in this view. */
  public Set<CsmRelationship> relationshipsOfType(CsmRelationshipType type) {
    Objects.requireNonNull(type, "type");
    Set<CsmRelationship> result = new LinkedHashSet<>();
    for (CsmRelationship relationship : relationships()) {
      if (relationship.type() == type) {
        result.add(relationship);
      }
    }
    return result;
  }

  /** Convenience: an element's own identity/shape lookup by {@link CsmElementId}. */
  public Optional<CsmElement> element(CsmElementId id) {
    Objects.requireNonNull(id, "id");
    return elements().stream().filter(element -> element.id().equals(id)).findFirst();
  }
}
