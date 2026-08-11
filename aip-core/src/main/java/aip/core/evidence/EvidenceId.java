package aip.core.evidence;

import java.util.Objects;

/**
 * An Evidence Item's stable identity, per the {@code Evidence
 * Identity} requirement: "a stable identity composed of a repository
 * identifier, its Evidence kind, and a scope key."
 *
 * <p>Deliberately distinct from an Evidence Item's current source
 * location (see {@link EvidenceLocation}): identity SHALL remain
 * stable across a re-analysis in which the underlying construct has
 * not materially changed, even if its current location has.
 *
 * @param repositoryIdentifier the stable identifier of the repository
 *     this evidence belongs to (see {@code Repository Identity}).
 * @param kind this Evidence Item's kind.
 * @param scopeKey a canonical/logical name for evidence with a
 *     language- or tool-native name, or a normalized
 *     repository-relative path for evidence with no natural name (see
 *     {@code Evidence Identity}). This type does not distinguish which
 *     case produced the value — that distinction belongs to whatever
 *     produces the identity, not to the identity's own shape.
 */
public record EvidenceId(String repositoryIdentifier, EvidenceKind kind, String scopeKey) {

  public EvidenceId {
    Objects.requireNonNull(repositoryIdentifier, "repositoryIdentifier");
    if (repositoryIdentifier.isBlank()) {
      throw new IllegalArgumentException("repositoryIdentifier must not be blank");
    }
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(scopeKey, "scopeKey");
    if (scopeKey.isBlank()) {
      throw new IllegalArgumentException("scopeKey must not be blank");
    }
  }

  @Override
  public String toString() {
    return repositoryIdentifier + ":" + kind + ":" + scopeKey;
  }
}
