package geumjeongyahak.domain.lesson.enums;

public enum TeacherAttendanceStatus {
    PRESENT("출석"),
    ABSENT("결석"),
    LATE("지각"),
    EXCUSED("공결");

    private final String displayName;

    TeacherAttendanceStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
