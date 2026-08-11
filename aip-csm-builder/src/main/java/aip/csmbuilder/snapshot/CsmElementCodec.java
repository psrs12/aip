package aip.csmbuilder.snapshot;

import aip.core.csm.ArchitectureComponentElement;
import aip.core.csm.Confidence;
import aip.core.csm.CsmElement;
import aip.core.csm.CsmElementId;
import aip.core.csm.CsmEntityKind;
import aip.core.csm.ExternalSystemElement;
import aip.core.csm.MethodElement;
import aip.core.csm.ModuleElement;
import aip.core.csm.NativeAttributes;
import aip.core.csm.PackageElement;
import aip.core.csm.ProjectElement;
import aip.core.csm.ProvenanceCategory;
import aip.core.csm.ProvenanceRecord;
import aip.core.csm.RepositoryElement;
import aip.core.csm.SourceLocation;
import aip.core.csm.TypeElement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * One-line tab-delimited encode/decode for every {@link CsmElement}
 * variant, used by {@link FilesystemSnapshotStore} for its {@code
 * elements.tsv} content file. Handles all eight {@link CsmEntityKind}
 * variants for completeness of this general-purpose codec, even though
 * CSM Builder itself only ever constructs a subset of them (see tasks.md
 * 24.4 — {@code ArchitectureComponentElement} is never produced by this
 * module).
 *
 * <p>Common columns (every kind): kind, id, name, provenance category,
 * provenance source reference, provenance timestamp, provenance
 * confidence (optional), native attributes (map). Kind-specific columns
 * follow: {@code TYPE}/{@code METHOD} add source-location file path and
 * position (both optional); {@code EXTERNAL_SYSTEM} adds system kind,
 * criticality, integration protocol, and owner (all optional); {@code
 * ARCHITECTURE_COMPONENT} adds its composition list.
 */
final class CsmElementCodec {

  private static final int COMMON_COLUMNS = 8;

  private CsmElementCodec() {}

  static String encode(CsmElement element) {
    List<String> fields = new ArrayList<>(COMMON_COLUMNS + 4);
    fields.add(element.kind().name());
    fields.add(TextEncoding.escape(element.id().value()));
    fields.add(TextEncoding.escape(element.name()));
    fields.add(element.provenance().category().name());
    fields.add(TextEncoding.escape(element.provenance().sourceReference()));
    fields.add(element.provenance().timestamp().toString());
    fields.add(TextEncoding.encodeOptional(element.provenance().confidence().map(Enum::name)));
    fields.add(TextEncoding.encodeMap(element.nativeAttributes().asMap()));

    switch (element) {
      case RepositoryElement ignored -> {}
      case ProjectElement ignored -> {}
      case ModuleElement ignored -> {}
      case PackageElement ignored -> {}
      case TypeElement type -> appendSourceLocation(fields, type.sourceLocation());
      case MethodElement method -> appendSourceLocation(fields, method.sourceLocation());
      case ExternalSystemElement external -> {
        fields.add(TextEncoding.encodeOptional(external.systemKind()));
        fields.add(TextEncoding.encodeOptional(external.criticality()));
        fields.add(TextEncoding.encodeOptional(external.integrationProtocol()));
        fields.add(TextEncoding.encodeOptional(external.owner()));
      }
      case ArchitectureComponentElement component ->
          fields.add(
              TextEncoding.encodeList(component.composition().stream().map(CsmElementId::value).toList()));
    }
    return String.join("\t", fields);
  }

  static CsmElement decode(String line) {
    String[] f = line.split("\t", -1);
    CsmEntityKind kind = CsmEntityKind.valueOf(f[0]);
    CsmElementId id = new CsmElementId(TextEncoding.unescape(f[1]));
    String name = TextEncoding.unescape(f[2]);
    ProvenanceCategory category = ProvenanceCategory.valueOf(f[3]);
    String sourceReference = TextEncoding.unescape(f[4]);
    Instant timestamp = Instant.parse(f[5]);
    Optional<Confidence> confidence = TextEncoding.decodeOptional(f[6]).map(Confidence::valueOf);
    ProvenanceRecord provenance = new ProvenanceRecord(category, sourceReference, timestamp, confidence);
    NativeAttributes nativeAttributes = NativeAttributes.of(TextEncoding.decodeMap(f[7]));

    return switch (kind) {
      case REPOSITORY -> new RepositoryElement(id, name, provenance, nativeAttributes);
      case PROJECT -> new ProjectElement(id, name, provenance, nativeAttributes);
      case MODULE -> new ModuleElement(id, name, provenance, nativeAttributes);
      case PACKAGE -> new PackageElement(id, name, provenance, nativeAttributes);
      case TYPE -> new TypeElement(id, name, provenance, nativeAttributes, decodeSourceLocation(f));
      case METHOD -> new MethodElement(id, name, provenance, nativeAttributes, decodeSourceLocation(f));
      case EXTERNAL_SYSTEM ->
          new ExternalSystemElement(
              id,
              name,
              provenance,
              nativeAttributes,
              TextEncoding.decodeOptional(f[COMMON_COLUMNS]),
              TextEncoding.decodeOptional(f[COMMON_COLUMNS + 1]),
              TextEncoding.decodeOptional(f[COMMON_COLUMNS + 2]),
              TextEncoding.decodeOptional(f[COMMON_COLUMNS + 3]));
      case ARCHITECTURE_COMPONENT ->
          new ArchitectureComponentElement(
              id,
              name,
              provenance,
              nativeAttributes,
              TextEncoding.decodeList(f[COMMON_COLUMNS]).stream().map(CsmElementId::new).toList());
    };
  }

  private static void appendSourceLocation(List<String> fields, Optional<SourceLocation> sourceLocation) {
    fields.add(TextEncoding.encodeOptional(sourceLocation.map(SourceLocation::filePath)));
    fields.add(TextEncoding.encodeOptional(sourceLocation.flatMap(SourceLocation::position)));
  }

  private static Optional<SourceLocation> decodeSourceLocation(String[] f) {
    Optional<String> filePath = TextEncoding.decodeOptional(f[COMMON_COLUMNS]);
    Optional<String> position = TextEncoding.decodeOptional(f[COMMON_COLUMNS + 1]);
    return filePath.map(path -> new SourceLocation(path, position));
  }
}
