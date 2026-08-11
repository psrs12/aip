## Context

See `proposal.md` for motivation and `explore.md` for the sequencing
decision this design builds on. This document is implementation-facing:
it resolves the six concrete questions the proposal deliberately left
open, so that `tasks.md` and eventual implementation have a fixed
architecture to build against. This repository currently contains no
code — every module and package named below is a decision this design
introduces, not something being reorganized.

Binding constraints carried forward, unchanged by this design:

- CSM Builder depends only on the Repository Evidence contract (from
  the archived `software-repository-understanding` specification) and
  the CSM vocabulary (from the archived `canonical-software-model`
  specification). It SHALL NOT depend on `aip-analyzer` or on any
  Repository Understanding implementation.
- `csm-builder`, `canonical-software-model`, and
  `software-repository-understanding` are fixed inputs; none is
  modified by this change.
- No new CSM vocabulary, no AI/heuristic reasoning, no Architecture
  Component/Boundary/Business Context construction — all already
  excluded at the specification level; this design does not reopen
  them.

## Goals / Non-Goals

**Goals:**
- Resolve module placement for CSM Builder and the Repository Evidence
  contract, preserving the `aip-analyzer`/RU-implementation dependency
  prohibition.
- Choose a concrete, justified snapshot persistence mechanism.
- Define a deterministic, versionable, extensible mapping-table
  representation for native dependency scope classification.
- Choose a concurrency approach for the Mapping Orchestrator's first
  version.
- Define the contract-faithful Repository Evidence fixture layer's
  structure and its replacement seam.
- Define test organization that structurally prevents fixture-based
  tests from being mistaken for real Repository Understanding
  integration coverage.

**Non-Goals:**
- Implementing Repository Understanding, in whole or in part.
- Building the real `Repository → RU → Repository Evidence → CSM
  Builder → CSM` integration test — reserved, not delivered.
- Choosing exact third-party library versions or build-tool wiring
  details beyond what's needed to state the architecture (e.g., naming
  "a JSON serialization library" rather than pinning a version).
- Introducing any new CSM entity kind, relationship type, provenance
  category, confidence representation, or lifecycle vocabulary.
- Writing code, `pom.xml` files, or any implementation artifact — that
  is `tasks.md` and implementation, not this document.

## Decisions

### 1. Module Placement

**Problem:** CLAUDE.md's module chain (`aip-core → aip-analyzer →
aip-rules → aip-ai → aip-cli/aip-server`) names no home for CSM
Builder or the Repository Evidence contract. CSM Builder must depend on
both the CSM domain model and the Repository Evidence domain model,
but must not depend on `aip-analyzer`.

**Decision:**
- **`aip-core`** hosts both foundational, language-independent domain
  models as pure data/vocabulary with no framework dependencies:
  the CSM domain model (entities, relationships, provenance — already
  implied by `canonical-software-model`) and the **Repository Evidence
  contract** (Evidence Item, Evidence kind, identity scheme,
  change-status/lifecycle vocabulary — from
  `software-repository-understanding`). These live in separate
  packages within `aip-core` (e.g. `aip.core.csm` and
  `aip.core.evidence`), not merged into one namespace, so a future
  extraction of the Evidence contract into its own module remains
  cheap if warranted — but they are not split into separate modules
  now, since both are small, dependency-free schemas and no module
  other than `aip-analyzer` and the new CSM Builder module needs one
  without plausibly needing the other eventually.
- **A new module, `aip-csm-builder`**, hosts the Mapping Orchestrator,
  the Evidence-Kind Mapper registry/contract, every Mapper
  implementation, snapshot construction, incremental re-derivation,
  mapper versioning, and validation integration. It depends only on
  `aip-core`.
- **`aip-analyzer`** (a future module, not built by this change) will
  depend on `aip-core` to implement concrete Language
  Analyzers/Build-System Detectors/Configuration Recognizers that
  *produce* instances of `aip-core`'s Repository Evidence contract.

```
                         aip-core
              ┌───────────────────────────────┐
              │  aip.core.csm                  │  ◀── CSM domain model
              │  aip.core.evidence              │  ◀── Repository Evidence
              └───────────────┬─────────────────┘      contract
                    ▲          │          ▲
                    │          │          │
        depends on  │          │          │  depends on
                    │          │          │
        ┌───────────┴──┐       │      ┌───┴───────────┐
        │ aip-analyzer  │       │      │ aip-csm-builder │  ◀── THIS CHANGE
        │ (future;      │       │      │  (this change)  │
        │  not built    │       │      └───┬─────────────┘
        │  here)        │       │          │
        └───────────────┘       │          │
                                 │          ▼
                                 │      CSM snapshots
                                 │      (consumed by future
                                 │       aip-rules, etc.)
                                 ▼
                    (aip-analyzer produces Repository
                     Evidence; aip-csm-builder consumes
                     it — neither depends on the other)
