# Explore: Architecture Compliance Agent

Status: preserved exploration notes carried into this change. Not a
tracked schema artifact — retained for traceability from
proposal/design decisions back to the reasoning that produced them,
following the same convention used by
`archive/2026-08-11-implement-csm-builder/explore.md`,
`define-analysis-framework/explore.md`, `define-rule-framework/explore.md`,
`define-finding-model/explore.md`, and `define-agent-framework/explore.md`.

## Problem Statement

Per `project.md` §11's evolution order, Architecture Compliance Agent
is the **final** named step:

```text
CSM Snapshot -> Analysis Framework -> AnalysisView / AnalysisResults
             -> Rule Framework -> RuleEvaluationResults
             -> Finding Model -> Findings
             -> Agent Framework -> Recommendations
             -> Architecture Compliance Agent
```

Unlike every prior step, this one is not itself a framework — it is
the first *concrete* agent, the one `project.md` §5 names first in its
Architecture Agents category, and the one `CLAUDE.md`'s AI Agent
Architecture section also names first in its own agent list. Both
Rule Framework and Agent Framework are already fully specified as
generic, implementation-agnostic contracts; this Explore covers what
new architecture, if any, a *specific* agent still needs, versus what
is simply configuration/registration against contracts already built.

`project.md` and `CLAUDE.md` give this agent a name and a category
(Architecture Agents) but, unlike every other capability explored so
far, **no further elaboration exists anywhere in the project's
grounding documents** — no dedicated section, no example scenario, no
field list. Everything this Explore grounds itself in comes from
inference against already-approved specifications, not a waiting
paragraph the way `project.md` §4.8 was waiting for Finding Model or
§6 was waiting for Agent Framework.

## Framing: this may be the first capability that introduces no new artifact type

