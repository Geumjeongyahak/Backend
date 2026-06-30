#!/usr/bin/env sh

set -eu

if [ "$#" -lt 3 ]; then
  echo "usage: $0 <kind> <issue-number> <slug>" >&2
  exit 1
fi

kind="$1"
issue="$2"
shift 2
slug_raw="$*"

normalize_slug() {
  printf '%s' "$1" \
    | tr '[:upper:]' '[:lower:]' \
    | sed -E 's/[^a-z0-9]+/-/g; s/^-+//; s/-+$//; s/-+/-/g'
}

normalized_issue="$(printf '%s' "$issue" | sed -E 's/[^0-9]//g')"
normalized_slug="$(normalize_slug "$slug_raw")"

case "$kind" in
  feat|feature)
    prefix="feature"
    ;;
  fix)
    prefix="fix"
    ;;
  hotfix)
    prefix="hotfix"
    ;;
  docs|style|refactor|test|chore)
    prefix="$kind"
    ;;
  *)
    echo "unsupported kind: $kind" >&2
    exit 2
    ;;
esac

if [ -z "$normalized_issue" ]; then
  echo "issue number must contain at least one digit" >&2
  exit 3
fi

if [ -z "$normalized_slug" ]; then
  echo "slug must contain at least one ASCII letter or digit" >&2
  exit 4
fi

printf '%s/%s-%s\n' "$prefix" "$normalized_issue" "$normalized_slug"
