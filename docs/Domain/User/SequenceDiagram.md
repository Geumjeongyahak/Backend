# User Domain Sequence Diagrams

각 API의 처리 흐름과 side effect를 포함한 시퀀스 다이어그램입니다.

---

## 1. 사용자 목록 조회 `GET /api/v1/users`

읽기 전용. DB 변경 없음.

```mermaid
sequenceDiagram
    participant Client
    participant JwtFilter
    participant UserAdminController
    participant UserCrudService
    participant UserRepository

    Client->>JwtFilter: GET /api/v1/users?page=0&size=20
    JwtFilter->>JwtFilter: JWT 검증 및 SecurityContext 설정
    JwtFilter->>UserAdminController: 요청 전달
    UserAdminController->>UserAdminController: @PreAuthorize 권한 검사<br/>(ADMIN or user:read:*)
    UserAdminController->>UserCrudService: getAllUsersPagination(request)
    UserCrudService->>UserRepository: findAll(PageRequest)
    UserRepository-->>UserCrudService: Page<User>
    UserCrudService-->>UserAdminController: PaginationResponse<UserSimpleResponse>
    UserAdminController-->>Client: 200 OK
```

---

## 2. 사용자 상세 조회 `GET /api/v1/users/{userId}`

읽기 전용. DB 변경 없음.

```mermaid
sequenceDiagram
    participant Client
    participant JwtFilter
    participant UserAdminController
    participant UserCrudService
    participant UserProxyService
    participant UserRepository

    Client->>JwtFilter: GET /api/v1/users/1
    JwtFilter->>JwtFilter: JWT 검증 및 SecurityContext 설정
    JwtFilter->>UserAdminController: 요청 전달
    UserAdminController->>UserAdminController: @PreAuthorize 권한 검사<br/>(ADMIN or user:read:*)
    UserAdminController->>UserCrudService: getUserById(userId)
    UserCrudService->>UserProxyService: getById(userId)
    UserProxyService->>UserRepository: findById(userId)
    alt 사용자 없음
        UserRepository-->>UserProxyService: Optional.empty()
        UserProxyService-->>UserCrudService: UserNotFoundException
        UserCrudService-->>UserAdminController: 예외 전파
        UserAdminController-->>Client: 404 RES-01-001
    else 사용자 있음
        UserRepository-->>UserProxyService: Optional<User>
        UserProxyService-->>UserCrudService: User
        UserCrudService-->>UserAdminController: UserDetailResponse
        UserAdminController-->>Client: 200 OK
    end
```

---

## 3. 사용자 생성 `POST /api/v1/users`

**Side Effect**: `users` 저장 + `user_credentials` (LOCAL) 생성

