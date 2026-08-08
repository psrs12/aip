## 1. Specification Consistency Review

- [x] 1.1 Review `proposal.md`, `design.md`, `explore.md`, and `spec.md`
      together for terminology consistency (entity names, relationship
      names, provenance/confidence vocabulary match exactly across all
      four documents)
- [x] 1.2 Confirm every design decision in `design.md` (§1–§20) has a
      corresponding requirement in `spec.md`, and confirm every
      requirement in `spec.md` traces back to a decision in `design.md` or
      an observation in `explore.md` (no invented requirements)
- [x] 1.3 Confirm the four explicitly deferred questions — exhaustive
      vocabulary catalog, numeric confidence mapping, API contract schema
      body placement, multi-repository federation mechanism — appear only
      as deferred/open items and are not silently assumed or resolved
      anywhere in `spec.md`
- [x] 1.4 Run `openspec validate "define-canonical-software-model" --type
      change --strict` and confirm it reports the change as valid

## 2. Requirement and Scenario Integrity

- [x] 2.1 Verify every in-spec cross-reference (e.g. "see Structured
      Provenance Record", "see Effective Knowledge and Precedence")
      resolves to an existing requirement name in `spec.md`
- [x] 2.2 Verify every normative SHALL / SHALL NOT / MAY statement in each
      requirement is covered by at least one scenario
- [x] 2.3 Verify the CONFLICTED-state requirement (Same-Category Conflict
      Marking and Resolution) and the Effective Knowledge and Precedence
      requirement are mutually consistent (precedence resolves
      cross-category conflicts; CONFLICTED marking covers same-category
      conflicts; no overlap or contradiction between the two)
- [x] 2.4 Verify the `## Purpose` section still accurately reflects the
      full, current set of requirements after all revisions

## 3. Stakeholder Review

- [x] 3.1 Circulate `proposal.md`, `design.md`, and `spec.md` for
      architecture and product review
- [x] 3.2 Capture review feedback and, where it changes intended
      behavior, update `design.md` and `spec.md` together (not `spec.md`
      alone) so the two remain aligned
- [x] 3.3 Obtain explicit sign-off that the CSM specification is approved
      as the basis for future implementation planning

## 4. Downstream Readiness

- [x] 4.1 Confirm the (separate, not-yet-created) Software Repository
      Understanding change can be scoped against this spec's Repository
      Evidence to CSM Transformation requirement without needing changes
      to this specification
- [x] 4.2 Record a placeholder for a future `implement-canonical-software-
      model` change (or equivalently named change) that will address
      concrete data model, storage, and technology decisions — explicitly
      out of scope for this change
- [x] 4.3 Record a placeholder for a future change to resolve each
      deferred question when it becomes blocking: exhaustive vocabulary
      catalog, numeric confidence mapping, API contract schema body
      placement, and multi-repository federation mechanism
- [x] 4.4 Confirm no Java code, Maven module, class design, or other
      implementation artifact was introduced anywhere in this change

## 5. Archive Preparation

- [x] 5.1 Confirm `openspec/specs/canonical-software-model/` does not yet
      exist, so archiving this change will create it as a new capability
      spec rather than modify an existing one
- [x] 5.2 Prepare a short archive summary noting this change delivered the
      CSM specification only (Explore → Propose → Design → Specify), with
      Review, Implementation, Test, and Verification still pending as
      separate future work
- [ ] 5.3 Archive this change only after Section 3 (Stakeholder Review)
      sign-off is complete — do not archive as a mechanical follow-on to
      this task list
