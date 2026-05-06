# GCE 배포 및 SSH 설정 가이드

이 문서는 `gcloud` CLI를 사용하여 Google Compute Engine(GCE)의 접속 정보를 확인하고, GitHub Actions 배포를 위한 SSH 키 설정 및 시크릿 관리 방법을 설명합니다.

---

## 1. GCE 접속 정보 확인 (gcloud)

GCP 프로젝트가 설정된 로컬 환경에서 아래 명령어를 사용하여 정보를 확인합니다.

### 1.1 인스턴스 목록 및 IP(Host) 확인
배포 대상 인스턴스의 `EXTERNAL_IP`가 GitHub Secrets의 **`GCE_HOST`**가 됩니다.
```bash
gcloud compute instances list
```

### 1.2 현재 SSH 사용자(User) 확인
GCE에 접속할 때 사용하는 기본 사용자 이름은 보통 로컬 시스템의 계정명 또는 GCP 계정 이메일의 앞부분입니다. GitHub Secrets의 **`GCE_USER`**에 해당합니다.
```bash
# SSH 접속을 시도하여 OS 상의 사용자 이름을 확인합니다.
gcloud compute ssh [INSTANCE_NAME] --command="whoami"
```

---

## 2. SSH 키 생성 및 인스턴스 등록

GitHub Actions가 GCE에 비밀번호 없이 접속하려면 전용 SSH 키 쌍이 필요합니다.

### 2.1 SSH 키 쌍 생성
로컬에서 배포 전용 키를 생성합니다. (비밀번호/Passphrase는 비워둡니다.)
```bash
ssh-keygen -t rsa -b 4096 -f ./gce-deploy-key -C "github-actions-deploy"
```
*   `gce-deploy-key`: Private Key (내용을 복사하여 GitHub **`GCE_SSH_KEY`**에 등록)
*   `gce-deploy-key.pub`: Public Key (GCE 인스턴스에 등록)

### 2.2 Public Key를 GCE 인스턴스에 추가
생성한 공개키(`.pub`)를 GCE 메타데이터에 등록하여 접속을 허용합니다.

**방법 A: gcloud 명령어로 추가 (추천)**
```bash
# 기존 메타데이터에 공개키 추가
gcloud compute instances add-metadata [INSTANCE_NAME] \
    --metadata-from-file ssh-keys=<(echo "[GCE_USER]:$(cat gce-deploy-key.pub)")
```

**방법 B: GCP 콘솔에서 추가**
1.  [GCE 인스턴스 상세 페이지]로 이동
2.  [수정(Edit)] 클릭
3.  [SSH 키] 항목에서 [항목 추가] 클릭
4.  `gce-deploy-key.pub` 파일의 내용 전체를 붙여넣기 후 저장

---

## 3. GitHub Secrets 설정 가이드

GitHub 리포지토리의 **Settings > Secrets and variables > Actions**에 아래 항목들을 정확히 등록합니다.

| 이름 | 내용 설명 | 확인 방법 |
| :--- | :--- | :--- |
| **`GCE_HOST`** | GCE 인스턴스의 외부 IP | `gcloud compute instances list` |
| **`GCE_USER`** | SSH 접속 계정명 | `gcloud compute ssh ... --command="whoami"` |
| **`GCE_SSH_KEY`** | 생성한 Private Key 전문 | `cat gce-deploy-key` (내용 전체) |
| **`GHCR_TOKEN`** | GitHub Personal Access Token | [GitHub PAT 설정](https://github.com/settings/tokens) (Classic 권한: `read:packages`, `write:packages`) |
| **`GCE_ENV_DEV`** | 애플리케이션 `.env` 파일 내용 | `.env-example` 참고하여 dev용 값 작성 |

---

## 4. 트러블슈팅

### 4.1 SSH 접속 권한 오류
GitHub Actions 실행 중 `Permission denied (publickey)` 오류가 발생한다면:
1.  `GCE_SSH_KEY` 시크릿에 Private Key가 정확히(줄바꿈 포함) 입력되었는지 확인합니다.
2.  GCE 인스턴스에 `GCE_USER`와 매칭되는 Public Key가 정상적으로 등록되었는지 확인합니다.

### 4.2 GHCR 이미지 Pull 오류
GCE 서버에서 `docker compose pull` 시 권한 오류가 발생한다면:
1.  `GHCR_TOKEN`의 권한에 `read:packages`가 포함되어 있는지 확인합니다.
2.  서버에서 직접 `docker login ghcr.io`를 수행하여 정상 로그인되는지 테스트합니다.
