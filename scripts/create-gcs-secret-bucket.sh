#!/usr/bin/env bash
set -euo pipefail

# 사용법: ./create-gcs-secret-bucket.sh <PROJECT_ID> <BUCKET_NAME>
# 용도: 첨부파일 등 Signed URL로만 접근하는 비공개 버킷
PROJECT_ID="${1:?PROJECT_ID 인자가 필요합니다}"
BUCKET_NAME="${2:?BUCKET_NAME 인자가 필요합니다}"

SERVICE_ACCOUNT="storage-service-account@${PROJECT_ID}.iam.gserviceaccount.com"

echo "[secret] 버킷 생성: gs://${BUCKET_NAME}"
gcloud storage buckets create "gs://${BUCKET_NAME}" \
  --project="${PROJECT_ID}" \
  --location=asia-northeast3 \
  --uniform-bucket-level-access

# allUsers 접근 없음 — 서비스 계정에만 권한 부여 (Signed URL 생성 포함)
gcloud storage buckets add-iam-policy-binding "gs://${BUCKET_NAME}" \
  --member="serviceAccount:${SERVICE_ACCOUNT}" \
  --role="roles/storage.objectAdmin"

echo ""
echo "=== [secret] 버킷 IAM ==="
gcloud storage buckets get-iam-policy "gs://${BUCKET_NAME}"