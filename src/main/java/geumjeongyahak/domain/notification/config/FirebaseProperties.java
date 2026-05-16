package geumjeongyahak.domain.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.firebase")
public record FirebaseProperties(
    boolean enabled,
    String projectId,
    String credentialsBase64
) {
}