```

This is a deliberate departure from reading CLAUDE.md's chain as
strictly linear: `aip-analyzer` and `aip-csm-builder` are **siblings**,
both depending only on `aip-core`, never on each other. This is the
only arrangement that satisfies the dependency prohibition while still
letting both modules share the same Repository Evidence types (so a
fixture and a future real RU implementation are interchangeable
producers of the exact same type, per Decision 5).

**Alternative considered:** fold CSM Builder's transformation logic
directly into `aip-core`. Rejected — `aip-core` is meant to stay a lean
domain-vocabulary layer; the Mapping Orchestrator, Mapper registry, and
snapshot lifecycle are substantial enough logic that bundling them in
would blur that boundary and make `aip-core` a much heavier dependency
for every future consumer of the CSM vocabulary alone.

**Alternative considered:** a separate `aip-evidence` module for the
Repository Evidence contract, distinct from `aip-core`. Rejected for
now — at this scale (two small schemas, one early consumer module) the
package-level separation inside `aip-core` gives the same intellectual
separation without an extra module and dependency edge to maintain;
revisit only if `aip-core` growth or a genuinely independent consumer
of Evidence-without-CSM ever justifies it.

### 2. Snapshot Persistence

**Problem:** Choose the first snapshot persistence mechanism —
durable, auditable, testable, without unjustified infrastructure.

**Decision: local filesystem, one immutable directory per snapshot.**
Each CSM Builder run writes a new snapshot as a self-contained
directory named by repository identifier and a monotonically
increasing sequence number (e.g.
`<repository-identifier>/snapshot-<n>/`), containing:
- The constructed CSM elements and relationships, serialized as
  structured, human-readable documents (one file or one JSON-Lines
  stream per CSM entity kind is sufficient for a first cut; exact
  layout is a `tasks.md`-level detail).
- A **Snapshot Manifest** — a small index recording, per CSM element
  identity: the Evidence-Kind Mapper and Mapper version that produced
  it, the originating Repository Evidence identity/identities, and a
  pointer into this snapshot's content files. This is CSM Builder's own
  internal bookkeeping (not CSM content, not new CSM vocabulary),
  directly analogous to the Discovery Manifest concept RU's own design
  already established one layer down.
- A small snapshot-level record of construction timestamp and
  validation outcome (see Decision on Snapshot Validation, already
  fixed at the specification level).

A snapshot directory, once written, is never modified — satisfying
"individually identifiable... rather than overwritten in place"
directly and mechanically. Incremental re-derivation (per the archived
spec) reads the prior snapshot's Manifest to determine which CSM
elements can be carried forward without invoking their Mapper again.

**Rationale:** This is the simplest mechanism that is genuinely
durable (survives process restarts, unlike in-memory-only), genuinely
auditable (a human or a script can inspect a snapshot directory
directly — fitting AIP's own explainability principle), and requires
no new runtime infrastructure (no database server, no embedded-engine
dependency) — only a filesystem and a serialization format. It is also
trivially testable: tests write to a temporary directory and read it
back, with no server or fixture cleanup beyond deleting a directory.

**Alternatives considered:**
- *In-memory only*: rejected — the archived spec's "prior snapshots
  remain retrievable" requirement is untestable across process
  restarts with no persistence at all, and this would make the
  incremental-re-derivation requirement partly moot (nothing to
  compare against on a fresh process).
- *Embedded database (e.g. SQLite)*: rejected for v1 as unjustified
  infrastructure at this stage — it would add a real new dependency
  and schema-migration surface for a benefit (transactional queries
  over CSM content) that isn't yet needed; large-repository query
  performance is explicitly RU's `Large-Repository Scalability`
  concern, not something CSM Builder's snapshot storage needs to solve
  on day one. Revisit if/when real usage data justifies it.

### 3. Mapping-Table Representation

**Problem:** Define how native dependency scope strings map to CSM's
`compile-time`/`runtime`/`test-only` vocabulary, deterministically,
explicitly, versionably, and extensibly, without new CSM vocabulary.

**Decision:** a **versioned, declarative mapping resource per build
system** (e.g. one small structured data file per build system —
`maven-scope-mapping`, `npm-scope-mapping`, and so on — bundled as
`aip-csm-builder` resources), each carrying an explicit version tag and
a flat table of native scope string → one of CSM's three already-
approved dependency-kind values. Loaded once at startup into an
immutable in-memory lookup used by the Dependency Kind Classification
logic. A native scope string with no entry in the table for its build
system yields no kind qualifier — never a guess — exactly as the
archived spec requires.

**Rationale:** A declarative, external table is more explicit and more
independently reviewable/extensible than a code-embedded switch
statement — a reviewer or architect can read and extend the mapping
without reading Mapper code, and each file's own version tag gives
"versionable" a concrete, inspectable meaning. It is deterministic by
construction (a flat lookup, never pattern-matching or heuristics) and
introduces zero new CSM vocabulary — the only values it can ever
produce are the three CSM already defines, or nothing.

**Alternative considered:** hardcoded per-build-system mapping in code
(an enum or switch statement). Rejected as the primary mechanism —
conflates reference data with code, forcing a full code change and
review cycle to extend coverage to a new build system's scope
vocabulary, and has no natural place to carry a version tag the way a
data file does. Remains a reasonable fallback if the resource-loading
mechanism is judged premature at implementation time, but the
data-file approach is the design decision.

### 4. Mapping Orchestrator Concurrency

**Problem:** Choose a concurrency approach for the Mapping
Orchestrator's first version, preserving deterministic output
regardless of execution order.

**Decision: sequential, single-threaded execution for v1**, with
Evidence Items processed in a fixed, deterministic order (sorted by
Evidence identity) so that two runs against unchanged input produce
byte-identical construction order, not merely identical final content.

**Rationale:** CSM element/relationship identity is already a pure,
order-independent function of Evidence identity (per the archived
spec's identity-derivation requirements), so the *set* of constructed
CSM content would be identical under parallel execution too — but
achieving that safely would require additional synchronization or a
post-hoc stabilization/sort pass before snapshot serialization, which
is unjustified complexity for a first version with no demonstrated
throughput problem. Each Evidence-Kind Mapper invocation is already
architected as an independent, side-effect-free unit of work (Decision
1 of the archived `design.md`), so nothing about sequential execution
forecloses introducing a concurrent executor later — it is a
substitutable implementation detail behind the same Mapper contract,
not an architectural commitment.

**Alternative considered:** a parallel executor (e.g. a fixed thread
pool processing Evidence Items concurrently) from the start. Rejected
for v1 — no current performance requirement justifies the added
complexity (synchronizing Snapshot Manifest writes, ensuring
deterministic serialization order despite non-deterministic completion
order), and premature parallelism here would violate the same
"avoid unjustified infrastructure" instinct behind Decision 2.

### 5. Fixture Layer

**Problem:** Define contract-faithful Repository Evidence fixtures,
representing only evidence the archived RU specification defines, in a
shape a future real RU implementation can replace without CSM Builder
changing.

**Decision:** Fixtures are constructed **using `aip-core`'s own
Repository Evidence contract types** — never a parallel or
fixture-specific schema. A small fixture-building API (e.g. a
`RepositoryEvidenceModelBuilder`) assembles valid instances of those
types: `Repository`, `Project` (including the reserved unmanaged
Project), `Module` (including the default Module case), `Package`,
`SourceUnit`, Method-level evidence, `ManifestDependencyEdge`,
`ImportEdge`, and `ApiContractDeclaration` Evidence Items, each
carrying the identity, kind-specific attributes, discovery outcome
status, and extraction-method tag the archived RU spec requires — and
nothing else. A small library of named fixture scenarios is
maintained, each explicitly traced (by comment or naming convention) to
the specific RU specification requirement/scenario it faithfully
represents (e.g. a fixture representing a `partial` `SourceUnit`
traces to RU's `Partial-Analysis Outcome Reporting` requirement).

This fixture-building code lives **inside `aip-csm-builder`'s test
sources**, in a clearly separated package (e.g.
`aip.csmbuilder.test.fixtures`) that CSM Builder's own production code
never imports — only test code does. This is what makes the future
replacement possible without touching CSM Builder: CSM Builder's
production code depends only on `aip-core`'s Repository Evidence
*types*, never on how an instance of them was produced. A future real
`aip-analyzer`-based RU implementation is simply a second, independent
producer of the same `aip-core` types, wired in at whatever future
integration point consumes both — CSM Builder itself does not change.

**Rationale:** Building fixtures directly against the real contract
types (rather than a fixture-specific DSL or mock) is what makes
"contract-faithful" a structural guarantee rather than a discipline to
remember — an invalid fixture (e.g. missing a required field) simply
fails to compile or construct, the same as any other misuse of the
Repository Evidence contract would.

### 6. Test Organization

**Problem:** Structurally separate fixture-based CSM Builder tests from
the future real-pipeline integration test, so the two are never
conflated.

**Decision:** a fixed naming and location convention, enforced by
review (and, where practical, by a lint/CI check per the archived
spec's own `18.5` test-coverage task):
- **Unit tests** (`*MapperTest`, identity-derivation tests, provenance
  tests, etc.) — exercise one Mapper or mechanism in isolation against
  hand-built fixture Evidence Items. Ordinary unit-test naming; no
  special marker needed since these obviously don't claim to be
  integration tests.
- **Contract tests** (e.g. `EvidenceKindMapperContractTest`) — a shared
  test suite run against every registered Mapper, verifying the
  Mapper contract itself (identity/provenance/traceability
  obligations), independent of any one Mapper's specific logic.
- **Fixture-based end-to-end transformation tests** — exercise the full
  `Repository Evidence fixture → CSM Builder → CSM` path. These MUST
  be named/packaged to make their fixture basis unmistakable (e.g.
  package `aip.csmbuilder.test.fixture`, class names ending
  `FixtureTest` or `FixtureEndToEndTest`). **No test file, class, or
  package delivered by this change may contain "RepositoryUnderstanding"
  or "RU" in a name implying real-pipeline integration** — that naming
  is reserved for the future real test.
- **Reserved real-pipeline integration test** — not built by this
  change. Its future name and location are fixed now so the seam is
  explicit rather than improvised later: a single test class named
  along the lines of `RepositoryToCsmPipelineIntegrationTest`,
  documented (via a code comment or a tracked follow-up task, not a
  written test body) as pending the future
  `implement-software-repository-understanding` change, exercising the
  full `Repository → Repository Understanding → Repository Evidence →
  CSM Builder → CSM` chain once a real RU implementation exists to
  supply the middle of it.

**Rationale:** Naming is the cheapest possible safeguard against the
exact risk named in the proposal — a fixture-based test being mistaken
for real RU integration coverage. Fixing the *future* test's name now,
even though it isn't written yet, prevents the seam from being
improvised inconsistently whenever RU implementation eventually
happens.

### 7. Exposure/Consumption Relationship Target Representation

**Problem:** The archived `csm-builder` specification's `API Contract
Relationship Construction` requirement is unconditional — "CSM Builder
SHALL map an `ApiContractDeclaration` Evidence Item to an
`exposure/consumption` relationship" — while its own "no invented
consumer" rule (and the archived design's Decision 9) forbids
populating that relationship's target with a fabricated consumer
entity when, as is true of every `ApiContractDeclaration` Evidence Item
RU currently produces, no consumer is evidenced at all. `aip-core`'s
`CsmRelationship.targetId` was originally a mandatory `CsmElementId`,
with no representation for "this relationship exists, but its other
end is not yet known." This is a gap in the archived design, not a
reopening of it — Decision 9 already committed to constructing the
relationship without a consumer; it simply never specified what the
mandatory field should hold in that case.

**Decision:** `CsmRelationship.targetId` becomes `Optional<CsmElementId>`
in `aip-core`. Absent means exactly "no target/consumer has been
evidenced" — never "the target is the source itself" and never an
invented placeholder entity. Every relationship-construction site that
already has both ends (`CONTAINMENT`, `DEPENDENCY`,
`INTEGRATION`/External System) continues to always populate a concrete
target via the added `CsmRelationship.of(...)` convenience factory;
only the `ApiContractDeclaration → exposure/consumption` mapping (the
one case whose originating evidence supplies no target identity at
all) uses the new `CsmRelationship.withUnevidencedTarget(...)` factory.
This is a change to `aip-core`'s CSM domain-model *type*, not to the
archived `canonical-software-model` specification's text — that
specification never mandates every relationship carry a resolved
target, and this design's own binding constraint that the archived
specs are fixed inputs is about their content, not about the
implementation types this change itself introduces in `aip-core`
(Decision 1).

**Rejected alternatives:**
- *Self-loop (`targetId == sourceId`)* — reads as "the Type/Method
  consumes itself," which is false and would silently corrupt any
  future analysis walking `exposure/consumption` edges (e.g. counting
  distinct consumers, or graphing exposure fan-out).
- *Skip relationship construction entirely when no consumer is known*
  — contradicts the requirement's own first scenario ("CSM Builder
  processes an `ApiContractDeclaration` Evidence Item... SHALL
  construct an `exposure/consumption` relationship"), and would
  silently drop the structural summary attribute the requirement also
  says must be carried.

**Consequence:** because `ApiContractDeclaration` evidence supplies no
consumer-identifying fact under RU's current evidence coverage (see the
archived RU specification's `Deterministic API Relationship Discovery`
requirement), every `exposure/consumption` relationship CSM Builder
constructs today has an absent target in practice. Task 12.2's "no
invented consumer" guard is therefore mechanically trivial today (there
is no consumer-lookup path to invent one from) but remains load-bearing
for whenever a future evidence kind or RU capability supplies
consumer-identifying facts — at that point, a target becomes populated
only by resolving to CSM content that already exists from that other
evidence, never fabricated here.

## Risks / Trade-offs

- [Co-locating the CSM and Evidence domain models in `aip-core`
  (Decision 1) could let `aip-core` grow unbounded as more capabilities
  are added] → Mitigation: strict package separation from day one
  (`aip.core.csm` vs. `aip.core.evidence`); revisit module extraction
  if growth or a new independent consumer ever justifies it.
- [File-based snapshot persistence (Decision 2) may not scale well to
  very large repositories or very high snapshot-retention volumes long
  term] → Mitigation: accepted for v1 per the explicit "simplest
  durable mechanism, no unjustified infrastructure" instruction;
  large-repository scalability is RU's own concern at the evidence
  layer, and this mechanism can be replaced behind the same snapshot
  abstraction if it ever becomes a real bottleneck.
- [Sequential Mapping Orchestrator execution (Decision 4) may become a
  throughput bottleneck on large repositories] → Mitigation:
  architecture doesn't preclude a future concurrent executor; each
  Mapper invocation is already independent and side-effect-free, so
  parallelizing later is a substitution, not a redesign.
- [Declarative mapping-table files (Decision 3) can drift from real
  build-system scope vocabularies as new build systems or scope
  conventions emerge] → Mitigation: same risk already accepted at the
  specification level; versioned files, an honest "unmapped = no
  qualifier" fallback, and straightforward extensibility (add a file,
  not a code change) keep drift low-cost to fix.
- [Fixtures (Decision 5) could subtly diverge from what a real RU
  implementation eventually produces for the same scenario, if RU's
  real behavior differs from this design's reading of the archived RU
  spec in some edge case] → Mitigation: fixtures are typed directly
  against `aip-core`'s real contract types (not a parallel schema), and
  each fixture traces to a specific RU spec requirement/scenario; the
  future `implement-software-repository-understanding` change should
  include a task validating real RU output against the same traced
  fixture scenarios.

## Migration Plan

Greenfield — no existing implementation to migrate from. This design
does not require, and should not wait for, a Repository Understanding
implementation to exist first, per the locked sequencing decision
(`explore.md`, `proposal.md`). The reserved real-pipeline integration
test (Decision 6) is the explicit, named seam a future
`implement-software-repository-understanding` change should fill in,
without this change or its module boundaries needing to change to
accommodate that future work.

## Open Questions

- Exact serialization format/library for snapshot content and the
  Snapshot Manifest (Decision 2) — an implementation choice; this
  design only commits to "structured, human-readable, filesystem-based,
  immutable per snapshot."
- Exact snapshot directory/file naming scheme beyond "keyed by
  repository identifier and a monotonically increasing sequence number"
  (Decision 2) — implementation detail.
- Exact test framework conventions (e.g. JUnit version, assertion
  library) — not an architectural decision.
- Whether the fixture-building package (Decision 5) is ever worth
  extracting into its own module if a second consumer besides CSM
  Builder's own tests emerges — not needed now, revisit if it happens.
