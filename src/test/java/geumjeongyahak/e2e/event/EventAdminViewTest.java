package geumjeongyahak.e2e.event;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

import io.restassured.http.ContentType;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

@DisplayName("E2E: 행사 일정 관리자 화면 테스트")
@ResourceLock("event-e2e-shared-state")
class EventAdminViewTest extends BaseEventTest {

    @Test
    @DisplayName("관리자 행사 목록 화면을 조회할 수 있다")
    void eventsPage_asAdmin_rendersList() {
        insertEvent(200L, "관리자 화면 행사", "2026-06-01", "18:00:00", "20:00:00", false);

        given()
            .basePath("")
            .cookie("JSESSIONID", loginAdminSession())
        .when()
            .get("/admin/event/events")
        .then()
            .statusCode(200)
            .contentType(containsString("text/html"))
            .body(containsString("행사 일정"))
            .body(containsString("관리자 화면 행사"))
            .body(containsString("/admin/event/events/new"));
    }

    @Test
    @DisplayName("관리자 화면에서 행사를 등록할 수 있다")
    void createEventFromAdminPage_redirectsAndCreates() {
        String sessionId = loginAdminSession();

        String location = given()
            .basePath("")
            .cookie("JSESSIONID", sessionId)
            .contentType(ContentType.URLENC)
            .formParam("title", "admin-created-event")
            .formParam("description", "created-from-admin-page")
            .formParam("eventDate", "2026-06-02")
            .formParam("startTime", "18:00")
            .formParam("endTime", "20:00")
            .redirects()
            .follow(false)
        .when()
            .post("/admin/event/events")
        .then()
            .statusCode(302)
            .header("Location", containsString("/admin/event/events/"))
            .extract()
            .header("Location");

        Long eventId = extractEventId(location);
        String title = jdbcTemplate.queryForObject("SELECT title FROM events WHERE id = ?", String.class, eventId);
        Long createdById = jdbcTemplate.queryForObject(
            "SELECT created_by_id FROM events WHERE id = ?",
            Long.class,
            eventId
        );

        assertThat(title).isEqualTo("admin-created-event");
        assertThat(createdById).isEqualTo(1L);
    }

    @Test
    @DisplayName("관리자 화면에서 행사를 수정할 수 있다")
    void updateEventFromAdminPage_redirectsAndUpdates() {
        insertEvent(210L, "수정 전 행사", "2026-06-03", "18:00:00", "20:00:00", false);

        given()
            .basePath("")
            .cookie("JSESSIONID", loginAdminSession())
            .contentType(ContentType.URLENC)
            .formParam("title", "admin-updated-event")
            .formParam("description", "")
            .formParam("eventDate", "2026-06-04")
            .formParam("startTime", "19:00")
            .formParam("endTime", "21:00")
            .redirects()
            .follow(false)
        .when()
            .post("/admin/event/events/{eventId}", 210L)
        .then()
            .statusCode(302)
            .header("Location", containsString("/admin/event/events/210/edit"));

        String title = jdbcTemplate.queryForObject("SELECT title FROM events WHERE id = 210", String.class);
        String description = jdbcTemplate.queryForObject("SELECT description FROM events WHERE id = 210", String.class);
        String eventDate = jdbcTemplate.queryForObject("SELECT event_date FROM events WHERE id = 210", String.class);

        assertThat(title).isEqualTo("admin-updated-event");
        assertThat(description).isEqualTo("");
        assertThat(eventDate).isEqualTo("2026-06-04");
    }

    @Test
    @DisplayName("관리자 화면에서 행사를 삭제할 수 있다")
    void deleteEventFromAdminPage_redirectsAndDeletes() {
        insertEvent(220L, "삭제 대상 행사", "2026-06-05", "18:00:00", "20:00:00", false);

        given()
            .basePath("")
            .cookie("JSESSIONID", loginAdminSession())
            .redirects()
            .follow(false)
        .when()
            .post("/admin/event/events/{eventId}/delete", 220L)
        .then()
            .statusCode(302)
            .header("Location", containsString("/admin/event/events"));

        Boolean isDeleted = jdbcTemplate.queryForObject("SELECT is_deleted FROM events WHERE id = 220", Boolean.class);
        assertThat(isDeleted).isTrue();
    }

    @Test
    @DisplayName("관리자 대시보드에서 행사 일정 진입점과 예정된 행사 수를 확인할 수 있다")
    void dashboard_asAdmin_rendersEventEntryAndUpcomingCount() {
        LocalDate today = LocalDate.now();
        insertEvent(230L, "예정된 행사", today.plusDays(1).toString(), "18:00:00", "20:00:00", false);
        insertEvent(231L, "지난 행사", today.minusDays(1).toString(), "18:00:00", "20:00:00", false);

        given()
            .basePath("")
            .cookie("JSESSIONID", loginAdminSession())
        .when()
            .get("/admin")
        .then()
            .statusCode(200)
            .contentType(containsString("text/html"))
            .body(containsString("예정된 행사"))
            .body(containsString("/admin/event/events?startDate=" + today))
            .body(containsString("행사 일정 관리"))
            .body(containsString("/admin/event/events"));
    }

    private String loginAdminSession() {
        return given()
            .basePath("")
            .contentType(ContentType.URLENC)
            .formParam("username", TEST_ADMIN_EMAIL)
            .formParam("password", TEST_ADMIN_PASSWORD)
            .redirects()
            .follow(false)
        .when()
            .post("/admin/auth/login")
        .then()
            .statusCode(302)
            .extract()
            .cookie("JSESSIONID");
    }

    private Long extractEventId(String location) {
        String prefix = "/admin/event/events/";
        int startIndex = location.indexOf(prefix) + prefix.length();
        int endIndex = location.indexOf("/edit", startIndex);
        return Long.valueOf(location.substring(startIndex, endIndex));
    }
}
