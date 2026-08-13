package aip.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import aip.core.csm.EvaluationIdentity;
import aip.core.csm.RuleEvaluationResultId;
import aip.core.csm.CsmScopeInstance;
import aip.core.csm.CsmSnapshotId;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * {@link RecommendationArtifactIdentity} tests, per {@code
 * Recommendation Artifact Identity} and {@code Recommendation Artifact
 * Identity Excludes Generated Content}.
 */
class RecommendationArtifactIdentityTest {

  private static final CsmSnapshotId SNAPSHOT = new CsmSnapshotId("repo", 1);
  private static final RuleEvaluationResultId RER =
      RuleEvaluationResultId.of("rule.a", 1, SNAPSHOT, CsmScopeInstance.wholeRepository(), Set.of());
  private static final EvaluationIdentity FINDING_IDENTITY = EvaluationIdentity.of(Set.of(RER));
  private static final GenerationIdentifier GENERATION_ID = new GenerationIdentifier("fixed-token-1");

  @Test
  void sameIdentityComponentsResolveToTheSameArtifactIdentity() {
    RecommendationArtifactIdentity first =
        RecommendationArtifactIdentity.of("agent.a", 1, FINDING_IDENTITY, GENERATION_ID);
    RecommendationArtifactIdentity second =
        RecommendationArtifactIdentity.of("agent.a", 1, FINDING_IDENTITY, GENERATION_ID);
    assertEquals(first, second);
  }

  @Test
  void differentGenerationIdentifierYieldsDistinguishableIdentity() {
    RecommendationArtifactIdentity first =
        RecommendationArtifactIdentity.of("agent.a", 1, FINDING_IDENTITY, GENERATION_ID);
    RecommendationArtifactIdentity second =
        RecommendationArtifactIdentity.of("agent.a", 1, FINDING_IDENTITY, new GenerationIdentifier("fixed-token-2"));
    assertNotEquals(first, second);
  }

  @Test
  void differentAgentIdentifierYieldsDistinguishableIdentity() {
    RecommendationArtifactIdentity a = RecommendationArtifactIdentity.of("agent.a", 1, FINDING_IDENTITY, GENERATION_ID);
    RecommendationArtifactIdentity b = RecommendationArtifactIdentity.of("agent.b", 1, FINDING_IDENTITY, GENERATION_ID);
    assertNotEquals(a, b);
  }

  @Test
  void differentAgentVersionYieldsDistinguishableIdentity() {
    RecommendationArtifactIdentity v1 = RecommendationArtifactIdentity.of("agent.a", 1, FINDING_IDENTITY, GENERATION_ID);
    RecommendationArtifactIdentity v2 = RecommendationArtifactIdentity.of("agent.a", 2, FINDING_IDENTITY, GENERATION_ID);
    assertNotEquals(v1, v2);
  }

  @Test
  void differentFindingEvaluationIdentityYieldsDistinguishableIdentity() {
    EvaluationIdentity otherFinding =
        EvaluationIdentity.of(
            Set.of(RuleEvaluationResultId.of("rule.b", 1, SNAPSHOT, CsmScopeInstance.wholeRepository(), Set.of())));
    RecommendationArtifactIdentity first = RecommendationArtifactIdentity.of("agent.a", 1, FINDING_IDENTITY, GENERATION_ID);
    RecommendationArtifactIdentity second = RecommendationArtifactIdentity.of("agent.a", 1, otherFinding, GENERATION_ID);
    assertNotEquals(first, second);
  }
}
