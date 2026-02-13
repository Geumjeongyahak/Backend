package sonmoeum.domain.subject.v1.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import sonmoeum.domain.subject.entity.Subject;

@Schema(description = "과목 상세 응답")
public record SubjectDetailResponse(

    @Schema(description = "과목 ID", example = "1")
    Long id,

    @Schema(description = "교실 ID", example = "1")
    Long classroomId,

    @Schema(description = "교사 ID", example = "1")
    Long teacherId,

    @Schema(description = "과목명", example = "국어")
    String name,

    @Schema(description = "과목 운영 시작 일자", example = "2026-02-01")
    LocalDate startAt,

    @Schema(description = "과목 운영 종료 일자", example = "2026-02-28")
    LocalDate endAt,

    @Schema(description = "과목의 총 수업 횟수", example = "12")
    Integer times,

    @Schema(description = "과목의 정기 수업 요일", example = "MONDAY")
    DayOfWeek dayOfWeek,

    @Schema(description = "과목의 정기 수업 시작 시간", example = "18:00")
    LocalTime startTime,

    @Schema(description = "과목의 정기 수업 종료 시간", example = "20:00")
    LocalTime endTime,

    @Schema(description = "교시", example = "1")
    Integer period,

    @Schema(description = "과목 설명", example = "과목 설명")
    String description
) {
    public static SubjectDetailResponse from(Subject subject) {
        return new SubjectDetailResponse(
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
