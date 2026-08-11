package aip.core.csm;

import java.util.Objects;
import java.util.Optional;

/**
 * A source-location reference retained as an attribute on a
 * {@link TypeElement} or {@link MethodElement}, per the
 * {@code Source File Representation} requirement: "Type and Method CSM
 * elements MAY retain a source-location reference (e.g., file path and
 * position) as an attribute for traceability purposes, without that
 * reference constituting a CSM relationship to a Source File entity."
 *
 * <p>Source File is deliberately not a CSM entity kind (see
 * {@link CsmEntityKind}) — this type exists precisely so a Type or
 * Method can carry location information without one.
 *
 * @param filePath a repository-relative file path.
 * @param position an optional position range within that file (e.g. a
 *     line/column range); not every producer of this type can supply
 *     one.
 */
public record SourceLocation(String filePath, Optional<String> position) {

  public SourceLocation {
    Objects.requireNonNull(filePath, "filePath");
    if (filePath.isBlank()) {
      throw new IllegalArgumentException("filePath must not be blank");
    }
    Objects.requireNonNull(position, "position");
  }

  public static SourceLocation of(String filePath) {
    return new SourceLocation(filePath, Optional.empty());
  }

  public static SourceLocation of(String filePath, String position) {
    return new SourceLocation(filePath, Optional.of(position));
  }
}
