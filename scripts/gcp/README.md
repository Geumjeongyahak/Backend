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
scripts/gcp/03_env_render/01_configure-preinfra-env.sh scripts/gcp/00_env/dev.env
scripts/gcp/01_infra/01_provision-gcp.sh scripts/gcp/00_env/dev.env
scripts/gcp/03_env_render/02_configure-runtime-env.sh scripts/gcp/00_env/dev.env --local-only
# 서버에 반영할 때:
scripts/gcp/03_env_render/02_configure-runtime-env.sh scripts/gcp/00_env/dev.env --apply-all
```

설정 단계 구분:

- 서버 생성 전: `01_configure-preinfra-env.sh`
  - GCP project/region/zone/network, App/DB VM 이름/머신/디스크/IP, 방화벽 CIDR,
    API/프론트 도메인, GCS bucket/service account, 배포 OS user/path, Tailscale tag,
    Cloud Logging 리소스 이름을 `scripts/gcp/00_env/<env>.env`에 대화형으로 기록합니다.
- 서버 생성 후 또는 install 직전/이후: `02_configure-runtime-env.sh`
  - `scripts/gcp/00_env/<env>.app.env`와 `<env>.db.env`를 렌더링/수정합니다.
  - DB 계정/암호, App DB 접속값, MailerSend SMTP, JWT/JWE, Firebase, Google OAuth,
    Google Drive 업로드 폴더,
    CORS/redirect, Admin bootstrap, Tailscale auth key를 설정합니다.
  - DB password, JWT/JWE, Admin password 등은 기존 값이 비어 있거나 `CHANGE_ME`이면
    Enter만 눌러도 랜덤값을 생성합니다. 이미 값이 있으면 Enter는 기존 값을 유지합니다.
  - `--apply-app`, `--apply-db`, `--apply-all` 모드는 원격 VM의 `~/app-dev/.env`/`~/db-dev/.env`를
    갱신하고 가능한 경우 서비스를 재시작/재설치합니다.
- App runtime 값이 많을 때는 `03_configure-app-runtime-section.sh`로 나눠서 설정합니다.
  - 섹션: `frontend`, `db`, `security`, `mailersend`, `oauth`, `firebase`, `gcp-storage`, `google-drive`, `logging`, `tailscale`, `all`
  - 예: `scripts/gcp/03_env_render/03_configure-app-runtime-section.sh scripts/gcp/00_env/dev.env mailersend`
  - Firebase/GCP credential은 JSON 파일 경로를 입력하면 자동으로 one-line base64로 인코딩해 env에 저장합니다.
  - Google OAuth는 client JSON 경로를 입력하면 `GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET`을 가져올 수 있습니다.
  - Google Drive 업로드는 로그인/회원가입용 OAuth와 별도 client를 권장하며, OAuth Playground에서 발급한 refresh token을 `google-drive` 섹션에 입력합니다.

prod 전체 순차 실행은 root의 `DEPLOY.md` 또는 다음 wrapper를 사용합니다.

```bash
scripts/gcp/07_deploy/00_deploy-env.sh scripts/gcp/00_env/prod.env
# generated prod.app.env/prod.db.env 편집 후
RENDER_ENVS=false scripts/gcp/07_deploy/00_deploy-env.sh scripts/gcp/00_env/prod.env
```

전체 runbook은 repository root의 `DEPLOY.md`를 봅니다.
