package geumjeongyahak.e2e.users;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import geumjeongyahak.domain.users.v1.dto.request.CreateUserRequest;
import geumjeongyahak.domain.users.v1.dto.response.UserDetailResponse;

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
                uniqueUsername,
                "Manager User",
                "password123!",
                "010-1234-5678",
                "MANAGER",
                null
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
            .body("email", equalTo(uniqueUsername + "@test.com"))
            .body("nickname", equalTo(uniqueUsername))
            .body("name", equalTo("Manager User"))
            .body("role", equalTo("MANAGER"))
            .log().all()
            .extract()
            .as(UserDetailResponse.class);

        userTestHelper.setUser(res.nickname());
    }

    @Test
    @DisplayName("관리자 권한으로 User 생성 성공 - VOLUNTEER 역할(201 Created)")
    void createUser_Success_Volunteer() {
        String uniqueUsername = "volunteer" + System.currentTimeMillis();
        CreateUserRequest req = new CreateUserRequest(
                uniqueUsername + "@test.com",
                uniqueUsername,
                "Volunteer User",
                "password123!",
                "010-2222-3333",
                "VOLUNTEER",
                null
        );

        var res = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post()
        .then()
            .statusCode(201)
            .body("role", equalTo("VOLUNTEER"))
            .log().all()
            .extract()
            .as(UserDetailResponse.class);

        userTestHelper.setUser(res.nickname());
    }

    @Test
    @DisplayName("관리자 권한으로 User 생성 성공 - GUEST 역할(201 Created)")
    void createUser_Success_Guest() {
        String uniqueUsername = "guest" + System.currentTimeMillis();
        CreateUserRequest req = new CreateUserRequest(
                uniqueUsername + "@test.com",
                uniqueUsername,
                "Guest User",
                "password123!",
                "010-3333-4444",
                "GUEST",
                null
        );

        var res = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post()
        .then()
            .statusCode(201)
            .body("role", equalTo("GUEST"))
            .log().all()
            .extract()
            .as(UserDetailResponse.class);

        userTestHelper.setUser(res.nickname());
    }

    @Test
    @DisplayName("선택적 필드 없이 User 생성 성공(201 Created)")
    void createUser_Success_WithoutOptionalFields() {
        String uniqueUsername = "minimal" + System.currentTimeMillis();
        CreateUserRequest req = new CreateUserRequest(
                uniqueUsername + "@test.com",
                uniqueUsername,
                "Minimal User",
                "password123!",
                null,  // phoneNumber optional
                "GUEST",
                null
        );

        var res = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post()
        .then()
            .statusCode(201)
            .body("email", equalTo(uniqueUsername + "@test.com"))
            .body("phoneNumber", nullValue())
            .log().all()
            .extract()
            .as(UserDetailResponse.class);

        userTestHelper.setUser(res.nickname());
    }

    @Test
    @DisplayName("일반 사용자 권한으로 User 생성 실패(403 Forbidden)")
    void createUser_Forbidden() {
        String uniqueUsername = "forbidden" + System.currentTimeMillis();
        CreateUserRequest req = new CreateUserRequest(
                uniqueUsername + "@test.com",
                uniqueUsername,
                "Forbidden User",
                "password123!",
                "010-1234-5678",
                "GUEST",
                null
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
                uniqueUsername,
                "Unauthorized User",
                "password123!",
                "010-1234-5678",
                "GUEST",
                null
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
        // 기존 admin 사용자와 동일한 nickname 사용
        CreateUserRequest req = new CreateUserRequest(
                "duplicate@test.com",
                TEST_ADMIN_USERNAME,  // 이미 존재하는 nickname
                "Duplicate User",
                "password123!",
                "010-1234-5678",
                "GUEST",
                null
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post()
        .then()
            .statusCode(409)
            .body("code", equalTo("BIZ-01-001"))  // DUPLICATE_USERNAME
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
                uniqueUsername,
                "Short Password User",
                "short",  // 8자 미만
                "010-1234-5678",
                "GUEST",
                null
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
                "invalid-email-format",  // 잘못된 이메일 형식
                uniqueUsername,
                "Invalid Email User",
                "password123!",
                "010-1234-5678",
                "GUEST",
                null
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
                uniqueUsername,
                "Invalid Phone User",
                "password123!",
                "invalid-phone",  // 잘못된 전화번호 형식
                "GUEST",
                null
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
                uniqueUsername,
                "Invalid Role User",
                "password123!",
                "010-1234-5678",
                "INVALID_ROLE",  // 존재하지 않는 역할
                null
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
