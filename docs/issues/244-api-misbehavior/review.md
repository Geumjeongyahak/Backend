# #244 검토 기록

## 규칙 1차 검증 (codex 전, work-an-issue 35)

`dev` 대비 diff 전체(35개 파일)를 규칙 문서와 나란히 놓고 판정했다.

| 문서 · 항목 | 판정 | 근거 |
|---|---|---|
| CLAUDE.md · 남의 Repository 직접 주입 금지 | 어김 → 고침 | `SiteHistoryService`가 새로 쓰는 파일 조회를 `FileRepository` 대신 `FileProxyService.findActiveByPublicUrl`로 (78ffa499). 첨부 정책 5개는 모두 자기 도메인 Repository만 쓴다. `AttachmentUploadService`는 post Repository 주입을 걷어냈다 |
| CLAUDE.md · 남은 경계 위반 (기존) | 해당 없음 (범위 밖) | `SiteHistoryService`의 기존 `FileRepository` 사용, `FileCleanupScheduler`의 post Repository 주입은 이번 변경 전부터 있다. PR `리뷰어에게`에 적는다 |
| CLAUDE.md · 응답 record, Entity 미노출 | 지킴 | `LessonSummaryResponse`에 `LessonStatus` enum만 더함 |
| CLAUDE.md · 커밋 scope | 지킴 | lesson · sitecontent · global · auth · file · post · meeting-record · purchase-request · vendor |
| writing-a-plan · 실패 경로·질의·판정 | 지킴 | 동시 재발급·6번째 기기·없는 토큰, 정책 질의 수(정책당 1번, 첫 true에서 멈춤) |
| writing-a-plan · 구현 중 바뀐 것 기록 | 지킴 | `plan.md` «구현 중 바뀐 것»: 6번 범위 축소(이미 400), 거래처 영수증 규칙 |
| changing-code · 호출자 전부 | 지킴 | `LessonSummaryResponse.from` 2 · `SortOrders.parse`/`toSortOrders` 4 · `createRefreshToken` 2(로컬·Google) · `deleteRefreshToken` 2(재발급·로그아웃) · `getDownloadUrl` 1 · `findActiveByPublicUrl` 1 |
| changing-code · 공유 지점 한 곳 | 지킴 | 정렬 파서 복사본(`ChannelListRequest`)을 `SortOrders` 하나로. 토큰 정리는 `createRefreshToken` 한 곳 |
| changing-code · 필요 없는 것 | 어김 → 고침 | 쓰지 않게 된 `RefreshTokenRepository.deleteByCredentialId` 삭제 |
| changing-code · 말하기 전에 본다 | 지킴 | 6번 500을 dev에서 재현 시도 → 이미 400. 거래처 영수증 규칙을 이력 API 권한에서 확인 |
| testing · 고치기 전 실패 | 지킴 | 1번 1, 3번 4+1(⑩ 거래처), 4번 2, 5번 3, 6번 단위 3이 고치기 전 실패. 6번 E2E 3건은 이미 400이라 회귀 방지용 |
| testing · 목 값 되읽기 | 지킴 | `AttachmentUploadServiceDownloadTest`는 허용/거절 갈래를 본다. `RefreshTokenServiceTest`는 지운 토큰 id 집합을 본다 |
| testing · 권한 통과·거절 둘 다 | 지킴 | 첨부 ①–⑩ 봉사자·게스트·관리자·`vendor:read:*` |
| testing · 날짜 미래 기준 | 지킴 | 수업 2027년, 연혁·첨부는 날짜 무관 |
| diff-signals · 반복문 안 질의 | 해당 없음 | 정책 순회는 최대 5번, 각 1질의. 토큰 정리는 조회 1 + 삭제 1 |
| diff-signals · 인자 없는 `now()` | 해당 없음 | 추가된 줄에 없음 (`grep` 0건) |
| diff-signals · 트랜잭션 안 외부 호출 · 원인 버리는 catch · 락 없는 확인 후 변경 | 해당 없음 | 추가된 줄에 없음. 동시 재발급은 계획대로 둘 다 성공 허용 |

## 커버리지 (JaCoCo, `verify.sh` 전체 1204건 뒤)

| 클래스 | 줄 | 분기 |
|---|---|---|
| `AttachmentUploadService` | 40/40 (100%) | 12/14 (85%) |
| `PostAttachmentReadPolicy` | 4/4 (100%) | 5/6 (83%) |
| `MeetingRecordAttachmentReadPolicy` | 4/4 (100%) | 5/6 (83%) |
| `PurchaseReceiptReadPolicy` | 5/5 (100%) | 7/8 (87%) |
| `VendorReceiptReadPolicy` | 3/3 (100%) | 4/6 (66%) |
| `SiteHistoryPhotoReadPolicy` | 1/1 (100%) | - |
| `RefreshTokenService` | 54/61 (88%) | 13/18 (72%) |
| `LocalAuthService` | 85/92 (92%) | 24/34 (70%) |
| `SortOrders` | 15/15 (100%) | 12/13 → 13/13 (단위 1건 추가) |
| `LessonSummaryResponse` | 13/14 (92%) | - |
| `SiteHistoryService` | 90/134 (67%) | 24/40 (60%) |

못 덮은 새 갈래: 정책들의 «로그인 안 한 사용자(null)» 갈래. 다운로드 API는 인증이 필요해 E2E로 닿지 않는다(기존 동작).
`SiteHistoryService`·`LocalAuthService`의 나머지는 이번에 안 바꾼 기존 갈래다.

## 실제 PostgreSQL 18 검증

Flyway V1–V9 적용 후 바뀐 E2E(수업 조회 · 연혁 · 분반 정렬 · 첨부 권한 · 게시글 파일 · 업로드 · 인증 · 채널) 165건 통과
(`stringtype=unspecified`, 테스트 실행에만).

## codex 리뷰

| 회차 | 범위 | P1 | P2 | 수용 | 반려 | 남은 P1·P2 |
|---|---|---|---|---|---|---|
| 1 | `dev` 대비 전체 | 0 | 2 | 1 (연혁 `src`를 사이트 콘텐츠 업로드로 제한) | 1 (동시 로그인 토큰 상한 — 정리 장치라 락 비용이 더 큼) | 0 |
| 2 | `dev` 대비 전체 | 0 | 3 | 3 (임시저장 첨부도 채널 read, 지운 구매 요청 영수증 제외, `ChannelProxyService.canRead`) | 0 | 0 |
| 3 | `dev` 대비 전체 | 0 | 1 | 1 (구매 영수증에 `purchase-request:read:*`) | 0 | 0 |
| 확인 | `dev` 대비 전체 | 0 | 0 | — | — | **0** |

지적 원문과 사유는 `harness/findings-ledger.md` 2026-10-01 #244 행. 규칙 1차 검증(35)에서 이미 고친 경계 위반·불필요 메서드는
codex 지적으로 다시 나오지 않았다.

## 검사

`scripts/harness/verify.sh` 통과 (H2). 첨부 권한 E2E는 ①–⑬.
