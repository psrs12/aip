#!/usr/bin/env bash
#
# Fixture-package scope guard: fails if any production source file
# (under a module's src/main/java) imports or references the given
# test-only fixture package.
#
# This exists to mechanically enforce invariant 4 (see
# openspec/changes/implement-csm-builder/design.md Decision 5 and
# openspec/changes/implement-csm-builder/tasks.md Section 21) -
# CSM Builder's production code SHALL NOT depend on the fixture-building
# API, only test code may. Maven's own src/main vs. src/test classpath
# separation already makes this unreachable at compile time; this check
# exists so the invariant is explicit and CI-visible rather than an
# implicit consequence of the build tool's own layout.
#
# Usage:
#   scripts/check-fixture-package-scope.sh <module> <fixture-package>
#
# Example:
#   scripts/check-fixture-package-scope.sh aip-csm-builder aip.csmbuilder.test.fixtures

set -euo pipefail

if [ "$#" -ne 2 ]; then
  echo "Usage: $0 <module> <fixture-package>" >&2
  exit 2
fi

module="$1"
fixture_package="$2"

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
main_src="$repo_root/$module/src/main/java"

if [ ! -d "$main_src" ]; then
  echo "ERROR: '$main_src' does not exist; cannot verify fixture-package scope." >&2
  exit 1
fi

matches="$(grep -rl "$fixture_package" "$main_src" 2>/dev/null || true)"

if [ -n "$matches" ]; then
  echo "ERROR: production source under '$main_src' references the test-only fixture package '$fixture_package':" >&2
  echo "$matches" >&2
  exit 1
fi

echo "OK: no production source under '$module/src/main/java' references '$fixture_package'"
