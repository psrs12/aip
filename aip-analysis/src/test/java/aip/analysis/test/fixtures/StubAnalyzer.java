package aip.analysis.test.fixtures;

import aip.analysis.Analyzer;
import aip.core.csm.AnalysisView;
import aip.core.csm.CsmScope;
import aip.core.csm.CsmScopeInstance;
import java.util.Objects;
import java.util.function.BiFunction;

/** A configurable, deterministic test-only {@link Analyzer}. */
public final class StubAnalyzer implements Analyzer {

  private final String identifier;
  private final int version;
  private final CsmScope scope;
  private final BiFunction<AnalysisView, CsmScopeInstance, Object> payloadFunction;

  public StubAnalyzer(String identifier, int version, CsmScope scope) {
    this(identifier, version, scope, (view, instance) -> instance.toString());
  }

  public StubAnalyzer(
      String identifier,
      int version,
      CsmScope scope,
      BiFunction<AnalysisView, CsmScopeInstance, Object> payloadFunction) {
    this.identifier = Objects.requireNonNull(identifier, "identifier");
    this.version = version;
    this.scope = Objects.requireNonNull(scope, "scope");
    this.payloadFunction = Objects.requireNonNull(payloadFunction, "payloadFunction");
  }

  @Override
  public String identifier() {
    return identifier;
  }

  @Override
  public int version() {
    return version;
  }

  @Override
  public CsmScope scope() {
    return scope;
  }

  @Override
  public Object analyze(AnalysisView view, CsmScopeInstance scopeInstance) {
    return payloadFunction.apply(view, scopeInstance);
  }
}
