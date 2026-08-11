package aip.csmbuilder.dependency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.core.csm.DependencyKind;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ResourceDependencyKindClassifierTest {

  @Test
  void mappableMavenScopesClassifyCorrectly() {
    ResourceDependencyKindClassifier classifier = ResourceDependencyKindClassifier.loadDefaults();

    assertEquals(Optional.of(DependencyKind.COMPILE_TIME), classifier.classify("maven", "compile"));
    assertEquals(Optional.of(DependencyKind.COMPILE_TIME), classifier.classify("maven", "provided"));
    assertEquals(Optional.of(DependencyKind.RUNTIME), classifier.classify("maven", "runtime"));
    assertEquals(Optional.of(DependencyKind.TEST_ONLY), classifier.classify("maven", "test"));
  }

  @Test
  void mappableNpmScopesClassifyCorrectly() {
    ResourceDependencyKindClassifier classifier = ResourceDependencyKindClassifier.loadDefaults();

    assertEquals(Optional.of(DependencyKind.COMPILE_TIME), classifier.classify("npm", "dependencies"));
    assertEquals(Optional.of(DependencyKind.TEST_ONLY), classifier.classify("npm", "devDependencies"));
  }

  @Test
  void unmappableScopeYieldsEmptyNeverAGuess() {
    ResourceDependencyKindClassifier classifier = ResourceDependencyKindClassifier.loadDefaults();

    assertTrue(classifier.classify("maven", "some-future-unknown-scope").isEmpty());
  }

  @Test
  void unrecognizedBuildSystemYieldsEmpty() {
    ResourceDependencyKindClassifier classifier = ResourceDependencyKindClassifier.loadDefaults();

    assertTrue(classifier.classify("gradle", "implementation").isEmpty());
  }

  @Test
  void versionTagIsReadableAndExcludedFromTheScopeTable() {
    ResourceDependencyKindClassifier classifier = ResourceDependencyKindClassifier.loadDefaults();

    assertEquals(Optional.of("1"), classifier.versionOf("maven"));
    // 'version' itself must never be treated as a native scope string.
    assertTrue(classifier.classify("maven", "version").isEmpty());
  }

  @Test
  void loadingASubsetOfBuildSystemsOmitsTheRest() {
    ResourceDependencyKindClassifier classifier = ResourceDependencyKindClassifier.load(java.util.List.of("maven"));

    assertTrue(classifier.classify("npm", "dependencies").isEmpty());
    assertTrue(classifier.versionOf("npm").isEmpty());
  }
}
