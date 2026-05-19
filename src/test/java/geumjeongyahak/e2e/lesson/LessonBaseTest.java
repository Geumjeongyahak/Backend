package geumjeongyahak.e2e.lesson;

import static io.restassured.RestAssured.given;
import static java.util.Map.entry;

import io.restassured.RestAssured;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import geumjeongyahak.e2e.BaseE2ETest;
import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.lesson.repository.LessonRepository;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.entity.UserPermission;
import geumjeongyahak.domain.users.repository.UserPermissionRepository;
import geumjeongyahak.e2e.util.TestLessonHelper;

@Tag("lesson")
public class LessonBaseTest extends BaseE2ETest {

    public static final String TEST_VOLUNTEER_USERNAME = "teacher01";
    public static final String TEST_MANAGER_USERNAME = "manager01";
    protected static final long CLASSROOM_ID = 1L;
    protected static final long TEACHER_ID = 2L;
    protected static final long TEACHER2_ID = 3L;
    protected String adminAccessToken;
    protected String managerAccessToken;
    protected String volunteerAccessToken;

    @Autowired
    protected TestLessonHelper lessonHelper;

    @Autowired
    protected LessonRepository lessonRepository;

    @Autowired
    protected UserPermissionRepository userPermissionRepository;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        RestAssured.basePath = "/api/v1/lessons";
        this.adminAccessToken = userTestHelper.generateAccessTokenByUserKey(TEST_ADMIN_USERNAME);
        userTestHelper.createTestUser(TEST_MANAGER_USERNAME, RoleType.MANAGER);
        this.managerAccessToken = userTestHelper.generateAccessTokenByUserKey(TEST_MANAGER_USERNAME);
        this.volunteerAccessToken = userTestHelper.generateAccessTokenByUserKey(TEST_VOLUNTEER_USERNAME);
    }

    @AfterEach
    @Override
    protected void tearDown() {
        lessonHelper.clearAll(getAuthHeader(adminAccessToken));
        super.tearDown();
    }

    protected Map<String, Object> createLessonRequest(
        Long subjectId,
        Long teacherId,
        String date,
        String startTime,
        String endTime,
        int period
    ) {
        return Map.ofEntries(
            entry("subjectId", subjectId),
            entry("teacherId", teacherId),
            entry("date", date),
            entry("startTime", startTime),
            entry("endTime", endTime),
            entry("period", period)
        );
    }

    protected Long createSubjectAndGetId(String namePrefix) {
        long unique = System.nanoTime();
        String[] days = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"};
        int dayIndex = Math.floorMod(Long.hashCode(unique), days.length);
        String dayOfWeek = days[dayIndex];
        int period = Math.floorMod(Long.hashCode(unique / 31), 6) + 1;
        String uniqueDate = LocalDate.of(2040, 1, 1)
            .plusDays(Math.floorMod(unique, 10_000))
            .toString();

        Map<String, Object> request = Map.ofEntries(
            entry("classroomId", CLASSROOM_ID),
            entry("teacherId", TEACHER_ID),
            entry("name", namePrefix + "-" + unique),
            entry("startAt", uniqueDate),
            entry("endAt", uniqueDate),
            entry("dayOfWeek", dayOfWeek),
            entry("startTime", "20:10:00"),
            entry("endTime", "20:50:00"),
            entry("period", period),
            entry("description", "Lesson E2E용 과목")
        );

        Map<String, Object> body = given()
            .basePath("/api/v1/subjects")
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(request)
            .when()
            .post()
            .then()
            .statusCode(201)
            .extract()
            .as(Map.class);

        return extractId(body);
    }

    protected Long createTrackedSubjectAndGetId(String namePrefix) {
        return lessonHelper.createSubjectAndRegister(
            getAuthHeader(adminAccessToken),
            CLASSROOM_ID,
            TEACHER_ID,
            namePrefix
        );
    }

    protected Long createTrackedSubjectAndGetId(
        String name,
        Long teacherId,
        String date,
        String dayOfWeek,
        int period
    ) {
        return lessonHelper.createSubjectAndRegister(
            getAuthHeader(adminAccessToken),
            CLASSROOM_ID,
            teacherId,
            name,
            date,
            dayOfWeek,
            period
        );
    }

    protected Long createLessonAndGetId(Map<String, Object> request, String token) {
        Map<String, Object> body = given()
            .basePath("/api/v1/lessons")
            .header(AUTH_HEADER, getAuthHeader(token))
            .contentType("application/json")
            .body(request)
            .when()
            .post()
            .then()
            .statusCode(201)
            .extract()
            .as(Map.class);

        return extractId(body);
    }

    protected Long createTrackedLessonAndGetId(
        Long subjectId,
        Long teacherId,
        String date,
        String startTime,
        String endTime,
        int period
    ) {
        return lessonHelper.createLessonAndRegister(
            getAuthHeader(adminAccessToken),
            subjectId,
            teacherId,
            date,
            startTime,
            endTime,
            period
        );
    }

    protected Long createTrackedLessonFixture(
        String subjectName,
        Long teacherId,
        String subjectDate,
        String subjectDayOfWeek,
        int subjectPeriod,
        String lessonDate,
        String startTime,
        String endTime,
        int lessonPeriod
    ) {
        Long subjectId = createTrackedSubjectAndGetId(subjectName, teacherId, subjectDate, subjectDayOfWeek, subjectPeriod);
        return createTrackedLessonAndGetId(subjectId, teacherId, lessonDate, startTime, endTime, lessonPeriod);
    }

    protected String createAccessTokenWithPermission(
        String userKeyPrefix,
        RoleType role,
        String permissionCode
    ) {
        String userKey = userKeyPrefix + System.nanoTime();
        User user = userTestHelper.createTestUser(userKey, role);
        userPermissionRepository.save(new UserPermission(user, permissionCode));
        return userTestHelper.generateAccessTokenByUserKey(userKey);
    }

    protected void deleteLesson(Long lessonId, String token) {
        given()
            .basePath("/api/v1/lessons")
            .header(AUTH_HEADER, getAuthHeader(token))
            .when()
            .delete("/{lessonId}", lessonId)
            .then()
            .statusCode(204);
    }

    protected Long extractId(Map<String, Object> body) {
        Object raw = body.get("lessonId");
        if (raw == null) raw = body.get("id");

        if (!(raw instanceof Number n)) {
            throw new IllegalStateException("응답에서 id/lessonId를 찾을 수 없습니다. body=" + body);
        }
        return n.longValue();
    }
}
