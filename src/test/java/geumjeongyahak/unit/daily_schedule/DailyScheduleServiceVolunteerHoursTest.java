package geumjeongyahak.unit.daily_schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import geumjeongyahak.domain.daily_schedule.enums.DailyScheduleStatus;
import geumjeongyahak.domain.daily_schedule.enums.DailyTeacherAttendanceStatus;
import geumjeongyahak.domain.daily_schedule.exception.DailyScheduleVolunteerHoursForbiddenException;
import geumjeongyahak.domain.daily_schedule.repository.DailyScheduleRepository;
import geumjeongyahak.domain.daily_schedule.repository.DailyStudentAttendanceRepository;
import geumjeongyahak.domain.daily_schedule.repository.DailyTeacherAttendanceRepository;
import geumjeongyahak.domain.daily_schedule.service.DailyScheduleService;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.DailyScheduleVolunteerHoursRequest;
import geumjeongyahak.domain.daily_schedule.v1.dto.response.DailyScheduleVolunteerHoursResponse;
import geumjeongyahak.domain.lesson.service.LessonProxyService;
import geumjeongyahak.domain.student.service.StudentProxyService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DailyScheduleServiceVolunteerHoursTest {

    @Mock
    private DailyScheduleRepository dailyScheduleRepository;

    @Mock
    private DailyTeacherAttendanceRepository dailyTeacherAttendanceRepository;

    @Mock
    private DailyStudentAttendanceRepository dailyStudentAttendanceRepository;

    @Mock
    private LessonProxyService lessonProxyService;

    @Mock
    private StudentProxyService studentProxyService;

    @InjectMocks
    private DailyScheduleService dailyScheduleService;

    @Test
    void getVolunteerHours_returnsRequesterHoursWhenTeacherIdIsNull() {
        Long requesterId = 2L;
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 31);
        given(dailyTeacherAttendanceRepository.sumVolunteerServiceMinutes(
            requesterId,
            from,
            to,
            DailyScheduleStatus.COMPLETED,
            DailyTeacherAttendanceStatus.ABSENT
        )).willReturn(360L);

        DailyScheduleVolunteerHoursResponse response = dailyScheduleService.getVolunteerHours(
            requesterId,
            false,
            new DailyScheduleVolunteerHoursRequest(from, to, null)
        );

        assertThat(response.teacherId()).isEqualTo(requesterId);
        assertThat(response.totalVolunteerServiceMinutes()).isEqualTo(360L);
        assertThat(response.totalVolunteerServiceHours()).isEqualByComparingTo(new BigDecimal("6.00"));
    }

    @Test
    void getVolunteerHours_allowsOtherTeacherWhenUserCanReadAnyDailySchedule() {
        Long requesterId = 2L;
        Long targetTeacherId = 3L;
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 31);
        given(dailyTeacherAttendanceRepository.sumVolunteerServiceMinutes(
            targetTeacherId,
            from,
            to,
            DailyScheduleStatus.COMPLETED,
            DailyTeacherAttendanceStatus.ABSENT
        )).willReturn(125L);

        DailyScheduleVolunteerHoursResponse response = dailyScheduleService.getVolunteerHours(
            requesterId,
            true,
            new DailyScheduleVolunteerHoursRequest(from, to, targetTeacherId)
        );

        assertThat(response.teacherId()).isEqualTo(targetTeacherId);
        assertThat(response.totalVolunteerServiceMinutes()).isEqualTo(125L);
        assertThat(response.totalVolunteerServiceHours()).isEqualByComparingTo(new BigDecimal("2.08"));
    }

    @Test
    void getVolunteerHours_returnsTotalHoursWhenRangeIsNull() {
        Long requesterId = 2L;
        given(dailyTeacherAttendanceRepository.sumVolunteerServiceMinutes(
            requesterId,
            null,
            null,
            DailyScheduleStatus.COMPLETED,
            DailyTeacherAttendanceStatus.ABSENT
        )).willReturn(480L);

        DailyScheduleVolunteerHoursResponse response = dailyScheduleService.getVolunteerHours(
            requesterId,
            false,
            new DailyScheduleVolunteerHoursRequest(null, null, null)
        );

        assertThat(response.teacherId()).isEqualTo(requesterId);
        assertThat(response.from()).isNull();
        assertThat(response.to()).isNull();
        assertThat(response.totalVolunteerServiceMinutes()).isEqualTo(480L);
        assertThat(response.totalVolunteerServiceHours()).isEqualByComparingTo(new BigDecimal("8.00"));
    }

    @Test
    void getVolunteerHours_throwsWhenUserRequestsOtherTeacherWithoutPermission() {
        Long requesterId = 2L;
        Long targetTeacherId = 3L;
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 31);

        assertThatThrownBy(() -> dailyScheduleService.getVolunteerHours(
            requesterId,
            false,
            new DailyScheduleVolunteerHoursRequest(from, to, targetTeacherId)
        )).isInstanceOf(DailyScheduleVolunteerHoursForbiddenException.class);

        verify(dailyTeacherAttendanceRepository, never()).sumVolunteerServiceMinutes(
            anyLong(),
            any(),
            any(),
            any(),
            any()
        );
    }
}
