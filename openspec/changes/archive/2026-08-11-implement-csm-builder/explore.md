# Explore: Implement CSM Builder

Status: preserved exploration notes carried into this change. Not a
tracked schema artifact — retained for traceability from
proposal/design decisions back to the reasoning that produced them,
following the same convention used by
`archive/2026-08-10-define-software-repository-understanding/explore.md`.

## Problem Statement

`csm-builder` (archived, `openspec/specs/csm-builder/spec.md`) defines
*what* the CSM Builder capability must do. Nothing has been
implemented yet — this repository contains no code at all. This
Explore covers the first-order question that shapes everything else
about the implementation: what does CSM Builder actually depend on at
runtime, given that its upstream producer (Repository Understanding)
has been specified but never built?

## Framing

```
Repository (real code)
    │
    ▼
Repository Understanding   ◀── SPEC ONLY (archived). No implementation
    │                          exists anywhere in this repository.
    ▼
Repository Evidence
    │
    ▼
CSM Builder                 ◀── THIS CHANGE
    │
    ▼
CSM
```

CSM Builder's entire input contract — the Repository Evidence Model —
has never been produced by real code. Left unaddressed, this would
either silently block `implement-csm-builder` on an unstarted,
unrelated change, or silently tempt the implementation into depending
on RU internals that don't exist. Neither is acceptable, so this was
raised and resolved explicitly before proceeding.

## Resolved Decision: Sequencing — Option C (Hybrid)

**Status: stakeholder-confirmed, binding on this change.**

Three options were framed during Explore:
- **(A)** Build entirely against hand-authored fixtures, fully decoupled
  from any real RU implementation.
- **(B)** Block `implement-csm-builder` on a prerequisite
  `implement-software-repository-understanding` change.
- **(C)** Hybrid — build against contract-faithful fixtures now, with a
  named, explicit future integration point once a real RU
  implementation exists.

**Decision: (C).** `implement-csm-builder` proceeds independently of a
Repository Understanding implementation, using Repository Evidence
fixtures that are faithful to the archived RU contract, with the gap
this creates treated as a tracked, explicit sequencing/integration
item rather than a silent one.

### Binding implementation rules

1. CSM Builder SHALL consume the already-defined Repository Evidence
   Model contract from the archived `software-repository-understanding`
   specification — that specification is the sole source of truth for
   evidence shape, identity, kinds, and change/lifecycle vocabulary.
2. Since Repository Understanding has not yet been implemented, the
   initial CSM Builder implementation and its tests use
   **contract-faithful Repository Evidence fixtures** in place of a
   real producer.
3. Fixtures MUST represent only evidence defined by the archived RU
   specification. No new evidence model, field, semantic, or RU
   behavior is invented to make a fixture convenient.
4. Repository Understanding is NOT implemented or embedded inside CSM
   Builder, in whole or in part.
5. CSM Builder SHALL NOT depend on `aip-analyzer` or on any future RU
   implementation module. Its only upstream dependency is the
   Repository Evidence contract itself.
6. The absence of a real RU implementation is an explicit
   **sequencing/integration gap**, not a defect in CSM Builder or in
   this change.
7. A future `implement-software-repository-understanding` change is
   expected to produce the same Repository Evidence contract and
   replace the fixtures as CSM Builder's real upstream producer,
   without CSM Builder itself needing to change to accommodate that
   swap (the contract is the seam).
8. Neither `openspec/specs/canonical-software-model/spec.md` nor
   `openspec/specs/software-repository-understanding/spec.md` is
   modified to accommodate this decision.
9. Test suites explicitly distinguish two kinds of end-to-end coverage,
   named and organized so neither is mistaken for the other:
   - **Fixture-based transformation tests** — exercise
     `Repository Evidence (fixture) → CSM Builder → CSM` and are what
     this change delivers.
   - **Real-pipeline integration test** — exercises the full
     `Repository → Repository Understanding → Repository Evidence →
     CSM Builder → CSM` chain, and cannot exist until a real RU
     implementation exists. This change does not deliver it, but
     reserves its place (e.g. a skipped/pending test or a clearly
     labeled future task) so it isn't forgotten.
10. This decision, and its future integration point, is recorded here
    (Explore) and carried into `design.md` when Design is reached —
    this file is the traceable record of *why*, `design.md` will
    record *how* the fixture layer and the future swap point are
    structured.

### Resulting dependency shape

```
        CSM Builder
             │
             │  depends on (compile-time + runtime)
             ▼
   Repository Evidence contract        ◀── from archived
   (data shapes, identity scheme,          software-repository-understanding
    change-status/lifecycle vocabulary)    spec.md — not code

             ▲
             │  fixtures implement/populate this contract
             │  for tests only, not a runtime dependency
             │
   Repository Evidence fixtures
   (test-only, contract-faithful,
    hand-authored)


        CSM Builder  ──X──▶  aip-analyzer            (explicitly refused)
        CSM Builder  ──X──▶  RU implementation module (does not exist; explicitly refused)
```

The architectural dependency is `CSM Builder → Repository Evidence
contract`, never `CSM Builder → Repository Understanding
implementation`. Fixtures satisfy the contract for now; they are a
test-time stand-in, not a production dependency CSM Builder is coupled
to.

## Other Threads Raised, Carried Forward to Design (Not Resolved Here)

These surfaced during Explore but don't need resolution before
Proposal — noted so they aren't lost:

- **Module placement.** CLAUDE.md's module chain
  (`aip-core → aip-analyzer → aip-rules → aip-ai → aip-cli/aip-server`)
  doesn't name "CSM Builder" or "Repository Evidence Model" explicitly.
  Candidate framing: the Repository Evidence Model *schema* (not any
  concrete language analyzer) and the CSM *schema* are both
  language-independent domain models, and could plausibly both live in
  `aip-core` (or a thin sibling module), with `aip-analyzer` reduced to
  "the concrete mechanics that populate the Evidence schema." This
  placement question directly interacts with Rule 5 above (no
  `aip-analyzer` dependency) and should be settled in Design.
- **Deferred technology choices**, now due for a concrete Design-phase
  decision (not decided in Explore, per `design.md`'s own prior
  deferral):
  - Snapshot persistence mechanism (in-memory only vs. file-backed vs.
    embedded DB) — leaning toward *some* durable form, since
    "prior snapshots remain retrievable" is untestable with in-memory
    only, but not decided here.
  - Concurrency/parallelism mechanism for the Mapping Orchestrator.
  - Concrete format/location for the native-scope-string →
    CSM-dependency-kind mapping table (Requirement: Dependency Kind
    Classification).

## Recommended Next Step

The one question that could have reshaped this change's whole scope —
sequencing against Repository Understanding — is now resolved and
binding. The remaining threads (module placement, deferred tech
choices) are ordinary Design-phase decisions, not scope-changing
Explore questions. Ready to move to Proposal.
