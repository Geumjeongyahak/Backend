package geumjeongyahak.domain.daily_schedule.v1.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "수업일지 관리 시트 링크 응답")
public record DailyScheduleJournalSheetLinkResponse(
    @Schema(
        description = "봉사시간 증빙용 수업일지 관리 Google Sheets URL",
        example = "https://docs.google.com/spreadsheets/d/example/edit"
    )
    String url
) {
}
