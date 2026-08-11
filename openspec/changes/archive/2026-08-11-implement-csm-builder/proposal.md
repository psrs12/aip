## Why

The `csm-builder` capability is fully specified and archived
(`openspec/specs/csm-builder/spec.md` — 29 requirements, 52 scenarios,
stakeholder-reviewed and approved), but no implementation exists
anywhere in this repository. Per `project.md`'s evolution order and
CLAUDE.md's mandatory workflow, Explore → Propose → Design → Specify →
Review are complete for CSM Builder; nothing has entered Implement.
This change carries CSM Builder from approved specification into
working code, so the CSM can, for the first time, actually be
constructed rather than only described.

## What Changes

- Implement the CSM Builder capability in code: the Mapping
  Orchestrator, the Evidence-Kind Mapper registry and contract, and a
  Mapper for each Repository Evidence kind the archived spec covers
  (`Repository`, `Project`, `Module`, `Package`, `SourceUnit`,
  Method-level evidence, `ManifestDependencyEdge`, `ImportEdge`,
  `ApiContractDeclaration`).
- Implement deterministic CSM element/relationship identity derivation,
  `observed`-only provenance construction, and evidence traceability,
  exactly as specified.
- Implement structural containment construction, file-location
  handling, dependency mapping with native-scope classification,
  External System construction via `integration` relationships, and
  API contract mapping — each per its corresponding archived
  requirement.
- Implement the explicit exclusions the spec requires: no
  `ConfigReference` construction, no `implementation/extension` or
  `invocation` relationships (no supporting evidence kind exists yet),
  no Architecture Component/Boundary/Business Context construction, no
  AI/heuristic reasoning anywhere in the code path.
- Implement versioned snapshot construction, incremental re-derivation
  keyed to Repository Evidence's change-status/lifecycle vocabulary,
  CSM Builder's own mapper-version reprocessing, reuse of CSM's
  existing conflict/precedence mechanism, and snapshot validation
  before use.
- Implement a **contract-faithful Repository Evidence fixture layer**
  standing in for a real Repository Understanding producer (see
  Sequencing Decision below) — test-only, not a runtime dependency of
  CSM Builder.
- Implement fixture-based transformation, unit, component, and contract
  tests covering the archived spec's 29 requirements and 52 scenarios.

This change does not modify the behavior specified by `csm-builder`,
`canonical-software-model`, or `software-repository-understanding` —
it implements what is already approved. It does not implement
Repository Understanding. It does not resolve module placement,
snapshot persistence technology, concurrency mechanism, or the
concrete dependency-kind mapping table — those are Design-phase
decisions (see Unresolved Questions for Design).

## Sequencing Decision (locked scope, carried from Explore)

Repository Understanding is specified and archived but has no
implementation anywhere in this repository. Rather than block this
change on an unstarted, unrelated implementation, or invent RU
internals CSM Builder shouldn't depend on, this change proceeds under
an explicitly resolved, binding decision:

1. Repository Understanding is **not** implemented as part of this
   change, in whole or in part.
2. CSM Builder depends **only** on the Repository Evidence
   contract/model as defined by the archived
   `software-repository-understanding` specification — never on a
   Repository Understanding implementation, and never directly on
   `aip-analyzer`.
3. This change uses **contract-faithful Repository Evidence fixtures**
   for implementation and transformation testing, standing in for a
   real producer.
4. Fixtures are derived strictly from the archived RU specification —
   no invented fields, semantics, evidence kinds, or producer behavior.
5. The resulting gap is explicit, not silent: Repository Understanding
   is specified but not yet implemented, so this change cannot provide
   a true `Repository → Repository Understanding → Repository Evidence
   → CSM Builder → CSM` integration test. That real end-to-end test
   belongs to the future `implement-software-repository-understanding`
   change.
6. The fixture boundary is designed (see design.md, once written) so a
   future real RU implementation can replace the fixture producer
   without requiring changes to CSM Builder's transformation contract
   or core implementation — the contract is the seam, not the fixture.
