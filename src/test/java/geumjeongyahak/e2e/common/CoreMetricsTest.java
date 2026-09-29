package geumjeongyahak.e2e.common;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import geumjeongyahak.e2e.request.RequestBaseTest;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.ValueAtPercentile;
import java.util.Arrays;
import java.util.Collection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 응답 시간 지표는 비용 때문에 경로 구분 없이 p95 값 하나만 낸다.
 * 경로(uri)마다, 버킷마다 시계열이 생기면 Cloud Monitoring 샘플 비용이 경로 수에 비례해 는다.
 */
@DisplayName("E2E: 핵심 지표 테스트")
class CoreMetricsTest extends RequestBaseTest {

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    @DisplayName("응답 시간 지표에는 경로 태그가 없다")
    void httpServerRequests_hasNoUriTag() {
        callTwoDifferentRoutes();

        assertThat(httpServerRequestTimers()).allSatisfy(timer -> assertThat(timer.getId().getTag("uri")).isNull());
    }

    @Test
    @DisplayName("응답 시간 지표는 버킷 없이 p95 값 하나만 낸다")
    void httpServerRequests_publishesP95WithoutBuckets() {
        callTwoDifferentRoutes();

        assertThat(httpServerRequestTimers()).allSatisfy(timer -> {
            var snapshot = timer.takeSnapshot();
            assertThat(snapshot.histogramCounts()).isEmpty();
            assertThat(Arrays.stream(snapshot.percentileValues()).map(ValueAtPercentile::percentile))
                .containsExactly(0.95);
        });
    }

    private void callTwoDifferentRoutes() {
        given().basePath("/api/v1/purchase-requests").header(AUTH_HEADER, getAuthHeader(volunteerToken)).get();
        given().basePath("/api/v1/classrooms").get();
    }

    private Collection<Timer> httpServerRequestTimers() {
        Collection<Timer> timers = meterRegistry.find("http.server.requests").timers();
        assertThat(timers).isNotEmpty();
        return timers;
    }
}
