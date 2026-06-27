# Monitoring Mirror

This directory is the editable mirror of `/home/min/Infra/monitoring`.

## Local standalone stack

```bash
make up-monitoring
make logs-monitoring
make down-monitoring
```

Alertmanager is optional because it needs local secrets:

```bash
cp infra/monitoring/monitoring.env.example infra/monitoring/monitoring.env
$EDITOR infra/monitoring/monitoring.env
make apply-monitoring-alert-env
make up-monitoring-alerts
```

`infra/monitoring/secrets/` and `scripts/local/` stay out of git.

## Sync with central Infra

```bash
scripts/monitoring/sync-monitoring.sh diff
scripts/monitoring/sync-monitoring.sh pull
scripts/monitoring/sync-monitoring.sh push
```

`push` copies this mirror into `/home/min/Infra/monitoring` and renders Alertmanager webhook secrets from `infra/monitoring/monitoring.env` when present. It does not restart services. Apply runtime changes explicitly:

```bash
/home/min/Infra/monitoring/scripts/restart.sh
```
