## Context

See `proposal.md` for motivation and `explore.md` for the framing this
design treats as confirmed and binding: the Analysis Framework → Rule
Framework → Finding Model split refines `canonical-software-model`'s
single "Analysis → Findings" pipeline transition into three
architectural layers, without modifying that archived specification;
`aip-analyzer` and `aip-analysis` are sibling modules, and `aip-rules`
consumes Analysis Framework's outputs, never `aip-analyzer`'s. This
document resolves the seven open architectural questions Explore
carried forward.

Terminology this design introduces and uses consistently:
- **Rule Type**: AIP-authored, versioned code implementing a fixed
  evaluation contract — the Rule Framework equivalent of an Analyzer.
  Organizations do not author Rule Types.
- **Rule**: a declaratively configured instance of a Rule Type,
  parameterized against CSM vocabulary (entity kinds, relationship
  types, element identities, native-attribute predicates) — this is
  the artifact `project.md` §3.7 means by "policy as code," the thing
  an organization authors and version-controls.
- **Rule Evaluation Result**: the durable output of evaluating one Rule
  against one Rule Scope instance.

Binding external facts this design does not reopen:
- `aip-core` already hosts `aip.core.csm`, including `AnalysisView` and
  `CsmSnapshotSource` (added by `define-analysis-framework`).
- `aip-analysis` already specifies the Analyzer contract, Analysis
  Scope (a declared CSM entity/relationship-kind set plus optional
  containment anchor plus optional native-attribute predicate),
  `AnalysisResult` (deterministic identity, traceability, opaque
  Analyzer-defined payload), and `AnalysisResultStore` (an abstraction;
  no concrete implementation exists yet). None of this is modified by
  this change.
- `canonical-software-model` already specifies that evaluating
  Architectural Boundary compliance is "a function of the Policy/Rule
  Model," giving Rule Framework one concrete, already-specified example
  of a built-in Rule Type's job.

## Goals / Non-Goals

**Goals:**
- Select a Policy-as-Code representation and justify it against
  extensibility, determinism, validation, versioning, and
  architectural fit.
- Define how a Rule composes AnalysisResults, AnalysisView content, and
  raw facts, and how that composition is declared and traced.
- Define Rule Scope/applicability without inventing a second,
  incompatible scope mechanism.
- Define Rule Evaluation Result identity, traceability, and durability.
- Decide evaluation-outcome recording (violations only / all outcomes /
  layered), with reasoning.
- Decide whether Rule Evaluation Results need a validation/publishing
  gate.
- Determine `aip-rules`'s dependency shape and contract ownership,
  evaluated explicitly rather than assumed.

**Non-Goals:**
- Writing `openspec/specs/rule-framework/spec.md` (Specify phase).
- Designing Finding Model — this design goes only as far as the Rule
  Evaluation Result shape a future Finding Model reads, per the
  Explore-confirmed pipeline (`Rule Framework → Findings`). Severity,
  confidence scoring, recommendation text, and remediation availability
  are Finding Model's concerns, not defined here.
- Choosing Rule Evaluation Result persistence *technology* — deferred
  the same way Analysis Result and Snapshot persistence technology were
  each deferred at their own Design stage.
- Reopening any of `define-analysis-framework`'s seven binding
  decisions, or its Decision 7's Analysis Scope vocabulary itself
  (reused here, not redefined).
- Editing `CLAUDE.md`. Its module-chain text is stale relative to the
  `aip-analyzer`/`aip-analysis` sibling relationship `define-analysis-framework`
  already established, but this design's own dependency diagram is
  authoritative for this change's purposes — fixing CLAUDE.md's prose
  is an unrelated, separate housekeeping change, not bundled here.
- Implementation code of any kind.

## Decisions

### 1. Policy-as-Code representation: parameterized Rule Types, not a DSL or adopted engine

