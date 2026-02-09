package sonmoeum.domain.auth.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
public enum RoleType {
    ADMIN(1L), MANAGER(2L), VOLUNTEER(3L), GUEST(4L),
    DEPT_FINANCE(1001L), DEPT_ACADEMIC(1002L), DEPT_IT(1003L), DEPT_SUPPORT(1004L),
    TEACHER(2001L);

    private final static Map<Long, RoleType> ID_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(RoleType::getId, Function.identity()));

    private final long id;
    private final long level;

    RoleType(long id) {
        this.id = id;
        this.level = id % 1000;
    }

    public String getAuthority() {
        if (this.level == 0) return "ROLE_" + this.name();
        return this.name();
    }
    public static RoleType findById(long id) {
        return ID_MAP.get(id);
    }


}
