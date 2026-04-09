BUCKET_NAME="your-bucket-name"

# 버킷 생성 (서울 리전)
gcloud storage buckets create gs://$BUCKET_NAME \
  --location=asia-northeast3 \
  --uniform-bucket-level-access

# 버킷 목록 확인
gcloud storage buckets list

# 퍼블릭 접근 허용 (이미지 버킷)
gcloud storage buckets add-iam-policy-binding gs://$BUCKET_NAME \
  --member="allUsers" \
  --role="roles/storage.objectViewer"

# 버킷 레벨 IAM 확인
gcloud storage buckets get-iam-policy gs://$BUCKET_NAME

# 버킷 권한 수정(읽기 권한 allUsers)
gcloud storage buckets add-iam-policy-binding gs://$BUCKET_NAME \
  --member="allUsers" \
  --role="roles/storage.objectViewer"