# HTTP 응답 시간 지표 수집 복구

- 이슈: #228
- 브랜치: `fix/228-metrics-latency-missing`

## 원인 (2026-09-29 dev 서버에서 확인)

| 지표 | 원인 | 근거 |
|---|---|---|
| HTTP 응답 시간 | 앱 서버 Ops Agent가 `_count`·`_bucket`만 남기고 `_sum`을 버린다. 히스토그램은 셋이 다 있어야 만들어진다 | 앱의 `/actuator/prometheus`는 `# TYPE http_server_requests_seconds histogram`과 `_bucket`·`_count`·`_sum`을 모두 낸다. 수집 `regex`에 `_sum`이 없다. Cloud Monitoring에 히스토그램 지표가 등록된 적이 없다 |
| PostgreSQL | DB 서버에서 Google로 나갈 길이 없다 | 외부 IP 없음, 서브넷의 비공개 Google 접근 꺼짐, Cloud NAT 없음. DB 서버에서 로그·모니터링 API와 패키지 저장소 모두 접속 실패. Ops Agent도 설치돼 있지 않다 |
| 알림 정책 9개 | 알림 채널이 하나도 없다 | 모든 정책의 알림 채널 수가 0이고, 프로젝트에 채널이 없다. 울려도 아무에게도 가지 않는다 |

## 무엇을 바꾸나

```diff
# scripts/gcp/05_app/01_install-app-service.sh
- regex: 'up|http_server_requests_seconds_(count|bucket)|hikaricp_connections|...'
+ regex: 'up|http_server_requests_seconds_(count|sum|bucket)|hikaricp_connections|...'
```

앱 설치 스크립트는 dev 배포 워크플로가 배포마다 다시 돌리므로, 병합하면 dev 앱 서버에 바로 적용된다.
서버에서 따로 할 일은 없다.

## 정한 것

| 항목 | 결정 | 이유 |
|---|---|---|
| DB 지표 | **하지 않는다** | DB 서버가 밖으로 나가려면 NAT나 비공개 Google 접근을 켜야 한다. 비용 때문에 막아 둔 길이다. 앱 서버에서 내부망으로 DB를 읽는 방법도 있지만 사람이 하지 않기로 정했다 |
| 알림 정책 | 건드리지 않는다 | 알림 채널이 없어 고쳐도 달라지는 것이 없다 |
| 로그 기반 지표로 p95 계산 | 하지 않는다 | 요청마다 로그를 남겨야 해서 로그 비용이 커진다. 지금은 경고·오류만 보낸다 |

## 실패 경로

| 상황 | 결과 |
|---|---|
| `_sum`을 넣어도 히스토그램이 안 생긴다 | 배포 30분 뒤 조회에서 드러난다. 그때 원인을 다시 본다 |
| 히스토그램 시계열로 지표 비용이 는다 | 경로 · 상태 · 메서드 조합마다 버킷이 생긴다. 배포 뒤 하루치 샘플 수를 본다 |

## 테스트 (먼저 쓰고 실패를 본다)

`scripts/gcp/tests/observability-config-test.sh`(검사에 이미 들어 있다)에 둘을 더한다.

| 검사 | 고치기 전 |
|---|---|
| 알림 정책·대시보드의 PromQL이 쓰는 지표 이름이 모두 앱 또는 DB 수집 설정의 `regex`에 걸린다 | 통과 (회귀 방지) |
| 그중 히스토그램(`_bucket`)을 쓰면, 같은 지표의 `_sum`·`_count`도 `regex`에 걸린다 | 실패 — `http_server_requests_seconds_sum`이 안 걸린다 |

이름만 대조하면 이번 결함을 못 잡는다. PromQL은 `_bucket`만 쓰고 그것은 수집되고 있었다. 빠진 것은
PromQL이 쓰지 않는 `_sum`이었다. 그래서 히스토그램은 셋을 함께 요구한다.

수동 확인: 병합 30분 뒤 Cloud Monitoring에 `http_server_requests_seconds` 히스토그램이 나오고 p95를 조회할 수 있다.

## 작업

1. 검사 둘 → 실패 확인 → 커밋 `test(global): …`
2. `regex`에 `_sum` → 통과 → 커밋 `fix(global): …`
3. PR, 병합 (dev 배포로 적용)
4. 30분 뒤 지표 확인, 결과를 이슈에 남긴다

## 안 하는 것

- DB 지표 수집, DB 서버 네트워크 변경
- 알림 정책 · 알림 채널
- 대시보드에 경로별 p95 추가, 배포 뒤 지표 확인 자동화 (Should)
- prod 지표 (prod 서버가 없다)
