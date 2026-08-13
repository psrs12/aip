package aip.core.csm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

/** {@link CsmScope} tests, per {@code Analysis Scope Declaration}. */
class CsmScopeTest {

  @Test
  void rejectsEmptyKindAndRelationshipDeclaration() {
    assertThrows(IllegalArgumentException.class, () -> CsmScope.of(Set.of(), Set.of()));
  }

  @Test
  void declaresAtLeastOneKindOrRelationshipType() {
    CsmScope kindOnly = CsmScope.ofEntityKinds(Set.of(CsmEntityKind.MODULE));
    assertTrue(kindOnly.declaresEntityKind(CsmEntityKind.MODULE));
    assertFalse(kindOnly.declaresEntityKind(CsmEntityKind.PACKAGE));

    CsmScope relationshipOnly = CsmScope.ofRelationshipTypes(Set.of(CsmRelationshipType.DEPENDENCY));
    assertTrue(relationshipOnly.declaresRelationshipType(CsmRelationshipType.DEPENDENCY));
  }

  @Test
  void isUnanchoredByDefault() {
    CsmScope scope = CsmScope.ofEntityKinds(Set.of(CsmEntityKind.MODULE));
    assertTrue(scope.containmentAnchor().isEmpty());
  }

  @Test
  void anchoredAtReturnsANewScopeWithTheAnchorSet() {
    CsmScope scope = CsmScope.ofEntityKinds(Set.of(CsmEntityKind.TYPE)).anchoredAt(CsmEntityKind.MODULE);
    assertEquals(CsmEntityKind.MODULE, scope.containmentAnchor().orElseThrow());
  }

  @Test
  void withNativeAttributePredicateReturnsANewScopeWithThePredicateSet() {
    CsmScope scope =
        CsmScope.ofEntityKinds(Set.of(CsmEntityKind.MODULE))
            .withNativeAttributePredicate(attrs -> attrs.get("ecosystem").isPresent());
    assertTrue(scope.nativeAttributePredicate().isPresent());
  }
}
