package geumjeongyahak.domain.daily_schedule.enums;

public enum DailyTeacherAttendanceStatus {
    PRESENT("출석"),
    ABSENT("결석"),
    LATE("지각"),
    EXCUSED("공결");

    private final String displayName;

    DailyTeacherAttendanceStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isActualAttendance() {
        return this == PRESENT || this == LATE;
    }
}
