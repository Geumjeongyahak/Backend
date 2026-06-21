package geumjeongyahak.unit.security;

import static org.assertj.core.api.Assertions.assertThat;

import geumjeongyahak.common.security.config.SecurityProperties;
import geumjeongyahak.common.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        SecurityProperties properties = new SecurityProperties();
        properties.getJwt().setSecret("test-jwt-secret-key-must-be-at-least-32-bytes");
        properties.getJwt().setOauth2TempExpSeconds(300);
        jwtTokenProvider = new JwtTokenProvider(properties);
    }

    @Test
    void oauth2TempToken_preservesProfileImageUrlClaim() {
        String token = jwtTokenProvider.createOAuth2TempToken(
            "google-user-id",
            "user@example.com",
            "https://example.com/google-profile.png"
        );

        assertThat(jwtTokenProvider.getSubject(token)).isEqualTo("google-user-id");
        assertThat(jwtTokenProvider.getEmail(token)).isEqualTo("user@example.com");
        assertThat(jwtTokenProvider.getProfileImageUrl(token))
            .isEqualTo("https://example.com/google-profile.png");
    }

    @Test
    void oauth2TempToken_allowsMissingProfileImageUrl() {
        String token = jwtTokenProvider.createOAuth2TempToken(
            "google-user-id",
            "user@example.com",
            null
        );

        assertThat(jwtTokenProvider.getProfileImageUrl(token)).isNull();
    }
}
