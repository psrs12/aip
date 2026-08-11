#!/usr/bin/env bash
#
# No-excluded-construction guard: fails if CSM Builder's construction
# packages (aip.csmbuilder.mapper, aip.csmbuilder.mapping) ever
# construct an ArchitectureComponentElement or a BOUNDARY_CONSTRAINT
# relationship - the CSM representations of an Architecture Component
# and an Architectural Boundary, per Exclusion of Architectural
# Inference and Declared-Knowledge Construction (tasks.md 24.4).
#
# Scoped deliberately to the construction packages only, not the whole
# module: aip.csmbuilder.snapshot's CsmElementCodec legitimately
# constructs an ArchitectureComponentElement in its *decode* path, for
# general round-trip completeness of the full CsmElement hierarchy (see
# its own class javadoc) - that is not Evidence-to-CSM construction and
# is not what this requirement is about.
#
# Business Context is not checked here: it is not a CSM entity kind at
# all (no Business Capability/Domain/Ownership/Criticality type exists
# anywhere in aip.core.csm), so constructing one is not merely excluded
# by discipline but impossible to even compile.
#
# Usage:
#   scripts/check-no-excluded-construction.sh <module>

set -euo pipefail

if [ "$#" -ne 1 ]; then
  echo "Usage: $0 <module>" >&2
  exit 2
fi

module="$1"

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
construction_src="$repo_root/$module/src/main/java/aip/csmbuilder/mapper $repo_root/$module/src/main/java/aip/csmbuilder/mapping"

violations=""
for dir in $construction_src; do
  if [ ! -d "$dir" ]; then
    echo "ERROR: '$dir' does not exist; cannot verify excluded-construction scope." >&2
    exit 1
  fi
  found="$(grep -rnE 'new ArchitectureComponentElement\(|CsmRelationshipType\.BOUNDARY_CONSTRAINT' "$dir" --include="*.java" 2>/dev/null || true)"
  if [ -n "$found" ]; then
    violations="$violations
$found"
  fi
done

if [ -n "$violations" ]; then
  echo "ERROR: CSM Builder's construction packages construct an excluded entity/relationship kind:" >&2
  echo "$violations" >&2
  exit 1
fi

echo "OK: aip.csmbuilder.mapper/mapping never construct an Architecture Component or Architectural Boundary"
