# Explore: Software Repository Understanding

Status: preserved exploration notes carried into this change. Not a
tracked schema artifact — retained for traceability from
proposal/design decisions back to the reasoning that produced them.

## Problem Statement

What must AIP discover and record about a software repository so that
the Canonical Software Model (CSM) can be built from it — without
Repository Understanding itself performing any architectural
interpretation that belongs to the CSM?

Grounded in `openspec/project.md` and the approved
`openspec/specs/canonical-software-model/spec.md`.

## Framing the Interface

```
┌──────────────┐        ┌────────────────────────┐        ┌───────────────────────┐
│  Repository   │──────▶│  Repository Evidence    │──────▶│  Canonical Software     │
│  (source of    │discover│  (raw, language-        │ CSM   │  Model                  │
│   truth)       │       │   specific observations)│Builder│  (language-neutral      │
│               │       │                          │(§5,   │   knowledge)            │
└──────────────┘        └────────────────────────┘ CSM spec)└───────────────────────┘
```

This change owns the left two boxes and the arrow between them. It must
produce evidence shaped so the already-approved
`Repository Evidence to CSM Transformation` requirement (CSM spec) can
consume it — it does not own the CSM Builder (the evidence→CSM
transformation stage) itself; that ownership is an open question (see
below).

Two requirements from the approved CSM spec form the load-bearing
contract this change must satisfy:

- **`Repository Evidence to CSM Transformation`**: "constructed
  exclusively from a defined Repository Evidence Model... every CSM
  element derived from evidence SHALL retain a traceable reference back
  to the evidence and source location..."
- **`CSM Relationship to the Repository Evidence Model`**: "depend on
  the Repository Evidence Model as its sole upstream source of
  evidence... valid and usable even when no policies or runtime data
  exist."

Both presuppose a "defined Repository Evidence Model" that does not yet
exist — closing that gap is this change's job.

## Repository-Understanding Boundary

The CSM spec already drew several lines that constrain this change from
the other side — Repository Understanding must produce evidence *for*
these, not duplicate them:

| CSM already claims | So Repository Understanding must... |
|---|---|
| CSM holds only interpreted "Knowledge," never raw Evidence (`Evidence and Knowledge Distinction`) | ...own the raw Evidence itself — source text, ASTs, build files, config entries, IaC fragments — as content, not just a pointer |
| Source File is explicitly excluded from the CSM vocabulary, "remains part of the Repository Evidence Model" (`Source File Representation`) | ...treat File as a first-class citizen of *its* model, even though CSM won't have a File node |
| Method-level CSM elements are optional per analyzer (`Method-Level Representation Capability`) | ...let evidence be honest about *how deep* a given analyzer went — evidence must state whether it went Type-level only or Type+Method, per language/module |
| CSM tracks provenance as `observed`/`declared`/`inferred` (`Provenance Classification of Knowledge`) | ...produce exactly the raw material for `observed` — Repository Understanding does not itself declare or infer anything architectural |
| CSM vocabulary is closed-but-extensible (`CSM Conceptual Vocabulary`) with a "native evidence attribute bag" escape hatch | ...preserve language-native detail that doesn't map cleanly, so the CSM Builder has something to put in that bag |

Working boundary statement: **Repository Understanding discovers and
records facts about a repository's structure and content, in whatever
shape those facts naturally take per language/tool — it does not
interpret, normalize, or classify anything into CSM vocabulary.** That
normalization is the CSM Builder's job — a stage the CSM design
describes (design §5) but which does not yet belong to any scoped
capability.

## Evidence Model — Working Inventory

### Supported repository artifacts

At minimum, mirroring what the CSM spec already anticipates being fed
into it: source files (by language), build/dependency manifests
(`pom.xml`, `package.json`, `go.mod`, ...), configuration files,
infrastructure-as-code fragments, and VCS metadata (for evidence-level
signals only — CSM explicitly excludes business/org detail, so VCS
history may inform evidence but must not be promoted to CSM ownership
data directly). The CSM's original containment hierarchy
(Repository → Project → Module → Package → File) is the right starting
inventory; open question is which levels Repository Understanding
discovers directly vs. leaves for the CSM Builder to derive from
lower-level facts.

### Discovery approach

```
OPTION A: Per-language plugin analyzers        OPTION B: Generic file/text
     ┌─────────┐   ┌─────────┐                  scanning + light heuristics
     │  Java    │   │   Go     │                       ┌──────────────┐
     │ Analyzer │   │ Analyzer │   ...                  │ Generic walker │
     └────┬────┘   └────┬────┘                          │ (any language) │
          └──────┬──────┘                                └──────────────┘
                 ▼
          Repository Evidence
```

