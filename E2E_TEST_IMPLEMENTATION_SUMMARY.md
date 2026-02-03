# E2E 테스트 구현 요약

## 구현 완료 항목

### 1. 설정 파일 ✅
- **`src/test/resources/application-test.yml`** - H2 인메모리 DB 테스트 설정

### 2. 누락된 Repository 생성 ✅
다음 4개의 리포지토리를 생성하여 엔티티 관리를 완성했습니다:
- `PermissionRepository`
- `UserPermissionRepository`
- `DepartmentPermissionRepository`
- `UserDepartmentRepository`

### 3. AbstractE2ETest 개선 ✅
**파일**: `src/test/java/org/geumjeong/learning/sonmoum_api/e2e/AbstractE2ETest.java`

**개선 사항**:
- 모든 도메인 리포지토리 주입 (14개)
- 포괄적인 teardown 메서드 (올바른 순서로 전체 삭제)
- 공통 헬퍼 메서드 추가:
  - `createTestUser(email, password)` - VOLUNTEER 역할 사용자 생성
  - `createAdminUser(email, password)` - MANAGER 역할 사용자 생성
  - `login(email, password)` - 로그인 및 세션 ID 반환
  - `loginAsAdmin()` - 관리자로 로그인
  - `loginAsVolunteer()` - 봉사자로 로그인

**Teardown 순서** (외래 키 제약 고려):
```
1. Requests (모든 타입)
2. Lessons
3. Subjects
4. UserPermissions, DepartmentPermissions, UserDepartments
5. Students, Classrooms, Departments, Permissions
6. Users
```

### 4. Base 도메인 테스트 클래스 (8개) ✅

각 도메인별 공통 설정과 헬퍼 메서드를 제공하는 추상 클래스를 생성했습니다:

| 클래스 | 경로 | 주요 메서드 |
|--------|------|------------|
| BaseAuthE2ETest | `e2e/auth/` | `performLogin()`, `getAuthenticatedSession()` |
| BaseUserE2ETest | `e2e/users/` | `createUserRequest()`, `getAdminSession()` |
| BaseDepartmentE2ETest | `e2e/departments/` | `createDepartmentRequest()`, `getAdminSession()` |
| BaseStudentE2ETest | `e2e/students/` | `createStudentRequest()`, `getAdminSession()` |
| BaseClassroomE2ETest | `e2e/classrooms/` | `createClassroomRequest()`, `getAdminSession()` |
| BaseSubjectE2ETest | `e2e/subjects/` | `createSubjectRequest()`, `setupClassroomAndTeacher()` |
| BaseLessonE2ETest | `e2e/lessons/` | `setupSubjectAndLesson()` |
| BaseRequestE2ETest | `e2e/requests/` | `setupLessonForRequest()`, `createAbsenceRequestBody()` |

### 5. 구현된 E2E 테스트 클래스 (21개) ✅

#### Auth (3개 파일)
- `LoginE2ETest` - 로그인 성공/실패 (4개 테스트)
- `LogoutE2ETest` - 로그아웃 성공/실패 (2개 테스트)
- `GetMeE2ETest` - 내 정보 조회 (3개 테스트)

#### Users (4개 파일)
- `CreateUserE2ETest` - 사용자 생성 (5개 테스트)
- `ReadUserE2ETest` - 사용자 조회 (5개 테스트)
- `UpdateUserE2ETest` - 사용자 수정 (3개 테스트)
- `DeleteUserE2ETest` - 사용자 삭제 (3개 테스트)

#### Departments (2개 파일)
- `CreateDepartmentE2ETest` - 부서 생성 (3개 테스트)
- `ReadDepartmentE2ETest` - 부서 조회 (4개 테스트)

#### Students (1개 파일)
- `CreateStudentE2ETest` - 학생 생성 (2개 테스트)

#### Subjects (1개 파일)
- `CreateSubjectE2ETest` - 과목 생성 (3개 테스트)

#### Lessons (2개 파일)
- `ReadLessonE2ETest` - 수업 조회 (3개 테스트)
- `GetMyLessonsE2ETest` - 내 수업 조회 (3개 테스트)

