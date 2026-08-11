package aip.csmbuilder.test.fixtures;

import aip.core.evidence.ChangeStatus;
import aip.core.evidence.DiscoveryOutcome;
import aip.core.evidence.EvidenceId;
import aip.core.evidence.FailureReason;
import aip.core.evidence.RepositoryEvidenceModel;
import java.util.Map;

/**
 * A named library of {@link RepositoryEvidenceModelBuilder}-built
 * fixture scenarios, per {@code design.md} Decision 5 (tasks.md 21.3).
 * Each method is traced, in its own javadoc, to the specific archived
 * {@code software-repository-understanding} requirement/scenario it
 * faithfully represents.
 *
 * <p>These are general-purpose, reusable scenarios — not tied to any
 * one Mapper's own unit tests (those already have their own small,
 * local fixtures). This library exists for the kind of test that needs
 * a whole, coherent Repository Evidence Model: an end-to-end
 * construction run, or a two-run incremental scenario.
 */
public final class FixtureScenarios {

  private FixtureScenarios() {}

  /**
   * A Project whose build system declares no sub-modules, containing
   * one default Module with a single Type. Traces to RU's {@code
   * Single-module Project yields a default Module} scenario: "a
   * detected or override-declared Project's build system declares no
   * sub-modules... Repository Understanding SHALL produce exactly one
   * default Module Evidence Item contained by that Project."
   */
  public static RepositoryEvidenceModel singleModuleProject() {
    RepositoryEvidenceModelBuilder b = RepositoryEvidenceModelBuilder.forRepository("repo-1");
    EvidenceId repository = b.repository();
    EvidenceId project = b.project("com.acme:app");
    EvidenceId module = b.defaultModule("com.acme:app:default");
    EvidenceId type = b.sourceUnit("com.acme.app.Main", "class");
    b.containment(repository, project);
    b.containment(project, module);
    b.containment(module, type);
    return b.build();
  }

  /**
   * A Project whose build system declares multiple sub-modules, each
   * contained by that Project and each with its own Type. Traces to
   * RU's {@code Multi-module build system produces multiple Modules}
   * scenario: "Repository Understanding SHALL produce one Module
   * Evidence Item per declared sub-module, each contained by that
   * Project."
   */
  public static RepositoryEvidenceModel multiModuleProject() {
    RepositoryEvidenceModelBuilder b = RepositoryEvidenceModelBuilder.forRepository("repo-1");
    EvidenceId repository = b.repository();
    EvidenceId project = b.project("com.acme:parent");
    EvidenceId moduleA = b.module("com.acme:module-a");
    EvidenceId moduleB = b.module("com.acme:module-b");
    EvidenceId typeA = b.sourceUnit("com.acme.a.TypeA", "class");
    EvidenceId typeB = b.sourceUnit("com.acme.b.TypeB", "interface");
    b.containment(repository, project);
    b.containment(project, moduleA);
    b.containment(project, moduleB);
    b.containment(moduleA, typeA);
    b.containment(moduleB, typeB);
    return b.build();
  }

  /**
   * A Type contained by the reserved unmanaged Project, representing
   * source claimed by no detected or override-declared Project. Traces
   * to RU's {@code Unclaimed files appear under the unmanaged Project}
   * scenario: "Repository Understanding SHALL represent that file as
   * contained by the repository's reserved unmanaged Project Evidence
   * Item."
   */
  public static RepositoryEvidenceModel unmanagedFiles() {
    RepositoryEvidenceModelBuilder b = RepositoryEvidenceModelBuilder.forRepository("repo-1");
    EvidenceId repository = b.repository();
    EvidenceId unmanagedProject = b.unmanagedProject();
    EvidenceId defaultModule = b.defaultModule("unmanaged:default");
    EvidenceId type = b.sourceUnit("scripts.BuildHelper", "class");
    EvidenceId file = b.file("scripts/BuildHelper.groovy", "scripts/BuildHelper.groovy");
    b.containment(repository, unmanagedProject);
    b.containment(unmanagedProject, defaultModule);
    b.containment(defaultModule, type);
    b.reference(type, file);
    return b.build();
  }

