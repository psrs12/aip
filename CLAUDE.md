# AIP — Claude Code Development Instructions

## Project

Architecture Intelligence Platform (AIP) is an AI-assisted
software architecture governance platform.

AIP continuously analyzes software systems for:

- Architecture compliance
- Design quality
- Security risks
- Technical risks
- Architectural drift
- Engineering policy compliance

AIP provides explainable findings, recommendations, and,
where appropriate, controlled remediation.

---

# Development Methodology

AIP follows Spec-Driven Development using OpenSpec.

The `openspec/` directory is the source of truth for:

- Product requirements
- Architecture decisions
- System behavior
- Feature specifications
- Proposed changes

Do not treat implementation code as the source of truth when
the implementation conflicts with an approved specification.

---

# Mandatory Development Workflow

For significant changes, follow:

Explore
  ↓
Proposal
  ↓
Design
  ↓
Specification
  ↓
Review
  ↓
Implementation
  ↓
Verification
  ↓
Archive

Do not skip the specification/design stages for significant
architectural or functional changes.

---

# Before Implementation

Before implementing a change:

1. Read `openspec/project.md`.
2. Identify relevant specifications under `openspec/specs/`.
3. Inspect the existing implementation.
4. Identify architectural boundaries affected by the change.
5. Create or update the appropriate OpenSpec change.
6. Define requirements and scenarios.
7. Define the technical design.
8. Review the proposed approach before implementation.

---

# Specification First

Do not create classes, interfaces, modules, or dependencies
simply because they appear useful.

First establish:

Requirement
  ↓
Behavior
  ↓
Design
  ↓
Implementation

If implementation reveals a missing requirement, update the
specification rather than silently changing the intended behavior.

---

# Architecture Principles

AIP follows these principles:

## Architecture First

Architecture decisions and boundaries must be explicit.

## Deterministic Analysis Before AI

Use deterministic analysis whenever a problem can be solved
deterministically.

Examples:

- Dependency analysis
- Circular dependency detection
- Layer violations
- Architecture boundary violations
- Complexity analysis
- Known security patterns

Use AI primarily for:

- Reasoning
- Explanation
- Recommendation
- Prioritization
- Remediation assistance

## Evidence Based

Every significant finding should be supported by observable
evidence.

Clearly distinguish:

Evidence
Analysis
Inference
Recommendation

## Language Independent

The Canonical Software Model (CSM) must remain independent
of any specific programming language.

Language-specific analyzers should translate source code into
the common software model.

## Human Controlled Remediation

AI must not autonomously make high-risk changes without
appropriate controls.

Preferred workflow:

Finding
  ↓
Recommendation
  ↓
Proposed Change
  ↓
Human Approval
  ↓
Implementation
  ↓
Testing
  ↓
Verification

## Policy as Code

Architecture and engineering policies should be machine-readable,
version-controlled, testable, and independently maintainable.

---

# Canonical Software Model

The CSM is a foundational architectural capability.

It should eventually represent concepts including:

- Repository
- Project
- Module
- Package
- Component
- Source File
- Type
- Method
- Dependency
- API
- External System
- Architectural Boundary
- Business Capability
- Domain
- Ownership
- Criticality
- Architecture Constraint

The CSM must not contain AI prompts or AI-specific implementation
details.

Findings, recommendations, and remediation plans should remain
separate from the CSM.

---

# AI Agent Architecture

AIP should use specialized agents rather than one monolithic
AI agent.

Potential agents include:

- Architecture Compliance Agent
- Architecture Drift Agent
- Dependency Boundary Agent
- Design Quality Agent
- Security Agent
- Risk Agent
- Compliance Agent
- Remediation Agent

Agents should consume shared AIP models and analysis results.

Agents should not independently redefine the meaning of the
software system.

---

# Technology

Initial implementation:

- Java
- Maven
- Java 21+
- AST-based analysis
- Rule-based analysis
- AI/LLM integration

Technology choices must support the architecture rather than
drive the architecture.

---

# Testing

Every implemented requirement must have corresponding tests.

Prefer:

- Unit tests
- Specification tests
- Rule tests
- Analyzer tests
- Integration tests

Tests should verify behavior defined by the specification.

---

# Dependency Discipline

Do not add a dependency simply because it is convenient.

Before introducing a significant dependency:

1. Identify why it is required.
2. Determine which module owns the dependency.
3. Check architectural impact.
4. Prefer the smallest appropriate dependency.
5. Avoid placing unrelated dependencies in `aip-core`.

---

# Module Architecture

The actual, implemented module structure (validated during each
capability's own design phase, per the note below — not the originally
assumed linear chain):

```
                    aip-core
                       |
   +----------+--------+--------+----------+----------+
   |          |        |        |          |          |
aip-csm-   aip-       aip-     aip-      aip-       aip-analyzer
builder    analysis   rules    findings  ai         (Repository
(built)    (built)    (built)  (built)   (built —   Understanding
                                          the only   implementation,
                                          AI-bearing not yet built)
                                          module)

                                                aip-cli / aip-server
                                                (future, not yet
                                                built)
```

`aip-csm-builder`, `aip-analysis`, `aip-rules`, `aip-findings`,
`aip-ai`, and `aip-analyzer` are six independent siblings — **each
depends on `aip-core` only, never on any other sibling**, even though
the content they produce flows through all of them in sequence at
runtime (Repository Evidence → CSM Snapshot → Analysis Results → Rule
Evaluation Results → Findings → Recommendations). Each layer reads its
upstream neighbor's output through a dedicated `aip-core` read
contract (`CsmSnapshotSource`, `AnalysisResultSource`,
`RuleEvaluationResultSource`, `FindingSource`) instead of a direct
module dependency — mechanically enforced by
`scripts/check-module-dependencies.sh` and
`scripts/check-no-module-reference.sh` at every module's own `verify`
phase. See `docs/architecture.md` §2–3 and `docs/design.md` §7 for the
full graph and the reasoning behind each contract's placement.

