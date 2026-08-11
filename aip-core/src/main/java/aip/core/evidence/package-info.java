/**
 * The Repository Evidence contract.
 *
 * <p>The single, authoritative representation of the Repository
 * Evidence Model, per
 * {@code openspec/specs/software-repository-understanding/spec.md}
 * (archived). This is the sole upstream input CSM Builder consumes
 * (see {@code Repository Evidence as Sole Construction Input} in
 * {@code openspec/specs/csm-builder/spec.md}) — no other package or
 * module SHALL define a parallel or duplicate Evidence type
 * (invariant 3; a source-scan enforcement of this is
 * {@code implement-csm-builder} tasks.md task 24.2).
 *
 * <p>This package intentionally never imports {@code aip.core.csm}.
 * Evidence is not Knowledge (see {@code Evidence and Knowledge
 * Distinction}) — even though both packages live in {@code aip-core}
 * for now (see {@code implement-csm-builder} design.md Decision 1),
 * they are kept structurally independent so neither package's
 * evolution is coupled to the other's.
 */
package aip.core.evidence;
