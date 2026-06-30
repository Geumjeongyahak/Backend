package geumjeongyahak.domain.auth.external.dto;

public record GoogleUserInfo(
    String sub,
    String email,
    boolean emailVerified,
    String name,
    String profileImageUrl
) {}
