package geumjeongyahak.common.security.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class SecurityProperties {

    private final Cors cors = new Cors();
    private final Jwt jwt = new Jwt();

    @Getter
    @Setter
    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class Jwt {

        private String secret;

        private String jweSecret;

        private long accessExpSeconds;

        private long oauth2TempExpSeconds;

        private long refreshExpSeconds;
    }
}