# #221 검토 기록

## 요청당 질의 수 (E2E, H2)

`UserDetailsCacheTest.secondRequest_doesNotQueryAuthTables` — 시드 사용자 2(봉사자 · 부서 2 · 개인 권한 1)의
토큰으로 `GET /api/v1/departments`를 두 번 부르고 두 번째 요청의 `PrepareStatementCount`를 센다.

| | 캐시 전 | 캐시 뒤 |
|---|---|---|
| 두 번째 요청의 질의 | 5 (인증 4 + 본문 1) | 1 (본문) |

## 무효화 (E2E)

같은 테스트 클래스. 캐시만 붙이고 무효화를 빼고 돌려 넷 다 실패하는 것을 본 뒤 리스너를 넣었다.

| 같은 토큰으로 다시 요청 | 캐시만 | 캐시 + 무효화 |
|---|---|---|
| 개인 권한 회수 뒤 `GET /api/v1/users` | 200 | 403 |
| ADMIN → VOLUNTEER 강등 뒤 | 200 | 403 |
| 부서 권한 프리셋 회수 뒤 | 200 | 403 |
| 사용자 삭제 뒤 `GET /api/v1/users/me` | 404 | 401 |

## codex 리뷰

| 회차 | 범위 | P1 | P2 | 수용 | 반려 | 남은 P1·P2 |
|---|---|---|---|---|---|---|
| 1 | `dev` 대비 전체 (HEAD b70a0aae) | 0 | 2 | 2 (01007788) | 0 | 0 |

지적 원문은 `harness/findings-ledger.md` 2026-10-01 #221 행. 두 건 모두 부하 측정 스크립트이고 캐시 · 무효화 코드에는 지적이 없었다.

## 부하 측정

`docs/reports/221-auth-permission-cache/index.html`. 임시 GCP VM 은 측정 뒤 `teardown.sh`로 VM · 디스크를 지우고
남은 VM · 디스크 · 스냅샷이 없음을 확인했다.
