COMPOSE := docker compose

LOCAL_PROJECT := -p backend-local
LOCAL_FILES := $(LOCAL_PROJECT) --env-file .env.local -f docker-compose.local.yml
MONITORING_PROJECT := -p gjlearn-monitoring
MONITORING_FILES := $(MONITORING_PROJECT) -f infra/monitoring/docker-compose.yml

LOCAL_SERVICES := app db

.PHONY: \
	up-local down-local ps-local logs-local build-local \
	up-monitoring up-monitoring-alerts down-monitoring ps-monitoring logs-monitoring \
	apply-monitoring-alert-env apply-monitoring-alert-env-central apply-monitoring-alert-env-all \
	sync-monitoring-diff sync-monitoring-pull sync-monitoring-push

up-local:
	$(COMPOSE) $(LOCAL_FILES) up --build --remove-orphans $(LOCAL_SERVICES)

down-local:
	$(COMPOSE) $(LOCAL_FILES) down --remove-orphans

ps-local:
	$(COMPOSE) $(LOCAL_FILES) ps $(LOCAL_SERVICES)

logs-local:
	$(COMPOSE) $(LOCAL_FILES) logs -f app db

build-local:
	$(COMPOSE) $(LOCAL_FILES) build $(LOCAL_SERVICES)

up-monitoring:
	$(COMPOSE) $(MONITORING_FILES) up -d --remove-orphans prometheus loki grafana

up-monitoring-alerts:
	$(COMPOSE) $(MONITORING_FILES) --profile alerts up -d --remove-orphans

down-monitoring:
	$(COMPOSE) $(MONITORING_FILES) --profile alerts down

ps-monitoring:
	$(COMPOSE) $(MONITORING_FILES) --profile alerts ps

logs-monitoring:
	$(COMPOSE) $(MONITORING_FILES) --profile alerts logs -f

apply-monitoring-alert-env:
	scripts/monitoring/apply-alert-env.sh repo

apply-monitoring-alert-env-central:
	scripts/monitoring/apply-alert-env.sh central

apply-monitoring-alert-env-all:
	scripts/monitoring/apply-alert-env.sh both

sync-monitoring-diff:
	scripts/monitoring/sync-monitoring.sh diff

sync-monitoring-pull:
	scripts/monitoring/sync-monitoring.sh pull

sync-monitoring-push:
	scripts/monitoring/sync-monitoring.sh push
