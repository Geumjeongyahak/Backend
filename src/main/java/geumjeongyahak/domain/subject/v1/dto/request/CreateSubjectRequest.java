package geumjeongyahak.domain.subject.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import geumjeongyahak.common.validation.annotation.ValidSubjectSchedule;

@ValidSubjectSchedule
@Schema(description = "과목 생성 요청")
public record CreateSubjectRequest(
    @NotNull
    @Schema(description = "교실 ID", example = "1")
    Long classroomId,

    @NotNull
    @Schema(description = "교사 ID", example = "2")
    Long teacherId,

    @NotBlank
    @Size(max = 50)
    @Schema(description = "과목명", example = "국어")
    String name,

    @NotNull
    @Schema(description = "과목 운영 시작 일자", example = "2026-06-01")
    LocalDate startAt,

    @NotNull
    @Schema(description = "과목 운영 종료 일자", example = "2026-08-30")
    LocalDate endAt,

    @NotNull
    @Min(1)
    @Schema(description = "과목의 총 수업 횟수", example = "12")
    Integer times,

    @NotNull
    @Schema(description = "과목의 정기 수업 요일", example = "MONDAY")
    DayOfWeek dayOfWeek,

    @NotNull
    @Schema(description = "과목의 정기 수업 시작 시간", example = "19:20:00")
    LocalTime startTime,

    @NotNull
    @Schema(description = "과목의 정기 수업 종료 시간", example = "20:00:00")
    LocalTime endTime,

    @NotNull
    @Min(1)
    @Schema(description = "교시", example = "1")
    Integer period,

    @Schema(description = "과목 설명", example = "과목 설명")
    String description
) {}
