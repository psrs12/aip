## Context

`design.md` and `specs/finding-model/spec.md` under
`openspec/changes/define-finding-model/` are the approved source of
truth for *what* Finding Model does; this document covers *how* it is
implemented, the same relationship `implement-rule-framework/design.md`
has to `define-rule-framework`. Four points raised in `proposal.md`'s
own Binding Decisions are resolved concretely here, carried forward as
settled:

1. **`Finding` placement**: promoted to `aip-core`, mirroring
   `AnalysisResult`'s and `RuleEvaluationResult`'s own placement
   exactly.
2. **`FindingMetadata` contract**: a new `aip-core` interface a
   `RuleEvaluationResult`'s payload MAY implement, resolving how
   Category/Severity/Description/Impact are actually obtained.
3. **Outcome qualification**: `FAIL` outcome + `FindingMetadata`-
   conforming payload, or no Finding is constructed.
4. **Unanchored "concerned element" resolution**: a Finding-Model-
   local, deterministic function of `repositoryIdentifier` alone, not
   the literal `aip-csm-builder`-constructed Repository element
   identity (out of reach without a new cross-module dependency).

Binding external facts this design does not reopen:
- `aip-core` already hosts (per implementation, not just plan)
  `RuleEvaluationResult`, `RuleEvaluationResultId`,
  `RuleEvaluationOutcome`, `CsmScope`/`CsmScopeInstance`,
  `CsmSnapshotId`, `CsmElementId`, `AnalysisResultId`, and
  `DeterministicHash` — all implemented and merged
  (`implement-rule-framework`).
- `aip-rules` already hosts a full, working implementation (unchanged
  by this change).
- `RuleEvaluationResult`'s concrete shape (final class, non-generic,
  `Object` payload, `ruleIdentifier`/`ruleVersion`/`sourceSnapshotId`/
  `scopeInstance`/`consumedAnalysisResultIds`/`outcome`/`payload`
  fields) is fixed and not modified by this change.
- `define-finding-model`'s eleven binding decisions (Finding as a
  distinct artifact referencing a non-empty `RuleEvaluationResult`
  identity set; two independent identity concepts; no mutable
  lifecycle state; the v1 Finding field set; direct traceability
  retention; no aggregation; no Scope concept; validate-before-publish
  trusting the prior layer's own gate; `aip-findings` module placement;
  `RuleEvaluationResultSource` in `aip-core`) are unmodified.

## Goals / Non-Goals

**Goals:**
- Give `RuleEvaluationResultSource`, `Finding`, `EvaluationIdentity`,
  `LogicalFindingIdentity`, and `FindingMetadata` concrete Java shapes.
- Resolve `FindingStore`'s module placement.
- Resolve the two implementation-level gaps named in Context (points 2
  and 4) with a concrete, testable mechanism.
- Establish the fixture layer's shape.
- Establish test organization, mirroring `implement-rule-framework` and
  `implement-analysis-framework`.

**Non-Goals:**
- Reopening any `define-finding-model`, `define-rule-framework`, or
  `define-analysis-framework` decision.
- A real `aip-rules` adapter.
- Agent Framework implementation, Recommendation generation, or any
  lifecycle presentation mechanism.
- Choosing `FindingStore`'s concrete persistence technology — deferred,
  per `define-finding-model/design.md` Decision 9 itself.
- Reaching the literal `aip-csm-builder`-constructed Repository element
  identity for the unanchored case (Context point 4) — a real gap,
  named and accepted, not solved by introducing a new dependency.

## Decisions

### 1. `Finding`'s concrete shape: a single, non-generic `aip-core` type

**Problem:** Given the placement decision (promotion to `aip-core`),
what is `Finding`'s concrete Java shape?

**Decision:** `Finding` is a final, non-generic class in
`aip.core.csm`, mirroring `RuleEvaluationResult`'s own shape:

```java
public final class Finding {
  EvaluationIdentity id;                          // Finding ID
  LogicalFindingIdentity logicalFindingIdentity;
  String ruleIdentifier;
  CsmSnapshotId sourceSnapshotId;
  CsmElementId concernedElementId;                // Location
  Set<RuleEvaluationResultId> referencedRuleEvaluationResultIds; // non-empty
  String category;
  String severity;
  Confidence confidence;                          // fixed to HIGH (Decision 6)
  String description;
  String impact;
}
```

No `Recommendation` or `Remediation availability` field exists at all
— per `define-finding-model/design.md` Decision 5, these are left
absent, not defaulted to an empty/null placeholder value; adding an
always-empty field would misstate that a value was considered and
withheld, rather than never modeled in v1.

