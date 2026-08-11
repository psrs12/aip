package aip.core.evidence;

import java.util.Objects;
import java.util.Optional;

/**
 * An Evidence Item's discovery outcome: its status, and — required
 * only for {@link DiscoveryOutcomeStatus#PARTIAL} or
 * {@link DiscoveryOutcomeStatus#FAILED} — the {@link FailureReason}
 * that produced it (see {@code Failure Reason Taxonomy}: "A discovery
 * outcome of {@code partial} or {@code failed} SHALL be accompanied by
 * a failure reason").
 */
public record DiscoveryOutcome(DiscoveryOutcomeStatus status, Optional<FailureReason> failureReason) {

  public DiscoveryOutcome {
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(failureReason, "failureReason");
    boolean reasonRequired =
        status == DiscoveryOutcomeStatus.PARTIAL || status == DiscoveryOutcomeStatus.FAILED;
    if (reasonRequired && failureReason.isEmpty()) {
      throw new IllegalArgumentException(
          "a PARTIAL or FAILED discovery outcome SHALL be accompanied by a failure reason"
              + " (Failure Reason Taxonomy)");
    }
    if (!reasonRequired && failureReason.isPresent()) {
      throw new IllegalArgumentException(
          "a COMPLETE discovery outcome SHALL NOT carry a failure reason");
    }
  }

  public static DiscoveryOutcome complete() {
    return new DiscoveryOutcome(DiscoveryOutcomeStatus.COMPLETE, Optional.empty());
  }

  public static DiscoveryOutcome partial(FailureReason reason) {
    return new DiscoveryOutcome(DiscoveryOutcomeStatus.PARTIAL, Optional.of(reason));
  }

  public static DiscoveryOutcome failed(FailureReason reason) {
    return new DiscoveryOutcome(DiscoveryOutcomeStatus.FAILED, Optional.of(reason));
  }
}
