package aip.ai.test.fixtures;

import aip.ai.Agent;
import aip.ai.AgentInvocationResult;
import aip.ai.GenerationProvenance;
import aip.core.csm.Finding;
import java.time.Instant;
import java.util.Map;
import java.util.function.Function;

/**
 * A configurable {@link Agent} for use across test classes — mirrors
 * {@code aip-rules}'s own {@code StubRuleType}. Either returns a fixed
 * {@link AgentInvocationResult} on every invocation, or throws to
 * exercise generation-failure handling.
 */
public final class StubAgent implements Agent {

  private final String identifier;
  private final int version;
  private final Function<Finding, AgentInvocationResult> behavior;

  private StubAgent(String identifier, int version, Function<Finding, AgentInvocationResult> behavior) {
    this.identifier = identifier;
    this.version = version;
    this.behavior = behavior;
  }

  /** An Agent that always succeeds with a fixed, representative Recommendation payload. */
  public static StubAgent succeeding(String identifier, int version) {
    return new StubAgent(
        identifier,
        version,
        finding ->
            new AgentInvocationResult(
                "consider splitting this Module along its two responsibilities",
                0.75,
                new GenerationProvenance(
                    "stub-provider", "stub-model-v1", Map.of("temperature", "0.2"), Instant.parse("2026-01-01T00:00:00Z"))));
  }

  /** An Agent that always succeeds, with an explicitly supplied Confidence value. */
  public static StubAgent succeedingWithConfidence(String identifier, int version, double confidence) {
    return new StubAgent(
        identifier,
        version,
        finding ->
            new AgentInvocationResult(
                "stub guidance",
                confidence,
                new GenerationProvenance(
                    "stub-provider", "stub-model-v1", Map.of(), Instant.parse("2026-01-01T00:00:00Z"))));
  }

  /** An Agent whose invocation always fails (simulating a model call failing or timing out). */
  public static StubAgent failing(String identifier, int version) {
    return new StubAgent(
        identifier,
        version,
        finding -> {
          throw new RuntimeException("simulated generation failure");
        });
  }

  @Override
  public String identifier() {
    return identifier;
  }

  @Override
  public int version() {
    return version;
  }

  @Override
  public AgentInvocationResult invoke(Finding finding) {
    return behavior.apply(finding);
  }
}
