package geumjeongyahak.e2e.event;

import static io.restassured.RestAssured.given;
import static java.util.Map.entry;

import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.entity.UserPermission;
import geumjeongyahak.domain.users.repository.UserPermissionRepository;
import geumjeongyahak.e2e.BaseE2ETest;
import io.restassured.RestAssured;

@Tag("event")
public abstract class BaseEventTest extends BaseE2ETest {

    protected static final String EVENT_MANAGE_PERMISSION = "event:manage:*";

    protected String adminToken;
    protected String guestToken;
    protected String eventManagerToken;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected UserPermissionRepository userPermissionRepository;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        RestAssured.basePath = "/api/v1/events";
        cleanupEvents();

        adminToken = userTestHelper.generateAccessTokenByUserKey(TEST_ADMIN_USERNAME);
        guestToken = userTestHelper.generateAccessTokenByUserKey("guest01");
        eventManagerToken = createAccessTokenWithPermission("event-manager", RoleType.VOLUNTEER, EVENT_MANAGE_PERMISSION);
    }

    @AfterEach
    @Override
    protected void tearDown() {
        cleanupEvents();
        super.tearDown();
    }

    protected Map<String, Object> eventRequest(
        String title,
        String description,
        String eventDate,
        String startTime,
        String endTime
    ) {
        return Map.ofEntries(
            entry("title", title),
            entry("description", description),
            entry("eventDate", eventDate),
            entry("startTime", startTime),
            entry("endTime", endTime)
        );
    }

    protected Long createEventAndGetId(Map<String, Object> request, String token) {
        Object rawId = given()
            .basePath("/api/v1/admin/events")
            .header(AUTH_HEADER, getAuthHeader(token))
            .contentType("application/json")
            .body(request)
            .when()
            .post()
            .then()
            .statusCode(201)
            .extract()
            .path("id");

        return ((Number) rawId).longValue();
    }

    protected void insertEvent(
        Long id,
        String title,
        String eventDate,
        String startTime,
        String endTime,
        boolean isDeleted
    ) {
        jdbcTemplate.update("""
            INSERT INTO events (
                id, title, description, event_date, start_time, end_time,
                created_by_id, updated_by_id, is_deleted, created_at, updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, 1, 1, ?, ?, ?)
            """,
            id,
            title,
            title + " 설명",
            eventDate,
            startTime,
            endTime,
            isDeleted,
            LocalDateTime.parse("2026-05-01T10:00:00"),
            LocalDateTime.parse("2026-05-01T10:00:00")
        );
    }

    protected String createAccessTokenWithPermission(String userKeyPrefix, RoleType role, String permissionCode) {
        String userKey = userKeyPrefix + System.nanoTime();
        User user = userTestHelper.createTestUser(userKey, role);
        userPermissionRepository.save(new UserPermission(user, permissionCode));
        return userTestHelper.generateAccessTokenByUserKey(userKey);
    }

    protected void cleanupEvents() {
        jdbcTemplate.update("DELETE FROM events");
    }
}
