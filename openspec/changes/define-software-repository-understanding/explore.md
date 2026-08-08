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

## Resolved Questions

1. **Does this change own the CSM Builder (evidence→CSM transformation),
   or is that a separate capability?** — Resolved: **no, it does not.**
   The archived CSM spec already contains a CSM-side requirement,
   `Repository Evidence to CSM Transformation`, that specifies the
   transformation stage's behavioral contract (map evidence onto CSM
   vocabulary, attach provenance/confidence, preserve traceability).
   That contract already belongs to the `canonical-software-model`
   capability, not to this one. The CSM design document confirms this
   split explicitly: "The Repository Evidence Model itself — its shape,
   and the analyzer capability that produces it — is designed in the
   separate Repository Understanding change; this design only fixes the
   shape of the interface the CSM side depends on." Repository
   Understanding is therefore scoped strictly to **Repository →
   Repository Evidence**: discovery and the Evidence Model's shape. The
   CSM Builder's implementation is future work for the CSM capability
   itself — the same future `implement-canonical-software-model` change
   already placeholdered in the archived CSM change's `tasks.md` (4.2)
   and `design.md` Context section. Repository Understanding does not
   need to know anything about CSM vocabulary, provenance categories, or
   confidence levels.

   ```
   ┌──────────────┐     ┌──────────────────┐     ┌─────────────────────┐     ┌───────────┐
   │  Repository   │────▶│  Repository        │────▶│  CSM Builder          │────▶│    CSM     │
   │               │     │  Evidence          │     │  (transformation stage)│     │           │
   └──────────────┘     └──────────────────┘     └─────────────────────┘     └───────────┘
        owned by:              owned by:                   owned by:
        Repository              Repository                  Canonical Software Model
        Understanding           Understanding                capability (future
        (this change)           (this change)                implementation change)
   ```

   Practical effect on the proposal: in scope is repository/build-file
   discovery, per-language source analyzers (Java first), the
   Repository Evidence Model's structure, incremental change-detection,
   partial-analysis reporting, and evidence-item identity/traceability.
   Out of scope is anything that assigns CSM provenance categories,
   confidence levels, or vocabulary classifications.

