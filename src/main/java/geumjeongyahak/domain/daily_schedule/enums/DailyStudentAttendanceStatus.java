package geumjeongyahak.domain.daily_schedule.enums;

public enum DailyStudentAttendanceStatus {
    PRESENT("출석"),
    ABSENT("결석"),
    LATE("지각");

    private final String displayName;

    DailyStudentAttendanceStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
