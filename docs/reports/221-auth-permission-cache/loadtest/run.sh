#!/usr/bin/env bash
# provision.sh 로 만든 VM 에 jar 하나를 올리고 시나리오 전부를 돌려 raw/<label>/ 에 모은다.
#   run.sh <label> <jar>        예) JAR_COMMIT=532f2676 run.sh before build/libs/geumjeongyahak-api-0.0.1.jar
# 매 측정 직전에 pg_stat_statements 를 비우고, 측정 동안 앱·DB VM 의 vmstat 을 1초 간격으로 남긴다.
#   MODE=vus  (기본) LEVELS 는 동시 사용자 수 — 닫힌 모델, 최대 처리량
#   MODE=rate        LEVELS 는 초당 요청 수 — 고정 도착률, 같은 부하에서 지연 · 자원 비교
set -euo pipefail

LABEL="$1"; JAR="$2"
ZONE="${ZONE:-us-east1-c}"; P="${PREFIX:-lt221}"
DURATION="${DURATION:-60}"; WARMUP="${WARMUP:-30}"; REST="${REST:-20}"
SCENARIOS=(${SCENARIOS:-dept-anon dept-auth me})
LEVELS=(${LEVELS:-1 10 30})
MODE="${MODE:-vus}"; SUFFIX=$([[ "$MODE" == rate ]] && echo r || echo c)

HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="$(git -C "$HERE" rev-parse --show-toplevel)"
OUT="$HERE/../raw/$LABEL"; mkdir -p "$OUT"

ssh_() { gcloud compute ssh "$1" --zone "$ZONE" --tunnel-through-iap --quiet --command "$2"; }
scp_() { gcloud compute scp --zone "$ZONE" --tunnel-through-iap --quiet "$@"; }
ip_()  { gcloud compute instances describe "$1" --zone "$ZONE" --format='value(networkInterfaces[0].networkIP)'; }

DB_IP=$(ip_ "$P-db"); APP_IP=$(ip_ "$P-app")
BASE="http://$APP_IP:8080"
dbq() { ssh_ "$P-db" "sudo -u postgres psql -d geumjeongyahak -XAt -F \$'\t' -c \"$1\""; }

# --- 앱 배포 · 기동 -------------------------------------------------------------
FAKE_SA=$(python3 - <<'PY'
import base64, json, subprocess
key = subprocess.run("openssl genrsa 2048 2>/dev/null | openssl pkcs8 -topk8 -nocrypt", shell=True,
                     capture_output=True, text=True, check=True).stdout
print(base64.b64encode(json.dumps({"type": "service_account", "project_id": "dummy", "private_key_id": "dummy",
    "private_key": key, "client_email": "dummy@dummy.iam.gserviceaccount.com", "client_id": "1",
    "token_uri": "https://oauth2.googleapis.com/token"}).encode()).decode())
PY
)
TMP=$(mktemp -d); trap 'rm -rf "$TMP"' EXIT
grep -v '^#' "$HERE/app.env" | sed "s/__DB_HOST__/$DB_IP/; s|__FAKE_SA__|$FAKE_SA|" > "$TMP/app.env"
# 같은 jar 가 이미 올라가 있으면 다시 올리지 않는다 (IAP 터널로 150MB 는 몇 분 걸린다)
# jar 는 해시로 이름을 붙여 두므로 전 · 후 jar 를 번갈아 돌려도 한 번씩만 올라간다.
LOCAL_SUM=$(sha256sum "$JAR" | cut -d' ' -f1)
REMOTE_JAR="app-${LOCAL_SUM:0:12}.jar"
if ! ssh_ "$P-app" "test -f ~/$REMOTE_JAR"; then
  cp "$JAR" "$TMP/$REMOTE_JAR" && scp_ "$TMP/$REMOTE_JAR" "$P-app:~/"
fi
scp_ "$TMP/app.env" "$REPO/src/test/resources/sql/init_data.sql" "$HERE/seed-topup.sql" "$P-app:~/"

ssh_ "$P-app" 'sudo systemctl stop gjapp 2>/dev/null; sudo systemctl reset-failed gjapp 2>/dev/null
sudo systemd-run --unit gjapp --uid "$USER" --working-directory "$HOME" -p EnvironmentFile="$HOME/app.env" \
  /usr/bin/java -Dserver.port=8080 -jar "$HOME/'"$REMOTE_JAR"'" >/dev/null
for i in $(seq 1 60); do curl -sf -o /dev/null localhost:8080/api/v1/departments && exit 0; sleep 3; done
sudo journalctl -u gjapp -n 50 --no-pager; exit 1'

