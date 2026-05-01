package geumjeongyahak.e2e.users;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import geumjeongyahak.domain.users.v1.dto.request.CreateUserRequest;
import geumjeongyahak.domain.users.v1.dto.response.UserDetailResponse;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@DisplayName("E2E: User 페이지네이션 테스트")
class UserPaginationTest extends UserBaseTest {

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();

        for (int i = 1; i <= 15; i++) {
            CreateUserRequest req = new CreateUserRequest(
                    "pagetest" + i + "@test.com",
                    "pagetest" + i,
                    "Page Test User " + i,
                    "pw_pagetest" + i,
                    "010-" + String.format("%04d", i) + "-5678",
                    "GUEST",
                    null
            );

            var createdUser = given()
                .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
                .contentType(ContentType.JSON)
                .body(req)
            .when()
                .post()
            .then()
                .statusCode(201)
                .extract()
                .as(UserDetailResponse.class);

            userTestHelper.setUser(createdUser.nickname());
        }
    }

    @Test
    @DisplayName("기본 페이지네이션 조회 성공(200 OK)")
    void getAllUsers_DefaultPagination() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .get()
        .then()
            .statusCode(200)
            .body("content", notNullValue())
            .body("page", equalTo(0))
            .body("size", greaterThan(0))
            .body("totalElements", greaterThan(0))
            .body("totalPages", greaterThan(0))
            .log().all();
    }

    @Test
    @DisplayName("페이지 크기 지정 조회 성공(200 OK)")
    void getAllUsers_WithPageSize() {
        int pageSize = 5;

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .queryParam("size", pageSize)
        .when()
            .get()
        .then()
            .statusCode(200)
            .body("content.size()", lessThanOrEqualTo(pageSize))
            .body("size", equalTo(pageSize))
            .body("page", equalTo(0))
            .log().all();
    }

    @Test
    @DisplayName("두 번째 페이지 조회 성공(200 OK)")
    void getAllUsers_SecondPage() {
        int pageSize = 5;
        int pageNumber = 1;

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .queryParam("page", pageNumber)
            .queryParam("size", pageSize)
        .when()
            .get()
        .then()
            .statusCode(200)
            .body("content", notNullValue())
            .body("page", equalTo(pageNumber))
            .body("size", equalTo(pageSize))
            .log().all();
    }

    @Test
    @DisplayName("큰 페이지 크기로 조회 성공(200 OK)")
    void getAllUsers_LargePageSize() {
        int pageSize = 100;

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .queryParam("size", pageSize)
        .when()
            .get()
        .then()
            .statusCode(200)
            .body("content", notNullValue())
            .body("size", equalTo(pageSize))
            .log().all();
    }

    @Test
    @DisplayName("마지막 페이지 조회 성공(200 OK)")
    void getAllUsers_LastPage() {
        int totalPages = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .queryParam("size", 5)
        .when()
            .get()
        .then()
            .statusCode(200)
            .extract()
            .path("totalPages");

        int lastPageNumber = totalPages - 1;
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .queryParam("page", lastPageNumber)
            .queryParam("size", 5)
        .when()
            .get()
        .then()
            .statusCode(200)
            .body("content", notNullValue())
            .body("page", equalTo(lastPageNumber))
            .body("totalPages", equalTo(totalPages))
            .log().all();
    }

    @Test
    @DisplayName("범위를 벗어난 페이지 조회 시 빈 결과 반환(200 OK)")
    void getAllUsers_OutOfRangePage() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .queryParam("page", 9999)
            .queryParam("size", 10)
        .when()
            .get()
        .then()
            .statusCode(200)
            .body("content.size()", equalTo(0))
            .body("page", equalTo(9999))
            .log().all();
    }

    @Test
    @DisplayName("일반 사용자 권한으로 페이지네이션 조회 실패(403 Forbidden)")
    void getAllUsers_Forbidden() {
        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .queryParam("page", 0)
            .queryParam("size", 10)
        .when()
            .get()
        .then()
            .statusCode(403)
            .log().all();
    }

    @Test
    @DisplayName("인증 없이 페이지네이션 조회 실패(401 Unauthorized)")
    void getAllUsers_Unauthorized() {
        given()
            .queryParam("page", 0)
            .queryParam("size", 10)
        .when()
            .get()
        .then()
            .statusCode(401)
            .log().all();
    }
}