```mermaid
sequenceDiagram
    participant Client
    participant JwtFilter
    participant UserAdminController
    participant UserCrudService
    participant UserProxyService
    participant UserRepository
    participant DepartmentProxyService
    participant UserCredentialService
    participant CredentialRepository

    Client->>JwtFilter: POST /api/v1/users (CreateUserRequest)
    JwtFilter->>JwtFilter: JWT 검증 및 SecurityContext 설정
    JwtFilter->>UserAdminController: 요청 전달
    UserAdminController->>UserAdminController: @PreAuthorize 권한 검사<br/>(ADMIN or user:manage:*)
    UserAdminController->>UserCrudService: createUser(request)

    UserCrudService->>UserProxyService: existsByNickname(nickname)
    UserProxyService->>UserRepository: existsByNickname(nickname)
    alt 닉네임 중복
        UserRepository-->>UserProxyService: true
        UserCrudService-->>UserAdminController: DuplicateNicknameException
        UserAdminController-->>Client: 409 BIZ-01-001
    end

    UserCrudService->>UserProxyService: existsByEmail(email)
    UserProxyService->>UserRepository: existsByEmail(email)
    alt 이메일 중복
        UserRepository-->>UserProxyService: true
        UserCrudService-->>UserAdminController: DuplicateEmailException
        UserAdminController-->>Client: 409 BIZ-01-002
    end

    opt departmentId 있음
        UserCrudService->>DepartmentProxyService: getById(departmentId)
        alt 부서 없음
            DepartmentProxyService-->>UserCrudService: DepartmentNotFoundException
            UserCrudService-->>UserAdminController: 예외 전파
            UserAdminController-->>Client: 404 RES-02-001
        end
    end

    Note over UserCrudService,CredentialRepository: 트랜잭션 시작
    UserCrudService->>UserRepository: save(user)
    UserRepository-->>UserCrudService: savedUser
    Note right of UserRepository: [Side Effect] users 레코드 생성

    UserCrudService->>UserCredentialService: createLocalCredential(user, email, password)
    UserCredentialService->>CredentialRepository: save(UserCredential{LOCAL})
    Note right of CredentialRepository: [Side Effect] user_credentials 레코드 생성<br/>provider=LOCAL, password_hash=BCrypt(password)
    Note over UserCrudService,CredentialRepository: 트랜잭션 커밋

    UserCrudService-->>UserAdminController: UserDetailResponse
    UserAdminController-->>Client: 201 Created
```

---

## 4. 사용자 수정 - 관리자 `PATCH /api/v1/users/{userId}`

**Side Effect**: `users` 변경, 이메일/비밀번호 변경 시 `user_credentials` 함께 갱신

```mermaid
sequenceDiagram
    participant Client
    participant JwtFilter
    participant UserAdminController
    participant UserCrudService
    participant UserProxyService
    participant UserRepository
    participant DepartmentProxyService
    participant UserCredentialService
    participant CredentialRepository

    Client->>JwtFilter: PATCH /api/v1/users/1 (UpdateUserRequest)
    JwtFilter->>JwtFilter: JWT 검증 및 SecurityContext 설정
    JwtFilter->>UserAdminController: 요청 전달
    UserAdminController->>UserAdminController: @PreAuthorize 권한 검사<br/>(ADMIN or user:manage:*)
    UserAdminController->>UserCrudService: updateUser(userId, request)

    UserCrudService->>UserRepository: findById(userId)
    alt 사용자 없음
        UserRepository-->>UserCrudService: Optional.empty()
        UserCrudService-->>UserAdminController: UserNotFoundException
        UserAdminController-->>Client: 404 RES-01-001
    end

    Note over UserCrudService,CredentialRepository: 트랜잭션 시작

    opt nickname 변경 요청
        UserCrudService->>UserProxyService: existsByNickname(newNickname)
        alt 닉네임 중복
            UserCrudService-->>Client: 409 BIZ-01-001
        end
        UserCrudService->>UserCrudService: user.setNickname(newNickname)
    end

    opt email 변경 요청
        UserCrudService->>UserRepository: existsByEmail(newEmail)
        alt 이메일 중복
            UserCrudService-->>Client: 409 BIZ-01-002
        end
        UserCrudService->>UserCrudService: user.setEmail(newEmail)
        UserCrudService->>UserCredentialService: updateLocalCredentialEmail(user, newEmail)
        UserCredentialService->>CredentialRepository: 이메일 갱신
        Note right of CredentialRepository: [Side Effect] user_credentials.credential_email 변경
    end

    opt password 변경 요청
        UserCrudService->>UserCredentialService: updateLocalPassword(user, BCrypt(newPassword))
        UserCredentialService->>CredentialRepository: 비밀번호 해시 갱신
        Note right of CredentialRepository: [Side Effect] user_credentials.password_hash 변경
    end

    opt role 변경 요청
        UserCrudService->>UserCrudService: user.setRole(newRole)
        Note right of UserCrudService: [Side Effect] 인가 범위 변경
    end

    opt departmentId 변경 요청
        UserCrudService->>DepartmentProxyService: getById(departmentId)
        UserCrudService->>UserCrudService: user.setDepartment(dept)
        Note right of UserCrudService: [Side Effect] 소속 부서 변경
    end

    Note right of UserRepository: [Side Effect] users 레코드 변경 (Dirty Checking)
    Note over UserCrudService,CredentialRepository: 트랜잭션 커밋

    UserCrudService-->>UserAdminController: UserDetailResponse
    UserAdminController-->>Client: 200 OK
```

