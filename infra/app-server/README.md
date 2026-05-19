# App Server Observability

This compose slice runs on the same GCE instance as the API and PostgreSQL containers.

It exposes local metrics for the external monitoring GCE to scrape:

- Spring Actuator: `:9090/actuator/prometheus`
- node-exporter: `:9100/metrics`
- cAdvisor: `:8081/metrics`
- postgres-exporter: `:9187/metrics`
- Alloy self metrics: `:12345/metrics`

Alloy also tails Docker container logs and pushes them to Loki:

```env
LOKI_PUSH_URL=http://MONITORING_PRIVATE_IP:3100/loki/api/v1/push
DEPLOY_ENV=dev
INSTANCE_NAME=app-dev-1
```

Keep these ports private. On GCE, allow them only from the monitoring instance private IP.
