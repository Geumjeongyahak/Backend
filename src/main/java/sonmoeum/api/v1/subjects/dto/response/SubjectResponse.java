package sonmoeum.api.v1.subjects.dto.response;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import sonmoeum.domain.subject.entity.Subject;

import io.swagger.v3.oas.annotations.media.Schema;

public record SubjectResponse(
    @Schema(description = "과목 ID", example = "1")
    Long id,
    @Schema(description = "분반 ID", example = "1")
    Long classId,
    @Schema(description = "선생님 ID", example = "1")
    Long teacherId,
    @Schema(description = "과목 이름", example = "국어")
    String name,
    @Schema(description = "시작일", example = "2024-03-01")
    LocalDate startAt,
    @Schema(description = "종료일", example = "2024-06-30")
    LocalDate endAt,
    @Schema(description = "수업 횟수", example = "15")
    Integer times,
    @Schema(description = "요일", example = "MONDAY")
    DayOfWeek dayOfWeek,
    @Schema(description = "시작 시간", example = "14:00:00")
    LocalTime startTime,
    @Schema(description = "종료 시간", example = "16:00:00")
    LocalTime endTime,
    @Schema(description = "수업 시간(분)", example = "120")
    Integer period,
    @Schema(description = "설명", example = "1학기 국어 수업")
    String description
) {
    public static SubjectResponse from(Subject subject) {
        return new SubjectResponse(
            subject.getId(),
            subject.getClassroom().getId(),
            subject.getTeacher().getId(),
            subject.getName(),
            subject.getStartAt(),
            subject.getEndAt(),
            subject.getTimes(),
            subject.getDayOfWeek(),
            subject.getStartTime(),
            subject.getEndTime(),
            subject.getPeriod(),
            subject.getDescription()
        );
    }
}
