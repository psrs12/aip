package aip.csmbuilder.snapshot;

import aip.core.csm.CsmElementId;

/**
 * One-line tab-delimited encode/decode for {@link SnapshotManifestEntry},
 * used by {@link FilesystemSnapshotStore} for its {@code manifest.tsv}
 * content file.
 */
final class SnapshotManifestEntryCodec {

  private SnapshotManifestEntryCodec() {}

  static String encode(SnapshotManifestEntry entry) {
    return String.join(
        "\t",
        TextEncoding.escape(entry.elementId().value()),
        TextEncoding.escape(entry.mapperIdentifier()),
        Integer.toString(entry.mapperVersion()),
        TextEncoding.escape(entry.originatingEvidenceReference()),
        TextEncoding.escape(entry.contentLocation()));
  }

  static SnapshotManifestEntry decode(String line) {
    String[] f = line.split("\t", -1);
    return new SnapshotManifestEntry(
        new CsmElementId(TextEncoding.unescape(f[0])),
        TextEncoding.unescape(f[1]),
        Integer.parseInt(f[2]),
        TextEncoding.unescape(f[3]),
        TextEncoding.unescape(f[4]));
  }
}
