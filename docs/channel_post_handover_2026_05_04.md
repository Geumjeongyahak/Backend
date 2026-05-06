# Channel/Post 변경 인수인계서

최종 수정일: `2026-05-04` (세션 2)  
대상 범위: `channel`, `post`, `file` 도메인 중심의 채널/게시글 구조 개편

---

## 1. 전체 작업 목표

회의 변경사항 기준으로 아래를 단계적으로 반영하는 것이 목적이다.

- 자료실 게시판 추가 (`RESOURCE` 채널 타입)
- `post_type` 제거
- `thumbnail_url` 추가, 발행 시 자동 설정
- `SYSTEM_MANAGED` 개념 제거, `binding_type` 기반 구조로 전환
- 게시글 초안(`DRAFT`) 기반 작성 플로우 도입
- 초안 생성 → 임시 저장 → 파일 연동 → 발행 흐름 완전 분리
- 컨트롤러를 역할별로 분리 (조회/초안/수정/발행)
- 권한 체계 정비 (pin은 manage 권한 전용 endpoint)

---

## 2. 완료된 사항 전체 목록

### 2.1 SQL / Seed

- `channels.management_mode` 제거, `channels.binding_type` 추가
- `ChannelType.RESOURCE` 시드 채널 반영
- `posts.post_type` 제거
- `posts.thumbnail_url`, `posts.expires_at` 추가
- `post_files`, `post_attachments` 테이블 추가
- `files.public_url` 추가
- 테스트 시드 채널을 `NOTICE`, `EVENT`, `RESOURCE`, `CLASSROOM`, `DEPARTMENT` 구조로 재정렬

### 2.2 Channel 도메인

- `managementMode` → `bindingType` 전환 (`STANDALONE`, `DOMAIN_LINKED`)
- `RESOURCE` 채널 타입 추가
- `ensureResourceChannel()` 추가
- seed 채널 = `STANDALONE`, 분반/부서 채널 = `DOMAIN_LINKED`로 분리
- 기존 버그 수정: `ensureEventChannel()`이 `NOTICE`를 잘못 조회하던 문제

### 2.3 Post 엔티티/서비스

- `postType` 필드 제거
- `thumbnailUrl`, `expiresAt` 추가
- `publish()`, `archive()`, `updateDraftExpiration()` 도메인 메서드 추가
- `Post`에 `postAttachments @OneToMany(LAZY)` + `@BatchSize(20)` 추가
- 검색 스펙에서 `postType` 제거
- 채널 `lastPostedAt`는 `PUBLISHED` 글만 기준으로 재계산

### 2.4 Draft 정책 설정

- `app.post.draft-expiration-minutes=60` (application.yml)
- `PostProperties.java` - 설정값 바인딩
- 서버가 만료 시각 자동 계산, 클라이언트는 `expiresAt`을 통제하지 않음

### 2.5 File 도메인

- `File.publicUrl` 필드 추가 (`files.public_url` SQL과 일치)
- `ImageUploadService.saveFile()` → `publicUrl(storedFile.url())` 저장
- `AttachmentUploadService.uploadAttachment()` → `publicUrl(storedFile.url())` 저장

### 2.6 신규 endpoint 전체 목록

모두 `/api/v1/channels/{channelId}/posts` 하위.

| Method | Path | 컨트롤러 | 권한 | 설명 |
|--------|------|----------|------|------|
| `POST` | `/drafts` | `PostDraftController` | `channelAccess.can('write')` | 초안 생성 |
| `PUT` | `/{postId}/draft` | `PostDraftController` | `channelAccess.can('write')` | 초안 임시 저장 |
| `POST` | `/{postId}/draft/images` | `PostDraftController` | `channelAccess.can('write')` | 이미지 업로드 + 연동 (multipart) |
| `POST` | `/{postId}/draft/attachments` | `PostDraftController` | `channelAccess.can('write')` | 첨부파일 업로드 + 연동 (multipart) |
| `PUT` | `/{postId}/publish` | `PostPublishController` | `channelAccess.can('write')` | 초안 발행 |
| `PUT` | `/{postId}/pin` | `PostWriteController` | `channelAccess.can('manage')` | 고정/고정 해제 |
| `POST` | `/` | `PostWriteController` | `channelAccess.can('write')` | 게시글 직접 생성 |
| `PUT` | `/{postId}` | `PostWriteController` | `manage` or `postAccess.can` | 게시글 수정 |
| `DELETE` | `/{postId}` | `PostWriteController` | `manage` or `postAccess.can` | 게시글 삭제 |
| `GET` | `/` | `PostQueryController` | `channelAccess.can('read')` | 게시글 목록 |
| `GET` | `/{postId}` | `PostQueryController` | `channelAccess.can('read')` | 게시글 상세 |

