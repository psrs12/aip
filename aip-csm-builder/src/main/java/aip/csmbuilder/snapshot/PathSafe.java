package aip.csmbuilder.snapshot;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Encodes a repository identifier into a single, filesystem-path-safe
 * directory-name segment, for {@link FilesystemSnapshotStore}. A real
 * repository identifier is not guaranteed to be path-safe (it may
 * contain {@code /}, for instance) — standard URL encoding is a
 * simple, collision-free, standard-library-only transform that
 * guarantees a single safe segment. Never decoded back: every {@link
 * SnapshotStore} method takes and returns the original, un-encoded
 * identifier — this encoding is purely a directory-naming detail.
 */
final class PathSafe {

  private PathSafe() {}

  static String encode(String repositoryIdentifier) {
    return URLEncoder.encode(repositoryIdentifier, StandardCharsets.UTF_8);
  }
}
