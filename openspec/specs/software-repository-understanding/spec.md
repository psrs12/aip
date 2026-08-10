## Purpose

Software Repository Understanding discovers a software repository's
structure and content and produces the Repository Evidence Model — the
sole, language-neutral-in-shape-but-native-in-content upstream input a
future CSM Builder consumes to construct the Canonical Software Model,
without performing any of the architectural interpretation that
capability reserves for itself.

## Requirements

### Requirement: Repository Evidence Model Shape
Repository Understanding SHALL produce a Repository Evidence Model
composed of typed Evidence Items, each carrying: a stable identity, an
Evidence kind, an attribute set defined by that Evidence kind's own
versioned attribute schema ("kind-specific attributes"), a current
source location (where applicable), a discovery outcome status, an
extraction-method tag, and relationships to other Evidence Items
(containment, dependency, or reference). Every Evidence kind SHALL
have a defined, versioned attribute schema; an Evidence Item SHALL
carry only attributes defined by its kind's schema, or preserve
otherwise-uncaptured detail in the native evidence attribute bag (see
Language Analyzer Contract), rather than carrying arbitrary
unschematized attributes. The Repository Evidence Model SHALL be a
complete, independently valid, independently queryable artifact whose
validity does not depend on any downstream consumer existing.

#### Scenario: Evidence Model queryable without a CSM Builder
- **WHEN** a Repository Evidence Model has been produced for a
  repository and no CSM Builder has been implemented or run
- **THEN** the Repository Evidence Model SHALL remain a valid,
  queryable representation of that repository's discovered structure

#### Scenario: Every Evidence Item carries required attributes
- **WHEN** an Evidence Item is produced by any discovery mechanism
- **THEN** it SHALL carry an identity, an Evidence kind, a discovery
  outcome status, and an extraction-method tag

#### Scenario: Kind-specific attributes conform to a versioned schema
- **WHEN** an Evidence Item of a given Evidence kind is produced
- **THEN** its kind-specific attributes SHALL conform to that Evidence
  kind's own versioned attribute schema, or be recorded in the native
  evidence attribute bag when not captured by that schema

---

### Requirement: Repository Identity
Repository Understanding SHALL establish a stable Repository
identifier for the repository being discovered and SHALL produce a
Repository Evidence Item representing the repository as the root of
its containment hierarchy. This Repository identifier SHALL be used as
the repository-identifier component of every other Evidence Item's
identity (see Evidence Identity). Repository Understanding SHALL NOT
prescribe a single required mechanism for establishing the Repository
identifier beyond requiring that it remain stable across repeated
discovery runs of the same repository; the exact derivation (e.g. from
a configured root, a VCS origin, or an explicit input) is an
implementation detail.

#### Scenario: Repository Evidence Item established
- **WHEN** Repository Understanding begins a discovery run against a
  repository
- **THEN** it SHALL produce a Repository Evidence Item carrying a
  stable Repository identifier used as the anchor for every other
  Evidence Item's identity produced during that run

#### Scenario: Repository identifier stable across runs
- **WHEN** the same repository is discovered in two separate discovery
  runs with no change to its identifying characteristics
- **THEN** the resulting Repository identifier SHALL be the same
  across both runs

---

### Requirement: Evidence Identity
Every Evidence Item SHALL carry a stable identity composed of a
repository identifier, its Evidence kind, and a scope key. The scope
key SHALL be a canonical/logical name for Evidence Items that have a
language- or tool-native name (a source construct's fully-qualified
name, a Module's build-system-declared identifier, a Project's
manifest-declared name), and SHALL be a normalized repository-relative
path for Evidence Items with no natural name (configuration files, IaC
fragments, build manifest files themselves). Identity SHALL be distinct
from an Evidence Item's current source location: identity SHALL remain
stable across a re-analysis in which the underlying construct has not
materially changed, even if its current location has.

#### Scenario: Canonical-name identity for a named construct
- **WHEN** a source analyzer discovers a construct with a
  language-native fully-qualified name
- **THEN** the resulting Evidence Item's identity SHALL be based on
  that canonical name, not on the file's path

#### Scenario: Path-based identity for unnamed evidence
- **WHEN** discovery produces an Evidence Item for a configuration
  file, IaC fragment, or build manifest with no language-native
  canonical name
- **THEN** the resulting Evidence Item's identity SHALL be based on its
  normalized repository-relative path

#### Scenario: Identity survives an unrelated location change
- **WHEN** a named construct's identity is established and a subsequent
  discovery run finds that construct's current source location has
  changed but the construct itself has not materially changed
- **THEN** the Evidence Item SHALL retain the same identity while its
  recorded current source location is updated

---

### Requirement: Evidence Kind Vocabulary Uses Native Terms
Evidence kinds SHALL describe discovered facts using the language's or
tool's own native terminology (e.g., a Java `class`, a Go `struct`, a
Maven `<dependency>`). Repository Understanding SHALL NOT classify an
Evidence Item into the Canonical Software Model's closed vocabulary
(e.g., CSM's `Type`, `Interface`, or `Architecture Component` entity
kinds) at any point.

