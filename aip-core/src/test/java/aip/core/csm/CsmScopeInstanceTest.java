package aip.core.csm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** {@link CsmScopeInstance} tests. */
class CsmScopeInstanceTest {

  @Test
  void wholeRepositoryIsUnanchored() {
    CsmScopeInstance instance = CsmScopeInstance.wholeRepository();
    assertFalse(instance.isAnchored());
    assertTrue(instance.anchorElementId().isEmpty());
  }

  @Test
  void anchoredAtCarriesTheAnchorElementId() {
    CsmElementId id = new CsmElementId("csm:module-a");
    CsmScopeInstance instance = CsmScopeInstance.anchoredAt(id);
    assertTrue(instance.isAnchored());
    assertEquals(id, instance.anchorElementId().orElseThrow());
  }

  @Test
  void differentAnchorsAreDistinguishable() {
    CsmScopeInstance a = CsmScopeInstance.anchoredAt(new CsmElementId("csm:a"));
    CsmScopeInstance b = CsmScopeInstance.anchoredAt(new CsmElementId("csm:b"));
    assertNotEquals(a, b);
  }

  @Test
  void wholeRepositoryInstancesAreEqual() {
    assertEquals(CsmScopeInstance.wholeRepository(), CsmScopeInstance.wholeRepository());
  }
}
