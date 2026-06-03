package geumjeongyahak.domain.users.v1.dto.response;

import geumjeongyahak.domain.subject.entity.Subject;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Schema(description = "사용자 교사 담당 과목 응답")
public record TeacherAssignmentResponse(
    @Schema(description = "과목 ID", example = "1")
    Long subjectId,

    @Schema(description = "과목명", example = "국어")
    String subjectName,

    @Schema(description = "담당 과목 분반 ID", example = "1")
    Long classroomId,

    @Schema(description = "담당 과목 분반명", example = "국화반")
    String classroomName,

    @Schema(description = "수업 요일", example = "MONDAY")
    DayOfWeek dayOfWeek,

    @Schema(description = "수업 시작 시간", example = "19:20:00")
    LocalTime startTime,

    @Schema(description = "수업 종료 시간", example = "20:00:00")
    LocalTime endTime,

    @Schema(description = "교시", example = "1")
    Integer period,

    @Schema(description = "과목 운영 시작일", example = "2026-06-01")
    LocalDate startAt,

    @Schema(description = "과목 운영 종료일", example = "2026-12-31")
    LocalDate endAt,

    @Schema(description = "담당 교사 배정 시각", example = "2026-06-01T10:15:30")
    LocalDateTime teacherAssignedAt
) {
    public static TeacherAssignmentResponse from(Subject subject) {
        return new TeacherAssignmentResponse(
            subject.getId(),
            subject.getName(),
            subject.getClassroom().getId(),
            subject.getClassroom().getName(),
            subject.getDayOfWeek(),
            subject.getStartTime(),
            subject.getEndTime(),
            subject.getPeriod(),
            subject.getStartAt(),
            subject.getEndAt(),
            subject.getTeacherAssignedAt()
        );
    }
}
