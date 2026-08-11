package aip.csmbuilder.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.core.csm.CsmRelationship;
import aip.core.csm.CsmRelationshipType;
import aip.core.evidence.DiscoveryOutcome;
import aip.core.evidence.EvidenceAttributes;
import aip.core.evidence.EvidenceId;
import aip.core.evidence.EvidenceItem;
import aip.core.evidence.EvidenceKind;
import aip.core.evidence.EvidenceRelationship;
import aip.core.evidence.EvidenceRelationshipType;
import aip.core.evidence.ExtractionMethod;
import aip.core.evidence.RepositoryEvidenceModel;
import aip.csmbuilder.identity.ElementIdentityDeriver;
import aip.csmbuilder.mapping.EvidenceAttributeKeys;
import aip.csmbuilder.mapping.EvidenceKindMapperRegistry;
import aip.csmbuilder.mapping.MappingOrchestrator;
import aip.csmbuilder.mapping.MappingResult;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * API contract relationship construction tests (tasks.md 12.1-12.3):
 * an {@code ApiContractDeclaration} Evidence Item produces an {@code
 * exposure/consumption} relationship attached to its declaring {@code
 * Type}/{@code Method}, and never invents a consumer element.
 */
class ApiContractMapperTest {

  private static final Clock FIXED_CLOCK = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);

  @Test
  void apiContractEvidenceProducesAnExposureRelationshipOnTheDeclaringType() {
    EvidenceItem type = sourceUnit("com.acme.pkg.MyController");
    EvidenceItem apiContract = apiContract("get-widget", "GET /widgets/{id}");
    EvidenceRelationship reference =
        new EvidenceRelationship(EvidenceRelationshipType.REFERENCE, apiContract.id(), type.id());
    RepositoryEvidenceModel model =
        RepositoryEvidenceModel.of(List.of(type, apiContract), List.of(reference));

    MappingResult result = construct(model);

    assertEquals(1, result.relationships().size());
    CsmRelationship relationship = result.relationships().get(0);
    assertEquals(CsmRelationshipType.EXPOSURE_CONSUMPTION, relationship.type());
    assertEquals(ElementIdentityDeriver.fromEvidenceId(type.id()), relationship.sourceId());
  }

  @Test
  void noConsumerEntityIsEverInvented() {
    EvidenceItem type = sourceUnit("com.acme.pkg.MyController");
    EvidenceItem apiContract = apiContract("get-widget", "GET /widgets/{id}");
    EvidenceRelationship reference =
        new EvidenceRelationship(EvidenceRelationshipType.REFERENCE, apiContract.id(), type.id());
    RepositoryEvidenceModel model =
        RepositoryEvidenceModel.of(List.of(type, apiContract), List.of(reference));

    MappingResult result = construct(model);

    assertEquals(
        1,
        result.elements().size(),
        "only the declaring Type is constructed (by TypeMapper) - the API contract evidence itself invents no consumer element");
    CsmRelationship relationship = result.relationships().get(0);
    assertTrue(relationship.targetId().isEmpty(), "target is absent, not self-referencing or invented");
  }

  @Test
  void structuralSummaryIsPreservedAsARelationshipAttribute() {
    EvidenceItem type = sourceUnit("com.acme.pkg.MyController");
    EvidenceItem apiContract = apiContract("get-widget", "GET /widgets/{id}");
    EvidenceRelationship reference =
        new EvidenceRelationship(EvidenceRelationshipType.REFERENCE, apiContract.id(), type.id());
    RepositoryEvidenceModel model =
        RepositoryEvidenceModel.of(List.of(type, apiContract), List.of(reference));

    MappingResult result = construct(model);

    CsmRelationship relationship = result.relationships().get(0);
    assertEquals(
        Optional.of("GET /widgets/{id}"),
        relationship.nativeAttributes().get(EvidenceAttributeKeys.API_STRUCTURAL_SUMMARY));
  }

  @Test
  void noDeclaringConstructMeansNoRelationshipConstructed() {
    EvidenceItem apiContract = apiContract("orphan", "GET /orphan");
    RepositoryEvidenceModel model = RepositoryEvidenceModel.of(List.of(apiContract), List.of());

    MappingResult result = construct(model);

    assertTrue(result.relationships().isEmpty());
  }

  @Test
  void relationshipIdentityIsStableAcrossRepeatedConstruction() {
    EvidenceItem type = sourceUnit("com.acme.pkg.MyController");
    EvidenceItem apiContract = apiContract("get-widget", "GET /widgets/{id}");
    EvidenceRelationship reference =
        new EvidenceRelationship(EvidenceRelationshipType.REFERENCE, apiContract.id(), type.id());
    RepositoryEvidenceModel model =
        RepositoryEvidenceModel.of(List.of(type, apiContract), List.of(reference));

    CsmRelationship first = construct(model).relationships().get(0);
    CsmRelationship second = construct(model).relationships().get(0);

    assertEquals(first.id(), second.id());
    assertEquals(ElementIdentityDeriver.forApiContractExposure(apiContract.id()), first.id());
  }

  private MappingResult construct(RepositoryEvidenceModel model) {
    EvidenceKindMapperRegistry registry = new EvidenceKindMapperRegistry();
    registry.register(new TypeMapper());
    registry.register(new ApiContractMapper());
    return new MappingOrchestrator(registry, FIXED_CLOCK).construct(model);
  }

  private static EvidenceItem sourceUnit(String scopeKey) {
    return new EvidenceItem(
        new EvidenceId("repo-1", EvidenceKind.SOURCE_UNIT, scopeKey),
        EvidenceAttributes.of(Map.of(EvidenceAttributeKeys.NATIVE_CONSTRUCT_KIND, "class")),
        Optional.empty(),
        DiscoveryOutcome.complete(),
        ExtractionMethod.FULL_PARSE);
  }

  private static EvidenceItem apiContract(String scopeKey, String structuralSummary) {
    return new EvidenceItem(
        new EvidenceId("repo-1", EvidenceKind.API_CONTRACT_DECLARATION, scopeKey),
        EvidenceAttributes.of(Map.of(EvidenceAttributeKeys.API_STRUCTURAL_SUMMARY, structuralSummary)),
        Optional.empty(),
        DiscoveryOutcome.complete(),
        ExtractionMethod.FULL_PARSE);
  }
}
