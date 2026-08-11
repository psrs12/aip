package aip.csmbuilder.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.core.csm.CsmElement;
import aip.core.csm.CsmElementId;
import aip.core.csm.ModuleElement;
import aip.core.csm.NativeAttributes;
import aip.core.evidence.ChangeStatus;
import aip.core.evidence.DiscoveryOutcome;
import aip.core.evidence.EvidenceAttributes;
import aip.core.evidence.EvidenceId;
import aip.core.evidence.EvidenceItem;
import aip.core.evidence.EvidenceKind;
import aip.core.evidence.ExtractionMethod;
import aip.core.evidence.RepositoryEvidenceModel;
import aip.csmbuilder.identity.ElementIdentityDeriver;
import aip.csmbuilder.mapper.ModuleMapper;
import aip.csmbuilder.provenance.ObservedProvenanceFactory;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Extension Mechanism Verification (tasks.md 20.1, 20.2): registering a
 * new {@link EvidenceKindMapper} for a previously unmapped Evidence
 * kind requires no change to {@link ElementIdentityDeriver}, {@link
 * ObservedProvenanceFactory}, or {@link MappingOrchestrator}'s
 * incremental scoping — per {@code Extension Mechanism for New
 * Evidence Kinds}.
 *
 * <p>{@link EvidenceKind} is itself a closed enum — this codebase
 * cannot introduce a literal new kind without a deliberate, versioned
 * change to the archived RU specification, which is exactly what this
 * requirement says a Mapper registration alone should never need. This
 * test therefore stands {@link EvidenceKind#FILE} in for "a previously
 * unmapped kind": no production Mapper is registered for it anywhere
 * in this codebase (File Evidence becomes a location attribute, not an
 * element — see {@code File Evidence Becomes a Location Attribute, Not
 * a Relationship}), and it carries no registration-time exclusion (see
 * {@link EvidenceKindMapperRegistry}'s {@code CONFIG_FILE}/{@code
 * CONFIG_REFERENCE} guard, which {@code FILE} is not part of), making
 * it a legitimate, currently-unmapped stand-in for a hypothetical
 * future kind.
 */
class ExtensionMechanismTest {

  private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-04-01T00:00:00Z"), ZoneOffset.UTC);

  @Test
  void registeringANewMapperLeavesExistingConstructionUnaffected() {
    EvidenceItem module = moduleItem("com.acme:module-a");
    EvidenceItem hypotheticalKindItem = hypotheticalKindItem("new-kind-item-1");
    RepositoryEvidenceModel model =
        RepositoryEvidenceModel.of(List.of(module, hypotheticalKindItem), List.of());

    EvidenceKindMapperRegistry registry = new EvidenceKindMapperRegistry();
    registry.register(new ModuleMapper());
    registry.register(new StubMapperUsingStandardMechanisms());

    MappingResult result = new MappingOrchestrator(registry, FIXED_CLOCK).construct(model);

    // The pre-existing ModuleMapper's output is exactly what it would
    // be without the new registration - unaffected.
    assertEquals(2, result.elements().size());
    Optional<CsmElement> moduleElement =
        result.elements().stream().filter(e -> e.id().equals(ElementIdentityDeriver.fromEvidenceId(module.id()))).findFirst();
    assertTrue(moduleElement.isPresent());

    // The new Mapper's own output used the same, unmodified identity-
    // derivation and provenance-construction utilities every other
    // Mapper uses - no framework change was needed for it to plug in.
    Optional<CsmElement> newKindElement =
        result.elements().stream()
            .filter(e -> e.id().equals(ElementIdentityDeriver.fromEvidenceId(hypotheticalKindItem.id())))
            .findFirst();
    assertTrue(newKindElement.isPresent());
    assertEquals(hypotheticalKindItem.id().toString(), newKindElement.get().provenance().sourceReference());
  }

  @Test
  void incrementalScopingAppliesToTheNewKindWithoutAnyChange() {
    EvidenceItem hypotheticalKindItem = hypotheticalKindItem("new-kind-item-1");
    RepositoryEvidenceModel model = RepositoryEvidenceModel.of(List.of(hypotheticalKindItem), List.of());
    StubMapperUsingStandardMechanisms stubMapper = new StubMapperUsingStandardMechanisms();
    EvidenceKindMapperRegistry registry = new EvidenceKindMapperRegistry();
    registry.register(stubMapper);

    CsmElement priorElement =
        new ModuleElement(
            new CsmElementId("csm:prior"),
            "prior",
            ObservedProvenanceFactory.fromEvidence(hypotheticalKindItem.id(), Instant.EPOCH),
            NativeAttributes.empty());
    PriorElementLookup priorElements =
        IncrementalConstructionTest.priorLookup(hypotheticalKindItem.id(), priorElement, stubMapper.mapperVersion());

    MappingResult result =
        new MappingOrchestrator(registry, FIXED_CLOCK)
            .construct(model, Map.of(hypotheticalKindItem.id(), ChangeStatus.UNCHANGED), priorElements);

    assertTrue(stubMapper.invocations.isEmpty(), "UNCHANGED carry-forward worked for the new kind with no special-casing");
    assertEquals(1, result.elements().size());
    assertEquals(priorElement, result.elements().get(0));
  }

  private static EvidenceItem moduleItem(String scopeKey) {
    return new EvidenceItem(
        new EvidenceId("repo-1", EvidenceKind.MODULE, scopeKey),
        EvidenceAttributes.empty(),
        Optional.empty(),
        DiscoveryOutcome.complete(),
        ExtractionMethod.MANIFEST_DECLARED);
  }

  private static EvidenceItem hypotheticalKindItem(String scopeKey) {
    return new EvidenceItem(
        new EvidenceId("repo-1", EvidenceKind.FILE, scopeKey),
        EvidenceAttributes.empty(),
        Optional.empty(),
        DiscoveryOutcome.complete(),
        ExtractionMethod.FULL_PARSE);
  }

  /**
   * A stand-in for a brand-new, hand-written Mapper for the
   * hypothetical new kind — deliberately calls exactly the same,
   * unmodified {@link ElementIdentityDeriver} and {@link
   * ObservedProvenanceFactory} utilities every production Mapper in
   * {@code aip.csmbuilder.mapper} calls, demonstrating that plugging in
   * a new kind requires no change to either.
   */
  private static final class StubMapperUsingStandardMechanisms implements EvidenceKindMapper {

    final List<EvidenceItem> invocations = new java.util.ArrayList<>();

    @Override
    public EvidenceKind supportedKind() {
      return EvidenceKind.FILE;
    }

    @Override
    public int mapperVersion() {
      return 1;
    }

    @Override
    public MappingResult map(EvidenceItem item, MappingContext context) {
      invocations.add(item);
      ModuleElement element =
          new ModuleElement(
              ElementIdentityDeriver.fromEvidenceId(item.id()),
              item.id().scopeKey(),
              ObservedProvenanceFactory.fromEvidence(item.id(), context.constructionTimestamp()),
              NativeAttributes.empty());
      return MappingResult.ofElement(element);
    }
  }
}
