# Requirement/Scenario Traceability Matrix

tasks.md 13.1: every requirement and scenario in
`openspec/changes/define-architecture-compliance-agent/specs/
architecture-compliance-agent/spec.md` (14 requirements, ~25
scenarios), mapped to the automated test(s) exercising it. Test
references are `ClassName.methodName`; unqualified class names live in
`aip-rules/src/test/java/aip/rules/boundarycompliance/...` or
`aip-ai/src/test/java/aip/ai/architecturecompliance/...` as noted.

## Architecture Compliance Agent Introduces No New Artifact or Mechanism

| Scenario | Test(s) |
|---|---|
| No new artifact type is introduced | `NoNewMechanismTest.registrationRequiresNoCoreMechanismChangeAndProducesAnOrdinaryValidatableResult` (aip-rules); `ArchitectureComplianceAgentEndToEndTest` (aip-ai) — every output is an ordinary `RuleEvaluationResult`/`Finding`/`Recommendation`, no new class implementing a new artifact contract exists anywhere in either subpackage. |
| No new identity, traceability, or validation mechanism is introduced | `NoNewMechanismTest.registrationRequiresNoCoreMechanismChangeAndProducesAnOrdinaryValidatableResult` (validates through `RuleEvaluationResultValidator` unmodified); `ArchitectureComplianceAgentEndToEndTest` (validates through `RecommendationValidator` unmodified) |
| No new module or shared contract is introduced | Structural: `BoundaryComplianceRuleType`/`BoundaryComplianceDiagnostic`/`BoundaryConstraintKind` live in `aip-rules`; `ArchitectureComplianceAgent`/`ArchitectureComplianceRecommendationContent` live in `aip-ai`. Root `pom.xml` gains no new `<module>` entry. |

## Boundary-Compliance Rule Type Registration

| Scenario | Test(s) |
|---|---|
| Boundary-compliance Rule Type registers without core mechanism changes | `NoNewMechanismTest.registrationRequiresNoCoreMechanismChangeAndProducesAnOrdinaryValidatableResult` |

## Boundary-Compliance Rule Scope Declaration

| Scenario | Test(s) |
|---|---|
| Rule Scope declares dependency and boundary/constraint relationship kinds | `BoundaryComplianceRuleType.scope()`'s own declaration (inspected directly; structural) |
| Rule Scope anchors on Architecture Component | `BoundaryComplianceRuleTypeTest` (every test anchors evaluation per-Component via `resultForAnchor`) |

## Boundary-Compliance Rule Type Declares No Analyzer Inputs

| Scenario | Test(s) |
|---|---|
| No Analyzer input is declared | `BoundaryComplianceRuleType.requiredAnalyzerIdentifiers()` returns `Set.of()` (structural) |
| Relationship content is read directly from the Analysis View | `BoundaryComplianceRuleTypeTest` (all tests evaluate via `AnalysisView` alone, no `AnalysisResult` ever consumed) |

## Boundary-Compliance Condition — Must-Not-Depend-On Violations

| Scenario | Test(s) |
|---|---|
| No violating dependency satisfies the condition | `BoundaryComplianceRuleTypeTest.noViolatingDependencySatisfiesTheConditionAndProducesPass` |
| A violating dependency does not satisfy the condition | `BoundaryComplianceRuleTypeTest.violatingDependencyProducesFail` |

## Must-Only-Communicate-Via Constraints Are Out of Scope

| Scenario | Test(s) |
|---|---|
| Non-"must not depend on" constraints are not evaluated | `BoundaryComplianceRuleTypeTest.nonMustNotDependOnConstraintIsNeverEvaluated` |

## Kind-Based Applicability for Boundary-Compliance Checking

| Scenario | Test(s) |
|---|---|
| Component with no relevant relationships is not evaluated | `BoundaryComplianceRuleTypeTest.componentWithNoRelevantRelationshipsProducesNoResult` |

## NOT_APPLICABLE Semantics for Boundary-Compliance Checking

| Scenario | Test(s) |
|---|---|
| Dependencies with no boundary to check against produce NOT_APPLICABLE | `BoundaryComplianceRuleTypeTest.dependencyPresentWithNoApplicableConstraintProducesNotApplicable` |
| A boundary with no dependencies at all is compliant, not inapplicable | `BoundaryComplianceRuleTypeTest.noViolatingDependencySatisfiesTheConditionAndProducesPass` |

## Boundary-Compliance Outcome Determination

