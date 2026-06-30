package geumjeongyahak.domain.request.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateAbsenceRequestRequest(

    @NotNull
    @Schema(description = "결석할 수업일. 로그인 사용자의 해당 날짜 DailySchedule을 대상으로 결석 요청을 생성합니다.", example = "2026-06-10")
    LocalDate lessonDate,

    @NotBlank
    @Schema(description = "결석 요청 제목", example = "개인 사정으로 결석합니다")
    String title,

    @NotBlank
    @Schema(description = "결석 사유", example = "개인 사정으로 인한 결석")
    String reason
) {}
