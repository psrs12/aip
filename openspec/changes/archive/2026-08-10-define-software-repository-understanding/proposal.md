## Why

The Canonical Software Model (CSM) specification already requires a
"defined Repository Evidence Model" as its sole upstream source
(`Repository Evidence to CSM Transformation`, `CSM Relationship to the
Repository Evidence Model`), but no such model exists yet — the CSM
cannot be built for any real repository until it does. Per
`openspec/project.md` §11, Software Repository Understanding is the
first capability in AIP's evolution order, preceding the CSM itself.
This change defines that capability: what AIP discovers about a
repository, how it discovers it, and the shape of the evidence it
produces — without performing any of the architectural interpretation
that the CSM specification reserves for itself.

## What Changes

- Define the **Repository Evidence Model**: the shape of raw,
  language/tool-specific facts Repository Understanding produces,
  structurally separate from CSM knowledge and never carrying CSM
  provenance or confidence.
- Define **repository, project, and module discovery**: build-file-driven
  Project detection (with an explicit override for repositories that
  don't map cleanly to a recognized build system), and the containment
  facts (Repository/Project/Module/Package/File) needed to seed the
  CSM's containment hierarchy.
- Define **source analysis**: per-language analyzers producing Type-level
  evidence at minimum, with Method-level evidence where an analyzer
  supports it, consistent with the CSM's own Type-minimum/Method-optional
  capability model.
- Define **build-system analysis**: parsing build and dependency
  manifests as evidence.
- Define **dependency discovery**: import/build-graph edges, qualified by
  dependency kind (compile-time/runtime/test-only) where discoverable.
- Define **API relationship discovery where deterministic**: mechanically
  discoverable contract surfaces (e.g. schema/interface definitions,
  explicit endpoint declarations) recorded as evidence — never a
  heuristic guess at what is "the public API."
- Define **configuration discovery**: the fact that a module reads a
  configuration key or references a secret, never the value.
- Define **repository change detection and incremental discovery**: a
  composite canonical-name/path evidence-identity scheme, with content
  hashing used only as an internal change-detection optimization, so
  repeated analysis of a large repository can be scoped to what changed.
- Define **partial-analysis reporting**: an explicit complete/partial/failed
  outcome per evidence item, with a failure-reason taxonomy, so
  discovery gaps are visible rather than silently dropped or disguised.
- Define **evidence identity and traceability**: every evidence item
  addressable by a stable identity that survives unrelated re-analysis
  and, where mechanically verifiable, simple renames.

This change does not implement any of the above in code. It does not
define the CSM Builder (the evidence→CSM transformation stage) — that
stage's behavioral contract already exists as a CSM-side requirement and
its implementation is future work for the Canonical Software Model
capability, not this one.

## Capabilities

### New Capabilities

- `software-repository-understanding`: The discovery capability that
  observes a software repository and produces the Repository Evidence
  Model — repository/project/module/package/file structure, source and
  build-system facts, dependency and deterministic API-contract facts,
  configuration references, change detection, partial-analysis status,
  and evidence identity/traceability — as the sole upstream input to the
  (separately specified) CSM Builder.

### Modified Capabilities

- None. The `canonical-software-model` specification's
  `Repository Evidence to CSM Transformation` and
  `CSM Relationship to the Repository Evidence Model` requirements
  already anticipate this capability's existence and do not need to
  change to accommodate it.

## Out of Scope

Per the Explore-phase boundary, this change explicitly does **not**
cover:

- **CSM vocabulary classification** — mapping evidence onto CSM entity
  kinds/relationship types is the CSM Builder's job.
- **CSM provenance categories** — Repository Understanding never assigns
  `observed`/`declared`/`inferred`; that classification happens only
  when evidence becomes CSM knowledge.
- **CSM confidence** — Repository Understanding never assigns
  HIGH/MEDIUM/LOW; its own evidence-quality signal (extraction-method
  tagging, partial-analysis status) is a distinct, non-CSM concept.
- **CSM reconciliation** — competing/conflicting knowledge and
  precedence are CSM-side concerns; Repository Understanding evidence is
  not "reconciled," only discovered.
- **Architectural intent classification** — no Architecture Component,
  Boundary, or stereotype inference; guessing that a package "is" a
  service is inferred architectural knowledge, not evidence.
- **CSM construction** — building, validating, or versioning a CSM
  snapshot is the CSM Builder's responsibility, tracked as future
  implementation work for the `canonical-software-model` capability
  (e.g., a future `implement-canonical-software-model` change).

## Impact

- **Affected specs**: introduces
  `openspec/specs/software-repository-understanding/`.
- **Affected code**: none yet — no implementation in this change.
- **Affects future work**: establishes the Repository Evidence Model
  contract that a future CSM Builder implementation must consume; the
  initial source analyzer is expected to target Java first, consistent
  with `project.md` §10's initial technology direction, though no
  analyzer implementation is part of this change.
- **Dependencies**: none introduced (no code, no libraries). Depends
  conceptually on the already-archived `canonical-software-model`
  specification as the downstream consumer this capability's evidence
  must satisfy.

## Relationship to the Existing CSM Specification

```
Repository
    ↓  (discovery)
Repository Understanding   ◀── this change
    ↓  (produces)
Repository Evidence         ◀── this change defines the shape
    ↓  (consumed by, not built here)
[future] CSM Builder        ◀── neither this change nor the archived
    ↓  (transformation)         CSM change; future CSM implementation work
Canonical Software Model    ◀── already specified (archived)
```

The archived CSM specification already commits to two requirements that
this change's Repository Evidence Model must satisfy as a consumer
contract:

- `Repository Evidence to CSM Transformation` — requires the CSM to be
  "constructed exclusively from a defined Repository Evidence Model,"
  with every derived CSM element retaining "a traceable reference back
  to the evidence and source location that produced it."
- `CSM Relationship to the Repository Evidence Model` — requires the CSM
  to treat the Repository Evidence Model as its "sole upstream source of
  evidence," and to remain valid even when no policies or runtime data
  exist.

This change satisfies the *producer* side of that contract (defining
what evidence looks like and how it is discovered); it does not
implement the *consumer* side (the CSM Builder). That gap is deliberate:
the CSM's own design already scoped itself to defining only "the shape
of the interface the CSM side depends on," explicitly leaving the
Repository Evidence Model's design — and the analyzer capability that
produces it — to this separate change.

## Unresolved Questions for Design

Carried forward from `explore.md`, none of which change this proposal's
scope or boundary:

- Exact, exhaustive failure-reason taxonomy and extraction-method
  taxonomy (illustrative categories only at this stage).
- Exact mechanical rename-detection signal to rely on for evidence
  identity continuity.
- Exact representation of the "unmanaged" pseudo-project surface for
  files with no owning build file and no override.
- Sequencing question (not a design detail, but worth surfacing before
  Design begins): the CSM Builder remains unassigned to any change.
  Design for this capability should not assume the CSM Builder exists
  or will be specified concurrently — the Repository Evidence Model
  must stand on its own as a valid, independently useful artifact.
