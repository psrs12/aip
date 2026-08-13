package aip.findings;

import aip.core.csm.EvaluationIdentity;
import aip.core.csm.Finding;
import java.util.Optional;

/**
 * The Finding Store abstraction: writes and retrieves {@link
 * Finding}s, per {@code Findings Are Durable, Individually
 * Identifiable Artifacts} — mirroring {@code aip-rules}'s own {@code
 * RuleEvaluationResultStore} precedent (per {@code
 * implement-finding-model/design.md} Decision 8, <strong>not</strong>
 * promoted to {@code aip-core} — the artifact moved, the store
 * abstraction did not).
 *
 * <p>Concrete persistence technology is deliberately not chosen here.
 */
public interface FindingStore {

  /** Writes {@code finding} as a new, immutable artifact. Never overwrites a previously written Finding. */
  Finding write(Finding finding);

  /** The Finding with the given Evaluation Identity, if one was written. */
  Optional<Finding> read(EvaluationIdentity id);
}
