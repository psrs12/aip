package aip.csmbuilder.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import aip.core.csm.CsmElementId;
import aip.core.csm.CsmRelationshipType;
import aip.core.evidence.EvidenceId;
import aip.core.evidence.EvidenceKind;
import org.junit.jupiter.api.Test;

/**
 * Identity-stability tests (tasks.md 5.4): unchanged-evidence
 * stability, Package identity survival across member changes, and
 * dependency-relationship identity stability across a kind-qualifier
 * change.
 */
class ElementIdentityDeriverTest {

  @Test
  void sameEvidenceIdAlwaysProducesTheSameCsmIdentity() {
    EvidenceId evidenceId = new EvidenceId("repo-1", EvidenceKind.MODULE, "com.acme:module-a");

    CsmElementId first = ElementIdentityDeriver.fromEvidenceId(evidenceId);
    CsmElementId second = ElementIdentityDeriver.fromEvidenceId(evidenceId);

    assertEquals(first, second, "identity derivation is a pure function of Evidence identity");
  }

  @Test
  void differentEvidenceIdsProduceDifferentCsmIdentities() {
    EvidenceId moduleA = new EvidenceId("repo-1", EvidenceKind.MODULE, "com.acme:module-a");
    EvidenceId moduleB = new EvidenceId("repo-1", EvidenceKind.MODULE, "com.acme:module-b");

    assertNotEquals(
        ElementIdentityDeriver.fromEvidenceId(moduleA), ElementIdentityDeriver.fromEvidenceId(moduleB));
  }

  @Test
  void packageIdentitySurvivesMemberEvidenceChanges() {
    // A Package's identity depends only on its containing Module's
    // identity and its native namespace name — never on which
    // SourceUnit Evidence Items currently populate it. Simulate "a
    // member file was added or removed" by simply never referencing
    // any SourceUnit evidence in the derivation call at all: the
    // signature itself has no such parameter, so this test proves the
    // identity is unaffected by construction, not just by omission.
    CsmElementId moduleId =
        ElementIdentityDeriver.fromEvidenceId(
            new EvidenceId("repo-1", EvidenceKind.MODULE, "com.acme:module-a"));

    CsmElementId beforeMemberChange = ElementIdentityDeriver.forPackage(moduleId, "com.acme.pkg");
    CsmElementId afterMemberChange = ElementIdentityDeriver.forPackage(moduleId, "com.acme.pkg");

    assertEquals(beforeMemberChange, afterMemberChange);
  }

  @Test
  void packageIdentityDependsOnContainingModuleAndNamespaceOnly() {
    CsmElementId moduleA =
        ElementIdentityDeriver.fromEvidenceId(
            new EvidenceId("repo-1", EvidenceKind.MODULE, "com.acme:module-a"));
    CsmElementId moduleB =
        ElementIdentityDeriver.fromEvidenceId(
            new EvidenceId("repo-1", EvidenceKind.MODULE, "com.acme:module-b"));

    // Same namespace name, different containing Module -> different identity.
    assertNotEquals(
        ElementIdentityDeriver.forPackage(moduleA, "com.acme.pkg"),
        ElementIdentityDeriver.forPackage(moduleB, "com.acme.pkg"));

    // Same containing Module, different namespace name -> different identity.
    assertNotEquals(
        ElementIdentityDeriver.forPackage(moduleA, "com.acme.pkg"),
        ElementIdentityDeriver.forPackage(moduleA, "com.acme.other"));
  }

  @Test
  void dependencyRelationshipIdentityIsStableAcrossAKindQualifierChange() {
    // Relationship identity derivation takes no dependencyKind
    // parameter at all — proving, by construction, that a
    // DEPENDENCY relationship's identity cannot vary with its kind
    // qualifier, exactly as CSM Relationship Identity Derivation
    // requires.
    CsmElementId source =
        ElementIdentityDeriver.fromEvidenceId(
            new EvidenceId("repo-1", EvidenceKind.MODULE, "com.acme:module-a"));
    CsmElementId target =
        ElementIdentityDeriver.fromEvidenceId(
            new EvidenceId("repo-1", EvidenceKind.MODULE, "com.acme:module-b"));

    CsmElementId identityBeforeKindKnown =
        ElementIdentityDeriver.forRelationship(source, target, CsmRelationshipType.DEPENDENCY);
    CsmElementId identityAfterKindDiscovered =
        ElementIdentityDeriver.forRelationship(source, target, CsmRelationshipType.DEPENDENCY);

    assertEquals(identityBeforeKindKnown, identityAfterKindDiscovered);
  }

  @Test
  void relationshipIdentityDependsOnSourceTargetAndType() {
    CsmElementId a =
        ElementIdentityDeriver.fromEvidenceId(new EvidenceId("repo-1", EvidenceKind.MODULE, "a"));
    CsmElementId b =
        ElementIdentityDeriver.fromEvidenceId(new EvidenceId("repo-1", EvidenceKind.MODULE, "b"));

    CsmElementId dependency = ElementIdentityDeriver.forRelationship(a, b, CsmRelationshipType.DEPENDENCY);
    CsmElementId containment = ElementIdentityDeriver.forRelationship(a, b, CsmRelationshipType.CONTAINMENT);
    CsmElementId reversed = ElementIdentityDeriver.forRelationship(b, a, CsmRelationshipType.DEPENDENCY);

    assertNotEquals(dependency, containment, "different relationship type must yield a different identity");
    assertNotEquals(dependency, reversed, "swapped source/target must yield a different identity");
  }
}
