package geumjeongyahak.domain.base.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
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
    private static final Map<ResourceType, String> RESOURCE_LABELS = new EnumMap<>(ResourceType.class);
    private static final Map<ActionType, String> ACTION_LABELS = new EnumMap<>(ActionType.class);

    static {
        // CHANNEL: 조회, 작성, 관리
        ALLOWED_COMBINATIONS.put(ResourceType.CHANNEL, Set.of(
            ActionType.READ, ActionType.CREATE, ActionType.UPDATE, ActionType.WRITE, ActionType.DELETE, ActionType.MANAGE
        ));

        // DEPARTMENT: 조회, 수정/관리
        ALLOWED_COMBINATIONS.put(ResourceType.DEPARTMENT, Set.of(
            ActionType.READ, ActionType.WRITE, ActionType.MANAGE, ActionType.GRANT, ActionType.REVOKE
        ));

        // REQUEST: 조회, 작성, 승인, 반려
        ALLOWED_COMBINATIONS.put(ResourceType.REQUEST, Set.of(
            ActionType.READ, ActionType.CREATE, ActionType.WRITE, ActionType.MANAGE, ActionType.APPROVE, ActionType.REJECT
        ));

        // CLASSROOM, STUDENT, SUBJECT, LESSON: 조회, 작성/수정
        ALLOWED_COMBINATIONS.put(ResourceType.CLASSROOM, Set.of(ActionType.READ, ActionType.CREATE, ActionType.UPDATE, ActionType.WRITE, ActionType.DELETE, ActionType.MANAGE));
        ALLOWED_COMBINATIONS.put(ResourceType.STUDENT, Set.of(ActionType.READ, ActionType.CREATE, ActionType.UPDATE, ActionType.WRITE, ActionType.DELETE, ActionType.MANAGE));
        ALLOWED_COMBINATIONS.put(ResourceType.SUBJECT, Set.of(ActionType.READ, ActionType.CREATE, ActionType.UPDATE, ActionType.WRITE, ActionType.DELETE, ActionType.MANAGE));
        ALLOWED_COMBINATIONS.put(ResourceType.LESSON, Set.of(ActionType.READ, ActionType.CREATE, ActionType.UPDATE, ActionType.WRITE, ActionType.DELETE, ActionType.MANAGE));

        // USER: 조회, 생성, 수정, 삭제, 권한 부여/회수, 관리
        ALLOWED_COMBINATIONS.put(ResourceType.USER, Set.of(ActionType.READ, ActionType.CREATE, ActionType.UPDATE, ActionType.WRITE, ActionType.DELETE, ActionType.GRANT, ActionType.REVOKE, ActionType.MANAGE));

        // FILE: 조회, 작성
        ALLOWED_COMBINATIONS.put(ResourceType.FILE, Set.of(ActionType.READ, ActionType.CREATE, ActionType.WRITE, ActionType.DELETE, ActionType.MANAGE));

        ALLOWED_COMBINATIONS.put(ResourceType.POST, Set.of(ActionType.READ, ActionType.CREATE, ActionType.UPDATE, ActionType.WRITE, ActionType.DELETE, ActionType.MANAGE));
        ALLOWED_COMBINATIONS.put(ResourceType.COMMENT, Set.of(ActionType.READ, ActionType.CREATE, ActionType.UPDATE, ActionType.WRITE, ActionType.DELETE, ActionType.MANAGE));

        RESOURCE_LABELS.put(ResourceType.CHANNEL, "채널");
        RESOURCE_LABELS.put(ResourceType.POST, "게시글");
        RESOURCE_LABELS.put(ResourceType.COMMENT, "댓글");
        RESOURCE_LABELS.put(ResourceType.CLASSROOM, "분반");
        RESOURCE_LABELS.put(ResourceType.STUDENT, "학생");
        RESOURCE_LABELS.put(ResourceType.SUBJECT, "과목");
        RESOURCE_LABELS.put(ResourceType.LESSON, "수업");
        RESOURCE_LABELS.put(ResourceType.DEPARTMENT, "부서");
        RESOURCE_LABELS.put(ResourceType.USER, "사용자");
        RESOURCE_LABELS.put(ResourceType.FILE, "파일");
        RESOURCE_LABELS.put(ResourceType.REQUEST, "요청");

        ACTION_LABELS.put(ActionType.READ, "조회");
        ACTION_LABELS.put(ActionType.CREATE, "등록");
        ACTION_LABELS.put(ActionType.UPDATE, "수정");
        ACTION_LABELS.put(ActionType.WRITE, "작성");
        ACTION_LABELS.put(ActionType.DELETE, "삭제");
        ACTION_LABELS.put(ActionType.GRANT, "권한 부여");
        ACTION_LABELS.put(ActionType.REVOKE, "권한 회수");
        ACTION_LABELS.put(ActionType.MANAGE, "관리");
        ACTION_LABELS.put(ActionType.APPROVE, "승인");
        ACTION_LABELS.put(ActionType.REJECT, "반려");
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

    public static List<PermissionDefinition> getGlobalPermissions() {
        return ALLOWED_COMBINATIONS.entrySet().stream()
            .flatMap(entry -> entry.getValue().stream()
                .map(action -> PermissionDefinition.global(entry.getKey(), action)))
            .sorted(Comparator
                .comparing(PermissionDefinition::resourceCode)
                .thenComparing(PermissionDefinition::actionCode))
            .toList();
    }

    public static String getResourceLabel(ResourceType resource) {
        return RESOURCE_LABELS.getOrDefault(resource, resource.getCode());
    }

    public static String getActionLabel(ActionType action) {
        return ACTION_LABELS.getOrDefault(action, action.getCode());
    }

    public record PermissionDefinition(
        String permissionCode,
        String resourceCode,
        String resourceLabel,
        String actionCode,
        String actionLabel,
        String scope,
        String label,
        String description
    ) {
        private static PermissionDefinition global(ResourceType resource, ActionType action) {
            String resourceLabel = getResourceLabel(resource);
            String actionLabel = getActionLabel(action);
            String code = PermissionCode.global(resource, action).value();
            return new PermissionDefinition(
                code,
                resource.getCode(),
                resourceLabel,
                action.getCode(),
                actionLabel,
                "*",
                resourceLabel + " " + actionLabel,
                code
            );
        }
    }
}
