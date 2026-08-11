# AIP Architecture and Design Flow

This document diagrams the architecture actually implemented —
grounded in the archived specifications under `openspec/specs/` and
the code under `aip-core`/`aip-csm-builder`. CSM Builder's
implementation is complete (all 92 tasks); see
[`openspec/changes/archive/2026-08-11-implement-csm-builder/tasks.md`](../openspec/changes/archive/2026-08-11-implement-csm-builder/tasks.md)
for the full task-by-task record,
[`.../traceability.md`](../openspec/changes/archive/2026-08-11-implement-csm-builder/traceability.md)
for the requirement/scenario-to-test matrix, and
[`.../invariants.md`](../openspec/changes/archive/2026-08-11-implement-csm-builder/invariants.md)
for the architectural invariants this document's guard diagram (§8)
reflects.

## 1. Capability chain

The platform's evolution order, per `openspec/project.md` §11. Each
box is independently specified and archived under `openspec/specs/`.

```mermaid
flowchart LR
    Repo[("Repository<br/>(real source code)")]
    RU["Software Repository<br/>Understanding<br/><sub>specified — not yet implemented</sub>"]
    RE["Repository Evidence<br/>Model"]
    CB["CSM Builder<br/><sub>specified — implemented</sub>"]
    CSM["Canonical Software<br/>Model (CSM)"]
    Future["Analysis / Rule /<br/>Agent Framework<br/><sub>future</sub>"]

    Repo -->|discovery| RU
    RU -->|produces| RE
    RE -->|sole input| CB
    CB -->|constructs, observed-only| CSM
    CSM -.->|future| Future
```

CSM Builder's implementation proceeded independently of a Repository
Understanding implementation — it depends only on the **Repository
Evidence contract** (a data shape, `aip.core.evidence`), never on a
concrete RU implementation module. Every test exercising CSM Builder
does so against hand-built, contract-faithful fixtures
(`aip.csmbuilder.test.fixtures`, test-scope only), never a real RU
pipeline — the real pipeline's integration test is a reserved,
not-yet-written seam:
[`RepositoryToCsmPipelineIntegrationTest`](../aip-csm-builder/src/test/java/aip/csmbuilder/integration/RepositoryToCsmPipelineIntegrationTest.java).
See
[`.../explore.md`](../openspec/changes/archive/2026-08-11-implement-csm-builder/explore.md)
for the full sequencing rationale.

## 2. Module dependency graph

```mermaid
graph TD
    core["<b>aip-core</b><br/><sub>aip.core.csm — CSM domain model<br/>aip.core.evidence — Repository Evidence contract</sub>"]
    csmbuilder["<b>aip-csm-builder</b><br/><sub>aip.csmbuilder.* </sub>"]
    analyzer["<b>aip-analyzer</b><br/><sub>future — not built yet</sub>"]

    csmbuilder --> core
    analyzer --> core
```

**Invariant, enforced by `scripts/check-module-dependencies.sh` bound
to `aip-csm-builder`'s `verify` phase:** `aip-csm-builder` depends on
`aip-core` only — never on `aip-analyzer` or any future Repository
Understanding implementation module. `aip-analyzer` and
`aip-csm-builder` are siblings: both depend on `aip-core`, neither
depends on the other. See
[`design.md`](../openspec/changes/archive/2026-08-11-implement-csm-builder/design.md)
Decision 1.

## 3. `aip-core`: two independent domain models

```mermaid
graph TD
    subgraph csm["aip.core.csm — the Canonical Software Model"]
        Elem["CsmElement (sealed)<br/><sub>Repository·Project·Module·Package·Type·<br/>Method·ArchitectureComponent·ExternalSystem</sub>"]
        Rel["CsmRelationship<br/><sub>+ CsmRelationshipType (8, closed)</sub>"]
        Prov["ProvenanceRecord<br/><sub>+ ProvenanceCategory, Confidence</sub>"]
        Subj["Subject / EffectiveKnowledgeStatus<br/>/ SubjectConflictMarker"]
        Valid["CsmValidator / ValidationResult"]
    end
    subgraph evidence["aip.core.evidence — the Repository Evidence contract"]
        EI["EvidenceItem<br/><sub>+ EvidenceId, EvidenceKind (12, closed)</sub>"]
        REM["RepositoryEvidenceModel<br/><sub>+ EvidenceRelationship</sub>"]
        Life["ChangeStatus / LifecycleState<br/>/ ClassifiedEvidenceItem"]
        Disc["DiscoveryOutcome / FailureReason<br/>/ ExtractionMethod"]
    end

    Elem --> Prov
    Rel --> Prov
    Subj --> Elem
    Subj --> Rel
    Valid --> Elem
    Valid --> Rel
    REM --> EI
    Life --> EI
    Disc --> EI
```

