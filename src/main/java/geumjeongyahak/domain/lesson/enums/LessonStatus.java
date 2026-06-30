package geumjeongyahak.domain.lesson.enums;

public enum LessonStatus {
    SCHEDULED("예정"),
    CANCELED("취소"),
    COMPLETED("완료");

    private final String displayName;

    LessonStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
