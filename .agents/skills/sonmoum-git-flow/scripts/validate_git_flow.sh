#!/usr/bin/env sh

set -eu

branch="${1:-$(git branch --show-current 2>/dev/null || true)}"
commit_line="${2:-}"
status=0

branch_pattern='^(feature|fix|hotfix|docs|style|refactor|test|chore)/[0-9]+-[a-z0-9]+(-[a-z0-9]+)*$'
legacy_branch_pattern='^feat/[0-9]+-[a-z0-9]+(-[a-z0-9]+)*$'
commit_pattern='^(feat|fix|docs|style|refactor|test|chore)\((user|lesson|subject|student|classroom|request|auth|global)\): .+$'
legacy_commit_pattern='^(feat|fix|docs|style|refactor|test|chore): .+$'

if [ -z "$branch" ]; then
  echo "WARN: could not determine current branch"
  status=1
elif printf '%s\n' "$branch" | grep -Eq "$branch_pattern"; then
  echo "OK: branch matches documented convention"
elif printf '%s\n' "$branch" | grep -Eq "$legacy_branch_pattern"; then
  echo "WARN: branch matches legacy feat/* pattern, not the documented default"
  status=1
else
  echo "WARN: branch does not match known Sonmoum patterns"
  status=1
fi

if [ -n "$commit_line" ]; then
  if printf '%s\n' "$commit_line" | grep -Eq "$commit_pattern"; then
    echo "OK: commit line matches documented convention"
  elif printf '%s\n' "$commit_line" | grep -Eq "$legacy_commit_pattern"; then
    echo "WARN: commit line matches legacy scope-less convention"
    status=1
  else
    echo "WARN: commit line does not match known Sonmoum patterns"
    status=1
  fi
fi

exit "$status"
