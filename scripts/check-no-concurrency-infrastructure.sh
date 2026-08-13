#!/usr/bin/env bash
#
# No-concurrency-infrastructure guard: fails if any production source
# file imports a thread pool, executor service, or other concurrency
# infrastructure API.
#
# This exists to mechanically enforce
# openspec/changes/define-analysis-framework/design.md Decision 6 and
# openspec/changes/implement-analysis-framework/tasks.md 6.2, 15.2:
# "Implement v1 dispatch as a simple sequential loop — no concurrency
# infrastructure (thread pool, executor service) introduced in this
# version." The Analyzer contract and dispatch loop are designed to
# permit concurrent invocation later without changing Analyzer
# semantics (per Analyzer Execution Order Independence); this guard
# only confirms the v1 *implementation* stays sequential, not that the
# contract could never support concurrency.
#
# Usage:
#   scripts/check-no-concurrency-infrastructure.sh <module>

set -euo pipefail

if [ "$#" -ne 1 ]; then
  echo "Usage: $0 <module>" >&2
  exit 2
fi

module="$1"

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
main_src="$repo_root/$module/src/main/java"

if [ ! -d "$main_src" ]; then
  echo "ERROR: '$main_src' does not exist; cannot verify concurrency-infrastructure exclusion." >&2
  exit 1
fi

forbidden_pattern='^import java\.util\.concurrent\.(Executor|ExecutorService|Executors|ThreadPoolExecutor|ForkJoinPool|CompletableFuture)'

violations="$(grep -rnE "$forbidden_pattern" "$main_src" --include="*.java" 2>/dev/null || true)"

if [ -n "$violations" ]; then
  echo "ERROR: production source under '$main_src' imports concurrency infrastructure:" >&2
  echo "$violations" >&2
  exit 1
fi

echo "OK: no production source under '$module/src/main/java' imports concurrency infrastructure"
