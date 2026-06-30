package geumjeongyahak.domain.auth.enums;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Getter
public enum RoleType {
    ADMIN,
    MANAGER,
    VOLUNTEER,
    GUEST;

    public GrantedAuthority getAuthority() {
        return new SimpleGrantedAuthority("ROLE_" + name());
    }
}
