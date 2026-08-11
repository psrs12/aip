/**
 * CSM Builder's core transformation architecture: the Evidence-Kind
 * Mapper contract, its registry, and the Mapping Orchestrator that
 * dispatches Repository Evidence to registered Mappers, per
 * {@code openspec/changes/implement-csm-builder/design.md} Decision 1.
 *
 * <p>This package depends only on {@code aip.core.evidence} (input)
 * and {@code aip.core.csm} (output) — never on {@code aip-analyzer} or
 * any Repository Understanding implementation (invariants 1, 2).
 */
package aip.csmbuilder.mapping;
