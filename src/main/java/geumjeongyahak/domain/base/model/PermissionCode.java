package geumjeongyahak.domain.base.model;

import java.util.regex.Pattern;

import geumjeongyahak.domain.base.enums.ActionType;
import geumjeongyahak.domain.base.enums.ResourceType;

/**
 * 리소스, 액션, 대상(ID 또는 Global)을 명확하게 분리하여 관리하는 권한 코드 Value Object입니다.
 */
public record PermissionCode(
        ResourceType resource,
        ActionType action,
        Long targetId,
        boolean isGlobal
) {

    private static final Pattern PATTERN = Pattern.compile(
            "^[a-z][a-z0-9_-]*:[a-z][a-z0-9_]*:(\\*|[1-9][0-9]*)$"
    );

    public PermissionCode {
        if (resource == null || action == null) {
            throw new IllegalArgumentException("Resource와 Action은 필수입니다.");
        }
        PermissionRegistry.validate(resource, action, isGlobal);
        if (!isGlobal && targetId == null) {
            throw new IllegalArgumentException("Global 권한이 아닐 경우 targetId는 필수입니다.");
        }
    }

    /**
     * 문자열 형식(resource:action:target)을 파싱하여 객체를 생성합니다.
     */
    public PermissionCode(String value) {
        this(parseParts(value));
    }

    private PermissionCode(String[] parts) {
        this(
                ResourceType.fromCode(parts[0]),
                ActionType.fromCode(parts[1]),
                "*".equals(parts[2]) ? null : Long.parseLong(parts[2]),
                "*".equals(parts[2])
        );
    }

    /**
     * 특정 ID를 대상으로 하는 권한 코드를 생성합니다.
     */
    public static PermissionCode of(ResourceType resource, ActionType action, Long targetId) {
        return new PermissionCode(resource, action, targetId, false);
    }

    /**
     * 전역 범위를 대상으로 하는 권한 코드를 생성합니다.
     */
    public static PermissionCode global(ResourceType resource, ActionType action) {
        return new PermissionCode(resource, action, null, true);
    }

    private static String[] parseParts(String value) {
        if (value == null || !PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("유효하지 않은 permission code 형식입니다: " + value);
        }
        return value.split(":");
    }

    public String value() {
        return String.format("%s:%s:%s", 
                resource.getCode(), 
                action.getCode(), 
                isGlobal ? "*" : targetId.toString());
    }

    public String authorityCode() {
        return value();
    }

    @Override
    public String toString() {
        return value();
    }
}
