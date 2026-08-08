# work-an-issue — 이슈 하나를 끝까지 끌고 간다

```text
work-an-issue <이슈 번호>
```

**성질: 자동.** 사람이 봐야 하는 자리는 `plan` 게이트 하나다. 나머지는 막히면 부른다.

```
05-issue ──▶ 10-plan ──[게이트 plan]──▶ 20-implement ──▶ 30-test ──▶ 40-review
                 ▲                            ▲                          │
                 └────────── 다시 설계 ────────┴───── P1·P2 있으면 ────────┘
```

## 단계

### 05 — 이슈를 받는다

```bash
gh issue view <번호> --json title,body,labels,url
```

원본은 GitHub에 있다. 받아온 본문은 이번 실행에만 쓰는 사본이라 **레포에 남기지
않는다.** 이슈 본문이 나중에 바뀌면 레포의 사본이 거짓말을 한다.

이슈 폴더가 있어야 한다.

```bash
ls -d docs/issues/<번호>-*
```

없으면 여기서 멈춘다. **무엇을 할지 고르고 폴더와 브랜치를 만드는 것은 사람의
일이다.** 워크플로가 이슈를 골라주기 시작하면 아무도 안 볼 브랜치가 쌓인다.

브랜치를 만든다. 통합 브랜치는 `dev`다.

```bash
git switch dev && git pull
git switch -c feat/<번호>-<slug>      # [FEAT] 이슈
git switch -c fix/<번호>-<slug>       # [FIX] 이슈
```

**접두사는 `feat/`다. `feature/`가 아니다.** 최근 40 PR에서 `feat/` 15건 ·
`fix/` 9건 · `feature/` 9건(전부 오래된 것)이다. `hotfix/`만 `main` 기준으로 딴다.

`<slug>`는 영문 소문자와 하이픈이고 이슈 폴더 이름의 뒷부분과 같다
(`feat/217-daily-schedule-query-optimization` ↔ `docs/issues/217-daily-schedule-query-optimization/`).

**검사 기준선을 여기서 잡는다.**

```bash
scripts/harness/verify.sh
```

시작 전부터 빨간불이면, 뒤에서 나오는 실패가 이번 변경 때문인지 원래 그랬는지를 못
가린다. 이미 깨져 있으면 그 사실을 먼저 보고하고 사람의 판단을 받는다.

### 10 — 설계

`docs/issues/<번호>-<slug>/plan.md`에 쓴다.

**계획은 이 레포를 모르는 사람이 읽어도 실행할 수 있어야 한다.** 작업마다 어느
파일을 건드리는지, 어떻게 확인하는지, 어떤 문서를 같이 고치는지를 적는다. 작업
하나가 커밋 하나쯤 되게 쪼갠다.

여기서 더 붙는 것은 이 레포의 사정 셋이다.

- **도메인 경계.** 통로가 둘이다. 조회는 상대 도메인의 `*ProxyService`를 주입하고,
  상태 변화·부수 효과는 이벤트다. 어느 쪽을 쓰는지, 이벤트면 누가 발행하고 누가
  받는지를 계획에 적는다. Proxy에 없는 조회가 필요하면 **그 도메인의 Proxy에 메서드를
  더하는 것**이지 Repository를 직접 주입하는 게 아니다.
- **스키마 변경.** 엔티티가 바뀌면 Flyway 마이그레이션이 따라온다. `V<n>__<설명>.sql`의
  번호를 계획 단계에서 정해둔다 — 구현 중에 정하면 같은 번호를 두 브랜치가 잡는다.
  `src/main/resources/sql/init_scheme.sql`도 같이 바뀌는지 확인한다
  (`SqlSchemaConsistencyTest`가 본다).
- **권한.** 새 엔드포인트면 `@PreAuthorize`가 무엇을 요구하는지를 적는다. 층이 둘이다 —
  역할은 `hasRole('ADMIN')`(RoleType은 ADMIN·MANAGER·VOLUNTEER·GUEST 넷뿐),
  세밀 권한은 `hasAuthority('user:manage:*')` 형태의 `PermissionCode`
  (`resource:action:target`)다. 새 조합을 쓰려면 `PermissionRegistry`에 먼저 등록한다.

`plan.md`의 첫 줄은 `# <제목>`이다. 게이트가 이 제목을 이슈 제목과 나란히 놓는다.

### 게이트 `plan` — 이 계획으로 구현해도 되나

**사람이 봐야 하는 유일한 자리다.** 물음에 다음을 실어 보낸다.

```
맡은 것: <이슈 제목>
계획한 것: <plan.md 첫 줄>
spec 이력: 있음 / 없음        docs/issues/<번호>-*/spec.md
되돌리기: <n>/2회
```

**맡은 것과 계획한 것을 나란히 놓는 이유가 있다.** 형식만 세는 판정은 주제가 어긋난
계획을 만점으로 통과시킨다. 두 제목을 붙여 놓으면 사람이 한눈에 본다.

