package geumjeongyahak.domain.event.v1.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;

import geumjeongyahak.common.validation.annotation.ValidEventTime;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@ValidEventTime
@Schema(description = "행사 수정 요청")
public record UpdateEventRequest(
    @Pattern(regexp = ".*\\S.*", message = "title은 공백일 수 없습니다.")
    @Size(max = 100)
    @Schema(description = "행사 제목", example = "문학의 밤")
    String title,

    @Schema(description = "행사 설명", example = "수정된 행사 설명입니다.")
    String description,

    @Schema(description = "행사 날짜", example = "2026-05-13")
    LocalDate eventDate,

    @Schema(description = "행사 시작 시간", example = "19:30:00", nullable = true)
    LocalTime startTime,

    @Schema(description = "행사 종료 시간", example = "21:30:00", nullable = true)
    LocalTime endTime
) {}
