package aip.csmbuilder.identity;

import aip.core.csm.CsmElementId;
import aip.core.csm.CsmRelationshipType;
import aip.core.evidence.EvidenceId;
import java.util.Objects;

/**
 * Pure, deterministic functions from Evidence identity (or other CSM
 * identities) to CSM identity, per {@code CSM Element Identity
 * Derivation} and {@code CSM Relationship Identity Derivation}.
 *
 * <p>Every method here is a pure function: the same input always
 * produces the same output, with no I/O, no randomness, and no
 * dependency on anything but its arguments — this is what lets CSM
 * identity inherit Repository Evidence identity's own rename-stability
 * guarantee "for free" (see {@code openspec/changes/implement-csm-builder/design.md}
 * Decision 3).
 */
public final class ElementIdentityDeriver {

  private ElementIdentityDeriver() {}

  /**
   * The CSM identity for an entity with a 1:1 relationship to a single
   * Evidence Item (Repository, Project, Module, Type, Method): a
   * stable transformation of that Evidence Item's own identity, which
   * changes if, and only if, the Evidence identity changes.
   */
  public static CsmElementId fromEvidenceId(EvidenceId evidenceId) {
    Objects.requireNonNull(evidenceId, "evidenceId");
    return new CsmElementId("csm:" + evidenceId);
  }

  /**
   * A {@code Package} element's CSM identity: a function of its
   * containing Module's CSM identity and its native namespace name —
   * independent of which specific {@code SourceUnit} Evidence Items
   * currently populate it, so the identity survives individual member
   * files being added or removed (see {@code CSM Element Identity
   * Derivation}).
   */
  public static CsmElementId forPackage(CsmElementId containingModuleId, String nativeNamespaceName) {
    Objects.requireNonNull(containingModuleId, "containingModuleId");
    Objects.requireNonNull(nativeNamespaceName, "nativeNamespaceName");
    if (nativeNamespaceName.isBlank()) {
      throw new IllegalArgumentException("nativeNamespaceName must not be blank");
    }
    return new CsmElementId("csm:package:" + containingModuleId + ":" + nativeNamespaceName);
  }

  /**
   * A CSM relationship's identity: a function of its source entity
   * identity, target entity identity, and relationship type —
   * deliberately excluding a dependency relationship's kind qualifier,
   * so the identity is stable even when the kind qualifier changes
   * between runs (see {@code CSM Relationship Identity Derivation}).
   */
  public static CsmElementId forRelationship(
      CsmElementId sourceId, CsmElementId targetId, CsmRelationshipType type) {
    Objects.requireNonNull(sourceId, "sourceId");
    Objects.requireNonNull(targetId, "targetId");
    Objects.requireNonNull(type, "type");
    return new CsmElementId("csm:rel:" + type + ":" + sourceId + "->" + targetId);
  }
}
