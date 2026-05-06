# 권한 체계 (Permission System)

이 문서는 금정야학 플랫폼의 권한 코드 구조, 부여 방법, 도메인별 접근 정책을 종합 정리합니다.

---

## 1. 권한의 구성

사용자의 최종 authority 목록은 세 가지 출처를 합산하여 결정됩니다.

```
사용자 authority 목록 = role 권한 + user 직접 권한 + department 권한
```

| 출처 | 형태 | 예시 |
|------|------|------|
| 기본 역할 (role) | `ROLE_{ROLE_NAME}` | `ROLE_ADMIN`, `ROLE_MANAGER` |
| 사용자 직접 권한 | `{resource}:{action}:{target}` | `user:manage:*` |
| 부서 권한 | `{resource}:{action}:{target}` | `channel:write:3` |

세 출처는 Spring Security의 `GrantedAuthority` 컬렉션으로 합산됩니다.  
`ADMIN` 역할 보유자는 모든 권한 검사를 우선 통과합니다.

---

## 2. Permission Code 형식

```
{resource}:{action}:{target}
```

| 구성요소 | 설명 | 규칙 |
|---------|------|------|
| `resource` | 리소스 종류 | 소문자 snake_case |
| `action` | 수행 작업 | 소문자 |
| `target` | 대상 범위 | `*` (전체) 또는 양수 정수 ID |

**예시**

| 코드 | 의미 |
|------|------|
| `user:read:*` | 모든 사용자 읽기 |
| `channel:manage:3` | ID=3 채널 관리 |
| `lesson:write:12` | ID=12 수업 쓰기 |

### 유효성 규칙

- 정규식: `^[a-z][a-z0-9_]*:[a-z][a-z0-9_]*:(\*|[1-9][0-9]*)$`
- `resource`와 `action`은 아래 허용 조합 목록(`PermissionRegistry`)에 있어야 합니다.
- `target`이 `*`이면 전체 범위, 숫자이면 해당 ID 단위 범위입니다.

---

## 3. 허용 Permission Code 목록 (PermissionRegistry)

API로 사용자에게 부여할 수 있는 코드는 아래 조합으로만 제한됩니다.  
이 외 조합은 `@ValidPermissionCode` 검증에서 거부됩니다.

| resource | 허용 action | 전체 범위 코드 | ID 범위 코드 |
|----------|------------|---------------|-------------|
| `user` | `read` | `user:read:*` | `user:read:{id}` |
| `user` | `write` | `user:write:*` | `user:write:{id}` |
| `user` | `manage` | `user:manage:*` | `user:manage:{id}` |
| `channel` | `read` | `channel:read:*` | `channel:read:{id}` |
| `channel` | `write` | `channel:write:*` | `channel:write:{id}` |
| `channel` | `manage` | `channel:manage:*` | `channel:manage:{id}` |
| `department` | `read` | `department:read:*` | `department:read:{id}` |
| `department` | `write` | `department:write:*` | `department:write:{id}` |
| `department` | `manage` | `department:manage:*` | `department:manage:{id}` |
| `request` | `read` | `request:read:*` | `request:read:{id}` |
| `request` | `write` | `request:write:*` | `request:write:{id}` |
| `request` | `approve` | `request:approve:*` | `request:approve:{id}` |
| `request` | `reject` | `request:reject:*` | `request:reject:{id}` |
| `classroom` | `read` | `classroom:read:*` | `classroom:read:{id}` |
| `classroom` | `write` | `classroom:write:*` | `classroom:write:{id}` |
| `student` | `read` | `student:read:*` | `student:read:{id}` |
| `student` | `write` | `student:write:*` | `student:write:{id}` |
| `subject` | `read` | `subject:read:*` | `subject:read:{id}` |
| `subject` | `write` | `subject:write:*` | `subject:write:{id}` |
| `lesson` | `read` | `lesson:read:*` | `lesson:read:{id}` |
| `lesson` | `write` | `lesson:write:*` | `lesson:write:{id}` |
| `file` | `read` | `file:read:*` | `file:read:{id}` |
| `file` | `write` | `file:write:*` | `file:write:{id}` |
| `post` | `read` | `post:read:*` | `post:read:{id}` |

