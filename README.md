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
scripts/                 # check-module-dependencies.sh — the
                           # dependency-graph guard bound to
                           # aip-csm-builder's `mvn verify`
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
    ├── implement-csm-builder/   # Active — implementation in progress
    └── archive/
        └── <date>-<change-name>/
```

## Current Status

| Capability | Status |
|---|---|
| Canonical Software Model (CSM) | Specified and archived — see [`openspec/specs/canonical-software-model/spec.md`](openspec/specs/canonical-software-model/spec.md) |
| Software Repository Understanding | Specified and archived (not yet implemented) — see [`openspec/specs/software-repository-understanding/spec.md`](openspec/specs/software-repository-understanding/spec.md) |
| CSM Builder | Specified and archived; **implementation in progress** — see [`openspec/specs/csm-builder/spec.md`](openspec/specs/csm-builder/spec.md) and the active [`implement-csm-builder`](openspec/changes/implement-csm-builder/) change |

CSM Builder's implementation proceeds independently of a Repository
Understanding implementation, against contract-faithful Repository
Evidence fixtures — see
[`openspec/changes/implement-csm-builder/explore.md`](openspec/changes/implement-csm-builder/explore.md)
for the sequencing decision and
[`openspec/changes/implement-csm-builder/design.md`](openspec/changes/implement-csm-builder/design.md)
for the resulting module architecture.

**Implementation progress: 34/91 tasks complete** (Sections 1–7 of
[`tasks.md`](openspec/changes/implement-csm-builder/tasks.md)):
Maven module scaffolding with an enforced `aip-csm-builder → aip-core`
dependency guard; the full CSM domain model (`aip.core.csm`) and
Repository Evidence contract (`aip.core.evidence`); the Mapping
Orchestrator, Mapper contract, and registry (`aip.csmbuilder.mapping`);
deterministic identity derivation (`aip.csmbuilder.identity`);
`observed`-only provenance construction and enforcement
(`aip.csmbuilder.provenance`); and the six structural entity Mappers
— Repository, Project, Module, Package, Type, Method
(`aip.csmbuilder.mapper`). 59 tests pass across both modules. See
[`docs/architecture.md`](docs/architecture.md) for how these pieces
fit together, and `tasks.md` for what's next.

## Contributing

Every significant feature or architectural change follows the mandatory
workflow above. Read `openspec/project.md`, the relevant specs under
`openspec/specs/`, and any active change under `openspec/changes/`
before proposing or implementing changes.
