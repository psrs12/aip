package aip.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import aip.ai.test.fixtures.FindingFixtures;
import aip.core.csm.Finding;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * {@link Recommendation} tests, per {@code Recommendation Is a
 * Distinct, Immutable Artifact From Finding}, {@code Recommendation
 * References Its Source Finding by Both Identities}, and {@code
 * Recommendation Content Is Agent-Defined and Opaque to the
 * Framework}.
 */
class RecommendationTest {

  private static final GenerationProvenance PROVENANCE =
      new GenerationProvenance("provider", "v1", Map.of(), Instant.parse("2026-01-01T00:00:00Z"));

  @Test
  void ofComputesIdentityFromTheSameComponents() {
    Finding finding = FindingFixtures.of("rule.a", "csm:element:m1");
    GenerationIdentifier generationId = new GenerationIdentifier("token-1");
    Recommendation recommendation =
        Recommendation.of(
            "agent.a", 1, finding.id(), finding.logicalFindingIdentity(), generationId, "content", 0.8, PROVENANCE);
    assertEquals(
        RecommendationArtifactIdentity.of("agent.a", 1, finding.id(), generationId), recommendation.id());
  }

  @Test
  void retainsBothFindingIdentitiesDirectly() {
    Finding finding = FindingFixtures.of("rule.a", "csm:element:m1");
    Recommendation recommendation =
        Recommendation.of(
            "agent.a",
            1,
            finding.id(),
            finding.logicalFindingIdentity(),
            GenerationIdentifier.generate(),
            "content",
            0.8,
            PROVENANCE);
    assertEquals(finding.id(), recommendation.findingEvaluationIdentity());
    assertEquals(finding.logicalFindingIdentity(), recommendation.findingLogicalFindingIdentity());
  }

  @Test
  void differentlyShapedContentFromDifferentAgentsIsBothAccepted() {
    Finding finding = FindingFixtures.of("rule.a", "csm:element:m1");
    Recommendation textContent =
        Recommendation.of(
            "agent.a", 1, finding.id(), finding.logicalFindingIdentity(), GenerationIdentifier.generate(),
            "split this module", 0.8, PROVENANCE);
    Recommendation structuredContent =
        Recommendation.of(
            "agent.b", 1, finding.id(), finding.logicalFindingIdentity(), GenerationIdentifier.generate(),
            Map.of("action", "split", "target", "moduleA"), 0.6, PROVENANCE);
    assertEquals("split this module", textContent.content());
    assertEquals(Map.of("action", "split", "target", "moduleA"), structuredContent.content());
  }

  @Test
  void rejectsConfidenceOutsideValidRange() {
    Finding finding = FindingFixtures.of("rule.a", "csm:element:m1");
    assertThrows(
        IllegalArgumentException.class,
        () ->
            Recommendation.of(
                "agent.a", 1, finding.id(), finding.logicalFindingIdentity(), GenerationIdentifier.generate(),
                "content", 1.5, PROVENANCE));
  }

  @Test
  void rejectsBlankAgentIdentifier() {
    Finding finding = FindingFixtures.of("rule.a", "csm:element:m1");
    assertThrows(
        IllegalArgumentException.class,
        () ->
            Recommendation.of(
                " ", 1, finding.id(), finding.logicalFindingIdentity(), GenerationIdentifier.generate(),
                "content", 0.5, PROVENANCE));
  }
}
