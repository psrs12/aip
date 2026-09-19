## MODIFIED Requirements

### Requirement: Analysis Result Validation Before Publication
The Analysis Framework SHALL validate a constructed Analysis Result
before it is considered usable output, checking at minimum: every CSM
element or Subject identity the Result references SHALL exist within
the source CSM Snapshot it was computed against; the Result's declared
producing Analyzer and version SHALL correspond to a currently
registered Analyzer; and the Result's content SHALL remain within its
Analyzer's declared Analysis Scope. An Analysis Result that fails this
validation SHALL NOT be published as usable output, and SHALL NOT be
written to an `AnalysisResultStore` as a valid Analysis Result. The
`AnalysisResultPublisher` SHALL publish, and write to the
`AnalysisResultStore`, only an Analysis Result that has successfully
passed the `AnalysisResultValidator`; validation SHALL occur, and
SHALL pass, before any write to the `AnalysisResultStore` is
attempted for that Result.

#### Scenario: Valid Result is published as usable output
- **WHEN** a constructed Analysis Result satisfies every validation
  check
- **THEN** it SHALL be published as usable output, and it SHALL be
  written to the `AnalysisResultStore`

#### Scenario: Result referencing a CSM identity absent from its source snapshot is rejected
- **WHEN** a constructed Analysis Result references a CSM element or
  Subject identity that does not exist within the CSM Snapshot it
  claims to be computed against
- **THEN** it SHALL fail validation, SHALL NOT be published, and SHALL
  NOT be written to the `AnalysisResultStore`

#### Scenario: Result from an unregistered Analyzer/version is rejected
- **WHEN** a constructed Analysis Result declares a producing Analyzer
  identifier and version that does not correspond to a currently
  registered Analyzer
- **THEN** it SHALL fail validation, SHALL NOT be published, and SHALL
  NOT be written to the `AnalysisResultStore`

#### Scenario: Result exceeding its Analyzer's declared scope is rejected
- **WHEN** a constructed Analysis Result's content extends beyond its
  producing Analyzer's declared Analysis Scope
- **THEN** it SHALL fail validation, SHALL NOT be published, and SHALL
  NOT be written to the `AnalysisResultStore`

#### Scenario: AnalysisResultStore never holds a Result that failed validation
- **WHEN** the `AnalysisResultStore` is queried for any Analysis Result
  it holds, by identity or by listing
- **THEN** every Analysis Result returned SHALL have previously passed
  the `AnalysisResultValidator`; no Analysis Result that failed
  validation SHALL ever be present in the `AnalysisResultStore`

## ADDED Requirements

### Requirement: Incremental Analysis Is Architecturally Supported
The Analysis Framework's Analysis Scope and Analysis Result identity
mechanisms SHALL be sufficient to determine, given two CSM Snapshots of
the same repository, which registered Analyzers may need re-execution
because CSM content within their declared Analysis Scope differs
between those two snapshots. This determination SHALL be made using
only each Analyzer's declared Analysis Scope (see Analysis Scope
Declaration) and the CSM content of the two snapshots being compared —
the Analysis Framework SHALL NOT require a declared dependency graph
between Analyzers, and SHALL NOT require a sophisticated, fine-grained,
cross-Analyzer invalidation mechanism to satisfy this requirement in
this version. A coarse determination — an Analyzer may need
re-execution whenever any CSM content within its declared Analysis
Scope differs between the two snapshots being compared — is sufficient
for v1.

#### Scenario: CSM Snapshots of the same repository can be compared for scope-relevant differences
- **WHEN** two CSM Snapshots of the same repository, taken at different
  points in construction history, are both available
- **THEN** it SHALL be possible to determine, for a given registered
  Analyzer, whether CSM content within that Analyzer's declared
  Analysis Scope differs between the two snapshots

#### Scenario: An Analyzer is identified as a re-execution candidate when scope-relevant content changed
- **WHEN** CSM content within a registered Analyzer's declared Analysis
  Scope differs between an earlier CSM Snapshot and a later CSM
  Snapshot of the same repository
- **THEN** that Analyzer SHALL be identifiable as a candidate for
  re-execution against the later snapshot

#### Scenario: An Analyzer is not identified as a re-execution candidate when its scope is unaffected
- **WHEN** no CSM content within a registered Analyzer's declared
  Analysis Scope differs between an earlier CSM Snapshot and a later
  CSM Snapshot of the same repository, even though other CSM content
  outside that Analyzer's declared Analysis Scope does differ
- **THEN** that Analyzer SHALL NOT be identified as a re-execution
  candidate on the basis of that unrelated, out-of-scope difference

#### Scenario: A cross-Analyzer dependency graph is not required
- **WHEN** the Analysis Framework determines which Analyzers may need
  re-execution between two CSM Snapshots
- **THEN** it SHALL do so without requiring any declared dependency, or
  execution-order relationship, between two or more Analyzers

#### Scenario: Sophisticated cross-Analyzer invalidation is not required
- **WHEN** the Analysis Framework determines which Analyzers may need
  re-execution between two CSM Snapshots
- **THEN** it SHALL be permitted to treat any scope-relevant CSM
  content difference as sufficient grounds for re-execution, without
  being required to compute a more precise, fine-grained invalidation
  determination within that scope
