package aip.core.csm;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The native-evidence-attribute bag: an opaque, immutable key/value
 * store attached to a CSM element or relationship, per the
 * {@code CSM Conceptual Vocabulary} requirement's escape hatch: "the
 * construct SHALL be preserved as an opaque native-evidence attribute
 * on the nearest matching CSM element, and SHALL NOT cause a new
 * entity kind or relationship type to be introduced."
 *
 * <p>Used both for language-native detail a Mapper cannot otherwise
 * represent (e.g. a {@code SourceUnit}'s native construct kind label —
 * see {@code Native Construct Kind Preserved as Attribute}) and for
 * relationship-level qualifiers such as a dependency's kind (see
 * {@code Dependency Kind Classification}).
 */
public final class NativeAttributes {

  private static final NativeAttributes EMPTY = new NativeAttributes(Map.of());

  private final Map<String, String> values;

  private NativeAttributes(Map<String, String> values) {
    this.values = values;
  }

  public static NativeAttributes empty() {
    return EMPTY;
  }

  public static NativeAttributes of(Map<String, String> values) {
    Objects.requireNonNull(values, "values");
    return new NativeAttributes(Map.copyOf(values));
  }

  /** Returns a copy of this bag with {@code key} set to {@code value}. */
  public NativeAttributes with(String key, String value) {
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(value, "value");
    Map<String, String> copy = new LinkedHashMap<>(values);
    copy.put(key, value);
    return new NativeAttributes(Map.copyOf(copy));
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
    return obj instanceof NativeAttributes other && values.equals(other.values);
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
