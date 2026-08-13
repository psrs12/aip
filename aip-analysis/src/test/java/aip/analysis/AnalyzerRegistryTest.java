package aip.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.analysis.test.fixtures.StubAnalyzer;
import aip.core.csm.CsmEntityKind;
import aip.core.csm.CsmScope;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** {@link AnalyzerRegistry} tests, per {@code Analyzer declares identifier, version, and scope}. */
class AnalyzerRegistryTest {

  private static final CsmScope SCOPE = CsmScope.ofEntityKinds(Set.of(CsmEntityKind.MODULE));

  @Test
  void registersAndLooksUpByIdentifier() {
    AnalyzerRegistry registry = new AnalyzerRegistry();
    Analyzer analyzer = new StubAnalyzer("analyzer.a", 1, SCOPE);
    registry.register(analyzer);

    assertEquals(analyzer, registry.lookup("analyzer.a").orElseThrow());
    assertTrue(registry.lookup("analyzer.unknown").isEmpty());
  }

  @Test
  void rejectsASecondRegistrationForTheSameIdentifier() {
    AnalyzerRegistry registry = new AnalyzerRegistry();
    registry.register(new StubAnalyzer("analyzer.a", 1, SCOPE));
    assertThrows(IllegalStateException.class, () -> registry.register(new StubAnalyzer("analyzer.a", 2, SCOPE)));
  }

  @Test
  void registeredIsOrderedByIdentifier() {
    AnalyzerRegistry registry = new AnalyzerRegistry();
    registry.register(new StubAnalyzer("analyzer.z", 1, SCOPE));
    registry.register(new StubAnalyzer("analyzer.a", 1, SCOPE));
    registry.register(new StubAnalyzer("analyzer.m", 1, SCOPE));

    List<String> identifiers = registry.registered().stream().map(Analyzer::identifier).toList();
    assertEquals(List.of("analyzer.a", "analyzer.m", "analyzer.z"), identifiers);
  }
}
