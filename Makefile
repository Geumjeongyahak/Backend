COMPOSE := docker compose
BASE_COMPOSE := -f docker-compose.yml
LOCAL_COMPOSE := -f docker-compose.yml -f docker-compose.local.yml

.PHONY: up up-local down down-local logs logs-local ps ps-local build-local push

up:
	$(COMPOSE) $(BASE_COMPOSE) up -d

up-local:
	$(COMPOSE) $(LOCAL_COMPOSE) up --build

build-local:
	$(COMPOSE) $(LOCAL_COMPOSE) build

down:
	$(COMPOSE) $(BASE_COMPOSE) down

down-local:
	$(COMPOSE) $(LOCAL_COMPOSE) down

logs:
	$(COMPOSE) $(BASE_COMPOSE) logs -f app

logs-local:
	$(COMPOSE) $(LOCAL_COMPOSE) logs -f app db

ps:
	$(COMPOSE) $(BASE_COMPOSE) ps

ps-local:
	$(COMPOSE) $(LOCAL_COMPOSE) ps
push:
	docker push ghcr.io/geumjeongyahak/backend:latest