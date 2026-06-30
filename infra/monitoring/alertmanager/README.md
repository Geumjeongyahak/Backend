# Alertmanager

The live config is project-owned:

```text
/home/min/Infra/monitoring/alertmanager/projects/gjlearn/alertmanager.yml
```

Put GJLearn Discord webhook URLs in `infra/monitoring/monitoring.env`, then run:

```bash
make apply-monitoring-alert-env
make apply-monitoring-alert-env-central
```

The rendered secret files are:

```text
/home/min/Infra/monitoring/secrets/alertmanager/gjlearn/default/discord/default
/home/min/Infra/monitoring/secrets/alertmanager/gjlearn/dev/discord/api
/home/min/Infra/monitoring/secrets/alertmanager/gjlearn/dev/discord/app
/home/min/Infra/monitoring/secrets/alertmanager/gjlearn/dev/discord/db
/home/min/Infra/monitoring/secrets/alertmanager/gjlearn/dev/discord/postgres
/home/min/Infra/monitoring/secrets/alertmanager/gjlearn/prod/discord/api
/home/min/Infra/monitoring/secrets/alertmanager/gjlearn/prod/discord/app
/home/min/Infra/monitoring/secrets/alertmanager/gjlearn/prod/discord/db
/home/min/Infra/monitoring/secrets/alertmanager/gjlearn/prod/discord/postgres
```

`default` catches alerts without an env/service route. Slack can be added in this same project config when needed. Keep secrets out of git and restart the Quadlet stack.
