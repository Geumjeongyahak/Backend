package geumjeongyahak.common.config;

import java.time.Clock;
import java.time.ZoneId;
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
}
