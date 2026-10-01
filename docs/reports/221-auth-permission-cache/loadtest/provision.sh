#!/usr/bin/env bash
# dev 와 같은 사양의 임시 VM 셋을 만든다 (#221 부하 측정).
#   lt221-db   e2-micro  Ubuntu 24.04  PostgreSQL 16   (dev: gjlearn-dev-db 와 같음)
#   lt221-app  e2-small  Ubuntu 24.04  OpenJDK 21      (dev: gjlearn-dev-app 과 같음)
#   lt221-load e2-standard-2           k6              (부하 생성기. 공유 코어가 아니라 병목이 되지 않게)
# 네트워크 태그를 붙이지 않는다 → default-allow-internal · ssh 규칙만 적용되고 앱 포트는 밖에 안 열린다.
set -euo pipefail

ZONE="${ZONE:-us-east1-c}"
P="${PREFIX:-lt221}"
LABELS="purpose=loadtest-221"
COMMON=(--zone "$ZONE" --image-family ubuntu-2404-lts-amd64 --image-project ubuntu-os-cloud
        --boot-disk-size 10GB --boot-disk-type pd-standard --labels "$LABELS"
        --no-service-account --no-scopes)

gcloud compute instances create "$P-db"   --machine-type e2-micro      "${COMMON[@]}"
gcloud compute instances create "$P-app"  --machine-type e2-small      "${COMMON[@]}"
gcloud compute instances create "$P-load" --machine-type e2-standard-2 "${COMMON[@]}"

ssh_() { gcloud compute ssh "$1" --zone "$ZONE" --tunnel-through-iap --quiet --command "$2"; }

# SSH 가 열릴 때까지 기다린다
for vm in db app load; do
  until ssh_ "$P-$vm" true 2>/dev/null; do sleep 5; done
done

# DB: dev 의 비기본 설정 중 성능에 닿는 것만 맞춘다 (shared_buffers=163848kB, max_connections=100, ssl=on 은 Ubuntu 기본).
# pg_stat_statements 는 측정용으로만 더한다 (dev 와 유일하게 다른 설정).
ssh_ "$P-db" 'set -e
sudo apt-get update -qq && sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq postgresql postgresql-contrib sysstat >/dev/null
CONF=$(ls -d /etc/postgresql/*/main)
echo "listen_addresses = '"'"'*'"'"'
shared_buffers = 163848kB
shared_preload_libraries = '"'"'pg_stat_statements'"'"'
pg_stat_statements.track = all" | sudo tee $CONF/conf.d/lt221.conf >/dev/null
echo "host all all 10.128.0.0/9 scram-sha-256" | sudo tee -a $CONF/pg_hba.conf >/dev/null
sudo systemctl restart postgresql
sudo -u postgres psql -qc "create user gj with password '"'"'gj'"'"'" -c "create database geumjeongyahak owner gj"
sudo -u postgres psql -d geumjeongyahak -qc "create extension pg_stat_statements" -c "grant pg_read_all_stats to gj"
psql --version'

ssh_ "$P-app" 'sudo apt-get update -qq && sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq openjdk-21-jre-headless postgresql-client sysstat >/dev/null && java -version 2>&1 | head -1'

ssh_ "$P-load" 'set -e
curl -sSL https://github.com/grafana/k6/releases/download/v1.3.0/k6-v1.3.0-linux-amd64.tar.gz | tar xz
sudo mv k6-*/k6 /usr/local/bin/ && k6 version'

echo "provisioned: $P-db $P-app $P-load"
