COMPOSE := docker compose

APP_IMAGE ?= ghcr.io/geumjeongyahak/backend:latest

BASE_FILES := -f docker-compose.yml
LOCAL_FILES := --env-file .env.local $(BASE_FILES) -f docker-compose.local.yml
DEV_FILES := $(BASE_FILES) -f docker-compose.dev.yml -f infra/app-server/docker-compose.observability.yml
MONITORING_FILES := -f infra/monitoring/docker-compose.prometheus.yml

APP_SERVER_SERVICES := app db node-exporter cadvisor postgres-exporter alloy

.PHONY: \
	up down ps logs \
	up-local down-local ps-local logs-local build-local \
	pull-dev up-dev down-dev ps-dev logs-dev prune-dev deploy-dev \
	up-monitoring down-monitoring ps-monitoring logs-monitoring pull-monitoring deploy-monitoring \
	push

up:
	$(COMPOSE) $(BASE_FILES) up -d --remove-orphans

down:
	$(COMPOSE) $(BASE_FILES) down

ps:
	$(COMPOSE) $(BASE_FILES) ps

logs:
	$(COMPOSE) $(BASE_FILES) logs -f app db

up-local:
	$(COMPOSE) $(LOCAL_FILES) up --build

down-local:
	$(COMPOSE) $(LOCAL_FILES) down

ps-local:
	$(COMPOSE) $(LOCAL_FILES) ps

logs-local:
	$(COMPOSE) $(LOCAL_FILES) logs -f app db

build-local:
	$(COMPOSE) $(LOCAL_FILES) build

pull-dev:
	$(COMPOSE) $(DEV_FILES) pull app node-exporter cadvisor postgres-exporter alloy

up-dev:
	$(COMPOSE) $(DEV_FILES) up -d --remove-orphans

down-dev:
	$(COMPOSE) $(DEV_FILES) down

ps-dev:
	$(COMPOSE) $(DEV_FILES) ps

logs-dev:
	$(COMPOSE) $(DEV_FILES) logs -f $(APP_SERVER_SERVICES)

prune-dev:
	docker image prune -f
	docker builder prune -f

deploy-dev: pull-dev up-dev prune-dev

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

push:
	docker push $(APP_IMAGE)
