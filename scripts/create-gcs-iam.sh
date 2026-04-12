#!/usr/bin/env bash
set -euo pipefail

# 사용법: ./create-gcs-iam.sh <PROJECT_ID>
PROJECT_ID="${1:?PROJECT_ID 인자가 필요합니다}"

SERVICE_ACCOUNT="storage-service-account@${PROJECT_ID}.iam.gserviceaccount.com"
KEY_DIR=".secret"
KEY_FILE="${KEY_DIR}/service-account.json"

# ────────────────────────────────────────────────
# 1. 프로젝트 레벨 역할 부여
# ────────────────────────────────────────────────
echo "[1/3] 프로젝트 IAM 역할 부여"

# GCS 객체 CRUD 권한
gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
  --member="serviceAccount:${SERVICE_ACCOUNT}" \
  --role="roles/storage.objectAdmin"

# Signed URL 생성을 위한 토큰 발급 권한
gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
  --member="serviceAccount:${SERVICE_ACCOUNT}" \
  --role="roles/iam.serviceAccountTokenCreator"

echo ""
echo "=== 현재 프로젝트 IAM 정책 ==="
gcloud projects get-iam-policy "${PROJECT_ID}"

# ────────────────────────────────────────────────
# 2. JSON 키 발급 (.secret/ 디렉터리에 저장)
# ────────────────────────────────────────────────
echo ""
echo "[2/3] 서비스 계정 키 발급 → ${KEY_FILE}"
mkdir -p "${KEY_DIR}"

gcloud iam service-accounts keys create "${KEY_FILE}" \
  --iam-account="${SERVICE_ACCOUNT}"

echo "키 파일이 ${KEY_FILE} 에 저장되었습니다. (.gitignore 적용됨)"

# ────────────────────────────────────────────────
# 3. 발급된 키 목록 확인
# ────────────────────────────────────────────────
echo ""
echo "[3/3] 발급된 키 목록"
gcloud iam service-accounts keys list \
  --iam-account="${SERVICE_ACCOUNT}"