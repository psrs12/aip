package aip.core.evidence;

import java.util.Objects;
import java.util.Optional;

/**
 * An Evidence Item's current physical source location, per
 * {@code Evidence Traceability}: "file path and, where applicable, a
 * position range."
 *
 * <p>Deliberately a distinct type from
 * {@code aip.core.csm.SourceLocation} even though structurally
 * similar — the two packages are kept independent (see
 * {@code aip.core.evidence}'s package-info). A CSM Type/Method's
 * source-location attribute is derived from an {@link EvidenceLocation}
 * by CSM Builder's own Mapper logic, not shared by reference.
 *
 * @param filePath a repository-relative file path.
 * @param position an optional position range within that file.
 */
public record EvidenceLocation(String filePath, Optional<String> position) {

  public EvidenceLocation {
    Objects.requireNonNull(filePath, "filePath");
    if (filePath.isBlank()) {
      throw new IllegalArgumentException("filePath must not be blank");
    }
    Objects.requireNonNull(position, "position");
  }

  public static EvidenceLocation of(String filePath) {
    return new EvidenceLocation(filePath, Optional.empty());
  }

  public static EvidenceLocation of(String filePath, String position) {
    return new EvidenceLocation(filePath, Optional.of(position));
  }
}