> **참고**: `post` 리소스는 PermissionRegistry에 정의되어 있지 않지만  
> `PostBoardController`에서 `post:read:*`가 `@PreAuthorize`에 사용됩니다.  
> 현재는 API를 통해 직접 부여할 수 없으며, 역할 또는 채널 권한으로 대체합니다.

---

## 4. 역할(Role)별 기본 접근 범위

기본 역할은 `@PreAuthorize("hasRole('...')")` 조건으로만 판단하며  
permission code로는 표현되지 않습니다.

| 역할 | authority 문자열 | 기본 접근 범위 |
|------|----------------|--------------|
| `ADMIN` | `ROLE_ADMIN` | 모든 API 우선 통과 |
| `MANAGER` | `ROLE_MANAGER` | 요청 승인/반려, 과목 CUD |
| `VOLUNTEER` | `ROLE_VOLUNTEER` | 일반 조회, 요청 제출, 수업 운영 |
| `GUEST` | `ROLE_GUEST` | 최소 읽기 |

---

## 5. 도메인별 API 접근 정책

### 5.1 User

| API | 접근 조건 |
|-----|---------|
| `GET /api/v1/users` | `ADMIN` \| `user:read:*` |
| `GET /api/v1/users/{userId}` | `ADMIN` \| `user:read:*` |
| `POST /api/v1/users` | `ADMIN` \| `user:manage:*` |
| `PATCH /api/v1/users/{userId}` | `ADMIN` \| `user:manage:*` |
| `DELETE /api/v1/users/{userId}` | `ADMIN` \| `user:manage:*` |
| `GET /api/v1/users/me` | 인증만 |
| `PATCH /api/v1/users/me` | 인증만 |
| `GET /api/v1/users/{userId}/permissions` | `ADMIN` \| `user:read:*` |
| `POST /api/v1/users/{userId}/permissions` | `ADMIN` \| `user:manage:*` |
| `DELETE /api/v1/users/{userId}/permissions` | `ADMIN` \| `user:manage:*` |

### 5.2 Department

| API | 접근 조건 |
|-----|---------|
| `GET /api/v1/departments` | 인증만 |
| `GET /api/v1/departments/{id}` | 인증만 |
| `POST /api/v1/departments` | `ADMIN` \| `department:manage:*` |
| `PUT /api/v1/departments/{id}` | `ADMIN` \| `department:manage:*` |
| `DELETE /api/v1/departments/{id}` | `ADMIN` \| `department:manage:*` |

### 5.3 Classroom

| API | 접근 조건 |
|-----|---------|
| `GET /api/v1/classrooms` | 인증만 |
| `GET /api/v1/classrooms/{id}` | 인증만 |
| `POST /api/v1/classrooms` | `ADMIN` \| `classroom:write:*`* |
| `PUT /api/v1/classrooms/{id}` | `ADMIN` \| `classroom:write:*`* |
| `DELETE /api/v1/classrooms/{id}` | `ADMIN` \| `classroom:write:*`* |

> \* 코드에서 `classroom:manage:*` 문자열을 사용하나, PermissionRegistry는 `classroom`에 `manage`를 허용하지 않습니다.  
> 실제로 해당 authority를 부여하려면 `classroom:write:*`을 사용하세요.

### 5.4 Student

| API | 접근 조건 |
|-----|---------|
| `GET /api/v1/students` | 인증만 |
| `GET /api/v1/students/{studentId}` | 인증만 |
| `POST /api/v1/students` | `ADMIN` 전용 |
| `PATCH /api/v1/students/{studentId}` | `ADMIN` 전용 |
| `DELETE /api/v1/students/{studentId}` | `ADMIN` 전용 |

### 5.5 Subject

| API | 접근 조건 |
|-----|---------|
| `GET /api/v1/subjects` | 인증만 |
| `GET /api/v1/subjects/{subjectId}` | 인증만 |
| `POST /api/v1/subjects` | `ADMIN` \| `MANAGER` |
| `PATCH /api/v1/subjects/{subjectId}` | `ADMIN` \| `MANAGER` |
| `DELETE /api/v1/subjects/{subjectId}` | `ADMIN` \| `MANAGER` |

### 5.6 Lesson

