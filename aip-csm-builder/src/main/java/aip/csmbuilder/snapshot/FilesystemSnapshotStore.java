package aip.csmbuilder.snapshot;

import aip.core.csm.CsmElement;
import aip.core.csm.CsmRelationship;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

/**
 * The filesystem-based {@link SnapshotStore} implementation, per {@code
 * design.md} Decision 2 (tasks.md 15.2, 15.3): one immutable directory
 * per snapshot, under {@code <root>/<repository-identifier>/snapshot-<n>/},
 * containing:
 *
 * <ul>
 *   <li>{@code elements.tsv} — one {@link CsmElementCodec}-encoded line
 *       per constructed CSM element, in the order supplied.
 *   <li>{@code relationships.tsv} — one {@link CsmRelationshipCodec}-encoded
 *       line per constructed CSM relationship.
 *   <li>{@code manifest.tsv} — the Snapshot Manifest, one {@link
 *       SnapshotManifestEntryCodec}-encoded line per element, its
 *       content location pointing back into {@code elements.tsv} by
 *       line number.
 *   <li>{@code snapshot.properties} — the snapshot-level construction
 *       timestamp.
 * </ul>
 *
 * <p>{@code repositoryIdentifier} is not assumed to be filesystem-path
 * safe (a real repository identifier may contain characters a
 * directory name cannot, e.g. {@code /}) — it is URL-encoded into its
 * directory name, a simple, standard-library-only, collision-free
 * transform. This is purely a directory-naming detail; {@link
 * #sequenceNumbers} and {@link #read} always accept and return the
 * original, un-encoded identifier.
 */
public final class FilesystemSnapshotStore implements SnapshotStore {

  private static final String SNAPSHOT_DIR_PREFIX = "snapshot-";
  private static final String ELEMENTS_FILE = "elements.tsv";
  private static final String RELATIONSHIPS_FILE = "relationships.tsv";
  private static final String MANIFEST_FILE = "manifest.tsv";
  private static final String PROPERTIES_FILE = "snapshot.properties";
  private static final String CONSTRUCTION_TIMESTAMP_KEY = "constructionTimestamp";

  private final Path root;

  public FilesystemSnapshotStore(Path root) {
    this.root = Objects.requireNonNull(root, "root");
  }

  @Override
  public synchronized Snapshot write(
      String repositoryIdentifier, SnapshotContent content, Instant constructionTimestamp) {
    Objects.requireNonNull(repositoryIdentifier, "repositoryIdentifier");
    Objects.requireNonNull(content, "content");
    Objects.requireNonNull(constructionTimestamp, "constructionTimestamp");

    long sequenceNumber = nextSequenceNumber(repositoryIdentifier);
    Path snapshotDir = snapshotDirectory(repositoryIdentifier, sequenceNumber);
    try {
      Files.createDirectories(snapshotDir);

      List<String> elementLines = content.elements().stream().map(CsmElementCodec::encode).toList();
      writeLines(snapshotDir.resolve(ELEMENTS_FILE), elementLines);

      List<String> relationshipLines =
          content.relationships().stream().map(CsmRelationshipCodec::encode).toList();
      writeLines(snapshotDir.resolve(RELATIONSHIPS_FILE), relationshipLines);

      List<SnapshotManifestEntry> manifestEntries = new ArrayList<>();
      for (int i = 0; i < content.elements().size(); i++) {
        CsmElement element = content.elements().get(i);
        MapperAttribution attribution = content.attributions().get(element.id());
        String contentLocation = ELEMENTS_FILE + ":" + (i + 1);
        manifestEntries.add(
            new SnapshotManifestEntry(
                element.id(),
                attribution.mapperIdentifier(),
                attribution.mapperVersion(),
                element.provenance().sourceReference(),
                contentLocation));
      }
      writeLines(
          snapshotDir.resolve(MANIFEST_FILE),
          manifestEntries.stream().map(SnapshotManifestEntryCodec::encode).toList());

      Properties properties = new Properties();
      properties.setProperty(CONSTRUCTION_TIMESTAMP_KEY, constructionTimestamp.toString());
      try (var out = Files.newOutputStream(snapshotDir.resolve(PROPERTIES_FILE))) {
        properties.store(out, "CSM Builder snapshot metadata");
      }

      return new Snapshot(
          repositoryIdentifier,
          sequenceNumber,
          constructionTimestamp,
          content.elements(),
          content.relationships(),
          new SnapshotManifest(manifestEntries));
    } catch (IOException e) {
      throw new UncheckedIOException("failed to write snapshot to " + snapshotDir, e);
    }
  }

