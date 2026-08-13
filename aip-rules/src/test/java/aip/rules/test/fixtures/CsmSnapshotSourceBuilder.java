package aip.rules.test.fixtures;

import aip.core.csm.ArchitectureComponentElement;
import aip.core.csm.CsmElement;
import aip.core.csm.CsmElementId;
import aip.core.csm.CsmRelationship;
import aip.core.csm.CsmRelationshipType;
import aip.core.csm.CsmSnapshotId;
import aip.core.csm.CsmSnapshotSource;
import aip.core.csm.DependencyKind;
import aip.core.csm.ModuleElement;
import aip.core.csm.NativeAttributes;
import aip.core.csm.ProvenanceRecord;
import aip.core.csm.TypeElement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A fluent fixture-building API assembling {@link CsmSnapshotSource}
 * instances entirely out of {@code aip.core.csm} types — mirrors
 * {@code aip-analysis}'s own fixture builder of the same shape and
 * name; not shared across modules, per this project's established
 * convention that each module's fixture layer is its own, test-scope-
 * only artifact.
 *
 * <p>Lives in {@code aip-rules}'s test sources only; production code
 * never depends on it (enforced by this module's own {@code
 * check-fixture-package-scope} build check).
 */
public final class CsmSnapshotSourceBuilder {

  private final String repositoryIdentifier;
  private final long sequenceNumber;
  private final List<CsmElement> elements = new ArrayList<>();
  private final List<CsmRelationship> relationships = new ArrayList<>();
  private int nextId = 1;

  private CsmSnapshotSourceBuilder(String repositoryIdentifier, long sequenceNumber) {
    this.repositoryIdentifier = repositoryIdentifier;
    this.sequenceNumber = sequenceNumber;
  }

  public static CsmSnapshotSourceBuilder forRepository(String repositoryIdentifier) {
    Objects.requireNonNull(repositoryIdentifier, "repositoryIdentifier");
    return new CsmSnapshotSourceBuilder(repositoryIdentifier, 1);
  }

  public CsmElementId module(String name) {
    return add(new ModuleElement(nextElementId(), name, observed(), NativeAttributes.empty()));
  }

  public CsmElementId type(String name) {
    return add(new TypeElement(nextElementId(), name, observed(), NativeAttributes.empty(), Optional.empty()));
  }

  public CsmSnapshotSourceBuilder containment(CsmElementId container, CsmElementId contained) {
    return relationship(CsmRelationshipType.CONTAINMENT, container, contained, Optional.empty());
  }

  public CsmSnapshotSourceBuilder dependency(CsmElementId source, CsmElementId target, DependencyKind kind) {
    return relationship(CsmRelationshipType.DEPENDENCY, source, target, Optional.of(kind));
  }

  public CsmSnapshotSourceBuilder boundaryConstraint(CsmElementId source, CsmElementId target) {
    return relationship(CsmRelationshipType.BOUNDARY_CONSTRAINT, source, target, Optional.empty());
  }

  /**
   * An Architecture Component composed of {@code composition}, per
   * {@code Component composed of existing structural elements} —
   * declared provenance (Architecture Components are never {@code
   * OBSERVED}, per {@link ArchitectureComponentElement}'s own
   * construction-time guard).
   */
  public CsmElementId architectureComponent(String name, List<CsmElementId> composition) {
    return add(
        new ArchitectureComponentElement(nextElementId(), name, declared(), NativeAttributes.empty(), composition));
  }

  /** An Architecture Component with inferred (rather than declared) provenance. */
  public CsmElementId architectureComponentInferred(String name, List<CsmElementId> composition) {
    return add(
        new ArchitectureComponentElement(
            nextElementId(), name, inferred(), NativeAttributes.empty(), composition));
  }

  /**
   * A "must not depend on" {@code BOUNDARY_CONSTRAINT} relationship,
   * carrying the {@code constraint-kind: must-not-depend-on}
   * {@code NativeAttributes} convention {@code BoundaryComplianceRuleType}
   * reads (`implement-architecture-compliance-agent/design.md`
   * Decision 2), with declared provenance.
   */
  public CsmElementId mustNotDependOnConstraint(CsmElementId source, CsmElementId target) {
    return boundaryConstraintWithAttributes(
        source, target, NativeAttributes.of(Map.of("constraint-kind", "must-not-depend-on")), declared());
  }

  /** A "must not depend on" constraint with inferred (rather than declared) provenance. */
  public CsmElementId mustNotDependOnConstraintInferred(CsmElementId source, CsmElementId target) {
    return boundaryConstraintWithAttributes(
        source, target, NativeAttributes.of(Map.of("constraint-kind", "must-not-depend-on")), inferred());
  }

  /** A {@code BOUNDARY_CONSTRAINT} relationship expressing a constraint shape other than "must not depend on". */
  public CsmElementId mustOnlyCommunicateViaConstraint(CsmElementId source, CsmElementId target) {
    return boundaryConstraintWithAttributes(
        source, target, NativeAttributes.of(Map.of("constraint-kind", "must-only-communicate-via")), declared());
  }

  private CsmElementId boundaryConstraintWithAttributes(
      CsmElementId source, CsmElementId target, NativeAttributes attributes, ProvenanceRecord provenance) {
    Objects.requireNonNull(source, "source");
    Objects.requireNonNull(target, "target");
    CsmRelationship relationship =
        CsmRelationship.of(
            nextRelationshipId(), CsmRelationshipType.BOUNDARY_CONSTRAINT, source, target, provenance, attributes);
    relationships.add(relationship);
    return relationship.id();
  }

  public CsmSnapshotSourceBuilder relationship(
      CsmRelationshipType type, CsmElementId source, CsmElementId target, Optional<DependencyKind> dependencyKind) {
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(source, "source");
    Objects.requireNonNull(target, "target");
    Objects.requireNonNull(dependencyKind, "dependencyKind");
    relationships.add(
        new CsmRelationship(
            nextRelationshipId(),
            type,
            source,
            Optional.of(target),
            observed(),
            NativeAttributes.empty(),
            dependencyKind));
    return this;
  }

  public CsmSnapshotSource build() {
    CsmSnapshotId id = new CsmSnapshotId(repositoryIdentifier, sequenceNumber);
    Set<CsmElement> elementSet = Set.copyOf(elements);
    Set<CsmRelationship> relationshipSet = Set.copyOf(relationships);
    return new CsmSnapshotSource() {
      @Override
      public Set<CsmElement> elements() {
        return elementSet;
      }

      @Override
      public Set<CsmRelationship> relationships() {
        return relationshipSet;
      }

      @Override
      public CsmSnapshotId id() {
        return id;
      }
    };
  }

  private CsmElementId add(CsmElement element) {
    elements.add(element);
    return element.id();
  }

  private CsmElementId nextElementId() {
    return new CsmElementId(repositoryIdentifier + ":element:" + nextId++);
  }

  private CsmElementId nextRelationshipId() {
    return new CsmElementId(repositoryIdentifier + ":relationship:" + nextId++);
  }

  private static ProvenanceRecord observed() {
    return ProvenanceRecord.observed("fixture", Instant.EPOCH);
  }

  private static ProvenanceRecord declared() {
    return ProvenanceRecord.declared("fixture", Instant.EPOCH);
  }

  private static ProvenanceRecord inferred() {
    return ProvenanceRecord.inferred("fixture", Instant.EPOCH, aip.core.csm.Confidence.HIGH);
  }
}