#### Scenario: Native construct name preserved as-is
- **WHEN** a language analyzer discovers a construct
- **THEN** the resulting Evidence Item SHALL record that construct's
  native kind label (e.g. "interface", "struct", "record") rather than
  a CSM entity kind

#### Scenario: No CSM vocabulary classification performed
- **WHEN** any Evidence Item is produced by any discovery mechanism
- **THEN** the Evidence Item SHALL NOT carry a CSM entity kind or CSM
  relationship type as its classification

---

### Requirement: Repository Discovery
Repository Understanding SHALL discover a repository's structure by
walking its file tree, producing a File Evidence Item (see File
Evidence and Kind-Specific Layering) for every discoverable file
relevant to subsequent discovery stages, including source files, build
manifests, configuration files, and infrastructure-as-code (IaC)
fragments. Repository Understanding SHALL, at minimum, discover IaC
files as generic File Evidence. Repository Understanding MAY
additionally identify a file as IaC-related through deterministic
file/path or recognized-pattern evidence (e.g. a recognized directory
convention or file extension), analogous to the pattern matching
performed by a Configuration Recognizer. This capability SHALL NOT
define semantic analysis of IaC content (e.g. interpreting Terraform,
CloudFormation, or Kubernetes manifest semantics, resource graphs, or
architectural implications); such analysis, if ever introduced, is out
of scope for Repository Understanding as defined by this change.

#### Scenario: Repository walk produces file-level evidence
- **WHEN** Repository Understanding is run against a repository
- **THEN** it SHALL produce a File Evidence Item for every discoverable
  file relevant to source, build-system, configuration, or IaC
  discovery

#### Scenario: IaC files receive at least generic File Evidence
- **WHEN** Repository Understanding discovers a file recognized as an
  infrastructure-as-code fragment (e.g. by a recognized file extension
  or directory convention)
- **THEN** it SHALL produce a File Evidence Item for that file, and MAY
  additionally tag it as IaC-related via deterministic pattern
  evidence, without performing any semantic interpretation of the IaC
  content

#### Scenario: No semantic IaC analysis performed
- **WHEN** an infrastructure-as-code fragment is discovered
- **THEN** Repository Understanding SHALL NOT produce any Evidence Item
  asserting the meaning, resource graph, or architectural implications
  of that fragment's content

---

### Requirement: File Evidence and Kind-Specific Layering
Every discoverable, relevant file SHALL receive a File Evidence Item
representing its bare existence and current physical location,
regardless of whether any richer, kind-specific evidence (e.g. a
`SourceUnit`, a build manifest, a `ConfigFile`, an API contract
declaration) is also produced for it. A File Evidence Item is a
physical/source-location anchor, not a structural container: it SHALL
NOT be represented as containing a Package or as a parent in the
structural containment chain defined by Package Evidence and
Containment (Module contains Package, Package contains source
construct, source construct contains method). Where a file is
successfully processed by a Language Analyzer, a Build-System
Detector, or a Configuration Recognizer, the resulting kind-specific
Evidence Item(s) SHALL be related back to that file's File Evidence
Item via a reference relationship recording where that evidence was
physically found — distinct from, and in addition to, whatever
structural containment relationships those Evidence Items separately
carry. Every kind-specific Evidence Item's traceability back to its
originating File Evidence Item SHALL be preserved regardless of its
position in the structural containment chain.

#### Scenario: Every relevant file receives File Evidence
- **WHEN** Repository Understanding discovers any file relevant to
  source, build-system, configuration, or IaC discovery
- **THEN** it SHALL produce a File Evidence Item for that file whether
  or not richer, kind-specific evidence is also produced

#### Scenario: Kind-specific evidence references its originating File Evidence
- **WHEN** a source file is successfully processed by a Language
  Analyzer
- **THEN** the resulting source-construct Evidence Item(s) SHALL carry
  a reference back to that file's File Evidence Item recording their
  physical location, distinct from their structural containment
  (Package, Module) relationships, and the File Evidence Item SHALL
  remain retrievable

#### Scenario: File Evidence is not a structural container
- **WHEN** a source construct's Package containment is queried
- **THEN** the Package Evidence Item, not the File Evidence Item,
  SHALL be recorded as its structural parent, even though the source
  construct also references the File Evidence Item for its physical
  location

#### Scenario: Unsupported-language file retains only File Evidence
- **WHEN** no Language Analyzer is registered for a file's identified
  language
- **THEN** that file SHALL be represented solely by its File Evidence
  Item, carrying the `failed` outcome and `unsupported-language`
  failure reason (see Source-File Discovery and Language
  Identification), with no kind-specific evidence layered on top

---

### Requirement: Project and Module Discovery
Repository Understanding SHALL detect Project boundaries using a
registry of recognized build-system markers (e.g. a `pom.xml`,
`package.json`, or `go.mod` file), producing a Project Evidence Item
for each detected build-system root. For a build system that itself
declares sub-modules, Repository Understanding SHALL produce a Module
Evidence Item for each declared sub-module, contained by its Project.
Where a detected or override-declared Project's build system declares
no explicit sub-modules, Repository Understanding SHALL produce a
single logical, default Module Evidence Item contained by that
Project, so that source and other evidence discovered within the
Project's scope retains a Module-level containment anchor. Repository
Understanding SHALL also support an explicit, externally-provided
override for declaring Project boundaries the build-system registry
does not recognize.