---

## 5. 사용자 삭제 `DELETE /api/v1/users/{userId}`

**Side Effect**: `users` 삭제 + CASCADE로 `user_credentials`, `user_permissions` 삭제

```mermaid
sequenceDiagram
    participant Client
    participant JwtFilter
    participant UserAdminController
    participant UserCrudService
    participant UserRepository

    Client->>JwtFilter: DELETE /api/v1/users/1
    JwtFilter->>JwtFilter: JWT 검증 및 SecurityContext 설정
    JwtFilter->>UserAdminController: 요청 전달
    UserAdminController->>UserAdminController: @PreAuthorize 권한 검사<br/>(ADMIN or user:manage:*)
    UserAdminController->>UserCrudService: deleteUserById(userId)

    UserCrudService->>UserRepository: existsById(userId)
    alt 사용자 없음
        UserRepository-->>UserCrudService: false
        UserCrudService-->>UserAdminController: UserNotFoundException
        UserAdminController-->>Client: 404 RES-01-001
    end

    Note over UserCrudService,UserRepository: 트랜잭션 시작
    UserCrudService->>UserRepository: deleteById(userId)
    Note right of UserRepository: [Side Effect] users 레코드 삭제
    Note right of UserRepository: [Side Effect] CASCADE → user_credentials 삭제<br/>(orphanRemoval=true)
    Note right of UserRepository: [Side Effect] CASCADE → user_permissions 삭제<br/>(orphanRemoval=true)
    Note over UserCrudService,UserRepository: 트랜잭션 커밋

    UserCrudService-->>UserAdminController: void
    UserAdminController-->>Client: 204 No Content
```

---

## 6. 본인 조회 `GET /api/v1/users/me`

읽기 전용. DB 변경 없음.

```mermaid
sequenceDiagram
    participant Client
    participant JwtFilter
    participant UserSelfController
    participant UserCrudService
    participant UserProxyService
    participant UserRepository

    Client->>JwtFilter: GET /api/v1/users/me
    JwtFilter->>JwtFilter: JWT 검증 및 SecurityContext 설정
    JwtFilter->>UserSelfController: 요청 전달 (CustomUserDetails 주입)
    UserSelfController->>UserSelfController: @PreAuthorize isAuthenticated() 검사
    UserSelfController->>UserCrudService: getUserById(userDetails.getUserId())
    UserCrudService->>UserProxyService: getById(userId)
    UserProxyService->>UserRepository: findById(userId)
    UserRepository-->>UserProxyService: Optional<User>
    UserProxyService-->>UserCrudService: User
    UserCrudService-->>UserSelfController: UserDetailResponse
    UserSelfController-->>Client: 200 OK
```

---

## 7. 본인 수정 `PATCH /api/v1/users/me`

**Side Effect**: `users` 변경, 이메일/비밀번호 변경 시 `user_credentials` 함께 갱신  
`role`, `departmentId`는 변경 불가 (UpdateSelfRequest에서 제외됨)

