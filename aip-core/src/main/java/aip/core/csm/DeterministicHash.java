package aip.core.csm;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * A stable, deterministic hex-digest function over an ordered sequence
 * of string components, used by identity types in this package (e.g.
 * {@link AnalysisResultId}) whose own specifications require identity
 * to be "a deterministic function" of several inputs, "never random or
 * otherwise non-reproducible."
 *
 * <p>Deliberately does not rely on {@link Object#hashCode()} (a
 * record's generated {@code hashCode} combination algorithm is a JDK
 * implementation detail, not a specified, cross-version-stable
 * contract) — this class instead hashes an explicit, ordered, {@code
 * '|'}-delimited canonical string with SHA-256, which is both
 * deterministic and stable across JVM versions and restarts.
 */
final class DeterministicHash {

  private DeterministicHash() {}

  /**
   * The SHA-256 hex digest of {@code components}, each rendered by
   * {@link String#valueOf(Object)} and joined with {@code '|'}. Two
   * calls with equal component sequences always produce the same
   * digest; no two different sequences are guaranteed collision-free
   * (SHA-256's own, cryptographically negligible collision
   * probability is treated as sufficient here, the same as every
   * other identity-derivation scheme in this project).
   */
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
      // SHA-256 is a mandatory algorithm for every JDK implementation
      // (see MessageDigest's own javadoc); this is unreachable in
      // practice.
      throw new IllegalStateException("SHA-256 MessageDigest unavailable", e);
    }
  }
}
