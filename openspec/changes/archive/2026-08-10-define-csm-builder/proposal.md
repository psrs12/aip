## Why

The Canonical Software Model (CSM) specification requires the CSM to be
"constructed exclusively from a defined Repository Evidence Model via a
transformation stage that maps evidence onto the CSM's closed
vocabulary" (`Repository Evidence to CSM Transformation`), but no such
transformation stage is yet specified. Both upstream inputs to that
stage are now archived and stable: `canonical-software-model` fixes the
output vocabulary, provenance model, and validation rules;
`software-repository-understanding` fixes the input evidence shape,
identity scheme, and change-detection vocabulary. Per `project.md`
§11's evolution order (Software Repository Understanding → Canonical
Software Model → Analysis Framework → ...), the transformation stage
between them — the CSM Builder — is the next capability to specify.
This change defines that capability: what it is permitted to construct,
how it maps evidence onto CSM vocabulary, and how it stays deterministic
and incremental — without redesigning either archived specification.

## What Changes

- Define the **CSM Builder** as a deterministic, evidence-driven
  transformation capability: Repository Evidence in, CSM knowledge out,
  with no heuristic or AI-based architectural interpretation anywhere
  in the transformation.
- Define the **Evidence-to-CSM mapping**: how each Repository Evidence
  kind (Repository, Project, Module, Package, source-construct/Type,
  Method, manifest-declared dependency edge, source-level reference
  edge, API contract declaration, File) maps onto the corresponding
  CSM entity kind or relationship type, using only vocabulary the CSM
  specification already defines.
- Define **provenance assignment**: CSM Builder assigns `observed`
  provenance, constructed per the CSM's already-approved
  `Structured Provenance Record` and `Provenance Classification of
  Knowledge` rules — never `declared` or `inferred`.
- Define **CSM identity derivation**: how a CSM element's stable
  identity is derived from the identity of the Repository Evidence
  Item(s) that produced it, so CSM's own `Incremental Model Updates`
  stable-identity requirement is satisfiable.
- Define **incremental re-derivation**: how CSM Builder consumes RU's
  `ADDED`/`UNCHANGED`/`MODIFIED`/`REMOVED` change-status and
  `PRESENT`/`TOMBSTONED`/`PURGED` lifecycle-state signals to scope
  CSM re-derivation to affected content, consistent with CSM's
  `Incremental Model Updates` requirement.
- Define **use of CSM's existing conflict and precedence machinery**:
  CSM Builder invokes `Same-Category Conflict Marking and Resolution`
  and `Effective Knowledge and Precedence` as already specified, rather
  than inventing new conflict-handling behavior.
