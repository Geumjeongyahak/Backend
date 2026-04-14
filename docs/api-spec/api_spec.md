# 금정야학 플랫폼 API 문서 안내

이 문서는 상세 스펙 본문이 아니라 API 문서의 진입점입니다.

기존에는 이 문서에 시퀀스 다이어그램과 도메인별 흐름 설명이 함께 들어 있었지만, 실제로는 도메인별 엔드포인트 문서와 내용이 분산되어 유지보수가 어려웠습니다. 현재는 상세 API 설명, side effect, 실패 케이스, 시퀀스 다이어그램을 각 도메인 문서로 옮기고 이 문서는 안내 역할만 맡습니다.

## 1. 공통 규칙

### 1.1 기본 정보

| 항목 | 값 |
|------|-----|
| Base URL | `/api/v1` |
| 인증 방식 | Session-based (Cookie) |
| Content-Type | `application/json` |
| API 문서 | Swagger UI (`/swagger-ui.html`) |

### 1.2 응답 형식

```json
// 성공
{ "success": true, "data": { ... }, "error": null }

// 실패
{ "success": false, "data": null, "error": { "code": "ERROR_CODE", "message": "..." } }
```

- `error.code`는 클라이언트 분기 기준으로 사용합니다.
- 공통/인증 에러 규칙은 [Error Codes](../error_codes.md)를 기준으로 봅니다.

## 2. 문서 읽는 순서

### 2.1 제품/설계 관점

- [PRD](../prd.md)
- [기술 명세서](../tech_spec.md)
- [데이터 모델](../data_model.md)

### 2.2 API 관점

- [도메인별 API 문서 인덱스](../api/README.md)
- [Auth](../api/Auth.md)
- [Users](../api/Users.md)
- [Departments](../api/Departments.md)
- [Classrooms](../api/Classrooms.md)
- [Students](../api/Students.md)
- [Channels](../api/Channels.md)
- [Posts](../api/Posts.md)
- [Comments](../api/Comments.md)

### 2.3 에러 코드 관점

- [Error Codes](../error_codes.md)

## 3. 문서 역할 분리

- 이 문서: API 문서 구조와 공통 규칙 안내
- `docs/api/*.md`: 도메인별 엔드포인트, 권한, side effect, 실패 케이스, 시퀀스 다이어그램
- `docs/error_codes.md`: 공통/인증/도메인 에러 코드 체계

## 4. 유지보수 원칙

- 시퀀스 다이어그램은 가능한 한 해당 도메인 문서 안에 둡니다.
- Swagger보다 중요한 운영 규칙, side effect, 권한 정책은 `docs/api/*.md`에 명시합니다.
- PR 문서에만 있는 정책 설명은 필요한 경우 정식 문서로 끌어올립니다.
