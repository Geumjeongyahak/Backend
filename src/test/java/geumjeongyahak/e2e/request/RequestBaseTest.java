package geumjeongyahak.e2e.request;

import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.daily_schedule.repository.DailyScheduleRepository;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.entity.UserPermission;
import geumjeongyahak.domain.users.repository.UserPermissionRepository;
import geumjeongyahak.e2e.BaseE2ETest;
import geumjeongyahak.e2e.util.TestLessonHelper;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static java.util.Map.entry;

/**
 * 요청(Request) 도메인 E2E 테스트 공통 베이스 클래스.
 *
 * <h3>init_data 고정 ID 참조</h3>
 * <ul>
 *   <li>ADMIN_ID  = 1 (admin1234 / ROLE_ADMIN)</li>
 *   <li>TEACHER_ID  = 2 (teacher01 / ROLE_VOLUNTEER)</li>
 *   <li>TEACHER2_ID = 3 (teacher02 / ROLE_VOLUNTEER)</li>
 *   <li>CLASSROOM_ID = 1 (벚꽃반)</li>
 *   <li>SUBJECT_ID = 1  (teacher01 담당 - 구입 요청 재사용)</li>
 * </ul>
 *
 * <h3>레슨 기반 요청 주의사항</h3>
 * AbsenceRequest 는 DailySchedule, LessonExchangeRequest 는 lessonId 를 통해 수업 상태를 변경하므로
 * 각 테스트 메서드는 {@link TestLessonHelper}로 독립적인 수업을 생성해야 한다.
 */
@Tag("request")
public abstract class RequestBaseTest extends BaseE2ETest {

    protected static final String GUEST_USERNAME = "guest01";
    protected static final String VOLUNTEER_USERNAME = "teacher01";   // id=2
    protected static final String VOLUNTEER2_USERNAME = "teacher02";  // id=3
    protected static final long CLASSROOM_ID = 1L;
    protected static final long TEACHER_ID = 2L;   // teacher01
    protected static final long TEACHER2_ID = 3L;  // teacher02
    protected static final long SUBJECT_ID = 1L;   // init_data subject (teacher01 담당)

    @Autowired
    protected TestLessonHelper lessonHelper;

    @Autowired
    protected UserPermissionRepository userPermissionRepository;

    @Autowired
    protected DailyScheduleRepository dailyScheduleRepository;

    protected String adminToken;
    protected String managerToken;
    protected String guestToken;
    protected String volunteerToken;   // teacher01
    protected String volunteer2Token;  // teacher02

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        adminToken = userTestHelper.generateAccessTokenByNickname(TEST_ADMIN_USERNAME);
        userTestHelper.createTestUser("manager01", RoleType.MANAGER);
        managerToken = userTestHelper.generateAccessTokenByNickname("manager01");
        guestToken = userTestHelper.generateAccessTokenByNickname(GUEST_USERNAME);
        volunteerToken = userTestHelper.generateAccessTokenByNickname(VOLUNTEER_USERNAME);
        volunteer2Token = userTestHelper.generateAccessTokenByNickname(VOLUNTEER2_USERNAME);
    }

    // ──────────────────────────────────────────────────────
    // 공통 요청 생성 헬퍼 (201 검증 후 id 반환)
    // ──────────────────────────────────────────────────────

    protected Long createAbsenceRequest(String authHeader, Long lessonId, String reason) {
        return createAbsenceRequest(authHeader, lessonId, "결석 요청", reason);
    }

    protected Long createAbsenceRequest(String authHeader, Long lessonId, String title, String reason) {
        Long dailyScheduleId = getDailyScheduleIdByLessonId(lessonId);
        return given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, authHeader)
            .contentType(ContentType.JSON)
            .body(Map.of("dailyScheduleId", dailyScheduleId, "title", title, "reason", reason))
            .post()
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");
    }

    protected Long getDailyScheduleIdByLessonId(Long lessonId) {
        LocalDate lessonDate = LocalDate.parse(lessonHelper.getLessonDate(getAuthHeader(adminToken), lessonId));
        return getDailyScheduleIdByLessonDate(lessonDate);
    }

    protected Long getDailyScheduleIdByLessonDate(LocalDate lessonDate) {
        return getDailyScheduleIdByClassroomAndLessonDate(CLASSROOM_ID, lessonDate);
    }

    protected Long getDailyScheduleIdByClassroomAndLessonDate(Long classroomId, LocalDate lessonDate) {
        return dailyScheduleRepository
            .findByClassroomIdAndLessonDateAndIsDeletedFalse(classroomId, lessonDate)
            .orElseThrow()
            .getId();
    }

    protected Long createLessonExchangeRequest(
        String authHeader,
        LocalDate lessonDate,
        String title,
        String content,
        LocalDateTime expiresAt
    ) {
        return given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, authHeader)
            .contentType(ContentType.JSON)
            .body(buildLessonExchangeRequestBody(
                lessonDate,
                title,
                content,
                expiresAt
            ))
            .post()
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");
    }

    protected Map<String, Object> buildLessonExchangeRequestBody(
        LocalDate lessonDate,
        String title,
        String content,
        LocalDateTime expiresAt
    ) {
        Long dailyScheduleId = dailyScheduleRepository
            .findByClassroomIdAndLessonDateAndIsDeletedFalse(CLASSROOM_ID, lessonDate)
            .orElseThrow()
            .getId();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("dailyScheduleId", dailyScheduleId);
        body.put("title", title);
        body.put("content", content);
        body.put("expiresAt", expiresAt.toString());
        return body;
    }

    protected Long createPurchaseRequest(String authHeader, Long classroomId,
        String title, String content, long price) {
        return given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, authHeader)
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(
                entry("title", title),
                entry("content", content),
                entry("classroomId", classroomId),
                entry("items", java.util.List.of(Map.ofEntries(
                    entry("name", title + " 품목"),
                    entry("reason", content),
                    entry("expectedPrice", price)
                ))),
                entry("receiptFileIds", java.util.List.of())
            ))
            .post()
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");
    }

    protected String createAccessTokenWithPermission(
        String nicknamePrefix,
        RoleType role,
        String permissionCode
    ) {
        String nickname = nicknamePrefix + System.nanoTime();
        User user = userTestHelper.createTestUser(nickname, role);
        userPermissionRepository.save(new UserPermission(user, permissionCode));
        return userTestHelper.generateAccessTokenByNickname(nickname);
    }

}
