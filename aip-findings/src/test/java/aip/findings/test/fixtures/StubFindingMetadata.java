package aip.findings.test.fixtures;

import aip.core.csm.FindingMetadata;

/**
 * A simple {@link FindingMetadata} implementation for use across test
 * classes — a {@code RuleEvaluationResult}'s payload implementing this
 * is what makes a {@code FAIL} outcome "qualify" for Finding
 * construction (`implement-finding-model/design.md` Decision 4).
 */
public record StubFindingMetadata(String category, String severity, String description, String impact)
    implements FindingMetadata {

  public static StubFindingMetadata of(String category, String severity) {
    return new StubFindingMetadata(category, severity, "stub description", "stub impact");
  }
}