7. Fixture-based transformation/unit/component tests (delivered by
   this change) are clearly and structurally separated from the future
   real-pipeline integration test (not delivered by this change).

See `explore.md` for the full reasoning and the options considered
before this was resolved.

## Capabilities

### New Capabilities

None. This change implements the already-specified `csm-builder`
capability; it introduces no new requirement-level behavior.

### Modified Capabilities

None. `csm-builder`, `canonical-software-model`, and
`software-repository-understanding` are not modified — this change
builds code that conforms to specifications already approved and
archived. Accordingly, this change's `.openspec.yaml` sets
`skip_specs: true`.

## Out of Scope

- **Implementing Repository Understanding**, in whole or in part — a
  separate, future `implement-software-repository-understanding`
  change.
- **The real end-to-end integration test** (`Repository → RU →
  Repository Evidence → CSM Builder → CSM`) — cannot exist until a
  real RU implementation exists; reserved as a placeholder, not
  delivered here.
- **Module placement** (e.g. whether the Repository Evidence schema
  lives in `aip-core` or elsewhere) — a Design-phase decision.
- **Snapshot persistence technology, concurrency/parallelism
  mechanism, and the concrete native-scope-to-dependency-kind mapping
  table** — all explicitly deferred by `design.md` at the
  specification stage, and still to be decided in this change's own
  Design phase, not prematurely resolved in this proposal.
- **Any future `declared`- or `inferred`-knowledge-producing
  capability** (e.g. an Architecture Compliance Agent) — out of scope
  for CSM Builder entirely, per the archived spec's Shape A boundary.
- **Modifying any archived specification** — `canonical-software-model`,
  `software-repository-understanding`, and `csm-builder` are all
  treated as fixed inputs to this change.

## Impact

- **Affected specs**: none (`skip_specs: true`; no requirement-level
  changes).
- **Affected code**: introduces the first code in this repository —
  the CSM Builder implementation, its Repository Evidence fixture
  layer, and its test suites. Exact module/package location is a
  Design-phase decision.
- **Dependencies**: introduces Java/Maven project structure per
  `project.md` §10's technology direction (Java 21+). No dependency on
  `aip-analyzer` or any Repository Understanding implementation module.
  Any additional library dependency (e.g. for snapshot persistence) is
  a Design-phase decision, subject to CLAUDE.md's Dependency
  Discipline section.
- **Affects future work**: establishes the concrete implementation a
  future `implement-software-repository-understanding` change must
  interoperate with by replacing the fixture producer behind the same
  Repository Evidence contract; establishes the code a future CSM
  consumer (Analysis Framework, per `project.md` §11) will read
  snapshots from.

## Unresolved Questions for Design

Carried forward from Explore, not resolved here:

1. **Module placement** — where CSM Builder's code lives relative to
   CLAUDE.md's `aip-core → aip-analyzer → aip-rules → aip-ai →
   aip-cli/aip-server` chain, and whether the Repository Evidence
   schema belongs in `aip-core` alongside the CSM domain model.
2. **Snapshot persistence mechanism** — in-memory only, file-backed, or
   an embedded store; must at minimum satisfy the archived spec's
   "prior snapshots remain retrievable" requirement.
3. **Concurrency/parallelism mechanism** for the Mapping Orchestrator.
4. **Concrete format and location** for the versioned, per-build-system
   native-scope-string → CSM-dependency-kind mapping table.
5. **Fixture layer structure** — how contract-faithful fixtures are
   authored/organized so they can be swapped out cleanly once a real
   RU implementation exists (per Sequencing Decision item 6).
6. **Test suite organization** — concrete separation of fixture-based
   tests from the reserved-but-undeliverable real-pipeline integration
   test (per Sequencing Decision item 7).

## Sequencing Note

This change does not require, and should not wait for, a Repository
Understanding implementation to exist first — that is the entire point
of the resolved sequencing decision above. A future
`implement-software-repository-understanding` change remains
unassigned and is explicitly out of this proposal's scope.
