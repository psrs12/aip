# AIP Architecture and Design Flow

This document diagrams the architecture actually implemented so far —
grounded in the archived specifications under `openspec/specs/` and
the code under `aip-core`/`aip-csm-builder`. It is kept in sync as
implementation progresses; see
[`openspec/changes/implement-csm-builder/tasks.md`](../openspec/changes/implement-csm-builder/tasks.md)
for current task-by-task status.

## 1. Capability chain

The platform's evolution order, per `openspec/project.md` §11. Each
box is independently specified and archived under `openspec/specs/`;
only CSM Builder has begun implementation.

```mermaid
flowchart LR
    Repo[("Repository<br/>(real source code)")]
    RU["Software Repository<br/>Understanding<br/><sub>specified — not yet implemented</sub>"]
    RE["Repository Evidence<br/>Model"]
    CB["CSM Builder<br/><sub>specified — implementation in progress</sub>"]
    CSM["Canonical Software<br/>Model (CSM)"]
    Future["Analysis / Rule /<br/>Agent Framework<br/><sub>future</sub>"]

    Repo -->|discovery| RU
    RU -->|produces| RE
    RE -->|sole input| CB
    CB -->|constructs, observed-only| CSM
    CSM -.->|future| Future
```

CSM Builder's implementation proceeds independently of a Repository
Understanding implementation — it depends only on the **Repository
Evidence contract** (a data shape, `aip.core.evidence`), never on a
concrete RU implementation module. See
[`openspec/changes/implement-csm-builder/explore.md`](../openspec/changes/implement-csm-builder/explore.md)
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
[`design.md`](../openspec/changes/implement-csm-builder/design.md)
Decision 1.

## 3. CSM Builder: package structure

```mermaid
graph TD
    subgraph mapper["aip.csmbuilder.mapper"]
        RM[RepositoryMapper]
        PjM[ProjectMapper]
        MM[ModuleMapper]
        PkM[PackageMapper]
        TM[TypeMapper]
        MeM[MethodMapper]
        Keys[EvidenceAttributeKeys]
    end
    subgraph mapping["aip.csmbuilder.mapping"]
        MO[MappingOrchestrator]
        Reg[EvidenceKindMapperRegistry]
        MC[MappingContext]
        MK["EvidenceKindMapper (contract)"]
        MRes[MappingResult]
        ERL[EvidenceRelationshipLookup]
    end
    subgraph identity["aip.csmbuilder.identity"]
        EID[ElementIdentityDeriver]
    end
    subgraph provenance["aip.csmbuilder.provenance"]
        OPF[ObservedProvenanceFactory]
        PG[ProvenanceGuard]
    end

    mapper --> mapping
    mapper --> identity
    mapper --> provenance
    mapping --> provenance
```

Every arrow is a real package dependency in the current codebase.
`aip.csmbuilder.mapper` (the concrete Mapper implementations) is the
only package that depends on all three of the others — `mapping`,
`identity`, and `provenance` remain independently meaningful and
independently testable.

## 4. Orchestration data flow

```mermaid
flowchart TD
    REM["RepositoryEvidenceModel<br/><sub>Items + Relationships</sub>"]
    Sort["sort Evidence Items by identity<br/><sub>deterministic, sequential — no concurrency</sub>"]
    Lookup["Registry.lookup(item.kind())"]
    Skip(["no Mapper registered → skip<br/><sub>this is how ConfigReference and<br/>implementation/extension·invocation<br/>exclusions are realized</sub>"])
    Map["Mapper.map(item, context)"]
    Guard["ProvenanceGuard.verify(result, item)<br/><sub>observed-only + traceable, or throw</sub>"]
    Merge["merge into accumulated MappingResult"]
    Result["MappingResult<br/><sub>CsmElements + CsmRelationships</sub>"]

    REM --> Sort --> Lookup
    Lookup -- not found --> Skip
    Lookup -- found --> Map --> Guard --> Merge
    Merge -->|next item| Lookup
    Merge -->|all items processed| Result
```

## 5. A single item's processing, in detail

Illustrated for `PackageMapper`, the one Mapper whose identity
derivation depends on another Evidence Item (its containing Module) —
see [`design.md`](../openspec/changes/implement-csm-builder/design.md)
Decision 3 and Decision 1's task-7 commit message for why this lookup
is order-independent.

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

## 6. Development workflow

Every significant capability or implementation change follows this
sequence (`CLAUDE.md`):

```mermaid
flowchart LR
    Explore --> Propose --> Design --> Specify --> Review --> Implement --> Test --> Verify --> Archive
```

`openspec/specs/` holds the source-of-truth output of Specify/Review
for each capability; `openspec/changes/archive/` preserves every
completed cycle's Explore/Proposal/Design/Tasks artifacts for
traceability.
