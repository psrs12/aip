# Explore: Canonical Software Model (CSM)

Status: preserved exploration notes carried into this change. Not a tracked
schema artifact — retained for traceability from proposal/design decisions
back to the reasoning that produced them.

## Problem Statement

What information must AIP collect and understand about a software system to
perform meaningful architecture, design, risk, and compliance analysis?

Specifically: what is the relationship between **Software Repository
Understanding** (the discovery activity) and the **Canonical Software Model**
(the durable, language-independent representation), and where is the line
between them?

## Observations

- Repository Understanding and the CSM are pipeline stages, not synonyms:
  Repository Understanding = language-specific discovery producing evidence;
  CSM = the normalized, durable, language-neutral graph that evidence is
  compiled into.

  ```
  Repository Understanding (activity)   →   Canonical Software Model (representation)
  language-specific evidence extraction      language-neutral structure that
  (AST, build files, VCS, IaC, config)       analysis, rules, and AI reasoning
                                              actually run against
  ```

- The CSM's boundary criterion is not "is this data about the software" but
  "is this a structural/relational fact needed for cross-cutting architecture
  reasoning, expressed in language-neutral terms." Everything else (raw
  evidence, policy, runtime streams, business org detail, infra detail) is an
  adjacent model referenced by ID, not embedded.

- Provenance and confidence are not optional metadata on top of the CSM —
  they need to be a first-class, uniform property across CSM elements, since
  "discovered fact" vs. "declared" vs. "inferred" vs. "external assertion"
  changes how downstream rules and AI reasoning must treat a piece of
  information (project.md §3.8 Evidence-Based Intelligence, §6 AI
  Philosophy).

- Architecture Components are the pivot point of the whole model — the place
  where discovered structure (packages/modules) meets declared/inferred
  intent (e.g. "this is the Order Service"), and where governance policy
  actually attaches. This needs dedicated design care, not treatment as
  "just another container."

- Incremental analysis is not a bolt-on feature; it constrains CSM design
  now (stable node identity, snapshotting/versioning, layered volatility
  between code-derived and business/policy metadata) rather than being
  solvable later.

## Area-by-Area Findings

### Structural containment: Repository → Project → Module → Package → File

| | Repository | Project | Module | Package | Source File |
|---|---|---|---|---|---|
| Why needed | Discovery/versioning/ownership unit | Independent build/deploy unit | Dependency-resolution unit | Logical grouping/encapsulation | Evidence provenance unit |
| Auto-discoverable | Yes (VCS) | Yes (build files) | Yes (build files) | Yes (AST/dir structure) | Yes (filesystem/VCS) |
| Externally provided | Naming/ownership metadata only | Business/product mapping | — | — | — |
| Evidence vs. derived | Evidence | Evidence | Evidence | Evidence | Evidence |
| In CSM | Yes, identity/root node | Yes | Yes | Yes | Border case — see below |
| Outside CSM | Raw VCS commit log | — | Build-tool-specific config | — | Raw text/AST |
| Relationships | contains Projects | contains Modules; depends-on Projects | contains Packages; depends-on Modules | contains Types | declares Types |

Open fork: does the CSM need a "File" node, or should file path be an
evidence/location attribute on Type/Method instead (Option B)? Leaning
toward Option B — a file is a storage artifact, not an architectural
concept; multiple types can share a file, and "file bloat" isn't the same
kind of concern as "class bloat."

### Code structure: Types/Classes/Interfaces, Methods

| | Types/Classes/Interfaces | Methods |
|---|---|---|
| Why needed | Coupling/cohesion/SOLID/pattern/boundary analysis | Call-graph, complexity, cross-boundary invocation |
| Auto-discoverable | Yes, AST | Yes, AST |
| Externally provided | No | No |
| Evidence vs. derived | Evidence (existence/signature) + derived (stereotype/role, often inferred) | Evidence (signature, calls-out) + derived (complexity metrics) |
| In CSM | Yes | Yes — signatures and call relationships, not full bodies |
| Outside CSM | Method bodies, private implementation detail | Full control-flow graph (analyzer-local evidence) |
| Relationships | implements/extends, depends-on, exposes-API, belongs-to-package | calls, is-called-by, belongs-to-type |

