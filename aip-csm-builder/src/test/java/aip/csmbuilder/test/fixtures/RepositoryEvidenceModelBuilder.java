package aip.csmbuilder.test.fixtures;

import aip.core.evidence.DiscoveryOutcome;
import aip.core.evidence.EvidenceAttributes;
import aip.core.evidence.EvidenceId;
import aip.core.evidence.EvidenceItem;
import aip.core.evidence.EvidenceKind;
import aip.core.evidence.EvidenceLocation;
import aip.core.evidence.EvidenceRelationship;
import aip.core.evidence.EvidenceRelationshipType;
import aip.core.evidence.ExtractionMethod;
import aip.core.evidence.RepositoryEvidenceModel;
import aip.csmbuilder.mapping.EvidenceAttributeKeys;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A fluent fixture-building API assembling {@link RepositoryEvidenceModel}
 * instances entirely out of {@code aip.core.evidence} types — never a
 * fixture-specific or parallel schema — per {@code design.md} Decision
 * 5 (tasks.md 21.1). An invalid fixture (a missing required field, an
 * out-of-vocabulary kind) simply fails to compile or construct, the
 * same as any other misuse of the Repository Evidence contract would;
 * "contract-faithful" is a structural guarantee here, not a discipline
 * to remember.
 *
 * <p>Lives in {@code aip-csm-builder}'s test sources only — see {@code
 * FixturePackageIsTestScopeOnlyTest} (tasks.md 21.2), which mechanically
 * confirms CSM Builder's production code never imports this package
 * (invariant 4). This is what lets a future real Repository
 * Understanding implementation replace this builder's output with real
 * discovery output without CSM Builder's production code changing at
 * all: production code depends only on {@code aip.core.evidence}
 * types, never on how an instance of them was produced.
 *
 * <p>Every entity-adding method returns the new item's {@link
 * EvidenceId} (not {@code this}) so a caller can immediately wire it
 * into a {@link #containment} or {@link #reference} relationship;
 * relationship-adding methods return {@code this} for further
 * chaining. Attribute keys reuse {@link EvidenceAttributeKeys} — CSM
 * Builder's own documented convention for the archived RU
 * specification's otherwise implementation-defined attribute schema —
 * so a fixture is guaranteed interpretable by CSM Builder's real
 * Mappers, not merely shaped like one.
 */
public final class RepositoryEvidenceModelBuilder {

  private final String repositoryIdentifier;
  private final List<EvidenceItem> items = new ArrayList<>();
  private final List<EvidenceRelationship> relationships = new ArrayList<>();

  private RepositoryEvidenceModelBuilder(String repositoryIdentifier) {
    this.repositoryIdentifier = repositoryIdentifier;
  }

  public static RepositoryEvidenceModelBuilder forRepository(String repositoryIdentifier) {
    Objects.requireNonNull(repositoryIdentifier, "repositoryIdentifier");
    return new RepositoryEvidenceModelBuilder(repositoryIdentifier);
  }

  // --- Structural container kinds -------------------------------------

  /** The Repository Evidence Item itself, scoped by this builder's own repository identifier. */
  public EvidenceId repository() {
    return addItem(EvidenceKind.REPOSITORY, repositoryIdentifier, Map.of(), DiscoveryOutcome.complete(), ExtractionMethod.MANIFEST_DECLARED);
  }

  public EvidenceId project(String scopeKey) {
    return project(scopeKey, DiscoveryOutcome.complete());
  }

  public EvidenceId project(String scopeKey, DiscoveryOutcome outcome) {
    return addItem(EvidenceKind.PROJECT, scopeKey, Map.of(), outcome, ExtractionMethod.MANIFEST_DECLARED);
  }

  /**
   * The reserved unmanaged Project, per RU's {@code Unmanaged
   * Pseudo-Project Handling} — CSM Builder maps it exactly like any
   * other Project (see {@code Unmanaged Evidence Mapping}), so this is
   * simply {@link #project} with a conventional scope key.
   */
  public EvidenceId unmanagedProject() {
    return project("unmanaged");
  }

  public EvidenceId module(String scopeKey) {
    return module(scopeKey, DiscoveryOutcome.complete());
  }

  public EvidenceId module(String scopeKey, DiscoveryOutcome outcome) {
    return addItem(EvidenceKind.MODULE, scopeKey, Map.of(), outcome, ExtractionMethod.MANIFEST_DECLARED);
  }

  /**
   * RU's default Module for a single-module Project, per {@code
   * Single-module Project yields a default Module} — again, no special
   * casing on CSM Builder's side, so this is a naming convenience only.
   */
  public EvidenceId defaultModule(String scopeKey) {
    return module(scopeKey);
  }

  public EvidenceId pkg(String scopeKey, String namespaceName) {
    return addItem(
        EvidenceKind.PACKAGE,
        scopeKey,
        Map.of(EvidenceAttributeKeys.NAMESPACE_NAME, namespaceName),
        DiscoveryOutcome.complete(),
        ExtractionMethod.FULL_PARSE);
  }

  // --- Source-level kinds ----------------------------------------------

  public EvidenceId sourceUnit(String scopeKey, String nativeConstructKind) {
    return sourceUnit(scopeKey, nativeConstructKind, DiscoveryOutcome.complete());
  }

  public EvidenceId sourceUnit(String scopeKey, String nativeConstructKind, DiscoveryOutcome outcome) {
    return addItem(
        EvidenceKind.SOURCE_UNIT,
        scopeKey,
        Map.of(EvidenceAttributeKeys.NATIVE_CONSTRUCT_KIND, nativeConstructKind),
        outcome,
        ExtractionMethod.FULL_PARSE);
  }

  public EvidenceId method(String scopeKey) {
    return method(scopeKey, DiscoveryOutcome.complete());
  }

  public EvidenceId method(String scopeKey, DiscoveryOutcome outcome) {
    return addItem(EvidenceKind.METHOD, scopeKey, Map.of(), outcome, ExtractionMethod.FULL_PARSE);
  }

  public EvidenceId file(String scopeKey, String filePath) {
    return addItemWithLocation(
        EvidenceKind.FILE, scopeKey, Map.of(), EvidenceLocation.of(filePath), DiscoveryOutcome.complete(), ExtractionMethod.FULL_PARSE);
  }

  public EvidenceId file(String scopeKey, String filePath, String position) {
    return addItemWithLocation(
        EvidenceKind.FILE,
        scopeKey,
        Map.of(),
        EvidenceLocation.of(filePath, position),
        DiscoveryOutcome.complete(),
        ExtractionMethod.FULL_PARSE);
  }

  // --- Fact kinds --------------------------------------------------------

  public EvidenceId manifestDependencyEdge(
      String scopeKey, String sourceModuleScopeKey, String targetScopeKey, String buildSystem, String nativeScope) {
    Map<String, String> attributes = new LinkedHashMap<>();
    attributes.put(EvidenceAttributeKeys.DEPENDENCY_SOURCE_MODULE, sourceModuleScopeKey);
    attributes.put(EvidenceAttributeKeys.DEPENDENCY_TARGET, targetScopeKey);
    attributes.put(EvidenceAttributeKeys.BUILD_SYSTEM, buildSystem);
    attributes.put(EvidenceAttributeKeys.NATIVE_SCOPE, nativeScope);
    return addItem(
        EvidenceKind.MANIFEST_DEPENDENCY_EDGE, scopeKey, attributes, DiscoveryOutcome.complete(), ExtractionMethod.MANIFEST_DECLARED);
  }

  public EvidenceId manifestDependencyEdge(
      String scopeKey,
      String sourceModuleScopeKey,
      String targetScopeKey,
      String buildSystem,
      String nativeScope,
      DiscoveryOutcome outcome) {
    Map<String, String> attributes = new LinkedHashMap<>();
    attributes.put(EvidenceAttributeKeys.DEPENDENCY_SOURCE_MODULE, sourceModuleScopeKey);
    attributes.put(EvidenceAttributeKeys.DEPENDENCY_TARGET, targetScopeKey);
    attributes.put(EvidenceAttributeKeys.BUILD_SYSTEM, buildSystem);
    attributes.put(EvidenceAttributeKeys.NATIVE_SCOPE, nativeScope);
    return addItem(EvidenceKind.MANIFEST_DEPENDENCY_EDGE, scopeKey, attributes, outcome, ExtractionMethod.MANIFEST_DECLARED);
  }

  public EvidenceId importEdge(String scopeKey, String sourceModuleScopeKey, String targetScopeKey) {
    Map<String, String> attributes = new LinkedHashMap<>();
    attributes.put(EvidenceAttributeKeys.DEPENDENCY_SOURCE_MODULE, sourceModuleScopeKey);
    attributes.put(EvidenceAttributeKeys.DEPENDENCY_TARGET, targetScopeKey);
    return addItem(EvidenceKind.IMPORT_EDGE, scopeKey, attributes, DiscoveryOutcome.complete(), ExtractionMethod.FULL_PARSE);
  }

  public EvidenceId apiContractDeclaration(String scopeKey, String structuralSummary) {
    return addItem(
        EvidenceKind.API_CONTRACT_DECLARATION,
        scopeKey,
        Map.of(EvidenceAttributeKeys.API_STRUCTURAL_SUMMARY, structuralSummary),
        DiscoveryOutcome.complete(),
        ExtractionMethod.FULL_PARSE);
  }

  public EvidenceId configFile(String scopeKey) {
    return addItem(EvidenceKind.CONFIG_FILE, scopeKey, Map.of(), DiscoveryOutcome.complete(), ExtractionMethod.FULL_PARSE);
  }

  public EvidenceId configReference(String scopeKey) {
    return addItem(EvidenceKind.CONFIG_REFERENCE, scopeKey, Map.of(), DiscoveryOutcome.complete(), ExtractionMethod.FULL_PARSE);
  }

  // --- Relationships -------------------------------------------------

  public RepositoryEvidenceModelBuilder containment(EvidenceId parent, EvidenceId child) {
    relationships.add(new EvidenceRelationship(EvidenceRelationshipType.CONTAINMENT, parent, child));
    return this;
  }

  /** A {@code SourceUnit}/Method-level item's back-reference to its File Evidence Item (see {@code File Evidence and Kind-Specific Layering}). */
  public RepositoryEvidenceModelBuilder reference(EvidenceId from, EvidenceId to) {
    relationships.add(new EvidenceRelationship(EvidenceRelationshipType.REFERENCE, from, to));
    return this;
  }

  public RepositoryEvidenceModel build() {
    return RepositoryEvidenceModel.of(items, relationships);
  }

  private EvidenceId addItem(
      EvidenceKind kind, String scopeKey, Map<String, String> attributes, DiscoveryOutcome outcome, ExtractionMethod extraction) {
    EvidenceId id = new EvidenceId(repositoryIdentifier, kind, scopeKey);
    items.add(new EvidenceItem(id, EvidenceAttributes.of(attributes), Optional.empty(), outcome, extraction));
    return id;
  }

  private EvidenceId addItemWithLocation(
      EvidenceKind kind,
      String scopeKey,
      Map<String, String> attributes,
      EvidenceLocation location,
      DiscoveryOutcome outcome,
      ExtractionMethod extraction) {
    EvidenceId id = new EvidenceId(repositoryIdentifier, kind, scopeKey);
    items.add(new EvidenceItem(id, EvidenceAttributes.of(attributes), Optional.of(location), outcome, extraction));
    return id;
  }
}
