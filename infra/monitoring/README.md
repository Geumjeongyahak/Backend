# Monitoring Mirror

This directory is the editable mirror of `/home/min/Infra/monitoring`.

GJLearn metrics, dashboards, and alerts no longer use this stack. They are collected by the GCE Ops Agent and viewed in Cloud Monitoring or the optional App VM Grafana. Keep this mirror only for other home-lab workloads.

## Local standalone stack

```bash
make up-monitoring
make logs-monitoring
make down-monitoring
```

Alertmanager is optional and uses a discard receiver until another workload adds routes:

```bash
make up-monitoring-alerts
```

`infra/monitoring/secrets/` and `scripts/local/` stay out of git.

## Sync with central Infra

```bash
scripts/monitoring/sync-monitoring.sh diff
scripts/monitoring/sync-monitoring.sh pull
scripts/monitoring/sync-monitoring.sh push
```

`push` copies this mirror into `/home/min/Infra/monitoring`. It does not restart services. Apply runtime changes explicitly:

```bash
/home/min/Infra/monitoring/scripts/restart.sh
```
