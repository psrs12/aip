package aip.csmbuilder.snapshot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A minimal escaping scheme shared by {@link CsmElementCodec} and
 * {@link CsmRelationshipCodec} for {@link FilesystemSnapshotStore}'s
 * tab-delimited snapshot content files. Backslash, tab, newline,
 * carriage return, {@code '='}, {@code ';'}, and {@code ','} are the
 * only characters with structural meaning in this format (field
 * delimiter, map-entry separator, map key/value separator, list
 * separator) and the only ones ever escaped — every other character,
 * including the colons, dots, and slashes typical of a CSM identity or
 * a repository-relative path, passes through untouched, keeping the
 * resulting files genuinely readable in a plain text editor.
 *
 * <p>An empty string represents an absent {@code Optional<String>}
 * field throughout this package's codecs — none of this format's
 * domain values (identities, names, file paths, attribute keys/values)
 * are ever legitimately empty-but-present in practice, so this is an
 * accepted, deliberate simplification of this internal, swappable
 * format, not a general-purpose guarantee.
 */
final class TextEncoding {

  private TextEncoding() {}

  static String escape(String value) {
    StringBuilder sb = new StringBuilder(value.length());
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
        case '\\' -> sb.append("\\\\");
        case '\t' -> sb.append("\\t");
        case '\n' -> sb.append("\\n");
        case '\r' -> sb.append("\\r");
        case '=' -> sb.append("\\=");
        case ';' -> sb.append("\\;");
        case ',' -> sb.append("\\,");
        default -> sb.append(c);
      }
    }
    return sb.toString();
  }

  static String unescape(String value) {
    StringBuilder sb = new StringBuilder(value.length());
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (c == '\\' && i + 1 < value.length()) {
        char next = value.charAt(++i);
        switch (next) {
          case '\\' -> sb.append('\\');
          case 't' -> sb.append('\t');
          case 'n' -> sb.append('\n');
          case 'r' -> sb.append('\r');
          case '=' -> sb.append('=');
          case ';' -> sb.append(';');
          case ',' -> sb.append(',');
          default -> sb.append('\\').append(next);
        }
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  /**
   * Splits {@code s} on unescaped occurrences of {@code delimiter},
   * leaving each returned part still escaped — call {@link #unescape}
   * on each part afterward.
   */
  static List<String> splitEscaped(String s, char delimiter) {
    List<String> parts = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '\\' && i + 1 < s.length()) {
        current.append(c).append(s.charAt(++i));
      } else if (c == delimiter) {
        parts.add(current.toString());
        current.setLength(0);
      } else {
        current.append(c);
      }
    }
    parts.add(current.toString());
    return parts;
  }

  static String encodeOptional(Optional<String> value) {
    Objects.requireNonNull(value, "value");
    return value.map(TextEncoding::escape).orElse("");
  }

  static Optional<String> decodeOptional(String field) {
    return field.isEmpty() ? Optional.empty() : Optional.of(unescape(field));
  }

  static String encodeList(List<String> values) {
    if (values.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < values.size(); i++) {
      if (i > 0) {
        sb.append(',');
      }
      sb.append(escape(values.get(i)));
    }
    return sb.toString();
  }

  static List<String> decodeList(String field) {
    if (field.isEmpty()) {
      return List.of();
    }
    return splitEscaped(field, ',').stream().map(TextEncoding::unescape).toList();
  }

  static String encodeMap(Map<String, String> map) {
    if (map.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (Map.Entry<String, String> entry : map.entrySet()) {
      if (!first) {
        sb.append(';');
      }
      first = false;
      sb.append(escape(entry.getKey())).append('=').append(escape(entry.getValue()));
    }
    return sb.toString();
  }

  static Map<String, String> decodeMap(String field) {
    Map<String, String> map = new LinkedHashMap<>();
    if (field.isEmpty()) {
      return map;
    }
    for (String entry : splitEscaped(field, ';')) {
      List<String> kv = splitEscaped(entry, '=');
      if (kv.size() != 2) {
        throw new IllegalArgumentException("malformed native-attribute entry: '" + entry + "'");
      }
      map.put(unescape(kv.get(0)), unescape(kv.get(1)));
    }
    return map;
  }
}
