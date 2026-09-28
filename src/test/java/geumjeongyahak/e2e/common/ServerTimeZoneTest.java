package geumjeongyahak.e2e.common;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import geumjeongyahak.domain.purchase_request.repository.PurchaseRequestRepository;
import geumjeongyahak.e2e.request.RequestBaseTest;
import io.restassured.http.ContentType;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 서버 OS 시간대와 상관없이 저장되는 시각이 한국 시간인지 본다.
 * 테스트 JVM 은 dev 서버와 같은 UTC 로 뜬다 (build.gradle).
 */
@DisplayName("E2E: 서버 시간대 테스트")
class ServerTimeZoneTest extends RequestBaseTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PurchaseRequestRepository purchaseRequestRepository;

    private Long requestId;

    @AfterEach
    void cleanup() {
        if (requestId != null && purchaseRequestRepository.existsById(requestId)) {
            purchaseRequestRepository.deleteById(requestId);
        }
    }

    @Test
    @DisplayName("JVM 기본 시간대는 한국 시간이다")
    void defaultTimeZone_isSeoul() {
        assertThat(ZoneId.systemDefault()).isEqualTo(SEOUL);
    }

    @Test
    @DisplayName("JPA 가 채우는 생성 시각은 한국 시간이다")
    void createdAt_isSeoulTime() {
        requestId = createPurchaseRequest(getAuthHeader(volunteerToken), CLASSROOM_ID, "시간대 검증", "생성 시각", 1000L);

        assertThat(savedTime("created_at")).isCloseTo(LocalDateTime.now(SEOUL), within(Duration.ofMinutes(1)));
    }

    @Test
    @DisplayName("엔티티가 인자 없는 now() 로 남기는 승인 시각은 한국 시간이다")
    void approvalAt_isSeoulTime() {
        requestId = createPurchaseRequest(getAuthHeader(volunteerToken), CLASSROOM_ID, "시간대 검증", "승인 시각", 1000L);
        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", "시간대 검증 승인"))
            .patch("/{requestId}/approve", requestId)
            .then()
            .statusCode(200);

        assertThat(savedTime("approval_at")).isCloseTo(LocalDateTime.now(SEOUL), within(Duration.ofMinutes(1)));
    }

    private LocalDateTime savedTime(String column) {
        return jdbcTemplate.queryForObject(
            "SELECT " + column + " FROM purchase_requests WHERE id = ?", Timestamp.class, requestId
        ).toLocalDateTime();
    }
}