- Define **traceability preservation**: every CSM element CSM Builder
  produces retains a reference back to the originating Repository
  Evidence Item(s), satisfying CSM's `Repository Evidence to CSM
  Transformation` requirement.
- Explicitly define the **capability's boundary** (see Out of Scope):
  what CSM Builder is categorically prohibited from constructing or
  performing, given that a purely deterministic, evidence-driven
  transformation cannot legitimately produce `declared` or `inferred`
  knowledge.

This change does not implement any of the above in code. It does not
resolve the six items explicitly carried forward from Explore as
Design-phase decisions (see Unresolved Questions for Design) — those
are deferred to `design.md`, not silently decided here.

## Capabilities

### New Capabilities

- `csm-builder`: The deterministic transformation capability that
  consumes the Repository Evidence Model and produces `observed` CSM
  knowledge — entity and relationship construction, provenance
  assignment, identity derivation, incremental re-derivation, and
  evidence traceability — as the sole mechanism by which CSM content is
  constructed, per the CSM specification's own
  `Repository Evidence to CSM Transformation` requirement.

### Modified Capabilities

- None. Both `canonical-software-model` and `software-repository-understanding`
  already anticipate this capability's existence (`Repository Evidence
  to CSM Transformation`, `CSM Relationship to the Repository Evidence
  Model`, `Evidence-to-CSM-Builder Interface Boundary`) and require no
  requirement-level changes to accommodate it.

## Out of Scope

Per the confirmed Shape A stakeholder decision, this change explicitly
does **not** cover:

- **Architecture Component inference or declaration** — Architecture
  Components can never carry `observed` provenance (per CSM's own
  `Architecture Component Representation` requirement); constructing
  one requires either a human declaration or heuristic/AI inference,
  neither of which CSM Builder performs.
- **Architectural Boundary inference or declaration** — same
  reasoning; Architectural Boundaries can never be `observed`.
- **Business Context construction** — Business Capability, Domain,
  Ownership, and Criticality mappings always carry `declared`
  provenance; CSM Builder has no declaring actor to attribute them to.
- **Any architectural grouping invented by CSM Builder** — e.g.
  grouping Modules or packages into a candidate component by naming
  convention or structural resemblance. This is exactly the
  "inferred architectural knowledge, not evidence" pattern
  `software-repository-understanding`'s own Explore ruled out one
  layer down; CSM Builder does not reintroduce it one layer up.
- **New CSM vocabulary** — no new entity kind, relationship type, or
  provenance/confidence category. CSM Builder maps evidence onto the
  vocabulary the CSM specification already defines, full stop.
- **Confidence assignment to observed knowledge** — CSM's own
  `Confidence for Inferred Knowledge` requirement reserves confidence
  for `inferred` elements only; CSM Builder never produces `inferred`
  elements and therefore never assigns confidence.
- **AI/LLM reasoning of any kind** — consistent with `project.md`
  §3.2's "Deterministic Analysis Before AI" principle and the same
  discipline `software-repository-understanding` already committed to.
- **Architecture compliance or policy evaluation** — CSM's own
  `Architectural Boundary Representation` requirement already states
  boundary-compliance evaluation is the Policy/Rule Model's job, not
  the CSM's; CSM Builder, further upstream, evaluates nothing.
- **Any dependency on the Policy/Rule Model or Runtime Model for
  construction** — per CSM's own `CSM Relationship to the Repository
  Evidence Model` requirement, the CSM (and therefore its construction)
  must remain valid and usable with neither present.
- **The six items carried forward from Explore as Design-phase
  decisions** (see below) — this proposal names them but does not
  resolve them.

## Impact

- **Affected specs**: introduces `openspec/specs/csm-builder/`.
- **Affected code**: none yet — no implementation in this change.
- **Affects future work**: establishes the transformation contract that
  a future `implement-csm-builder` change would build against; also
  establishes the boundary that any future `declared`- or
  `inferred`-knowledge-producing capability (e.g. a future Architecture
  Compliance Agent, or a human-declaration ingestion mechanism) must
  respect — those remain separate, later capabilities, not part of this
  one.
- **Dependencies**: none introduced (no code, no libraries). Depends
  conceptually on both already-archived specifications as fixed
  upstream/downstream contracts this capability's mapping logic must
  satisfy without modifying either.

## Relationship to the Existing Specifications

```
Repository Evidence          ◀── already specified (archived,
    ↓  (consumed by,             software-repository-understanding)
       not built here)
CSM Builder                  ◀── this change
    ↓  (produces, deterministic,
       observed-only)
Canonical Software Model     ◀── already specified (archived,
                                  canonical-software-model)
```

This change satisfies the *transformation* side of a contract both
archived specifications already committed to from their own sides:

- `canonical-software-model`'s `Repository Evidence to CSM
  Transformation` requirement already specifies the transformation
  stage's behavioral contract (map evidence onto CSM's closed
  vocabulary, attach provenance, preserve traceability) — this change
  fills that stage in, without altering the requirement itself.
- `software-repository-understanding`'s `Evidence-to-CSM-Builder
  Interface Boundary` requirement already states RU "SHALL expose the
  Repository Evidence Model as the sole interface a future CSM Builder
  consumes" — this change is that future CSM Builder, without altering
  RU's requirement either.

Neither archived specification requires modification for this change to
proceed.

## Unresolved Questions for Design

Carried forward from Explore, per stakeholder direction — none of these
change this proposal's Shape A boundary or scope, and none are to be
resolved by introducing new CSM vocabulary:

1. **Snapshot vs. live graph** — does CSM Builder produce a fresh,
   versioned CSM snapshot each run, or incrementally mutate a
   long-lived CSM graph in place?
2. **Dependency evidence reconciliation** — how manifest-declared
   dependency edges and source-level reference edges combine into
   CSM's single `dependency` relationship type.
3. **External System construction** — whether, and under what
   mechanical rule, CSM Builder represents an unresolved
   manifest-declared dependency target as a CSM External System
   boundary node.
4. **`ConfigReference` representation** — how (or whether, for this
   capability's initial scope) a `ConfigReference` Evidence Item is
   represented in CSM, given no existing CSM relationship type is an
   obvious fit.
5. **Partial-evidence handling** — how CSM Builder treats a CSM element
   built from `partial` (vs. `complete`) Repository Evidence, without
   introducing anything confidence-shaped onto `observed` knowledge.
6. **CSM Builder/mapper versioning** — whether and how a change to CSM
   Builder's own mapping logic (independent of any RU analyzer-version
   change) triggers re-derivation of previously-built CSM elements.

## Sequencing Note

This proposal does not require, and should not wait for, any other
capability to exist first — both of its upstream/downstream contracts
are already archived and stable. A future `implement-csm-builder`
change (implementation) and any future `declared`/`inferred`-knowledge
capability (e.g. an Architecture Compliance Agent) remain unassigned to
any change and are explicitly out of this proposal's scope.
