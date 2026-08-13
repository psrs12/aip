package aip.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.core.csm.CsmEntityKind;
import aip.core.csm.CsmScope;
import aip.rules.test.fixtures.StubRuleType;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * {@link RuleRegistry} tests, per {@code Rule configures a Rule Type
 * without code} and its own unregistered-Rule-Type rejection.
 */
class RuleRegistryTest {

  private static final CsmScope SCOPE = CsmScope.ofEntityKinds(Set.of(CsmEntityKind.MODULE));

  @Test
  void registersAndLooksUpByIdentifierWhenItsRuleTypeIsRegistered() {
    RuleTypeRegistry ruleTypeRegistry = new RuleTypeRegistry();
    ruleTypeRegistry.register(new StubRuleType("ruletype.a", 1, SCOPE));
    RuleRegistry ruleRegistry = new RuleRegistry(ruleTypeRegistry);

    Rule rule = new Rule("rule.a", 1, "ruletype.a", Map.of());
    ruleRegistry.register(rule);

    assertEquals(rule, ruleRegistry.lookup("rule.a").orElseThrow());
  }

  @Test
  void rejectsARuleReferencingAnUnregisteredRuleType() {
    RuleTypeRegistry ruleTypeRegistry = new RuleTypeRegistry();
    RuleRegistry ruleRegistry = new RuleRegistry(ruleTypeRegistry);

    Rule rule = new Rule("rule.a", 1, "ruletype.unregistered", Map.of());
    assertThrows(IllegalArgumentException.class, () -> ruleRegistry.register(rule));
  }

  @Test
  void rejectsASecondRegistrationForTheSameIdentifier() {
    RuleTypeRegistry ruleTypeRegistry = new RuleTypeRegistry();
    ruleTypeRegistry.register(new StubRuleType("ruletype.a", 1, SCOPE));
    RuleRegistry ruleRegistry = new RuleRegistry(ruleTypeRegistry);
    ruleRegistry.register(new Rule("rule.a", 1, "ruletype.a", Map.of()));

    assertThrows(
        IllegalStateException.class, () -> ruleRegistry.register(new Rule("rule.a", 2, "ruletype.a", Map.of())));
  }

  @Test
  void oneRuleTypeSupportsMultipleIndependentlyConfiguredRules() {
    RuleTypeRegistry ruleTypeRegistry = new RuleTypeRegistry();
    ruleTypeRegistry.register(new StubRuleType("ruletype.a", 1, SCOPE));
    RuleRegistry ruleRegistry = new RuleRegistry(ruleTypeRegistry);

    Rule ruleOne = new Rule("rule.one", 1, "ruletype.a", Map.of("threshold", 1));
    Rule ruleTwo = new Rule("rule.two", 1, "ruletype.a", Map.of("threshold", 2));
    ruleRegistry.register(ruleOne);
    ruleRegistry.register(ruleTwo);

    assertTrue(ruleRegistry.registered().containsAll(java.util.List.of(ruleOne, ruleTwo)));
  }
}