#### Scenario: Project detected from a recognized build manifest
- **WHEN** a directory contains a manifest recognized by the
  build-system registry
- **THEN** Repository Understanding SHALL produce a Project Evidence
  Item rooted at that directory

#### Scenario: Multi-module build system produces multiple Modules
- **WHEN** a detected Project's build system declares multiple
  sub-modules
- **THEN** Repository Understanding SHALL produce one Module Evidence
  Item per declared sub-module, each contained by that Project

#### Scenario: Single-module Project yields a default Module
- **WHEN** a detected or override-declared Project's build system
  declares no sub-modules
- **THEN** Repository Understanding SHALL produce exactly one default
  Module Evidence Item contained by that Project, and all source and
  other evidence discovered within the Project's scope SHALL be
  represented as contained by that default Module

#### Scenario: Override declares an unrecognized Project boundary
- **WHEN** an explicit override designates a directory as a Project
  boundary that the build-system registry does not recognize
- **THEN** Repository Understanding SHALL produce a Project Evidence
  Item for that directory based on the override

---

### Requirement: Project Boundary Precedence
Where an explicit, externally-provided override and build-system
detection both apply to overlapping repository content, the override
SHALL take precedence over build-system detection for that overlapping
scope. Build-system detection SHALL take precedence over the fallback
unmanaged-Project assignment (see Unmanaged Pseudo-Project Handling)
for its own detected scope. Repository Understanding SHALL NOT
silently merge two conflicting explicit overrides that both claim
overlapping repository content into a single combined boundary. Where
two or more explicit overrides conflict over the same repository
content, Repository Understanding SHALL report the conflict as a
discovery outcome of `failed` for the affected scope, with a
`conflicting-override` failure reason (see Failure Reason Taxonomy),
rather than inventing a merged or arbitrarily chosen boundary.

#### Scenario: Explicit override takes precedence over build-system detection
- **WHEN** an explicit override and build-system detection both apply
  to overlapping repository content
- **THEN** Repository Understanding SHALL use the override's Project
  boundary for that content rather than the build-system-detected
  boundary

#### Scenario: Build-system detection takes precedence over the unmanaged fallback
- **WHEN** a directory is claimed by build-system detection and has no
  explicit override
- **THEN** Repository Understanding SHALL NOT assign that directory to
  the unmanaged Project

#### Scenario: Conflicting overrides are reported, not merged
- **WHEN** two explicit overrides declare conflicting Project
  boundaries for overlapping repository content
- **THEN** Repository Understanding SHALL report the conflict via a
  discovery outcome of `failed` with a `conflicting-override` failure
  reason for the affected scope, and SHALL NOT produce a merged or
  arbitrarily chosen Project boundary for it

---

### Requirement: Unmanaged Pseudo-Project Handling
Files claimed by no detected Project and no explicit override SHALL be
represented as belonging to a single, reserved, repository-scoped
Project Evidence Item designated as unmanaged. Repository Understanding
SHALL NOT silently omit such files from evidence, and SHALL NOT merge
them into an unrelated detected Project.

#### Scenario: Unclaimed files appear under the unmanaged Project
- **WHEN** a file is not contained by any detected or
  override-declared Project
- **THEN** Repository Understanding SHALL represent that file as
  contained by the repository's reserved unmanaged Project Evidence
  Item

#### Scenario: Unmanaged coverage is queryable
- **WHEN** a consumer queries the Repository Evidence Model for the
  contents of the unmanaged Project
- **THEN** Repository Understanding SHALL make the full set of files not
  claimed by any detected or declared Project determinable from that
  query

---

### Requirement: Source-File Discovery and Language Identification
For every discovered source file, Repository Understanding SHALL
identify the file's language, primarily from its file extension, and
MAY use content inspection to disambiguate when the extension alone is
insufficient. Where content inspection is used, it SHALL be
deterministic and repeatable: identifying the same, unchanged file
content with the same Repository Understanding version SHALL yield the
same language identification result on every run. Repository
Understanding SHALL route each source file to the Language Analyzer
registered for its identified language.

#### Scenario: Language identified from file extension
- **WHEN** a source file has an extension unambiguously associated
  with one registered language
- **THEN** Repository Understanding SHALL route that file to the
  Language Analyzer registered for that language

#### Scenario: Content-based language identification is repeatable
- **WHEN** content inspection is used to disambiguate a file's
  language
- **THEN** identifying that same file's language in a subsequent
  discovery run, using the same Repository Understanding version
  against unchanged file content, SHALL yield the same result

#### Scenario: No analyzer registered for an identified language
- **WHEN** a source file's language is identified but no Language
  Analyzer is registered for it
