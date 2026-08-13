package aip.ai;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * A stable, deterministic hex-digest function over an ordered sequence
 * of string components, used by {@link RecommendationArtifactIdentity}.
 *
 * <p>An {@code aip-ai}-local equivalent of {@code aip-core}'s own
 * package-private {@code DeterministicHash} — duplicated rather than
 * reused, since {@code aip-core}'s implementation is deliberately
 * package-private to {@code aip.core.csm} and this project's own
 * established discipline is that each identity scheme owns its digest
 * computation directly rather than reaching across a module boundary
 * for an internal utility (`implement-agent-framework/design.md`
 * Decision 3).
 */
final class DeterministicHash {

  private DeterministicHash() {}

  static String of(Object... components) {
    Objects.requireNonNull(components, "components");
    StringBuilder canonical = new StringBuilder();
    for (int i = 0; i < components.length; i++) {
      if (i > 0) {
        canonical.append('|');
      }
      canonical.append(String.valueOf(components[i]));
    }
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder(hash.length * 2);
      for (byte b : hash) {
        hex.append(String.format("%02x", b));
      }
      return hex.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 MessageDigest unavailable", e);
    }
  }
}
