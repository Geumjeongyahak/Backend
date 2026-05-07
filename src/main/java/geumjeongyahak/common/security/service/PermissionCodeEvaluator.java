package geumjeongyahak.common.security.service;

import geumjeongyahak.domain.base.enums.ActionType;
import geumjeongyahak.domain.base.enums.ResourceType;
import geumjeongyahak.domain.base.model.PermissionRegistry;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
public class PermissionCodeEvaluator implements PermissionEvaluator {

    /**
     * @PreAuthorize("hasPermission(#channelId, 'channel', 'write')")
     * authorities에서 'channel:write:*' 또는 'channel:write:{channelId}' 를 검사한다.
     */
    @Override
    public boolean hasPermission(Authentication auth, Serializable targetId, String targetType, Object permission) {
        if (auth == null || targetType == null || permission == null) return false;
        ResourceType resource;
        ActionType action;

        try {
            resource = ResourceType.fromCode(targetType);
            action = ActionType.fromCode(permission.toString());
        } catch (IllegalArgumentException e) {
            return false;
        }

        return auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(a -> matchesPermission(a, resource, action, targetId));
    }

    private boolean matchesPermission(String authority, ResourceType resource, ActionType action, Serializable targetId) {
        if (PermissionRegistry.isScopeAllowed(resource, action, true) &&
            authority.equals(resource.getCode() + ":" + action.getCode() + ":*")) {
            return true;
        }

        return targetId != null &&
            PermissionRegistry.isScopeAllowed(resource, action, false) &&
            authority.equals(resource.getCode() + ":" + action.getCode() + ":" + targetId);
    }

    @Override
    public boolean hasPermission(Authentication auth, Object targetDomainObject, Object permission) {
        return false;
    }
}
