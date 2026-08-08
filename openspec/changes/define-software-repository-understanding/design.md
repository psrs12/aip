## Context

See `proposal.md` for motivation and `explore.md` for the full
exploration this design formalizes. This document is conceptual and
technology-independent: it defines the architecture of the Repository
Understanding capability and the shape of the Repository Evidence
Model it produces. It does not choose a programming language, parser
library, storage engine, or concurrency framework for the
implementation, except where a technology-level distinction is
necessary to state the architectural boundary clearly — such
mentions are explicitly labeled "implementation option," not decided
here.

The boundary this design preserves, unchanged from the proposal:

```
Repository
    ↓  (discovery)
Repository Understanding      ◀── THIS CHANGE
    ↓  (produces)
Repository Evidence            ◀── THIS CHANGE defines the shape
    ↓  (consumed by, not built here)
[future] CSM Builder           ◀── future CSM implementation work
    ↓  (transformation)
Canonical Software Model       ◀── already specified (archived)
```

## Goals / Non-Goals

**Goals:**
- Define the Repository Evidence Model's structure: evidence kinds,
  identity, relationships, and metadata (outcome status,
  extraction-method).
- Define the discovery architecture: how a repository is walked,
  how Projects/Modules/Files are found, and how language- and
  build-system-specific analysis plugs in.
- Define incremental discovery, evidence identity, and traceability
  precisely enough to support continuous monitoring of large
  repositories.
- Resolve the three remaining Explore questions (failure-reason /
  extraction-method taxonomy detail, exact rename-detection signal,
  unmanaged pseudo-project representation).
- Define the extension mechanism for new languages and build systems.
- Define, precisely, the interface contract the future CSM Builder
  will consume — without designing the CSM Builder itself.

**Non-Goals:**
- Choosing a parser library, storage technology, or concurrency
  framework.
- Designing the CSM Builder (evidence→CSM transformation) — that
  remains future CSM-side implementation work.
- Introducing CSM provenance categories, CSM confidence, CSM
  reconciliation, or architectural intent classification anywhere in
  this capability — Repository Understanding produces evidence, not
  knowledge.
- Producing an exhaustive, closed catalog of every language/build
  system/failure reason — this design fixes the categories and the
  extension mechanism, following the same pattern the CSM used for its
  own vocabulary.

## Decisions

### 1. Repository Evidence Model — Overall Shape

**Problem:** The Evidence Model needs a concrete shape precise enough
for the CSM Builder to consume, without becoming a second copy of CSM
vocabulary (which would blur the Evidence/Knowledge line the CSM
spec's `Evidence and Knowledge Distinction` requirement already draws).

**Options:**
- (A) Flat list of per-file records with no relationships between
  them.
- (B) A structured graph of typed **Evidence Items**, each with an
  identity, a kind, kind-specific attributes, a source location, an
  outcome status, an extraction-method tag, and relationships to other
  Evidence Items (containment, dependency, reference) — expressed in
  the Evidence Model's own vocabulary, not the CSM's.
- (C) Raw, per-analyzer output with no common shape at all.

**Trade-offs:** (A) cannot represent containment or dependency, which
the CSM Builder needs for traceability and which incremental discovery
needs for blast-radius scoping. (C) makes every consumer (including a
future CSM Builder) write per-language adapters against arbitrary
shapes, reintroducing the language-coupling problem the CSM avoided
at its own layer. (B) costs an upfront schema but gives one stable,
language-neutral *evidence* vocabulary (distinct from CSM's
*knowledge* vocabulary) that every analyzer target and every consumer
shares.

**Recommendation: (B).** Evidence kinds are named after the discovery
*fact* they represent (e.g., `SourceUnit`, `BuildManifest`,
`ImportEdge`, `ConfigReference`), not after CSM's closed vocabulary —
an Evidence kind like `SourceUnit` records a language-native construct
(a Java `class`, a Go `struct`) **using that language's own native
name**, not pre-classified into CSM's `Type`/`Interface` distinction.
That classification is the CSM Builder's job. This is the sharp form
of the Evidence/Knowledge boundary: Repository Understanding never
pre-interprets a construct into CSM's vocabulary, even when the mapping
looks obvious.

### 2. Repository / Project / Module Discovery

**Problem:** Formalizing Explore's resolved decision (build-file-driven
detection + override) into an architecture, and resolving the
previously-deferred "unmanaged pseudo-project" representation.

