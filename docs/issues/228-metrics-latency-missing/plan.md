# 핵심 지표만 싸게 수집

- 이슈: #228
- 브랜치: `fix/228-metrics-latency-missing`
- 되돌리기 1회: 처음 계획(히스토그램 살리기, DB 서버에 에이전트 설치)을 비용 때문에 다시 설계했다

## 원인 (2026-09-29 dev 서버에서 확인)

| 지표 | 원인 | 근거 |
|---|---|---|
| HTTP 응답 시간 | 앱 서버 Ops Agent가 `_count`·`_bucket`만 남기고 `_sum`을 버린다. 히스토그램은 셋이 다 있어야 만들어진다 | 앱의 `/actuator/prometheus`는 `histogram` 타입과 `_bucket`·`_count`·`_sum`을 모두 낸다. 수집 `regex`에 `_sum`이 없다. Cloud Monitoring에 히스토그램 지표가 등록된 적이 없다 |
| PostgreSQL | DB 서버에서 Google로 나갈 길이 없다 | 외부 IP 없음, 서브넷의 비공개 Google 접근 꺼짐, Cloud NAT 없음. DB 서버에서 로그 · 모니터링 API와 패키지 저장소 모두 접속 실패. Ops Agent도 설치돼 있지 않다 |
| 알림 정책 9개 | 알림 채널이 하나도 없다 | 모든 정책의 알림 채널 수가 0이다. 울려도 아무에게도 가지 않는다 |

## 비용

Prometheus 형식 지표는 **샘플 100만 개당 $0.06, 무료 구간 없음**(Cloud Billing 카탈로그, 2026-09-29).
60초 수집이면 시계열 하나가 한 달에 약 43,000샘플이다. 비용은 시계열 수로 정해진다.

| 방식 | 시계열 | 월 비용 (dev 한 대) |
|---|---|---|
| 히스토그램 살리기 (`_sum`만 추가) | 경로 · 상태 조합마다 33개. 조합 150개면 약 5,000 | 약 $13 |
| 경로별 p95 하나 | 약 460 | 약 $1.2 |
| **경로 구분 없는 p95 하나 (채택)** | 약 20 | 약 $0.05 |

## 무엇을 바꾸나

| 파일 | 변경 |
|---|---|
| `application.yml` | `percentiles-histogram` 대신 `percentiles: 0.95`. 버킷 30여 개가 p95 값 하나가 된다 |
| `AppConfig` | `MeterFilter.ignoreTags("uri", "exception")`. 응답 시간 지표가 엔드포인트 수만큼 늘지 않는다 |
| 앱 설치 스크립트 수집 `regex` | `up \| http_server_requests_seconds(_count\|_sum)? \| hikaricp_connections_(active\|pending) \| jvm_memory_used_bytes` |
| 모니터링 스크립트 | p95 조회를 `quantile="0.95"`로, JVM 스레드 · 연결 총합 차트를 DB 연결 사용 중 · 대기로 |
| 내부 Grafana 대시보드 | `hikaricp_connections` → `hikaricp_connections_active` |

앱 설치 스크립트는 dev 배포 워크플로가 배포마다 다시 돌린다. 병합하면 서버 작업 없이 적용된다.
모니터링 스크립트는 수동으로만 돈다. 알림 채널이 없어 다시 돌리지 않는다.

### 핵심 지표

| 지표 | 보는 것 |
|---|---|
| `up` | 앱이 살아 있나 |
| `http_server_requests_seconds_count` (상태별) | 요청 수, 5xx 비율 |
| `http_server_requests_seconds{quantile="0.95"}` | 느려졌나 |
| `hikaricp_connections_pending` | DB 병목. 0보다 크면 요청이 DB 연결을 기다린다 |
| `hikaricp_connections_active` | DB 연결 사용량 (풀 최대 10) |
| `jvm_memory_used_bytes` | 메모리 |

DB 서버 지표 대신 **앱의 DB 연결 대기**를 DB 병목 신호로 쓴다. DB 서버를 건드리지 않고 이미 수집된다.

## 정한 것

| 항목 | 결정 | 이유 |
|---|---|---|
| 경로별 응답 시간 | 버린다 | 비용이 엔드포인트 수에 비례한다. 느린 경로는 로그의 `request`(#227)로 찾는다 |
| DB 서버 지표 | 하지 않는다 | 나가는 길을 열려면 NAT나 비공개 Google 접근을 켜야 한다. 비용 때문에 막은 길이다 |
| 알림 정책 | 건드리지 않는다 | 알림 채널이 없다 |
| 로그 기반 지표 | 하지 않는다 | 요청마다 로그를 남겨야 해서 로그 비용이 커진다 |

## 실패 경로

| 상황 | 결과 |
|---|---|
| 요약 지표(`quantile`)가 Cloud Monitoring에 안 생긴다 | 배포 30분 뒤 조회에서 드러난다. 8월 31일까지 같은 형식이 들어온 기록이 있다 |
| p95가 JVM 안에서 계산돼 여러 서버를 합칠 수 없다 | 앱 서버가 한 대다 |

## 테스트

| 층 | 검사 | 고치기 전 |
|---|---|---|
| E2E | 응답 시간 지표에 경로 태그가 없다 | 실패 |
| E2E | 응답 시간 지표가 버킷 없이 p95 하나만 낸다 | 실패 |
| 설정 검사 | 알림 · 대시보드가 쓰는 지표가 수집 `regex`에 모두 걸리고, 히스토그램 · 요약은 `_sum` · `_count`도 걸린다 | 실패 (`_sum`) |

빌드한 jar를 로컬 PostgreSQL로 띄워 실제 출력을 확인했다. 경로 다섯 개가 상태별 두 묶음으로
합쳐지고, 수집 `regex`를 지나는 시계열이 16개였다(바꾸기 전 dev 서버에서 640개).

수동 확인: 병합 30분 뒤 Cloud Monitoring에 `http_server_requests_seconds` 요약 지표가 나온다.

## 안 하는 것

- DB 지표 수집, DB 서버 네트워크 변경
- 알림 정책 · 알림 채널, 모니터링 스크립트 다시 돌리기
- 경로별 p95, 배포 뒤 지표 확인 자동화
- prod 지표 (prod 서버가 없다)