**Problem:** `project.md` §3.7 wants organizations to author policies
("Handlers must not access repositories directly," etc.) in a
machine-readable, version-controlled, testable, independently
maintainable form. Analyzer's own shape (AIP-authored code, no
end-user authoring surface) doesn't satisfy this alone — something has
to let a non-AIP-maintainer define a policy without writing and
compiling Java against this codebase.

**Decision:** A two-level model, mirroring Analyzer's own code/registry
split but adding one more level for end-user authoring:
- **Rule Type** — AIP-authored, versioned code (identical shape to
  Analyzer: stable identifier, monotonic version, declares what it
  needs). A Rule Type implements one *kind* of check — e.g. "forbidden
  dependency between two CSM regions," "required boundary compliance,"
  "no External System reached without an approved integration
  boundary."
- **Rule** — a declarative, version-controlled configuration binding a
  Rule Type to specific CSM vocabulary: entity kinds, relationship
  types, element identities, and/or native-attribute predicates (the
  exact terms `Architecture Constraints Excluded from the CSM` already
  names as what a policy is expressed in terms of). This is what an
  organization authors — parameters, not code or a general-purpose
  expression grammar.

Determinism, validation, and versioning all follow directly: a Rule
Type's evaluation logic is ordinary versioned code (as deterministic
and testable as an Analyzer); a Rule's own "version" is just its
configuration's own version-control history: no new versioning
concept, no new determinism obligation beyond what Rule Types already
carry.

**Alternatives considered:**
- **A purpose-built DSL/predicate language** over CSM vocabulary.
  Rejected for v1 — real complexity (grammar design, a parser,
  versioning a whole language, and ensuring arbitrary expressions in
  it stay deterministic) disproportionate to a first cut. Not rejected
  forever: if parameterized Rule Types prove too inflexible once real
  policies are written against them, a DSL is a natural, additive
  evolution — a Rule Type could itself be defined *in* a future DSL
  without changing the Rule/Rule-Type split this decision establishes.
