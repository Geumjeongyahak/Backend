package geumjeongyahak.e2e.security;

import static io.restassured.RestAssured.given;

import geumjeongyahak.e2e.BaseE2ETest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("security")
@DisplayName("E2E: 정적 리소스 보안")
class StaticResourceSecurityTest extends BaseE2ETest {

    @Test
    @DisplayName("mock 이미지는 인증 없이 조회된다")
    void mockImage_permitAll() {
        given()
        .when()
            .get("/mock/event-field-day.png")
        .then()
            .statusCode(200);
    }
}
