# Architecture Intelligence Platform (AIP)

AIP is an AI-assisted software engineering platform that continuously
analyzes software systems to identify architectural, design, security,
technical, operational, and compliance risks. It is intended to act as
a continuous architecture governance capability throughout the
software development lifecycle, not a one-time architecture review
tool.

See [`openspec/project.md`](openspec/project.md) for the full project
vision, core principles, functional scope, and technology direction.
See [`docs/architecture.md`](docs/architecture.md) for architecture
and design-flow diagrams of what's actually implemented so far.

## Development Methodology

AIP follows Spec-Driven Development using [OpenSpec](https://github.com/Fission-AI/OpenSpec).
Specifications under `openspec/specs/` are the source of truth for
system behavior and architectural decisions. See
[`CLAUDE.md`](CLAUDE.md) for the mandatory development workflow:

```
Explore → Propose → Design → Specify → Review → Implement → Test → Verify → Archive
```

Implementation is not started until a change's specification and
design have been reviewed and approved.

## Repository Layout

```
pom.xml                  # Maven parent aggregator (Java 21+)
aip-core/                 # Foundational domain models: the Canonical
│                          # Software Model (aip.core.csm) and the
│                          # Repository Evidence contract
│                          # (aip.core.evidence)
aip-csm-builder/           # Deterministic transformation of Repository
                           # Evidence into observed CSM knowledge
                           # (depends on aip-core only)
scripts/                 # Architectural guard scripts bound to `mvn
                           # verify` — dependency graph, fixture scope,
                           # test naming, evidence-type uniqueness,
                           # AI/heuristic-import exclusion, excluded-
                           # construction exclusion
docs/
└── architecture.md       # Architecture and design-flow diagrams

openspec/
├── project.md          # Project vision, principles, and scope
├── config.yaml          # OpenSpec project configuration
├── specs/                # Approved, current specifications (source of truth)
│   ├── canonical-software-model/
│   ├── software-repository-understanding/
│   └── csm-builder/
└── changes/              # In-flight and archived change proposals
    └── archive/
        ├── 2026-08-10-define-csm-builder/
        ├── 2026-08-10-define-software-repository-understanding/
        └── 2026-08-11-implement-csm-builder/   # tasks.md, traceability.md, invariants.md
```

## Current Status

| Capability | Status |
|---|---|
| Canonical Software Model (CSM) | Specified and archived — see [`openspec/specs/canonical-software-model/spec.md`](openspec/specs/canonical-software-model/spec.md) |
| Software Repository Understanding | Specified and archived (not yet implemented) — see [`openspec/specs/software-repository-understanding/spec.md`](openspec/specs/software-repository-understanding/spec.md) |
| CSM Builder | Specified, archived, and **implemented** — see [`openspec/specs/csm-builder/spec.md`](openspec/specs/csm-builder/spec.md) and the archived [`implement-csm-builder`](openspec/changes/archive/2026-08-11-implement-csm-builder/) change |

CSM Builder's implementation proceeded independently of a Repository
Understanding implementation, against contract-faithful Repository
Evidence fixtures — see
[`.../explore.md`](openspec/changes/archive/2026-08-11-implement-csm-builder/explore.md)
for the sequencing decision and
[`.../design.md`](openspec/changes/archive/2026-08-11-implement-csm-builder/design.md)
for the resulting module architecture.

**Implementation complete: 92/92 tasks** (all 24 sections of
[`tasks.md`](openspec/changes/archive/2026-08-11-implement-csm-builder/tasks.md)):
the full CSM domain model (`aip.core.csm`) and Repository Evidence
contract (`aip.core.evidence`) in `aip-core`; and in `aip-csm-builder`,
the Mapping Orchestrator with incremental re-derivation and Mapper
versioning (`aip.csmbuilder.mapping`), deterministic identity
derivation (`aip.csmbuilder.identity`), `observed`-only provenance
construction and enforcement (`aip.csmbuilder.provenance`), seven
structural/relational Mappers (`aip.csmbuilder.mapper`), a swappable
dependency-kind classifier (`aip.csmbuilder.dependency`), a validated
and persisted snapshot layer (`aip.csmbuilder.snapshot`), and a
test-only fixture-building API (`aip.csmbuilder.test.fixtures`). 166
tests and 6 architectural build guards pass under `mvn verify` across
both modules. See [`docs/architecture.md`](docs/architecture.md) for
how these pieces fit together, and
[`.../traceability.md`](openspec/changes/archive/2026-08-11-implement-csm-builder/traceability.md)
for the full requirement/scenario-to-test mapping.

## Contributing

Every significant feature or architectural change follows the mandatory
workflow above. Read `openspec/project.md`, the relevant specs under
`openspec/specs/`, and any active change under `openspec/changes/`
before proposing or implementing changes.
