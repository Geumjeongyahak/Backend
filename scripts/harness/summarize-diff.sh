#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel)"
cd "${REPO_ROOT}"

echo "# Diff Summary"
echo
echo "## Branch"
git status --short --branch
echo
echo "## Changed files"
git diff --name-only
git ls-files --others --exclude-standard
echo
echo "## Diff stat"
git diff --stat
echo
echo "## Recent commits"
git log --oneline -n 5
