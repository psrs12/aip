package aip.csmbuilder.snapshot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aip.core.csm.CsmElement;
import aip.core.csm.CsmElementId;
import aip.core.csm.CsmRelationship;
import aip.core.csm.CsmRelationshipType;
import aip.core.csm.ExternalSystemElement;
import aip.core.csm.ModuleElement;
import aip.core.csm.NativeAttributes;
import aip.core.csm.ProvenanceRecord;
import aip.core.csm.SourceLocation;
import aip.core.csm.TypeElement;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Snapshot Store tests (tasks.md 15.4), written against the {@link
 * SnapshotStore} abstraction (declared type), backed by {@link
 * FilesystemSnapshotStore} only as the one implementation this change
 * provides.
 */
class FilesystemSnapshotStoreTest {

  private static final Instant TIMESTAMP = Instant.parse("2026-01-01T00:00:00Z");

  @TempDir Path tempDir;

  @Test
  void writingThenReadingReturnsEquivalentContent() {
    SnapshotStore store = new FilesystemSnapshotStore(tempDir);
    SnapshotContent content = sampleContent("com.acme:module-a");

    Snapshot written = store.write("repo-1", content, TIMESTAMP);
    Optional<Snapshot> read = store.read("repo-1", written.sequenceNumber());

    assertTrue(read.isPresent());
    assertEquals(written.repositoryIdentifier(), read.get().repositoryIdentifier());
    assertEquals(written.sequenceNumber(), read.get().sequenceNumber());
    assertEquals(written.constructionTimestamp(), read.get().constructionTimestamp());
    assertEquals(content.elements(), read.get().elements());
    assertEquals(content.relationships(), read.get().relationships());
    assertEquals(content.elements().size(), read.get().manifest().size());
  }

  @Test
  void multipleRunsProduceDistinguishableSnapshots() {
    SnapshotStore store = new FilesystemSnapshotStore(tempDir);

    Snapshot first = store.write("repo-1", sampleContent("com.acme:module-a"), TIMESTAMP);
    Snapshot second = store.write("repo-1", sampleContent("com.acme:module-b"), TIMESTAMP.plusSeconds(60));

    assertEquals(1L, first.sequenceNumber());
    assertEquals(2L, second.sequenceNumber());
    assertEquals(List.of(1L, 2L), store.sequenceNumbers("repo-1"));
    assertTrue(
        first.elements().get(0).id() != second.elements().get(0).id()
            || !first.elements().get(0).equals(second.elements().get(0)));
  }

  @Test
  void priorSnapshotsRemainRetrievableAndUnaffectedByANewWrite() {
    SnapshotStore store = new FilesystemSnapshotStore(tempDir);

    Snapshot first = store.write("repo-1", sampleContent("com.acme:module-a"), TIMESTAMP);
    Optional<Snapshot> readBeforeSecondWrite = store.read("repo-1", first.sequenceNumber());

    store.write("repo-1", sampleContent("com.acme:module-b"), TIMESTAMP.plusSeconds(60));
    Optional<Snapshot> readAfterSecondWrite = store.read("repo-1", first.sequenceNumber());

    assertTrue(readBeforeSecondWrite.isPresent());
    assertTrue(readAfterSecondWrite.isPresent());
    assertEquals(readBeforeSecondWrite.get().elements(), readAfterSecondWrite.get().elements());
    assertEquals(readBeforeSecondWrite.get().relationships(), readAfterSecondWrite.get().relationships());
  }

  @Test
  void unknownRepositoryHasNoSequenceNumbers() {
    SnapshotStore store = new FilesystemSnapshotStore(tempDir);
    assertTrue(store.sequenceNumbers("no-such-repo").isEmpty());
  }

  @Test
  void unknownSequenceNumberIsAbsent() {
    SnapshotStore store = new FilesystemSnapshotStore(tempDir);
    store.write("repo-1", sampleContent("com.acme:module-a"), TIMESTAMP);

    assertTrue(store.read("repo-1", 99L).isEmpty());
  }

  @Test
  void readLatestReturnsTheMostRecentWrite() {
    SnapshotStore store = new FilesystemSnapshotStore(tempDir);
    store.write("repo-1", sampleContent("com.acme:module-a"), TIMESTAMP);
    Snapshot second = store.write("repo-1", sampleContent("com.acme:module-b"), TIMESTAMP.plusSeconds(60));

    Optional<Snapshot> latest = store.readLatest("repo-1");

    assertTrue(latest.isPresent());
    assertEquals(second.sequenceNumber(), latest.get().sequenceNumber());
  }

  @Test
  void repositoryIdentifierNeedNotBeFilesystemSafe() {
    SnapshotStore store = new FilesystemSnapshotStore(tempDir);
    String repositoryIdentifier = "github.com/acme/repo one";

    Snapshot written = store.write(repositoryIdentifier, sampleContent("com.acme:module-a"), TIMESTAMP);
    Optional<Snapshot> read = store.read(repositoryIdentifier, written.sequenceNumber());

    assertTrue(read.isPresent());
    assertEquals(repositoryIdentifier, read.get().repositoryIdentifier());
  }

