#!/usr/bin/env bash
#
# CSM/Analysis contract uniqueness guard: fails if any Java source file
# outside the aip.core.csm package declares a class/record/interface/
# enum whose name matches one of the CSM or Analysis Framework
# contract's own type names.
#
# This exists to mechanically enforce
# openspec/changes/implement-analysis-framework/tasks.md 14.3: no
# parallel Subject, CsmElement, AnalysisResult, or snapshot type may
# exist anywhere else in the codebase (e.g. an aip-analysis-local
# "AnalysisResult" DTO that drifts from the real, aip-core-hosted
# type over time) - the same discipline
# check-no-duplicate-evidence-types.sh already applies to the
# Repository Evidence contract.
#
# Usage:
#   scripts/check-no-duplicate-csm-types.sh
#
# Scans every module's src/main and src/test - not just aip-analysis -
# since a duplicate could in principle be introduced anywhere in the
# repository.

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

# The CSM domain model's and Analysis Framework's own type names
# (aip.core.csm). Not exhaustive of every aip.core.csm type - scoped to
# the ones most likely to be reinvented by a consuming module
# (mirroring check-no-duplicate-evidence-types.sh's own scope).
reserved_names=(
  CsmElement
  CsmElementId
  CsmRelationship
  CsmRelationshipType
  CsmEntityKind
  Subject
  SubjectConflictMarker
  CsmSnapshotId
  CsmSnapshotSource
  CsmScope
  CsmScopeInstance
  AnalysisView
  AnalysisResult
  AnalysisResultId
)

violations=()
for name in "${reserved_names[@]}"; do
  while IFS= read -r file; do
    package_line="$(grep -m1 '^package ' "$file" || true)"
    if [[ "$package_line" != "package aip.core.csm;" ]]; then
      violations+=("$name declared in $file ($package_line)")
    fi
  done < <(
    grep -rlE "(public |)(final |)(class|record|interface|enum) ${name}\b" \
      --include="*.java" \
      */src/main/java */src/test/java 2>/dev/null || true
  )
done

if [ "${#violations[@]}" -gt 0 ]; then
  echo "ERROR: CSM/Analysis contract type name(s) declared outside aip.core.csm:" >&2
  printf '  %s\n' "${violations[@]}" >&2
  exit 1
fi

echo "OK: no CSM/Analysis contract type name is declared outside aip.core.csm"
