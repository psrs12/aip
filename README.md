# Architecture Intelligence Platform (AIP)

AIP is an AI-assisted software engineering platform that continuously
analyzes software systems to identify architectural, design, security,
technical, operational, and compliance risks. It is intended to act as
a continuous architecture governance capability throughout the
software development lifecycle, not a one-time architecture review
tool.

See [`openspec/project.md`](openspec/project.md) for the full project
vision, core principles, functional scope, and technology direction.

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
openspec/
├── project.md          # Project vision, principles, and scope
├── config.yaml          # OpenSpec project configuration
├── specs/                # Approved, current specifications (source of truth)
│   └── canonical-software-model/
└── changes/              # In-flight and archived change proposals
    └── archive/
        └── <date>-<change-name>/
```

## Current Status

| Capability | Status |
|---|---|
| Canonical Software Model (CSM) | Specified and archived — see [`openspec/specs/canonical-software-model/spec.md`](openspec/specs/canonical-software-model/spec.md) |
| Software Repository Understanding | Explore phase in progress — see [`openspec/changes/define-software-repository-understanding/`](openspec/changes/define-software-repository-understanding/) |

No implementation code exists yet. The initial implementation is
expected to use Java, Maven, and Java 21+ once the relevant
specifications are approved (see `openspec/project.md` §10).

## Contributing

Every significant feature or architectural change follows the mandatory
workflow above. Read `openspec/project.md`, the relevant specs under
`openspec/specs/`, and any active change under `openspec/changes/`
before proposing or implementing changes.
