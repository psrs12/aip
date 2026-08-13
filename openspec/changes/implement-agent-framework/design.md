## Context

`design.md` and `specs/agent-framework/spec.md` under
`openspec/changes/define-agent-framework/` are the approved source of
truth for *what* Agent Framework does; this document covers *how* it
is implemented, the same relationship `implement-finding-model/design.md`
has to `define-finding-model`. This is the first `implement-*` change
introducing a genuinely non-deterministic mechanism (minting a
Generation Identifier), so several concrete decisions below exist
specifically to keep that non-determinism contained to exactly where
`define-agent-framework/design.md` says it belongs, and nowhere else.

Binding external facts this design does not reopen:
- `aip-core` already hosts (per implementation, not just plan)
  `Finding`, `EvaluationIdentity`, `LogicalFindingIdentity`,
  `RuleEvaluationResultId`, `CsmSnapshotId`, and `Confidence` — all
  implemented and merged (`implement-finding-model`).
- `aip-findings` already hosts a full, working implementation
  (unchanged by this change).
- `Finding`'s concrete shape (final class, non-generic, `Set<RuleEvaluationResultId>`
  reference set, `EvaluationIdentity`/`LogicalFindingIdentity`,
  `CsmElementId concernedElementId`, `CsmSnapshotId sourceSnapshotId`,
  `String category`/`severity`/`description`/`impact`, `Confidence
  confidence`) is fixed and not modified by this change.
- `define-agent-framework`'s eleven binding decisions (no intermediate
  AI Reasoning artifact; Recommendation as a distinct artifact
  referencing exactly one Finding; the two-part identity/provenance
  split; Findings-only consumption via `FindingSource`; no conflict/
  precedence semantics; Agent-produced mandatory Confidence, distinct
  from Finding Confidence; structural-only validation; module/contract
  boundaries; Recommendation-only output boundary; AI-content
  isolation; explicit determinism guarantees and non-guarantees) are
  unmodified.

## Goals / Non-Goals

**Goals:**
- Give `FindingSource`, `Agent`, `AgentRegistry`, `Recommendation`,
  `RecommendationArtifactIdentity`, `GenerationIdentifier`, and
  `GenerationProvenance` concrete Java shapes.
- Resolve `RecommendationStore`'s module placement (already decided
  upstream: stays in `aip-ai`) and its concrete interface shape.
- Resolve exactly how, and where, a Generation Identifier is minted,
  and confirm this is the only place in the entire codebase permitted
  to use non-deterministic generation.
- Resolve Recommendation's Confidence value's concrete type, since
  `define-agent-framework/design.md` establishes it is Agent-declared
  and distinct from Finding's qualitative `Confidence` enum but does
  not fix a concrete representation.
- Resolve how a generation failure (per spec `Failure Semantics`)
  propagates as "no Recommendation," not a partially-constructed one.
- Establish the fixture layer's shape and CI guard wiring, including
  why `check-no-ai-heuristic-imports.sh` is deliberately not applied
  to `aip-ai`.
- Establish test organization, mirroring `implement-finding-model`.

**Non-Goals:**
- Reopening any `define-agent-framework`, `define-finding-model`,
  `define-rule-framework`, or `define-analysis-framework` decision.
- A real LLM/model-provider adapter.
- The Architecture Compliance Agent or any other specific agent.
- Choosing `RecommendationStore`'s concrete persistence technology —
  deferred, per `define-agent-framework/design.md` Decision 8 itself.
- Promoting `Recommendation` or any of its identity/provenance types
  to `aip-core` — explicitly declined upstream (Decision 8), not
  revisited here.

## Decisions

### 1. `FindingSource`'s concrete shape

**Problem:** Concrete shape for `define-agent-framework/design.md`
Decision 8's `aip-core` contract?

**Decision:** Mirrors `RuleEvaluationResultSource` exactly, with two
list methods matching the two grouping keys `Finding Source Shape`
names:

```java
public interface FindingSource {
  Optional<Finding> read(EvaluationIdentity id);
  Set<Finding> listByLogicalFindingIdentity(LogicalFindingIdentity id);
  Set<Finding> listBySourceSnapshotId(CsmSnapshotId snapshotId);
}
```

Two `list*` methods, not one overloaded/unioned method — mirroring
this project's own established practice of one accessor per distinct
retrieval key (e.g. `RuleEvaluationResultStore`'s own
`listByRuleAndSnapshot`, itself a compound but single key; here the
spec names two independent keys, each deserving its own method rather
than a single method accepting an ambiguous "by what" parameter).