- **THEN** Repository Understanding SHALL still produce a File Evidence
  Item for that file, with a discovery outcome of `failed` and a
  failure reason of `unsupported-language`, and SHALL NOT silently omit
  the file from evidence

---

### Requirement: Language Analyzer Contract
A Language Analyzer SHALL, for a given source file, produce zero or
more source-construct Evidence Items, a single discovery outcome status
for that file, a single extraction-method tag for that file, and MAY
produce an opaque bag of native evidence attributes for
analyzer-specific detail not otherwise captured. A Language Analyzer
SHALL NOT assign CSM provenance, CSM confidence, or any CSM vocabulary
classification to the Evidence Items it produces.

#### Scenario: Analyzer output includes required metadata
- **WHEN** a Language Analyzer processes a source file
- **THEN** its output SHALL include a discovery outcome status and an
  extraction-method tag for that file

#### Scenario: Analyzer preserves unmapped native detail
- **WHEN** a Language Analyzer encounters a native construct or
  attribute it does not have a dedicated Evidence attribute for
- **THEN** it MAY preserve that detail in the native evidence attribute
  bag rather than discarding it

---

### Requirement: Package Evidence and Containment
Where a language provides a native source-organization namespace
concept (e.g. a Java package, a Go package, a .NET namespace),
Repository Understanding SHALL produce a Package Evidence Item
representing that namespace and SHALL represent the containment
relationships of a Module containing its Packages and a Package
containing the source-construct Evidence Items declared within it, so
that the full containment chain Repository → Project → Module →
Package → source construct → method (where present) is discoverable
from evidence alone. A Package Evidence Item SHALL represent only the
native namespace name and its containment facts, and SHALL NOT carry
or imply any architectural grouping, component, or business meaning.

#### Scenario: Package containment discoverable from evidence
- **WHEN** a Language Analyzer discovers a source construct declared
  within a native package or namespace
- **THEN** Repository Understanding SHALL produce a Package Evidence
  Item for that namespace (if not already produced) and SHALL
  represent the source construct as contained by it

#### Scenario: Language without a package concept
- **WHEN** a language has no native package or namespace concept
- **THEN** Repository Understanding SHALL NOT fabricate a Package
  Evidence Item, and the resulting source-construct Evidence Item
  SHALL be represented as directly contained by its Module; its
  relationship to its File Evidence Item remains a reference/location
  relationship (see File Evidence and Kind-Specific Layering), not
  structural containment

#### Scenario: Package evidence carries no architectural meaning
- **WHEN** a Package Evidence Item is produced
- **THEN** it SHALL represent only the native namespace name and
  containment facts, and SHALL NOT be asserted as, or associated with,
  an Architecture Component or any other CSM-vocabulary grouping

---

### Requirement: Type-Level Minimum Capability
Every Language Analyzer SHALL be capable of producing Evidence Items
for top-level source constructs (types, or the language's closest
equivalent) for the language(s) it supports. Type-level construct
discovery SHALL be the minimum capability required for a Language
Analyzer to be considered valid.

#### Scenario: Minimal analyzer produces type-level evidence only
- **WHEN** a Language Analyzer supports only type-level extraction for
  its language
- **THEN** its output SHALL still be considered valid Repository
  Evidence, provided it produces Evidence Items for the file's
  top-level constructs

---

### Requirement: Optional Method-Level Representation
A Language Analyzer MAY produce Evidence Items for methods (or the
language's closest equivalent) nested within a source construct.
Method-level Evidence Items are OPTIONAL and their absence SHALL NOT
cause the containing source-construct Evidence Item, or the Repository
Evidence Model as a whole, to be considered invalid.

#### Scenario: Method-level evidence present
- **WHEN** a Language Analyzer supports method-level extraction for a
  given source construct
- **THEN** it SHALL produce Evidence Items for that construct's methods
  nested under the construct's Evidence Item

#### Scenario: Absence of method-level evidence is not an error
- **WHEN** a Language Analyzer does not support method-level extraction
  for a given source construct
- **THEN** the resulting Evidence Item SHALL be valid without any
  nested method-level Evidence Items

---

### Requirement: Build-System Detection
Repository Understanding SHALL detect build systems using a registry of
pluggable Build-System Detectors, each recognizing its own manifest
format and capable of extracting declared dependencies and any
sub-module structure the build system itself defines. Build-system
detection SHALL be a distinct extension point from Language Analyzers.

#### Scenario: Recognized manifest triggers build-system analysis
- **WHEN** a directory contains a manifest recognized by a registered
  Build-System Detector
- **THEN** Repository Understanding SHALL extract that manifest's
  declared dependencies and sub-module structure as Evidence

---

### Requirement: Dependency Evidence
Repository Understanding SHALL represent dependency facts using two
distinct Evidence kinds, which SHALL NOT be merged into a single
representation: manifest-declared dependency edges, extracted from
build-system analysis; and source-level reference edges, extracted
from source analysis (e.g. an import statement). Where a build
manifest declares a dependency's scope or kind (e.g. Maven's
`compile`, `provided`, or `test` scopes; npm's `dependencies`,
`devDependencies`, or `peerDependencies`; or an equivalent for another
build system), Repository Understanding SHALL record that scope or
kind string verbatim, in the build system's own native vocabulary, as
a manifest-declared dependency edge's attribute. Repository
Understanding SHALL NOT normalize, map, or collapse a build system's
native scope vocabulary into the Canonical Software Model's
compile-time/runtime/test-only dependency-kind classification, or into
any other CSM vocabulary; that semantic classification is the CSM
Builder's responsibility, not Repository Understanding's.

