package sonmoeum.common.response;

import sonmoeum.common.error.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ApiResponse<T> {
    private final String status;
    private final String message;
    private final String code;
    private final T data;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("success", "요청이 성공적으로 처리되었습니다.", "S000", data);
    }
    
    public static ApiResponse<Void> success() {
        return new ApiResponse<>("success", "요청이 성공적으로 처리되었습니다.", "S000", null);
    }

    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return new ApiResponse<>("error", errorCode.getMessage(), errorCode.getCode(), null);
    }

    public static ApiResponse<Void> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>("error", message, errorCode.getCode(), null);
    }
}
