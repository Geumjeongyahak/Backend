package geumjeongyahak.e2e.subject;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import geumjeongyahak.domain.lesson.event.LessonDailyScheduleSyncRequestedEvent;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.TestPropertySource;

@DisplayName("E2E: 시간표 복사 1회의 동기화 횟수와 조회 문 수 (#242)")
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Import(SubjectCopyQueryCountTest.SyncCounterConfig.class)
class SubjectCopyQueryCountTest extends SubjectBaseTest {

    private static final int MONDAYS_IN_TARGET = 4;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    /** 요청은 서버 스레드에서 돌아 @RecordApplicationEvents로는 안 잡힌다. 앱 전역 리스너로 센다. */
    @TestConfiguration
    static class SyncCounterConfig {

        @Bean
        SyncCounter syncCounter() {
            return new SyncCounter();
        }
    }

    static class SyncCounter {

        final AtomicInteger count = new AtomicInteger();

        @EventListener
        void on(LessonDailyScheduleSyncRequestedEvent event) {
            count.incrementAndGet();
        }
    }

    @Autowired
    private SyncCounter syncCounter;

    @Test
    @DisplayName("한 분반의 1~3교시를 복사하면 DailySchedule 동기화는 교시 수가 아니라 날짜 수만큼이다")
    void copyThreePeriods_syncsOncePerClassroomDate() {
        List<Long> ids = List.of(
            createMondaySubject(1L, 2L, "1교시", "2099-03-02", "2099-03-31", "11:30:00", "12:10:00", 1),
            createMondaySubject(1L, 2L, "2교시", "2099-03-02", "2099-03-31", "12:20:00", "13:00:00", 2),
            createMondaySubject(1L, 2L, "3교시", "2099-03-02", "2099-03-31", "13:00:00", "13:30:00", 3)
        );
        syncCounter.count.set(0);
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(Map.of("subjectIds", ids, "startAt", "2099-04-01", "endAt", "2099-04-30"))
        .when()
            .post("/copy")
        .then()
            .statusCode(201);

        long syncs = syncCounter.count.get();
        long writes = statistics.getEntityInsertCount() + statistics.getEntityUpdateCount() + statistics.getEntityDeleteCount();
        System.out.printf(
            "[query-count] copy3periods syncs=%d statements=%d writes=%d reads=%d%n",
            syncs, statistics.getPrepareStatementCount(), writes, statistics.getPrepareStatementCount() - writes
        );

        assertThat(syncs).isEqualTo(MONDAYS_IN_TARGET);
    }
}
