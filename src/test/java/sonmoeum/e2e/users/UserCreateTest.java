package sonmoeum.e2e.users;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sonmoeum.domain.auth.enums.RoleType;
import sonmoeum.domain.users.v1.dto.request.CreateUserRequest;
import sonmoeum.domain.users.v1.dto.response.UserResponse;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@DisplayName("E2E: User 생성 테스트")
class UserCreateTest extends UserBaseTest {

    @Test
    @DisplayName("관리자 권한으로 User 생성 성공 - MANAGER 역할(201 Created)")
    void createUser_Success_Manager() {
        String uniqueUsername = "manager" + System.currentTimeMillis();
        CreateUserRequest req = new CreateUserRequest(
                uniqueUsername + "@test.com",
                "password123!",
                "Manager User",
                uniqueUsername + "@test.com",
                "010-1234-5678",
                RoleType.ROLE_MANAGER.name()
        );

        var res = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post()
        .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("username", equalTo(uniqueUsername + "@test.com"))
            .body("name", equalTo("Manager User"))
            .body("roles", notNullValue())
            .body("roles.size()", equalTo(1))
            .body("roles[0].name", equalTo("ROLE_MANAGER"))
            .log().all()
            .extract()
            .as(UserResponse.class);

        userTestHelper.setUser(res.username());
    }

    @Test
    @DisplayName("관리자 권한으로 User 생성 성공 - VOLUNTEER 역할(201 Created)")
    void createUser_Success_Volunteer() {
        String uniqueUsername = "volunteer" + System.currentTimeMillis();
        CreateUserRequest req = new CreateUserRequest(
                uniqueUsername + "@test.com",
                "password123!",
                "Volunteer User",
                uniqueUsername + "@test.com",
                "010-2222-3333",
                RoleType.ROLE_VOLUNTEER.name()
        );

        var res = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post()
        .then()
            .statusCode(201)
            .body("roles[0].name", equalTo("ROLE_VOLUNTEER"))
            .log().all()
            .extract()
            .as(UserResponse.class);

        userTestHelper.setUser(res.username());
    }

    @Test
    @DisplayName("관리자 권한으로 User 생성 성공 - GUEST 역할(201 Created)")
    void createUser_Success_Guest() {
        String uniqueUsername = "guest" + System.currentTimeMillis();
        CreateUserRequest req = new CreateUserRequest(
                uniqueUsername + "@test.com",
                "password123!",
                "Guest User",
                uniqueUsername + "@test.com",
                "010-3333-4444",
                RoleType.ROLE_GUEST.name()
        );

        var res = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post()
        .then()
            .statusCode(201)
            .body("roles[0].name", equalTo("ROLE_GUEST"))
            .log().all()
            .extract()
            .as(UserResponse.class);

        userTestHelper.setUser(res.username());
    }

    @Test
    @DisplayName("선택적 필드 없이 User 생성 성공(201 Created)")
    void createUser_Success_WithoutOptionalFields() {
        String uniqueUsername = "minimal" + System.currentTimeMillis();
        CreateUserRequest req = new CreateUserRequest(
                uniqueUsername + "@test.com",
                "password123!",
                "Minimal User",
                null,  // email optional
                null,  // phoneNumber optional
                RoleType.ROLE_GUEST.name()
        );

        var res = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post()
        .then()
            .statusCode(201)
            .body("username", equalTo(uniqueUsername + "@test.com"))
            .body("email", nullValue())
            .body("phoneNumber", nullValue())
            .log().all()
            .extract()
            .as(UserResponse.class);

        userTestHelper.setUser(res.username());
    }

