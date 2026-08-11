package aip.csmbuilder.snapshot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import aip.core.csm.ArchitectureComponentElement;
import aip.core.csm.Confidence;
import aip.core.csm.CsmElement;
import aip.core.csm.CsmElementId;
import aip.core.csm.ExternalSystemElement;
import aip.core.csm.MethodElement;
import aip.core.csm.ModuleElement;
import aip.core.csm.NativeAttributes;
import aip.core.csm.PackageElement;
import aip.core.csm.ProjectElement;
import aip.core.csm.ProvenanceRecord;
import aip.core.csm.RepositoryElement;
import aip.core.csm.SourceLocation;
import aip.core.csm.TypeElement;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Encode/decode round-trip coverage for every {@link CsmElement}
 * variant (tasks.md 15.3), including {@link ArchitectureComponentElement}
 * for this codec's own completeness even though CSM Builder never
 * constructs one (tasks.md 24.4).
 */
class CsmElementCodecTest {

  private static final Instant TIMESTAMP = Instant.parse("2026-01-01T00:00:00Z");
  private static final ProvenanceRecord OBSERVED = ProvenanceRecord.observed("repo-1:MODULE:x", TIMESTAMP);

  @Test
  void repositoryElementRoundTrips() {
    assertRoundTrips(new RepositoryElement(new CsmElementId("csm:1"), "repo-1", OBSERVED, NativeAttributes.empty()));
  }

  @Test
  void projectElementRoundTrips() {
    assertRoundTrips(new ProjectElement(new CsmElementId("csm:1"), "proj", OBSERVED, NativeAttributes.empty()));
  }

  @Test
  void moduleElementRoundTrips() {
    assertRoundTrips(
        new ModuleElement(
            new CsmElementId("csm:1"),
            "mod",
            OBSERVED,
            NativeAttributes.empty().with("key", "value")));
  }

  @Test
  void packageElementRoundTrips() {
    assertRoundTrips(new PackageElement(new CsmElementId("csm:1"), "com.acme.pkg", OBSERVED, NativeAttributes.empty()));
  }

  @Test
  void typeElementWithSourceLocationRoundTrips() {
    assertRoundTrips(
        new TypeElement(
            new CsmElementId("csm:1"),
            "MyType",
            OBSERVED,
            NativeAttributes.empty().with("nativeConstructKind", "class"),
            Optional.of(SourceLocation.of("src/MyType.java", "1:1-2:1"))));
  }

  @Test
  void typeElementWithoutSourceLocationRoundTrips() {
    assertRoundTrips(
        new TypeElement(new CsmElementId("csm:1"), "MyType", OBSERVED, NativeAttributes.empty(), Optional.empty()));
  }

  @Test
  void methodElementRoundTrips() {
    assertRoundTrips(
        new MethodElement(
            new CsmElementId("csm:1"), "doThing()", OBSERVED, NativeAttributes.empty(), Optional.empty()));
  }

  @Test
  void externalSystemElementWithAllAttributesRoundTrips() {
    assertRoundTrips(
        new ExternalSystemElement(
            new CsmElementId("csm:1"),
            "com.thirdparty:lib",
            OBSERVED,
            NativeAttributes.empty(),
            Optional.of("SaaS integration"),
            Optional.of("high"),
            Optional.of("HTTPS"),
            Optional.of("platform-team")));
  }

  @Test
  void externalSystemElementUnresolvedRoundTrips() {
    assertRoundTrips(ExternalSystemElement.unresolved(new CsmElementId("csm:1"), "com.thirdparty:lib", OBSERVED));
  }

  @Test
  void architectureComponentElementRoundTrips() {
    // Architecture Component provenance is never OBSERVED (see
    // ArchitectureComponentElement) - CSM Builder never constructs one
    // anyway (tasks.md 24.4), so DECLARED is used purely to satisfy
    // this type's own invariant for this codec-completeness test.
    ProvenanceRecord declared = ProvenanceRecord.declared("architecture-doc:1", TIMESTAMP);
    assertRoundTrips(
        new ArchitectureComponentElement(
            new CsmElementId("csm:1"),
            "billing-component",
            declared,
            NativeAttributes.empty(),
            List.of(new CsmElementId("csm:2"), new CsmElementId("csm:3"))));
  }

  @Test
  void confidenceRoundTripsWhenPresent() {
    ProvenanceRecord inferred = ProvenanceRecord.inferred("heuristic:1", TIMESTAMP, Confidence.MEDIUM);
    assertRoundTrips(new ModuleElement(new CsmElementId("csm:1"), "mod", inferred, NativeAttributes.empty()));
  }

  private static void assertRoundTrips(CsmElement element) {
    String encoded = CsmElementCodec.encode(element);
    CsmElement decoded = CsmElementCodec.decode(encoded);
    assertEquals(element, decoded);
  }
}
