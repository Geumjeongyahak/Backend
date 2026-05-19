package geumjeongyahak.e2e.request.purchase;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import geumjeongyahak.domain.vendor.repository.VendorBalanceHistoryRepository;
import geumjeongyahak.domain.vendor.repository.VendorRepository;
import geumjeongyahak.e2e.request.RequestBaseTest;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Tag("purchase-request")
@DisplayName("E2E: 거래처 관리 테스트")
class VendorAdminTest extends RequestBaseTest {

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private VendorBalanceHistoryRepository vendorBalanceHistoryRepository;

    private Long createdVendorId;

    @AfterEach
    void cleanup() {
        if (createdVendorId != null) {
            vendorBalanceHistoryRepository.deleteAllByVendor_Id(createdVendorId);
            if (vendorRepository.existsById(createdVendorId)) {
                vendorRepository.deleteById(createdVendorId);
            }
            createdVendorId = null;
        }
    }

    @Test
    @DisplayName("관리자가 거래처 생성 후 조회 → 201, 목록 포함")
    void createVendor_asAdmin_returns201() {
        createdVendorId = given()
            .basePath("/api/v1/admin/vendors")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "name", "테스트 거래처",
                "description", "테스트용"
            ))
            .post()
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("name", equalTo("테스트 거래처"))
            .body("balance", equalTo(0))
            .extract()
            .jsonPath()
            .getLong("id");

        var ids = given()
            .basePath("/api/v1/admin/vendors")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .queryParam("keyword", "테스트")
            .get()
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList("id", Long.class);

        assertThat(ids).contains(createdVendorId);
    }

    @Test
    @DisplayName("거래처 충전 → 잔액 증가, 이력 저장")
    void chargeVendor_increasesBalanceAndCreatesHistory() {
        createdVendorId = createVendor("충전 거래처");

        given()
            .basePath("/api/v1/admin/vendors")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "amount", 100000L,
                "memo", "테스트 충전"
            ))
            .post("/{vendorId}/charges", createdVendorId)
            .then()
            .statusCode(200)
            .body("balance", equalTo(100000));

        given()
            .basePath("/api/v1/admin/vendors")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .get("/{vendorId}/histories", createdVendorId)
            .then()
            .statusCode(200)
            .body("", hasSize(1))
            .body("[0].type", equalTo("CHARGE"))
            .body("[0].amount", equalTo(100000));
    }

    private Long createVendor(String name) {
        return given()
            .basePath("/api/v1/admin/vendors")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("name", name))
            .post()
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");
    }
}
