#!/usr/bin/env bash
# 앱 jar 를 커밋 SHA 단위 릴리스로 바꿔 끼운다. 배포와 되돌리기가 같은 절차다.
#
#   02_switch-release.sh <sha>             releases/<sha>.jar 로 전환
#   02_switch-release.sh --list            보관된 릴리스와 지금 릴리스
#
# 배치 (APP_DIR, 기본 ~/app-dev):
#   releases/<sha>.jar   CI가 초록인 커밋의 jar. 최근 RELEASES_KEEP 개만 남긴다
#   app.jar -> releases/<sha>.jar   systemd 가 띄우는 경로 (01_install-app-service.sh 의 JAR_PATH)
#
# 전환 뒤 헬스 체크가 실패하면 바로 이전 릴리스로 되돌리고 실패로 끝난다.
# DB 마이그레이션은 되돌리지 않는다. 마이그레이션은 이전 코드가 견디는 모양(컬럼 추가 등)으로만 쓴다.
# Flyway 는 모르는 미래 마이그레이션을 기본으로 무시하므로(ignoreMigrationPatterns=*:future) 옛 jar 도 뜬다.
set -euo pipefail

APP_DIR="${APP_DIR:-$HOME/app-dev}"
SERVICE_NAME="${SERVICE_NAME:-gjlearn-app}"
HEALTH_URL="${HEALTH_URL:-http://127.0.0.1:9090/actuator/health}"
HEALTH_TIMEOUT_SECONDS="${HEALTH_TIMEOUT_SECONDS:-120}"
RELEASES_KEEP="${RELEASES_KEEP:-10}"
INSTALL_SCRIPT="${INSTALL_SCRIPT:-${APP_DIR}/01_install-app-service.sh}"

RELEASES_DIR="${APP_DIR}/releases"
APP_JAR="${APP_DIR}/app.jar"

current_release() {
  if [[ -L "${APP_JAR}" ]]; then
    basename "$(readlink "${APP_JAR}")" .jar
  fi
}

list_releases() {
  local current
  current="$(current_release)"
  echo "current: ${current:-(none)}"
  ls -1t "${RELEASES_DIR}"/*.jar 2>/dev/null | while read -r jar; do
    local sha
    sha="$(basename "${jar}" .jar)"
    printf '%s %s\n' "$([[ "${sha}" == "${current}" ]] && echo '*' || echo ' ')" "${sha}"
  done
}

# 처음 한 번: 예전 배포가 남긴 진짜 파일 app.jar 를 릴리스로 옮겨 되돌릴 자리를 만든다
adopt_legacy_jar() {
  if [[ -f "${APP_JAR}" && ! -L "${APP_JAR}" ]]; then
    mv "${APP_JAR}" "${RELEASES_DIR}/legacy.jar"
    ln -s "releases/legacy.jar" "${APP_JAR}"
    echo "adopted existing app.jar as release 'legacy'"
  fi
}

point_to() {
  ln -sfn "releases/$1.jar" "${APP_DIR}/app.jar.next"
  mv -T "${APP_DIR}/app.jar.next" "${APP_JAR}"
}

restart() {
  if [[ -x "${INSTALL_SCRIPT}" ]]; then
    (cd "${APP_DIR}" && "${INSTALL_SCRIPT}")
  else
    sudo systemctl restart "${SERVICE_NAME}.service"
  fi
}

wait_healthy() {
  local deadline=$((SECONDS + HEALTH_TIMEOUT_SECONDS))
  while (( SECONDS < deadline )); do
    if curl -fsS "${HEALTH_URL}" >/dev/null 2>&1 && sudo systemctl is-active --quiet "${SERVICE_NAME}"; then
      return 0
    fi
    sleep 3
  done
  return 1
}

prune() {
  local keep_current
  keep_current="$(current_release)"
  ls -1t "${RELEASES_DIR}"/*.jar 2>/dev/null | tail -n "+$((RELEASES_KEEP + 1))" | while read -r jar; do
    [[ "$(basename "${jar}" .jar)" == "${keep_current}" ]] && continue
    rm -f "${jar}"
    echo "pruned $(basename "${jar}")"
  done
}

main() {
  if [[ "${1:-}" == "--list" ]]; then
    list_releases
    return 0
  fi

  local target="${1:-}"
  if [[ ! "${target}" =~ ^([0-9a-f]{40}|legacy)$ ]]; then
    echo "usage: $0 <commit sha (40 hex)> | legacy | --list" >&2
    return 2
  fi

  mkdir -p "${RELEASES_DIR}"
  adopt_legacy_jar

  if [[ ! -f "${RELEASES_DIR}/${target}.jar" ]]; then
    echo "release not found: ${target}" >&2
    list_releases >&2
    return 1
  fi

  local previous
  previous="$(current_release)"
  if [[ "${previous}" == "${target}" ]]; then
    echo "already on ${target}; restarting"
  fi

  echo "switch: ${previous:-(none)} -> ${target}"
  point_to "${target}"
  restart

  if wait_healthy; then
    echo "healthy on ${target}"
    prune
    return 0
  fi

  echo "unhealthy on ${target} after ${HEALTH_TIMEOUT_SECONDS}s" >&2
  if [[ -n "${previous}" && "${previous}" != "${target}" ]]; then
    echo "rolling back to ${previous}" >&2
    point_to "${previous}"
    restart
    if wait_healthy; then
      echo "rolled back to ${previous}" >&2
    else
      echo "rollback to ${previous} is also unhealthy" >&2
    fi
  fi
  return 1
}

main "$@"
