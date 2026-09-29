package geumjeongyahak.e2e.lesson;

import static org.assertj.core.api.Assertions.assertThat;

import geumjeongyahak.domain.lesson.service.LessonAdminViewService;
import geumjeongyahak.domain.lesson.service.LessonAdminViewService.LessonFilter;
import geumjeongyahak.e2e.BaseE2ETest;
import java.time.Clock;
import java.time.YearMonth;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("E2E: 관리자 수업 목록 기간 테스트")
class LessonAdminListRangeTest extends BaseE2ETest {

    @Autowired
    private LessonAdminViewService lessonAdminViewService;

    @Autowired
    private Clock clock;

    @Test
    @DisplayName("기간을 주지 않으면 이번 달 수업만 읽는다")
    void withoutRange_readsCurrentMonthOnly() {
        YearMonth thisMonth = YearMonth.now(clock);

        var page = lessonAdminViewService.getLessons(new LessonFilter(null, null, null, 0, 100, null));

        assertThat(page.content()).allSatisfy(row -> assertThat(YearMonth.from(row.date())).isEqualTo(thisMonth));
    }
}