2. **Incremental discovery / change detection.** CSM's `Incremental
   Model Updates` requirement already mandates "change-scoping input
   from the Repository Evidence Model" — this is a hard, already-approved
   dependency, not optional. Re-discovering an entire large repository
   on every pass defeats that requirement, and pushing the diff work
   downstream into the CSM Builder is architecturally awkward: it would
   require CSM to retain a full copy of prior evidence to diff against,
   which `CSM Scope Boundary` already forbids. The diffing has to happen
   on the Repository Understanding side.

   *Options:* (A) full re-discovery every run, no incrementality at RU
   — simplest, but has nowhere valid to keep "prior evidence" for the
   CSM Builder to diff against; (B) RU performs change detection itself
   (via VCS diff / content-signature comparison against the prior
   discovery pass) and tags each evidence item
   added/modified/removed/unchanged before handing evidence onward; (C)
   an external trigger (e.g. CI) tells RU what changed.

   **Resolved: B**, with C usable only as an optional performance hint
   layered on top of B (skip a full repo walk when a reliable external
   diff is available; fall back to B's own detection otherwise) — never
   as the sole mechanism, since not every trigger context can supply a
   trustworthy diff (squashed merges, non-git VCS, first-ever scan).
   Trade-off: B costs RU some internal state (a manifest of prior
   evidence signatures) but keeps "what changed" entirely on the
   repository-facing side, where it belongs, instead of forcing the CSM
   Builder to re-derive something it has no legitimate way to compute.

   RU-owned: the change-detection mechanism, the manifest/state format,
   and the added/modified/removed/unchanged vocabulary (a
   discovery-lifecycle concept, unrelated to CSM provenance). CSM
   Builder-owned: deciding which CSM elements actually need
   re-derivation from a changed evidence item, including
   dependency-aware invalidation (e.g., re-checking a changed Module's
   dependents) — CSM's own incremental logic, working off RU's signal.

3. **Project boundary detection.** CSM's containment hierarchy assumes
   Repository→Project→Module→Package→Type, but "Project" isn't
   self-evident — one build file, many nested build files (monorepo),
   or no build file at all are all real cases.

   *Options:* (A) build-file-driven — a Project is anchored by a
   recognized build/package manifest (`pom.xml`, `package.json`,
   `go.mod`, ...), with multi-module build systems mapping to one
   Project containing multiple Modules, matching that ecosystem's own
   concept; (B) directory-heuristic-driven — every top-level directory
   is its own Project regardless of tooling; (C) purely externally
   declared via a human-maintained config, no automatic detection; (D)
   hybrid — A as the default, with an optional override/declaration for
   repos that don't map cleanly (no build file, or a build tool whose
   module concept doesn't match what should be modeled as a Project).

   **Resolved: D.** Build-file detection matches what each ecosystem's
   own tooling already agrees on, so it is accurate for the common
   case; the override exists for the inevitable edge cases, mirroring
   the same "closed-but-extensible, escape hatch for what doesn't fit"
   philosophy the CSM used for its own vocabulary. Trade-off: B is
   simple but would wrongly split legitimate multi-module setups; C is
   safest long-term but fails AIP's own discovery mandate (project.md
   §4.1) as a sole mechanism; D costs a small, extensible registry of
   recognized build-system markers plus an override format. Files with
   no owning build file and no override become an explicit "unmanaged"
   surface (tied to question 5 below) rather than silently merged into
   an arbitrary Project.

   RU-owned: the whole detection algorithm, the build-system registry,
   the override file format, how "unmanaged" files are surfaced.
   CSM-Builder-owned: essentially nothing — Project is a clean case
   where RU's evidence maps 1:1 onto the CSM's `observed` Project entity
   with no CSM-side judgment involved.

4. **Evidence-item identity.** This is the load-bearing decision for
   both incremental discovery (question 2) and CSM's own stable-identity
   requirement — the decision that most directly determines whether
   continuous monitoring of large repositories is tractable rather than
   something that "flaps" (spurious delete+recreate) on every trivial
   change.

   *Options:* (A) path-based identity — normalized repo-relative path
   (+ inner-element path for sub-file items); simple, but a file rename
   with unchanged content breaks identity continuity, which — per CSM's
   stable-identity requirement — looks like deletion+recreation,
   causing unnecessary re-derivation churn and potentially disrupting
   adjacent-model references (e.g., a declared Business Capability
   mapping keyed to a Module that "reappears" under a new identity after
   a harmless package rename); (B) content-hash-based identity —
   survives moves when content is unchanged, but changes identity on any
   trivial edit (even a comment) — the opposite failure mode, and
   expensive to compute at scale without its own pre-filtering; (C)
   composite/canonical-name identity — for evidence with a
   language-native canonical name (Type, Method, Module, Package,
   Project), identity is that canonical name, so a Type's identity
   survives its file being renamed; falls back to path-based identity
   (A) for evidence with no natural canonical name (config files, IaC
   fragments, the build manifest itself); (D) combination — C as the
   actual identity scheme (with A as its necessary fallback), plus
   content hashing used only as an internal, cheap change-detection
   pre-check (question 2) — never as identity itself.

   **Resolved: D.** This gives the most semantically stable identity
   CSM actually needs, while being honest that raw files without a
   natural name have no better option than path.

   A sharp, deliberate line: rename detection relies only on
   **mechanically verifiable** signals (e.g., an explicit VCS rename
   record), never on content-similarity heuristics ("this new file is
   95% similar to that deleted one, probably a rename"). That kind of
   similarity judgment is *inference*, and inference does not belong in
   Repository Evidence — it would be exactly the "smart" architectural
   guessing this exploration already ruled out of scope (see "Evidence
   vs. Inferred Architectural Knowledge" above). When no mechanical
   rename signal exists, a path-identified item that disappears and a
   new one that appears are honestly treated as remove+add.

   RU-owned: the entire identity scheme, the hash-based
   change-detection optimization, and the mechanical-rename-only rule.
   CSM-Builder-owned: how RU's stable identity gets mapped onto CSM's
   own element identity across schema versions — a CSM-side concern
   that trusts RU's identity as its anchor.

5. **Partial-analysis reporting.** Parsing can fail or be incomplete for
   many reasons (syntax errors, unsupported constructs,
   generated/binary/oversized files, timeouts). Silently dropping these
   creates invisible gaps that look like "no violations found" instead
   of "we couldn't look" — a direct threat to Evidence-Based
   Intelligence (project.md §3.8).

   *Options:* (A) silent skip — unacceptable, recreates exactly the
   silent-gap risk the CSM's own `Method-Level Representation
   Capability` precedent already rejected; (B) best-effort fallback with
   no status flag — worse than A in a sense, since it looks identical to
   a clean parse to any downstream consumer, hiding the gap instead of
   just dropping it; (C) explicit per-item outcome status — `complete` /
   `partial` (with reason) / `failed` (with reason), attached to the
   evidence item (or a placeholder item, for files that fail before any
   structure is extracted); (D) C plus a coarse failure-reason taxonomy
   (parse error, unsupported construct, size/timeout limit, excluded by
   config, binary/non-text).

   **Resolved: D** — cheap to add over C, and valuable for diagnostics
   and future analyzer-improvement decisions.

   RU-owned: the status vocabulary, the failure-reason taxonomy, and how
   a partial/failed item is represented. CSM-Builder-owned: what to do
   with a `partial`/`failed` item when building the CSM — e.g., still
   emitting an `observed` element for the part that succeeded, or how a
   Module with incomplete evidence should be flagged for downstream
   analysis. That is a CSM/Analysis-side policy decision, not RU's.

6. **Evidence quality / confidence.** Is there a quality signal RU needs
   beyond question 5's three-way status, and how do we ensure it never
   becomes a disguised copy of CSM's confidence model?

   *Options:* (A) nothing beyond question 5's status — simple, but
   forecloses future flexibility (e.g., a fast heuristic pre-scan mode
   for very large repos, or noting evidence came from an older analyzer
   version); (B) a separate "extraction-method" tag per evidence item
   (e.g., `full-parse` / `heuristic-scan` / `externally-declared`),
   orthogonal to question 5's outcome status — captures *how
   rigorously* something was looked at, not *whether it finished*; (C)
   reuse CSM-style HIGH/MEDIUM/LOW tiers for evidence too.

   **Resolved: B. Option C is explicitly rejected.** CSM's confidence
   model exists specifically to qualify *inferred architectural*
   knowledge; Repository Understanding never infers anything about
   architecture, so borrowing that vocabulary would misleadingly imply
   RU is making CSM-style judgments and would blur exactly the boundary
   this exploration exists to protect. B is named unmistakably
   differently from CSM's vocabulary, with no numeric or tiered scale at
   all on the RU side: RU's job is to honestly *label how it obtained* a
   piece of evidence, never to *rate how much to trust* it.

   RU-owned: the extraction-method taxonomy and tagging. CSM-Builder-owned:
   whether/how an evidence item's extraction-method tag influences
   anything CSM-side (e.g., being more conservative about a CSM element
   sourced from a heuristic-scan item) — an interpretive judgment that
   belongs downstream, not to RU.

## Deferred Questions

Genuinely deferrable to Design/Spec — narrowing these further now would
not change the boundary or any decision above:

- Exact, exhaustive failure-reason taxonomy (question 5) and
  extraction-method taxonomy (question 6) — illustrative categories are
  given above; the full enumeration follows the same
  core-but-extensible pattern the CSM used for its own vocabulary.
- Exact mechanical rename-detection signal to rely on (e.g., specific
  VCS rename-tracking APIs/thresholds) — an implementation choice
  within the "mechanical signals only, never similarity heuristics"
  rule already fixed above.
- Exact representation of the "unmanaged" pseudo-project surface
  (question 3) for files with no owning build file and no override.

## Repository Understanding → Repository Evidence Boundary

Consolidating all six resolutions into one boundary statement:

**Repository Understanding owns:** discovery of repository content and
structure; the Repository Evidence Model's shape; per-language source
analyzers; build-file-driven Project detection (with override);
change-scoped incremental discovery keyed on a composite
canonical-name/path identity scheme; honest complete/partial/failed
outcome reporting with a failure-reason taxonomy; and an
extraction-method tag describing how each piece of evidence was
obtained. None of this requires knowledge of CSM vocabulary, provenance
categories, or confidence levels.

**Repository Understanding explicitly does not own:** the CSM Builder
(evidence→CSM transformation, already specified as a CSM-side
requirement and deferred to a future CSM implementation change);
assigning `observed`/`declared`/`inferred` provenance to anything;
assigning HIGH/MEDIUM/LOW confidence to anything; any architectural
interpretation, stereotyping, or classification of discovered structure
(that is `inferred` CSM knowledge, not evidence).

## Recommended Next Step (as explored)

All six questions are now resolved at the decision level, with three
narrow implementation-detail items intentionally deferred to Design
(see above) — none of them scope-changing. Ready to move to the
proposal.