# 비어 있는 DB 면 시드를 넣는다 (스키마는 앱이 Flyway 로 만든다)
if [[ "$(dbq 'select count(*) from users')" == "0" ]]; then
  ssh_ "$P-app" "export PGHOST=$DB_IP PGUSER=gj PGPASSWORD=gj PGDATABASE=geumjeongyahak
psql -v ON_ERROR_STOP=1 -q -f init_data.sql && psql -v ON_ERROR_STOP=1 -q -f seed-topup.sql >/dev/null"
fi

# --- 환경 스냅샷 ---------------------------------------------------------------
{
  echo "label=$LABEL"; echo "jar_sha256=$LOCAL_SUM"; echo "jar_commit=${JAR_COMMIT:-unknown}"; echo "mode=$MODE"
  echo "date=$(date -Iseconds)"; echo "duration_s=$DURATION warmup_s=$WARMUP rest_s=$REST"
  gcloud compute instances list --filter="labels.purpose=loadtest-221" --format='value(name,machineType.basename())'
  echo "--- db"; dbq "select version()"
  dbq "select name, setting from pg_settings where name in ('shared_buffers','max_connections','work_mem','effective_cache_size','ssl','shared_preload_libraries')"
  dbq "select relname, n_live_tup from pg_stat_user_tables where relname in ('users','user_credentials','user_permissions','department_permissions','departments') order by 1"
  echo "--- app→db rtt"; ssh_ "$P-app" "ping -c 20 -q $DB_IP | tail -1"
  echo "--- app"; ssh_ "$P-app" 'java -version 2>&1 | head -1; nproc; free -m | sed -n 2p'
} > "$OUT/env.txt" 2>&1

# --- 토큰 (측정 구간 밖에서 로그인) -------------------------------------------------
ssh_ "$P-load" "rm -f tokens.json; for id in \$(seq 6 63); do
  curl -sf $BASE/api/v1/auth/login -H 'Content-Type: application/json' -d '{\"email\":\"lt'\$id'@test.com\",\"password\":\"teacher01\"}'
  echo; done | python3 -c 'import json,sys; print(json.dumps([json.loads(l)[\"accessToken\"] for l in sys.stdin if l.strip()]))' > tokens.json
n=\$(python3 -c 'import json; print(len(json.load(open(\"tokens.json\"))))'); echo \"\$n tokens\"; [ \"\$n\" -eq 58 ]" \
  || { echo "로그인 토큰이 58개가 아니다 — 인증 시나리오가 익명으로 측정되지 않게 멈춘다" >&2; exit 1; }
scp_ "$HERE/k6.js" "$P-load:~/"

# --- 워밍업 (JIT · 커넥션 풀 · DB 버퍼) ----------------------------------------------
for s in "${SCENARIOS[@]}"; do
  ssh_ "$P-load" "k6 run -q --no-summary -e BASE=$BASE -e SCENARIO=$s -e MODE=vus -e VUS=10 -e DURATION=${WARMUP}s k6.js" >/dev/null
done

# --- 측정 ---------------------------------------------------------------------
for s in "${SCENARIOS[@]}"; do
  for c in "${LEVELS[@]}"; do
    name="$s-$SUFFIX$c"; echo "== $LABEL $name"
    sleep "$REST"
    dbq "select pg_stat_statements_reset()" >/dev/null
    ssh_ "$P-app" "timeout $((DURATION + 5)) vmstat -n 1" > "$OUT/$name.app-vmstat.txt" 2>/dev/null &
    ssh_ "$P-db"  "timeout $((DURATION + 5)) vmstat -n 1" > "$OUT/$name.db-vmstat.txt" 2>/dev/null &
    # Hikari: 활성 · 대기 커넥션을 1초마다
    ssh_ "$P-app" "end=\$((SECONDS + $DURATION)); while [ \$SECONDS -lt \$end ]; do
      curl -s 127.0.0.1:9090/actuator/prometheus | grep -E '^hikaricp_connections_(active|pending)' | awk '{printf \"%s \", \$2}'; echo; sleep 1; done" > "$OUT/$name.hikari.txt" 2>/dev/null &
    ssh_ "$P-load" "k6 run -q -e BASE=$BASE -e SCENARIO=$s -e MODE=$MODE -e VUS=$c -e RATE=$c -e DURATION=${DURATION}s --summary-export summary.json k6.js >/dev/null && cat summary.json" > "$OUT/$name.k6.json"
    wait
    dbq "select calls, round(total_exec_time::numeric, 2), round(mean_exec_time::numeric, 4), regexp_replace(query, '\s+', ' ', 'g')
         from pg_stat_statements where dbid = (select oid from pg_database where datname = 'geumjeongyahak')
         order by calls desc limit 20" > "$OUT/$name.pgss.tsv"
  done
done

echo "done → $OUT"
