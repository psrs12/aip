#!/usr/bin/env bash
#
# Evidence-contract uniqueness guard: fails if any Java source file
# outside the aip.core.evidence package declares a class/record/
# interface/enum whose name matches one of the Repository Evidence
# contract's own type names.
#
# This exists to mechanically enforce invariant 3 (see
# openspec/changes/implement-csm-builder/tasks.md 3.6, 24.2): aip.core.evidence
# is the single, authoritative Repository Evidence contract - no
# duplicate or parallel Evidence type may exist anywhere else in the
# codebase (e.g. a CSM-Builder-local "EvidenceItem" DTO that drifts
# from the real contract over time).
#
# Usage:
#   scripts/check-no-duplicate-evidence-types.sh
#
# Scans every module's src/main and src/test - not just aip-csm-builder
# - since a duplicate could in principle be introduced anywhere in the
# repository.

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

# The Repository Evidence contract's own type names (aip.core.evidence).
reserved_names=(
  EvidenceItem
  EvidenceId
  EvidenceKind
  EvidenceAttributes
  EvidenceLocation
  EvidenceRelationship
  EvidenceRelationshipType
  DiscoveryOutcome
  DiscoveryOutcomeStatus
  FailureReason
  ExtractionMethod
  ChangeStatus
  LifecycleState
  ClassifiedEvidenceItem
  RepositoryEvidenceModel
)

violations=()
for name in "${reserved_names[@]}"; do
  # Find every Java file declaring a top-level type with this exact
  # name (class/record/interface/enum), across every module's main and
  # test sources.
  while IFS= read -r file; do
    package_line="$(grep -m1 '^package ' "$file" || true)"
    if [[ "$package_line" != "package aip.core.evidence;" ]]; then
      violations+=("$name declared in $file ($package_line)")
    fi
  done < <(
    grep -rlE "(public |)(final |)(class|record|interface|enum) ${name}\b" \
      --include="*.java" \
      */src/main/java */src/test/java 2>/dev/null || true
  )
done

if [ "${#violations[@]}" -gt 0 ]; then
  echo "ERROR: Evidence contract type name(s) declared outside aip.core.evidence:" >&2
  printf '  %s\n' "${violations[@]}" >&2
  exit 1
fi

echo "OK: no Repository Evidence contract type name is declared outside aip.core.evidence"
