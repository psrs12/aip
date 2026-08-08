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

The intended initial module structure is:

aip-core
  ↓
aip-analyzer
  ↓
aip-rules
  ↓
aip-ai
  ↓
aip-cli / aip-server

The exact dependency graph must be validated during design.

`aip-core` must remain independent of:

- AI frameworks
- CLI frameworks
- Server frameworks
- Language-specific analyzers

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

The immediate goal is to establish the conceptual foundation
of AIP before implementing the first agent.

Current exploration topic:

"What information must AIP collect and understand about a
software system to perform meaningful architecture, design,
risk, and compliance analysis?"

The next major architectural artifact is the:

Canonical Software Model (CSM).

Do not implement the CSM until its conceptual boundaries,
responsibilities, and requirements have been specified.