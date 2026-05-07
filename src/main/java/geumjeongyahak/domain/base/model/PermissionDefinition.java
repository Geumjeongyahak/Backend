package geumjeongyahak.domain.base.model;

import geumjeongyahak.domain.base.enums.ActionType;
import geumjeongyahak.domain.base.enums.PermissionScope;
import geumjeongyahak.domain.base.enums.ResourceType;

public record PermissionDefinition(
    String permissionCode,
    String resourceCode,
    String resourceLabel,
    String actionCode,
    String actionLabel,
    String scope,
    boolean globalAllowed,
    boolean targetAllowed,
    String label,
    String description
) {
    public static PermissionDefinition global(ResourceType resource, ActionType action, PermissionScope scope) {
        return of(resource, action, scope, PermissionCode.global(resource, action).value());
    }

    public static PermissionDefinition of(ResourceType resource, ActionType action, PermissionScope scope) {
        String code = scope.allowsGlobal()
            ? PermissionCode.global(resource, action).value()
            : resource.getCode() + ":" + action.getCode();
        return of(resource, action, scope, code);
    }

    private static PermissionDefinition of(ResourceType resource, ActionType action, PermissionScope scope, String code) {
        String resourceLabel = PermissionRegistry.getResourceLabel(resource);
        String actionLabel = PermissionRegistry.getActionLabel(action);
        return new PermissionDefinition(
            code,
            resource.getCode(),
            resourceLabel,
            action.getCode(),
            actionLabel,
            scope.getCode(),
            scope.allowsGlobal(),
            scope.allowsTarget(),
            resourceLabel + " " + actionLabel,
            code
        );
    }
}