**Architecture:** A **Discovery Orchestrator** walks the repository
tree once per run and applies a **Build-System Detector Registry** —
pluggable detectors, each recognizing its own manifest marker(s)
(`pom.xml`, `package.json`, `go.mod`, ...) and knowing how to enumerate
that build system's own sub-modules for multi-module setups. Each
successful detection produces a `Project` Evidence Item (and `Module`
Evidence Items for any sub-modules the build system itself declares).
An optional override file (`.aip/projects.yaml` or equivalent) lets a
human declare Project boundaries the registry doesn't recognize.

**Unmanaged pseudo-project representation (resolved):** files claimed
by no detected Project and no override are collected into a single,
well-known, per-repository `Project` Evidence Item with a reserved
identity (e.g. `unmanaged`), rather than being silently dropped or
merged into an arbitrary neighboring Project. This keeps discovery
coverage fully queryable — "how much of this repository is unmanaged"
is itself a visible, honest signal, and a large unmanaged bucket is a
prompt to add a detector or override, not a hidden gap.

**Alternative considered:** one unmanaged pseudo-project per
unclaimed top-level directory (finer-grained, but adds structure for
a case that is, by definition, unstructured) — rejected for now in
favor of the single-bucket approach; can be revisited if a real
repository shows this matters (deferred to Design refinement / spec,
not architecture-changing).

### 3. Source Analysis Architecture

**Architecture:** File discovery → language identification (primarily
file extension, with optional content sniffing for ambiguous
extensions) → dispatch to the registered **Language Analyzer** for
that language → the analyzer produces `SourceUnit` Evidence Items
(Type-level, at minimum) and, where supported, nested Method-level
Evidence Items, each tagged with an outcome status and
extraction-method (see decisions 4 and 11).

**Options considered:** a single monolithic multi-language parser
(rejected — recreates the exact language-coupling problem the CSM
vocabulary was designed to avoid, one layer upstream) vs. a pluggable
per-language analyzer registry (recommended — mirrors the CSM's own
"new language = new analyzer, never a schema change" philosophy,
extended to this capability).

### 4. Language Analyzer Abstraction

**Problem:** Define a stable contract new language analyzers implement,
independent of any specific language's tooling.

**Contract (conceptual, not a Java interface):** given a file (or
related file set) and repository context, an analyzer returns: zero or
more `SourceUnit` Evidence Items (native construct name, native kind
label, source location, and optional nested Method-level Evidence
Items); one outcome status (decision 11); one extraction-method tag
(decision 11); and an optional bag of **native evidence attributes** —
opaque, analyzer-defined key/value data preserved for a future CSM
Builder's own "native evidence attribute bag" escape hatch (the CSM's
mechanism for absorbing language-specific detail without expanding its
schema — this design deliberately keeps the two escape hatches
aligned across the boundary).

**Options:** (A) require every analyzer to produce a full AST; (B)
require only minimal Type/Method signature-level facts, with deeper
detail optional via the native-attribute bag.

**Recommendation: (B).** Matches the CSM's own Type-minimum /
Method-optional capability model and keeps the contract cheap enough
that onboarding a new language doesn't require a full-fidelity parser
on day one — a lightweight analyzer that only extracts top-level
construct names is still valid, just less capable, consistent with
`Method-Level Representation Capability`'s own precedent for graceful,
honestly-reported capability variance.

### 5. Build-System Analysis

**Architecture:** A separate **Build-System Detector Registry**,
structurally similar to the Language Analyzer Registry but operating
at the manifest/directory level rather than the individual-file level:
each detector recognizes its manifest format, extracts declared
dependencies (with declared dependency kind, e.g. Maven's
`<scope>`), and enumerates any sub-modules the build system itself
defines.

**Alternative considered:** unifying build-system detection and
language analysis into one plugin mechanism. Rejected — their unit of
work differs fundamentally (a manifest describing a Module's
dependencies and sub-structure vs. a source file describing types and
methods); keeping them distinct extension points keeps each simpler
even though both follow the same registry pattern.

### 6. Dependency Discovery

**Problem:** Dependencies are discoverable at two different fidelities
that must not be silently merged.

**Decision:** Keep two distinct dependency Evidence kinds:
- **Manifest-declared dependencies** (`ManifestDependencyEdge`):
  Module-to-Module or Module-to-external-library edges from
  build-system analysis, carrying the declared dependency kind
  (compile-time/runtime/test-only) as stated by the manifest.
- **Source-level reference edges** (`ImportEdge`): Type-to-Type or
  File-to-File edges from source analysis (e.g. Java `import`
  statements).