- **Adopting an external rule/policy-engine format** (e.g.
  Rego/OPA-shaped). Rejected — `CLAUDE.md`'s Dependency Discipline
  ("prefer the smallest appropriate dependency... do not add a
  dependency simply because it is convenient") argues against pulling
  in a third-party expression engine and its own runtime/sandboxing
  concerns before a simpler, dependency-free approach has been shown
  insufficient. It would also tie policy authoring UX to a foreign
  ecosystem's conventions rather than CSM's own vocabulary-first
  design.
- **Rules as Java code only, no end-user configuration surface at
  all.** Rejected — this is Analyzer's own shape, and reusing it
  verbatim here would abandon `project.md` §3.7's explicit intent
  rather than making any genuine first attempt at it.

### 2. Rule-to-Analysis composition: declared inputs, no dynamic dependency graph

**Problem:** A realistic policy ("no Module may depend on an External
System without an approved integration boundary") needs more than one
fact — here, both a dependency fact and a boundary-declaration fact.
How does a Rule Type declare and consume more than one upstream input
without Rule Framework needing to resolve an ad hoc dependency graph at
evaluation time?

**Decision:** A Rule Type declares its inputs explicitly, in two parts:
- A set of required Analyzer identifiers (optionally with a minimum
  version) whose already-published `AnalysisResult`s it reads, via the
  `AnalysisResultSource` contract (Decision 7).
- A Scope declaration (Decision 3) for direct `AnalysisView` reads —
  identical in shape to Analysis Scope, for facts that don't need a
  dedicated Analyzer (e.g. "does this Subject carry a declared boundary
  relationship" is already directly queryable from `AnalysisView`
  without any Analyzer computing it first).

Both are declared, static facts about a Rule Type, not a dynamic
graph Rule Framework computes at runtime. A Rule Type never depends on
another Rule Type's output — this preserves Analysis Framework's own
independence discipline (its Decision 2) one layer up: Rules depend
only on the finished, already-published outputs of the *prior* layer
(Analysis), never on each other, and never on an in-progress
evaluation.

**Alternatives considered:**
- **A Rule Type may consume another Rule Type's Rule Evaluation
  Result**, allowing compound policies built from simpler ones.
  Rejected for v1 — this reintroduces exactly the topological-ordering,
  cross-artifact-versioning complexity `define-analysis-framework`'s
  own Decision 2 rejected for Analyzers, for the same reason: a real
  cost (ordering, cycle detection, re-evaluation cascades) for a
  compositional convenience that a sufficiently expressive set of
  built-in Rule Types can likely cover without it. Not foreclosed
  permanently, deliberately deferred.
- **Rule Types query CSM/Analysis content freely at evaluation time**
  (no declared-inputs requirement, direct ad hoc reads). Rejected —
  undeclared inputs make Rule Evaluation Result traceability (Decision
  4) and scope-based applicability (Decision 3) unenforceable; a Rule
  Type's own honesty about what it reads is what a defense-in-depth
  validation gate (Decision 6) has something concrete to check against.

### 3. Rule Scope: identical shape to Analysis Scope, not a second mechanism

**Problem:** Does Rule Framework need its own scope-declaration
concept, and if so, how does it avoid becoming a second, subtly
incompatible mechanism alongside Analyzer's already-specified Analysis
Scope?

**Decision:** Rule Scope reuses Analysis Scope's exact, already-approved
shape verbatim — a declared, non-empty set of CSM entity kinds and/or
relationship types, an optional containment-level anchor, and an
optional native-attribute predicate refinement. Rather than each
consumer (Analyzer, Rule) redeclaring this shape independently and
risking silent drift between them, `aip-core` hosts one general Scope
declaration type both Analysis Scope and Rule Scope are defined in
terms of. This does not modify `define-analysis-framework`'s Decision
7 — Analysis Scope's own vocabulary is unchanged — it only gives that
already-specified shape a shared, reusable home now that a second
consumer needs the identical thing, the same "general capability
belongs in `aip-core`" reasoning `define-analysis-framework`'s own
Decision 3 already applied to `AnalysisView`.

Scope-instance enumeration (unanchored → once per repository; anchored
→ once per matching contained element) and kind-based/native-attribute
applicability (Analysis Scope's own established behavior) both carry
over to Rule Scope unchanged.

**Alternatives considered:**
- **A parallel, Rule-specific scope concept**, independently designed.
  Rejected — no functional reason for Rule Scope to differ from
  Analysis Scope's already-solved shape, and a parallel concept risks
  exactly the "second, incompatible mechanism" this question warns
  against.
- **Rule Scope implied entirely by a Rule's own parameters, with no
  separate declaration at all.** Rejected — without an explicit
  declaration, applicability (does this Rule even apply to this
  Analysis View) and incremental re-evaluation eligibility (mirroring
  Analysis Framework's own architecturally-supported-but-not-sophisticated
  incremental posture) have nothing structural to key off; every other
  layer in this system (Mapper, Analyzer) declares its scope
  explicitly rather than leaving it implicit, and Rule should not be
  the first exception.

### 4. Rule Evaluation Result identity, traceability, and durability

**Problem:** Every prior layer (CSM Snapshot, `AnalysisResult`) uses a
deterministic-identity-plus-traceability-plus-durable-store shape. Does
Rule Evaluation Result follow it unchanged, or does composing multiple
`AnalysisResult`s (Decision 2) require extending it?

**Decision:** Rule Evaluation Result's identity is a deterministic
function of: the Rule's identifier and version, the source CSM
Snapshot identity, the Rule Scope instance evaluated, **and the full
set of consumed `AnalysisResult` identities** — extending the
established (producer, version, source, scope) shape with one more
term, because Rule Framework is the first layer whose input is
multi-artifact rather than a single view derived from a single source.
An Analyzer only ever had one `AnalysisView`, itself deterministically
derived from one `CsmSnapshotSource`, so "source identity" alone was
sufficient; a Rule can depend on several independently-versioned
`AnalysisResult`s, so identity must fold in all of them to remain a
faithful function of everything the outcome actually depends on. Never
random or otherwise non-reproducible, consistent with every identity
scheme in this system so far.

Traceability records the same information the identity is computed
from — Rule identifier and version, source CSM Snapshot identity, Rule
Scope instance, and the consumed `AnalysisResult` identity set —
completing the explainability chain `project.md` §3.3/§6 describe:
(future) Finding → Rule Evaluation Result → `AnalysisResult`s →
`AnalysisView` Subjects → CSM elements → originating Evidence.

Durability follows `AnalysisResult`'s own precedent directly: a
`RuleEvaluationResultStore` abstraction (interface only; concrete
implementation deferred, same as `AnalysisResultStore` and
`SnapshotStore` before it).

**Alternatives considered:**
- **Identity omits the consumed `AnalysisResult` set**, relying on
  source CSM Snapshot identity alone (mirroring Analyzer exactly, no
  extension). Rejected — this would under-specify identity for a
  multi-input Rule: two evaluations against the same CSM Snapshot but
  different `AnalysisResult` inputs (e.g. after an Analyzer version
  bump that didn't change the CSM Snapshot itself) would collide on
  identity despite depending on genuinely different inputs, violating
  the "identity is a deterministic function of everything it depends
  on" discipline every prior layer holds to.

### 5. Evaluation outcome semantics: every outcome recorded, retention is a store concern

**Problem:** Should Rule Framework record only violations, every
evaluation outcome (pass/fail/not-applicable), or something layered
between the two? Auditability and reproducibility argue one way,
storage volume the other.

**Decision:** Layered, but decisively so: **Rule Framework's own
contract SHALL produce and offer every evaluation outcome for
publication** — a Rule Evaluation Result exists whether its outcome is
pass, fail, or not-applicable, satisfying the same non-destructive,
nothing-silently-dropped posture the CSM's own knowledge model holds to
elsewhere. What this decision does **not** mandate is that every
concrete store implementation retains every passing outcome verbatim,
forever — retention/summarization policy for low-information-value
passing outcomes is a store-implementation and operations concern,
deferred exactly the way persistence *technology* itself is deferred
(Decision 4; Non-Goals). The framework's obligation stops at "nothing
is silently skipped or discarded at evaluation time"; how long a
concrete store keeps a passing Result is not this design's decision to
make.

**Binding sub-decision: `NOT_APPLICABLE` is a produced Rule Evaluation
Result, distinct from "not evaluated at all."** Found during Specify
and made explicit here rather than left implicit: this decision's
three-way outcome vocabulary (pass/fail/not-applicable) describes what
happens once a Rule Scope instance *is* evaluated — it does not, by
itself, say what happens when a Rule Scope instance fails kind-based or
native-attribute applicability (Decision 3) in the first place. Those
are two different situations and this design treats them as
semantically distinct, not two names for the same thing:
- **Not applicable to this Scope instance** (kind-based or
  native-attribute applicability fails) — the Rule Type is never
  invoked for that instance. No Rule Evaluation Result of any outcome
  is produced. This mirrors Analyzer's own "no Mapper/Analyzer found →
  skip, not an error" pattern exactly.
- **Applicable and evaluated, but no policy outcome is determinable**
  (the Rule Scope instance passed applicability, the Rule Type *is*
  invoked, but the specific content its condition depends on is absent)
  — this produces an explicit `NOT_APPLICABLE` Rule Evaluation Result,
  offered for validation and publication like any other outcome.

Conflating these would either (a) silently drop the "this Rule Type was
invoked but couldn't reach a determination" fact, weakening
auditability the same way a violations-only design would, or (b)
produce a `NOT_APPLICABLE` Result for every Scope instance a Rule Type
was never even invoked against, defeating the point of declaring
applicability at all (Decision 3) and creating unbounded low-value
Result volume for every inapplicable instance in every repository.

**Alternatives considered:**
- **Violations only.** Rejected — silently drops the "this Rule was
  evaluated and passed" fact, which is itself sometimes
  audit-relevant (proving a policy *was* checked, not merely that it
  wasn't violated), and forecloses the "layered" middle ground without
  a corresponding storage-volume benefit this design can't already get
  by deferring retention policy instead.
- **All outcomes, with no acknowledgment of storage cost.** Rejected as
  stated — ignoring the real, named concern (large repositories, many
  Rules × many Scope instances, every incremental run) would be
  answering only half the question the Explore phase raised.

### 6. Validation and publishing: yes, mirroring the established pattern with Rule-specific checks

**Problem:** Does Rule Evaluation Result need a validator/publisher gate
like `CsmValidator`/`SnapshotPublisher` and
`AnalysisResultValidator`/`AnalysisResultPublisher`?

**Decision:** Yes. A `RuleEvaluationResultValidator` checks, before a
`RuleEvaluationResultPublisher` ever calls
`RuleEvaluationResultStore.write`:
- **Referential integrity** — every consumed `AnalysisResult` identity
  and every CSM element/Subject identity a Rule Evaluation Result
  references SHALL actually exist within the source CSM Snapshot and
  among the declared consumed `AnalysisResult`s.
- **Rule/version consistency** — the declared producing Rule and its
  version SHALL correspond to a currently registered Rule.
- **Scope containment** — the Result's content SHALL stay within its
  Rule's declared Rule Scope (Decision 3).

This is a direct, undiluted continuation of the same defense-in-depth
posture every prior layer holds to independent of whether today's
implementations are careful — not a new invention, a third application
of an already-proven pattern.

**Alternatives considered:**
- **No validation gate, trusting Rule Type implementations.** Rejected
  for the same reason `define-analysis-framework`'s own Decision 5
  rejected it: guards exist independent of implementation quality, so
  a future Rule Type author's mistake fails loudly at publish time
  rather than silently producing an untraceable or out-of-scope Rule
  Evaluation Result.

### 7. Module dependency and contract ownership: `aip-rules` depends on `aip-core` only, via a new `AnalysisResultSource` contract

**Problem:** Does `aip-rules` depend on `aip-analysis` directly to read
`AnalysisResult`s, or does the same reasoning that kept `aip-analysis`
from depending on `aip-csm-builder` directly apply here too? This is
evaluated explicitly, not assumed.

**Decision:** `aip-core` gains a second new read contract —
`AnalysisResultSource` — mirroring `CsmSnapshotSource`'s own shape:
read an `AnalysisResult` by identity, list `AnalysisResult`s by
(Analyzer identifier, source CSM Snapshot identity). `aip-rules`
depends on `aip-core` only — `AnalysisView`, the shared Scope
declaration (Decision 3), and this new `AnalysisResultSource` — and
SHALL NOT depend on `aip-analysis` directly.

The reasoning transfers cleanly, with one nuance worth naming
explicitly: `aip-analysis`'s own `AnalysisResultStore` (per its
Decision 4) is *already* an abstraction, not a concrete
implementation the way `aip-csm-builder`'s `Snapshot`/`SnapshotStore`
were before `CsmSnapshotSource` existed — so this isn't quite coupling
a consumer to a producer's concrete implementation detail the way the
original anti-pattern was. But `AnalysisResultSource`'s *shape* (a
read-only contract over persisted, producer-agnostic content) is
identical to `CsmSnapshotSource`'s, and the same multi-future-consumer
argument applies: Rule Framework is not the only future reader of
`AnalysisResult`s (Agent Framework and a future CLI/query surface are
named candidates in `define-analysis-framework`'s own Decision 3
reasoning). Placing the contract in `aip-core` now, while only one
concrete consumer exists, costs little and avoids a second
"promote to `aip-core`" migration later once a second consumer
appears — exactly the situation `CsmSnapshotSource` itself was
introduced to resolve after the fact for CSM content.

`aip-analysis`'s own `AnalysisResultStore` interface is expected to
become (or be trivially adapted into) an implementation of
`AnalysisResultSource`, the same way `aip-csm-builder`'s `Snapshot`
adapts to satisfy `CsmSnapshotSource` — a future
`implement-rule-framework`/`implement-analysis-framework` concern, not
built here.

```
  aip-core (aip.core.csm)
  - CSM domain model + Subject / EffectiveKnowledgeStatus /
    SubjectConflictMarker             (existing)
  - CsmSnapshotSource                 (existing)
  - AnalysisView                      (existing)
  - shared Scope declaration          (this change)
  - AnalysisResultSource              (this change)
        ^              ^              ^
        | depends on   | depends on   | depends on
        |              |              |
  aip-csm-builder   aip-analysis   aip-rules
  (existing;        (defined;      (new module,
   implemented)      not yet        THIS CHANGE)
                      implemented)

  aip-csm-builder, aip-analysis, and aip-rules are siblings -
  none of the three depends on either of the others
```

**Alternatives considered:**
- **`aip-rules` depends on `aip-analysis` directly**, since
  `AnalysisResultStore` is already an interface, not a concrete class.
  Rejected — while the coupling risk is genuinely smaller than the
  original `aip-csm-builder` case (named explicitly above, not
  glossed over), it still ties every future `AnalysisResult` consumer
  to `aip-analysis` as a module, and the marginal cost of hosting one
  more small, general, read-only contract in `aip-core` is low relative
  to avoiding a second forced migration once a second consumer besides
  `aip-rules` needs the identical thing.
- **Fold `aip-rules` into `aip-analysis`** (no new module). Rejected
  for the same reason CSM Builder and Analysis Framework stayed
  separate modules: conflates two capabilities the evolution order
  itself treats as distinct steps, and blurs `aip-analysis`'s own
  dependency-graph guard.

## Risks / Trade-offs

- [Rule Types (Decision 1) may prove too inflexible once real
  organizational policies are written against them, since they are
  parameterized code rather than a general expression language] →
  Mitigation: explicitly recorded as a deliberate, revisitable v1
  scope choice, not a permanent ceiling — a future DSL can define Rule
  Types themselves without changing the Rule/Rule-Type split.
- [Declared, non-compositional Rule Types (Decision 2) may require
  duplicating logic across several Rule Types that would otherwise
  share a sub-check] → Mitigation: an accepted v1 cost, mirroring
  Analyzer's own accepted recomputation cost; revisit if it becomes a
  measured problem rather than a theoretical one.
- [Extending Rule Evaluation Result identity to include a consumed-
  AnalysisResult set (Decision 4) means identity can change even when
  neither the Rule nor the CSM Snapshot changed, if only an upstream
  Analyzer's version changed] → This is intended, not a flaw (it keeps
  identity a faithful function of everything actually consumed) — but
  it does mean Rule Evaluation Result churn is coupled to Analyzer
  churn one layer up, which could surprise an implementer expecting
  Rule-level stability independent of Analysis Framework's own
  versioning cadence.
- [Deferring retention policy for passing outcomes (Decision 5) to a
  future store implementation means this design does not yet know
  whether "every outcome, forever" is operationally viable at scale] →
  Genuinely unresolved; flagged rather than guessed at, consistent
  with not inventing implementation detail merely to close the
  question.
- [Placing `AnalysisResultSource` in `aip-core` now (Decision 7), ahead
  of a second concrete consumer actually existing, is itself a bet that
  the multi-consumer future named in `define-analysis-framework`'s own
  reasoning materializes] → Mitigation: low-cost if wrong (one small,
  general interface sitting unused by a second consumer is not
  expensive to carry), and the alternative (discovering the need later
  and migrating) is exactly the friction `CsmSnapshotSource`'s own
  introduction already had to absorb once for CSM content.
