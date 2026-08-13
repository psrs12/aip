package aip.findings.test.fixtures;

import aip.core.csm.EvaluationIdentity;
import aip.core.csm.Finding;
import aip.findings.FindingStore;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A test-only, in-memory {@link FindingStore} implementation —
 * concrete persistence technology is deliberately deferred, so
 * production code never chooses one; this exists only so this
 * module's own tests can exercise the {@link FindingStore} abstraction
 * end to end.
 */
public final class InMemoryFindingStore implements FindingStore {

  private final Map<EvaluationIdentity, Finding> byId = new LinkedHashMap<>();

  @Override
  public Finding write(Finding finding) {
    Objects.requireNonNull(finding, "finding");
    if (byId.containsKey(finding.id())) {
      throw new IllegalStateException(
          "a Finding is already stored for identity " + finding.id() + "; a store SHALL NOT overwrite a"
              + " previously written Finding");
    }
    byId.put(finding.id(), finding);
    return finding;
  }

  @Override
  public Optional<Finding> read(EvaluationIdentity id) {
    Objects.requireNonNull(id, "id");
    return Optional.ofNullable(byId.get(id));
  }
}
