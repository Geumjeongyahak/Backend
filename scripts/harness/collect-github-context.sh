#!/usr/bin/env bash
set -euo pipefail

LIMIT="${HARNESS_CONTEXT_LIMIT:-5}"
REPO_ROOT="$(git rev-parse --show-toplevel)"
cd "${REPO_ROOT}"

printf '# GitHub Context Snapshot\n\n'
printf 'Generated at: %s\n\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)"

printf '## Local templates\n\n'
if [[ -d .github/ISSUE_TEMPLATE ]]; then
  while IFS= read -r template; do
    printf '### %s\n\n' "${template}"
    sed -n '1,220p' "${template}"
    printf '\n\n'
  done < <(find .github/ISSUE_TEMPLATE -type f | sort)
else
  printf 'No .github/ISSUE_TEMPLATE directory found.\n\n'
fi

if [[ -f .github/pull_request_template.md ]]; then
  printf '### .github/pull_request_template.md\n\n'
  sed -n '1,260p' .github/pull_request_template.md
  printf '\n\n'
else
  printf '### Pull request template\n\n'
  printf 'No .github/pull_request_template.md file found. Use recent merged/open PR bodies as the style source.\n\n'
fi

if command -v gh >/dev/null 2>&1; then
  printf '## Recent issues\n\n'
  gh issue list --limit "${LIMIT}" --state all \
    --json number,title,body,labels,state,url,createdAt \
    --template '{{range .}}{{printf "### #%v %s (%s)\n%s\n\n%s\n\n" .number .title .state .url .body}}{{end}}' || \
    printf 'Failed to read issues with gh.\n\n'

  printf '## Recent pull requests\n\n'
  gh pr list --limit "${LIMIT}" --state all \
    --json number,title,body,headRefName,baseRefName,state,url,createdAt \
    --template '{{range .}}{{printf "### #%v %s (%s)\n%s\nhead: %s -> base: %s\n\n%s\n\n" .number .title .state .url .headRefName .baseRefName .body}}{{end}}' || \
    printf 'Failed to read pull requests with gh.\n\n'
else
  printf '## Recent issues / pull requests\n\n'
  printf 'gh command not found; only local templates were collected.\n\n'
fi