`csm` and `evidence` are deliberately independent — neither package
imports the other. This is what lets a CSM element's provenance
reference an Evidence identity by opaque string (`ProvenanceRecord.sourceReference()`)
without a compile-time coupling between the two contracts (see
`aip.core.evidence`'s own package-info for the full rationale).
`Subject`/`SubjectConflictMarker`/`CsmValidator` were added mid-implementation
(Section 18/19) to close two real gaps found while implementing CSM
Builder — see `design.md` Decisions 10 and 11.

## 4. `aip-csm-builder`: package structure

```mermaid
graph TD
    subgraph mapper["aip.csmbuilder.mapper"]
        RM[RepositoryMapper]
        PjM[ProjectMapper]
        MM[ModuleMapper]
        PkM[PackageMapper]
        TM[TypeMapper]
        MeM[MethodMapper]
        ACM[ApiContractMapper]
        FLR[FileLocationResolver]
    end
    subgraph mapping["aip.csmbuilder.mapping"]
        MO[MappingOrchestrator]
        Reg[EvidenceKindMapperRegistry]
        MK["EvidenceKindMapper (contract)"]
        CRB[ContainmentRelationshipBuilder]
        DRB[DependencyRelationshipBuilder]
        ESB[ExternalSystemRelationshipBuilder]
        ERTG[ExcludedRelationshipTypeGuard]
        FEF[FailedEvidenceFilter]
        PEL["PriorElementLookup (contract)"]
        CS[ConflictedSubjects]
    end
    subgraph identity["aip.csmbuilder.identity"]
        EID[ElementIdentityDeriver]
    end
    subgraph provenance["aip.csmbuilder.provenance"]
        OPF[ObservedProvenanceFactory]
        PG[ProvenanceGuard]
    end
    subgraph dependency["aip.csmbuilder.dependency"]
        DKC["DependencyKindClassifier (contract)"]
        RDKC[ResourceDependencyKindClassifier]
    end
    subgraph snapshot["aip.csmbuilder.snapshot"]
        SS["SnapshotStore (contract)"]
        FSS[FilesystemSnapshotStore]
        SP[SnapshotPublisher]
        SM[SnapshotManifest]
    end
    subgraph fixtures["aip.csmbuilder.test.fixtures<br/><sub>test-scope only</sub>"]
        REB[RepositoryEvidenceModelBuilder]
        FS[FixtureScenarios]
    end

    mapper --> mapping
    mapper --> identity
    mapper --> provenance
    mapping --> dependency
    mapping --> provenance
    snapshot --> mapping
    fixtures -.->|test code only| mapper
    fixtures -.->|test code only| mapping
```

Every solid arrow is a real, compile-scope package dependency; dashed
arrows are test-scope only. Two directional invariants worth noting,
both mechanically enforced (§7):

- **`mapping` never depends on `snapshot`.** Incremental re-derivation
  (`PriorElementLookup`) is `mapping`'s own minimal interface —
  `EvidenceId -> Optional<CsmElement + mapperVersion>` — not a
  dependency on `Snapshot`/`SnapshotManifest`. `snapshot` depends on
  `mapping` (to persist what it constructs), never the reverse. See
  `design.md` Decision 9.
- **Nothing in `aip.csmbuilder.mapper`/`mapping` depends on
  `aip.csmbuilder.test.fixtures`.** The fixture-building API exists
  only in test sources; production construction logic depends only on
  `aip.core.evidence` *types*, never on how an instance of them was
  produced (`scripts/check-fixture-package-scope.sh`).

## 5. Evidence → CSM construction pipeline (one run)

The full `MappingOrchestrator.construct` flow, including incremental
scoping (Sections 16–17) and the batch relationship builders that run
after every Evidence Item has been dispatched (Sections 8, 10–11).

```mermaid
flowchart TD
    REM["RepositoryEvidenceModel<br/><sub>Items + Relationships</sub>"]
    Sort["sort Evidence Items by identity<br/><sub>deterministic, sequential — no concurrency</sub>"]
    Failed{"discovery outcome<br/>= failed?"}
    SkipFailed(["skip — no CSM content<br/>from failed evidence"])
    Status{"change status?"}
    SkipRemoved(["skip — omitted from this<br/>snapshot, no PURGED wait"])
    Lookup["Registry.lookup(item.kind())"]
    SkipUnmapped(["no Mapper registered → skip<br/><sub>how ConfigFile/ConfigReference and<br/>implementation/extension·invocation<br/>exclusions are realized</sub>"])
    PriorCheck{"UNCHANGED and prior<br/>element's Mapper version<br/>matches current?"}
    Carry["carry prior element forward<br/><sub>Mapper NOT invoked</sub>"]
    Map["Mapper.map(item, context)"]
    Guard["ProvenanceGuard.verify(result, item)<br/><sub>observed-only + traceable, or throw</sub>"]
    Merge["merge into accumulated MappingResult"]
    Batch["ContainmentRelationshipBuilder<br/>DependencyRelationshipBuilder<br/>ExternalSystemRelationshipBuilder"]
    ExclGuard["ExcludedRelationshipTypeGuard.verify(...)<br/><sub>no implementation/extension or invocation</sub>"]
    Result["MappingResult<br/><sub>CsmElements + CsmRelationships</sub>"]

    REM --> Sort --> Failed
    Failed -- yes --> SkipFailed
    Failed -- no --> Status
    Status -- REMOVED --> SkipRemoved
    Status -- ADDED/MODIFIED/UNCHANGED --> Lookup
    Lookup -- not found --> SkipUnmapped
    Lookup -- found --> PriorCheck
    PriorCheck -- yes --> Carry --> Merge
    PriorCheck -- no --> Map --> Guard --> Merge
    Merge -->|next item| Failed
    Merge -->|all items processed| Batch --> ExclGuard --> Result
```

## 6. A single item's processing, in detail

Illustrated for `PackageMapper`, the one Mapper whose identity
derivation depends on another Evidence Item (its containing Module) —
see `PackageMapper`'s and `EvidenceRelationshipLookup`'s own class
javadoc for why this lookup is order-independent (it re-derives the
Module's identity via the same pure function every other Mapper uses,
rather than asking the Orchestrator whether the Module has already
been processed).

```mermaid
sequenceDiagram
    participant O as MappingOrchestrator
    participant R as EvidenceKindMapperRegistry
    participant M as PackageMapper
    participant L as EvidenceRelationshipLookup
    participant D as ElementIdentityDeriver
    participant P as ObservedProvenanceFactory
    participant G as ProvenanceGuard

    O->>R: lookup(PACKAGE)
    R-->>O: PackageMapper
    O->>M: map(packageItem, context)
    M->>L: findSingleSource(CONTAINMENT, packageItem.id())
    L-->>M: containing Module's EvidenceId
    M->>D: fromEvidenceId(moduleEvidenceId)
    D-->>M: containing Module's CsmElementId
    M->>D: forPackage(moduleId, namespaceName)
    D-->>M: this Package's CsmElementId
    M->>P: fromEvidence(packageItem.id(), context.constructionTimestamp())
    P-->>M: ProvenanceRecord (observed)
    M-->>O: MappingResult(PackageElement)
    O->>G: verify(result, packageItem)
    G-->>O: OK
    O->>O: merge into accumulated result
```

Note that `PackageMapper` never asks the Orchestrator "has the Module
already been mapped?" — it independently re-derives the Module's CSM
identity via the same pure `ElementIdentityDeriver` function every
other Mapper uses. This is what makes the result identical regardless
of dispatch order (proven by `PackageMapperTest.packageIdentityIsIndependentOfDispatchOrderRelativeToItsModule`).

## 7. Snapshot lifecycle across two runs

How construction, validation, persistence, and incremental
re-derivation compose — the scenario
`CsmBuilderFixtureEndToEndTest` exercises directly (Sections 15–19).

```mermaid
sequenceDiagram
    participant Caller
    participant MO as MappingOrchestrator
    participant SP as SnapshotPublisher
    participant CV as CsmValidator
    participant Store as FilesystemSnapshotStore

    rect rgb(235, 245, 255)
    note over Caller,Store: Run 1 — full construction
    Caller->>MO: construct(evidenceModel)
    MO-->>Caller: MappingResult
    Caller->>SP: publish(store, repoId, content, ts)
    SP->>CV: validate(elements, relationships)
    CV-->>SP: ValidationResult (valid)
    SP->>Store: write(repoId, content, ts)
    Store-->>SP: Snapshot #1
    SP-->>Caller: PublicationOutcome(published)
    end

    rect rgb(245, 255, 240)
    note over Caller,Store: Run 2 — incremental
    Caller->>MO: construct(evidenceModel, changeStatuses, priorElements)
    note right of MO: UNCHANGED + matching Mapper version → carried forward<br/>REMOVED → omitted<br/>ADDED/MODIFIED/version-changed → reconstructed
    MO-->>Caller: MappingResult
    Caller->>SP: publish(store, repoId, content, ts')
    SP->>CV: validate(elements, relationships)
    CV-->>SP: ValidationResult (valid)
    SP->>Store: write(repoId, content, ts')
    Store-->>SP: Snapshot #2
    SP-->>Caller: PublicationOutcome(published)
    end

    Caller->>Store: read(repoId, 1)
    Store-->>Caller: Snapshot #1, unaffected by Snapshot #2
```

A snapshot that fails `CsmValidator` is never written at all — `SnapshotPublisher`
gates `SnapshotStore.write`, so an invalid snapshot and a written one
are mutually exclusive outcomes (`SnapshotPublisher.PublicationOutcome`'s
own invariant).

## 8. Architectural guards enforced at `mvn verify`

Six build-time checks, independent of the test suite, per
[`invariants.md`](../openspec/changes/archive/2026-08-11-implement-csm-builder/invariants.md).
All run in the `verify` phase; five are `aip-csm-builder`-scoped, one
(evidence-type uniqueness) is reactor-wide.

```mermaid
flowchart LR
    subgraph checks["scripts/"]
        C1[check-module-dependencies.sh]
        C2[check-no-duplicate-evidence-types.sh]
        C3[check-fixture-package-scope.sh]
        C4[check-test-naming.sh]
        C5[check-no-ai-heuristic-imports.sh]
        C6[check-no-excluded-construction.sh]
    end

    C1 -->|invariants 1, 2| I1["aip-csm-builder → aip-core only"]
    C2 -->|invariant 3| I2["no duplicate Evidence type<br/>outside aip.core.evidence"]
    C3 -->|invariant 4| I3["fixtures are test-scope only"]
    C4 -->|invariant 8| I4["no test name implies<br/>real-pipeline RU coverage"]
    C5 -->|invariant 10| I5["no AI/LLM/randomness import"]
    C6 -->|invariant 10| I6["no Architecture Component /<br/>Architectural Boundary construction"]
```

## 9. Development workflow

Every significant capability or implementation change follows this
sequence (`CLAUDE.md`) — CSM Builder is the first capability to have
completed the full cycle, archived under
[`openspec/changes/archive/2026-08-11-implement-csm-builder/`](../openspec/changes/archive/2026-08-11-implement-csm-builder/).

```mermaid
flowchart LR
    Explore --> Propose --> Design --> Specify --> Review --> Implement --> Test --> Verify --> Archive
```

`openspec/specs/` holds the source-of-truth output of Specify/Review
for each capability; `openspec/changes/archive/` preserves every
completed cycle's Explore/Proposal/Design/Tasks artifacts for
traceability.
