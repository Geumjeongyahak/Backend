# Post API

게시글은 특정 채널에 귀속됩니다.  
이 문서는 게시글 초안(`DRAFT`), 발행(`PUBLISHED`), 이미지/첨부 연동 및 컨트롤러 분리 구조를 반영한 최신 API 문서입니다.

## 1. 역할과 범위

- **PostQueryController**: 게시글 목록 및 상세 조회
- **PostDraftController**: 초안 생명주기 관리 (생성/임시저장/파일연동)
- **PostPublishController**: 초안 발행
- **PostWriteController**: 게시글 직접 생성/수정/삭제 및 상단 고정(Pin)

## 2. 핵심 규칙

### 2.1 게시글 상태 (`PostStatus`)

| 값 | 의미 |
|---|---|
| `DRAFT` | 작성 중인 초안 상태. 목록에 노출되지 않으며 만료 시각(`expiresAt`)이 존재함. |
| `PUBLISHED` | 공개된 게시글 상태. 목록 및 상세 조회 대상. |
| `ARCHIVED` | 보관 상태. 일반 목록에서 제외됨. |

### 2.2 초안 및 발행 흐름

1. `POST /drafts`: 빈 초안 생성 (ID 발급)
2. `PUT /{postId}/draft`: 제목, 본문 등 임시 저장
3. `POST /{postId}/draft/images`: 이미지 업로드 및 연동
4. `POST /{postId}/draft/attachments`: 첨부파일 업로드 및 연동
5. `PUT /{postId}/publish`: 최종 발행 (상태가 `PUBLISHED`로 변경됨)

### 2.3 썸네일 자동 설정 정책

- 발행 시 `thumbnailUrl`을 명시하지 않으면, 연동된 이미지(`post_files`) 중 순서가 가장 빠른 이미지의 URL이 자동으로 설정됩니다.

### 2.4 상단 고정 (`isPinned`)

- 채널 관리 권한(`manage`)이 있는 사용자만 설정 가능하며, 별도의 `/pin` 엔드포인트로 관리됩니다.

## 3. 권한 정책

| API | 권한 | 설명 |
|---|---|---|
| 조회 (Query) | `channel.can('read')` | 채널 읽기 권한 필요. `allowGuestRead=true` 채널은 비로그인 조회 가능 |
| 초안/발행 (Draft/Publish) | `channel.can('write')` + 소유권 | 작성 권한 및 본인 글 확인 |
| 수정/삭제 (Write) | `channel.can('manage')` or 소유권 | 관리자 또는 본인 |
| 고정 (Pin) | `channel.can('manage')` | 관리자 전용 |

## 4. 엔드포인트

### 4.1 초안 관리 (`PostDraftController`)

#### 4.1.1 초안 생성
- **URL**: `POST /api/v1/channels/{channelId}/posts/drafts`
- **응답**: `201 Created` (PostDetailResponse)

#### 4.1.2 초안 임시 저장
- **URL**: `PUT /api/v1/channels/{channelId}/posts/{postId}/draft`
- **Body**: `SaveDraftRequest`
  ```json
  {
    "title": "임시 제목",
    "contentHtml": "<p>임시 본문</p>",
    "allowComment": true,
    "thumbnailUrl": null
  }
  ```

#### 4.1.3 초안 이미지/첨부 연동
- **이미지**: `POST /api/v1/channels/{channelId}/posts/{postId}/draft/images` (Multipart)
- **첨부**: `POST /api/v1/channels/{channelId}/posts/{postId}/draft/attachments` (Multipart)

### 4.2 발행 (`PostPublishController`)

#### 4.2.1 초안 발행
- **URL**: `PUT /api/v1/channels/{channelId}/posts/{postId}/publish`
- **Body**: `PublishPostRequest`
  ```json
  {
    "title": "최종 제목 (필수)",
    "contentHtml": "<p>최종 본문 (필수)</p>",
    "allowComment": true,
    "thumbnailUrl": null
  }
  ```

### 4.3 게시글 관리 (`PostWriteController`)

#### 4.3.1 게시글 직접 생성 (초안 미사용 시)
- **URL**: `POST /api/v1/channels/{channelId}/posts`
- **Body**: `CreatePostRequest`

#### 4.3.2 게시글 수정
- **URL**: `PUT /api/v1/channels/{channelId}/posts/{postId}`
- **Body**: `UpdatePostRequest`

#### 4.3.3 상단 고정 설정
- **URL**: `PUT /api/v1/channels/{channelId}/posts/{postId}/pin`
- **Body**: `PinPostRequest` (`{ "isPinned": true }`)

#### 4.3.4 게시글 삭제
- **URL**: `DELETE /api/v1/channels/{channelId}/posts/{postId}`

### 4.4 조회 (`PostQueryController`, `PostBoardController`)

#### 4.4.1 채널 내 목록 조회
- **URL**: `GET /api/v1/channels/{channelId}/posts`

#### 4.4.2 게시글 상세 조회
- **URL**: `GET /api/v1/channels/{channelId}/posts/{postId}`
- `allowGuestRead=true` 채널의 발행 게시글은 비로그인 사용자도 조회할 수 있습니다.

#### 4.4.3 통합 게시판 조회
- **URL**: `GET /api/v1/posts`
