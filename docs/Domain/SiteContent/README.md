# Site Content Domain

Issue: [#118](https://github.com/Geumjeongyahak/Backend/issues/118)

공개 사이트에 표시되는 편집 가능한 콘텐츠를 관리하는 도메인입니다. API 경로는 프론트 계약에 맞춰 `/api/v1/site-contents/**`를 유지하지만, 내부 저장 모델과 테이블 이름은 `site_content`를 기준으로 둡니다.

## Scope

- 기관 소개 페이지의 교장/부서 소개 콘텐츠
- 반 소개 페이지의 주중반/주말 오전반/주말 오후반 콘텐츠
- 연혁 페이지의 항목 및 사진 URL 콘텐츠
- 관리자 항목별 생성, 수정, 삭제
- 비로그인 공개 조회

## Model

### `site_contents`

| Column | Purpose |
|--------|---------|
| `content_type` | `PRINCIPAL`, `DEPARTMENT`, `CLASSROOM` |
| `ref_id` | 기존 `departments.id` 또는 `classrooms.id`와 연결할 때 사용 |
| `title` | 부서명, 교장 직책, 반 이름 |
| `name` | 교장 이름처럼 title과 별도로 표시할 이름 |
| `content_group` | 반 그룹: `WEEKDAY`, `WEEKEND_MORNING`, `WEEKEND_AFTERNOON` |
| `sort_order` | 표시 순서 |

### `site_content_items`

줄바꿈 표시용 문자열 배열을 저장합니다.

- 부서/교장: `responsibilities[]`
- 반: `description[]`

### `site_histories`

연혁 페이지의 개별 항목을 저장합니다.

| Column | Purpose |
|--------|---------|
| `title` | 연혁 제목. 공개 응답의 필수 필드 |
| `detail` | 상세 설명 |
| `link_label` | 링크 표시 텍스트 |
| `link_href` | 링크 URL |
| `sort_order` | 표시 순서 |

### `site_history_photos`

연혁 항목의 사진 URL 목록을 저장합니다.

| Column | Purpose |
|--------|---------|
| `history_id` | `site_histories.id` |
| `file_id` | 업로드 파일 ID. 삭제/교체 side effect 추적용 |
| `src` | 이미지 URL |
| `alt` | 이미지 설명 |
| `sort_order` | 표시 순서 |

## Principal Policy

교장은 인증 Role이 아니라 공개 페이지 표시 콘텐츠입니다.

- `RoleType.PRINCIPAL`은 추가하지 않습니다.
- `ADMIN` 권한과 교장 직책을 섞지 않습니다.
- 교장 카드는 `site_contents.content_type = PRINCIPAL`로 구분합니다.
- 실제 교장 계정 권한이 필요해지면 별도 auth/permission 이슈에서 다룹니다.

## API Facade

프론트가 이미 기관 정보 섹션으로 호출하므로 API 경로는 유지합니다.

- `GET /api/v1/site-contents/departments`
- `GET /api/v1/site-contents/classes`
- `GET /api/v1/site-contents/history`
- `POST /api/v1/site-contents/departments`
- `PUT /api/v1/site-contents/departments/{contentId}`
- `DELETE /api/v1/site-contents/departments/{contentId}`
- `POST /api/v1/site-contents/classes`
- `PUT /api/v1/site-contents/classes/{contentId}`
- `DELETE /api/v1/site-contents/classes/{contentId}`
- `POST /api/v1/site-contents/history`
- `PUT /api/v1/site-contents/history/{historyId}`
- `DELETE /api/v1/site-contents/history/{historyId}`
- `POST /api/v1/files/images/site-contents`

REST 요청/응답은 프론트 렌더링 계약을 우선합니다.

- 교장 데이터가 없더라도 `principal`은 null이 아니라 `{ "id": 0, "title": "교장", "name": "", "responsibilities": [] }`로 반환합니다.
- 부서 관리 요청은 `{ title, name, responsibilities }`만 받습니다.
- 반 관리 요청은 `{ groupId, name, description }`만 받습니다.
- `groupId`는 `weekday`, `weekendMorning`, `weekendAfternoon`입니다.
- 연혁 관리 요청은 `{ title, detail, linkLabel, linkHref, photos }`만 받습니다.
- 연혁 사진은 URL-only로 저장할 수 있지만, GCS 정리를 위해 업로드 응답의 `fileId`를 `photos[].fileId`로 함께 보내는 것을 권장합니다.
- 연혁 수정에서 빠진 사진 파일과 연혁 삭제 시 연결된 사진 파일은 soft delete 처리됩니다.
- `contentType`, `refId`, `sortOrder`는 REST 요청 DTO에서 제외합니다.
- 내부 관리자 Thymeleaf 화면은 운영 보정용으로 타입, ref, 정렬 값을 관리할 수 있습니다.

## Admin Pages

- `/admin/site-content/contents`: 교장, 부서, 반 소개 콘텐츠 관리
- `/admin/site-content/history`: 연혁 콘텐츠 관리