#### Scenario: Manifest-declared dependency retains its native scope string
- **WHEN** a build manifest declares a dependency with an explicit
  scope or kind string
- **THEN** the resulting manifest-declared dependency Evidence Item
  SHALL record that scope or kind string verbatim, in the build
  system's own native vocabulary

#### Scenario: No normalization into CSM dependency classification
- **WHEN** a manifest-declared dependency's native scope string is
  recorded as evidence
- **THEN** Repository Understanding SHALL NOT translate, map, or
  collapse that string into CSM's compile-time/runtime/test-only
  vocabulary or any other CSM classification

#### Scenario: Source-level and manifest-declared dependencies remain distinct
- **WHEN** both a manifest-declared dependency and a source-level
  reference exist between the same two Modules
- **THEN** Repository Understanding SHALL represent them as two
  distinct Evidence Items rather than merging them into one

---

### Requirement: Deterministic API Relationship Discovery
Repository Understanding SHALL discover API contract declarations only
from mechanically explicit, deterministic sources (e.g. an OpenAPI or
Protocol Buffers definition file, or an explicit framework annotation
or declaration marking an endpoint or exported interface), recording a
lightweight structural summary (such as operation names, paths, or
verbs) as Evidence attributes alongside a reference to the full
contract definition's location. Repository Understanding SHALL NOT
infer that a construct is part of an API surface from naming
conventions, structural resemblance, or any other non-explicit signal.

#### Scenario: Explicit contract declaration produces API evidence
- **WHEN** a source file contains an explicit, framework-recognized
  declaration of an API endpoint or an OpenAPI/Protocol-Buffers
  definition file is discovered
- **THEN** Repository Understanding SHALL produce an API contract
  Evidence Item referencing that declaration's location and a
  structural summary of its declared operations

#### Scenario: No inference from naming or structural resemblance
- **WHEN** a construct's name or structure resembles a common API
  pattern (e.g. a class named `*Controller`) without any explicit
  contract declaration
- **THEN** Repository Understanding SHALL NOT produce an API contract
  Evidence Item for that construct

---

### Requirement: Configuration Recognizer Mechanism
Repository Understanding SHALL detect configuration files and
source-level configuration/secret-reference idioms using a registry of
pluggable Configuration Recognizers, structurally analogous to the
Language Analyzer Registry and the Build-System Detector Registry.
Each Configuration Recognizer recognizes its own configuration file
pattern (e.g. a filename, extension, or path convention) or its own
source-level configuration/secret access idiom (e.g. an
environment-variable accessor, or a framework's configuration-injection
annotation). The Configuration Recognizer Registry SHALL be a distinct
extension point from the Language Analyzer Registry and the
Build-System Detector Registry. A Configuration Recognizer SHALL NOT
capture, record, or otherwise retain the value of any configuration
entry or secret it recognizes — only the fact that a configuration
file or reference exists.

#### Scenario: Configuration file recognized via a registered pattern
- **WHEN** a file matches a pattern registered with a Configuration
  Recognizer
- **THEN** Repository Understanding SHALL produce a Configuration File
  Evidence Item for it

#### Scenario: No recognizer registered for a configuration-like file
- **WHEN** a file may hold configuration but matches no pattern
  registered with any Configuration Recognizer
- **THEN** Repository Understanding SHALL NOT produce a Configuration
  File Evidence Item for it, and the file SHALL remain represented
  only by its File Evidence Item (see File Evidence and Kind-Specific
  Layering)

#### Scenario: Recognizer never captures values
- **WHEN** a Configuration Recognizer matches a configuration file or
  a source-level configuration/secret reference
- **THEN** it SHALL record only the fact of that match and SHALL NOT
  record the underlying value

---

### Requirement: Configuration Reference Discovery
Repository Understanding SHALL record the existence of configuration
files matched by a registered Configuration Recognizer and, where
source analysis detects a syntactic reference to a configuration key
or secret-like identifier matched by a registered Configuration
Recognizer (see Configuration Recognizer Mechanism), SHALL record that
a reference exists. Repository Understanding SHALL NOT record, store,
or otherwise capture the value of any configuration entry or secret.

#### Scenario: Configuration file existence recorded
- **WHEN** a configuration file matched by a registered Configuration
  Recognizer is discovered
- **THEN** Repository Understanding SHALL produce a Configuration File
  Evidence Item for it without capturing its contents' values

#### Scenario: Configuration reference recorded without its value
- **WHEN** source code contains a syntactic reference to a
  configuration key or secret-like identifier matched by a registered
  Configuration Recognizer
