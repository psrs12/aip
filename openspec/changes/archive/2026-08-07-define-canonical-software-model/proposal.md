## Why

AIP cannot perform meaningful architecture, design, risk, or compliance
analysis without a durable, language-independent representation of a
software system to reason over. Analyzers, rule evaluation, and AI
reasoning all need a common target — but the project must decide what that
representation includes, what it excludes, and how it stays trustworthy
before any analyzer or rule engine can be built against it. Per
`openspec/project.md` §11, the Canonical Software Model (CSM) is the
required foundation immediately after Software Repository Understanding,
and CLAUDE.md requires the CSM be designed and specified before it is
implemented. This change establishes that design and specification.

## What Changes

- Define the **boundary of the CSM**: which entities, relationships, and
  attributes are in scope as durable, structural/relational model content,
  and which belong in adjacent models (evidence store, policy/rule model,
  runtime/observability store, business/org systems, infrastructure-as-code)
  referenced by identity rather than embedded.
- Define the **CSM vocabulary**: a small, closed set of language-neutral
  structural kinds (e.g. containment levels, type/interface concepts,
  method/signature concepts) and relationship types (e.g. depends-on,
  implements, exposes, integrates-with, composed-of, realizes, owns), plus
  the mechanism by which language-specific analyzers map their native
  constructs onto this vocabulary without altering the CSM schema itself.
- Define the **relationship between repository evidence and the CSM**: how
  language-specific, analyzer-produced evidence is normalized/compiled into
  CSM elements, and how CSM elements retain traceability back to the
  evidence and source location that produced them.
- Define how **provenance of architectural knowledge** is represented and
  tracked uniformly across CSM elements — distinguishing exactly three
  provenance categories: observed (mechanically derived from evidence),
  declared (asserted by a human or an authoritative external source —
  externally asserted knowledge is understood as declared, not as a
  separate category), and inferred (produced by AIP's own heuristics or
  AI reasoning), with confidence where applicable — so that downstream
  rule evaluation and AI reasoning never treat declared or inferred
  knowledge as equivalent to mechanically observed fact.
- Define the **separation of technical evidence from inferred/derived
  knowledge**, including how Architecture Components sit at the boundary
  between observed structure and declared/inferred architectural intent.
- Explicitly scope out of the CSM: raw evidence (source text/AST/method
  bodies), governance policies and architecture constraints, runtime
  telemetry streams, full infrastructure-as-code detail, and business/org
  detail beyond thin cross-reference metadata — establishing that these are
  adjacent models the CSM references, not content it owns.

This change does not implement any of the above in code. It does not define
Software Repository Understanding (the discovery/evidence-collection
activity) — that remains a separate capability and change, connected to the
CSM by a defined evidence-to-CSM interface.

## Capabilities

### New Capabilities

- `canonical-software-model`: The language-independent structural and
  relational model of a software system — its boundary, entity/relationship
  vocabulary, provenance tracking, and its interface to repository evidence,
  policy, runtime, and business-context models that remain outside it.

### Modified Capabilities

- None. No existing specs are present in `openspec/specs/` yet.

## Impact

- **Affected specs**: introduces `openspec/specs/canonical-software-model/`.
- **Affected code**: none yet — no implementation in this change.
- **Affects future work**: establishes the schema/vocabulary that the
  Analysis Framework, Rule Framework, Finding Model, and Agent Framework
  (per project.md §11) will all depend on; also establishes the contract
  the future Software Repository Understanding capability must produce
  evidence against.
- **Dependencies**: none introduced (no code, no libraries).
