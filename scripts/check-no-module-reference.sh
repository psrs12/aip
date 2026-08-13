#!/usr/bin/env bash
#
# Generic no-reference guard: fails if any source file (main or test)
# under a given module contains a match for a given forbidden pattern
# (typically a package prefix).
#
# This generalizes check-no-csm-builder-analysis-adapter.sh's own
# purpose so later implement-* changes (Finding Model not referencing
# aip-rules, Agent Framework not referencing aip-findings, ...) can
# reuse it directly instead of each writing a near-identical
# hardcoded script - the same "general mechanism, not a second copy"
# discipline this project applies to production code, applied here to
# CI tooling.
#
# Usage:
#   scripts/check-no-module-reference.sh <module> <forbidden-pattern>
#
# Example:
#   scripts/check-no-module-reference.sh aip-rules 'aip\.analysis\.'

set -euo pipefail

if [ "$#" -ne 2 ]; then
  echo "Usage: $0 <module> <forbidden-pattern>" >&2
  exit 2
fi

module="$1"
forbidden_pattern="$2"

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
main_src="$repo_root/$module/src/main/java"
test_src="$repo_root/$module/src/test/java"

if [ ! -d "$main_src" ]; then
  echo "ERROR: '$main_src' does not exist; cannot verify." >&2
  exit 1
fi

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
  echo "ERROR: '$module' source references forbidden pattern '$forbidden_pattern':" >&2
  echo "$violations" >&2
  exit 1
fi

echo "OK: no '$module' source references '$forbidden_pattern'"
