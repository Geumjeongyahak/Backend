#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel)"
cd "${REPO_ROOT}"

printf '==> git diff --check\n'
git diff --check

printf '\n==> shell syntax\n'
while IFS= read -r script; do
  [[ -z "${script}" ]] && continue
  bash -n "${script}"
  printf 'ok %s\n' "${script}"
done < <(find scripts -type f -name '*.sh' | sort)

printf '\n==> harness JSON schema parse\n'
python3 - <<'PY'
import json
from pathlib import Path
for path in sorted(Path('harness/schemas').glob('*.json')):
    with path.open(encoding='utf-8') as f:
        json.load(f)
    print(f'ok {path}')
PY

printf '\n==> Gradle tests\n'
./gradlew test