| Scenario | Test(s) |
|---|---|
| A violation produces FAIL | `BoundaryComplianceRuleTypeTest.violatingDependencyProducesFail` |
| No violation produces PASS | `BoundaryComplianceRuleTypeTest.noViolatingDependencySatisfiesTheConditionAndProducesPass` |
| No dependencies at all still produces PASS, not NOT_APPLICABLE | `BoundaryComplianceRuleTypeTest.noViolatingDependencySatisfiesTheConditionAndProducesPass` |

## Boundary-Compliance Category and Severity

| Scenario | Test(s) |
|---|---|
| Category and Severity are fixed across all instances | `BoundaryComplianceRuleTypeTest.categoryAndSeverityAreFixedAcrossDifferentComponents` |

## Architecture Compliance Agent Registration

| Scenario | Test(s) |
|---|---|
| Architecture Compliance Agent registers without core mechanism changes | `ArchitectureComplianceAgentTest.registersWithAgentFrameworksExistingRegistry` |

## Architecture Compliance Agent Consumption Boundary

| Scenario | Test(s) |
|---|---|
| Agent consumes only a Finding | `ArchitectureComplianceAgentTest.agentReadsNoContentBeyondTheGivenFinding`; architecturally guaranteed by `ArchitectureComplianceAgent.invoke`'s own signature. |

## Architecture Compliance Agent Recommendation Content

| Scenario | Test(s) |
|---|---|
| Recommendation identifies the violation | `ArchitectureComplianceAgentTest.recommendationContentIdentifiesTheComponentAndDependencyTarget`; `ArchitectureComplianceAgentEndToEndTest` |
| Recommendation includes remediation guidance | `ArchitectureComplianceAgentTest.recommendationIncludesRemediationGuidance` |

## Architecture Compliance Agent Recommendation Excludes Code and Proposed Changes

| Scenario | Test(s) |
|---|---|
| No code or diff is produced | `ArchitectureComplianceAgentTest.recommendationContentContainsNoCodeOrDiffContent`; structurally, `ArchitectureComplianceRecommendationContent`'s own field list has no code/diff-shaped field. |

## Boundary Provenance Is Propagated, Not Differentiated

| Scenario | Test(s) |
|---|---|
| Outcome and Severity are unaffected by boundary provenance | `BoundaryComplianceRuleTypeTest.declaredAndInferredBoundaryViolationsAreDeterminedIdentically` |
| Boundary provenance remains traceable | Architecturally guaranteed: the violated `BOUNDARY_CONSTRAINT` relationship's own `ProvenanceRecord` (declared/inferred) is CSM content unmodified by this Rule Type; nothing in `BoundaryComplianceDiagnostic` strips or overrides it. |

## Compliance Scope Limited to Boundary Violations

| Scenario | Test(s) |
|---|---|
| Only boundary violations are within scope | Structural: `BoundaryComplianceRuleType`'s Rule Scope reads only `DEPENDENCY`/`BOUNDARY_CONSTRAINT` relationships; `ArchitectureComplianceAgent` produces only `ArchitectureComplianceRecommendationContent` — neither type has any code path touching a design, security, risk, or other compliance concern. |

## Binding Decisions (this change's own `design.md`)

| Decision | Verification |
|---|---|
| 1. New subpackages, no new module | `aip.rules.boundarycompliance`, `aip.ai.architecturecompliance` subpackage placement (structural); root `pom.xml` unchanged module list |
| 2. `constraint-kind` `NativeAttributes` convention | `BoundaryComplianceRuleTypeTest.nonMustNotDependOnConstraintIsNeverEvaluated` |
| 3. `BoundaryComplianceRuleType` concrete shape | `BoundaryComplianceRuleTypeTest` (all tests) |
| 4. `PASS`/`NOT_APPLICABLE`/`FAIL` payload shape | `BoundaryComplianceRuleTypeTest.dependencyPresentWithNoApplicableConstraintProducesNotApplicable` (NOT_APPLICABLE payload never interpreted as FindingMetadata); `.violatingDependencyProducesFail` (FAIL payload is `BoundaryComplianceDiagnostic`) |
| 5. `BoundaryComplianceDiagnostic` dynamic Description/Impact | `BoundaryComplianceRuleTypeTest.categoryAndSeverityAreFixedAcrossDifferentComponents`; `ArchitectureComplianceAgentTest.recommendationContentIdentifiesTheComponentAndDependencyTarget` |
| 6. `ArchitectureComplianceAgent` deterministic, template-based realization | `ArchitectureComplianceAgentEndToEndTest`; `GenerationProvenance.modelProviderIdentifier() == "deterministic-template"` |
| 7. `ArchitectureComplianceRecommendationContent` shape | `ArchitectureComplianceAgentTest.recommendationContentIdentifiesTheComponentAndDependencyTarget`, `.recommendationContentContainsNoCodeOrDiffContent` |