| API | 접근 조건 |
|-----|---------|
| `GET /api/v1/lessons` | 인증만 |
| `GET /api/v1/lessons/me` | 인증만 |
| `GET /api/v1/lessons/{lessonId}` | 인증만 |
| `GET /api/v1/lessons/{lessonId}/student-attendances` | 인증만 |
| `GET /api/v1/lessons/{lessonId}/note` | 인증만 |
| `PATCH /api/v1/lessons/{lessonId}/teacher-attendance` | 인증만 |
| `PATCH /api/v1/lessons/{lessonId}/student-attendances` | 인증만 |
| `PATCH /api/v1/lessons/{lessonId}/status` | 인증만 |
| `PUT /api/v1/lessons/{lessonId}/note` | 인증만 |
| `POST /api/v1/lessons` | `ADMIN` \| `lesson:manage:*`* |
| `PATCH /api/v1/lessons/{lessonId}` (admin) | `ADMIN` \| `lesson:manage:*`* |
| `DELETE /api/v1/lessons/{lessonId}` | `ADMIN` \| `lesson:manage:*`* |

> \* 코드에서 `lesson:manage:*` 문자열을 사용하나, PermissionRegistry는 `lesson`에 `manage`를 허용하지 않습니다.  
> 현재 permission API로 부여할 수 없으며 `ADMIN` 역할로 접근하세요.

### 5.7 Request (결석/교환/구입)

| API | 접근 조건 |
|-----|---------|
| `POST /api/v1/absence-requests` | 인증만 |
| `GET /api/v1/absence-requests` | 인증만 |
| `GET /api/v1/absence-requests/{requestId}` | 인증만 |
| `PATCH /api/v1/absence-requests/{requestId}/approve` | `ADMIN` \| `MANAGER` |
| `PATCH /api/v1/absence-requests/{requestId}/reject` | `ADMIN` \| `MANAGER` |
| `DELETE /api/v1/absence-requests/{requestId}` | 인증만 (본인 한정) |
| `POST /api/v1/lesson-exchange-requests` | 인증만 |
| `GET /api/v1/lesson-exchange-requests` | 인증만 |
| `GET /api/v1/lesson-exchange-requests/{requestId}` | 인증만 |
| `PATCH /api/v1/lesson-exchange-requests/{requestId}/approve` | `ADMIN` \| `MANAGER` |
| `PATCH /api/v1/lesson-exchange-requests/{requestId}/reject` | `ADMIN` \| `MANAGER` |
| `POST /api/v1/lesson-exchange-requests/{requestId}/proposals` | 인증만 |
| `POST /api/v1/purchase-requests` | 인증만 |
| `GET /api/v1/purchase-requests` | 인증만 |
| `GET /api/v1/purchase-requests/{requestId}` | 인증만 |
| `PATCH /api/v1/purchase-requests/{requestId}/approve` | `ADMIN` \| `MANAGER` |
| `PATCH /api/v1/purchase-requests/{requestId}/reject` | `ADMIN` \| `MANAGER` |
| `POST /api/v1/subject-exchange-requests` | 인증만 |
| `GET /api/v1/subject-exchange-requests` | 인증만 |
| `GET /api/v1/subject-exchange-requests/{requestId}` | 인증만 |
| `PATCH /api/v1/subject-exchange-requests/{requestId}/approve` | `ADMIN` \| `MANAGER` |
| `PATCH /api/v1/subject-exchange-requests/{requestId}/reject` | `ADMIN` \| `MANAGER` |

### 5.8 Channel

채널은 역할/코드 외에도 채널 자체의 `accessLevel` 정책에 따라 접근이 결정됩니다.

| API | 접근 조건 |
|-----|---------|
| `GET /api/v1/channels` | 인증만 |
| `GET /api/v1/channels/{id}` | `ADMIN` \| `channel:read:{id}` \| `channel:read:*` \| accessLevel ≥ READ_ONLY |
| `POST /api/v1/channels` | `ADMIN` \| `channel:manage:*` (채널 생성)* |
| `PUT /api/v1/channels/{id}` | `ADMIN` \| `channel:manage:*` \| `channel:manage:{id}` |
| `DELETE /api/v1/channels/{id}` | `ADMIN` \| `channel:manage:*` \| `channel:manage:{id}` |

> \* 코드에서 `channel:create:*`을 사용하나, PermissionRegistry는 `channel`에 `create`를 허용하지 않습니다.  
> 현재 permission API로 부여할 수 없으며 `ADMIN` 역할 또는 `channel:manage:*`을 사용하세요.

### 5.9 Post/Comment

