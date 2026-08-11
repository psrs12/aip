package aip.csmbuilder.dependency;

import aip.core.csm.DependencyKind;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

/**
 * The default {@link DependencyKindClassifier}: loads a versioned,
 * declarative mapping resource per build system (one small
 * {@code .properties} file per build system, bundled as
 * {@code aip-csm-builder} resources under
 * {@code aip/csmbuilder/dependency/mapping/<buildSystem>.properties}),
 * per {@code openspec/changes/implement-csm-builder/design.md}
 * Decision 3.
 *
 * <p>Each resource file's {@code version} property is reserved
 * (excluded from the native-scope table itself) and readable via
 * {@link #versionOf}.
 */
public final class ResourceDependencyKindClassifier implements DependencyKindClassifier {

  private static final String RESOURCE_PATH_PREFIX = "/aip/csmbuilder/dependency/mapping/";
  private static final String VERSION_KEY = "version";
  private static final List<String> DEFAULT_BUILD_SYSTEMS = List.of("maven", "npm");

  private final Map<String, Map<String, DependencyKind>> tables;
  private final Map<String, String> versions;

  private ResourceDependencyKindClassifier(
      Map<String, Map<String, DependencyKind>> tables, Map<String, String> versions) {
    this.tables = tables;
    this.versions = versions;
  }

  /** Loads the build systems this codebase currently ships mapping resources for. */
  public static ResourceDependencyKindClassifier loadDefaults() {
    return load(DEFAULT_BUILD_SYSTEMS);
  }

  public static ResourceDependencyKindClassifier load(List<String> buildSystems) {
    Objects.requireNonNull(buildSystems, "buildSystems");
    Map<String, Map<String, DependencyKind>> tables = new LinkedHashMap<>();
    Map<String, String> versions = new LinkedHashMap<>();
    for (String buildSystem : buildSystems) {
      Properties properties = loadProperties(buildSystem);
      versions.put(buildSystem, properties.getProperty(VERSION_KEY, "unversioned"));

      Map<String, DependencyKind> table = new LinkedHashMap<>();
      for (String key : properties.stringPropertyNames()) {
        if (key.equals(VERSION_KEY)) {
          continue;
        }
        table.put(key, DependencyKind.valueOf(properties.getProperty(key)));
      }
      tables.put(buildSystem, Map.copyOf(table));
    }
    return new ResourceDependencyKindClassifier(Map.copyOf(tables), Map.copyOf(versions));
  }

  private static Properties loadProperties(String buildSystem) {
    String resourcePath = RESOURCE_PATH_PREFIX + buildSystem + ".properties";
    Properties properties = new Properties();
    try (InputStream in = ResourceDependencyKindClassifier.class.getResourceAsStream(resourcePath)) {
      if (in == null) {
        throw new IllegalStateException("missing dependency-kind mapping resource: " + resourcePath);
      }
      properties.load(in);
    } catch (IOException e) {
      throw new UncheckedIOException("failed to load " + resourcePath, e);
    }
    return properties;
  }

  @Override
  public Optional<DependencyKind> classify(String buildSystem, String nativeScope) {
    Objects.requireNonNull(buildSystem, "buildSystem");
    Objects.requireNonNull(nativeScope, "nativeScope");
    Map<String, DependencyKind> table = tables.get(buildSystem);
    if (table == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(table.get(nativeScope));
  }

  /** The version tag of {@code buildSystem}'s mapping resource, if that build system was loaded. */
  public Optional<String> versionOf(String buildSystem) {
    return Optional.ofNullable(versions.get(buildSystem));
  }
}