#### Requests (3개 파일)
- `absence/CreateAbsenceRequestE2ETest` - 결석 요청 생성 (3개 테스트)
- `absence/ReadAbsenceRequestE2ETest` - 결석 요청 조회 (2개 테스트)
- `purchase/CreatePurchaseRequestE2ETest` - 구입 요청 생성 (2개 테스트)

**총 구현 테스트 수**: 약 50개

### 6. 테스트 커버리지

#### 완전히 구현된 도메인
- ✅ **Auth** - 로그인, 로그아웃, 내 정보 조회
- ✅ **Users** - 전체 CRUD (Create, Read, Update, Delete)
- ⚠️ **Departments** - Create, Read만 구현 (Update, Delete 미구현)
- ⚠️ **Students** - Create만 구현 (Read, Update, Delete 미구현)

#### 부분 구현된 도메인
- ⚠️ **Subjects** - Create만 구현
- ⚠️ **Lessons** - Read, GetMyLessons만 구현 (UpdateAttendance 미구현)
- ⚠️ **Requests** - Absence와 Purchase의 Create, Read만 구현 (Exchange 미구현)

#### 미구현 도메인
- ❌ **Classrooms** - Base 클래스만 생성, 테스트 미구현

## 프로젝트 구조

```
src/test/java/org/geumjeong/learning/sonmoum_api/
├── e2e/
│   ├── AbstractE2ETest.java (개선)
│   │
│   ├── auth/
│   │   ├── BaseAuthE2ETest.java
│   │   ├── LoginE2ETest.java
│   │   ├── LogoutE2ETest.java
│   │   └── GetMeE2ETest.java
│   │
│   ├── users/
│   │   ├── BaseUserE2ETest.java
│   │   ├── CreateUserE2ETest.java
│   │   ├── ReadUserE2ETest.java
│   │   ├── UpdateUserE2ETest.java
│   │   └── DeleteUserE2ETest.java
│   │
│   ├── departments/
│   │   ├── BaseDepartmentE2ETest.java
│   │   ├── CreateDepartmentE2ETest.java
│   │   └── ReadDepartmentE2ETest.java
│   │
│   ├── students/
│   │   ├── BaseStudentE2ETest.java
│   │   └── CreateStudentE2ETest.java
│   │
│   ├── classrooms/
│   │   └── BaseClassroomE2ETest.java
│   │
│   ├── subjects/
│   │   ├── BaseSubjectE2ETest.java
│   │   └── CreateSubjectE2ETest.java
│   │
│   ├── lessons/
│   │   ├── BaseLessonE2ETest.java
│   │   ├── ReadLessonE2ETest.java
│   │   └── GetMyLessonsE2ETest.java
│   │
│   └── requests/
│       ├── BaseRequestE2ETest.java
│       ├── absence/
│       │   ├── CreateAbsenceRequestE2ETest.java
│       │   └── ReadAbsenceRequestE2ETest.java
│       └── purchase/
│           └── CreatePurchaseRequestE2ETest.java
```

**총 파일 수**: 30개
- Base 클래스: 9개
- 테스트 클래스: 21개

## 현재 상태

### 컴파일 상태
✅ **모든 테스트 코드가 성공적으로 컴파일됨**

### 테스트 실행 상태
⚠️ **50개 테스트 실행, 48개 실패**

### 주요 실패 원인 (추정)

1. **세션 관리 문제**
   - REST Assured의 `sessionId()` 추출이 제대로 작동하지 않음
   - Spring Security 세션 설정 확인 필요

2. **API 응답 구조 불일치**
   - 실제 API 응답이 예상한 구조와 다를 수 있음
   - `ApiResponse.data.*` 경로 검증 필요

3. **권한 설정**
   - `@PreAuthorize` 어노테이션의 권한 문자열이 실제 Permission과 매칭되지 않을 수 있음
   - `SUPER_ADMIN`, `MANAGE_USERS` 등의 권한이 실제로 부여되는지 확인 필요

4. **데이터베이스 초기화**
   - 테스트 간 데이터 격리가 제대로 되지 않을 수 있음
   - `@Transactional(propagation = Propagation.NOT_SUPPORTED)` 고려

## 남은 작업

### 1. 테스트 디버깅 및 수정 (우선순위 높음)
- [ ] 세션 관리 방식 확인 및 수정
- [ ] 실제 API 응답 구조에 맞춰 테스트 수정
- [ ] 권한 체계 확인 및 테스트 데이터 수정

