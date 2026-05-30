package geumjeongyahak.e2e.event;

import static io.restassured.RestAssured.given;
import static java.util.Map.entry;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("E2E: Event API 테스트")
class EventApiTest extends BaseEventTest {

    @Test
    @DisplayName("인증 없이 행사 목록 기간 조회 성공(200)")
    void getEvents_publicWithDateRange_returnsMatchingEvents() {
        insertEvent(100L, "문학의 밤", "2026-05-13", "19:00:00", "21:00:00", false);
        insertEvent(101L, "봄맞이 행사", "2026-05-02", "18:00:00", "20:00:00", false);
        insertEvent(102L, "범위 밖 행사", "2026-06-01", "18:00:00", "20:00:00", false);
        insertEvent(103L, "삭제 행사", "2026-05-03", "18:00:00", "20:00:00", true);

        given()
            .queryParam("startDate", "2026-05-01")
            .queryParam("endDate", "2026-05-31")
            .get()
            .then()
            .statusCode(200)
            .body("content.id", contains(101, 100))
            .body("content.id", not(hasItem(102)))
            .body("content.id", not(hasItem(103)))
            .body("size", equalTo(20))
            .body("totalElements", equalTo(2));
    }

    @Test
    @DisplayName("인증 없이 행사 상세 조회 성공(200)")
    void getEvent_public_returnsEvent() {
        insertEvent(110L, "문학의 밤", "2026-05-13", "19:00:00", "21:00:00", false);

        given()
            .get("/{eventId}", 110L)
            .then()
            .statusCode(200)
            .body("id", equalTo(110))
            .body("title", equalTo("문학의 밤"))
            .body("eventDate", equalTo("2026-05-13"))
            .body("lastModifiedById", equalTo(1))
            .body("lastModifiedByName", equalTo("관리자"));
    }

    @Test
    @DisplayName("관리자가 행사 등록 성공(201)")
    void createEvent_asAdmin_returnsCreated() {
        Map<String, Object> request = eventRequest(
            "문학의 밤",
            "문학의 밤 행사 설명",
            "2026-05-13",
            "19:00:00",
            "21:00:00"
        );

        given()
            .basePath("/api/v1/admin/events")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType("application/json")
            .body(request)
            .when()
            .post()
            .then()
            .statusCode(201)
            .body("title", equalTo("문학의 밤"))
            .body("eventDate", equalTo("2026-05-13"))
            .body("lastModifiedById", equalTo(1));
    }

    @Test
    @DisplayName("event:manage:* 권한으로 행사 등록 성공(201)")
    void createEvent_withManagePermission_returnsCreated() {
        Map<String, Object> request = eventRequest(
            "행사 관리자 등록",
            "행사 관리자 권한으로 등록",
            "2026-05-14",
            "18:00:00",
            "20:00:00"
        );

        given()
            .basePath("/api/v1/admin/events")
            .header(AUTH_HEADER, getAuthHeader(eventManagerToken))
            .contentType("application/json")
            .body(request)
            .when()
            .post()
            .then()
            .statusCode(201)
            .body("title", equalTo("행사 관리자 등록"));
    }

    @Test
    @DisplayName("권한 없는 사용자가 행사 등록 실패(403)")
    void createEvent_asGuest_returnsForbidden() {
        Map<String, Object> request = eventRequest(
            "권한 없는 등록",
            "실패해야 하는 요청",
            "2026-05-15",
            "18:00:00",
            "20:00:00"
        );

        given()
            .basePath("/api/v1/admin/events")
            .header(AUTH_HEADER, getAuthHeader(guestToken))
            .contentType("application/json")
            .body(request)
            .when()
            .post()
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("행사 수정은 부분 수정 가능하고 description 빈 문자열 허용(200)")
    void updateEvent_partialUpdate_returnsUpdated() {
        Long eventId = createEventAndGetId(eventRequest(
            "수정 전 행사",
            "설명",
            "2026-05-16",
            "18:00:00",
            "20:00:00"
        ), adminToken);

        Map<String, Object> request = Map.ofEntries(
            entry("title", "수정 후 행사"),
            entry("description", "")
        );

        given()
            .basePath("/api/v1/admin/events")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType("application/json")
            .body(request)
            .when()
            .patch("/{eventId}", eventId)
            .then()
            .statusCode(200)
            .body("title", equalTo("수정 후 행사"))
            .body("description", equalTo(""))
            .body("eventDate", equalTo("2026-05-16"))
            .body("startTime", equalTo("18:00:00"))
            .body("endTime", equalTo("20:00:00"));
    }

    @Test
    @DisplayName("행사 수정 시 시간은 함께 전달해야 함(400)")
    void updateEvent_withOnlyStartTime_returnsBadRequest() {
        Long eventId = createEventAndGetId(eventRequest(
            "시간 수정 행사",
            "설명",
            "2026-05-17",
            "18:00:00",
            "20:00:00"
        ), adminToken);

        given()
            .basePath("/api/v1/admin/events")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType("application/json")
            .body(Map.of("startTime", "19:00:00"))
            .when()
            .patch("/{eventId}", eventId)
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("종료 시간이 시작 시간보다 빠르면 행사 등록 실패(400)")
    void createEvent_invalidTime_returnsBadRequest() {
        Map<String, Object> request = eventRequest(
            "잘못된 시간",
            "실패해야 하는 요청",
            "2026-05-18",
            "20:00:00",
            "19:00:00"
        );

        given()
            .basePath("/api/v1/admin/events")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType("application/json")
            .body(request)
            .when()
            .post()
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("행사 삭제 후 상세 조회 실패(404)")
    void deleteEvent_thenGetReturnsNotFound() {
        Long eventId = createEventAndGetId(eventRequest(
            "삭제 대상 행사",
            "삭제될 행사",
            "2026-05-19",
            "18:00:00",
            "20:00:00"
        ), adminToken);

        given()
            .basePath("/api/v1/admin/events")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .when()
            .delete("/{eventId}", eventId)
            .then()
            .statusCode(204);

        given()
            .basePath("/api/v1/events")
            .get("/{eventId}", eventId)
            .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("행사 목록 조회에서 startDate/endDate 중 하나만 전달하면 실패(400)")
    void getEvents_withIncompleteDateRange_returnsBadRequest() {
        given()
            .queryParam("startDate", "2026-05-01")
            .get()
            .then()
            .statusCode(400);
    }
}
