package sonmoeum.api.v1.subjects.dto.request;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSubjectRequest(
    @Schema(description = "분반 ID", example = "1")
    @NotNull(message = "분반 ID는 필수입니다.")
    Long classId,

    @Schema(description = "선생님 ID", example = "1")
    @NotNull(message = "선생님 ID는 필수입니다.")
    Long teacherId,

    @Schema(description = "과목 이름", example = "국어")
    @NotBlank(message = "과목 이름은 필수입니다.")
    @Size(max = 50, message = "과목 이름은 50자 이하여야 합니다.")
    String name,

    @Schema(description = "시작일", example = "2024-03-01")
    @NotNull(message = "시작일은 필수입니다.")
    LocalDate startAt,

    @Schema(description = "종료일", example = "2024-06-30")
    @NotNull(message = "종료일은 필수입니다.")
    LocalDate endAt,

    @Schema(description = "수업 횟수", example = "15")
    @NotNull(message = "수업 횟수는 필수입니다.")
    Integer times,

    @Schema(description = "요일", example = "MONDAY")
    @NotNull(message = "요일은 필수입니다.")
    DayOfWeek dayOfWeek,

    @Schema(description = "시작 시간", example = "14:00:00")
    @NotNull(message = "시작 시간은 필수입니다.")
    LocalTime startTime,

    @Schema(description = "종료 시간", example = "16:00:00")
    @NotNull(message = "종료 시간은 필수입니다.")
    LocalTime endTime,

    @Schema(description = "수업 시간(분)", example = "120")
    @NotNull(message = "수업 시간은 필수입니다.")
    Integer period,

    @Schema(description = "설명", example = "1학기 국어 수업")
    @Size(max = 1000, message = "설명은 1000자 이하여야 합니다.")
    String description
) {}