`Category` and `Severity` are plain `String` — `project.md` §4.8 does
not name a fixed vocabulary for either, and no CSM or Rule Framework
type already exists to reuse (unlike `Confidence`, Decision 6 below).
Introducing a speculative enum for either now would be inventing
structure `project.md` does not require.

**Alternatives considered:**
- **A generic `Finding<T>` parameterized by diagnostic payload type**,
  mirroring how `RuleEvaluationResult` might have been generic.
  Rejected — `RuleEvaluationResult` itself was deliberately kept
  non-generic (`implement-rule-framework/design.md` Decision 1's own
  reasoning: Category/Severity/Description/Impact are already
  extracted, typed fields on `Finding` itself via `FindingMetadata`,
  not a raw opaque payload Finding needs to stay generic over).

### 2. `EvaluationIdentity` and `LogicalFindingIdentity`: two `aip-core` records

**Problem:** Concrete shape for `define-finding-model/design.md`
Decision 3's two identity concepts?

**Decision:** Two single-field records in `aip.core.csm`, mirroring
`RuleEvaluationResultId`'s own shape:

```java
public record EvaluationIdentity(String value) { ... }
public record LogicalFindingIdentity(String value) { ... }
```

- `EvaluationIdentity.of(Set<RuleEvaluationResultId> referenced)` —
  `DeterministicHash` over the referenced set's values, sorted before
  hashing (identical reasoning to `RuleEvaluationResultId`'s own
  `consumedAnalysisResultIds` sort — `Set` iteration order must never
  affect the digest).
- `LogicalFindingIdentity.of(String ruleIdentifier, CsmElementId
  concernedElementId)` — `DeterministicHash` over exactly these two
  components, deliberately excluding `sourceSnapshotId`, Rule version,
  and `RuleEvaluationResultId` — the entire point of this identity
  (`define-finding-model/design.md` Decision 3).

Two distinct record types, not one record with a discriminant field —
mirroring the project's own established precedent of one record type
per identity concept (`AnalysisResultId`, `RuleEvaluationResultId`
are each their own type); conflating the two into one type with a
"kind" tag would make it possible to compare an `EvaluationIdentity`
against a `LogicalFindingIdentity` by accident, exactly what `spec.md`'s
`Evaluation Identity and Logical Finding Identity Are Distinct`
requirement exists to prevent.

**Alternatives considered:**
- **Reuse `RuleEvaluationResultId`'s own record type for both**, since
  all three are just `String value` wrappers. Rejected — type-level
  distinctness is the entire mechanism preventing accidental
  conflation; collapsing them into one type (or into raw `String`)
  would make the compiler unable to catch a swapped argument.

### 3. `FindingMetadata`: the Category/Severity/Description/Impact provenance contract

**Problem:** Concretely, how does Finding Model obtain
Category/Severity/Description/Impact from "the producing Rule Type's
static configuration" (`define-finding-model/design.md` Decision 5)
without `aip-findings` depending on `aip-rules`, given
`RuleEvaluationResult.payload()` is a plain, Rule-Type-opaque `Object`?

**Decision:** A new `aip-core` interface, `FindingMetadata`:

```java
public interface FindingMetadata {
  String category();
  String severity();
  String description();
  String impact();
}
```

A Rule Type whose `FAIL` outcomes should produce Findings populates its
`RuleTypeEvaluation` payload (in `aip-rules`, unchanged by this
change) with a value implementing this interface. Finding construction
checks `payload instanceof FindingMetadata` and reads these four
methods through the interface only — never by branching on a payload's
concrete Rule-Type-specific class, which would violate `Extension
Behavior — New Rule Types Require No Finding Model Changes`. This is
additive to `aip-core`: `RuleEvaluationResult.payload()`'s declared
type remains `Object`, unconstrained; `FindingMetadata` is a structural
contract a payload value may or may not satisfy, not a new required
field on `RuleEvaluationResult` itself, so no already-approved Rule
Framework type or requirement is modified.

This directly satisfies `Extension Behavior`'s own conditional clause
— "provided that Rule Type's Rule declares the Category and Severity
configuration this specification requires" — by giving that clause a
concrete mechanism: implementing `FindingMetadata` on the payload *is*
"declaring" that configuration, in a way Finding Model can read
generically, with zero Rule-Type-specific code in `aip-findings`.