- **THEN** Repository Understanding SHALL produce a Configuration
  Reference Evidence Item recording that the reference exists, and
  SHALL NOT record the referenced value

---

### Requirement: Incremental Analysis Scope
Repository Understanding SHALL support scoping a discovery run to: the
Evidence Items directly derived from changed content, and any other
Evidence Item whose own recorded attributes are directly computed from
that changed content (e.g., a Module's manifest-declared dependency
edges when its build manifest changes, or a Package's containment set
when a contained source construct's file is added, removed, or
relocated) — rather than requiring a full re-discovery of an entire
repository on every run. An Evidence Item with no such direct
dependency on changed content SHALL NOT be required to be reprocessed.

#### Scenario: Unaffected evidence is not reprocessed
- **WHEN** a discovery run is performed on a repository where only a
  subset of files have changed since the prior run
- **THEN** Repository Understanding SHALL be able to complete that run
  by processing only the changed content and the Evidence Items whose
  attributes are directly computed from it, without reprocessing
  Evidence Items with no such direct dependency on the changed content

---

### Requirement: Change Detection Classification
For each discovery run after an initial run, Repository Understanding
SHALL classify each Evidence Item's change status as exactly one of:
`ADDED`, `UNCHANGED`, `MODIFIED`, or `REMOVED`, based on a comparison
against retained state from the prior run. This change-status
vocabulary is distinct from, and governs, the lifecycle-state
vocabulary defined in Evidence Lifecycle States: an `ADDED`,
`UNCHANGED`, or `MODIFIED` classification SHALL result in the Evidence
Item's lifecycle state being (or remaining) `PRESENT`; a `REMOVED`
classification SHALL result in the Evidence Item's lifecycle state
becoming `TOMBSTONED`. These two vocabularies SHALL NOT be used
interchangeably: change status describes what happened in a given run;
lifecycle state describes the Evidence Item's current standing.

#### Scenario: New evidence yields an ADDED classification
- **WHEN** discovery finds a file or construct with no corresponding
  Evidence Item in the retained state from the prior run
- **THEN** the resulting Evidence Item(s) SHALL be classified as
  `ADDED` for that run, and SHALL enter the `PRESENT` lifecycle state

#### Scenario: Modified file yields a MODIFIED classification
- **WHEN** a previously discovered file's content differs from its
  previously recorded state
- **THEN** its corresponding Evidence Item(s) SHALL be classified as
  `MODIFIED` for that run, and SHALL remain in the `PRESENT` lifecycle
  state

#### Scenario: Unchanged file yields an UNCHANGED classification
- **WHEN** a previously discovered file's content is identical to its
  previously recorded state
- **THEN** its corresponding Evidence Item(s) SHALL be classified as
  `UNCHANGED` for that run, and SHALL remain in the `PRESENT` lifecycle
  state

#### Scenario: Removed evidence yields a REMOVED classification
- **WHEN** a previously discovered file's or construct's corresponding
  Evidence Item is no longer discoverable in the current run
- **THEN** that Evidence Item SHALL be classified as `REMOVED` for that
  run, and SHALL transition to the `TOMBSTONED` lifecycle state

---

### Requirement: VCS Rename Handling
Where the repository's version control system provides its own
deterministic rename or move detection, Repository Understanding SHALL
use that mechanism to preserve an Evidence Item's identity across a
detected rename or move, and SHALL record that identity continuity was
established via VCS-provided rename detection. Where no such mechanism
is available, a file that disappears and a new file that appears SHALL
be classified as `REMOVED` and `ADDED` respectively (see Change
Detection Classification), rather than inferred to be a rename.

#### Scenario: VCS-detected rename preserves identity
- **WHEN** the version control system reports a file as renamed or
  moved between two revisions
- **THEN** Repository Understanding SHALL preserve the corresponding
  Evidence Item's identity across that rename and SHALL record that the
  continuity was established via VCS rename detection

#### Scenario: No rename inference without VCS support
- **WHEN** no version-control rename-detection mechanism is available
  and a previously discovered file disappears while a different new
  file appears
- **THEN** Repository Understanding SHALL classify this as `REMOVED`
  and `ADDED` respectively, and SHALL NOT infer that the two are the
  same renamed item based on content similarity

---

### Requirement: Partial-Analysis Outcome Reporting
Every discovery attempt against an Evidence Item SHALL record a
discovery outcome status of exactly one of: `complete`, `partial`, or
`failed`. Repository Understanding SHALL NOT silently omit an item that
could not be fully analyzed, and SHALL NOT represent a partial or
best-effort extraction as though it were `complete`.

#### Scenario: Failed analysis is recorded, not omitted
- **WHEN** a file cannot be analyzed at all (e.g. due to a parse error)
- **THEN** Repository Understanding SHALL produce an Evidence Item for
  that file with a discovery outcome of `failed`, rather than omitting
  the file from evidence

#### Scenario: Partial extraction is not represented as complete
- **WHEN** an analyzer extracts only some of a file's structure before
  encountering an error or limitation
- **THEN** the resulting Evidence Item(s) SHALL carry a discovery
  outcome of `partial`, not `complete`

