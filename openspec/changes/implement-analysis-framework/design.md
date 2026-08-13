## Context

`design.md` and `spec.md` under `openspec/changes/define-analysis-framework/`
are the approved source of truth for *what* Analysis Framework does;
this document covers *how* it is implemented, the same relationship
`implement-csm-builder/design.md` had to `define-csm-builder`. Two
questions raised during this change's own `explore.md` were resolved
by explicit direction before this phase began, and are binding,
carried forward unchanged:

1. **`AnalysisResult` placement**: promoted to `aip-core` as the
   canonical framework-level artifact type — one logical artifact, not
   split into separate identity/traceability/payload core types; its
   payload stays Analyzer-defined and opaque to `aip-core`. Justified
   by `CsmElement`/`AnalysisView` precedent, not `CsmSnapshotSource`'s
   own decomposable shape.
2. **No real `aip-csm-builder` → `CsmSnapshotSource` adapter** in this
   change. `aip-analysis` stays fully independent of `aip-csm-builder`;
   fixtures are sufficient; a real adapter, if ever built, is a
   producer-side (`aip-csm-builder`) follow-up, not this change's
   concern.

Binding external facts this design does not reopen:
- `aip-core` already hosts the CSM domain model (`aip.core.csm`) and
  the Repository Evidence contract (`aip.core.evidence`), both
  implemented and working.
- `aip-csm-builder` already hosts a full, working implementation:
  `Snapshot` (`repositoryIdentifier: String, sequenceNumber: long,
  constructionTimestamp, elements, relationships, manifest`),
  `SnapshotStore`, `SnapshotManifest`, `CsmValidator`/
  `SnapshotPublisher`. `Snapshot`'s own identity shape
  (`repositoryIdentifier` + `sequenceNumber`) is precedent for
  Decision 3 below, not a dependency.
- `define-analysis-framework`'s seven binding decisions (module
  placement, Analyzer independence, `AnalysisView` in `aip-core`,
  Result identity/traceability/store, validation/publish gate,
  sequential-v1 concurrency, Analysis Scope vocabulary) are unmodified.
- `define-rule-framework/design.md` Decision 3 already commits to "one
  general Scope declaration type both Analysis Scope and Rule Scope
  are defined in terms of," hosted in `aip-core` — already-approved,
  cross-capability architecture this change implements one layer
  early (Decision 2, below), not invents.

## Goals / Non-Goals

**Goals:**
- Give `AnalysisResult`, `CsmSnapshotSource`, `AnalysisView`, and the
  shared Scope declaration type concrete Java shapes.
- Resolve `AnalysisResultStore`'s own module placement, left open by
  `explore.md`'s question 3.
- Establish the fixture layer's shape and scope-enforcement mechanism.
- Establish test organization, mirroring `implement-csm-builder`.

**Non-Goals:**
- Reopening any `define-analysis-framework` decision.
- A real `aip-csm-builder` adapter (Binding Decision 2).
- Rule Framework, Finding Model, or Agent Framework implementation.
- Choosing `AnalysisResultStore`'s concrete persistence technology —
  deferred, per `define-analysis-framework/design.md` Decision 4
  itself.
- Implementing Repository Understanding, or resolving CSM Builder's
  own pre-existing upstream gap.

## Decisions

### 1. `AnalysisResult`'s concrete shape: a single, non-generic `aip-core` type with an opaque `Object` payload

**Problem:** Given Binding Decision 1 (promotion to `aip-core`), what
is `AnalysisResult`'s concrete Java shape — generic over its payload
type, or not?

**Decision:** `AnalysisResult` is a **final class** (not sealed — there
is no closed set of "kinds" the way `CsmElement` has eight; it is one
artifact kind with Analyzer-defined variation only in its payload) in
`aip.core.csm`, carrying: its identity (Decision 4's
`AnalysisResultId`), the producing Analyzer's identifier and version,
the source `CsmSnapshotSource` identity (`CsmSnapshotId`, Decision 3),
the Analysis Scope instance covered, and an **`Object` payload field**
— non-generic. A consumer that knows the concrete Analyzer casts the
payload itself; `aip-core` neither constrains nor interprets it,
mirroring `NativeAttributes`'s own escape-hatch precedent and
`define-analysis-framework/design.md` Decision 4's explicit "opaque,
Analyzer-defined payload" language.

