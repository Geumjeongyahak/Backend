package sonmoeum.domain.subject.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import sonmoeum.common.validation.annotation.ValidSubjectSchedule;

@ValidSubjectSchedule
public record UpdateSubjectRequest(

    @Schema(description = "교실 ID", example = "1")
    Long classroomId,

    @Schema(description = "교사 ID", example = "3")
    Long teacherId,

    @Size(max = 50)
    @Schema(description = "과목명", example = "수학")
    String name,

    @Schema(description = "과목 운영 시작 일자", example = "2026-06-15")
    LocalDate startAt,

    @Schema(description = "과목 운영 종료 일자", example = "2026-08-30")
    LocalDate endAt,

    @Min(1)
    @Schema(description = "과목의 총 수업 횟수", example = "24")
    Integer times,

    @Schema(description = "과목의 정기 수업 요일", example = "THURSDAY")
    DayOfWeek dayOfWeek,

    @Schema(description = "과목의 정기 수업 시작 시간", example = "20:10:00")
    LocalTime startTime,

    @Schema(description = "과목의 정기 수업 종료 시간", example = "20:50:00")
    LocalTime endTime,

    @Min(1)
    @Schema(description = "교시", example = "2")
    Integer period,

    @Schema(description = "과목 설명", example = "과목 설명 수정")
    String description
) {
}
