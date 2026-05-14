package geumjeongyahak.domain.base.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum ResourceType {
    CHANNEL("channel"),
    SUBJECT("subject"),
    STUDENT("student"),
    DEPARTMENT("department"),
    LESSON("lesson"),
    USER("user"),
    ABSENCE_REQUEST("absence-request"),
    PURCHASE_REQUEST("purchase-request"),
    LESSON_EXCHANGE_REQUEST("lesson-exchange-request");

    private final String code;

    public static ResourceType fromCode(String code) {
        return Arrays.stream(values())
                .filter(r -> r.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 리소스 타입입니다: " + code));
    }
}
