package geumjeongyahak.common.security.service;

import geumjeongyahak.domain.base.enums.ActionType;
import geumjeongyahak.domain.base.enums.ResourceType;
import geumjeongyahak.domain.base.model.PermissionRegistry;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class DomainPermissionChecker {

    public boolean hasPermission(CustomUserDetails userDetails, ResourceType resource, ActionType action, Object resourceId) {
        if (userDetails == null) return false;
        if (userDetails.isAdmin()) return true;

        return userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(a -> matchesPermission(a, resource, action, resourceId));
    }

    private boolean matchesPermission(String authority, ResourceType resource, ActionType action, Object resourceId) {
        if (PermissionRegistry.isScopeAllowed(resource, action, true) &&
            authority.equals(resource.getCode() + ":" + action.getCode() + ":*")) {
            return true;
        }

        return resourceId != null &&
            PermissionRegistry.isScopeAllowed(resource, action, false) &&
            authority.equals(resource.getCode() + ":" + action.getCode() + ":" + resourceId);
    }
}
