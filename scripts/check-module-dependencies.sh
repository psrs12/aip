#!/usr/bin/env bash
#
# Dependency-graph guard: fails if a Maven module's resolved dependency
# graph (as reported by `mvn dependency:tree`) contains anything other
# than an explicitly allowed set of groupId:artifactId coordinates.
#
# This exists to mechanically enforce module-boundary invariants such
# as "aip-csm-builder depends on aip-core only" (see
# openspec/changes/implement-csm-builder/design.md Decision 1 and the
# invariants recorded in openspec/changes/implement-csm-builder/tasks.md
# Section 1), rather than relying on code review alone to catch a
# dependency that should never have been added.
#
# Usage:
#   scripts/check-module-dependencies.sh <module> <allowed-groupId:artifactId> [...]
#
# Example:
#   scripts/check-module-dependencies.sh aip-csm-builder aip:aip-core

set -euo pipefail

if [ "$#" -lt 2 ]; then
  echo "Usage: $0 <module> <allowed-groupId:artifactId> [...]" >&2
  exit 2
fi

module="$1"
shift
allowed=("$@")

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

tree_output="$(mvn -pl "$module" dependency:tree -Dscope=compile 2>&1)"

if ! echo "$tree_output" | grep -q "^\[INFO\] BUILD SUCCESS$"; then
  echo "ERROR: 'mvn -pl $module dependency:tree' did not succeed; cannot verify dependency graph." >&2
  echo "$tree_output" >&2
  exit 1
fi

# Lines under the tree header look like:
#   [INFO] aip:aip-csm-builder:jar:0.1.0-SNAPSHOT        <- the module itself, skip
#   [INFO] \- aip:aip-core:jar:0.1.0-SNAPSHOT:compile     <- a dependency
# Strip the "[INFO] " prefix and any tree-drawing characters, then take
# the groupId:artifactId (first two colon-separated fields).
found=()
while IFS= read -r line; do
  found+=("$line")
done < <(
  echo "$tree_output" \
    | sed -n '/--- dependency:.*:tree /,/^\[INFO\] --*$/p' \
    | grep -E '^\[INFO\][[:space:]]+[+\\|].*' \
    | sed -E 's/^\[INFO\][[:space:]]+[+\\|() -]*//' \
    | awk -F: '{print $1":"$2}'
)

violations=()
for dep in "${found[@]}"; do
  is_allowed=false
  for ok in "${allowed[@]}"; do
    if [ "$dep" = "$ok" ]; then
      is_allowed=true
      break
    fi
  done
  if [ "$is_allowed" = false ]; then
    violations+=("$dep")
  fi
done

if [ "${#violations[@]}" -gt 0 ]; then
  echo "ERROR: module '$module' has dependencies outside its allowed set." >&2
  echo "  Allowed: ${allowed[*]}" >&2
  echo "  Found:   ${found[*]:-<none>}" >&2
  echo "  Violating: ${violations[*]}" >&2
  exit 1
fi

echo "OK: module '$module' depends only on: ${allowed[*]}"
