COMPOSE := docker compose

LOCAL_PROJECT := -p backend-local
LOCAL_FILES := $(LOCAL_PROJECT) --env-file .env.local -f docker-compose.local.yml
MONITORING_FILES := -f infra/monitoring/docker-compose.prometheus.yml

LOCAL_SERVICES := app db

.PHONY: \
	up-local down-local ps-local logs-local build-local \
	up-monitoring down-monitoring ps-monitoring logs-monitoring pull-monitoring deploy-monitoring

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

pull-monitoring:
	$(COMPOSE) $(MONITORING_FILES) pull

up-monitoring:
	$(COMPOSE) $(MONITORING_FILES) up -d --remove-orphans

down-monitoring:
	$(COMPOSE) $(MONITORING_FILES) down

ps-monitoring:
	$(COMPOSE) $(MONITORING_FILES) ps

logs-monitoring:
	$(COMPOSE) $(MONITORING_FILES) logs -f

deploy-monitoring: pull-monitoring up-monitoring
