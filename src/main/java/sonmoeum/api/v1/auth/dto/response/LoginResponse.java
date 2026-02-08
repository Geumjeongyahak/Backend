package sonmoeum.api.v1.auth.dto.response;

import sonmoeum.api.v1.users.dto.response.UserResponse;

public record LoginResponse(
    String accessToken,
    UserResponse user
) {}