**Alternatives considered:**
- **A single `list(Object key)` method dispatching on key type.**
  Rejected — untyped, loses compile-time safety, and no precedent in
  this codebase does this.

### 2. Recommendation's concrete shape: an `aip-ai`-local, non-generic type

**Problem:** Given `define-agent-framework/design.md` Decision 8's
explicit non-promotion, what is `Recommendation`'s concrete Java
shape, and where does it live?

**Decision:** `Recommendation` is a final, non-generic class in
`aip.ai` (not `aip.core.csm`):

```java
public final class Recommendation {
  RecommendationArtifactIdentity id;
  String agentIdentifier;
  int agentVersion;
  EvaluationIdentity findingEvaluationIdentity;
  LogicalFindingIdentity findingLogicalFindingIdentity;
  GenerationIdentifier generationIdentifier;
  Object content;                    // opaque, Agent-defined guidance
  double confidence;                 // Decision 4, below
  GenerationProvenance provenance;   // Decision 5, below
}
```

Structurally mirrors `RuleEvaluationResult`/`Finding`'s own shape
(identity, producing-artifact traceability, an opaque payload) with
two additions those types don't have: `generationIdentifier` (present
directly, not only folded into `id`, so a consumer can inspect it
without recomputing/decomposing the identity) and `confidence`
(mandatory, per spec `Recommendation Confidence Is Agent-Produced and
Mandatory`). No field represents a claimed mutation of CSM content, an
`AnalysisResult`, a `RuleEvaluationResult`, or the referenced Finding
— structurally guaranteed by this field list containing nothing of
that shape (per spec `Deterministic-Layer Protection`).

