package sonmoeum.e2e.requests.purchase;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import sonmoeum.e2e.requests.BaseRequestE2ETest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

@Tag("e2e")
@DisplayName("Purchase Request E2E Tests - Create")
class CreatePurchaseRequestE2ETest extends BaseRequestE2ETest {

    @Test
    @DisplayName("구입 요청 생성 성공")
    void createPurchaseRequestSuccess() {
        String sessionCookie = getVolunteerSession();

        var requestBody = createPurchaseRequestBody("색연필 세트", 5, 50000);

        given()
            .cookie("SESSION", sessionCookie)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(requestBody)
        .when()
            .post("/api/v1/requests/purchase")
        .then()
            .statusCode(HttpStatus.CREATED.value())
            .body("data.itemName", equalTo("색연필 세트"))
            .body("data.quantity", equalTo(5))
            .body("data.id", notNullValue());
    }

    @Test
    @DisplayName("구입 요청 생성 실패 - 인증 필요")
    void createPurchaseRequestFailUnauthenticated() {
        var requestBody = createPurchaseRequestBody("색연필 세트", 5, 50000);

        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(requestBody)
        .when()
            .post("/api/v1/requests/purchase")
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }
}
