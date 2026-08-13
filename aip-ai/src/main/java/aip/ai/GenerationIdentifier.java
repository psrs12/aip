package aip.ai;

import java.util.Objects;
import java.util.UUID;

/**
 * An opaque, invocation-scoped token, per {@code Generation Identifier
 * Uniqueness and Non-Content-Derivation}: "Every Agent invocation
 * SHALL be assigned a Generation Identifier that is unique among that
 * Agent's invocations and is not derived from the generated content of
 * the Recommendation it identifies."
 *
 * <p>{@link #generate()} is the only call to {@code UUID.randomUUID()}
 * — or any randomness source — anywhere in this codebase; see {@code
 * package-info.java} and {@code implement-agent-framework/design.md}
 * Decision 3 for why this is a deliberate, narrowly-scoped exception
 * to this project's otherwise-universal determinism discipline. Only
 * {@link RecommendationConstructor} calls {@link #generate()} — an
 * {@link Agent} implementation never mints its own, keeping "the
 * framework assigns it per invocation" a structural guarantee.
 *
 * @param value the opaque token string. Never blank.
 */
public record GenerationIdentifier(String value) {

  public GenerationIdentifier {
    Objects.requireNonNull(value, "value");
    if (value.isBlank()) {
      throw new IllegalArgumentException("GenerationIdentifier value must not be blank");
    }
  }

  /** Mints a fresh, unique-per-invocation Generation Identifier. Never derived from generated content. */
  public static GenerationIdentifier generate() {
    return new GenerationIdentifier(UUID.randomUUID().toString());
  }

  @Override
  public String toString() {
    return value;
  }
}
