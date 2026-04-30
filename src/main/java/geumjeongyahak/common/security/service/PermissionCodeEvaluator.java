package geumjeongyahak.common.security.service;

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
        String wildcard = targetType + ":" + permission + ":*";
        String specific  = targetType + ":" + permission + ":" + targetId;
        return auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(a -> a.equals(wildcard) || a.equals(specific));
    }

    @Override
    public boolean hasPermission(Authentication auth, Object targetDomainObject, Object permission) {
        return false;
    }
}