**Alternatives considered:**
- **A separate, `aip-findings`-local registry mapping Rule Type
  identifier to static Category/Severity/Description/Impact values**,
  configured independently of the Rule Type itself. Rejected —
  duplicates configuration that already exists once, at the Rule
  Type's own registration; two independently-maintained copies could
  drift, and `define-finding-model/design.md` Decision 5 is explicit
  that these values are "taken directly from" Rule Type configuration,
  not independently re-declared.
- **Require every `RuleEvaluationResult` to carry Category/Severity as
  new, first-class fields.** Rejected — this reopens
  `RuleEvaluationResult`'s already-approved, already-implemented shape
  (`implement-rule-framework/design.md` Decision 1), explicitly
  out of scope.
- **Add a fifth `aip-rules` method to the `RuleType` contract
  returning Category/Severity/Description/Impact directly, and have
  `aip-findings` depend on `aip-rules` to call it.** Rejected — directly
  contradicts `define-finding-model/design.md` Decision 11's binding
  "`aip-findings` depends on `aip-core` only" choice; would also
  require `aip-findings` to hold a live `RuleTypeRegistry` reference,
  which the `RuleEvaluationResultSource`-only consumption model never
  requires elsewhere.

### 4. Outcome qualification: `FAIL` + `FindingMetadata`-conforming payload only

**Problem:** Which `RuleEvaluationResult`s "qualify" for Finding
construction — a question `explore.md` named and `design.md`/`spec.md`
never explicitly settled?

**Decision:** A `RuleEvaluationResult` qualifies for Finding
construction if and only if:
1. its `outcome()` is `RuleEvaluationOutcome.FAIL`, **and**
2. its `payload()` is an instance of `FindingMetadata` (Decision 3).

`PASS` and `NOT_APPLICABLE` never qualify — a Finding, by every
ordinary reading `project.md` §3.5's remediation workflow implies
("Finding -> Recommendation -> ... -> Verification"), represents an
actionable problem; a `PASS` (compliant) or `NOT_APPLICABLE` (nothing
to check) result has nothing to remediate. A `FAIL` result whose
payload does not implement `FindingMetadata` also produces no Finding
— silently, not as a construction error — mirroring `Kind-Based Rule
Applicability`'s own "no matching kind, no Result at all" pattern, one
layer up: a Rule Type that never intends its `FAIL` outcomes to become
Findings (e.g. an internal audit-only Rule Type) simply omits
`FindingMetadata` from its payload, and Finding Model treats that as
"does not apply to Finding construction," not a defect.

**Alternatives considered:**
- **Every outcome (`PASS`/`FAIL`/`NOT_APPLICABLE`) qualifies,
  including compliant ones, as an audit trail.** Rejected — `spec.md`'s
  `Aggregation Is Not Performed in This Version` requirement already
  presumes some results don't qualify ("one Finding per
  RuleEvaluationResult that qualifies"); `RuleEvaluationResult` itself
  already durably records every outcome for audit purposes (`Rule
  Evaluation Outcome Semantics`), so Finding Model re-surfacing `PASS`
  as a "Finding" would duplicate that audit function under a name
  `project.md` reserves for problems.
- **Silently error/reject construction for a `FAIL` result lacking
  `FindingMetadata`, rather than skipping it.** Rejected — this would
  force every registered Rule Type to declare `FindingMetadata` merely
  to avoid errors, even where a Rule Type deliberately has no
  Finding-Model-facing use; skipping (no Finding, no error) mirrors the
  established applicability-gating precedent instead of inventing a new
  failure mode.

### 5. Unanchored-scope concerned element identity: Finding-Model-local, `repositoryIdentifier`-derived

**Problem:** For an unanchored Rule Scope instance,
`define-finding-model/design.md` Decision 3 requires the concerned CSM
element to be "the Repository CSM element's own identity" — but that
literal identity is a function of Repository Evidence Item identity,
reachable only from `aip-csm-builder`, a module `aip-findings` has no
dependency on (Context point 4).

**Decision:** For the unanchored case, the concerned element identity
used in `LogicalFindingIdentity` and `Finding.concernedElementId()` is
computed as a Finding-Model-local, deterministic function of the
source `CsmSnapshotId.repositoryIdentifier()` alone:

```java
CsmElementId.repositorySubject(String repositoryIdentifier)
  -> new CsmElementId("csm:repository-subject:" + repositoryIdentifier)
```

This is **not** guaranteed equal to the literal Repository element
identity `aip-csm-builder`'s `RepositoryMapper` constructs — it is a
distinct, Finding-Model-owned convention, stable across CSM Snapshots
of the same repository (satisfying every actual `spec.md` scenario:
none asserts equality to a real `aip-csm-builder`-constructed
identity, only that the *same* value results for the *same* repository
across runs). Named explicitly as an accepted v1 limitation in Risks/
Trade-offs, not silently absorbed as if the literal identity were
reached.

