package aip.ai;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

/**
 * {@link GenerationIdentifier} tests, per {@code Generation Identifier
 * Uniqueness and Non-Content-Derivation}.
 */
class GenerationIdentifierTest {

  @Test
  void generateProducesDistinctTokensAcrossCalls() {
    // Each Agent invocation gets its own Generation Identifier -
    // repeated calls to generate() (as RecommendationConstructor
    // performs once per invocation) never collide.
    GenerationIdentifier first = GenerationIdentifier.generate();
    GenerationIdentifier second = GenerationIdentifier.generate();
    assertNotEquals(first, second);
  }
}
