package sonmoeum.domain.auth.enums;

public enum RoleLevel {
    UNKNOWN, BASIC, DEPARTMENT, ADDITIONAL;

    public static RoleLevel getByCode(long code) {
        if (code < 1000) return BASIC;
        if (code < 2000) return DEPARTMENT;
        if (code < 3000) return ADDITIONAL;
        return UNKNOWN;
    }
}
