package aip.ai;

import java.util.Optional;

/**
 * The Recommendation Store abstraction: writes and retrieves {@link
 * Recommendation}s, per {@code Recommendations Are Durable,
 * Individually Identifiable Artifacts} — mirroring {@code
 * aip-findings}'s own {@code FindingStore} precedent (per {@code
 * define-agent-framework/design.md} Decision 8, <strong>not</strong>
 * promoted to {@code aip-core} — no named future consumer justifies
 * it yet).
 *
 * <p>Concrete persistence technology is deliberately not chosen here.
 */
public interface RecommendationStore {

  /** Writes {@code recommendation} as a new, immutable artifact. Never overwrites a previously written one. */
  Recommendation write(Recommendation recommendation);

  /** The Recommendation with the given Artifact Identity, if one was written. */
  Optional<Recommendation> read(RecommendationArtifactIdentity id);
}
