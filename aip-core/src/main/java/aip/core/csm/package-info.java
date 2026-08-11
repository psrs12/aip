/**
 * The Canonical Software Model (CSM) domain model.
 *
 * <p>Language-independent structural and relational representation of
 * a software system, per {@code openspec/specs/canonical-software-model/spec.md}.
 * Every type in this package models vocabulary that specification
 * already defines — entity kinds, relationship types, provenance,
 * and confidence. This package SHALL NOT grow a new entity kind,
 * relationship type, provenance category, or confidence
 * representation without a corresponding, deliberate, versioned
 * change to that specification (see
 * {@code CSM Versioning and Evolution} and
 * {@code Exclusion of New CSM Vocabulary} in
 * {@code openspec/specs/csm-builder/spec.md}).
 *
 * <p>Out of scope for this package, deliberately: Architectural
 * Boundary and Business Context (Business Capability, Domain,
 * Ownership, Criticality) are not among the core structural entity
 * kinds this specification enumerates (see
 * {@code CSM Conceptual Vocabulary}); modeling them is left to a
 * future, deliberate extension rather than being guessed at here.
 */
package aip.core.csm;
