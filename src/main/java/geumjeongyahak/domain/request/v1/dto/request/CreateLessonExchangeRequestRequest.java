package geumjeongyahak.domain.request.v1.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.OptBoolean;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateLessonExchangeRequestRequest(

    @NotNull
    @Schema(description = "교환 대상 수업일. 로그인 사용자의 해당 날짜 DailySchedule에 연결된 수업 전체를 하루 단위로 교환 요청합니다.", example = "2026-06-10")
    LocalDate lessonDate,

    @NotBlank
    @Schema(description = "요청 제목", example = "수업 교환 요청")
    String title,

    @NotBlank
    @Schema(description = "요청 내용", example = "사정으로 인해 교환을 요청합니다.")
    String content,

    @JsonFormat(pattern = "uuuu-MM-dd", lenient = OptBoolean.FALSE)
    @Schema(description = "요청 만료일(yyyy-MM-dd). 생략하거나 수업일을 선택하면 대상 수업 시작 시각에 만료되며, 이전 날짜를 선택하면 해당 날짜 23:59:59에 만료됩니다.", example = "2026-06-09")
    LocalDate expiresDate
) {}
