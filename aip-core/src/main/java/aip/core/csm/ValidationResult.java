package aip.core.csm;

import java.util.List;
import java.util.Objects;

/**
 * The outcome of applying {@link CsmValidator} to a set of CSM
 * elements/relationships, per {@code CSM Validation Expectations}: "A
 * CSM, or any element within it, SHALL be considered invalid unless it
 * satisfies, at minimum: ..." — either {@link #success()} with no
 * violations, or a {@link #failure} with at least one human-readable
 * violation message.
 */
public record ValidationResult(boolean valid, List<String> violations) {

  private static final ValidationResult SUCCESS = new ValidationResult(true, List.of());

  public ValidationResult {
    Objects.requireNonNull(violations, "violations");
    violations = List.copyOf(violations);
    if (valid && !violations.isEmpty()) {
      throw new IllegalArgumentException("a valid result SHALL carry no violations");
    }
    if (!valid && violations.isEmpty()) {
      throw new IllegalArgumentException("an invalid result SHALL carry at least one violation");
    }
  }

  public static ValidationResult success() {
    return SUCCESS;
  }

  public static ValidationResult failure(List<String> violations) {
    return new ValidationResult(false, violations);
  }
}