### 2.7 컨트롤러 분리

기존 `PostController` → 4개로 분리. 모두 `@Tag(name = "Post")`로 Swagger에서 통합 표시.

- `PostQueryController` — 조회
- `PostDraftController` — 초안 라이프사이클 (생성/저장/파일연동)
- `PostWriteController` — 수정/삭제/pin
- `PostPublishController` — 발행

### 2.8 주요 정책 변경

- **isPinned**: `UpdatePostRequest`에서 제거. `PUT /{postId}/pin` 전용 endpoint로 분리, `manage` 권한 필요
- **thumbnailUrl 자동 설정**: 발행 시 `thumbnailUrl` 미전달 → `post_files`에서 `sortOrder` 가장 낮은 이미지의 `publicUrl` 자동 적용
- **images 응답 제외**: `PostDetailResponse`에서 이미지 목록 제거 (이미지는 `contentHtml`에서 렌더링). 첨부파일(`attachments`)만 응답에 포함
- **N+1 방지**: `PostRepository.findWithAttachmentsByIdAndChannelId()` — `@EntityGraph({"channel", "author", "postAttachments", "postAttachments.file"})` + `@Query`로 단일 JOIN 쿼리. 상세 응답이 필요한 모든 서비스 메서드에서 사용

### 2.9 DTO 변경 요약

| DTO | 변경 내용 |
|-----|-----------|
| `CreatePostRequest` | `expiresAt` 제거 |
| `UpdatePostRequest` | `expiresAt` 제거, `isPinned` 제거 |
| `SaveDraftRequest` | 신규: `title`, `contentHtml`, `allowComment`, `thumbnailUrl` (null이면 기존값 유지) |
| `PublishPostRequest` | 신규: `title`(필수), `contentHtml`(필수), `allowComment`, `thumbnailUrl` |
| `PinPostRequest` | 신규: `isPinned`(필수) |
| `PostDetailResponse` | `images` 제거, `attachments: List<PostAttachmentInfo>` 추가 |

---

## 3. 기존 구현과 충돌 지점 (테스트 수정 필요)

### 3.1 `BasePostTest.java`

```java
// ❌ 기존 (삭제된 enum)
import geumjeongyahak.domain.channel.enums.ChannelManagementMode;
...
.managementMode(ChannelManagementMode.SYSTEM_MANAGED)

// ✅ 수정 후
import geumjeongyahak.domain.channel.enums.ChannelBindingType;
...
.bindingType(ChannelBindingType.STANDALONE)
```

### 3.2 `PostLifecycleTest.java`

**채널 생성 부분:**
```java
// ❌ 기존
.managementMode(ChannelManagementMode.USER_MANAGED)

// ✅ 수정 후
.bindingType(ChannelBindingType.STANDALONE)
```

**`createPost()` 헬퍼 내 `CreatePostRequest`:**
```java
// ❌ 기존 (6개 인자, 3번째가 postType)
new CreatePostRequest(title, "<p>...</p>", "GENERAL", "PUBLISHED", false, true)

// ✅ 수정 후 (postType 제거, thumbnailUrl 추가)
new CreatePostRequest(title, "<p>...</p>", "PUBLISHED", false, true, null)
```

**`UpdatePostRequest` 사용 부분:**
```java
// ❌ 기존 (6개 인자, postType + isPinned 포함)
new UpdatePostRequest("수정 후 제목", "<p>수정된 본문</p>", "NOTICE", "PUBLISHED", true, false)

// ✅ 수정 후 (postType 제거, isPinned 제거, thumbnailUrl 추가)
new UpdatePostRequest("수정 후 제목", "<p>수정된 본문</p>", "PUBLISHED", false, null)
```

**응답 검증 부분:**
```java
// ❌ 기존 (제거된 필드)
.body("postType", equalTo("NOTICE"))
.body("isPinned", equalTo(true))  // updatePost로 설정 불가

// ✅ 수정 후
// postType 검증 제거
// isPinned 검증은 pin endpoint 호출 후 별도로 확인
```

### 3.3 `PostNestedResourceTest.java`

**`CreatePostRequest` 생성자:**
```java
// ❌ 기존 (postType 포함 6개 인자)
new CreatePostRequest("4월 운영 공지", "<p>...</p>", "NOTICE", "PUBLISHED", true, true)

// ✅ 수정 후
new CreatePostRequest("4월 운영 공지", "<p>...</p>", "PUBLISHED", true, true, null)
```

