package geumjeongyahak.common.security.service;

import geumjeongyahak.domain.base.enums.ActionType;
import geumjeongyahak.domain.base.enums.ResourceType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class DomainPermissionChecker {

    public boolean hasPermission(CustomUserDetails userDetails, ResourceType resource, ActionType action, Object resourceId) {
        if (userDetails == null) return false;
        if (userDetails.isAdmin()) return true;

        String wildcard = resource.getCode() + ":" + action.getCode() + ":*";
        String specific  = resource.getCode() + ":" + action.getCode() + ":" + resourceId;

        return userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(a -> a.equals(wildcard) || a.equals(specific));
    }
}
