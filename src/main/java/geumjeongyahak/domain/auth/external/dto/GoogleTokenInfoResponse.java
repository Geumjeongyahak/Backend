package geumjeongyahak.domain.auth.external.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleTokenInfoResponse(
    String sub,
    String email,
    @JsonProperty("email_verified") String emailVerified,
    String name,
    String picture
) {
    public boolean isEmailVerified() {
        return "true".equalsIgnoreCase(emailVerified);
    }
}
