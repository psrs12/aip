package aip.core.evidence;

import java.util.Objects;

/**
 * A relationship between two Evidence Items — containment, dependency,
 * or reference (see {@link EvidenceRelationshipType}).
 */
public record EvidenceRelationship(EvidenceRelationshipType type, EvidenceId sourceId, EvidenceId targetId) {

  public EvidenceRelationship {
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(sourceId, "sourceId");
    Objects.requireNonNull(targetId, "targetId");
  }
}