게시글과 댓글은 채널의 `accessLevel`과 명시적 channel 권한을 함께 검사합니다.

| API | 접근 조건 |
|-----|---------|
| `GET /api/v1/posts` (전체 게시글) | 인증만 |
| `GET /api/v1/channels/{channelId}/posts` | `channel:read:{channelId}` \| `channel:read:*` \| accessLevel ≥ READ_ONLY |
| `GET /api/v1/channels/{channelId}/posts/{postId}` | 인증만 |
| `POST /api/v1/channels/{channelId}/posts` (게시) | `channel:write:{channelId}` \| `channel:write:*` \| accessLevel ≥ READ_WRITE |
| `POST /api/v1/channels/{channelId}/posts/drafts` | 채널 write 권한 동일 |
| `PUT /api/v1/channels/{channelId}/posts/{postId}/publish` | 채널 write 권한 동일 |
| `PUT /api/v1/channels/{channelId}/posts/{postId}` (수정) | 채널 manage 권한 \| 글 작성자 본인 |
| `PUT /api/v1/channels/{channelId}/posts/{postId}/pin` | `channel:manage:{channelId}` \| `channel:manage:*` |
| `DELETE /api/v1/channels/{channelId}/posts/{postId}` | 채널 manage 권한 \| 글 작성자 본인 |
| `POST /api/v1/channels/{channelId}/posts/{postId}/comments` | accessLevel ≥ READ_COMMENT \| 채널 write/manage 권한 |
| `GET /api/v1/channels/{channelId}/posts/{postId}/comments` | 인증만 |
| `DELETE /api/v1/channels/{channelId}/posts/{postId}/comments/{commentId}` | 채널 manage 권한 \| 댓글 작성자 본인 |

### 5.10 File

| API | 접근 조건 |
|-----|---------|
| `POST /api/v1/files/images/profile` | 인증만 |
| `POST /api/v1/files/images/posts` | 인증만 |
| `POST /api/v1/files/images/purchase-items` | 인증만 |
| `POST /api/v1/files/attachments` | 인증만 |
| `GET /api/v1/files/attachments/{fileId}/download-url` | 인증만 |
| `DELETE /api/v1/files/attachments/{fileId}` | 인증만 |

---

## 6. 채널 AccessLevel 정책

채널별로 설정된 `accessLevel`은 명시적 권한 없이 접근할 수 있는 기본 범위를 결정합니다.

| AccessLevel | 읽기 | 댓글 작성 | 게시글 작성 |
|------------|------|----------|-----------|
| `READ_ONLY` | ✓ | - | - |
| `READ_COMMENT` | ✓ | ✓ | - |
| `READ_WRITE` | ✓ | ✓ | ✓ |

`MANAGE` 액션(채널 수정/삭제, 게시글 핀 고정 등)은 accessLevel과 무관하게  
`ADMIN` 역할 또는 `channel:manage:{id}` 명시적 권한이 필요합니다.

---

## 7. 권한 부여 방법 요약

### 방법 1: 역할(role) 변경

관리자가 `PATCH /api/v1/users/{userId}`로 `role` 필드를 변경합니다.  
변경 즉시 해당 역할에 따른 기본 접근 범위가 적용됩니다.

### 방법 2: 사용자 직접 권한 부여

`POST /api/v1/users/{userId}/permissions`로 특정 permission code를 직접 부여합니다.  
역할로 표현하기 어려운 세부 예외 권한을 부여할 때 사용합니다.

```json
{ "permissionCode": "channel:manage:3" }
```

### 방법 3: 부서 권한 (Department)

사용자를 특정 부서에 소속시키면 해당 부서에 설정된 permission code들이 자동으로 적용됩니다.  
부서 소속 변경은 `PATCH /api/v1/users/{userId}` (`departmentId` 필드)로 수행합니다.

---

## 8. 권한 검사 우선순위

```
1. ADMIN 역할 → 즉시 통과 (모든 @PreAuthorize 생략 효과)
2. @PreAuthorize hasRole() → Spring Security role 확인
3. @PreAuthorize hasAuthority() → authority 문자열 exact match
4. @PreAuthorize hasPermission() → PermissionCodeEvaluator: wildcard(*) 또는 specific(id) 검사
5. @channelAccess.can() → 명시적 권한 + 채널 accessLevel 정책 순서로 검사
```
