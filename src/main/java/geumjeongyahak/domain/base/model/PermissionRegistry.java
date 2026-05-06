package geumjeongyahak.domain.base.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import geumjeongyahak.domain.base.enums.ActionType;
import geumjeongyahak.domain.base.enums.ResourceType;

/**
 * 리소스별로 허용되는 액션 조합을 정의하고 검증하는 레지스트리입니다.
 * 개발 과정에서 새로운 리소스나 액션이 추가될 때 이 곳에 정의합니다.
 */
public class PermissionRegistry {
    private static final Map<ResourceType, Set<ActionType>> ALLOWED_COMBINATIONS = new EnumMap<>(ResourceType.class);

    static {
        // CHANNEL: 조회, 작성, 관리
        ALLOWED_COMBINATIONS.put(ResourceType.CHANNEL, Set.of(
            ActionType.READ, ActionType.WRITE, ActionType.MANAGE
        ));

        // DEPARTMENT: 조회, 수정/관리
        ALLOWED_COMBINATIONS.put(ResourceType.DEPARTMENT, Set.of(
            ActionType.READ, ActionType.WRITE, ActionType.MANAGE
        ));

        // REQUEST: 조회, 작성, 승인, 반려
        ALLOWED_COMBINATIONS.put(ResourceType.REQUEST, Set.of(
            ActionType.READ, ActionType.WRITE, ActionType.APPROVE, ActionType.REJECT
        ));

        // CLASSROOM, STUDENT, SUBJECT, LESSON: 조회, 작성/수정
        ALLOWED_COMBINATIONS.put(ResourceType.CLASSROOM, Set.of(ActionType.READ, ActionType.WRITE));
        ALLOWED_COMBINATIONS.put(ResourceType.STUDENT, Set.of(ActionType.READ, ActionType.WRITE));
        ALLOWED_COMBINATIONS.put(ResourceType.SUBJECT, Set.of(ActionType.READ, ActionType.WRITE));
        ALLOWED_COMBINATIONS.put(ResourceType.LESSON, Set.of(ActionType.READ, ActionType.WRITE));

        // USER: 조회, 생성, 수정, 삭제, 권한 부여/회수, 관리
        ALLOWED_COMBINATIONS.put(ResourceType.USER, Set.of(ActionType.READ, ActionType.WRITE, ActionType.MANAGE));

        // FILE: 조회, 작성
        ALLOWED_COMBINATIONS.put(ResourceType.FILE, Set.of(ActionType.READ, ActionType.WRITE));
    }

    public static boolean isAllowed(ResourceType resource, ActionType action) {
        return ALLOWED_COMBINATIONS.getOrDefault(resource, Collections.emptySet()).contains(action);
    }

    public static void validate(ResourceType resource, ActionType action) {
        if (!isAllowed(resource, action)) {
            throw new IllegalArgumentException(
                String.format("허용되지 않는 권한 조합입니다: %s:%s", resource.getCode(), action.getCode())
            );
        }
    }

    public static Set<ActionType> getAllowedActions(ResourceType resource) {
        return ALLOWED_COMBINATIONS.getOrDefault(resource, Collections.emptySet());
    }
}