    @Test
    @DisplayName("일반 사용자 권한으로 User 생성 실패(403 Forbidden)")
    void createUser_Forbidden() {
        String uniqueUsername = "forbidden" + System.currentTimeMillis();
        CreateUserRequest req = new CreateUserRequest(
                uniqueUsername + "@test.com",
                "password123!",
                "Forbidden User",
                uniqueUsername + "@test.com",
                "010-1234-5678",
                RoleType.ROLE_GUEST.name()
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post()
        .then()
            .statusCode(403)
            .body("code", equalTo("AUTHZ001"))  // ACCESS_DENIED
            .log().all();
    }

    @Test
    @DisplayName("인증 없이 User 생성 실패(401 Unauthorized)")
    void createUser_Unauthorized() {
        String uniqueUsername = "unauthorized" + System.currentTimeMillis();
        CreateUserRequest req = new CreateUserRequest(
                uniqueUsername + "@test.com",
                "password123!",
                "Unauthorized User",
                uniqueUsername + "@test.com",
                "010-1234-5678",
                RoleType.ROLE_GUEST.name()
        );

        given()
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post()
        .then()
            .statusCode(401)
            .log().all();
    }

    @Test
    @DisplayName("중복된 username으로 User 생성 실패(409 Conflict)")
    void createUser_DuplicateUsername() {
        // 기존 admin 사용자와 동일한 username 사용
        CreateUserRequest req = new CreateUserRequest(
                TEST_ADMIN_USERNAME,  // 이미 존재하는 username
                "password123!",
                "Duplicate User",
                "duplicate@test.com",
                "010-1234-5678",
                RoleType.ROLE_GUEST.name()
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post()
        .then()
            .statusCode(409)
            .body("code", equalTo("BIZ002"))  // DUPLICATE_USERNAME
            .log().all();
    }

    @Test
    @DisplayName("필수 필드 누락 시 User 생성 실패(400 Bad Request)")
    void createUser_MissingRequiredFields() {
        String invalidReq = """
            {
                "name": "Test User"
            }
            """;

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(invalidReq)
        .when()
            .post()
        .then()
            .statusCode(400)
            .body("code", equalTo("VAL001"))  // VALIDATION_ERROR
            .log().all();
    }

    @Test
    @DisplayName("짧은 비밀번호로 User 생성 실패(400 Bad Request)")
    void createUser_ShortPassword() {
        String uniqueUsername = "shortpw" + System.currentTimeMillis();
        CreateUserRequest req = new CreateUserRequest(
                uniqueUsername + "@test.com",
                "short",  // 8자 미만
                "Short Password User",
                uniqueUsername + "@test.com",
                "010-1234-5678",
                RoleType.ROLE_GUEST.name()
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post()
        .then()
            .statusCode(400)
            .body("code", equalTo("VAL001"))  // VALIDATION_ERROR
            .log().all();
    }

    @Test
    @DisplayName("잘못된 이메일 형식으로 User 생성 실패(400 Bad Request)")
    void createUser_InvalidEmail() {
        String uniqueUsername = "invalidemail" + System.currentTimeMillis();
        CreateUserRequest req = new CreateUserRequest(
                uniqueUsername + "@test.com",
                "password123!",
                "Invalid Email User",
                "invalid-email-format",  // 잘못된 이메일 형식
                "010-1234-5678",
                RoleType.ROLE_GUEST.name()
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post()
        .then()
            .statusCode(400)
            .body("code", equalTo("VAL001"))  // VALIDATION_ERROR
            .log().all();
    }

    @Test
    @DisplayName("잘못된 전화번호 형식으로 User 생성 실패(400 Bad Request)")
    void createUser_InvalidPhoneNumber() {
        String uniqueUsername = "invalidphone" + System.currentTimeMillis();
        CreateUserRequest req = new CreateUserRequest(
                uniqueUsername + "@test.com",
                "password123!",
                "Invalid Phone User",
                uniqueUsername + "@test.com",
                "invalid-phone",  // 잘못된 전화번호 형식
                RoleType.ROLE_GUEST.name()
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post()
        .then()
            .statusCode(400)
            .body("code", equalTo("VAL001"))  // VALIDATION_ERROR
            .log().all();
    }

    @Test
    @DisplayName("유효하지 않은 역할로 User 생성 실패(400 Bad Request)")
    void createUser_InvalidRole() {
        String uniqueUsername = "invalidrole" + System.currentTimeMillis();
        CreateUserRequest req = new CreateUserRequest(
                uniqueUsername + "@test.com",
                "password123!",
                "Invalid Role User",
                uniqueUsername + "@test.com",
                "010-1234-5678",
                "INVALID_ROLE"  // 존재하지 않는 역할
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post()
        .then()
            .statusCode(400)
            .log().all();
    }
}
