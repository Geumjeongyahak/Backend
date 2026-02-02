package sonmoeum.domain.auth.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum PermissionType {
    SUPER_ADMIN(100L),
    MANAGE_USERS(200L),
    MANAGE_DEPARTMENTS(300L),
    MANAGE_CLASSROOMS(400L),
    MANAGE_STUDENTS(500L),
    MANAGE_SUBJECTS(600L),
    MANAGE_LESSONS(700L),
    MANAGE_REQUESTS(800L);

    private static final Map<Long, PermissionType> ID_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(PermissionType::getId, Function.identity()));

    private final long id;

    PermissionType(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public static Optional<PermissionType> findById(long id) {
        return Optional.ofNullable(ID_MAP.get(id));
    }
}