Every prior capability introduced exactly one new durable artifact:
CSM Snapshot, `AnalysisResult`, `RuleEvaluationResult`, Finding,
Recommendation. Architecture Compliance Agent is a strong candidate to
be the first capability that introduces **none** — it is a *concrete
instance* of two already-fully-specified extension points (a Rule
Type, per `define-rule-framework`'s `Extension Mechanism for New Rule
Types`; an Agent, per `define-agent-framework`'s `Agent Registration
and Extension Behavior`), not a new layer in the pipeline. If that
holds, this capability's specification is much more about *behavior
and boundaries specific to this one concrete Rule Type/Agent pair*
than about new structural mechanism — worth confirming explicitly
before Design, not assumed here, because it changes what "Design" even
means for this capability relative to every prior one.

## What already exists, directly grounding this capability's scope

Not invented for this Explore — each of these is already-approved,
binding text:

- **`canonical-software-model`'s `Architectural Boundary
  Representation` requirement** is the one concrete, ready-built policy
  shape this project already models: an Architectural Boundary is a
  relationship between two or more Architecture Components (or a
  Component and an External System) describing a structural constraint
  ("must not depend on", "must only communicate via"), carrying
  declared-or-inferred provenance. Its own text states directly:
  **"Evaluating compliance with a boundary is a function of the
  Policy/Rule Model and SHALL NOT be performed by the CSM itself"** —
  i.e., detecting a boundary violation is explicitly named as a Rule
  Framework concern, not a CSM concern, and (by the pipeline this
  project has built) not something the CSM or Analysis Framework layer
  does either.
- **`canonical-software-model`'s `Architecture Constraints Excluded
  from the CSM` requirement** gives the canonical example policy this
  entire project has repeated at every layer since `project.md` §3.7:
  *"any Module must not depend on any External System without an
  approved integration boundary."* This is the same example
  `define-rule-framework`'s own artifacts used repeatedly to motivate
  Rule Type composition (Decision 2: a Rule Type reading both a
  dependency fact and a boundary-declaration fact).
- **`define-rule-framework/proposal.md`'s own Impact section already
  names this exact capability's likely deterministic half**, not
  invented here: *"the Rule Type/registry pattern a future
  `implement-rule-framework` change and its built-in Rule Types (e.g.
  a boundary-compliance Rule Type, satisfying `canonical-software-model`'s
  own `Architectural Boundary Representation` requirement) will build
  on."* This creates a genuine, real ownership question this Explore
  surfaces rather than silently resolves (see Open Questions, below):
  does a boundary-compliance Rule Type belong to a future
  `implement-rule-framework` change (as that proposal anticipated,
  written before this capability existed) or to this capability?
- **`define-agent-framework`'s own Cross-Capability Impacts section**
  states: *"A future Architecture Compliance Agent... is now known to
  depend on: the Agent contract, `FindingSource`, and Recommendation's
  identity/provenance shape... If such an agent needs content beyond
  what Finding already exposes, that is a scoped extension for that
  future capability's own Design to justify."* This is a direct,
  already-approved invitation: if boundary-violation reasoning needs
  more than a bare Finding's own traceability carries, this capability
  is where that case gets made — not assumed, and not silently
  expanded without justification.
- **`define-agent-framework`'s own `Agent Contract` requirement**
  already fixes this capability's Agent-side shape: exactly one
  Finding consumed per invocation (via `FindingSource` only), exactly
  one Recommendation produced, Confidence Agent-declared, content
  opaque to the framework. Nothing about *this* agent's concrete
  reasoning is pre-decided by that — only the contract shape it must
  satisfy.

## Framing: module placement — likely no new module

Unlike every prior capability (each of which justified a new sibling
module), a concrete Rule Type and a concrete Agent are each, by
`define-rule-framework`'s and `define-agent-framework`'s own extension
requirements, meant to be addable **without core mechanism changes** —
the same way `implement-csm-builder`'s own many concrete Mappers
(Repository, Project, Module, Package, Type, Method, API Contract...)
all live *within* `aip-csm-builder` rather than each getting a
dedicated module. The strong candidate: a boundary-compliance Rule
Type registers within `aip-rules`; a concrete Architecture Compliance
Agent registers within `aip-ai`. No new module. This also fits
`project.md` §5's own scale — dozens of named future agents across
five categories; a one-module-per-agent pattern would not scale the
way one-module-per-capability did for the six framework-level layers
already built.

```text
  aip-core        - CSM domain model, CsmSnapshotSource,
                    AnalysisResultSource, RuleEvaluationResultSource,
                    FindingSource

  aip-rules       - Rule Framework (specified, not yet implemented)
                    - candidate home for a boundary-compliance Rule Type

  aip-ai          - Agent Framework (specified, not yet implemented)
                    - candidate home for a concrete Architecture
                      Compliance Agent

  This capability likely adds code to aip-rules and aip-ai, not a new
  sibling module - a genuine departure from every prior capability's
  own module-placement pattern, worth confirming rather than assuming
```

## What "compliance" means here — scoped to boundary violations, not invented broader

`project.md` names "Architecture Compliance Agent" with no further
definition, and separately lists six other, more specific Architecture
Agents (Drift, Dependency Boundary, Layering, Domain Boundary, API
Architecture, Event Architecture) as siblings, not sub-concerns of this
one. Reading "Architecture Compliance" narrowly — scoped to the one
concrete policy shape the CSM already models (Architectural Boundary
Representation) — is the most conservative, best-grounded candidate:
this agent explains and recommends remediation for **boundary
violations** specifically, not a general, undefined "architecture
compliance" surface that would require inventing taxonomy `project.md`
doesn't provide. The other six Architecture Agents remain separate,
later capabilities, each presumably narrow in the same way once
defined.

## Open architectural questions for Design

None of these have been through a stakeholder-confirmation round yet.

1. **Does this capability define the boundary-compliance Rule Type, or
   assume it already exists?** `define-rule-framework/proposal.md`
   named it as `implement-rule-framework`'s own future work, written
   before this capability was explored. Options: (a) this capability
   specifies the Rule Type (its applicability, its condition, its
   Category/Severity configuration) as part of its own scope, with
   `implement-architecture-compliance-agent` implementing both halves
   together; (b) this capability assumes a boundary-compliance Rule
   Type is `implement-rule-framework`'s responsibility and specifies
   only the Agent half, consuming whatever Findings that Rule Type
   eventually produces; (c) a hybrid — this capability specifies the
   Rule Type's *policy shape* (since it's the one demonstrating
   end-to-end what a real Rule looks like) while leaving generic
   Rule Type infrastructure to Rule Framework's own implementation.
   Not decided here — a real ownership question, not a rhetorical one.
2. **Does this capability introduce any new artifact, or purely
   register against existing extension points?** Framed above as the
   likely case (no new artifact) — needs confirmation, because if
   Design finds a genuine gap (e.g., a boundary-specific traceability
   need Finding's own shape doesn't carry), that would be new
   architecture, not registration, and should be named explicitly
   rather than smuggled in.
3. **Does the Agent need content beyond a bare Finding?** A boundary
   violation's Finding already carries (per `define-finding-model`)
   the concerned CSM element identity, source CSM Snapshot identity,
   and Evidence/traceability content. Is that sufficient for a
   *useful* Recommendation (e.g. "introduce an approved integration
   boundary"), or does good guidance need broader context (e.g. what
   approved integration boundaries already exist elsewhere in this
   repository, so the Agent can suggest reusing one rather than
   inventing generic advice)? If the latter, `define-agent-framework`
   Decision 4 already names the mechanism (a scoped, justified
   additional declared input) — this would be the first concrete case
   exercising it, not a new mechanism.
4. **What does the boundary-compliance Rule Type's condition actually
   check, precisely?** "An observed dependency exists where a declared
   boundary relationship forbids it" is the plain-language version;
   Design needs to state this as a precise, Rule-Scope-shaped
   condition (which CSM relationship kinds it reads, whether it's
   anchored per-Component or per-relationship-pair, how it correlates
   an observed dependency relationship with a declared boundary
   relationship covering the same two Components).
5. **What does a boundary-violation Recommendation actually say, and
   how prescriptive is it?** `project.md` §3.5's remediation workflow
   stops this capability's own output at Recommendation (per
   `define-agent-framework` Decision 9) — no code generation, no
   Proposed Change. Does this agent's Recommendation content have a
   more specific expected shape than Agent Framework's own generic
   "opaque, Agent-defined content" (e.g., does it need to name the
   specific violating relationship, the specific boundary it
   violates, and a suggested remediation *approach* in prose, without
   generating an actual change)? Worth defining as this capability's
   own concrete content expectations, distinct from the generic
   contract.
6. **Declared vs. inferred boundaries — does compliance checking
   differ?** `Architectural Boundary Representation` allows both
   declared and inferred provenance for a boundary. Does this agent
   (or its underlying Rule Type) treat a violation of an *inferred*
   boundary the same as a violation of a *declared* one, or does
   provenance affect Severity/Confidence/how prescriptive the
   Recommendation is?
7. **Module placement — confirmed no new module, or does a concrete
   agent's own prompt/reasoning code need enough structure to justify
   one?** Framed above as "no new module" being the strong candidate;
   not decided.
8. **Does "Compliance" scope stay narrow to boundary violations, or
   does `project.md`'s bare name invite a broader reading Design
   should consider before narrowing?** Framed above as narrow being
   the best-grounded reading; flagged for explicit confirmation since
   `project.md` gives no elaboration either way.
9. **Is this genuinely the last capability in the current evolution
   order, and what happens after it?** `project.md` §11 names nothing
   further; §5 names dozens of other agents with no stated ordering
   among them. Worth noting, not resolving: after this capability,
   `project.md`'s own evolution order is exhausted, and future work
   would draw from §5's broader catalogue without a stated sequence —
   out of scope for this Explore, but worth naming so it isn't
   silently assumed this is the end of all future work.

## Recommended Next Step

Question 1 (Rule Type ownership) is this Explore's most consequential
open question — it determines whether this capability's Design
resolves a genuine cross-capability boundary question (who builds the
boundary-compliance Rule Type) or can assume it away. Question 2
(no new artifact) is the framing question most similar in kind to
prior rounds' central tensions (Finding Model's identity split, Agent
Framework's non-determinism) — except here the candidate answer is
"nothing new is needed," which itself deserves the same explicit
confirmation before Design treats it as settled, rather than being
waved through because it's the "boring" answer.