Option A matches the CSM design's language-independence mechanism
(design §19: "new language requires a new analyzer... never a change to
the CSM's own structural definition") — Repository Understanding is
presumably where those per-language analyzers live. Option B alone
would not produce Type/Method-level fidelity. Expected shape: a hybrid
— generic repository/build-file discovery (language-agnostic) plus
pluggable per-language source analyzers, with Java as the first
analyzer per `project.md` §10.

### Incremental analysis

The CSM's `Incremental Model Updates` requirement already demands
"change-scoping input from the Repository Evidence Model (which
evidence changed since the last build)" (CSM design §17) — this pushes
a real, non-optional requirement onto this change: Repository
Understanding must be able to report *what changed* since a prior
discovery pass (via VCS diff, mtime, or content hash), not just produce
a fresh full evidence set every time. This is a hard dependency, not a
nice-to-have — the CSM's incremental story is unimplementable without
it.

### Multi-language repositories

A single repository/monorepo may mix languages per module. Evidence
must be attributable per-module/per-analyzer so the CSM Builder can
apply `Method-Level Representation Capability` correctly *per module*
(e.g., a Java module gets Method-level evidence, a bundled Python
script only gets Type-level) rather than treating "method-level
support" as a whole-repository, all-or-nothing property.

### Scalability

Ties directly to incremental analysis — the same change-scoping
mechanism that makes CSM updates cheap also makes discovery itself
cheap on large repos. Open question: does discovery run as one pass
over the whole repo (with the *CSM Builder* doing the incremental
filtering), or does discovery itself skip unchanged files? Leaning
toward discovery being naturally incremental too (no reason to
re-parse an unchanged file's AST), but this is a design decision, not
settled by Explore alone.

### Partial analysis

What happens when an analyzer can't fully process a file (parse error,
unsupported language feature, binary/generated file, size limit
exceeded)? The CSM's `Method-Level Representation Capability` already
establishes a precedent — graceful degradation is fine, but must be
honestly reported, not silently claimed as complete. Repository
Understanding needs an equivalent: partial/failed evidence must be
recorded as such (not silently dropped, not silently pretended
complete), so the CSM Builder — and ultimately a Finding — can know a
given Module's evidence is incomplete.

### Traceability

The other hard, non-negotiable link to CSM: `Repository Evidence to CSM
Transformation` requires "every CSM element derived from evidence SHALL
retain a traceable reference back to the evidence and source location
that produced it." This means every piece of Repository Evidence needs
a **stable, addressable identity** (not just "this file" but something
that survives across analysis runs, per CSM's `Incremental Model
Updates` stable-identity requirement) — file path alone is not enough
if files get renamed; some evidence-item identity scheme is needed.

## Evidence vs. Inferred Architectural Knowledge

This is the same discipline the CSM change had to learn explicitly (the
reason `Provenance Classification of Knowledge` and `Same-Category
Conflict Marking` exist). The risk here is a Repository Understanding
analyzer being tempted to be "smart" — e.g., guessing that a package
named `com.acme.order.service` is "the Order Service component." That
guess may be legitimate, but it is **inferred architectural knowledge,
not evidence**, and per the CSM's own boundary it belongs in the CSM
(as an `inferred` Architecture Component), not in the Evidence Model.

Working rule for this change: **Repository Evidence records only what
is mechanically true about the repository's structure and content — it
must not contain interpretive judgments, stereotypes, or architectural
classifications, even heuristic ones.** Any heuristic classification an
analyzer might want to perform belongs downstream, in the CSM Builder's
`inferred` territory, not upstream in evidence. This likely deserves to
be a first-class requirement in the eventual spec, mirroring how
explicitly the CSM spec drew its own boundary.

## Open Questions

1. Does this change own the CSM Builder (evidence→CSM transformation),
   or is that a separate, fourth capability? The CSM design assumed a
   3-stage pipeline (analyzer → evidence → builder → CSM);
   "Repository Understanding" per `project.md`'s evolution order seems
   to name only the discovery half.
2. Is discovery itself incremental, or does discovery always run in
   full and incrementality is purely the CSM's problem?
3. Where does "Project" boundary detection live? (single build file =
   one Project? monorepo with multiple build files = multiple
   Projects?) — evidence-discoverable, but the exact heuristic needs to
   be pinned down.
4. What identity scheme survives file renames/moves for evidence-item
   traceability and incremental re-analysis?
5. How is a partial/failed analysis of a single file surfaced — as
   evidence with a "failed" flag, as an absence, or as a distinct
   evidence-quality signal?
6. Does Repository Understanding need its own confidence/quality
   concept (e.g., "parsed cleanly" vs. "best-effort/degraded"), separate
   from CSM's `inferred`-only confidence model, since evidence itself
   can be low-fidelity without being "inferred" in CSM's sense?

## Recommended Next Step (as explored)

Solid enough to formalize as a proposal, provided open question 1 (CSM
Builder ownership) is resolved first — it changes the shape of "What
Changes" and the Capabilities section materially depending on the
answer.
