package sonmoeum.common.security.config;

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
    private final OAuth2 oauth2 = new OAuth2();

    @Getter
    @Setter
    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class Jwt {

        private String secret;

        private long accessExpSeconds = 1800;

        private long refreshExpSeconds = 1209600;

        // Refresh 토큰 쿠키 이름(추후 쿠키 전환 대비)
        private String refreshCookieName = "refresh_token";

        private String refreshCookieSameSite = "Lax";

        private boolean refreshCookieSecure = true;

        private String refreshCookiePath = "/";

        private long refreshCookieMaxAgeSeconds = 0;
    }

    @Getter
    @Setter
    public static class OAuth2 {

        // OAuth2 로그인 성공 후 프론트로 보낼 리다이렉트 URI
        private String redirectUri = "http://localhost:3000/oauth2/redirect";

        // Access 토큰 전달 방식
        private String tokenTransport = "fragment";
    }
}