package geumjeongyahak.e2e.util;

import static io.restassured.RestAssured.given;
import static java.util.Map.entry;

import io.restassured.http.ContentType;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * Request 도메인 E2E 테스트에서 보조 도메인(과목/수업)을 생성·조회·삭제하는 헬퍼.
 * <p>
 * - Subject: SUBJECT_SEQ(100번~)로 name·dayOfWeek·period를 자동 조합하고, 날짜 범위를
 *   2050-01-01 기준으로 seq마다 1일씩 증가시켜 classroom+dayOfWeek+period 중복 충돌 방지.
 * - Lesson: DATE_OFFSET으로 날짜를 자동 증가 → 동일 teacher+date 충돌 방지
 * </p>
 */
@Component
public class TestLessonHelper {

    private static final LocalDate SUBJECT_BASE_DATE = LocalDate.of(2050, 1, 1);
    private static final LocalDate LESSON_BASE_DATE = LocalDate.of(2026, 8, 1);
    private static final AtomicLong SUBJECT_SEQUENCE = new AtomicLong();
    private final List<Long> createdSubjectIds = new ArrayList<>();
    private final List<Long> createdLessonIds = new ArrayList<>();

    public String uniqueName(String prefix) {
        return prefix + "-" + System.nanoTime();
    }

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
        long sequence = SUBJECT_SEQUENCE.incrementAndGet();
        LocalDate uniqueDate = SUBJECT_BASE_DATE.plusDays(sequence);

        Map<String, Object> req = Map.ofEntries(
            entry("classroomId", classroomId),
            entry("teacherId", teacherId),
            entry("name", "Request테스트과목-" + sequence),
            entry("startAt", uniqueDate.toString()),
            entry("endAt", uniqueDate.toString()),
            entry("dayOfWeek", uniqueDate.getDayOfWeek().name()),
            entry("startTime", "09:00:00"),
            entry("endTime", "10:00:00"),
            entry("period", 1),
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

    public Long createSubjectAndRegister(
        String authHeader,
        long classroomId,
        long teacherId,
        String namePrefix
    ) {
        long sequence = SUBJECT_SEQUENCE.incrementAndGet();
        LocalDate uniqueDate = SUBJECT_BASE_DATE.plusDays(sequence);

        return createSubjectAndRegister(
            authHeader,
            classroomId,
            teacherId,
            namePrefix + "-" + sequence,
            uniqueDate.toString(),
            uniqueDate.getDayOfWeek().name(),
            1
        );
    }

    public Long createSubjectAndRegister(
        String authHeader,
        long classroomId,
        long teacherId,
        String name,
        String date,
        String dayOfWeek,
        int period
    ) {
        Map<String, Object> req = Map.ofEntries(
            entry("classroomId", classroomId),
            entry("teacherId", teacherId),
            entry("name", name),
            entry("startAt", date),
            entry("endAt", date),
            entry("dayOfWeek", dayOfWeek),
            entry("startTime", "09:00:00"),
            entry("endTime", "10:00:00"),
            entry("period", period),
            entry("description", "Request E2E 테스트용 임시 과목")
        );

        Long subjectId = given()
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

        createdSubjectIds.add(subjectId);
        return subjectId;
    }

    // ──────────────────────────────────────────────────────
    // Lesson
    // ──────────────────────────────────────────────────────

    /**
     * 테스트용 수업을 생성하고 lessonId를 반환한다.
     * 날짜는 BASE_DATE(2026-08-01)에서 자동 증가하여 teacher+date 충돌을 방지한다.
     */
    public Long createLessonAndGetId(String authHeader, Long subjectId, Long teacherId) {
        return createLessonAndGetId(
            authHeader,
            subjectId,
            teacherId,
            LESSON_BASE_DATE.plusDays(Math.floorMod(System.nanoTime(), 10_000)).toString(),
            "09:00:00",
            "10:00:00",
            1
        );
    }

    public Long createLessonAndGetId(
        String authHeader,
        Long subjectId,
        Long teacherId,
        String date,
        String startTime,
        String endTime,
        int period
    ) {

        Map<String, Object> req = Map.ofEntries(
            entry("subjectId", subjectId),
            entry("teacherId", teacherId),
            entry("date", date),
            entry("startTime", startTime),
            entry("endTime", endTime),
            entry("period", period)
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

    public Long createLessonAndRegister(String authHeader, Long subjectId, Long teacherId) {
        Long lessonId = createLessonAndGetId(authHeader, subjectId, teacherId);
        createdLessonIds.add(lessonId);
        return lessonId;
    }

    public Long createLessonAndRegister(
        String authHeader,
        Long subjectId,
        Long teacherId,
        String date,
        String startTime,
        String endTime,
        int period
    ) {
        Long lessonId = createLessonAndGetId(authHeader, subjectId, teacherId, date, startTime, endTime, period);
        createdLessonIds.add(lessonId);
        return lessonId;
    }

    /** 과목을 비활성화(소프트 삭제)한다 (cleanup용). */
    public void deleteSubject(String authHeader, Long subjectId) {
        given()
            .basePath("/api/v1/subjects")
            .header("Authorization", authHeader)
            .delete("/{id}", subjectId)
            .then()
            .statusCode(204);
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

    public void clearAll(String authHeader) {
        for (Long lessonId : createdLessonIds.reversed()) {
            deleteLesson(authHeader, lessonId);
        }
        createdLessonIds.clear();

        for (Long subjectId : createdSubjectIds.reversed()) {
            deleteSubject(authHeader, subjectId);
        }
        createdSubjectIds.clear();
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

    /** 수업 날짜(date) 문자열을 반환한다. */
    public String getLessonDate(String authHeader, Long lessonId) {
        return given()
            .basePath("/api/v1/lessons")
            .header("Authorization", authHeader)
            .get("/{id}", lessonId)
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getString("date");
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

    /** 과목의 담당 교사 ID를 반환한다. */
    public Long getSubjectTeacherId(String authHeader, Long subjectId) {
        return given()
            .basePath("/api/v1/subjects")
            .header("Authorization", authHeader)
            .get("/{id}", subjectId)
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getLong("teacherId");
    }
}
