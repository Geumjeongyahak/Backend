COMPOSE := docker compose

APP_IMAGE ?= ghcr.io/geumjeongyahak/backend:latest

BASE_FILES := -f docker-compose.yml
APP_FILES := -f docker-compose.app.yml -f docker-compose.dev.yml
DB_FILES := -f docker-compose.db.yml
LOCAL_PROJECT := -p backend-local
LOCAL_FILES := $(LOCAL_PROJECT) --env-file .env.local $(BASE_FILES) -f docker-compose.local.yml
MONITORING_FILES := -f infra/monitoring/docker-compose.prometheus.yml

LOCAL_SERVICES := app db
APP_SERVER_SERVICES := app node-exporter
DB_SERVER_SERVICES := db node-exporter

.PHONY: \
	up down ps logs \
	up-local down-local ps-local logs-local build-local \
	pull-dev up-dev down-dev ps-dev logs-dev prune-dev deploy-dev \
	pull-app-dev up-app-dev down-app-dev ps-app-dev logs-app-dev deploy-app-dev \
	pull-db-dev up-db-dev down-db-dev ps-db-dev logs-db-dev deploy-db-dev \
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
	$(COMPOSE) $(LOCAL_FILES) up --build --remove-orphans $(LOCAL_SERVICES)

down-local:
	$(COMPOSE) $(LOCAL_FILES) down --remove-orphans

ps-local:
	$(COMPOSE) $(LOCAL_FILES) ps $(LOCAL_SERVICES)

logs-local:
	$(COMPOSE) $(LOCAL_FILES) logs -f app db

build-local:
	$(COMPOSE) $(LOCAL_FILES) build $(LOCAL_SERVICES)

pull-dev:
	$(COMPOSE) $(APP_FILES) pull app node-exporter

up-dev:
	$(COMPOSE) $(APP_FILES) up -d --remove-orphans

down-dev:
	$(COMPOSE) $(APP_FILES) down

ps-dev:
	$(COMPOSE) $(APP_FILES) ps

logs-dev:
	$(COMPOSE) $(APP_FILES) logs -f $(APP_SERVER_SERVICES)

prune-dev:
	docker image prune -f
	docker builder prune -f

deploy-dev: deploy-app-dev

pull-app-dev:
	$(COMPOSE) $(APP_FILES) pull app node-exporter

up-app-dev:
	$(COMPOSE) $(APP_FILES) up -d --remove-orphans

down-app-dev:
	$(COMPOSE) $(APP_FILES) down

ps-app-dev:
	$(COMPOSE) $(APP_FILES) ps

logs-app-dev:
	$(COMPOSE) $(APP_FILES) logs -f $(APP_SERVER_SERVICES)

deploy-app-dev: pull-app-dev up-app-dev prune-dev

pull-db-dev:
	$(COMPOSE) $(DB_FILES) pull db node-exporter

up-db-dev:
	$(COMPOSE) $(DB_FILES) up -d --remove-orphans

down-db-dev:
	$(COMPOSE) $(DB_FILES) down

ps-db-dev:
	$(COMPOSE) $(DB_FILES) ps

logs-db-dev:
	$(COMPOSE) $(DB_FILES) logs -f $(DB_SERVER_SERVICES)

deploy-db-dev: pull-db-dev up-db-dev prune-dev

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