**Trade-off:** Pre-merging these into one "dependency" evidence kind
would be convenient for a consumer, but would silently discard
information the CSM Builder — or a future risk analysis — might care
about (e.g., a manifest-declared dependency with zero corresponding
source-level references, suggesting an unused dependency). Keeping
them separate costs a small amount of consumer-side complexity in
exchange for not losing that signal. The CSM Builder decides whether
and how to reconcile the two into a single CSM `depends-on`
relationship — that reconciliation is explicitly CSM-side work, not
evidence-side.

### 7. Deterministic API Relationship Discovery

**Problem:** Scope "API relationship discovery" tightly enough that it
never crosses into inferring what is architecturally "the public API"
— only what is mechanically declared as a contract.

**Decision:** A distinct Evidence kind (`ApiContractDeclaration`) is
produced only from **mechanically discoverable, explicit contract
declarations** — an OpenAPI/Swagger document, a `.proto` file, or an
explicit framework annotation/declaration that marks an endpoint or
exported interface (e.g., a route registration, an explicit `export`).
It is never produced from "this class looks like a controller" —
that kind of stereotype guessing is inferred architectural knowledge
and stays entirely out of Repository Understanding.

**Options:** (A) parse and store the full schema body as evidence
content; (B) record only a reference to the schema file's location; (C)
a hybrid — a lightweight, deterministically-extractable structural
summary (operation names, paths, verbs) as evidence attributes,
alongside a reference to the full schema body rather than duplicating
it.

**Recommendation: (C).** This deliberately avoids pre-committing to
where a full schema body should live long-term — the CSM's own spec
already lists "API contract schema body placement" as an open question
for a future specification. Providing both a queryable summary and a
location reference means Repository Understanding's evidence is useful
today without foreclosing that future CSM-side decision.

### 8. Configuration Reference Discovery

**Decision:** Record the existence of configuration files
(`ConfigFile` Evidence Items) and, from source analysis, the fact that
code references a configuration key or secret-like identifier
(`ConfigReference` Evidence Items) via **syntactic pattern matching**
against known configuration-access idioms (e.g. `System.getenv(...)`,
a framework's `@Value("${...}")`-style injection). Configuration
**values** are never captured, per the CSM's own exclusion of
configuration values and secrets.

**Options:** (A) simple syntactic pattern matching (recommended); (B)
deep data-flow analysis of configuration wiring end-to-end. (B) is
noted as a plausible future deepening but is out of scope — it is
substantially more complex, language/framework-specific, and not
needed to satisfy the CSM's current configuration-reference
requirement. Pattern matching is mechanical detection of "a reference
exists," not architectural judgment, so it stays on the evidence side
of the line.

### 9. Incremental Analysis and Change Detection

**Architecture:** Repository Understanding maintains a **Discovery
Manifest** — its own persisted state (not CSM content, not evidence
content in the sense the CSM Builder consumes; a bookkeeping artifact
of the discovery process itself) mapping each evidence item's identity
to its last-seen content signature, last discovery outcome, and the
analyzer version that produced it. Each run:
1. Applies cheap pre-filters (mtime/size) to skip obviously-unchanged
   files before any hashing.
2. Computes content signatures for candidates and compares against the
   manifest to classify each item as added/modified/removed/unchanged.
3. Additionally reclassifies an item as needing reprocessing if the
   **analyzer version** that would now process it differs from the one
   recorded — an improved analyzer succeeding where a prior version
   reported `partial`/`failed` is a legitimate "modified" transition
   even when the file's own content hasn't changed (see decision 15).

**Exact rename-detection signal (resolved):** Rely on the source
control system's own rename-tracking (e.g. git's tree-diff rename
detection) as the sole rename-detection signal, with a fallback to
honest remove+add when no VCS is present or it lacks that capability.

