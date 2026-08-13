package aip.core.csm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

/**
 * {@link AnalysisResultId} tests, per {@code Deterministic Analysis
 * Result Identity}.
 */
class AnalysisResultIdTest {

  private static final CsmSnapshotId SNAPSHOT = new CsmSnapshotId("repo", 1);
  private static final CsmScopeInstance SCOPE_INSTANCE = CsmScopeInstance.wholeRepository();

  @Test
  void identicalInputsProduceIdenticalIdentity() {
    AnalysisResultId first = AnalysisResultId.of("analyzer.cycle-detector", 1, SNAPSHOT, SCOPE_INSTANCE);
    AnalysisResultId second = AnalysisResultId.of("analyzer.cycle-detector", 1, SNAPSHOT, SCOPE_INSTANCE);
    assertEquals(first, second);
  }

  @Test
  void differentAnalyzerVersionYieldsDistinguishableIdentity() {
    AnalysisResultId v1 = AnalysisResultId.of("analyzer.cycle-detector", 1, SNAPSHOT, SCOPE_INSTANCE);
    AnalysisResultId v2 = AnalysisResultId.of("analyzer.cycle-detector", 2, SNAPSHOT, SCOPE_INSTANCE);
    assertNotEquals(v1, v2);
  }

  @Test
  void differentSnapshotIdentityYieldsDistinguishableIdentity() {
    AnalysisResultId first = AnalysisResultId.of("analyzer.a", 1, SNAPSHOT, SCOPE_INSTANCE);
    AnalysisResultId second = AnalysisResultId.of("analyzer.a", 1, new CsmSnapshotId("repo", 2), SCOPE_INSTANCE);
    assertNotEquals(first, second);
  }

  @Test
  void differentScopeInstanceYieldsDistinguishableIdentity() {
    AnalysisResultId whole = AnalysisResultId.of("analyzer.a", 1, SNAPSHOT, CsmScopeInstance.wholeRepository());
    AnalysisResultId anchored =
        AnalysisResultId.of("analyzer.a", 1, SNAPSHOT, CsmScopeInstance.anchoredAt(new CsmElementId("csm:m1")));
    assertNotEquals(whole, anchored);
  }

  @Test
  void differentAnalyzerIdentifierYieldsDistinguishableIdentity() {
    AnalysisResultId a = AnalysisResultId.of("analyzer.a", 1, SNAPSHOT, SCOPE_INSTANCE);
    AnalysisResultId b = AnalysisResultId.of("analyzer.b", 1, SNAPSHOT, SCOPE_INSTANCE);
    assertNotEquals(a, b);
  }
}
