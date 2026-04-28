package geumjeongyahak.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "app.oauth2.google")
@Getter
@Setter
public class GoogleOAuth2Properties {
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String frontendRedirectUri;
}
