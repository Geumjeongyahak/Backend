package geumjeongyahak.e2e.daily_schedule;

import static org.assertj.core.api.Assertions.assertThat;

import geumjeongyahak.domain.daily_schedule.service.DailyScheduleAdminViewService;
import geumjeongyahak.domain.daily_schedule.service.DailyScheduleAdminViewService.DailyScheduleFilter;
import geumjeongyahak.e2e.BaseE2ETest;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("E2E: 관리자 하루 일정 목록 기간 테스트")
class DailyScheduleAdminRangeTest extends BaseE2ETest {

    @Autowired
    private DailyScheduleAdminViewService dailyScheduleAdminViewService;

    @Test
    @DisplayName("기간이 42일을 넘으면 시작일부터 42일로 줄인다")
    void longRange_isClampedTo42Days() {
        LocalDate from = LocalDate.now().plusDays(7);

        var page = dailyScheduleAdminViewService.getDailySchedules(
            new DailyScheduleFilter(from, from.plusDays(100), null, null, null));

        assertThat(page.to()).isEqualTo(from.plusDays(41));
    }
}