**Alternatives considered:**
- **Give `aip-findings` a dependency on `AnalysisView`** (reversing
  `define-finding-model/design.md` Decision 11's "Specify found no
  such need" conclusion), reading the CSM Snapshot's actual `REPOSITORY`-
  kind element to obtain its real identity. Rejected for this change —
  this would reopen an upstream binding decision Design was instructed
  not to reopen; a future change could revisit Decision 11 explicitly
  if a concrete need for the literal identity (not just a stable
  synthetic stand-in) is demonstrated.
- **Give `aip-findings` a dependency on `aip-csm-builder` directly** to
  call `ElementIdentityDeriver.fromEvidenceId`. Rejected outright —
  directly violates `define-finding-model/design.md` Decision 11 and
  every established-contracts-only boundary in this system.

### 6. `Confidence` reuse: `Finding.confidence()` is always `Confidence.HIGH`

**Problem:** `define-finding-model/design.md` Decision 5 fixes
Confidence at "a single, maximal, deterministic value." What concrete
type represents it?

**Decision:** Reuse `aip-core`'s existing `Confidence` enum
(`HIGH`/`MEDIUM`/`LOW`, already defined for CSM's own inferred-
knowledge confidence) rather than inventing a new Finding-specific
type. `Finding.confidence()` always returns `Confidence.HIGH` — the
maximal existing qualitative level. `Confidence`'s own javadoc ties it
to `ProvenanceCategory.INFERRED` CSM content specifically, but reusing
its three-level vocabulary for a distinct, compatible purpose (a
fixed, always-maximal Finding-level value) does not assert Findings
are inferred CSM knowledge — it reuses the same qualitative scale for
a second, textually-documented purpose, avoiding a second enum that
would say nothing `Confidence` doesn't already say.

**Alternatives considered:**
- **A new `FindingConfidence` enum with a single `CERTAIN`/`DETERMINISTIC`
  value.** Rejected — a single-value enum communicates nothing an
  existing three-value enum's maximal member doesn't already
  communicate; would be a second type for the same concept, against
  this project's own repeated "reuse before inventing" discipline
  (`CsmScope` as Rule Scope, `Confidence` here).

### 7. `RuleEvaluationResultSource` contract shape

**Problem:** Concrete shape for `define-finding-model/design.md`
Decision 11's `aip-core` contract?

**Decision:** Mirrors `AnalysisResultSource` exactly:

```java
public interface RuleEvaluationResultSource {
  Optional<RuleEvaluationResult> read(RuleEvaluationResultId id);
  Set<RuleEvaluationResult> list(String ruleIdentifier, CsmSnapshotId snapshotId);
}
```

**Alternatives considered:** none beyond the interface `AnalysisResultSource`
already established as this project's own contract shape for a
read-only, producer-agnostic upstream-artifact source — reusing it
verbatim is the entire point of the precedent.

### 8. `FindingStore`/`FindingValidator`/`FindingPublisher`: `aip-findings`, mirroring the established gate

**Problem:** Module placement and shape for the validate-before-publish
gate `define-finding-model/design.md` Decision 9 requires?

**Decision:** `FindingStore` (interface only), `FindingValidator`, and
`FindingPublisher` all live in `aip-findings`, mirroring
`RuleEvaluationResultStore`/`RuleEvaluationResultValidator`/
`RuleEvaluationResultPublisher`'s own placement in `aip-rules` exactly
(never promoted to `aip-core` — only the *artifact type* `Finding` is
promoted, never its store, per this project's established store-
placement precedent). `FindingValidator` checks:
- every referenced `RuleEvaluationResultId` exists and is resolvable
  via `RuleEvaluationResultSource.read(...)` (trusting, not
  re-verifying, `RuleEvaluationResultValidator`'s own already-completed
  work on that Result's own consumed `AnalysisResult`s/CSM content —
  `define-finding-model/design.md` Decision 9's own trust-boundary
  reasoning);
- `EvaluationIdentity` and `LogicalFindingIdentity` are each
  independently recomputable from, and equal to, the Finding's declared
  referenced set / `(ruleIdentifier, concernedElementId)` pair;
- the referenced `RuleEvaluationResultId` set is non-empty.

**Alternatives considered:** none beyond the already-established
validate-then-write gate shape; no genuine fork here.

### 9. Module scaffolding: `aip-findings`, reusing `check-no-module-reference.sh`

**Problem:** Dependency-graph enforcement mechanics?

**Decision:** `aip-findings` is a new Maven module, sibling of
`aip-csm-builder`/`aip-analysis`/`aip-rules`, depending on `aip-core`
only. Its POM wires the same `check-module-dependencies.sh` and
`check-fixture-package-scope.sh` guards every sibling module already
has, plus **three** executions of the generic
`scripts/check-no-module-reference.sh` built during
`implement-rule-framework` specifically so a future `implement-*`
change could reuse it directly:
- `(aip-findings, 'aip\.rules\.')`
- `(aip-findings, 'aip\.analysis\.')`
- `(aip-findings, 'aip\.csmbuilder\.')`

This is the first payoff of that earlier investment — no new shell
script is written for this change's own no-adapter guard.

**Alternatives considered:** none — the generic script's entire purpose
was avoiding a fork here.

### 10. Fixture layer

**Problem:** What does the test-only fixture layer need to provide?

**Decision:** `aip-findings/src/test/java/aip/findings/test/fixtures/`
provides:
- `InMemoryRuleEvaluationResultSource` — a fixture
  `RuleEvaluationResultSource`, mirroring `InMemoryAnalysisResultSource`
  from `aip-rules`.
- `InMemoryFindingStore` — a fixture `FindingStore`, mirroring
  `InMemoryRuleEvaluationResultStore`.
- A `RuleEvaluationResultBuilder`-style helper constructing fixture
  `RuleEvaluationResult`s (anchored/unanchored Rule Scope instances,
  all three outcomes, `FindingMetadata`-conforming and non-conforming
  payloads) — since `aip-findings` has no dependency on `aip-rules`'s
  own `RuleType`/`Rule` machinery to produce these organically, unlike
  `aip-rules`'s own fixture layer, which could at least construct real
  CSM content via a `CsmSnapshotSourceBuilder`. `aip-findings`
  constructs `RuleEvaluationResult`s directly via
  `RuleEvaluationResult.of(...)`, which is sufficient — Finding Model
  never reads CSM content itself (`define-finding-model/design.md`
  Decision 8, no Scope concept), so no CSM-element-graph fixture
  builder is needed here at all, only enough `CsmElementId`/
  `CsmSnapshotId`/`CsmScopeInstance` values to construct representative
  `RuleEvaluationResult`s.
- `StubFindingMetadata` — a simple record implementing
  `FindingMetadata`, for use across test classes.

**Alternatives considered:** none — this directly follows from
Finding Model's own no-CSM-content-reading nature (Decision 8 in the
upstream design), simpler than every prior fixture layer for exactly
that reason.