Language-independence mechanism: CSM defines a small, closed vocabulary of
structural kinds and relationship types. Each language analyzer maps its own
native constructs (Java class/interface/record/enum; Go struct/interface;
Python class/protocol; C# class/interface/record) onto that vocabulary.
Anything that doesn't map cleanly is preserved as an opaque "native
evidence" attribute bag on the node — it does not become a new CSM concept.
New language support = new analyzer + mapping, not new CSM schema.

### Dependencies, APIs, External Systems

| | Dependencies | APIs | External Systems |
|---|---|---|---|
| Why needed | Layering/circularity/boundary rules — core of architecture analysis | Contract surface for boundary/compatibility analysis | Blast-radius, risk, SPOF analysis |
| Auto-discoverable | Mostly (import/build graph); *sanctioned intent* is not | Partially (OpenAPI/.proto discoverable; ad-hoc internal APIs less so) | Partially (connection/SDK usage discoverable; criticality/ownership is not) |
| Externally provided | Sanctioned/forbidden dependency policy | API classification (public/internal/deprecated) sometimes curated | Criticality, ownership, contractual info |
| Evidence vs. derived | Evidence: import edges. Derived: violation findings | Evidence: schema/signature. Derived: "is public API" | Evidence: outbound call. Derived: risk classification |
| In CSM | Yes — dependency graph is central | Yes — typed relationship + contract descriptor | Yes, as opaque boundary nodes |
| Outside CSM | Historical drift trend (analytics over CSM snapshots) | Full schema bodies (reference + summary in CSM; body in evidence store) | Runtime telemetry from the external system |
| Relationships | depends-on (direction, strength, type: compile/runtime/test) | exposes, consumes | calls-external, integrates-with |

External systems are modeled as boundary nodes: opaque interior, attributed
exterior only (name, type, criticality, integration protocol, owner). AIP
does not attempt to model a third-party system's internals.

### Configuration & Infrastructure Definitions

| | Configuration | Infrastructure Definitions |
|---|---|---|
| Why needed | Secrets exposure, environment-specific behavior, flags affecting architecture | Deployment topology; whether code structure matches deployment structure |
| Auto-discoverable | Yes (config files, env var references) | Yes (Terraform/K8s/Dockerfiles/Helm) |
| Externally provided | Never secret values — only references | Cloud account/environment mapping sometimes |
| Evidence vs. derived | Evidence | Evidence (topology) + derived (module-to-deployable mapping, drift) |
| In CSM | Only structural facts (reads-config, references-secret) — not values | Thin "Deployable Unit" node + relationship to Modules |
| Outside CSM | Config values, environment overrides | Full IaC graph (security groups, IAM) — adjacent model, cross-referenced by ID |
| Relationships | reads-config, references-secret | deployed-as, module-maps-to-deployable |

### Architecture Components & Architectural Boundaries

| | Architecture Components | Architectural Boundaries |
|---|---|---|
| Why needed | The unit layering/boundary policies reference | The rule surface where violations are detected |
| Auto-discoverable | Partially (naming/package heuristics; not reliable alone) | No — boundaries are intent |
| Externally provided | Often (architect-declared component definitions) | Yes — boundary definitions are policy |
| Evidence vs. derived | Derived (inferred) or asserted (declared) — provenance must be distinguished | Derived from policy + structural evidence |
| In CSM | Yes — grouping layer over Modules/Packages, tagged `provenance: declared \| inferred` | Yes — typed relationship/constraint anchored to Components |
| Outside CSM | Inference confidence/rationale may belong with Finding evidence rather than permanent CSM state | — |
| Relationships | composed-of Modules/Packages | constrains relationships between Components |

This is the most important CSM design fork: an Architecture Component is a
named grouping whose provenance (declared vs. inferred) must be tracked as a
first-class attribute, or a low-confidence inference risks being silently
treated as ground truth by rule evaluation later.

### Business Context: Business Capabilities, Domains, Ownership, Criticality

| | Business Capability | Domain | Ownership | System Criticality |
|---|---|---|---|---|
| Why needed | Business relevance for risk prioritization | DDD-style domain-boundary analysis | Routing findings; bus-factor proxy | Prioritizing findings by business impact |
| Auto-discoverable | No | Partially inferable, essentially external | Partially (git blame is a weak proxy; CODEOWNERS helps) | No |
| Externally provided | Yes | Yes (optionally AI-assisted suggestion) | Yes (CODEOWNERS as assist, not sole source) | Yes |
| Evidence vs. derived | External assertion | Mixed | Mixed (assertion + weak evidence) | External assertion |
| In CSM | Yes — mapping layer over technical components | Yes | Yes, as metadata on Component/Module | Yes, as metadata/tier |
| Outside CSM | Org chart, product roadmap (referenced, not owned) | — | Personal contact info/team rosters (linked by owner ID) | — |
| Relationships | realized-by Component(s) | contains Component(s) / bounded-context | owns (Team ↔ Component) | rates (Component/System) |

Business context relates to technical structure via a separate,
thinly-connected mapping layer (`realizes`, `owns`, `belongs-to-domain`
edges into technical nodes) — not fusion into technical node identity. This
keeps each side independently revisable and keeps provenance
(asserted vs. discovered) unambiguous, and protects incremental analysis
since business mappings change on a different cadence than code.

### Architecture Constraints & Governance Policies

```
Governance Policy (org-wide, machine-readable, e.g. "Handlers must not
                    access repositories directly")
        │ instantiated-as / scoped-to
        ▼
Architecture Constraint (that policy applied to this system's actual
                          components)
```

| | Architecture Constraints | Governance Policies |
|---|---|---|
| Why needed | The evaluable rule against real CSM nodes | The reusable, org-level rule definition |
| Auto-discoverable | No | No |
| Externally provided | Yes | Yes, version-controlled |
| Evidence vs. derived | Neither — a distinct category: intent/rule | Same |
| In CSM | **No** | **No** |
| Outside CSM | Yes — separate Policy/Rule model referencing CSM vocabulary | Yes |
| Relationships | references Component/Layer *kinds*, not just instances | — |

Constraints and policies are not CSM content — they are a separate model
*written in terms of* CSM vocabulary (node kinds, relationship types,
component names) and evaluated against a CSM snapshot to produce findings.
This separation is what makes "policy as code" and "language-independent
CSM" both possible simultaneously.

### Runtime Information

| | Runtime Information |
|---|---|
| Why needed | Validates whether the static structural model matches actual behavior; feeds performance/operational risk analysis |
| Auto-discoverable | Yes, but from a different source (APM/observability) on a different cadence (continuous stream vs. commit-triggered) |
| Externally provided | Often ingested via integration (OpenTelemetry, cloud provider APIs) |
| Evidence vs. derived | Evidence, but a different species: temporal/observational rather than structural |
| In CSM | **No**, not the raw stream. Only a stable cross-reference key (runtime identity) and optional derived enrichment (e.g. `depends_on.confirmed_at_runtime`) |
| Outside CSM | Trace store, metrics time-series, incident log — separate systems, joined by identity key |
| Relationships | observed-in runtime-service (loose join key, not a structural edge) |

This keeps the CSM a structural snapshot (reproducible, per project.md's
Reliability quality attribute) while letting runtime evidence enrich
specific edges/nodes without turning the CSM into a telemetry sink.

### Evidence vs. Derived Knowledge (cross-cutting, not a separate area)

Every CSM node/edge should be classifiable along two axes:

```
PROVENANCE AXIS                       CERTAINTY AXIS
- Discovered  (from source)           - Fact      (directly observed)
- Declared    (human/spec)            - Inferred  (heuristic, has confidence)
- Inferred    (AIP heuristic)         - Asserted  (external, taken as given,
- External    (3rd-party API)                      not independently verified)
```

Without this, downstream rule evaluation and AI reasoning cannot distinguish
"AIP is certain X depends on Y" from "AIP guesses this package is the Order
component with 70% confidence" — a distinction project.md §3.8 and §6
explicitly require to survive into findings.

## Questions Explored

1. What is the boundary of the CSM?
2. What should NOT belong in the CSM?
3. How should language-specific information be represented without making
   the CSM language-specific?
4. How should business context relate to technical structure?
5. How should runtime information relate to the CSM?
6. How should architecture constraints be represented?
7. How should external systems be represented?
8. What information is required for incremental analysis?

## Options Considered

- **File as CSM node vs. file as evidence/location attribute** — leaning
  toward the latter (Option B): a file is a storage artifact, not an
  architectural concept.
- **Architecture Component inference in v1** — considered whether AIP
  should attempt automatic component inference immediately, vs. requiring
  declared components initially and deferring inference to a later
  capability, to avoid conflating "discovery" and "interpretation" in the
  first implementation.
- **CSM root: single-repository vs. multi-repository** — considered
  whether Repository is necessarily the CSM root, or whether a CSM can span
  multiple repositories for a system composed of many repos.

## Trade-offs

- Keeping method bodies, full IaC, policy definitions, and runtime streams
  *outside* the CSM keeps it reproducible, language-neutral, and
  reasonably sized — at the cost of requiring well-defined cross-reference
  joins to adjacent models (evidence store, policy engine, observability
  platform, infra model).
- Tracking provenance/confidence uniformly adds schema complexity up front,
  but its absence would let inferred/asserted knowledge silently masquerade
  as fact in later rule evaluation and AI reasoning — judged not worth the
  risk.
- Treating declared vs. inferred Architecture Components as distinct from
  the start adds a modeling dimension early, but deferring it risks a
  redesign once automatic component inference is introduced later.

## Preliminary Conclusions

1. Repository Understanding and CSM are separable, sequential capabilities
   connected by an evidence → CSM-builder interface, not a single capability.
2. The CSM boundary criterion: structural/relational facts needed for
   cross-cutting architecture reasoning, in language-neutral terms; adjacent
   detail (evidence, policy, runtime, business org detail, infra detail)
   stays in adjacent models referenced by ID.
3. Provenance and confidence must be first-class, uniform CSM metadata.
4. Architecture Components are the pivot point where discovered structure
   meets declared/inferred intent, and need dedicated design attention.
5. Incremental analysis requirements (stable identity, snapshotting,
   layered volatility) constrain CSM design now, not later.

## Open Questions Carried Forward

- Does "Source File" get any CSM representation, or purely evidence-location
  metadata?
- Is a Method-level CSM node warranted for every language, or should some
  analyzers stop at Type-level with method signatures/counts only?
- Rough scale of the closed structural vocabulary — a dozen kinds? Three
  dozen?
- Should Architecture Component be user-declared-only in v1, deferring
  automatic inference to a later capability?
- Where does an API contract's schema body live — CSM reference plus
  evidence-store body, or an inline lightweight structural summary?
- Is there one CSM per repository, or can a CSM span multiple repositories
  for a system composed of many repos? This affects whether Repository is
  the CSM root or merely a contained node.

## Recommended Next Step (as explored)

Formalize a change scoped to the CSM boundary and vocabulary specifically
(this change, `define-canonical-software-model`), keeping "Software
Repository Understanding" as a separate, preceding/sibling capability and
change — the pipeline-stage distinction above indicates these are separable
capabilities connected by a clean evidence → CSM interface.