  /**
   * A {@code SourceUnit} Evidence Item with a {@code partial} discovery
   * outcome — some structure was captured, but not all. Traces to RU's
   * {@code Partial-Analysis Outcome Reporting} requirement: "Repository
   * Understanding SHALL record a discovery outcome of {@code partial}"
   * accompanied by a failure reason (here, {@code PARSE_ERROR}).
   */
  public static RepositoryEvidenceModel partialEvidence() {
    RepositoryEvidenceModelBuilder b = RepositoryEvidenceModelBuilder.forRepository("repo-1");
    EvidenceId module = b.module("com.acme:module-a");
    b.sourceUnit(
        "com.acme.a.PartiallyParsed", "class", DiscoveryOutcome.partial(FailureReason.PARSE_ERROR));
    return b.build();
  }

  /**
   * A {@code ManifestDependencyEdge} whose native scope maps cleanly to
   * a CSM {@code DependencyKind}. Traces to {@code csm-builder/spec.md}'s
   * {@code Mappable native scope produces a kind-qualified relationship}
   * scenario (Maven's {@code compile} scope maps to {@code
   * compile-time}).
   */
  public static RepositoryEvidenceModel mappableDependencyScope() {
    RepositoryEvidenceModelBuilder b = RepositoryEvidenceModelBuilder.forRepository("repo-1");
    EvidenceId moduleA = b.module("com.acme:module-a");
    EvidenceId moduleB = b.module("com.acme:module-b");
    b.manifestDependencyEdge("edge-1", "com.acme:module-a", "com.acme:module-b", "maven", "compile");
    return b.build();
  }

  /**
   * A {@code ManifestDependencyEdge} whose native scope has no
   * corresponding mapping-table entry. Traces to {@code
   * csm-builder/spec.md}'s {@code Unmappable native scope produces an
   * unqualified relationship} scenario: CSM Builder SHALL NOT guess a
   * kind qualifier for it.
   */
  public static RepositoryEvidenceModel unmappableDependencyScope() {
    RepositoryEvidenceModelBuilder b = RepositoryEvidenceModelBuilder.forRepository("repo-1");
    b.module("com.acme:module-a");
    b.module("com.acme:module-b");
    b.manifestDependencyEdge(
        "edge-1", "com.acme:module-a", "com.acme:module-b", "maven", "some-future-scope");
    return b.build();
  }

  /**
   * A two-run scenario: a Module present in the first run's Repository
   * Evidence Model is {@code REMOVED} (and {@code TOMBSTONED}, not yet
   * {@code PURGED}) in the second run's classification, while its
   * Evidence Item body is still present in the second run's model
   * (retained during the tombstone retention window). Traces to RU's
   * {@code Evidence Lifecycle States}: "A {@code TOMBSTONED} item
   * remains retrievable for at least one subsequent discovery run
   * before becoming eligible for {@code PURGED}," together with {@code
   * csm-builder/spec.md}'s {@code Evidence Lifecycle Interaction}
   * requirement.
   */
  public static TwoRunScenario removedAndTombstonedAcrossTwoRuns() {
    RepositoryEvidenceModelBuilder firstRunBuilder = RepositoryEvidenceModelBuilder.forRepository("repo-1");
    EvidenceId moduleId = firstRunBuilder.module("com.acme:module-a");
    RepositoryEvidenceModel firstRun = firstRunBuilder.build();

    RepositoryEvidenceModelBuilder secondRunBuilder = RepositoryEvidenceModelBuilder.forRepository("repo-1");
    secondRunBuilder.module("com.acme:module-a"); // retained during the tombstone window
    RepositoryEvidenceModel secondRun = secondRunBuilder.build();
    Map<EvidenceId, ChangeStatus> secondRunChangeStatuses = Map.of(moduleId, ChangeStatus.REMOVED);

    return new TwoRunScenario(firstRun, secondRun, secondRunChangeStatuses);
  }

  /** A fixture scenario spanning two CSM Builder runs, for incremental-construction tests (Sections 16-17). */
  public record TwoRunScenario(
      RepositoryEvidenceModel firstRun,
      RepositoryEvidenceModel secondRun,
      Map<EvidenceId, ChangeStatus> secondRunChangeStatuses) {}
}