```mermaid
sequenceDiagram
    participant Client
    participant JwtFilter
    participant UserSelfController
    participant UserCrudService
    participant UserProxyService
    participant UserRepository
    participant UserCredentialService
    participant CredentialRepository

    Client->>JwtFilter: PATCH /api/v1/users/me (UpdateSelfRequest)
    JwtFilter->>JwtFilter: JWT 검증 및 SecurityContext 설정
    JwtFilter->>UserSelfController: 요청 전달 (CustomUserDetails 주입)
    UserSelfController->>UserSelfController: @PreAuthorize isAuthenticated() 검사
    UserSelfController->>UserCrudService: updateUser(userDetails.getUserId(), UpdateSelfRequest)

    UserCrudService->>UserRepository: findById(userId)
    UserRepository-->>UserCrudService: User

    Note over UserCrudService,CredentialRepository: 트랜잭션 시작
    Note over UserCrudService: role=empty, departmentId=empty → 변경 불가

    opt nickname 변경 요청
        UserCrudService->>UserProxyService: existsByNickname(newNickname)
        alt 닉네임 중복
            UserCrudService-->>Client: 409 BIZ-01-001
        end
        UserCrudService->>UserCrudService: user.setNickname(newNickname)
    end

    opt email 변경 요청
        UserCrudService->>UserRepository: existsByEmail(newEmail)
        alt 이메일 중복
            UserCrudService-->>Client: 409 BIZ-01-002
        end
        UserCrudService->>UserCrudService: user.setEmail(newEmail)
        UserCrudService->>UserCredentialService: updateLocalCredentialEmail(user, newEmail)
        UserCredentialService->>CredentialRepository: 이메일 갱신
        Note right of CredentialRepository: [Side Effect] user_credentials.credential_email 변경<br/>다음 로그인 시 새 이메일로만 가능
    end

    opt password 변경 요청
        UserCrudService->>UserCredentialService: updateLocalPassword(user, BCrypt(newPassword))
        UserCredentialService->>CredentialRepository: 비밀번호 해시 갱신
        Note right of CredentialRepository: [Side Effect] user_credentials.password_hash 변경<br/>기존 비밀번호 즉시 무효화
    end

    Note right of UserRepository: [Side Effect] users 레코드 변경 (Dirty Checking)
    Note over UserCrudService,CredentialRepository: 트랜잭션 커밋

    UserCrudService-->>UserSelfController: UserDetailResponse
    UserSelfController-->>Client: 200 OK
```

---

## 8. 권한 목록 조회 `GET /api/v1/users/{userId}/permissions`

읽기 전용. DB 변경 없음.

```mermaid
sequenceDiagram
    participant Client
    participant JwtFilter
    participant UserPermissionController
    participant UserPermissionService
    participant UserPermissionRepository

    Client->>JwtFilter: GET /api/v1/users/1/permissions
    JwtFilter->>JwtFilter: JWT 검증 및 SecurityContext 설정
    JwtFilter->>UserPermissionController: 요청 전달
    UserPermissionController->>UserPermissionController: @PreAuthorize 권한 검사<br/>(ADMIN or user:read:*)
    UserPermissionController->>UserPermissionService: getAllPermissions(userId)
    UserPermissionService->>UserPermissionRepository: findAllByUserId(userId)
    UserPermissionRepository-->>UserPermissionService: List<UserPermission>
    UserPermissionService-->>UserPermissionController: List<PermissionResponse>
    UserPermissionController-->>Client: 200 OK
```

---

## 9. 권한 추가 `POST /api/v1/users/{userId}/permissions`

**Side Effect**: `user_permissions` 레코드 추가 (이미 존재하면 무시)  
이후 해당 사용자의 인가 결과에 즉시 영향