---

### Requirement: Failure Reason Taxonomy
A discovery outcome of `partial` or `failed` SHALL be accompanied by a
failure reason drawn from a shared, versioned taxonomy that includes,
at minimum: `parse-error`, `unsupported-construct`,
`unsupported-language`, `size-limit-exceeded`, `timeout`,
`binary-or-non-text`, `excluded-by-configuration`, and
`conflicting-override` (see Project Boundary Precedence). This
taxonomy is core but non-exhaustive: additional failure reasons MAY be
introduced in future versioned specification changes without
invalidating existing Evidence.

#### Scenario: Failure reason drawn from the shared taxonomy
- **WHEN** a discovery outcome of `partial` or `failed` is recorded
- **THEN** it SHALL include a failure reason that is a member of the
  shared failure-reason taxonomy

#### Scenario: Taxonomy extension does not invalidate prior evidence
- **WHEN** a future specification change adds a new failure reason to
  the taxonomy
- **THEN** Evidence Items using previously defined failure reasons
  SHALL remain valid

---

### Requirement: Extraction Method Tagging
Every Evidence Item produced from source or build-system analysis
SHALL carry an extraction-method tag drawn from a shared, versioned
taxonomy that includes, at minimum: `full-parse`, `heuristic-scan`,
`manifest-declared`, and `externally-declared`. The extraction-method
tag SHALL describe how the evidence was obtained and SHALL NOT be
used, or usable, as a substitute for CSM confidence.

#### Scenario: Extraction method recorded per item
- **WHEN** an Evidence Item is produced by any discovery mechanism
- **THEN** it SHALL carry exactly one extraction-method tag from the
  shared taxonomy

#### Scenario: Extraction method is not a confidence signal
- **WHEN** an Evidence Item's extraction-method tag is inspected
- **THEN** it SHALL be understood only as a description of the
  extraction technique used, and SHALL NOT be interpreted as a
  trust or confidence rating

---

### Requirement: Evidence Lifecycle States
An Evidence Item's identity SHALL persist through a lifecycle of
exactly one of: `PRESENT`, `TOMBSTONED`, or `PURGED`. This
lifecycle-state vocabulary is distinct from the per-run change-status
vocabulary defined in Change Detection Classification (`ADDED`,
`UNCHANGED`, `MODIFIED`, `REMOVED`); the two SHALL NOT be used
interchangeably. An Evidence Item enters or remains in the `PRESENT`
state when classified as `ADDED`, `UNCHANGED`, or `MODIFIED` in a given
run, and transitions to `TOMBSTONED` when classified as `REMOVED`. A
`TOMBSTONED` Evidence Item SHALL remain retrievable for at least one
subsequent discovery run after entering that state, rather than being
purged immediately, and SHALL become eligible for the `PURGED` state
only after that retention period has elapsed (the exact retention
period is an implementation detail, deferred).

#### Scenario: Change classification determines lifecycle state
- **WHEN** an Evidence Item is classified as `ADDED`, `UNCHANGED`, or
  `MODIFIED` in a discovery run (see Change Detection Classification)
- **THEN** its lifecycle state SHALL be `PRESENT` following that run

#### Scenario: Removed evidence becomes TOMBSTONED, not purged immediately
- **WHEN** an Evidence Item's underlying file or construct is removed
  from the repository and classified `REMOVED` for that run
- **THEN** that Evidence Item SHALL transition to the `TOMBSTONED`
  lifecycle state and SHALL remain retrievable for at least the next
  discovery run before becoming eligible for the `PURGED` state

---

### Requirement: Analyzer Version Reprocessing
Repository Understanding SHALL classify an Evidence Item as eligible
for reprocessing when the Language Analyzer or Build-System Detector
version responsible for it differs from the version recorded at its
last discovery, even when the underlying file content has not changed.
Where reprocessing is triggered solely by an analyzer-version change,
Repository Understanding SHALL classify the resulting Evidence Item's
change status as `MODIFIED` (see Change Detection Classification) for
that run, rather than `UNCHANGED`.

#### Scenario: Analyzer upgrade triggers reprocessing
- **WHEN** a Language Analyzer's version changes between two discovery
  runs and a file it previously processed has not itself changed
- **THEN** Repository Understanding SHALL reprocess that file using the
  new analyzer version and SHALL classify the resulting Evidence
  Item(s) as `MODIFIED` rather than `UNCHANGED` for that run

---

### Requirement: Evidence Traceability
Every Evidence Item SHALL retain its current source location (file
path and, where applicable, a position range) in addition to, and
separately from, its stable identity, such that a consumer can
determine both what an Evidence Item is (identity) and where it
currently resides (location).

#### Scenario: Location and identity are independently retrievable
- **WHEN** a consumer inspects an Evidence Item
- **THEN** it SHALL be able to retrieve both the item's stable identity
  and its current source location as distinct attributes

---

### Requirement: Multi-Language Repository Handling
Repository Understanding SHALL support a single Module or unmanaged
scope containing source files in more than one language, routing each
file independently to its own applicable Language Analyzer. Repository
Understanding SHALL NOT require or assert a single primary language for
a Module or repository.

