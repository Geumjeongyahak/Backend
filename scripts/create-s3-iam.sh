# 인자를 받아서 서비스 계정 생성
PROJECT_ID=$1

# 프로젝트 레벨 역할 부여
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:storage-service-account@$PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/storage.objectAdmin"

# Google Workspace API 사용 위해 추가 역할 부여
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:storage-service-account@$PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/iam.serviceAccountTokenCreator"

# 현재 IAM 정책 확인
gcloud projects get-iam-policy $PROJECT_ID


# JSON 키 발급
gcloud iam service-accounts keys create ./service-account.json \
  --iam-account=storage-service-account@$PROJECT_ID.iam.gserviceaccount.com

# 발급된 키 목록 확인
gcloud iam service-accounts keys list \
  --iam-account=storage-service-account@$PROJECT_ID.iam.gserviceaccount.com

