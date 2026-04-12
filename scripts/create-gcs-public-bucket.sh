#!/usr/bin/env bash
set -euo pipefail

# 사용법: ./create-gcs-public-bucket.sh <PROJECT_ID> <BUCKET_NAME>
# 용도: 이미지 등 퍼블릭 읽기가 필요한 객체 전용 버킷
PROJECT_ID="${1:?PROJECT_ID 인자가 필요합니다}"
BUCKET_NAME="${2:?BUCKET_NAME 인자가 필요합니다}"

SERVICE_ACCOUNT="storage-service-account@${PROJECT_ID}.iam.gserviceaccount.com"

echo "[public] 버킷 생성: gs://${BUCKET_NAME}"
gcloud storage buckets create "gs://${BUCKET_NAME}" \
  --project="${PROJECT_ID}" \
  --location=asia-northeast3 \
  --uniform-bucket-level-access

# allUsers 읽기 허용 (CDN/직접 URL 접근용)
gcloud storage buckets add-iam-policy-binding "gs://${BUCKET_NAME}" \
  --member="allUsers" \
  --role="roles/storage.objectViewer"

# 서비스 계정에 업로드/삭제 권한 부여
gcloud storage buckets add-iam-policy-binding "gs://${BUCKET_NAME}" \
  --member="serviceAccount:${SERVICE_ACCOUNT}" \
  --role="roles/storage.objectAdmin"

echo ""
echo "=== [public] 버킷 IAM ==="
gcloud storage buckets get-iam-policy "gs://${BUCKET_NAME}"