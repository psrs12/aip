package aip.core.evidence;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * An Evidence Item's kind-specific attribute set, per the
 * {@code Repository Evidence Model Shape} requirement: "an attribute
 * set defined by that Evidence kind's own versioned attribute schema
 * (\"kind-specific attributes\")... an Evidence Item SHALL carry only
 * attributes defined by its kind's schema, or preserve
 * otherwise-uncaptured detail in the native evidence attribute bag."
 *
 * <p>Deliberately a distinct type from {@code aip.core.csm.NativeAttributes}
 * — see {@code aip.core.evidence}'s package-info for why the two
 * packages don't share types.
 */
public final class EvidenceAttributes {

  private static final EvidenceAttributes EMPTY = new EvidenceAttributes(Map.of());

  private final Map<String, String> values;

  private EvidenceAttributes(Map<String, String> values) {
    this.values = values;
  }

  public static EvidenceAttributes empty() {
    return EMPTY;
  }

  public static EvidenceAttributes of(Map<String, String> values) {
    Objects.requireNonNull(values, "values");
    return new EvidenceAttributes(Map.copyOf(values));
  }

  /** Returns a copy of this bag with {@code key} set to {@code value}. */
  public EvidenceAttributes with(String key, String value) {
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(value, "value");
    Map<String, String> copy = new LinkedHashMap<>(values);
    copy.put(key, value);
    return new EvidenceAttributes(Map.copyOf(copy));
  }

  public Optional<String> get(String key) {
    return Optional.ofNullable(values.get(key));
  }

  public boolean isEmpty() {
    return values.isEmpty();
  }

  public Map<String, String> asMap() {
    return values;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof EvidenceAttributes other && values.equals(other.values);
  }

  @Override
  public int hashCode() {
    return values.hashCode();
  }

  @Override
  public String toString() {
    return values.toString();
  }
}
