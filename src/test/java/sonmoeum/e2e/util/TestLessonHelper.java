package sonmoeum.e2e.util;

import static io.restassured.RestAssured.given;
import static java.util.Map.entry;

import io.restassured.http.ContentType;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/**
 * Request 도메인 E2E 테스트에서 보조 도메인(과목/수업)을 생성·조회·삭제하는 헬퍼.
 * <p>
 * - Subject: SUBJECT_SEQ(100번~)로 name·dayOfWeek·period를 자동 조합 → 충돌 방지
 * - Lesson: DATE_OFFSET으로 날짜를 자동 증가 → 동일 teacher+date 충돌 방지
 * </p>
 */
@Component
public class TestLessonHelper {

    private static final AtomicInteger SUBJECT_SEQ = new AtomicInteger(100);
    private static final AtomicInteger DATE_OFFSET = new AtomicInteger(0);
    private static final LocalDate LESSON_BASE_DATE = LocalDate.of(2026, 8, 1);
    private static final String[] DAYS = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"};

    // ──────────────────────────────────────────────────────
    // Subject
    // ──────────────────────────────────────────────────────

    /**
     * 테스트용 과목을 생성하고 subjectId를 반환한다.
     *
     * @param authHeader "Bearer {token}" 형식의 인증 헤더 값
     * @param classroomId 분반 ID
     * @param teacherId 담당 교사 ID
     */
    public Long createSubjectAndGetId(String authHeader, long classroomId, long teacherId) {
        int seq = SUBJECT_SEQ.getAndIncrement();
        String dayOfWeek = DAYS[seq % DAYS.length];
        int period = (seq % 6) + 1;

        Map<String, Object> req = Map.ofEntries(
            entry("classroomId", classroomId),
            entry("teacherId", teacherId),
            entry("name", "Request테스트과목-" + seq),
            entry("startAt", "2026-03-02"),
            entry("endAt", "2026-12-31"),
            entry("times", 12),
            entry("dayOfWeek", dayOfWeek),
            entry("startTime", "09:00:00"),
            entry("endTime", "10:00:00"),
            entry("period", period),
            entry("description", "Request E2E 테스트용 임시 과목")
        );

        return given()
            .basePath("/api/v1/subjects")
            .header("Authorization", authHeader)
            .contentType(ContentType.JSON)
            .body(req)
            .post()
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");
    }

    // ──────────────────────────────────────────────────────
    // Lesson
    // ──────────────────────────────────────────────────────

    /**
     * 테스트용 수업을 생성하고 lessonId를 반환한다.
     * 날짜는 BASE_DATE(2026-08-01)에서 자동 증가하여 teacher+date 충돌을 방지한다.
     */
    public Long createLessonAndGetId(String authHeader, Long subjectId, Long teacherId) {
        String date = LESSON_BASE_DATE.plusDays(DATE_OFFSET.getAndIncrement()).toString();

        Map<String, Object> req = Map.ofEntries(
            entry("subjectId", subjectId),
            entry("teacherId", teacherId),
            entry("date", date),
            entry("startTime", "09:00:00"),
            entry("endTime", "10:00:00"),
            entry("period", 1)
        );

        return given()
            .basePath("/api/v1/lessons")
            .header("Authorization", authHeader)
            .contentType(ContentType.JSON)
            .body(req)
            .post()
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("lessonId");
    }

    /** 수업을 삭제한다 (cleanup용). */
    public void deleteLesson(String authHeader, Long lessonId) {
        given()
            .basePath("/api/v1/lessons")
            .header("Authorization", authHeader)
            .delete("/{id}", lessonId)
            .then()
            .statusCode(204);
    }

    // ──────────────────────────────────────────────────────
    // Side-effect 검증용 조회
    // ──────────────────────────────────────────────────────

    /** 수업의 교사 출석 상태(teacherAttendance) 문자열을 반환한다. */
    public String getLessonTeacherAttendance(String authHeader, Long lessonId) {
        return given()
            .basePath("/api/v1/lessons")
            .header("Authorization", authHeader)
            .get("/{id}", lessonId)
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getString("teacherAttendance");
    }

    /** 수업의 담당 교사 이름(teacherName)을 반환한다. */
    public String getLessonTeacherName(String authHeader, Long lessonId) {
        return given()
            .basePath("/api/v1/lessons")
            .header("Authorization", authHeader)
            .get("/{id}", lessonId)
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getString("teacherName");
    }
}
