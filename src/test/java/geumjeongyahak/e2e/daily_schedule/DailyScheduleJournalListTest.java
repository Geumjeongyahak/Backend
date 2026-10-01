package geumjeongyahak.e2e.daily_schedule;

import static io.restassured.RestAssured.given;
import static java.util.Map.entry;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import geumjeongyahak.domain.auth.v1.dto.request.LocalLoginRequest;
import geumjeongyahak.e2e.BaseE2ETest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("daily-schedule")
@DisplayName("E2E: 수업일지 목록 필터·페이징")
public class DailyScheduleJournalListTest extends BaseE2ETest {

    private static final long CLASSROOM_ID = 1L;
    private static final long TEACHER_ID = 2L;
    private static final String APPS_SCRIPT_BOT_EMAIL = "geumjeongyahak-apps-script-bot@gmail.com";
    private static final String APPS_SCRIPT_BOT_PASSWORD = "apps-script-bot123!";
    private static final LocalDate BASE_DATE = LocalDate.of(2081, 1, 1);
    private static final AtomicLong SEQUENCE = new AtomicLong();

    private String adminAccessToken;
    private String volunteerAccessToken;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        RestAssured.basePath = "/api/v1/daily-schedules";
        this.adminAccessToken = userTestHelper.generateAccessTokenByUserKey(TEST_ADMIN_USERNAME);
        this.volunteerAccessToken = userTestHelper.generateAccessTokenByUserKey("teacher01");
    }

    @Test
    @DisplayName("일지가 있는 일정만 날짜·id 내림차순으로 페이징하고 전체 건수를 센다")
    void list_pagesOnlyJournaledSchedules() {
        String tag = newTag();
        List<Long> scheduleIds = createJournaledSchedules(tag, 5);
        createDailyScheduleSource("과목-" + tag + "-일지없음", nextLessonDate());

        JsonPath firstPage = list(adminAccessToken, Map.of("keyword", tag, "size", 2, "page", 0));
        List<Long> newestFirst = new ArrayList<>(scheduleIds).reversed();
        assertThat(firstPage.getLong("totalElements"), is(5L));
        assertThat(firstPage.getList("content.dailyScheduleId", Long.class),
            contains(newestFirst.get(0), newestFirst.get(1)));

        JsonPath secondPage = list(adminAccessToken, Map.of("keyword", tag, "size", 2, "page", 1));
        assertThat(secondPage.getList("content.dailyScheduleId", Long.class),
            contains(newestFirst.get(2), newestFirst.get(3)));

        JsonPath lastPage = list(adminAccessToken, Map.of("keyword", tag, "size", 2, "page", 2));
        assertThat(lastPage.getList("content.dailyScheduleId", Long.class),
            contains(newestFirst.get(4)));

        JsonPath beyond = list(adminAccessToken, Map.of("keyword", tag, "size", 2, "page", 5));
        assertThat(beyond.getList("content"), hasSize(0));
        assertThat(beyond.getLong("totalElements"), is(5L));
    }

    @Test
    @DisplayName("mine=true면 요청자가 담당인 일정만 나온다")
    void list_mineFiltersByRequester() {
        String tag = newTag();
        createJournaledSchedules(tag, 2);

        assertThat(
            list(volunteerAccessToken, Map.of("keyword", tag, "mine", true)).getLong("totalElements"), is(2L));
        assertThat(
            list(adminAccessToken, Map.of("keyword", tag, "mine", true)).getLong("totalElements"), is(0L));
        assertThat(
            list(adminAccessToken, Map.of("keyword", tag, "mine", false)).getLong("totalElements"), is(2L));
    }

    @Test
    @DisplayName("키워드는 과목명·일지 내용·분반명·교사명에 대소문자 없이 걸린다")
    void list_keywordMatchesEveryField() {
        String tag = newTag();
        Long scheduleId = createJournaledSchedules(tag, 1).getFirst();

        assertThat(
            list(adminAccessToken, Map.of("keyword", "과목-" + tag.toUpperCase())).getLong("totalElements"), is(1L));
        assertThat(
            list(adminAccessToken, Map.of("keyword", "  내용-" + tag + "  ")).getLong("totalElements"), is(1L));

        JsonPath byTag = list(adminAccessToken, Map.of("keyword", tag));
        String classroomName = byTag.getString("content[0].classroomName");
        String teacherName = byTag.getString("content[0].teacherName");

        assertThat(
            list(adminAccessToken, Map.of("keyword", classroomName, "size", 100))
                .getList("content.dailyScheduleId", Long.class),
            hasItem(scheduleId));
        assertThat(
            list(adminAccessToken, Map.of("keyword", teacherName, "size", 100))
                .getList("content.dailyScheduleId", Long.class),
            hasItem(scheduleId));
    }

    @Test
    @DisplayName("키워드의 %와 _는 글자 그대로 찾는다")
    void list_keywordWildcardsAreLiteral() {
        String tag = newTag();
        createJournaledSchedules(tag, 1);

        assertThat(
            list(adminAccessToken, Map.of("keyword", tag + "%")).getLong("totalElements"), is(0L));
        assertThat(
            list(adminAccessToken, Map.of("keyword", "_" + tag.substring(1))).getLong("totalElements"), is(0L));
    }

    private JsonPath list(String accessToken, Map<String, ?> params) {
        return given()
            .header(AUTH_HEADER, getAuthHeader(accessToken))
            .queryParams(params)
            .get()
            .then()
            .statusCode(200)
            .extract()
            .jsonPath();
    }

    private List<Long> createJournaledSchedules(String tag, int count) {
        String botAccessToken = loginAppsScriptBot();
        List<Long> scheduleIds = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            LocalDate lessonDate = nextLessonDate();
            Long lessonId = createDailyScheduleSource("과목-" + tag + "-" + i, lessonDate);
            scheduleIds.add(writeJournal(botAccessToken, lessonDate, lessonId, "내용-" + tag + " " + i));
        }
        return scheduleIds;
    }

    private Long writeJournal(String botAccessToken, LocalDate lessonDate, Long lessonId, String note) {
        return given()
            .header(AUTH_HEADER, getAuthHeader(botAccessToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "lessonDate", lessonDate.toString(),
                "classroomId", CLASSROOM_ID,
                "personalInfoConsent", true,
                "residentRegistrationNumberPrefix", "900101",
                "lessonJournals", List.of(Map.of("lessonId", lessonId, "note", note))
            ))
            .post("/journal")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getLong("dailyScheduleId");
    }

    private String newTag() {
        return "t" + UUID.randomUUID().toString().substring(0, 8);
    }

    private LocalDate nextLessonDate() {
        return BASE_DATE.plusDays(SEQUENCE.incrementAndGet());
    }

    private Long createDailyScheduleSource(String name, LocalDate lessonDate) {
        Long subjectId = given()
            .basePath("/api/v1/subjects")
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(
                entry("classroomId", CLASSROOM_ID),
                entry("name", name),
                entry("startAt", lessonDate.toString()),
                entry("endAt", lessonDate.toString()),
                entry("dayOfWeek", lessonDate.getDayOfWeek().name()),
                entry("startTime", "09:00:00"),
                entry("endTime", "10:00:00"),
                entry("period", 1),
                entry("description", "수업일지 목록 E2E 테스트용 과목")
            ))
            .post()
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");

        return given()
            .basePath("/api/v1/lessons")
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(
                entry("subjectId", subjectId),
                entry("teacherId", TEACHER_ID),
                entry("date", lessonDate.toString()),
                entry("startTime", "09:00:00"),
                entry("endTime", "10:00:00"),
                entry("period", 1)
            ))
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