  @Test
  void manifestContentLocationPointsIntoTheElementsFile() {
    SnapshotStore store = new FilesystemSnapshotStore(tempDir);
    SnapshotContent content = sampleContent("com.acme:module-a");

    Snapshot written = store.write("repo-1", content, TIMESTAMP);

    CsmElementId elementId = content.elements().get(0).id();
    Optional<SnapshotManifestEntry> entry = written.manifest().find(elementId);
    assertTrue(entry.isPresent());
    assertEquals("elements.tsv:1", entry.get().contentLocation());
    assertEquals("moduleMapper", entry.get().mapperIdentifier());
    assertEquals(1, entry.get().mapperVersion());
  }

  @Test
  void valuesContainingDelimiterCharactersRoundTripExactly() {
    // Native attribute values and names containing this format's own
    // structural characters (tab, '=', ';', ',', backslash, newline)
    // must still round-trip exactly.
    SnapshotStore store = new FilesystemSnapshotStore(tempDir);
    CsmElementId id = new CsmElementId("csm:module:tricky");
    ProvenanceRecord provenance = ProvenanceRecord.observed("repo-1:MODULE:tricky", TIMESTAMP);
    NativeAttributes attributes =
        NativeAttributes.of(
            Map.of("weird=key;with,chars", "value\twith\nnewline\\and=semi;colon,comma"));
    ModuleElement element = new ModuleElement(id, "tricky\tname;with=chars,here", provenance, attributes);
    SnapshotContent content =
        new SnapshotContent(
            List.of(element), List.of(), Map.of(id, new MapperAttribution("moduleMapper", 1)));

    Snapshot written = store.write("repo-1", content, TIMESTAMP);
    Snapshot read = store.read("repo-1", written.sequenceNumber()).orElseThrow();

    assertEquals(element, read.elements().get(0));
  }

  private SnapshotContent sampleContent(String moduleScopeKey) {
    CsmElementId moduleId = new CsmElementId("csm:module:" + moduleScopeKey);
    ProvenanceRecord moduleProvenance = ProvenanceRecord.observed("repo-1:MODULE:" + moduleScopeKey, TIMESTAMP);
    ModuleElement module = new ModuleElement(moduleId, moduleScopeKey, moduleProvenance, NativeAttributes.empty());

    CsmElementId typeId = new CsmElementId("csm:type:" + moduleScopeKey + ".MyType");
    ProvenanceRecord typeProvenance =
        ProvenanceRecord.observed("repo-1:SOURCE_UNIT:" + moduleScopeKey + ".MyType", TIMESTAMP);
    TypeElement type =
        new TypeElement(
            typeId,
            moduleScopeKey + ".MyType",
            typeProvenance,
            NativeAttributes.empty().with("nativeConstructKind", "class"),
            Optional.of(SourceLocation.of("src/main/java/MyType.java", "1:1-10:1")));

    CsmElementId externalId = new CsmElementId("csm:external:repo-1:com.thirdparty:lib");
    ProvenanceRecord externalProvenance =
        ProvenanceRecord.observed("repo-1:MANIFEST_DEPENDENCY_EDGE:edge-1", TIMESTAMP);
    ExternalSystemElement external =
        ExternalSystemElement.unresolved(externalId, "com.thirdparty:lib", externalProvenance);

    List<CsmElement> elements = List.of(module, type, external);
    Map<CsmElementId, MapperAttribution> attributions =
        Map.of(
            moduleId, new MapperAttribution("moduleMapper", 1),
            typeId, new MapperAttribution("typeMapper", 1),
            externalId, new MapperAttribution("EXTERNAL_SYSTEM", 1));

    CsmRelationship containment =
        CsmRelationship.of(
            new CsmElementId("csm:rel:CONTAINMENT:" + moduleId + "->" + typeId),
            CsmRelationshipType.CONTAINMENT,
            moduleId,
            typeId,
            moduleProvenance,
            NativeAttributes.empty());
    CsmRelationship integration =
        CsmRelationship.of(
            new CsmElementId("csm:rel:INTEGRATION:" + moduleId + "->" + externalId),
            CsmRelationshipType.INTEGRATION,
            moduleId,
            externalId,
            externalProvenance,
            NativeAttributes.empty());
    CsmRelationship exposure =
        CsmRelationship.withUnevidencedTarget(
            new CsmElementId("csm:rel:exposure:" + typeId),
            CsmRelationshipType.EXPOSURE_CONSUMPTION,
            typeId,
            typeProvenance,
            NativeAttributes.empty().with("structuralSummary", "GET /widgets"));

    return new SnapshotContent(elements, List.of(containment, integration, exposure), attributions);
  }
}
