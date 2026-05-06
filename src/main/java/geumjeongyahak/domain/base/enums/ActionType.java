package geumjeongyahak.domain.base.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum ActionType {
    READ("read"),
    CREATE("create"),
    UPDATE("update"),
    WRITE("write"),
    DELETE("delete"),
    GRANT("grant"),
    REVOKE("revoke"),
    MANAGE("manage"),
    APPROVE("approve"),
    REJECT("reject");

    private final String code;

    public static ActionType fromCode(String code) {
        return Arrays.stream(values())
                .filter(a -> a.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 액션 타입입니다: " + code));
    }
}
