package geumjeongyahak.domain.subject.v1.dto.response;

import geumjeongyahak.domain.subject.entity.Subject;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.DayOfWeek;

@Schema(description = "복사하지 못한 과목과 이유. 409 응답의 failures 항목")
public record SubjectCopyFailure(
    @Schema(description = "원본 과목 ID", example = "57")
    Long sourceSubjectId,
    @Schema(description = "분반 ID", example = "19")
    Long classroomId,
    @Schema(description = "분반 이름", example = "씨앗반")
    String classroomName,
    @Schema(description = "요일", example = "SATURDAY")
    DayOfWeek dayOfWeek,
    @Schema(description = "교시", example = "3")
    Integer period,
    @Schema(description = "과목명", example = "영어")
    String subjectName,
    @Schema(description = "실패 이유", example = "같은 분반에 실제 수업 날짜와 시간이 겹치는 과목이 존재합니다.")
    String reason
) {

    public static SubjectCopyFailure of(Subject source, String reason) {
        return new SubjectCopyFailure(
            source.getId(),
            source.getClassroom().getId(),
            source.getClassroom().getName(),
            source.getDayOfWeek(),
            source.getPeriod(),
            source.getName(),
            reason
        );
    }
}