**Alternatives considered:**
- **A generic `Recommendation<T>` parameterized by content type.**
  Rejected — the established non-generic-with-opaque-`Object`-payload
  pattern (`AnalysisResult`, `RuleEvaluationResult`, and implicitly
  `Finding`'s own typed-but-Rule-Type-declared fields) is reused
  verbatim; no concrete need for compile-time content typing exists.

### 3. `RecommendationArtifactIdentity` and `GenerationIdentifier`

**Problem:** Concrete shapes, and exactly how a Generation Identifier
is minted?

**Decision:** Two single-field records in `aip.ai`:

```java
public record GenerationIdentifier(String value) {
  public static GenerationIdentifier generate() {
    return new GenerationIdentifier(UUID.randomUUID().toString());
  }
}

public record RecommendationArtifactIdentity(String value) {
  public static RecommendationArtifactIdentity of(
      String agentIdentifier, int agentVersion,
      EvaluationIdentity findingEvaluationIdentity,
      GenerationIdentifier generationIdentifier) {
    // deterministic hash over exactly these four components
  }
}
```

`GenerationIdentifier.generate()` is the **only** call to
`UUID.randomUUID()` (or any randomness source) anywhere in this
codebase, and it exists in the one module (`aip-ai`) deliberately
carved out of the `check-no-ai-heuristic-imports.sh` guard (Binding
Decision 2, `proposal.md`) for exactly this reason. `RecommendationConstructor`
(Decision 6, below) is the only caller — an `Agent` implementation
never mints its own Generation Identifier, keeping "the framework
assigns it per invocation" (spec `Generation Identifier Uniqueness and
Non-Content-Derivation`) a structural guarantee, not a convention an
Agent author must remember to follow.

`RecommendationArtifactIdentity.of`'s hash uses a small,
`aip-ai`-local equivalent of `aip-core`'s own package-private
`DeterministicHash` (SHA-256 over a `'|'`-joined canonical string) —
duplicated rather than reused, because `aip-core`'s implementation is
deliberately package-private to `aip.core.csm` and this project's own
established discipline (per `implement-rule-framework/design.md`'s and
`implement-finding-model/design.md`'s own identity types) is that each
identity scheme owns its digest computation directly rather than
reaching across a module boundary for an internal utility. This is a
few lines of duplicated, well-understood code, not a shared contract
worth promoting.

**Alternatives considered:**
- **A monotonic per-Agent invocation counter instead of a UUID.**
  Rejected — spec `Generation Identifier Uniqueness and
  Non-Content-Derivation` explicitly permits either and does not
  prescribe one; a counter would require shared mutable state across
  invocations (an `AgentRegistry`-held counter, or similar), directly
  undermining `Agent Invocation Independence`'s own "no shared mutable
  state between invocations" property (`define-agent-framework/design.md`
  Decision 5's corollary). A UUID requires no shared state at all.
- **Expose `aip-core`'s `DeterministicHash` as a package-visible (not
  private) utility, reused by `aip-ai`.** Rejected — `aip-ai` has no
  compile dependency on `aip-core`'s internal package structure beyond
  its public types; widening `DeterministicHash`'s visibility purely
  for this one additional caller, across a module boundary, is a
  larger and more permanent change than duplicating ~15 lines.

### 4. Recommendation Confidence: a `double` in `[0.0, 1.0]`

**Problem:** `define-agent-framework/design.md` Decision 6 establishes
Confidence is Agent-declared, mandatory, and distinct from Finding's
qualitative `Confidence` enum, but does not fix a concrete
representation.

**Decision:** `Recommendation.confidence()` is a primitive `double`,
constrained to `[0.0, 1.0]` at construction time. This is the
conventional representation for a model-elicited or heuristic
confidence score (continuous, not the three discrete qualitative
levels `aip-core`'s `Confidence` enum uses for CSM's own inferred-
knowledge concept) — reusing that enum here would falsely suggest
Recommendation Confidence shares Finding Confidence's qualitative
vocabulary and meaning, which Decision 6 explicitly says it does not.
A primitive `double` (not `Double`) keeps Confidence structurally
mandatory — no null branch a future reader must interpret, mirroring
the "never optional, mandatory or explicitly fixed" discipline every
other artifact's fields in this system hold to.

**Alternatives considered:**
- **Reuse `aip-core`'s `Confidence` enum**, mirroring `Finding`'s own
  reuse (`implement-finding-model/design.md` Decision 6). Rejected —
  that reuse was justified specifically because Finding's Confidence
  is fixed to one value with no variation to express; Recommendation
  Confidence is the opposite case (genuinely Agent-declared and
  variable), and forcing it into three discrete levels would lose
  precision a model's own expressed certainty (e.g. a raw probability)
  legitimately carries.
- **An unconstrained `double`** with no range validation. Rejected —
  "Confidence" conventionally means a probability-like value; leaving
  the range unconstrained would let a malformed Agent silently declare
  a meaningless value like `47.0`, which the required-field-presence
  validation (Decision 8, below) can and should catch as a structural
  defect.

### 5. `GenerationProvenance`

**Problem:** Concrete shape?

**Decision:**

```java
public record GenerationProvenance(
    String modelProviderIdentifier,
    String modelVersion,
    Map<String, String> generationConfiguration,
    Instant generationTimestamp) { ... }
```

`generationConfiguration` is `Map<String, String>` — opaque,
Agent-defined key/value pairs (e.g. `"temperature" -> "0.2"`), never
interpreted by the framework (per spec `Generation Provenance`),
consistent with every other opaque-payload precedent in this system.
`Instant` for the timestamp — the standard JDK time-point type, no new
type needed.

**Alternatives considered:** none beyond the interface shape
`Generation Provenance`'s own spec text already names field-for-field.

### 6. `RecommendationConstructor`: the invocation pipeline, and failure semantics

**Problem:** Concrete shape for the "one Finding in, one Recommendation
(or none, on failure) out" pipeline, and how a generation failure
propagates per spec `Failure Semantics`?

**Decision:**

```java
public final class RecommendationConstructor {
  public static Optional<Recommendation> construct(Agent agent, Finding finding) {
    GenerationIdentifier generationIdentifier = GenerationIdentifier.generate();
    AgentInvocationResult result;
    try {
      result = agent.invoke(finding);
    } catch (RuntimeException generationFailure) {
      return Optional.empty();
    }
    // wrap result + generationIdentifier + agent identity + Finding's
    // two identities into a full Recommendation, computing its
    // RecommendationArtifactIdentity
  }
}
```

`Agent.invoke(Finding): AgentInvocationResult` (Decision 7, below) is
permitted to throw an unchecked exception to signal a generation
failure (a model call failing or timing out); `RecommendationConstructor`
catches it and returns `Optional.empty()` — no partially-constructed
Recommendation is ever built, matching spec `Failure Semantics`'s
"result in no Recommendation being produced for that invocation,
rather than a partially-constructed or invalid Recommendation being
published" exactly. This mirrors `FindingConstructor`'s own
`Optional<Finding>` return shape one layer down, for the same
"sometimes there is legitimately nothing to construct, silently, not
as an error the caller must separately branch on" reason.

**Alternatives considered:**
- **A checked exception on `Agent.invoke`.** Rejected — would force
  every future Agent implementation and every caller of
  `RecommendationConstructor` to handle a checked exception for what
  this framework already treats as a normal, expected outcome
  (`Optional.empty()`), not an exceptional control-flow path; consistent
  with this codebase's existing preference for `Optional`-shaped "may
  not apply" returns over checked exceptions (e.g. `FindingConstructor`
  itself).
- **Retry the generation step automatically on failure.** Rejected
  explicitly — spec `Failure Semantics` states "This specification
  does not define retry or orchestration infrastructure for generation
  failures"; retrying here would silently exceed that boundary.

### 7. `Agent` contract and `AgentInvocationResult`

**Problem:** Concrete interface shape?

**Decision:**

```java
public interface Agent {
  String identifier();
  int version();
  AgentInvocationResult invoke(Finding finding);
}

public record AgentInvocationResult(Object content, double confidence, GenerationProvenance provenance) { ... }
```

`Agent.invoke` takes a `Finding` directly (already resolved through
`FindingSource` by the caller, mirroring `RuleType.evaluate`'s own
already-resolved-input shape) and returns the three Agent-declared
components `RecommendationConstructor` cannot supply itself (content,
Confidence, Generation Provenance) — everything else on the final
`Recommendation` (identity, Generation Identifier, Agent identifier/
version, Finding's two identities) is computed or supplied by the
framework, never by the Agent, keeping "the framework assigns
identity-bearing components" a structural property (Decision 3).

**Alternatives considered:** none beyond the shape spec `Agent
Contract` and `Recommendation Content Is Agent-Defined and Opaque to
the Framework` together already fix.

### 8. `AgentRegistry`, `RecommendationStore`, `RecommendationValidator`/`RecommendationPublisher`

**Problem:** Concrete shapes for the remaining framework mechanics?

**Decision:** Direct, unmodified continuations of established pattern:
- `AgentRegistry` mirrors `RuleTypeRegistry`: `register(Agent)`,
  `lookup(String identifier): Optional<Agent>`.
- `RecommendationStore` mirrors `FindingStore`: `write(Recommendation)`,
  `read(RecommendationArtifactIdentity): Optional<Recommendation>`.
- `RecommendationValidator.validate(Recommendation, FindingSource,
  AgentRegistry): ValidationResult` checks, per spec `Recommendation
  Validation Before Publication`: the referenced Finding (by
  Evaluation Identity) exists and is resolvable via
  `FindingSource.read`; the declared Agent identifier/version matches
  a currently registered Agent; Generation Provenance is present with
  a non-blank model/provider identifier and a non-null timestamp;
  Confidence is within `[0.0, 1.0]`; content is non-null. It does
  **not** re-verify the referenced Finding's own upstream chain
  (consumed `RuleEvaluationResult`s, CSM content) — trusting
  `FindingValidator`'s own already-completed work, the same
  trust-boundary posture every prior validator in this project holds
  to.
- `RecommendationPublisher.publish(RecommendationStore, Recommendation,
  FindingSource, AgentRegistry): PublicationOutcome` — validate-then-
  write gate, mirroring `FindingPublisher` exactly.

**Alternatives considered:** none — no genuine fork; this is the
"everything else is a direct, un-modified continuation of established
pattern" portion `define-agent-framework/design.md`'s own Context
section already names.

### 9. Module scaffolding: `aip-ai`, structurally isolated, `check-no-ai-heuristic-imports.sh` deliberately not wired

**Problem:** Dependency-graph enforcement mechanics, and how to
reconcile this module's legitimate use of randomness with the
project's existing no-randomness guard?

**Decision:** `aip-ai` is a new Maven module depending on `aip-core`
only. Its POM wires:
- `check-module-dependencies.sh` (`aip-ai`, `aip:aip-core`) — the
  forward-direction guard.
- `check-fixture-package-scope.sh` (`aip-ai`, `aip.ai.test.fixtures`).
- Four executions of `check-no-module-reference.sh`, forbidding
  `aip\.findings\.`, `aip\.rules\.`, `aip\.analysis\.`, and
  `aip\.csmbuilder\.` — the complete no-cross-reference set against
  every deterministic module, mirroring `aip-findings`'s own three
  (which covered its own three deterministic upstream siblings;
  `aip-ai` has one more, `aip-findings` itself, to exclude).

`check-no-ai-heuristic-imports.sh` is **deliberately not wired** for
`aip-ai` — see `proposal.md`'s Binding Decision 2. The reverse
guard ("no deterministic module depends on `aip-ai`") requires no new
script: every one of `aip-csm-builder`/`aip-analysis`/`aip-rules`/
`aip-findings`'s own existing `check-module-dependencies.sh` executions
already assert "depends only on `aip:aip-core`," which structurally
makes a dependency on `aip-ai` impossible without editing one of those
four already-guarded POMs — an edit this change makes to none of them.

**Alternatives considered:**
- **Write a new reverse-dependency script.** Rejected — the existing
  four forward guards already fully close this gap; a new script would
  be redundant enforcement of a property already mechanically
  guaranteed.
- **Apply `check-no-ai-heuristic-imports.sh` to `aip-ai` anyway, with
  a carve-out for `UUID`.** Rejected — the guard's own forbidden-pattern
  list already excludes `java.util.UUID` (it targets `java.util.Random`/
  `SecureRandom`/`ThreadLocalRandom` and named AI/LLM SDK packages, not
  `UUID`), so applying it would pass today, but doing so would
  misrepresent `aip-ai` as subject to the same "must stay
  deterministic" discipline every other module's own use of that guard
  actually enforces — this module is deliberately exempt in kind, not
  merely in current import list.

### 10. Fixture layer

**Problem:** What does the test-only fixture layer need to provide?

**Decision:** `aip-ai/src/test/java/aip/ai/test/fixtures/` provides:
- `InMemoryFindingSource` — a fixture `FindingSource`, mirroring
  `InMemoryRuleEvaluationResultSource`.
- `InMemoryRecommendationStore` — a fixture `RecommendationStore`,
  mirroring `InMemoryFindingStore`.
- `StubAgent` — a configurable `Agent` (fixed content/confidence/
  provenance on success; a mode that throws to exercise generation
  failure), with a nested `Context`-style configuration record,
  mirroring `aip-rules`'s own `StubRuleType`.
- A small helper for constructing fixture `Finding`s directly via
  `Finding.of(...)` (`aip-core`, already public) — no CSM-element-graph
  builder needed, since Agent Framework, like Finding Model, never
  reads CSM content itself.

**Alternatives considered:** none — directly follows from Agent
Framework's own no-CSM-content-reading nature, the same reasoning
`implement-finding-model/design.md` Decision 10 already gave one layer
down.

## Risks / Trade-offs

- [`GenerationIdentifier.generate()`'s `UUID.randomUUID()` call is the
  only non-deterministic code path in the entire codebase, deliberately
  outside every other module's no-randomness guard] → Intentional and
  narrowly scoped: exactly one call site (`RecommendationConstructor`),
  in exactly one module, for exactly the one purpose spec `Generation
  Identifier Uniqueness and Non-Content-Derivation` requires. Not a
  gap in the guard system — a deliberate, documented, minimal
  exception to it.
- [`RecommendationValidator`'s Confidence range check (`[0.0, 1.0]`) is
  an implementation-level convention, not literally specified — spec
  `Recommendation Confidence Is Agent-Produced and Mandatory` requires
  presence, not a specific numeric range] → Accepted, documented in
  Decision 4: representing Confidence as a `double` at all is this
  change's own concrete-shape decision; constraining its range is the
  natural corollary of choosing that representation, not an invented
  requirement beyond what the upstream spec settled.
- [Duplicating a small `DeterministicHash`-equivalent in `aip-ai`
  (Decision 3) rather than reusing `aip-core`'s package-private one]
  → Accepted, low-risk: ~15 lines, well-understood algorithm, no
  behavioral drift risk since both compute the identical SHA-256-over-
  joined-components function.
- [No orchestrator that automatically discovers every Finding and
  invokes every registered Agent against each — this change provides
  only the per-invocation building blocks (`Agent`, `AgentRegistry`,
  `RecommendationConstructor`, validate/publish)] → Deliberate: neither
  `define-agent-framework/design.md` nor `spec.md` requires a
  Finding-discovery-and-dispatch loop the way Rule Framework's own
  Scope-instance enumeration required an orchestrator; "which Findings
  to invoke which Agents against, and when" is left as a caller/future-
  CLI concern, not something this framework's own contract mandates.