`aip-core` must remain independent of:

- AI frameworks
- CLI frameworks
- Server frameworks
- Language-specific analyzers

`aip-ai` is the one module permitted to depend on AI/LLM mechanics
internally (though no concrete LLM provider is wired yet — the one
Agent implemented so far, `ArchitectureComplianceAgent`, is
deterministic and template-based). It is the only module exempt from
the no-randomness/no-AI-import build guard
(`scripts/check-no-ai-heuristic-imports.sh`), and it is not a
dependency of, nor is it depended on by, any of the five deterministic
modules.

---

# Change Management

OpenSpec changes should be used for significant changes.

A change should explain:

- Why the change is needed
- Current problem
- Proposed behavior
- Requirements
- Scenarios
- Architectural impact
- Implementation approach
- Verification approach

Completed changes should be archived according to the OpenSpec workflow.

---

# Claude Behavior

When asked to implement a feature:

Do not immediately write code.

First determine whether the request requires a specification
change.

If it does:

1. Explore the problem.
2. Identify affected specifications.
3. Propose the change.
4. Define requirements and scenarios.
5. Define the design.
6. Review the design.
7. Implement only after the specification is sufficiently clear.

When requirements are ambiguous, identify the ambiguity rather
than silently making an architectural decision.

When an existing implementation conflicts with the specification,
highlight the conflict.

Always prefer explicit architectural decisions over accidental
architecture.

---

# Current Development Focus

Every capability named in `openspec/project.md` §11's evolution order
has completed the full Explore → Propose → Design → Specify → Review →
Implement → Test → Verify → Archive cycle at least once:

Canonical Software Model → CSM Builder → Analysis Framework →
Rule Framework → Finding Model → Agent Framework →
Architecture Compliance Agent.

`openspec/specs/` holds the current, approved specification for each;
`openspec/changes/archive/` holds every completed cycle's
proposal/design/tasks/traceability artifacts (16 archived changes as
of this writing — Analysis Framework alone has been through the cycle
twice: its original `2026-08-13` pair, plus a `2026-09-18` amendment
pair adding Incremental Analysis support and strengthening the
Analysis Result validation/publish gate). `mvn verify` from the
repository root builds, tests, and architecturally guards all six
implemented modules together (384 tests as of the last full run — see
`docs/architecture.md` for the up-to-date pipeline diagram and
`docs/design.md` for the underlying technical design).

Not yet built:

- A real Software Repository Understanding implementation
  (`aip-analyzer`) — `software-repository-understanding` is specified
  but every capability above it was implemented against
  contract-faithful fixtures instead, per each `implement-*` change's
  own deliberate sequencing decision.
- Any `project.md` §5 agent beyond Architecture Compliance (Architecture
  Drift, Dependency Boundary, Layering, Domain Boundary, API
  Architecture, Event Architecture, and the Design/Security/Risk/
  Compliance categories).
- A real LLM/model-provider adapter — every Agent implemented so far
  (`ArchitectureComplianceAgent`) is deterministic and template-based.
- `aip-cli` / `aip-server` — no design work has started on either.
- A real `aip-csm-builder` → `aip-analysis` adapter (CSM Builder's
  `Snapshot` does not implement `CsmSnapshotSource`) and any concrete
  `Analyzer` — every module boundary in the pipeline has so far been
  exercised only against hand-built test fixtures, never real,
  non-fixture content from the module immediately upstream.

## In progress: closing the real-data wiring gap

`openspec/changes/explore-next-evolution/explore.md` is a completed
Explore-phase inventory (not itself a proposal) concluding that,
although `project.md` §11's named evolution order is fully specified
and mostly implemented, the pipeline has never been run against real
repository content — most consequentially, nothing in the system can
construct the Architecture Component/Boundary content the one built
Agent (Architecture Compliance) requires, because CSM Builder is
mechanically forbidden from constructing it
(`csm-builder`'s own `Exclusion of Architectural Inference and
Declared-Knowledge Construction` requirement). It recommends closing
this gap, in priority order, before adding further Agents or
capabilities: (1) resolve declared/inferred CSM knowledge construction,
(2) build a real `aip-csm-builder` → `aip-analysis` adapter plus a
first concrete `Analyzer`, (3) choose persistence technology for the
four still-interface-only `*Store`s.

`openspec/changes/define-declared-knowledge-construction/` addresses
item (1): Propose → Design → Specify are complete (a new module,
`aip-declared-knowledge`, constructing `declared`-only CSM knowledge
from a plain Java `Declaration` value, validated against a supplied
baseline `CsmSnapshotSource`, combined with `observed` content via a
new general `aip-core` `CompositeCsmSnapshotSource`). Its first formal
Review pass found three REQUIRED CHANGE items (a factual overclaim
about existing cross-category precedence machinery, an ambiguous
Boundary Constraint endpoint-resolution rule, and a missing
per-element identity-derivation decision) — recommendation **REVISE**,
not yet approved. No implementation code exists for this change yet;
do not treat `aip-declared-knowledge` as built. Check this change's own
status before continuing it or starting related work.

When picking up new work, follow the same workflow this project has
used for every capability so far: Explore first, do not skip Design or
Specify for a significant change, and check whether an existing
`define-*` change already specifies the capability before writing a
new one.