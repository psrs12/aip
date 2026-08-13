package aip.core.csm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * {@link AnalysisResult} tests, per {@code Analyzer Contract},
 * {@code Analysis Result Traceability}, and {@code Analysis Result
 * Payload Is Analyzer-Defined}.
 */
class AnalysisResultTest {

  private static final CsmSnapshotId SNAPSHOT = new CsmSnapshotId("repo", 1);
  private static final CsmScopeInstance SCOPE_INSTANCE = CsmScopeInstance.wholeRepository();

  @Test
  void ofComputesIdentityFromTheSameTraceabilityComponents() {
    AnalysisResult result = AnalysisResult.of("analyzer.a", 1, SNAPSHOT, SCOPE_INSTANCE, "payload");
    assertEquals(AnalysisResultId.of("analyzer.a", 1, SNAPSHOT, SCOPE_INSTANCE), result.id());
  }

  @Test
  void retainsTraceabilityToProducingAnalyzerAndSourceSnapshot() {
    AnalysisResult result = AnalysisResult.of("analyzer.a", 3, SNAPSHOT, SCOPE_INSTANCE, "payload");
    assertEquals("analyzer.a", result.analyzerIdentifier());
    assertEquals(3, result.analyzerVersion());
    assertEquals(SNAPSHOT, result.sourceSnapshotId());
    assertEquals(SCOPE_INSTANCE, result.scopeInstance());
  }

  @Test
  void payloadIsOpaqueAndReturnedUnmodified() {
    Object payload = new Object();
    AnalysisResult result = AnalysisResult.of("analyzer.a", 1, SNAPSHOT, SCOPE_INSTANCE, payload);
    assertEquals(payload, result.payload());
  }

  @Test
  void differentlyShapedPayloadsAreBothAccepted() {
    AnalysisResult stringPayload = AnalysisResult.of("analyzer.a", 1, SNAPSHOT, SCOPE_INSTANCE, "text");
    AnalysisResult listPayload =
        AnalysisResult.of("analyzer.b", 1, SNAPSHOT, SCOPE_INSTANCE, java.util.List.of(1, 2, 3));
    assertEquals("text", stringPayload.payload());
    assertEquals(java.util.List.of(1, 2, 3), listPayload.payload());
  }

  @Test
  void rejectsNullPayload() {
    assertThrows(
        NullPointerException.class,
        () -> new AnalysisResult(
            AnalysisResultId.of("analyzer.a", 1, SNAPSHOT, SCOPE_INSTANCE),
            "analyzer.a",
            1,
            SNAPSHOT,
            SCOPE_INSTANCE,
            null));
  }

  @Test
  void rejectsBlankAnalyzerIdentifier() {
    assertThrows(
        IllegalArgumentException.class,
        () -> AnalysisResult.of(" ", 1, SNAPSHOT, SCOPE_INSTANCE, "payload"));
  }
}
