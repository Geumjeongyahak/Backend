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
    static PermissionDefinition global(ResourceType resource, ActionType action, PermissionScope scope) {
        String resourceLabel = PermissionRegistry.getResourceLabel(resource);
        String actionLabel = PermissionRegistry.getActionLabel(action);
        String code = PermissionCode.global(resource, action).value();
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
