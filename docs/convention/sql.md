# SQL Schema Convention

## 배포 전

- 첫 production 배포 전까지 `src/main/resources/db/migration/V1__initial_schema.sql`은 최신 전체 스키마로 수정할 수 있다.
- `src/main/resources/sql/init_scheme.sql`은 개발 초기화용 최신 전체 스키마로 유지한다.
- `src/test/resources/sql/init_scheme.sql`은 H2 호환 최신 전체 스키마로 유지한다.
- 세 스키마의 테이블과 핵심 컬럼은 의미상 같아야 한다.
- 테스트는 `spring.jpa.hibernate.ddl-auto: validate`로 SQL drift를 드러낸다.

## 배포 후

- 첫 production 배포 이후에는 `V1__initial_schema.sql`을 수정하지 않는다.
- DB 변경은 새 Flyway migration으로 추가한다.
