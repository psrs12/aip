package aip.core.evidence;

/**
 * The Repository Evidence Model's closed set of Evidence kinds, per
 * the archived {@code software-repository-understanding} specification.
 *
 * <p>Structural container kinds ({@link #REPOSITORY}, {@link #PROJECT},
 * {@link #MODULE}, {@link #PACKAGE}, {@link #FILE}) and fact kinds
 * ({@link #MANIFEST_DEPENDENCY_EDGE}, {@link #IMPORT_EDGE},
 * {@link #API_CONTRACT_DECLARATION}, {@link #CONFIG_FILE},
 * {@link #CONFIG_REFERENCE}) are Repository Understanding's own
 * generic vocabulary. {@link #SOURCE_UNIT} and {@link #METHOD} carry a
 * language-native construct kind label as an attribute (see
 * {@code Evidence Kind Vocabulary Uses Native Terms}) rather than
 * having one Java enum constant per native label (a Java
 * {@code class}, a Go {@code struct}, ... all remain
 * {@link #SOURCE_UNIT}).
 */
public enum EvidenceKind {
  REPOSITORY,
  PROJECT,
  MODULE,
  PACKAGE,
  FILE,
  SOURCE_UNIT,
  METHOD,
  MANIFEST_DEPENDENCY_EDGE,
  IMPORT_EDGE,
  API_CONTRACT_DECLARATION,
  CONFIG_FILE,
  CONFIG_REFERENCE
}