**Alternatives considered:**
- **`AnalysisResult<P>`, generic over payload type `P`.** Rejected —
  would give a caller holding a concrete Analyzer's own result strong
  typing, but forces every framework-level collection, registry, and
  future `AnalysisResultSource`-style contract to work with wildcard
  types (`AnalysisResult<?>`), and gains little: the payload is
  explicitly opaque to the framework by design, so framework code
  never benefits from the type parameter, only application code would
  — application code can cast once at its own boundary instead.
- **A `Map<String, ?>`-shaped payload**, mirroring `NativeAttributes`
  exactly. Rejected — `NativeAttributes` fits CSM's own flat,
  string-keyed native-evidence-attribute vocabulary; an Analyzer's
  result (e.g. a full dependency-cycle graph) is a poor fit for a flat
  map without an Analyzer inventing its own serialization into map
  entries, adding friction `Object` avoids.

### 2. Shared Scope declaration type: `CsmScope`, defined in `aip-core` now

**Problem:** Does this change define Analysis Scope as
`aip-analysis`-local, or build the general, `aip-core`-hosted Scope
type `define-rule-framework/design.md` Decision 3 already commits to
sharing with a future Rule Scope?

**Decision:** Define **`CsmScope`** in `aip.core.csm`: a declared,
non-empty set of `CsmEntityKind` and/or `CsmRelationshipType` values,
an optional containment-level anchor (a `CsmEntityKind`), and an
optional native-attribute predicate (a `Predicate<NativeAttributes>`
or equivalent). Analysis Scope *is* `CsmScope` — no
`aip-analysis`-local wrapper type. This is not new architecture: it is
implementing, one `implement-*` change early, a cross-capability
decision Rule Framework's own Design phase already made and bound
itself to. Building it generally now avoids the same
"promote-after-the-fact" migration `CsmSnapshotSource`'s own
introduction (one layer down) already had to absorb once.

**Alternatives considered:**
- **`AnalysisScope`, `aip-analysis`-local, promoted to `aip-core`
  later when `implement-rule-framework` needs it.** Rejected — the
  promotion decision was already made by `define-rule-framework`;
  deferring the *implementation* of an already-approved decision to a
  later change only recreates the migration this project has
  repeatedly chosen to avoid when a future consumer is already named.

### 3. `CsmSnapshotSource` and `CsmSnapshotId`: concrete shape

**Problem:** What does `CsmSnapshotSource` concretely expose, and what
is its identity type?