**응답 검증:**
```java
// ❌ 제거 필요
.body("postType", equalTo("NOTICE"))
```

### 3.4 `ChannelCrudTest.java`

```java
// ❌ 기존 응답 검증 (managementMode 필드 제거됨)
.body("managementMode", equalTo("USER_MANAGED"))

// ✅ 수정 후 (bindingType으로 교체)
.body("bindingType", equalTo("STANDALONE"))
```

---

## 4. 권한 체계 E2E 테스트 계획

`channelAccess` 빈의 `can('write')`, `can('read')`, `can('manage')` 와 `postAccess` 빈의 소유권 확인을 검증한다.

### 4.1 draft 생성 권한

```
POST /api/v1/channels/{channelId}/posts/drafts
```

| 케이스 | 사용자 | 예상 결과 |
|--------|--------|-----------|
| 채널 write 권한 있음 (ADMIN) | ADMIN | 201 |
| 채널 write 권한 없음 (READ_ONLY 채널의 GUEST) | GUEST | 403 |
| 비로그인 | - | 401 |

### 4.2 draft 저장/이미지/첨부 권한

```
PUT  /{postId}/draft
POST /{postId}/draft/images
POST /{postId}/draft/attachments
```

| 케이스 | 예상 결과 |
|--------|-----------|
| 본인 소유 초안 | 200 |
| 타인 소유 초안 | 403 |
| PUBLISHED 상태 게시글 | 400 (DRAFT가 아님) |
| 비로그인 | 401 |

### 4.3 publish 권한

```
PUT /{postId}/publish
```

| 케이스 | 예상 결과 |
|--------|-----------|
| 본인 소유 초안, 제목/본문 있음 | 200 |
| 타인 소유 초안 | 403 |
| 제목 없이 발행 | 400 |
| 이미 PUBLISHED 상태 | 400 |

### 4.4 pin 권한

```
PUT /{postId}/pin
```

| 케이스 | 사용자 | 예상 결과 |
|--------|--------|-----------|
| manage 권한 있음 (ADMIN/MANAGER) | ADMIN | 200 |
| manage 권한 없음 (일반 GUEST) | GUEST | 403 |
| manage 권한 없음 (게시글 작성자, GUEST) | 작성자 GUEST | 403 |

### 4.5 수정/삭제 권한

```
PUT  /{postId}
DELETE /{postId}
```

| 케이스 | 예상 결과 |
|--------|-----------|
| 본인 작성자 | 200 / 204 |
| manage 권한 보유자 (ADMIN) | 200 / 204 |
| 타인 + manage 권한 없음 | 403 |

---

## 5. 신규 endpoint E2E 테스트 시나리오

### 5.1 초안 생성 (`POST /drafts`)

```
시나리오: 초안 생성 성공
- ADMIN 토큰으로 POST /api/v1/channels/{channelId}/posts/drafts
- 응답 status=201
- body.status == "DRAFT"
- body.title == ""
- body.contentHtml == ""
- body.expiresAt != null
- body.attachments.size() == 0
```

### 5.2 초안 임시 저장 (`PUT /{postId}/draft`)

```
시나리오: 임시 저장 성공
- draft 생성 후 PUT /{postId}/draft { title: "임시 제목", contentHtml: "<p>임시</p>" }
- body.status == "DRAFT"
- body.title == "임시 제목"
- body.expiresAt이 이전 값보다 늦음 (연장 확인)

시나리오: null 필드는 기존값 유지
- title만 전달 → contentHtml은 이전 값 그대로

시나리오: 타인 초안 저장 시도 → 403
```

### 5.3 초안 첨부파일 연동 (`POST /{postId}/draft/attachments`)

```
시나리오: 첨부파일 업로드 및 연동 성공
- draft 생성
- multipart/form-data로 파일 전송
- 응답 body에 fileId, originalName, contentType, fileSize, ext, url 포함
- GET /{postId} 호출 시 body.attachments 에 해당 파일 포함 확인
  (fileId, originalName, sortOrder)

시나리오: 동일 파일 중복 연동 → 200 (idempotent, 중복 저장 없음)

시나리오: PUBLISHED 게시글에 attach 시도 → 400
```

> 주의: 파일 업로드 E2E는 `StorageService` 구현체가 테스트 환경에서 동작해야 한다.  
> 현재 테스트 프로필에서 StorageService가 mock인지 확인 후 테스트 작성 방식을 결정해야 한다.

