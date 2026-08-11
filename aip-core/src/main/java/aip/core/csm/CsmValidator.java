package aip.core.csm;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Applies the Canonical Software Model's own validation expectations to
 * a set of constructed CSM elements and relationships, per {@code CSM
 * Validation Expectations}: "A CSM, or any element within it, SHALL be
 * considered invalid unless it satisfies, at minimum: ..."
 *
 * <p>Most of that requirement's checklist is already unconditionally
 * guaranteed by this package's own types and is therefore not
 * re-checked here as live logic (re-affirmed instead as documentation,
 * so the full checklist stays visible in one place):
 *
 * <ul>
 *   <li>"every element belongs to a defined entity kind or relationship
 *       type from the core vocabulary" — {@link CsmElement} is {@code
 *       sealed} to exactly the archived vocabulary's eight
 *       implementations, and {@link CsmEntityKind}/{@link
 *       CsmRelationshipType} are closed enums; no other value is
 *       constructible.
 *   <li>"every element carries a valid, structured provenance record"
 *       — every {@link CsmElement}/{@link CsmRelationship} constructor
 *       requires a non-null {@link ProvenanceRecord}, whose own
 *       constructor requires a non-blank source reference.
 *   <li>"every element classified as inferred carries a confidence
 *       level of HIGH, MEDIUM, or LOW" — {@link ProvenanceRecord}'s own
 *       constructor rejects an {@code INFERRED} record without a
 *       {@link Confidence}, and {@link Confidence} is a closed enum of
 *       exactly those three values.
 *   <li>"every element derived from evidence retains a traceable
 *       reference to that evidence" — the same non-blank source
 *       reference above; CSM Builder's own {@code
 *       Evidence Traceability Preservation} guarantee (see {@code
 *       aip.csmbuilder.provenance.ProvenanceGuard}) is what makes that
 *       reference actually trace to real Evidence, which is a
 *       construction-time concern this snapshot-level validator cannot
 *       re-derive without the original Repository Evidence Model.
 *   <li>"the absence of Method-level elements SHALL NOT, by itself, be
 *       treated as invalid" and "a subject's effective knowledge being
 *       marked CONFLICTED SHALL NOT, by itself, be treated as invalid"
 *       — both are non-requirements ("SHALL NOT invalidate"); satisfied
 *       by this validator simply never checking for either condition.
 * </ul>
 *
 * <p>The one expectation this validator actively checks: "every
 * Architecture Component and Architectural Boundary carries a
 * provenance classification of declared or inferred (never observed)."
 * {@link ArchitectureComponentElement}'s own constructor already
 * enforces this unconditionally for elements — an
 * {@code ARCHITECTURE_COMPONENT} with {@code OBSERVED} provenance
 * cannot be constructed in the first place, so this validator does not
 * re-check elements for it. Nothing enforces the relationship-side
 * equivalent, though: a {@link CsmRelationshipType#BOUNDARY_CONSTRAINT}
 * relationship (the CSM's representation of an Architectural Boundary,
 * per {@code Architectural Boundary Representation}) has no such
 * constructor guard on {@link CsmRelationship} — that is the genuine
 * gap this validator closes.
 */
public final class CsmValidator {

  private CsmValidator() {}

  public static ValidationResult validate(List<CsmElement> elements, List<CsmRelationship> relationships) {
    Objects.requireNonNull(elements, "elements");
    Objects.requireNonNull(relationships, "relationships");

    List<String> violations = new ArrayList<>();
    for (CsmRelationship relationship : relationships) {
      if (relationship.type() == CsmRelationshipType.BOUNDARY_CONSTRAINT
          && relationship.provenance().category() == ProvenanceCategory.OBSERVED) {
        violations.add(
            "Architectural Boundary relationship " + relationship.id() + " carries OBSERVED"
                + " provenance, which SHALL NOT occur (see 'Architectural Boundary Representation')");
      }
    }

    return violations.isEmpty() ? ValidationResult.success() : ValidationResult.failure(violations);
  }
}
