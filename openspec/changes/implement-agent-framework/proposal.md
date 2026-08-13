## Why

The `agent-framework` capability is fully specified, reviewed, and
merged (`openspec/changes/define-agent-framework/` — 27 requirements,
~50 scenarios, tasks describing the implementation surface), but no
implementation exists anywhere in this repository. This change carries
Agent Framework from approved specification into working code, the
same relationship `implement-finding-model` had to `define-finding-model`
(completed and merged), extended with the fact that this is the
**first AI-bearing module** in the codebase — every module before it
(`aip-csm-builder`, `aip-analysis`, `aip-rules`, `aip-findings`) is
deterministic by construction and CI-guarded to stay that way.

## What Changes

- Implement `FindingSource` in `aip-core`: Agent Framework's own
  contribution to `aip-core` (`define-agent-framework/design.md`
  Decision 8) — read a Finding by its Evaluation Identity, list
  Findings by Logical Finding Identity or source CSM Snapshot
  identity.
- Implement the Agent contract, `AgentRegistry`, and the Finding-only
  consumption boundary — an Agent obtains its Finding exclusively
  through `FindingSource`, never through a direct dependency on
  `aip-findings` or any deterministic module.
- Implement Recommendation's two-part identity model: a deterministic
  **Recommendation Artifact Identity** (Agent identifier/version,
  referenced Finding's Evaluation Identity, and an opaque, per-
  invocation **Generation Identifier**) and a non-identity-bearing
  **Generation Provenance** record (model/provider identifier and
  version, generation configuration, timestamp) — resolving, per this
  change's own `design.md`, the concrete Java shapes and the one
  genuinely new mechanical concern this framework introduces: minting
  a unique-but-content-independent Generation Identifier per
  invocation, the first legitimate use of non-determinism anywhere in
  this codebase.
- Implement `RecommendationConstructor`: a per-invocation, one-Finding-
  in/one-Recommendation-out (or none, on generation failure) pipeline,
  mirroring `FindingConstructor`'s shape one layer down.
- Implement the validate-before-publish gate
  (`RecommendationValidator`/`RecommendationPublisher`), explicitly
  scoped to structural/referential/provenance checks only — never a
  claim about substantive guidance quality, per `define-agent-framework/
  design.md` Decision 7.
- Implement `RecommendationStore` as a persistence abstraction in
  `aip-ai` (**not** promoted to `aip-core` — a deliberate departure
  from `FindingStore`/`RuleEvaluationResultStore`/`AnalysisResultStore`'s
  own early-promotion pattern, since `define-agent-framework/design.md`
  Decision 8 explicitly declines to promote it absent a named future
  consumer).
- Implement a fixture layer for `FindingSource`, test only — not a
  real `aip-findings` adapter.
- Confirm the extension mechanism (a new Agent requires no
  Recommendation construction/identity/traceability/validation change,
  and no change to any deterministic capability).

This change does not modify the behavior specified by `agent-framework`,
`finding-model`, `rule-framework`, `analysis-framework`,
`canonical-software-model`, `csm-builder`, or `software-repository-
understanding` — it implements what is already approved. It does not
implement the Architecture Compliance Agent or any other specific
agent from `project.md` §5's catalogue, a Proposed Change artifact,
code generation, remediation execution, or any capability downstream
of Recommendation. It does not wire a real LLM provider — Agent
behavior in this change is exercised through fixture/stub Agents only,
mirroring every prior `implement-*` change's fixture-first approach to
its own upstream dependency.

## Binding Decisions (locked scope, carried from Explore)

1. **`aip-ai` is the first AI-bearing module, structurally isolated**:
   depends on `aip-core` only, and — per `define-agent-framework/
   design.md` Decision 8 — is not a sibling of, and shares no
   dependency relationship with, `aip-csm-builder`/`aip-analysis`/
   `aip-rules`/`aip-findings`. Every one of those five modules' own
   existing `check-module-dependencies.sh` guard (each already asserts
   "depends only on `aip:aip-core`") already, structurally, proves none
   of them can depend on `aip-ai` — no new reverse-dependency script is
   needed; this change adds only the forward-direction guard
   (`aip-ai` depends on `aip-core` only) plus the four
   no-cross-reference guards (`aip-findings`/`aip-rules`/`aip-analysis`/
   `aip-csm-builder`), mirroring `implement-finding-model`'s own three.
2. **`check-no-ai-heuristic-imports.sh` is deliberately NOT wired for
   `aip-ai`**: that guard exists to keep the five deterministic modules
   free of randomness and AI/LLM SDK imports; `aip-ai` is the one
   module in this codebase where a Generation Identifier's minting
   legitimately requires a non-deterministic, non-content-derived
   token (per `define-agent-framework/design.md` Decision 3 and spec
   `Generation Identifier Uniqueness and Non-Content-Derivation`).
   Applying that guard to `aip-ai` would contradict the very capability
   this change implements.
3. **Recommendation identity and content types are `aip-ai`-local, not
   promoted to `aip-core`**: unlike `AnalysisResult`, `RuleEvaluationResult`,
   and `Finding` (each promoted because a specific, already-named
   future consumer existed), `define-agent-framework/design.md`
   Decision 8 explicitly declines to promote `Recommendation`,
   `RecommendationArtifactIdentity`, `GenerationIdentifier`, and
   `GenerationProvenance` — no capability named anywhere in `project.md`
   or this project's history yet needs to read Recommendations as an
   upstream input. This is carried forward unchanged, not revisited.
4. **No real LLM adapter**: this change does not wire a real
   connection to any model provider — `aip-ai`'s own tests use
   fixture/stub `Agent` implementations, mirroring
   `implement-finding-model`'s own fixture-first approach to
   `aip-rules` one layer down.

## Impact

- **Affected code**: adds the `aip-ai` module; adds `FindingSource` to
  `aip-core`. No changes to `aip-csm-builder`, `aip-analysis`,
  `aip-rules`, `aip-findings`, or any existing `aip-core` type.
- **Affected specs**: none — `skip_specs: true`, since this change
  implements `agent-framework`'s already-approved requirements without
  altering them.
- **Dependencies**: `aip-ai` depends on `aip-core` only, per
  `define-agent-framework/design.md` Decisions 4, 8, enforced by a
  dependency-graph CI check mirroring every prior module's own, and
  four reuses of the generic `scripts/check-no-module-reference.sh`
  built during `implement-rule-framework`.
- **Prerequisite**: `implement-finding-model`'s own tasks are already
  complete and merged — `Finding`, `EvaluationIdentity`,
  `LogicalFindingIdentity`, and `CsmSnapshotId` already exist and
  compile in `aip-core`. No blocking prerequisite gate is needed.
- **Affects future work**: unblocks the Architecture Compliance
  Agent's own future `implement-*` change, which will register a
  concrete Agent against this framework's contract rather than
  building its own.
