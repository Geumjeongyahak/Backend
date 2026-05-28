# File API

파일 API는 GCS 업로드 파일과 Google Drive 파일 메타데이터를 모두 `files` 테이블에 기록하고, 게시글 첨부에서 사용할 `fileId`를 제공합니다.

## GCS 첨부파일 업로드

- **URL**: `POST /api/v1/files/attachments`
- **권한**: 인증 필요
- **Content-Type**: `multipart/form-data`
- **동작**: 백엔드가 파일을 GCS에 업로드하고 `files` 레코드를 생성합니다.
- **응답**: `FileUploadResponse`

## Google Drive 파일 등록

- **URL**: `POST /api/v1/files/drive`
- **권한**: 인증 필요
- **Content-Type**: `application/json`
- **동작**: 프론트가 Google Drive API로 직접 업로드한 뒤 전달한 URL과 메타데이터만 `files` 테이블에 기록합니다.
- **주의**: 백엔드는 Drive 클라이언트 라이브러리를 사용하지 않고, 실제 파일 업로드/삭제도 수행하지 않습니다.
- **저장 기준**: `files.is_google_drive=true`, `public_url=driveUrl`로 저장합니다.

### Request

```json
{
  "driveUrl": "https://drive.google.com/file/d/abc123/view?usp=sharing",
  "originalName": "2026 자료집.pdf",
  "mimeType": "application/pdf",
  "fileSize": 204800
}
```

### Response

```json
{
  "fileId": "550e8400-e29b-41d4-a716-446655440000",
  "originalName": "2026 자료집.pdf",
  "contentType": "application/pdf",
  "fileSize": 204800,
  "ext": "pdf",
  "isGoogleDrive": true,
  "url": "https://drive.google.com/file/d/abc123/view?usp=sharing"
}
```

## 게시글 첨부 연결

- **URL**: `POST /api/v1/channels/{channelId}/posts/{postId}/attachments`
- **Content-Type**: `application/json`
- **Body**: `{ "fileId": "...", "sortOrder": 0 }`
- GCS 업로드로 받은 `fileId`와 Drive 등록으로 받은 `fileId`를 동일하게 사용할 수 있습니다.

## 다운로드 URL 조회

- **URL**: `GET /api/v1/files/attachments/{fileId}/download-url`
- GCS 파일은 signed URL을 반환합니다.
- Google Drive 파일은 등록된 `driveUrl`을 그대로 반환합니다.
