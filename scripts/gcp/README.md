# GCP scripts

폴더명 앞 숫자는 권장 실행 순서입니다. 같은 폴더 안에서도 파일명 앞 숫자를 따라 실행합니다.

```text
00_env/              환경 파일 예시와 로컬 runtime env(gitignore)
01_infra/            GCE/GCS/IAM/firewall 인프라 생성 및 출력
02_credentials/      GCS service account key/base64 credential 생성
03_env_render/       App/DB 서버용 .env 렌더링
04_db/               DB VM bootstrap/install
05_app/              App VM bootstrap/install
06_observability/    Cloud Logging/Monitoring alert 구성
07_deploy/           env 하나로 infra→DB→App 순차 자동화
```

빠른 dev 순서:

```bash
cp scripts/gcp/00_env/dev.env.example scripts/gcp/00_env/dev.env
vi scripts/gcp/00_env/dev.env
scripts/gcp/01_infra/01_provision-gcp.sh scripts/gcp/00_env/dev.env
scripts/gcp/03_env_render/00_render-server-env.sh scripts/gcp/00_env/dev.env db > scripts/gcp/00_env/dev.db.env
scripts/gcp/03_env_render/00_render-server-env.sh scripts/gcp/00_env/dev.env app > scripts/gcp/00_env/dev.app.env
chmod 600 scripts/gcp/00_env/dev.db.env scripts/gcp/00_env/dev.app.env
# edit scripts/gcp/00_env/dev.db.env and scripts/gcp/00_env/dev.app.env secrets
```

prod 전체 순차 실행은 root의 `DEPLOY.md` 또는 다음 wrapper를 사용합니다.

```bash
scripts/gcp/07_deploy/00_deploy-env.sh scripts/gcp/00_env/prod.env
# generated prod.app.env/prod.db.env 편집 후
RENDER_ENVS=false scripts/gcp/07_deploy/00_deploy-env.sh scripts/gcp/00_env/prod.env
```

전체 runbook은 repository root의 `DEPLOY.md`를 봅니다.