```mermaid
sequenceDiagram
    participant Client
    participant JwtFilter
    participant UserPermissionController
    participant UserPermissionService
    participant UserProxyService
    participant UserPermissionRepository

    Client->>JwtFilter: POST /api/v1/users/1/permissions<br/>{ "permissionCode": "user:manage:*" }
    JwtFilter->>JwtFilter: JWT 검증 및 SecurityContext 설정
    JwtFilter->>UserPermissionController: 요청 전달
    UserPermissionController->>UserPermissionController: @PreAuthorize 권한 검사<br/>(ADMIN or user:manage:*)
    UserPermissionController->>UserPermissionService: addPermission(userId, permissionCode)

    UserPermissionService->>UserProxyService: getById(userId)
    alt 사용자 없음
        UserProxyService-->>UserPermissionService: UserNotFoundException
        UserPermissionService-->>UserPermissionController: 예외 전파
        UserPermissionController-->>Client: 404 RES-01-001
    end

    Note over UserPermissionService,UserPermissionRepository: 트랜잭션 시작
    UserPermissionService->>UserPermissionRepository: findByUserIdAndPermissionCode(userId, permissionCode)
    alt 이미 존재
        UserPermissionRepository-->>UserPermissionService: Optional<UserPermission>
        Note over UserPermissionService: 중복 저장 없이 skip
    else 없음
        UserPermissionRepository-->>UserPermissionService: Optional.empty()
        UserPermissionService->>UserPermissionRepository: save(new UserPermission(user, permissionCode))
        Note right of UserPermissionRepository: [Side Effect] user_permissions 레코드 추가<br/>이후 해당 사용자 @PreAuthorize 통과 범위 변경
    end
    Note over UserPermissionService,UserPermissionRepository: 트랜잭션 커밋

    UserPermissionService->>UserPermissionRepository: findAllByUserId(userId)
    UserPermissionRepository-->>UserPermissionService: List<UserPermission>
    UserPermissionService-->>UserPermissionController: List<PermissionResponse>
    UserPermissionController-->>Client: 200 OK (갱신된 전체 권한 목록)
```

---

## 10. 권한 제거 `DELETE /api/v1/users/{userId}/permissions`

**Side Effect**: `user_permissions` 레코드 삭제 (없으면 무시)  
이후 해당 사용자의 인가 결과에 즉시 영향

```mermaid
sequenceDiagram
    participant Client
    participant JwtFilter
    participant UserPermissionController
    participant UserPermissionService
    participant UserProxyService
    participant UserPermissionRepository

    Client->>JwtFilter: DELETE /api/v1/users/1/permissions<br/>{ "permissionCode": "user:manage:*" }
    JwtFilter->>JwtFilter: JWT 검증 및 SecurityContext 설정
    JwtFilter->>UserPermissionController: 요청 전달
    UserPermissionController->>UserPermissionController: @PreAuthorize 권한 검사<br/>(ADMIN or user:manage:*)
    UserPermissionController->>UserPermissionService: removePermission(userId, permissionCode)

    UserPermissionService->>UserProxyService: getById(userId)
    alt 사용자 없음
        UserProxyService-->>UserPermissionService: UserNotFoundException
        UserPermissionService-->>UserPermissionController: 예외 전파
        UserPermissionController-->>Client: 404 RES-01-001
    end

    Note over UserPermissionService,UserPermissionRepository: 트랜잭션 시작
    UserPermissionService->>UserPermissionRepository: findByUserIdAndPermissionCode(userId, permissionCode)
    alt 권한 존재
        UserPermissionRepository-->>UserPermissionService: Optional<UserPermission>
        UserPermissionService->>UserPermissionRepository: delete(userPermission)
        Note right of UserPermissionRepository: [Side Effect] user_permissions 레코드 삭제<br/>이후 해당 사용자 @PreAuthorize 통과 범위 변경
    else 없음
        UserPermissionRepository-->>UserPermissionService: Optional.empty()
        Note over UserPermissionService: 삭제 대상 없음, 오류 없이 진행
    end
    Note over UserPermissionService,UserPermissionRepository: 트랜잭션 커밋

    UserPermissionService->>UserPermissionRepository: findAllByUserId(userId)
    UserPermissionRepository-->>UserPermissionService: List<UserPermission>
    UserPermissionService-->>UserPermissionController: List<PermissionResponse>
    UserPermissionController-->>Client: 200 OK (갱신된 전체 권한 목록)
```
