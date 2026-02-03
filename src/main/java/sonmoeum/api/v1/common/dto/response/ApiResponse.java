package sonmoeum.api.v1.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ApiResponse<T>(
    @Schema(description = "성공 여부", example = "true")
    boolean success,

    @Schema(description = "응답 데이터")
    T data,

    @Schema(description = "에러 정보")
    Object error
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> error(Object error) {
        return new ApiResponse<>(false, null, error);
    }
}
