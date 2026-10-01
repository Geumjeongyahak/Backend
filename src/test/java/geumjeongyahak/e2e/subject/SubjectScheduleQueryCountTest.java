package geumjeongyahak.e2e.subject;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;
import java.util.Map;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@DisplayName("E2E: 과목 생성·일정 변경 1회의 조회 문 수 (N+1 회귀 방지)")
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class SubjectScheduleQueryCountTest extends SubjectBaseTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Statistics statistics() {
        return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    private long createSubject(String startAt, String endAt) {
        return given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(Map.ofEntries(
                Map.entry("classroomId", 1L),
                Map.entry("teacherId", 2L),
                Map.entry("name", "질의 수 측정"),
                Map.entry("startAt", startAt),
                Map.entry("endAt", endAt),
                Map.entry("dayOfWeek", "MONDAY"),
                Map.entry("startTime", "19:20:00"),
                Map.entry("endTime", "20:00:00"),
                Map.entry("period", 1)
            ))
        .when()
            .post()
        .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");
    }

    private static final int LESSON_DATES = 18;
    /** (분반, 날짜) 동기화 1번의 조회: 수업 · 일정 · 교사 출석 · 학생 · 학생 출석. 학생 수와 무관해야 한다. */
    private static final int READS_PER_SYNC = 5;

    /** 조회(읽기) 문 수를 돌려준다. 쓰기는 새 행 수만큼 나가는 게 정상이라 따로 센다. */
    private long countReads(String label, Runnable request) {
        Statistics statistics = statistics();
        statistics.clear();
        request.run();
        long writes = statistics.getEntityInsertCount() + statistics.getEntityUpdateCount()
            + statistics.getEntityDeleteCount();
        long statements = statistics.getPrepareStatementCount();
        System.out.printf(
            "[query-count] %s statements=%d writes=%d reads=%d%n",
            label, statements, writes, statements - writes
        );
        return statements - writes;
    }

    @Test
    @DisplayName("한 학기(월요일 18회) 과목의 기간 변경 재생성")
    void recreateLessons() {
        long subjectId = createSubject("2099-03-02", "2099-06-30");

        long reads = countReads("recreateLessons", () -> given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(Map.of("startAt", "2099-03-09", "endAt", "2099-07-06"))
        .when()
            .patch("/{subjectId}/schedule", subjectId)
        .then()
            .statusCode(200));

        // 옛 날짜 18개 삭제 동기화 + 새 날짜 18개 생성 동기화. 수정 전 263
        assertThat(reads).isLessThanOrEqualTo(2L * LESSON_DATES * READS_PER_SYNC + 40);
    }

    @Test
    @DisplayName("한 학기(월요일 18회) 과목 생성")
    void createSubjectWithTeacher() {
        long reads = countReads("createSubjectWithTeacher", () -> createSubject("2099-03-02", "2099-06-30"));

        // 수정 전 137 (학생 수만큼 출석을 하나씩 조회)
        assertThat(reads).isLessThanOrEqualTo((long) LESSON_DATES * READS_PER_SYNC + 20);
    }
}
