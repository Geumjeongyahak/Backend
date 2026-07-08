package geumjeongyahak.unit.daily_schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import geumjeongyahak.domain.daily_schedule.service.DailyScheduleAdminService;
import geumjeongyahak.domain.daily_schedule.v1.controller.DailyScheduleAdminController;
import geumjeongyahak.domain.daily_schedule.v1.dto.response.DailyScheduleJournalSheetLinkResponse;
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
}