  @Override
  public List<Long> sequenceNumbers(String repositoryIdentifier) {
    Objects.requireNonNull(repositoryIdentifier, "repositoryIdentifier");
    Path repositoryDir = repositoryDirectory(repositoryIdentifier);
    if (!Files.isDirectory(repositoryDir)) {
      return List.of();
    }
    try (var entries = Files.list(repositoryDir)) {
      return entries
          .map(path -> path.getFileName().toString())
          .filter(name -> name.startsWith(SNAPSHOT_DIR_PREFIX))
          .map(name -> Long.parseLong(name.substring(SNAPSHOT_DIR_PREFIX.length())))
          .sorted()
          .toList();
    } catch (IOException e) {
      throw new UncheckedIOException("failed to list snapshots under " + repositoryDir, e);
    }
  }

  @Override
  public Optional<Snapshot> read(String repositoryIdentifier, long sequenceNumber) {
    Objects.requireNonNull(repositoryIdentifier, "repositoryIdentifier");
    Path snapshotDir = snapshotDirectory(repositoryIdentifier, sequenceNumber);
    if (!Files.isDirectory(snapshotDir)) {
      return Optional.empty();
    }
    try {
      Properties properties = new Properties();
      try (var in = Files.newInputStream(snapshotDir.resolve(PROPERTIES_FILE))) {
        properties.load(in);
      }
      Instant constructionTimestamp = Instant.parse(properties.getProperty(CONSTRUCTION_TIMESTAMP_KEY));

      List<CsmElement> elements =
          readLines(snapshotDir.resolve(ELEMENTS_FILE)).stream().map(CsmElementCodec::decode).toList();
      List<CsmRelationship> relationships =
          readLines(snapshotDir.resolve(RELATIONSHIPS_FILE)).stream()
              .map(CsmRelationshipCodec::decode)
              .toList();
      List<SnapshotManifestEntry> manifestEntries =
          readLines(snapshotDir.resolve(MANIFEST_FILE)).stream()
              .map(SnapshotManifestEntryCodec::decode)
              .toList();

      return Optional.of(
          new Snapshot(
              repositoryIdentifier,
              sequenceNumber,
              constructionTimestamp,
              elements,
              relationships,
              new SnapshotManifest(manifestEntries)));
    } catch (IOException e) {
      throw new UncheckedIOException("failed to read snapshot from " + snapshotDir, e);
    }
  }

  private long nextSequenceNumber(String repositoryIdentifier) {
    List<Long> existing = sequenceNumbers(repositoryIdentifier);
    return existing.isEmpty() ? 1L : existing.get(existing.size() - 1) + 1;
  }

  private Path repositoryDirectory(String repositoryIdentifier) {
    return root.resolve(PathSafe.encode(repositoryIdentifier));
  }

  private Path snapshotDirectory(String repositoryIdentifier, long sequenceNumber) {
    return repositoryDirectory(repositoryIdentifier).resolve(SNAPSHOT_DIR_PREFIX + sequenceNumber);
  }

  private static void writeLines(Path file, List<String> lines) throws IOException {
    // A zero-line file (no elements, or no relationships) is
    // legitimate - Files.write with an empty list still creates the
    // (empty) file, keeping the snapshot's shape uniform regardless of
    // content.
    Files.write(file, lines, StandardCharsets.UTF_8);
  }

  private static List<String> readLines(Path file) throws IOException {
    List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
    return lines.isEmpty() ? List.of() : lines.stream().filter(line -> !line.isEmpty()).toList();
  }
}
