package aip.csmbuilder.mapping;

import aip.core.csm.CsmElementId;
import aip.core.csm.ModuleElement;
import aip.core.csm.NativeAttributes;
import aip.core.csm.ProvenanceRecord;
import aip.core.evidence.EvidenceItem;
import aip.core.evidence.EvidenceKind;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A minimal, non-production {@link EvidenceKindMapper} used only to
 * exercise the Mapper/Registry/Orchestrator contract itself (tasks.md
 * 4.4) — independent of any real domain Mapper, which arrive in later
 * sections. Records the order in which it was invoked so tests can
 * assert on {@link MappingOrchestrator}'s deterministic dispatch
 * order.
 */
final class RecordingTestMapper implements EvidenceKindMapper {

  private final EvidenceKind kind;
  private final int version;
  final List<EvidenceItem> invocations = new ArrayList<>();

  RecordingTestMapper(EvidenceKind kind, int version) {
    this.kind = kind;
    this.version = version;
  }

  @Override
  public EvidenceKind supportedKind() {
    return kind;
  }

  @Override
  public int mapperVersion() {
    return version;
  }

  @Override
  public MappingResult map(EvidenceItem item, MappingContext context) {
    invocations.add(item);
    ModuleElement element =
        new ModuleElement(
            new CsmElementId("csm:" + item.id()),
            item.id().scopeKey(),
            ProvenanceRecord.observed(item.id().toString(), Instant.EPOCH),
            NativeAttributes.empty());
    return MappingResult.ofElement(element);
  }
}
