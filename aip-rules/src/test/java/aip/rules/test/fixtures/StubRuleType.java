package aip.rules.test.fixtures;

import aip.core.csm.AnalysisResult;
import aip.core.csm.AnalysisView;
import aip.core.csm.CsmScope;
import aip.core.csm.CsmScopeInstance;
import aip.rules.Rule;
import aip.rules.RuleType;
import aip.rules.RuleTypeEvaluation;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/** A configurable, deterministic test-only {@link RuleType}. */
public final class StubRuleType implements RuleType {

  /** The full context passed to a stub's evaluation function. */
  public record Context(
      Rule rule, AnalysisView view, CsmScopeInstance scopeInstance, Map<String, Set<AnalysisResult>> consumedAnalysisResults) {}

  private final String identifier;
  private final int version;
  private final Set<String> requiredAnalyzerIdentifiers;
  private final CsmScope scope;
  private final Function<Context, RuleTypeEvaluation> evaluationFunction;

  public StubRuleType(String identifier, int version, CsmScope scope) {
    this(identifier, version, Set.of(), scope, ctx -> RuleTypeEvaluation.pass(ctx.scopeInstance().toString()));
  }

  public StubRuleType(
      String identifier,
      int version,
      Set<String> requiredAnalyzerIdentifiers,
      CsmScope scope,
      Function<Context, RuleTypeEvaluation> evaluationFunction) {
    this.identifier = Objects.requireNonNull(identifier, "identifier");
    this.version = version;
    this.requiredAnalyzerIdentifiers = Set.copyOf(requiredAnalyzerIdentifiers);
    this.scope = Objects.requireNonNull(scope, "scope");
    this.evaluationFunction = Objects.requireNonNull(evaluationFunction, "evaluationFunction");
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
  public Set<String> requiredAnalyzerIdentifiers() {
    return requiredAnalyzerIdentifiers;
  }

  @Override
  public CsmScope scope() {
    return scope;
  }

  @Override
  public RuleTypeEvaluation evaluate(
      Rule rule, AnalysisView view, CsmScopeInstance scopeInstance, Map<String, Set<AnalysisResult>> consumedAnalysisResults) {
    return evaluationFunction.apply(new Context(rule, view, scopeInstance, consumedAnalysisResults));
  }
}