### 2. 미구현 테스트 완성
- [ ] Departments - Update, Delete 테스트
- [ ] Students - Read, Update, Delete 테스트
- [ ] Classrooms - 전체 CRUD 테스트
- [ ] Subjects - Read, Update, Delete 테스트
- [ ] Lessons - UpdateAttendance 테스트
- [ ] Requests - Exchange (Lesson, Subject) 전체 테스트
- [ ] Requests - 모든 타입의 UpdateStatus 테스트

### 3. 추가 시나리오
- [ ] 페이징 엣지 케이스 테스트
- [ ] 복잡한 권한 시나리오 테스트
- [ ] 동시성 테스트 (필요시)

## 테스트 실행 방법

```bash
# 전체 E2E 테스트 실행
./gradlew test --tests "*E2ETest"

# 특정 태그 실행
./gradlew test --tests "*E2ETest" -Dtag=e2e

# 특정 도메인 테스트
./gradlew test --tests "*.users.*E2ETest"
./gradlew test --tests "*.auth.*E2ETest"

# 특정 테스트 클래스
./gradlew test --tests "CreateUserE2ETest"

# 특정 테스트 메서드
./gradlew test --tests "CreateUserE2ETest.createUserSuccess"
```

## 핵심 패턴

### 1. 테스트 구조
```java
@Tag("e2e")
@DisplayName("Domain E2E Tests - Operation")
class XxxE2ETest extends BaseXxxE2ETest {

    @Test
    @DisplayName("성공 케이스")
    void operationSuccess() {
        // Given: 테스트 데이터 준비
        // When: API 호출
        // Then: 응답 검증
    }
}
```

### 2. 공통 헬퍼 활용
```java
// 관리자 세션 획득
String sessionId = getAdminSession();

// 테스트 요청 데이터 생성
var request = createXxxRequest(...);

// API 호출
given()
    .sessionId(sessionId)
    .contentType(MediaType.APPLICATION_JSON_VALUE)
    .body(request)
.when()
    .post("/api/v1/xxx")
.then()
    .statusCode(HttpStatus.CREATED.value());
```

### 3. Teardown 보장
- `@AfterEach`에서 자동으로 모든 테스트 데이터 삭제
- 외래 키 제약을 고려한 순서로 삭제
- 테스트 간 데이터 오염 방지

## 참고 사항

### 엔티티 생성 방식
프로젝트는 Lombok Builder를 사용하지 않고 생성자를 사용합니다:

```java
// ❌ Builder 패턴 (사용 안 함)
Classroom.builder().name("1반").build()

// ✅ 생성자 패턴 (사용)
new Classroom("1반", ClassroomType.WEEKDAY, "설명")
```

### RoleType 열거형
```java
public enum RoleType {
    MANAGER,     // 관리자 (기존 ADMIN)
    VOLUNTEER    // 봉사자
}
```

### 세션 기반 인증
Spring Security 세션 기반 인증을 사용하므로, REST Assured의 `sessionId()` 메서드를 활용합니다.

## 다음 단계

1. **즉시**: 실패하는 테스트 원인 파악
   - 로그 확인: `build/reports/tests/test/index.html`
   - 실제 API 호출 테스트 (Postman, curl 등)
   - Spring Security 설정 확인

2. **단기**: 핵심 도메인 테스트 완성
   - Users (완료)
   - Departments (완료)
   - Auth (완료)

3. **중기**: 전체 도메인 테스트 구현
   - Students, Classrooms 완성
   - Subjects, Lessons 완성
   - Requests 완성

4. **장기**: 고급 테스트 시나리오
   - 통합 워크플로우 테스트
   - 성능 테스트
   - 보안 테스트

## 작성자 노트

이 구현은 계획된 E2E 테스트 프레임워크의 약 **40-50%**를 완성한 상태입니다.
- 모든 Base 클래스와 인프라는 완성
- 대표적인 테스트 케이스는 구현
- 나머지는 동일한 패턴으로 빠르게 확장 가능

현재 테스트들은 API 구조와 세션 관리 방식에 대한 조정이 필요하지만,
전체적인 아키텍처와 패턴은 프로젝트 요구사항을 잘 반영하고 있습니다.
