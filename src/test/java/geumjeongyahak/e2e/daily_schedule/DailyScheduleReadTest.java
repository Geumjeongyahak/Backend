package geumjeongyahak.e2e.daily_schedule;

import static io.restassured.RestAssured.given;
import static java.util.Map.entry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import geumjeongyahak.domain.auth.v1.dto.request.LocalLoginRequest;
import geumjeongyahak.e2e.BaseE2ETest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("daily-schedule")
@DisplayName("E2E: DailySchedule 조회 테스트")
public class DailyScheduleReadTest extends BaseE2ETest {

    private static final long CLASSROOM_ID = 1L;
    private static final long TEACHER_ID = 2L;
    private static final String APPS_SCRIPT_BOT_EMAIL = "geumjeongyahak-apps-script-bot@gmail.com";
    private static final String APPS_SCRIPT_BOT_PASSWORD = "apps-script-bot123!";
    private static final LocalDate BASE_DATE = LocalDate.of(2051, 1, 1);
    private static final AtomicLong SEQUENCE = new AtomicLong();

    private String adminAccessToken;
    private String volunteerAccessToken;
    private String guestAccessToken;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        RestAssured.basePath = "/api/v1/daily-schedules";
        this.adminAccessToken = userTestHelper.generateAccessTokenByUserKey(TEST_ADMIN_USERNAME);
        this.volunteerAccessToken = userTestHelper.generateAccessTokenByUserKey("teacher01");
        this.guestAccessToken = userTestHelper.generateAccessTokenByUserKey("guest01");
    }

    @Test
    @DisplayName("날짜와 분반 ID로 DailySchedule 상세 조회 성공")
    void getDailyScheduleByClassroomAndDate_Success() {
        LocalDate lessonDate = nextLessonDate();
        String subjectName = "날짜반상세조회-" + SEQUENCE.get();
        createDailyScheduleSource(subjectName, lessonDate);

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .queryParam("classroomId", CLASSROOM_ID)
            .queryParam("lessonDate", lessonDate.toString())
            .when()
            .get("/detail")
            .then()
            .statusCode(200)
            .body("dailyScheduleId", notNullValue())
            .body("classroomId", is((int) CLASSROOM_ID))
            .body("lessonDate", is(lessonDate.toString()))
            .body("teacherId", is((int) TEACHER_ID))
            .body("isExchanged", is(false))
            .body("isAbsent", is(false))
            .body("lessons[0].subjectName", is(subjectName))
            .body("lessons[0].period", is(1))
            .body("teacherAttendance", notNullValue());
    }

    @Test
    @DisplayName("날짜와 분반 ID에 해당하는 DailySchedule이 없으면 404")
    void getDailyScheduleByClassroomAndDate_NotFound() {
        LocalDate emptyDate = nextLessonDate().plusYears(10);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .queryParam("classroomId", CLASSROOM_ID)
            .queryParam("lessonDate", emptyDate.toString())
            .when()
            .get("/detail")
            .then()
            .statusCode(404)
            .body("code", is("RES-11-001"));
    }

    @Test
    @DisplayName("게스트는 날짜와 분반 ID로 DailySchedule 상세 조회를 할 수 없다")
    void getDailyScheduleByClassroomAndDate_Forbidden_Guest() {
        LocalDate lessonDate = nextLessonDate();
        createDailyScheduleSource("게스트조회차단-" + SEQUENCE.get(), lessonDate);

        given()
            .header(AUTH_HEADER, getAuthHeader(guestAccessToken))
            .queryParam("classroomId", CLASSROOM_ID)
            .queryParam("lessonDate", lessonDate.toString())
            .when()
            .get("/detail")
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("Apps Script Bot은 날짜와 분반 ID로 DailySchedule 상세를 조회할 수 있다")
    void getDailyScheduleByClassroomAndDate_asAppsScriptBot_success() {
        LocalDate lessonDate = nextLessonDate();
        String subjectName = "Bot상세조회-" + SEQUENCE.get();
        createDailyScheduleSource(subjectName, lessonDate);
        String botAccessToken = loginAppsScriptBot();

        given()
            .header(AUTH_HEADER, getAuthHeader(botAccessToken))
            .queryParam("classroomId", CLASSROOM_ID)
            .queryParam("lessonDate", lessonDate.toString())
            .when()
            .get("/detail")
            .then()
            .statusCode(200)
            .body("dailyScheduleId", notNullValue())
            .body("classroomId", is((int) CLASSROOM_ID))
            .body("teacherId", is((int) TEACHER_ID))
            .body("lessons[0].lessonId", notNullValue())
            .body("lessons[0].subjectName", is(subjectName));
    }

    @Test
    @DisplayName("Apps Script Bot은 수업 일지를 최초 작성하고 수정할 수 있다")
    void createAndUpdateJournal_asAppsScriptBot_success() {
        LocalDate lessonDate = nextLessonDate();
        Long lessonId = createDailyScheduleSource("Bot일지작성-" + SEQUENCE.get(), lessonDate);
        String botAccessToken = loginAppsScriptBot();

        Long dailyScheduleId = given()
            .header(AUTH_HEADER, getAuthHeader(botAccessToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "lessonDate", lessonDate.toString(),
                "classroomId", CLASSROOM_ID,
                "personalInfoConsent", true,
                "residentRegistrationNumberPrefix", "900101",
                "lessonJournals", List.of(Map.of(
                    "lessonId", lessonId,
                    "note", "Apps Script Bot이 최초 작성한 수업 내용입니다."
                ))
            ))
            .post("/journal")
            .then()
            .statusCode(200)
            .body("dailyScheduleId", notNullValue())
            .body("residentRegistrationNumberPrefix", is("900101"))
            .body("personalInfoConsent", is(true))
            .body("lessons[0].lessonId", is(lessonId.intValue()))
            .body("lessons[0].note", is("Apps Script Bot이 최초 작성한 수업 내용입니다."))
            .extract()
            .jsonPath()
            .getLong("dailyScheduleId");

        given()
            .header(AUTH_HEADER, getAuthHeader(botAccessToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "personalInfoConsent", true,
                "residentRegistrationNumberPrefix", "900101",
                "lessonJournals", List.of(Map.of(
                    "lessonId", lessonId,
                    "note", "Apps Script Bot이 수정한 수업 내용입니다."
                ))
            ))
            .patch("/{dailyScheduleId}/journal", dailyScheduleId)
            .then()
            .statusCode(200)
            .body("dailyScheduleId", is(dailyScheduleId.intValue()))
            .body("lessons[0].lessonId", is(lessonId.intValue()))
            .body("lessons[0].note", is("Apps Script Bot이 수정한 수업 내용입니다."));
    }

    private LocalDate nextLessonDate() {
        return BASE_DATE.plusDays(SEQUENCE.incrementAndGet());
    }

    private Long createDailyScheduleSource(String name, LocalDate lessonDate) {
        Long subjectId = createSubject(name, lessonDate);
        return createLesson(subjectId, lessonDate);
    }

    private Long createSubject(String name, LocalDate lessonDate) {
        Map<String, Object> request = Map.ofEntries(
            entry("classroomId", CLASSROOM_ID),
            entry("name", name),
            entry("startAt", lessonDate.toString()),
            entry("endAt", lessonDate.toString()),
            entry("dayOfWeek", lessonDate.getDayOfWeek().name()),
            entry("startTime", "09:00:00"),
            entry("endTime", "10:00:00"),
            entry("period", 1),
            entry("description", "DailySchedule 조회 E2E 테스트용 과목")
        );

        return given()
            .basePath("/api/v1/subjects")
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(request)
            .post()
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");
    }

    private Long createLesson(Long subjectId, LocalDate lessonDate) {
        Map<String, Object> request = Map.ofEntries(
            entry("subjectId", subjectId),
            entry("teacherId", TEACHER_ID),
            entry("date", lessonDate.toString()),
            entry("startTime", "09:00:00"),
            entry("endTime", "10:00:00"),
            entry("period", 1)
        );

        return given()
            .basePath("/api/v1/lessons")
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(request)
            .post()
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("lessonId");
    }

    private String loginAppsScriptBot() {
        return given()
            .basePath("/api/v1/auth")
            .contentType(ContentType.JSON)
            .body(new LocalLoginRequest(APPS_SCRIPT_BOT_EMAIL, APPS_SCRIPT_BOT_PASSWORD))
            .post("/login")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getString("accessToken");
    }
}
