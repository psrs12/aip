package aip.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.core.csm.CsmEntityKind;
import aip.core.csm.CsmScope;
import aip.rules.test.fixtures.StubRuleType;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** {@link RuleTypeRegistry} tests, per {@code Rule Type declares identifier, version, and inputs}. */
class RuleTypeRegistryTest {

  private static final CsmScope SCOPE = CsmScope.ofEntityKinds(Set.of(CsmEntityKind.MODULE));

  @Test
  void registersAndLooksUpByIdentifier() {
    RuleTypeRegistry registry = new RuleTypeRegistry();
    RuleType ruleType = new StubRuleType("ruletype.a", 1, SCOPE);
    registry.register(ruleType);

    assertEquals(ruleType, registry.lookup("ruletype.a").orElseThrow());
    assertTrue(registry.lookup("ruletype.unknown").isEmpty());
  }

  @Test
  void rejectsASecondRegistrationForTheSameIdentifier() {
    RuleTypeRegistry registry = new RuleTypeRegistry();
    registry.register(new StubRuleType("ruletype.a", 1, SCOPE));
    assertThrows(IllegalStateException.class, () -> registry.register(new StubRuleType("ruletype.a", 2, SCOPE)));
  }

  @Test
  void registeredIsOrderedByIdentifier() {
    RuleTypeRegistry registry = new RuleTypeRegistry();
    registry.register(new StubRuleType("ruletype.z", 1, SCOPE));
    registry.register(new StubRuleType("ruletype.a", 1, SCOPE));

    List<String> identifiers = registry.registered().stream().map(RuleType::identifier).toList();
    assertEquals(List.of("ruletype.a", "ruletype.z"), identifiers);
  }
}
