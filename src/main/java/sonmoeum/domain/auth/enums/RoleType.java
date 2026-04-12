package sonmoeum.domain.auth.enums;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
public enum RoleType {
    ROLE_ADMIN(1L), ROLE_MANAGER(2L), ROLE_VOLUNTEER(3L), ROLE_GUEST(4L),

    DEPT_ACADEMIC_PLANNING(1001L), DEPT_EDUCATION_RESEARCH(1002L), DEPT_STUDENT_SAFETY(1003L),
    DEPT_GENERAL_AFFAIRS(1004L), DEPT_PUBLIC_RELATIONS(1005L), DEPT_EDITORIAL(1006L),

    TEACHER(2001L);

    private final static Map<Long, RoleType> ID_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(RoleType::getId, Function.identity()));

    private final long id;
    private final RoleLevel level;

    RoleType(long id) {
        this.id = id;
        this.level = RoleLevel.getByCode(id);
    }

    public GrantedAuthority getAuthority() {
        return new SimpleGrantedAuthority(name());
    }
    public static RoleType findById(long id) {
        return ID_MAP.get(id);
    }


}
