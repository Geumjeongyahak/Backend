package geumjeongyahak.domain.base.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import geumjeongyahak.domain.base.enums.ActionType;
import geumjeongyahak.domain.base.enums.PermissionScope;
import geumjeongyahak.domain.base.enums.ResourceType;

/**
 * 리소스별로 허용되는 액션 조합을 정의하고 검증하는 레지스트리입니다.
 * 개발 과정에서 새로운 리소스나 액션이 추가될 때 이 곳에 정의합니다.
 */
public class PermissionRegistry {
    private static final Map<ResourceType, Map<ActionType, PermissionScope>> ALLOWED_COMBINATIONS = new EnumMap<>(ResourceType.class);
    private static final Map<ResourceType, String> RESOURCE_LABELS = new EnumMap<>(ResourceType.class);
    private static final Map<ActionType, String> ACTION_LABELS = new EnumMap<>(ActionType.class);

    static {
        allow(ResourceType.USER, PermissionScope.GLOBAL_ONLY,
            ActionType.READ, ActionType.WRITE, ActionType.MANAGE, ActionType.GRANT);
        allow(ResourceType.DEPARTMENT, PermissionScope.GLOBAL_ONLY,
            ActionType.WRITE, ActionType.MANAGE, ActionType.GRANT);
        allow(ResourceType.STUDENT, PermissionScope.GLOBAL_ONLY,
            ActionType.WRITE, ActionType.MANAGE);
        allow(ResourceType.SUBJECT, PermissionScope.GLOBAL_ONLY,
            ActionType.READ, ActionType.WRITE, ActionType.MANAGE);
        allow(ResourceType.LESSON, PermissionScope.GLOBAL_ONLY,
            ActionType.READ, ActionType.WRITE, ActionType.MANAGE);
        allow(ResourceType.DAILY_SCHEDULE, PermissionScope.GLOBAL_ONLY,
            ActionType.READ, ActionType.MANAGE);
        allow(ResourceType.CHANNEL, PermissionScope.BOTH,
            ActionType.READ, ActionType.WRITE, ActionType.MANAGE);
        allow(ResourceType.ABSENCE_REQUEST, PermissionScope.GLOBAL_ONLY,
            ActionType.READ, ActionType.MANAGE);
        allow(ResourceType.PURCHASE_REQUEST, PermissionScope.GLOBAL_ONLY,
            ActionType.READ, ActionType.MANAGE, ActionType.REVIEW);
        allow(ResourceType.LESSON_EXCHANGE_REQUEST, PermissionScope.GLOBAL_ONLY,
            ActionType.MANAGE);

        RESOURCE_LABELS.put(ResourceType.CHANNEL, "채널");
        RESOURCE_LABELS.put(ResourceType.SUBJECT, "과목");
        RESOURCE_LABELS.put(ResourceType.STUDENT, "학생");
        RESOURCE_LABELS.put(ResourceType.DEPARTMENT, "부서");
        RESOURCE_LABELS.put(ResourceType.LESSON, "수업");
        RESOURCE_LABELS.put(ResourceType.DAILY_SCHEDULE, "하루 일정");
        RESOURCE_LABELS.put(ResourceType.USER, "사용자");
        RESOURCE_LABELS.put(ResourceType.ABSENCE_REQUEST, "결석 요청");
        RESOURCE_LABELS.put(ResourceType.PURCHASE_REQUEST, "구입 요청");
        RESOURCE_LABELS.put(ResourceType.LESSON_EXCHANGE_REQUEST, "수업 교환 요청");

        ACTION_LABELS.put(ActionType.READ, "조회");
        ACTION_LABELS.put(ActionType.WRITE, "작성");
        ACTION_LABELS.put(ActionType.GRANT, "권한 부여/회수");
        ACTION_LABELS.put(ActionType.MANAGE, "관리");
        ACTION_LABELS.put(ActionType.REVIEW, "승인/반려");
    }

    public static boolean isAllowed(ResourceType resource, ActionType action) {
        return ALLOWED_COMBINATIONS.getOrDefault(resource, Collections.emptyMap()).containsKey(action);
    }

    public static boolean isScopeAllowed(ResourceType resource, ActionType action, boolean isGlobal) {
        PermissionScope scope = ALLOWED_COMBINATIONS.getOrDefault(resource, Collections.emptyMap()).get(action);
        return scope != null && scope.allows(isGlobal);
    }

    public static void validate(ResourceType resource, ActionType action) {
        if (!isAllowed(resource, action)) {
            throw new IllegalArgumentException(
                String.format("허용되지 않는 권한 조합입니다: %s:%s", resource.getCode(), action.getCode())
            );
        }
    }

    public static void validate(ResourceType resource, ActionType action, boolean isGlobal) {
        validate(resource, action);
        if (!isScopeAllowed(resource, action, isGlobal)) {
            throw new IllegalArgumentException(
                String.format("허용되지 않는 권한 범위입니다: %s:%s:%s",
                    resource.getCode(), action.getCode(), isGlobal ? "*" : "{id}")
            );
        }
    }

    public static Set<ActionType> getAllowedActions(ResourceType resource) {
        return ALLOWED_COMBINATIONS.getOrDefault(resource, Collections.emptyMap()).keySet();
    }

    public static List<PermissionDefinition> getGlobalPermissions() {
        return ALLOWED_COMBINATIONS.entrySet().stream()
            .flatMap(entry -> entry.getValue().entrySet().stream()
                .filter(rule -> rule.getValue().allowsGlobal())
                .map(rule -> PermissionDefinition.global(entry.getKey(), rule.getKey(), rule.getValue())))
            .sorted(Comparator
                .comparing(PermissionDefinition::resourceCode)
                .thenComparing(PermissionDefinition::actionCode))
            .toList();
    }

    public static List<PermissionDefinition> getAssignablePermissions() {
        return ALLOWED_COMBINATIONS.entrySet().stream()
            .flatMap(entry -> entry.getValue().entrySet().stream()
                .map(rule -> PermissionDefinition.of(entry.getKey(), rule.getKey(), rule.getValue())))
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

    private static void allow(ResourceType resource, PermissionScope scope, ActionType... actions) {
        Map<ActionType, PermissionScope> rules = ALLOWED_COMBINATIONS.computeIfAbsent(resource, ignored -> new EnumMap<>(ActionType.class));
        Arrays.stream(actions).forEach(action -> rules.put(action, scope));
    }

}
