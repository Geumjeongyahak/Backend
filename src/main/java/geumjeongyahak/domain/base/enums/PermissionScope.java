package geumjeongyahak.domain.base.enums;

public enum PermissionScope {
    GLOBAL_ONLY("global", true, false),
    TARGET_ONLY("target", false, true),
    BOTH("both", true, true);

    private final String code;
    private final boolean globalAllowed;
    private final boolean targetAllowed;

    PermissionScope(String code, boolean globalAllowed, boolean targetAllowed) {
        this.code = code;
        this.globalAllowed = globalAllowed;
        this.targetAllowed = targetAllowed;
    }

    public String getCode() {
        return code;
    }

    public boolean allowsGlobal() {
        return globalAllowed;
    }

    public boolean allowsTarget() {
        return targetAllowed;
    }

    public boolean allows(boolean isGlobal) {
        return isGlobal ? allowsGlobal() : allowsTarget();
    }
}
