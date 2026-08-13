package aip.analysis;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Registration and identifier-based lookup of {@link Analyzer}s, per
 * {@code Analyzer declares identifier, version, and scope}.
 *
 * <p>At most one Analyzer may be registered per identifier —
 * registering a second Analyzer for an already-registered identifier
 * is rejected rather than silently overwriting the first, mirroring
 * {@code aip-csm-builder}'s own {@code EvidenceKindMapperRegistry}
 * precedent. {@link #registered()} returns Analyzers ordered by
 * identifier ({@link TreeMap}'s natural ordering) so {@link
 * AnalysisOrchestrator}'s own dispatch order is deterministic and
 * reproducible across runs — a convenience for stable output ordering,
 * not something {@code Analyzer Execution Order Independence} itself
 * requires, since no Analyzer's own result may depend on that order
 * regardless.
 *
 * <p>Accepts no declared ordering or dependency between Analyzers, per
 * {@code No declared dependency between Analyzers is honored} — this
 * registry's own API has no method through which one could even be
 * expressed.
 */
public final class AnalyzerRegistry {

  private final TreeMap<String, Analyzer> byIdentifier = new TreeMap<>();

  public void register(Analyzer analyzer) {
    Objects.requireNonNull(analyzer, "analyzer");
    String identifier = analyzer.identifier();
    Objects.requireNonNull(identifier, "analyzer.identifier()");
    if (identifier.isBlank()) {
      throw new IllegalArgumentException("Analyzer identifier must not be blank");
    }
    if (byIdentifier.containsKey(identifier)) {
      throw new IllegalStateException(
          "an Analyzer is already registered for identifier " + identifier + "; registering a"
              + " second Analyzer for the same identifier is not permitted");
    }
    byIdentifier.put(identifier, analyzer);
  }

  public Optional<Analyzer> lookup(String identifier) {
    Objects.requireNonNull(identifier, "identifier");
    return Optional.ofNullable(byIdentifier.get(identifier));
  }

  /** Every registered Analyzer, ordered by identifier. */
  public Collection<Analyzer> registered() {
    return byIdentifier.values();
  }
}
