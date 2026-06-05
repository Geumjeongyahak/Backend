package geumjeongyahak.domain.teacher_application.v1.dto.response;

import geumjeongyahak.domain.subject.entity.Subject;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalTime;

@Schema(description = "교원 신청 실제 배정 시간표 과목 응답")
public record AssignedSubjectResponse(
    @Schema(description = "과목 ID", example = "3")
    Long subjectId,

    @Schema(description = "과목명", example = "국어")
    String subjectName,

    @Schema(description = "교시", example = "1")
    Integer period,

    @Schema(description = "시작 시간", example = "19:00:00")
    LocalTime startTime,

    @Schema(description = "종료 시간", example = "20:20:00")
    LocalTime endTime
) {
    public static AssignedSubjectResponse from(Subject subject) {
        return new AssignedSubjectResponse(
            subject.getId(),
            subject.getName(),
            subject.getPeriod(),
            subject.getStartTime(),
            subject.getEndTime()
        );
    }
}
