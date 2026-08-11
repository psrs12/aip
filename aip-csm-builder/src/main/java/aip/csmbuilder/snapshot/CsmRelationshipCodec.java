package aip.csmbuilder.snapshot;

import aip.core.csm.Confidence;
import aip.core.csm.CsmElementId;
import aip.core.csm.CsmRelationship;
import aip.core.csm.CsmRelationshipType;
import aip.core.csm.DependencyKind;
import aip.core.csm.NativeAttributes;
import aip.core.csm.ProvenanceCategory;
import aip.core.csm.ProvenanceRecord;
import java.time.Instant;
import java.util.Optional;

/**
 * One-line tab-delimited encode/decode for {@link CsmRelationship},
 * used by {@link FilesystemSnapshotStore} for its {@code
 * relationships.tsv} content file. Unlike {@link CsmElementCodec},
 * {@link CsmRelationship} is a single record type (not a sealed
 * hierarchy), so no kind-based branching is needed.
 */
final class CsmRelationshipCodec {

  private CsmRelationshipCodec() {}

  static String encode(CsmRelationship relationship) {
    return String.join(
        "\t",
        relationship.type().name(),
        TextEncoding.escape(relationship.id().value()),
        TextEncoding.escape(relationship.sourceId().value()),
        TextEncoding.encodeOptional(relationship.targetId().map(CsmElementId::value)),
        relationship.provenance().category().name(),
        TextEncoding.escape(relationship.provenance().sourceReference()),
        relationship.provenance().timestamp().toString(),
        TextEncoding.encodeOptional(relationship.provenance().confidence().map(Enum::name)),
        TextEncoding.encodeMap(relationship.nativeAttributes().asMap()),
        TextEncoding.encodeOptional(relationship.dependencyKind().map(Enum::name)));
  }

  static CsmRelationship decode(String line) {
    String[] f = line.split("\t", -1);
    CsmRelationshipType type = CsmRelationshipType.valueOf(f[0]);
    CsmElementId id = new CsmElementId(TextEncoding.unescape(f[1]));
    CsmElementId sourceId = new CsmElementId(TextEncoding.unescape(f[2]));
    Optional<CsmElementId> targetId = TextEncoding.decodeOptional(f[3]).map(CsmElementId::new);
    ProvenanceCategory category = ProvenanceCategory.valueOf(f[4]);
    String sourceReference = TextEncoding.unescape(f[5]);
    Instant timestamp = Instant.parse(f[6]);
    Optional<Confidence> confidence = TextEncoding.decodeOptional(f[7]).map(Confidence::valueOf);
    ProvenanceRecord provenance = new ProvenanceRecord(category, sourceReference, timestamp, confidence);
    NativeAttributes nativeAttributes = NativeAttributes.of(TextEncoding.decodeMap(f[8]));
    Optional<DependencyKind> dependencyKind = TextEncoding.decodeOptional(f[9]).map(DependencyKind::valueOf);

    return new CsmRelationship(id, type, sourceId, targetId, provenance, nativeAttributes, dependencyKind);
  }
}
