#!/usr/bin/env bash
#
# Test-naming guard: fails if any test source file's name contains
# "RepositoryUnderstanding" or "RU" in a way that would imply
# real-pipeline integration coverage.
#
# This exists to mechanically enforce invariant 8 (see
# openspec/changes/implement-csm-builder/design.md Decision 6 and
# openspec/changes/implement-csm-builder/tasks.md Section 22): a
# fixture-based CSM Builder test must never be mistaken for real
# Repository Understanding integration coverage. The one reserved
# exception is the future pipeline-integration test's own fixed name
# (see tasks.md 22.3), which is excluded explicitly rather than by
# pattern, so the exclusion itself stays visible and auditable here.
#
# Usage:
#   scripts/check-test-naming.sh <module>
#
# Example:
#   scripts/check-test-naming.sh aip-csm-builder

set -euo pipefail

if [ "$#" -ne 1 ]; then
  echo "Usage: $0 <module>" >&2
  exit 2
fi

module="$1"

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
test_src="$repo_root/$module/src/test/java"

if [ ! -d "$test_src" ]; then
  echo "ERROR: '$test_src' does not exist; cannot verify test naming." >&2
  exit 1
fi

# The one reserved, deliberate exception (tasks.md 22.3): the future
# real-pipeline integration test's own fixed name. It intentionally
# does contain neither "RepositoryUnderstanding" nor "RU" as it
# happens - listed here anyway so a future rename of that reserved
# seam is forced to reconsider this guard too, rather than silently
# passing or silently breaking.
reserved_exception="RepositoryToCsmPipelineIntegrationTest.java"

violations="$(find "$test_src" -name '*.java' -type f \
  | grep -v "/$reserved_exception\$" \
  | xargs -I{} basename {} \
  | grep -E "RepositoryUnderstanding|RU" || true)"

if [ -n "$violations" ]; then
  echo "ERROR: test file name(s) under '$test_src' contain 'RepositoryUnderstanding' or 'RU', implying real-pipeline integration coverage no fixture-based test may claim:" >&2
  echo "$violations" >&2
  echo "(the one reserved exception is $reserved_exception - see tasks.md 22.3)" >&2
  exit 1
fi

echo "OK: no test file name under '$module/src/test/java' implies real-pipeline integration coverage"
