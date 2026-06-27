package geumjeongyahak.common.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import geumjeongyahak.common.mail.MailProperties;

@Configuration
@EnableConfigurationProperties(MailProperties.class)
public class AppConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