`spec 이력`이 «없음»이면 `ideate-issue`를 안 거친 것이다. 무엇을 왜 하는지 사람이
판단한 기록이 없다는 뜻이고, 그대로 승인하면 승인한 사람이 진다.

| 답 | 다음 |
|---|---|
| `승인` | 20으로 |
| `다시 설계` | 10으로. **예산 2회.** 다 쓰면 사람이 봐야 한다 |
| `막힘` | 여기서 끝난다 |

되돌리기에 예산을 두는 이유는 설계가 계속 얕을 때 무한히 돌기 때문이다.
[`gates-ledger.tsv`](../../../gates-ledger.tsv)에 한 행을 적는다.

### 20 — 구현

**테스트를 먼저 쓰고, 실패하는 것을 보고, 통과시킨다.** 실패를 안 봤으면 그 테스트가
맞는 것을 보고 있는지 모른다.

커밋 메시지는 Conventional Commits다.

```
<type>(<scope>): <subject>

type   feat fix docs style refactor test chore
scope  request purchase-request user global daily-schedule subject lesson auth ...
```

scope는 **도메인 폴더 이름을 하이픈으로 바꾼 것**이다 (`purchase_request` →
`purchase-request`). 전체 목록은
[`docs/convention/convention.md`](../../../../docs/convention/convention.md) §1.3에 있다.

**작은 논리 단위로 담는다.** 이 레포의 최근 이력이 그렇게 돼 있다 — 이슈 하나에
`feat` → `docs` → `fix`가 순서대로 붙는다. 한 커밋에 전부 넣으면 되돌릴 때 통째로만
되돌아간다.

작업을 갈라서 병렬로 돌리는 자리는 **작업들이 서로의 결과를 안 볼 때**다. 같은
파일을 두 작업이 고치면 순서대로 한다.

### 30 — 테스트

경계와 실패를 덮는다. 이 레포의 테스트는 세 층이다 (2026-08-08 기준 파일 수).

| 층 | 어디 | 무엇 | 수 |
|---|---|---|---|
| E2E | `src/test/java/geumjeongyahak/e2e/` | 요청부터 응답까지, 권한 포함 | 106 |
| 단위 | `src/test/java/geumjeongyahak/unit/` | 서비스·엔티티·검증기의 판단 | 44 |
| 도메인 | `src/test/java/geumjeongyahak/domain/` | 외부 연동을 낀 서비스와 변환기 | 4 |

`domain/` 층은 Drive 저장소나 docx 템플릿처럼 **바깥과 붙는 것**을 본다. 새 테스트를
여기 두는 것은 그 성질일 때뿐이고, 아니면 `unit/`이다.

**새 엔드포인트는 E2E가 있어야 한다.** 권한 분기가 있으면 통과하는 역할과 막히는
역할을 둘 다 친다. 최근 추가된 것들이 그 형태다 (`PurchaseRequestStatusTest`,
`TeacherAssignmentAdminTest`).

날짜가 들어가는 테스트는 **미래 기준으로 만든다.** 고정 날짜로 짜면 그날이 지나는
순간 깨진다. `#217`에서 실제로 그렇게 깨졌다.

```bash
./gradlew test --tests '*<바뀐 것>*'    # 먼저 좁게
scripts/harness/verify.sh              # 그다음 전체
```

### 40 — 리뷰

**프로토콜: [codex-review.md](../codex-review.md)**

검사가 먼저다. 통과 못 한 코드를 리뷰에 보내면 지적이 그것으로 덮인다.

```bash
scripts/harness/verify.sh || exit 1
```

그다음 codex CLI로 리뷰를 받는다. 절차와 부르는 기준은 전부
[codex-review.md](../codex-review.md)에 있다. 요지만 옮기면

- **여러 파일에 걸치거나 구조가 바뀌었을 때 한 번.** 한 줄 고친 것에는 안 부른다
- 안 부르고 병합해도 된다. 다만 **한 번 부르기로 했으면 수렴할 때까지 돈다**
- 남은 `P1`·P2를 두고 병합하지 않는다
- 지적 한 건이 [`findings-ledger.md`](../../../findings-ledger.md) 한 행이다

`P1`·`P2`가 남으면 **20으로 되돌아간다.** 여기서 실패로 끝내면 사람은 「실패」 한 줄만
본다. 되돌아가는 것은 정상 흐름이다.

## 나가는 것

- `feat/<번호>-<slug>` 브랜치와 커밋들
- `docs/issues/<번호>-<slug>/plan.md` · `review.md`
- `verify.sh` 통과 기록
- `gates-ledger.tsv`의 `plan` 행 · `findings-ledger.md`의 지적 행들

다음은 [`land-a-pr`](land-a-pr.md)다.
