#!/usr/bin/env bash
#
# No-real-adapter guard: fails if any source file under aip-csm-builder
# references CsmSnapshotSource, CsmScope, or AnalysisResult.
#
# This exists to mechanically enforce
# openspec/changes/implement-analysis-framework/proposal.md's own
# Binding Decision 2 ("no real aip-csm-builder adapter in this
# change... aip-analysis remains fully independent of aip-csm-builder")
# for the whole build's lifetime, not merely at design time. Unlike the
# module-level dependency-graph guard (aip-analysis depends on aip-core
# only), this checks the *reverse* direction: aip-csm-builder already
# depends on aip-core, so nothing stops it from referencing these new
# aip-core types without any new Maven dependency being added - this
# check closes that gap explicitly.
#
# Usage:
#   scripts/check-no-csm-builder-analysis-adapter.sh

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
main_src="$repo_root/aip-csm-builder/src/main/java"
test_src="$repo_root/aip-csm-builder/src/test/java"

forbidden_pattern='\b(CsmSnapshotSource|CsmScope|CsmScopeInstance|AnalysisResult|AnalysisResultId|AnalysisView)\b'

violations=""
for dir in "$main_src" "$test_src"; do
  if [ -d "$dir" ]; then
    matches="$(grep -rnE "$forbidden_pattern" "$dir" --include="*.java" 2>/dev/null || true)"
    if [ -n "$matches" ]; then
      violations="$violations
$matches"
    fi
  fi
done

if [ -n "$violations" ]; then
  echo "ERROR: aip-csm-builder source references an Analysis Framework aip-core type — no real adapter is permitted in this change:" >&2
  echo "$violations" >&2
  exit 1
fi

echo "OK: no aip-csm-builder source references CsmSnapshotSource, CsmScope, or AnalysisResult"