**Decision:** `CsmSnapshotSource` is an interface in `aip.core.csm`
exposing: `Set<CsmElement> elements()`, `Set<CsmRelationship>
relationships()`, and `CsmSnapshotId id()`. **`CsmSnapshotId`** is a
new `aip-core` record — `CsmSnapshotId(String repositoryIdentifier,
long sequenceNumber)` — conceptually aligned with, but structurally
independent of, `aip-csm-builder`'s own `Snapshot(repositoryIdentifier,
sequenceNumber, ...)` identity shape (Context, above). No dependency
is created by this alignment; a future adapter (Binding Decision 2)
would simply map `Snapshot`'s two identity fields onto
`CsmSnapshotId`'s two fields directly, if and when it is built.

**Alternatives considered:**
- **A single opaque `String` or `UUID` snapshot identity.** Rejected —
  loses the repository-identifier/sequence-number structure every
  other identity scheme in this project already exposes structurally
  (e.g. `AnalysisResult`'s own identity, Decision 4, needs to reference
  "source CSM Snapshot identity" meaningfully, not an opaque token).

### 4. `AnalysisResult` identity: `AnalysisResultId`

**Problem:** What concrete type represents `AnalysisResult`'s
deterministic identity?

**Decision:** `AnalysisResultId`, a new `aip-core` record, is a
deterministic function of (Analyzer identifier, Analyzer version,
`CsmSnapshotId`, Analysis Scope instance) — computed via a stable hash
or structural equality over those four components (implementation
detail; the requirement is reproducibility, not a specific algorithm).
Never random or `UUID`-based, per `define-analysis-framework/design.md`
Decision 4, unmodified.

**Alternatives considered:**
(none beyond what Decision 4 of `define-analysis-framework/design.md`
already settled — this decision only fixes the concrete type, not the
identity scheme itself)

### 5. `AnalysisResultStore` stays in `aip-analysis`, not promoted to `aip-core`

**Problem:** Now that `AnalysisResult` itself lives in `aip-core`
(Binding Decision 1), should `AnalysisResultStore` — the persistence
abstraction — move there too?

**Decision:** **No — `AnalysisResultStore` stays in `aip-analysis`**,
mirroring `SnapshotStore`'s own precedent exactly: `SnapshotStore`
stays in `aip-csm-builder` even though `CsmElement`/`CsmRelationship`
(what it stores) live in `aip-core`. The type promoted to `aip-core`
is the *artifact* (`AnalysisResult`, mirroring `CsmElement`); the
*store abstraction* (write/read/list operations, retry, batching, and
eventually a chosen persistence technology) is a producer-module
concern. A future `AnalysisResultSource` (`define-rule-framework`'s
own, not built by this change) is a separate, smaller, read-only
`aip-core` contract — the same relationship `CsmSnapshotSource` already
has to `SnapshotStore` one layer down, not a reason to promote
`AnalysisResultStore` itself.

**Alternatives considered:**
- **Promote `AnalysisResultStore` to `aip-core` alongside
  `AnalysisResult`.** Rejected — no established precedent for
  promoting a full read/write store abstraction to `aip-core`;
  `SnapshotStore` didn't move when `CsmElement` was defined there, and
  `AnalysisResultSource`'s own future existence (a read-only contract)
  is sufficient for `aip-rules`'s own needs without this.

### 6. Fixture layer and scope enforcement

**Problem:** How is `CsmSnapshotSource` tested without a real producer?

**Decision:** A test-only fixture-building API, `aip-analysis`-local,
analogous to `aip-csm-builder`'s own `RepositoryEvidenceModelBuilder`
— constructs `CsmSnapshotSource`-conforming instances directly from
`aip-core` types (`CsmElement`, `CsmRelationship`, `CsmSnapshotId`), no
production code path ever depends on it. A build/lint check (mirroring
`aip-csm-builder`'s `check-fixture-package-scope.sh`) confirms the
fixture package is never imported outside test scope.

**Alternatives considered:**
(none — this directly implements `define-analysis-framework/tasks.md`
Section 12 and `implement-csm-builder`'s own precedent verbatim; no
genuine fork exists here)

### 7. Test organization

**Problem:** How are tests organized across unit/contract/fixture/
end-to-end levels?

**Decision:** Mirror `implement-csm-builder`'s own organization: unit
tests per type (identity, traceability, validation), contract tests
for `CsmSnapshotSource`/`AnalysisView`/`AnalysisResult`/`Analyzer`
independent of any concrete implementation, and one fixture-based
end-to-end test exercising the full Analysis Orchestrator against a
representative multi-Analyzer scenario (`define-analysis-framework/
tasks.md` 13.2).

**Alternatives considered:**
(none — directly follows established precedent)

## Risks / Trade-offs

- [`AnalysisResult` now lives in `aip-core` (Binding Decision 1), the
  first framework-level artifact type placed there since `CsmElement`
  itself] → `aip-core` grows a second "many producers, opaque payload"
  type; accepted as the correct extension of established precedent,
  not a scope-creep risk, since `aip-core`'s own role (general,
  producer-agnostic vocabulary) is exactly what this fits.
- [No real `aip-csm-builder` adapter (Binding Decision 2) means this
  change, once complete, has no live producer feeding it — mirroring
  CSM Builder's own "no live consumer" position relative to Rule
  Framework before this change] → Named explicitly, consistent with
  how `implement-csm-builder` itself named the mirror-image gap; a
  future small integration change is the natural resolution, not
  built here.
- [`CsmScope` (Decision 2) is implemented ahead of Rule Framework's own
  `implement-rule-framework` change actually needing it] → Low risk:
  the shape was already fixed by `define-rule-framework/design.md`
  Decision 3; if that decision is ever revisited, this change's own
  `CsmScope` would need to change too, but no more so than any other
  already-approved cross-capability contract this project builds
  ahead of its second consumer.
- [`AnalysisResult`'s `Object` payload (Decision 1) sacrifices
  compile-time type safety for framework-level flexibility] → Accepted
  v1 trade-off, consistent with `NativeAttributes`'s own precedent;
  revisit only if concrete Analyzer implementations demonstrate a
  concrete need for stronger typing.

## Migration Plan

None — `aip-analysis` is a new module; no existing code changes.

## Open Questions

None remaining — both questions `explore.md` raised were resolved
before this phase began, and no new fork was found while giving them
concrete shape.