### 5.4 초안 발행 (`PUT /{postId}/publish`)

```
시나리오: 발행 성공
- draft 생성 → saveDraft(title, contentHtml) → publish
- body.status == "PUBLISHED"
- body.expiresAt == null
- 채널 lastPostedAt 갱신 확인

시나리오: thumbnailUrl 미전달 시 첫 이미지 자동 설정
- draft 생성 → 이미지 업로드 연동 → publish(thumbnailUrl 없이)
- body.thumbnailUrl == (연동된 이미지의 publicUrl)

시나리오: 제목 없이 발행 → 400

시나리오: 타인 초안 발행 → 403
```

### 5.5 게시글 pin (`PUT /{postId}/pin`)

```
시나리오: ADMIN이 게시글 고정
- ADMIN 토큰으로 PUT /{postId}/pin { isPinned: true }
- body.isPinned == true
- GET /posts 목록에서 해당 게시글이 최상단 정렬 확인

시나리오: ADMIN이 고정 해제
- PUT /{postId}/pin { isPinned: false }
- body.isPinned == false

시나리오: GUEST가 pin 시도 → 403
```

### 5.6 `PostDetailResponse.attachments` 검증

상세 조회 응답에 `attachments` 배열이 올바르게 포함되는지 별도 검증.

```
- attachments[0].fileId != null
- attachments[0].originalName != null
- attachments[0].contentType != null
- attachments[0].fileSize > 0
- attachments[0].sortOrder == 0
- 두 번째 파일 연동 후 attachments[1].sortOrder == 1
```

---

## 6. TestHelper 보강 필요사항

### 6.1 `TestPostHelper` 보강

현재 `registerPost()` / `clearAll()` 만 있음.

추가 필요:
```java
// draft 생성 + 등록 원스텝 헬퍼
public Long createDraftAndRegister(Long channelId, String accessToken)

// post_attachments, post_files 직접 검증용 (tearDown 포함)
```

### 6.2 `TestFileHelper` 신규 생성

```java
@Service
public class TestFileHelper {
    // multipart 요청 빌더 (바이너리 없이 테스트 가능한 최소 payload)
    public RequestSpecification multipartImageRequest(String fieldName, String filename)
    public RequestSpecification multipartAttachmentRequest(String fieldName, String filename)
    
    // 정리
    public void clearAll()  // file + post_files + post_attachments
}
```

### 6.3 Repository 직접 주입 검증

일부 테스트에서 DB 상태를 직접 확인해야 할 수 있음.

```java
@Autowired PostAttachmentRepository postAttachmentRepository;
@Autowired PostFileRepository postFileRepository;

// 발행 후 lastPostedAt 갱신 검증
@Autowired ChannelRepository channelRepository;
```

---

## 7. 현재 컴파일 상태

- `./gradlew compileJava` 통과
- `compileTestJava` 미실행 — 위 3절의 충돌 지점으로 인해 **테스트 컴파일이 깨진다**
- 테스트는 3절 수정 완료 후 `./gradlew test` 실행

---

## 8. 다음 작업 순서 (권장)

1. **3절의 기존 테스트 충돌 수정** (컴파일 복구)
   - `BasePostTest`, `PostLifecycleTest`, `PostNestedResourceTest`, `ChannelCrudTest`
2. **4절 권한 체계 E2E 테스트 작성**
3. **5절 신규 endpoint E2E 테스트 작성**
   - `PostDraftLifecycleTest` 신규 파일 권장
4. **TestFileHelper 구현** (파일 업로드 테스트 전 선행 필요)
5. **Swagger 문서 동기화** (`docs/api/Posts.md`, `docs/api/Channels.md`)

---

## 9. 빠른 재개 포인트

테스트 작성자가 바로 시작하려면 아래 파일부터 본다.

- 충돌 수정: `BasePostTest.java`, `PostLifecycleTest.java`, `PostNestedResourceTest.java`, `ChannelCrudTest.java`
- 신규 테스트 작성 참고:
  - `PostDraftController.java` — 초안 endpoint
  - `PostPublishController.java` — 발행 endpoint
  - `PostWriteController.java` — pin endpoint
  - `PostDetailResponse.java` — attachments 응답 구조
- 권한 구조 참고:
  - `@channelAccess.can('write')`, `@channelAccess.can('manage')` — 채널 단위 권한
  - `@postAccess.can(#postId)` — 게시글 소유권
  - `PostActionService.getOwnedPost()` — 소유권 검증 로직 위치
