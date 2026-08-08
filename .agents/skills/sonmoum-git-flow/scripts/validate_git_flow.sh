#!/usr/bin/env sh

set -eu

branch="${1:-$(git branch --show-current 2>/dev/null || true)}"
commit_line="${2:-}"
status=0

# `feat/` 가 현행입니다. `feature/` 는 오래된 브랜치에만 남아 있습니다.
# `hotfix/` 만 이슈 번호를 요구하지 않습니다. 프로덕션이 멈춘 자리에서 이슈를 먼저
# 만들라고 막으면 그 규칙은 안 지켜집니다 (`hotfix/prod-health-retry`).
branch_pattern='^((feat|fix|docs|style|refactor|test|chore)/[0-9]+|hotfix/)-?[a-z0-9]+(-[a-z0-9]+)*$'
legacy_branch_pattern='^feature/[0-9]+-[a-z0-9]+(-[a-z0-9]+)*$'
# scope 는 도메인 폴더 이름을 하이픈으로 바꾼 것입니다. 고정 목록을 두면 새 도메인이
# 생길 때마다 여기가 먼저 막습니다.
commit_pattern='^(feat|fix|docs|style|refactor|test|chore)\([a-z][a-z0-9-]*\): .+$'
legacy_commit_pattern='^(feat|fix|docs|style|refactor|test|chore): .+$'

if [ -z "$branch" ]; then
  echo "WARN: could not determine current branch"
  status=1
elif printf '%s\n' "$branch" | grep -Eq "$branch_pattern"; then
  echo "OK: branch matches documented convention"
elif printf '%s\n' "$branch" | grep -Eq "$legacy_branch_pattern"; then
  echo "WARN: branch matches legacy feature/* pattern, use feat/* instead"
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