## Risks / Trade-offs

- [Unanchored-scope concerned element identity (Decision 5) is a
  Finding-Model-local synthetic value, not the literal
  `aip-csm-builder`-constructed Repository element identity] → Named
  explicitly, not silently absorbed. Every `spec.md` scenario is
  satisfied (stability across snapshots of the same repository); a
  future need for the literal identity would require revisiting
  `define-finding-model/design.md` Decision 11 (giving `aip-findings`
  an `AnalysisView` dependency), a deliberate future change, not a
  silent gap in this one.
- [`FindingMetadata` (Decision 3) is a new `aip-core` contract a Rule
  Type author must remember to implement on its `FAIL`-outcome
  payloads for those outcomes to ever become Findings] → Accepted;
  documented on `FindingMetadata`'s own javadoc and exercised by the
  Extension Mechanism test (a second, structurally distinct example
  Rule Type's payload implementing it). No enforcement mechanism
  forces a Rule Type to implement it — by design, since some Rule
  Types may legitimately never intend to produce Findings (Decision
  4's own reasoning).
- [Outcome qualification (Decision 4) is an implementation-level
  resolution of a gap `define-finding-model`'s own artifacts left open,
  not a literal restatement of an explicit upstream requirement] →
  Reported transparently in `proposal.md`'s Binding Decisions and here;
  consistent with every prior `implement-*` change's practice of
  surfacing, not silently resolving, a gap found during implementation.
- [`Confidence` reuse (Decision 6) repurposes a CSM-scoped enum for a
  Finding-level concept whose own javadoc names a narrower original
  purpose] → Accepted low-risk reuse; the enum's three ordered levels
  are a general-purpose qualitative vocabulary this project already
  has, and Finding Model's usage is textually distinct and documented,
  not silently conflated with CSM inferred-knowledge Confidence.
