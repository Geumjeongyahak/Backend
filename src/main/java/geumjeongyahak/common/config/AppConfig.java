package geumjeongyahak.common.config;

import java.time.Clock;
import java.time.ZoneId;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import geumjeongyahak.common.mail.MailProperties;

@Configuration
@EnableConfigurationProperties(MailProperties.class)
public class AppConfig {

    // 서비스가 쓰는 시간대. JVM 기본 시간대(GeumjeongyahakApiApplication)와 Clock 빈이 이 값을 따른다.
    public static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");

    @Bean
    public Clock clock() {
        return Clock.system(ZONE_ID);
    }

    // 응답 시간 지표를 경로·예외마다 나누지 않는다. 엔드포인트 수만큼 시계열이 늘어 지표 비용이 커진다.
    // 어느 경로가 느린지는 로그의 request 로 찾는다.
    @Bean
    public MeterFilter ignoreRouteTags() {
        return MeterFilter.ignoreTags("uri", "exception");
    }
}
