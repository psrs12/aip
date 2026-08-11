#!/usr/bin/env bash
#
# No-AI/heuristic/randomness-import guard: fails if any production
# source file imports a randomness API or a known AI/LLM SDK package.
#
# This exists to mechanically enforce tasks.md 24.3 ("no AI/LLM call,
# heuristic scoring, or probabilistic-inference code path exists
# anywhere in aip-csm-builder") at the one level a source scan can
# check precisely: import statements, not prose. This module's own
# javadoc extensively and deliberately documents this exclusion (e.g.
# "never on AI/LLM reasoning, heuristic inference, or probabilistic
# judgment") - a keyword scan over comments would immediately false-
# positive on that very documentation, so this check is scoped to
# `^import` lines only, which cannot appear in a comment.
#
# The stronger, structural guarantee is scripts/check-module-dependencies.sh
# (tasks.md 24.1): aip-csm-builder's resolved dependency graph permits
# only aip-core, so none of these imports could even compile against a
# real AI/ML library if one were attempted. This check adds precision
# at the source level and also catches JDK-native randomness APIs,
# which check-module-dependencies.sh's dependency-graph scope does not.
#
# Usage:
#   scripts/check-no-ai-heuristic-imports.sh <module>

set -euo pipefail

if [ "$#" -ne 1 ]; then
  echo "Usage: $0 <module>" >&2
  exit 2
fi

module="$1"

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
main_src="$repo_root/$module/src/main/java"

if [ ! -d "$main_src" ]; then
  echo "ERROR: '$main_src' does not exist; cannot verify AI/heuristic import exclusion." >&2
  exit 1
fi

# JDK-native randomness/probabilistic APIs, plus common AI/LLM SDK
# package prefixes. Matched against `^import` lines only.
forbidden_pattern='^import (java\.util\.Random|java\.security\.SecureRandom|java\.util\.concurrent\.ThreadLocalRandom|com\.openai|com\.anthropic|dev\.langchain4j|ai\.djl|org\.tensorflow|org\.deeplearning4j|weka\.)'

violations="$(grep -rnE "$forbidden_pattern" "$main_src" --include="*.java" 2>/dev/null || true)"

if [ -n "$violations" ]; then
  echo "ERROR: production source under '$main_src' imports a randomness or AI/LLM SDK package:" >&2
  echo "$violations" >&2
  exit 1
fi

echo "OK: no production source under '$module/src/main/java' imports a randomness or AI/LLM SDK package"
