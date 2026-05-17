package geumjeongyahak.domain.daily_schedule.enums;

public enum DailyScheduleStatus {
    SCHEDULED("예정"),
    CANCELLED("휴강"),
    COMPLETED("완료");

    private final String displayName;

    DailyScheduleStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