#### Scenario: Mixed-language Module produces evidence per file
- **WHEN** a Module contains source files in more than one language
- **THEN** Repository Understanding SHALL produce Evidence Items for
  each file using that file's own applicable Language Analyzer

#### Scenario: No primary-language classification is asserted
- **WHEN** a Module contains files in multiple languages
- **THEN** Repository Understanding SHALL NOT designate any one
  language as that Module's primary language

---

### Requirement: Large-Repository Scalability
Repository Understanding's discovery process SHALL treat each file or
module as an independently processable unit of work. Evidence Items
SHALL be made available to a consumer incrementally, as they are
produced, and SHALL NOT require full-repository discovery to complete
before any Evidence Item becomes available.

#### Scenario: Evidence available before full-repository discovery completes
- **WHEN** discovery is performed against a large repository
- **THEN** Evidence Items for already-processed files SHALL be
  available to a consumer before discovery of the entire repository has
  completed

---

### Requirement: Extension Mechanism for Languages, Build Systems, and Configuration Recognition
Support for an additional programming language, build system, or
configuration file pattern/reference idiom SHALL be introduced only by
registering a new Language Analyzer, Build-System Detector, or
Configuration Recognizer (respectively) against Repository
Understanding's existing contracts. Introducing such support SHALL NOT
require a change to the Repository Evidence Model's core structure
(its identity scheme, outcome-status vocabulary, or extraction-method
vocabulary), regardless of which of the three registries the new
implementation is added to.

#### Scenario: New language added without core model changes
- **WHEN** a new Language Analyzer is registered for a previously
  unsupported language
- **THEN** the Repository Evidence Model's identity scheme,
  outcome-status vocabulary, and extraction-method vocabulary SHALL
  remain unchanged

#### Scenario: New build system added without core model changes
- **WHEN** a new Build-System Detector is registered for a previously
  unsupported build system
- **THEN** the Repository Evidence Model's identity scheme,
  outcome-status vocabulary, and extraction-method vocabulary SHALL
  remain unchanged

#### Scenario: New configuration recognizer added without core model changes
- **WHEN** a new Configuration Recognizer is registered for a
  previously unrecognized configuration file pattern or
  configuration/secret reference idiom
- **THEN** the Repository Evidence Model's identity scheme,
  outcome-status vocabulary, and extraction-method vocabulary SHALL
  remain unchanged

---

### Requirement: Evidence-to-CSM-Builder Interface Boundary
Repository Understanding SHALL expose the Repository Evidence Model as
the sole interface a future CSM Builder consumes, and SHALL have no
outbound dependency on the Canonical Software Model, its vocabulary, or
any CSM Builder implementation. Repository Understanding SHALL NOT
construct, validate, or version a CSM snapshot.

#### Scenario: Repository Understanding operates without a CSM Builder
- **WHEN** no CSM Builder implementation exists
- **THEN** Repository Understanding SHALL still be able to complete
  discovery and produce a valid Repository Evidence Model

#### Scenario: No CSM snapshot construction performed
- **WHEN** Repository Understanding completes a discovery run
- **THEN** it SHALL NOT produce, validate, or version anything
  represented as a CSM snapshot

---

### Requirement: Exclusion of CSM Interpretation and AI Reasoning
Repository Understanding SHALL NOT assign a CSM provenance category
(observed, declared, or inferred) to any Evidence Item. Repository
Understanding SHALL NOT assign a CSM confidence level to any Evidence
Item. Repository Understanding SHALL NOT perform architectural
interpretation (e.g., classifying a package as an Architecture
Component) or infer business meaning (e.g., mapping a Module to a
business capability). Repository Understanding SHALL NOT use AI
reasoning, heuristic inference, or probabilistic judgment to produce
any Evidence Item — every Evidence Item SHALL be the mechanical result
of discovery, parsing, or explicit declaration.

#### Scenario: No provenance category assigned by Repository Understanding
- **WHEN** any Evidence Item is produced
- **THEN** it SHALL NOT carry an `observed`, `declared`, or `inferred`
  CSM provenance classification

#### Scenario: No confidence level assigned by Repository Understanding
- **WHEN** any Evidence Item is produced
- **THEN** it SHALL NOT carry a HIGH/MEDIUM/LOW or any other CSM
  confidence level

#### Scenario: No architectural or business interpretation performed
- **WHEN** Repository Understanding discovers a package, module, or
  naming pattern that could plausibly suggest an architectural role or
  business meaning
- **THEN** it SHALL record only the mechanical fact discovered (e.g.
  the package's name and containment), and SHALL NOT produce any
  Evidence asserting an architectural role or business meaning for it

#### Scenario: No AI or heuristic judgment used to produce evidence
- **WHEN** an Evidence Item is produced by any discovery mechanism
- **THEN** it SHALL be traceable to a mechanical parsing, discovery, or
  explicit-declaration step, and SHALL NOT be the product of AI
  reasoning or probabilistic inference
