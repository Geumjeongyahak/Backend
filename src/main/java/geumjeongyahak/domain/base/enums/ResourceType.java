package geumjeongyahak.domain.base.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum ResourceType {
    CHANNEL("channel"),
    POST("post"),
    COMMENT("comment"),
    CLASSROOM("classroom"),
    STUDENT("student"),
    SUBJECT("subject"),
    LESSON("lesson"),
    DEPARTMENT("department"),
    USER("user"),
    FILE("file"),
    REQUEST("request");

    private final String code;

    public static ResourceType fromCode(String code) {
        return Arrays.stream(values())
                .filter(r -> r.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 리소스 타입입니다: " + code));
    }
}
