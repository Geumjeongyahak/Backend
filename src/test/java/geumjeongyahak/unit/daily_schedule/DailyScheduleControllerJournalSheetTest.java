package geumjeongyahak.unit.daily_schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import geumjeongyahak.domain.daily_schedule.service.DailyScheduleAdminService;
import geumjeongyahak.domain.daily_schedule.v1.controller.DailyScheduleAdminController;
import geumjeongyahak.domain.daily_schedule.v1.dto.response.DailyScheduleJournalSheetLinkResponse;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DailyScheduleControllerJournalSheetTest {

    @Mock
    private DailyScheduleAdminService dailyScheduleAdminService;

    @Test
    void getJournalSheetLink_returnsServiceResponse() {
        DailyScheduleAdminController controller = new DailyScheduleAdminController(dailyScheduleAdminService);
        DailyScheduleJournalSheetLinkResponse serviceResponse =
            new DailyScheduleJournalSheetLinkResponse("https://docs.google.com/spreadsheets/d/sheet-id/edit");
        given(dailyScheduleAdminService.getJournalSheetLink()).willReturn(serviceResponse);

        var response = controller.getJournalSheetLink();

        assertThat(response.getBody()).isEqualTo(serviceResponse);
        verify(dailyScheduleAdminService).getJournalSheetLink();
    }

    @Test
    void getMonthlyJournalSheetData_returnsServiceResponse() {
        DailyScheduleAdminController controller = new DailyScheduleAdminController(dailyScheduleAdminService);
        YearMonth month = YearMonth.of(2026, 7);
        given(dailyScheduleAdminService.getMonthlyJournalSheetData(month)).willReturn(List.of());

        var response = controller.getMonthlyJournalSheetData(month);

        assertThat(response.getBody()).isEmpty();
        verify(dailyScheduleAdminService).getMonthlyJournalSheetData(month);
    }
}
