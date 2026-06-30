#!/usr/bin/env sh

set -eu

if [ "$#" -lt 3 ]; then
  echo "usage: $0 <type> <scope> <subject> [issue-number]" >&2
  exit 1
fi

type="$1"
scope="$2"
subject="$3"
issue="${4:-}"

case "$type" in
  feat|fix|docs|style|refactor|test|chore)
    ;;
  *)
    echo "unsupported commit type: $type" >&2
    exit 2
    ;;
esac

case "$scope" in
  user|lesson|subject|student|classroom|request|auth|global)
    ;;
  *)
    echo "unsupported scope: $scope" >&2
    exit 3
    ;;
esac

normalized_subject="$(printf '%s' "$subject" | sed -E 's/[[:space:]]+/ /g; s/^ //; s/ $//')"

if [ -z "$normalized_subject" ]; then
  echo "subject must not be empty" >&2
  exit 4
fi

printf '%s(%s): %s\n' "$type" "$scope" "$normalized_subject"

if [ -n "$issue" ]; then
  normalized_issue="$(printf '%s' "$issue" | sed -E 's/[^0-9]//g')"
  if [ -z "$normalized_issue" ]; then
    echo "issue number must contain at least one digit" >&2
    exit 5
  fi
  printf '\nRefs #%s\n' "$normalized_issue"
fi
