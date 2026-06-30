package geumjeongyahak.domain.event.v1.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;

import geumjeongyahak.common.validation.annotation.ValidEventTime;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@ValidEventTime
@Schema(description = "행사 생성 요청")
public record CreateEventRequest(
    @NotBlank
    @Size(max = 100)
    @Schema(description = "행사 제목", example = "문학의 밤")
    String title,

    @Schema(description = "행사 설명", example = "문학의 밤 행사에 대한 상세 설명입니다.")
    String description,

    @NotNull
    @Schema(description = "행사 날짜", example = "2026-05-13")
    LocalDate eventDate,

    @Schema(description = "행사 시작 시간", example = "19:00:00", nullable = true)
    LocalTime startTime,

    @Schema(description = "행사 종료 시간", example = "21:00:00", nullable = true)
    LocalTime endTime
) {}
