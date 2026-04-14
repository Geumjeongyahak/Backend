package geumjeongyahak.e2e.lesson;

import static io.restassured.RestAssured.given;
import static java.util.Map.entry;

import io.restassured.RestAssured;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import geumjeongyahak.e2e.BaseE2ETest;

@Tag("lesson")
public class LessonBaseTest extends BaseE2ETest {

    public static final String TEST_VOLUNTEER_USERNAME = "teacher01";
    protected static final long CLASSROOM_ID = 1L;
    protected static final long TEACHER_ID = 2L;
    private static final AtomicInteger SUBJECT_SEQ = new AtomicInteger(0);
    protected String adminAccessToken;
    protected String volunteerAccessToken;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        RestAssured.basePath = "/api/v1/lessons";
        this.adminAccessToken = userTestHelper.generateAccessToken(TEST_ADMIN_USERNAME);

        this.volunteerAccessToken = userTestHelper.generateAccessToken(TEST_VOLUNTEER_USERNAME);
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
        int seq = SUBJECT_SEQ.incrementAndGet();

        String[] days = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"};
        String dayOfWeek = days[seq % days.length];
        int period = (seq % 6) + 1;

        Map<String, Object> request = Map.ofEntries(
            entry("classroomId", CLASSROOM_ID),
            entry("teacherId", TEACHER_ID),
            entry("name", namePrefix + "-" + seq),
            entry("startAt", "2026-03-02"),
            entry("endAt", "2026-06-30"),
            entry("times", 12),
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
