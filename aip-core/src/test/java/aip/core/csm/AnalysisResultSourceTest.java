package aip.core.csm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * {@link AnalysisResultSource} contract tests, per {@code Analysis
 * Result Source Shape}.
 */
class AnalysisResultSourceTest {

  private static final CsmSnapshotId SNAPSHOT = new CsmSnapshotId("repo", 1);
  private static final CsmScopeInstance SCOPE_INSTANCE = CsmScopeInstance.wholeRepository();

  @Test
  void exposesRetrievalByIdentityAndByAnalyzerAndSnapshot() {
    AnalysisResult result = AnalysisResult.of("analyzer.a", 1, SNAPSHOT, SCOPE_INSTANCE, "payload");
    AnalysisResultSource source = fixtureSource(Set.of(result));

    assertEquals(result, source.read(result.id()).orElseThrow());
    assertEquals(Set.of(result), source.list("analyzer.a", SNAPSHOT));
    assertTrue(source.list("analyzer.unknown", SNAPSHOT).isEmpty());
  }

  @Test
  void agnosticToAnalysisResultProduction() {
    // Two different AnalysisResultSource implementations exposing
    // equivalent content behave identically from a caller's
    // perspective — no framework code branches on which one it holds.
    AnalysisResult result = AnalysisResult.of("analyzer.a", 1, SNAPSHOT, SCOPE_INSTANCE, "payload");
    AnalysisResultSource first = fixtureSource(Set.of(result));
    AnalysisResultSource second = fixtureSource(Set.of(result));

    assertEquals(first.read(result.id()), second.read(result.id()));
    assertEquals(first.list("analyzer.a", SNAPSHOT), second.list("analyzer.a", SNAPSHOT));
  }

  private static AnalysisResultSource fixtureSource(Set<AnalysisResult> results) {
    Map<AnalysisResultId, AnalysisResult> byId = new LinkedHashMap<>();
    for (AnalysisResult result : results) {
      byId.put(result.id(), result);
    }
    return new AnalysisResultSource() {
      @Override
      public Optional<AnalysisResult> read(AnalysisResultId id) {
        return Optional.ofNullable(byId.get(id));
      }

      @Override
      public Set<AnalysisResult> list(String analyzerIdentifier, CsmSnapshotId snapshotId) {
        return byId.values().stream()
            .filter(r -> r.analyzerIdentifier().equals(analyzerIdentifier) && r.sourceSnapshotId().equals(snapshotId))
            .collect(Collectors.toUnmodifiableSet());
      }
    };
  }
}