This refines the framing from Explore, which described rename
detection as relying only on "mechanically verifiable" signals as
opposed to "content-similarity heuristics." In practice, most VCS
rename detection (including git's) *is* implemented as a
similarity computation over blob content — there is usually no
separately recorded "this was a rename" fact to point to. The
distinction that actually matters, and that this design commits to, is
**not** "similarity computation vs. no similarity computation," but:
- The computation is confined to **identity-continuity bookkeeping**
  (is this the same evidence item as before) — it produces no
  statement about the software's architecture or design intent.
- It uses the VCS's own deterministic, versioned, repeatable algorithm
  — Repository Understanding does not invent its own similarity
  heuristic on top of raw file content.
- The outcome is recorded transparently: an evidence item whose
  identity was preserved via rename detection is flagged as such in
  the Discovery Manifest, never silently indistinguishable from an
  unchanged item.

This keeps the original intent (no invented architectural inference in
evidence) intact while being honest about how rename tracking actually
works.

### 10. Evidence Identity and Traceability

**Decision:** Formalizing Explore's composite identity scheme:
identity = **(Repository identifier, Evidence kind, Scope key)**,
where Scope key is:
- a **canonical/logical name** for evidence with a language-native
  name (a `SourceUnit`'s fully-qualified name, a Module's build-system
  identifier, a Project's manifest-declared name) — this is what
  survives a file move/rename with unchanged content; or
- a **normalized repository-relative path** for evidence with no
  natural name (config files, IaC fragments, the build manifest file
  itself).

Identity is explicitly **not** the same as source location: every
evidence item also carries a **current** source location (file path +
position range where applicable) that can legitimately change between
runs (a file moves, a method shifts lines) without the item's identity
changing. This separation — stable identity vs. current location — is
what makes both incremental discovery (decision 9) and the CSM's own
traceability requirement (`Repository Evidence to CSM Transformation`)
satisfiable simultaneously: the CSM Builder anchors on identity for
stability, and reads location for "where is this evidence, right now."

### 11. Partial-Analysis and Failure Handling

**Outcome status (per evidence item / discovery attempt):**
`complete` | `partial` | `failed`.

**Failure/partial-reason taxonomy (resolved, illustrative and
extensible — not closed forever):**
- `parse-error` — a syntax error prevented a full parse.
- `unsupported-construct` — the language was recognized but a specific
  construct isn't yet handled.
- `unsupported-language` — no analyzer is registered for this file
  type; the file exists only as a bare `File` Evidence Item with no
  structural content.
- `size-limit-exceeded`
- `timeout`
- `binary-or-non-text`
- `excluded-by-configuration` — deliberately excluded (e.g. vendored or
  generated code).

**Extraction-method tag (resolved, illustrative and extensible):**
- `full-parse` — a complete, structural parse succeeded.
- `heuristic-scan` — a best-effort text/pattern-based extraction was
  used (instead of, or after, a full-parse attempt).
- `manifest-declared` — the evidence came from a build manifest's
  declarative content, not source parsing.
- `externally-declared` — the evidence came from an explicit
  human-provided override (e.g. a manual Project-boundary override).

Both taxonomies are governed the same way the CSM governs its own
vocabulary: a shared, versioned enumeration that analyzers select from
and that is extended only through a deliberate process, not
unilaterally per analyzer. This design fixes the categories and the
extension mechanism; the exhaustive final list is a spec-level, not
architecture-level, concern.

### 12. Multi-Language Repository Handling

**Decision:** Discovery operates per-Module (or per-unmanaged-scope);
within a Module's scope, each source file is independently routed to
its own language's analyzer. A Module's evidence can freely mix
languages (a Java backend, an embedded SQL migration directory, a
Python build helper script) — each file's evidence stands on its own,
tagged with its own language/analyzer. Repository Understanding never
computes or asserts a single "primary language" for a Module; if that
concept is ever useful, it is a derived/inferred fact for the CSM or
Analysis side to compute from evidence, not a fact Repository
Understanding manufactures itself. Method-level capability (per
`Method-Level Representation Capability`) is tracked per file/analyzer,
never assumed uniformly across a whole Module or repository.

### 13. Large-Repository Scalability

**Problem:** A full, naive walk-and-parse of a very large repository on
every run does not scale, and this directly affects whether AIP can
continuously monitor large repositories at all.

**Options:**
- (A) Single-process, single-pass full walk every run — simplest, does
  not scale.
- (B) Incremental scoping only (decision 9) — necessary but not
  sufficient on its own for the *first* full scan of a large repo.
- (C) Stateless, per-file analyzer invocation — each file/module is
  analyzed independently with no shared mutable state, so work is
  embarrassingly parallelizable.
- (D) Streamed/chunked evidence emission — evidence is produced
  incrementally (e.g. per-module or per-batch) rather than requiring
  the entire Evidence Model for a large monorepo to be materialized in
  memory at once before anything can be consumed.

**Recommendation: combine B + C + D.** Incremental scoping bounds
*repeat* work; stateless per-file analysis makes even a first full
scan parallelizable; streamed emission avoids an all-or-nothing memory
requirement. None of this is a specific technology choice (no
particular parallel-execution framework or streaming protocol is
selected here) — it is an architectural commitment that the discovery
pipeline's unit of work is small, independent, and streamable, which
any reasonable implementation technology can satisfy.

### 14. Extension Points for Additional Languages/Build Systems

**Decision:** The **Language Analyzer Registry** (decision 4) and the
**Build-System Detector Registry** (decision 5) are the only two
extension points. Adding a language or build system means registering
a new plugin against the stable Analyzer Contract or Detector Contract
— it never requires modifying the Evidence Model's core structure
(identity scheme, outcome status, extraction-method vocabulary). This
mirrors, one layer upstream, exactly the discipline the CSM's own
`Language Independence` requirement established: new capability comes
from new plugins, never from redesigning the shared model.

### 15. Evidence Lifecycle and Update Semantics

**Decision:** An evidence item's lifecycle, tracked via the Discovery
Manifest: **new** → **present** (unchanged across N runs) →
**modified** (content changed, or the responsible analyzer version
changed and produces a different result) → **removed** (tombstoned —
retained as a visible "this existed, and was here" record for at least
one subsequent cycle, not purged immediately) → eventually **purged**
per a retention policy. The tombstone period gives a consumer (a CSM
Builder, or an auditor) the ability to observe a removal explicitly
rather than having evidence simply vanish. The exact retention window
is an implementation/spec detail, deferred (see Open Questions) — it
does not affect this lifecycle's architecture.

### 16. Boundary Between Evidence and CSM Construction

**Decision (reaffirming proposal.md and Explore's resolved question
1):** The Repository Evidence Model is a complete, independently valid,
independently queryable artifact — "what does this repository look
like structurally, right now, per discovery" is fully answerable from
evidence alone, with no dependency on the CSM Builder existing. The CSM
Builder is the sole, exclusive consumer that transforms evidence into
CSM knowledge (assigning provenance, confidence, vocabulary
classification, and performing reconciliation) — none of which
Repository Understanding performs, references, or anticipates
structurally. This capability has zero outbound dependency on CSM
concepts; the dependency runs in one direction only, evidence → CSM.

## Risks / Trade-offs

- [Composite canonical-name/path identity scheme is more complex to
  implement per-analyzer than plain path-based identity] → Mitigation:
  judged necessary — it is what makes CSM's own stable-identity
  requirement and low-churn incremental analysis simultaneously
  achievable; the complexity is contained inside each analyzer's own
  canonical-naming logic, not exposed to consumers.
- [VCS-based rename detection is unavailable in non-git repositories or
  VCS-less contexts] → Mitigation: honest fallback to remove+add is an
  accepted, visible limitation, not a silent gap.
- [Extensible failure-reason/extraction-method taxonomies risk
  inconsistent tagging if analyzers are developed independently] →
  Mitigation: both taxonomies are centrally governed and versioned,
  the same discipline the CSM applies to its own vocabulary.
- [Parallel, streamed evidence emission for large-repo scalability adds
  coordination complexity to Discovery Manifest updates] → Mitigation:
  manifest updates can be structured as append-only/batched per unit of
  work rather than requiring a single shared-mutable-state bottleneck;
  the exact concurrency mechanism is an implementation decision,
  deferred.
- [A single repository-wide "unmanaged" bucket could become a large,
  undifferentiated dumping ground] → Mitigation: it is fully visible
  and queryable by design — a large unmanaged bucket is itself a
  legible discovery-quality signal prompting a new detector or
  override, not a hidden gap.
- [Analyzer-version-driven reprocessing (decision 15) causes evidence
  churn after every Repository Understanding upgrade, even with no
  repository changes] → Mitigation: accepted and desired — an improved
  analyzer producing better evidence should be reflected; this is
  documented expected behavior, not a defect.

## Migration Plan

Greenfield — no existing Repository Understanding implementation to
migrate from. Sequencing note: a future CSM Builder implementation
must be built against the Evidence Model contract this design fixes
(decision 16's interface summary); this design does not require, and
should not wait for, the CSM Builder's own change to exist first.

## Open Questions

- Exact canonical-naming algorithm per language (e.g., how a language
  without a "fully qualified class name" concept, such as Go, should
  compute a canonical Scope key) — deferred to the specification and
  to each analyzer's own implementation; does not change this design's
  identity-scheme architecture.
- Exact tombstone retention window before a removed evidence item is
  purged from the Discovery Manifest — an operational/implementation
  parameter, not an architectural one.
- Exact concurrency/parallelism mechanism for large-repository
  scalability (decision 13) — an implementation choice; this design
  only commits to the unit of work being small, independent, and
  streamable.
- Whether a derived "primary language per Module" signal is ever worth
  computing, and by whom (Repository Understanding as an additional
  observed fact, vs. left entirely to CSM/Analysis) — low priority, not
  resolved here, does not block specification of the decisions above.
