package aip.analysis.test.fixtures;

import aip.core.csm.CsmElement;
import aip.core.csm.CsmElementId;
import aip.core.csm.CsmRelationship;
import aip.core.csm.CsmRelationshipType;
import aip.core.csm.CsmSnapshotId;
import aip.core.csm.CsmSnapshotSource;
import aip.core.csm.DependencyKind;
import aip.core.csm.ModuleElement;
import aip.core.csm.NativeAttributes;
import aip.core.csm.PackageElement;
import aip.core.csm.ProjectElement;
import aip.core.csm.ProvenanceRecord;
import aip.core.csm.RepositoryElement;
import aip.core.csm.TypeElement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A fluent fixture-building API assembling {@link CsmSnapshotSource}
 * instances entirely out of {@code aip.core.csm} types — never a
 * fixture-specific or parallel schema — per {@code
 * implement-analysis-framework/design.md} Decision 6 and this
 * change's own Binding Decision 2 (no real {@code aip-csm-builder}
 * adapter).
 *
 * <p>Lives in {@code aip-analysis}'s test sources only; production
 * code never depends on it (mirroring {@code aip-csm-builder}'s own
 * {@code RepositoryEvidenceModelBuilder}, and enforced the same way by
 * this module's own {@code check-fixture-package-scope} build check).
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
    return forRepository(repositoryIdentifier, 1);
  }

  public static CsmSnapshotSourceBuilder forRepository(String repositoryIdentifier, long sequenceNumber) {
    Objects.requireNonNull(repositoryIdentifier, "repositoryIdentifier");
    return new CsmSnapshotSourceBuilder(repositoryIdentifier, sequenceNumber);
  }

  public CsmElementId repository(String name) {
    return add(new RepositoryElement(nextElementId(), name, observed(), NativeAttributes.empty()));
  }

  public CsmElementId project(String name) {
    return add(new ProjectElement(nextElementId(), name, observed(), NativeAttributes.empty()));
  }

  public CsmElementId module(String name) {
    return add(new ModuleElement(nextElementId(), name, observed(), NativeAttributes.empty()));
  }

  public CsmElementId module(String name, NativeAttributes nativeAttributes) {
    return add(new ModuleElement(nextElementId(), name, observed(), nativeAttributes));
  }

  public CsmElementId pkg(String name) {
    return add(new PackageElement(nextElementId(), name, observed(), NativeAttributes.empty()));
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
}
