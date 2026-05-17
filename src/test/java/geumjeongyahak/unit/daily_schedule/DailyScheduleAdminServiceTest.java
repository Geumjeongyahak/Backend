package geumjeongyahak.unit.daily_schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.classroom.enums.ClassroomType;
import geumjeongyahak.domain.daily_schedule.entity.DailySchedule;
import geumjeongyahak.domain.daily_schedule.enums.DailyScheduleStatus;
import geumjeongyahak.domain.daily_schedule.repository.DailyScheduleRepository;
import geumjeongyahak.domain.daily_schedule.repository.DailyStudentAttendanceRepository;
import geumjeongyahak.domain.daily_schedule.repository.DailyTeacherAttendanceRepository;
import geumjeongyahak.domain.daily_schedule.service.DailyScheduleAdminService;
import geumjeongyahak.domain.daily_schedule.service.DailyScheduleService;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.UpdateDailyScheduleStatusRequest;
import geumjeongyahak.domain.daily_schedule.v1.dto.response.DailyScheduleDetailResponse;
import geumjeongyahak.domain.lesson.enums.LessonStatus;
import geumjeongyahak.domain.lesson.service.LessonProxyService;
import geumjeongyahak.domain.student.service.StudentProxyService;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.service.UserProxyService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DailyScheduleAdminServiceTest {

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

    @Mock
    private UserProxyService userProxyService;

    private DailyScheduleAdminService dailyScheduleAdminService;

    @BeforeEach
    void setUp() {
        DailyScheduleService dailyScheduleService = new DailyScheduleService(
            dailyScheduleRepository,
            dailyTeacherAttendanceRepository,
            dailyStudentAttendanceRepository,
            lessonProxyService,
            studentProxyService,
            userProxyService
        );
        dailyScheduleAdminService = new DailyScheduleAdminService(
            dailyScheduleRepository,
            dailyScheduleService,
            lessonProxyService
        );
    }

    @Test
    void updateStatus_updatesDailyScheduleStatus() {
        LocalDate lessonDate = LocalDate.of(2026, 5, 20);
        Classroom classroom = classroom(1L);
        User teacher = teacher(2L, "홍길동");
        DailySchedule dailySchedule = dailySchedule(100L, classroom, teacher, lessonDate);

        given(dailyScheduleRepository.findByIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(Optional.of(dailySchedule));
        given(lessonProxyService.getActiveLessonsByClassroomAndDate(classroom.getId(), lessonDate))
            .willReturn(List.of());
        given(dailyTeacherAttendanceRepository.findByDailyScheduleIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(Optional.empty());
        given(dailyStudentAttendanceRepository.findAllByDailyScheduleIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(List.of());

        DailyScheduleDetailResponse response = dailyScheduleAdminService.updateStatus(
            dailySchedule.getId(),
            1L,
            true,
            new UpdateDailyScheduleStatusRequest(DailyScheduleStatus.COMPLETED)
        );

        assertThat(dailySchedule.getStatus()).isEqualTo(DailyScheduleStatus.COMPLETED);
        assertThat(response.status()).isEqualTo(DailyScheduleStatus.COMPLETED);
        verify(lessonProxyService).updateActiveLessonsStatusByClassroomAndDate(
            classroom.getId(),
            lessonDate,
            LessonStatus.COMPLETED
        );
    }

    private DailySchedule dailySchedule(Long id, Classroom classroom, User teacher, LocalDate lessonDate) {
        DailySchedule dailySchedule = new DailySchedule(
            classroom,
            teacher,
            lessonDate,
            LocalTime.of(14, 0),
            LocalTime.of(16, 0)
        );
        ReflectionTestUtils.setField(dailySchedule, "id", id);
        return dailySchedule;
    }

    private Classroom classroom(Long id) {
        Classroom classroom = Classroom.builder()
            .name("장미반")
            .type(ClassroomType.WEEKDAY)
            .build();
        ReflectionTestUtils.setField(classroom, "id", id);
        return classroom;
    }

    private User teacher(Long id, String name) {
        User teacher = User.builder()
            .nickname(name)
            .name(name)
            .role(RoleType.VOLUNTEER)
            .build();
        ReflectionTestUtils.setField(teacher, "id", id);
        return teacher;
    }
}
