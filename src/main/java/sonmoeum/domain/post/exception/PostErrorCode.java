package sonmoeum.domain.post.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import sonmoeum.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum PostErrorCode implements ErrorCode {
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "RES-09-001", "게시글을 찾을 수 없습니다."),
    DUPLICATE_POST(HttpStatus.CONFLICT, "BIZ-09-001", "이미 존재하는 게시글입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
