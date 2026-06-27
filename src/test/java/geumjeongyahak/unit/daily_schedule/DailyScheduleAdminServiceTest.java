package geumjeongyahak.unit.daily_schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.classroom.enums.ClassroomType;
import geumjeongyahak.domain.daily_schedule.entity.DailySchedule;
import geumjeongyahak.domain.daily_schedule.entity.DailyTeacherAttendance;
import geumjeongyahak.domain.daily_schedule.enums.DailyScheduleStatus;
import geumjeongyahak.domain.daily_schedule.enums.DailyTeacherAttendanceStatus;
import geumjeongyahak.domain.daily_schedule.exception.InvalidDailyScheduleAttendanceStateException;
import geumjeongyahak.domain.daily_schedule.exception.InvalidDailyTeacherCheckOutTimeException;
import geumjeongyahak.domain.daily_schedule.repository.DailyScheduleRepository;
import geumjeongyahak.domain.daily_schedule.repository.DailyStudentAttendanceRepository;
import geumjeongyahak.domain.daily_schedule.repository.DailyTeacherAttendanceRepository;
import geumjeongyahak.domain.daily_schedule.service.DailyScheduleAdminService;
import geumjeongyahak.domain.daily_schedule.service.DailyScheduleService;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.UpdateDailyScheduleStatusRequest;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.UpdateDailyTeacherAttendanceCorrectionRequest;
import geumjeongyahak.domain.daily_schedule.v1.dto.response.DailyScheduleDetailResponse;
import geumjeongyahak.domain.lesson.entity.Lesson;
import geumjeongyahak.domain.lesson.enums.LessonStatus;
import geumjeongyahak.domain.lesson.service.LessonProxyService;
import geumjeongyahak.domain.student.service.StudentProxyService;
import geumjeongyahak.domain.subject.entity.Subject;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.service.UserProxyService;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
            dailyTeacherAttendanceRepository,
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

    @Test
    void correctTeacherAttendance_updatesAttendanceAndCheckOutTime() {
        LocalDate lessonDate = LocalDate.of(2026, 5, 20);
        Classroom classroom = classroom(1L);
        User teacher = teacher(2L, "홍길동");
        DailySchedule dailySchedule = dailySchedule(100L, classroom, teacher, lessonDate);
        DailyTeacherAttendance teacherAttendance = teacherAttendance(dailySchedule);
        LocalDateTime attendedAt = LocalDateTime.of(2026, 5, 20, 14, 30);
        LocalDateTime checkedOutAt = LocalDateTime.of(2026, 5, 20, 16, 0);

        given(dailyScheduleRepository.findByIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(Optional.of(dailySchedule));
        given(dailyTeacherAttendanceRepository.findByDailyScheduleIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(Optional.of(teacherAttendance));
        given(lessonProxyService.getActiveLessonsByClassroomAndDate(classroom.getId(), lessonDate))
            .willReturn(List.of());
        given(dailyStudentAttendanceRepository.findAllByDailyScheduleIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(List.of());

        DailyScheduleDetailResponse response = dailyScheduleAdminService.correctTeacherAttendance(
            dailySchedule.getId(),
            1L,
            true,
            new UpdateDailyTeacherAttendanceCorrectionRequest(
                DailyTeacherAttendanceStatus.LATE,
                attendedAt,
                checkedOutAt
            )
        );

        assertThat(teacherAttendance.getStatus()).isEqualTo(DailyTeacherAttendanceStatus.LATE);
        assertThat(teacherAttendance.getAttendedAt()).isEqualTo(attendedAt);
        assertThat(teacherAttendance.getCheckedOutAt()).isEqualTo(checkedOutAt);
        assertThat(response.teacherAttendance().status()).isEqualTo(DailyTeacherAttendanceStatus.LATE);
        assertThat(response.teacherAttendance().attendedAt()).isEqualTo(attendedAt);
        assertThat(response.teacherAttendance().checkedOutAt()).isEqualTo(checkedOutAt);
    }

    @Test
    void correctTeacherAttendance_overwritesExistingCheckOutTime() {
        LocalDate lessonDate = LocalDate.of(2026, 5, 20);
        Classroom classroom = classroom(1L);
        User teacher = teacher(2L, "홍길동");
        DailySchedule dailySchedule = dailySchedule(100L, classroom, teacher, lessonDate);
        DailyTeacherAttendance teacherAttendance = teacherAttendance(dailySchedule);
        teacherAttendance.checkOut(LocalDateTime.of(2026, 5, 20, 15, 30));
        LocalDateTime checkedOutAt = LocalDateTime.of(2026, 5, 20, 16, 30);

        given(dailyScheduleRepository.findByIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(Optional.of(dailySchedule));
        given(dailyTeacherAttendanceRepository.findByDailyScheduleIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(Optional.of(teacherAttendance));
        given(lessonProxyService.getActiveLessonsByClassroomAndDate(classroom.getId(), lessonDate))
            .willReturn(List.of());
        given(dailyStudentAttendanceRepository.findAllByDailyScheduleIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(List.of());

        DailyScheduleDetailResponse response = dailyScheduleAdminService.correctTeacherAttendance(
            dailySchedule.getId(),
            1L,
            true,
            new UpdateDailyTeacherAttendanceCorrectionRequest(
                DailyTeacherAttendanceStatus.PRESENT,
                LocalDateTime.of(2026, 5, 20, 14, 0),
                checkedOutAt
            )
        );

        assertThat(teacherAttendance.getCheckedOutAt()).isEqualTo(checkedOutAt);
        assertThat(response.teacherAttendance().checkedOutAt()).isEqualTo(checkedOutAt);
    }

    @Test
    void correctTeacherAttendance_clearsTimesAndRecalculatesScheduleWhenStatusIsAbsent() {
        LocalDate lessonDate = LocalDate.of(2026, 5, 20);
        Classroom classroom = classroom(1L);
        User teacher = teacher(2L, "홍길동");
        DailySchedule dailySchedule = dailySchedule(100L, classroom, teacher, lessonDate);
        dailySchedule.updateStatus(DailyScheduleStatus.COMPLETED);
        DailyTeacherAttendance teacherAttendance = teacherAttendance(dailySchedule);
        teacherAttendance.checkOut(LocalDateTime.of(2026, 5, 20, 16, 0));

        given(dailyScheduleRepository.findByIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(Optional.of(dailySchedule));
        given(dailyTeacherAttendanceRepository.findByDailyScheduleIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(Optional.of(teacherAttendance));
        given(lessonProxyService.getActiveLessonsByClassroomAndDate(classroom.getId(), lessonDate))
            .willReturn(List.of());
        given(dailyStudentAttendanceRepository.findAllByDailyScheduleIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(List.of());

        DailyScheduleDetailResponse response = dailyScheduleAdminService.correctTeacherAttendance(
            dailySchedule.getId(),
            1L,
            true,
            new UpdateDailyTeacherAttendanceCorrectionRequest(DailyTeacherAttendanceStatus.ABSENT, null, null)
        );

        assertThat(teacherAttendance.getStatus()).isEqualTo(DailyTeacherAttendanceStatus.ABSENT);
        assertThat(teacherAttendance.getAttendedAt()).isNull();
        assertThat(teacherAttendance.getCheckedOutAt()).isNull();
        assertThat(dailySchedule.getStatus()).isEqualTo(DailyScheduleStatus.SCHEDULED);
        assertThat(response.status()).isEqualTo(DailyScheduleStatus.SCHEDULED);
        verify(lessonProxyService).updateActiveLessonsStatusByClassroomAndDate(
            classroom.getId(),
            lessonDate,
            LessonStatus.SCHEDULED
        );
    }

    @Test
    void correctTeacherAttendance_recalculatesScheduleWhenStatusIsExcused() {
        LocalDate lessonDate = LocalDate.of(2026, 5, 20);
        Classroom classroom = classroom(1L);
        User teacher = teacher(2L, "홍길동");
        DailySchedule dailySchedule = dailySchedule(100L, classroom, teacher, lessonDate);
        dailySchedule.updateStatus(DailyScheduleStatus.COMPLETED);
        DailyTeacherAttendance teacherAttendance = teacherAttendance(dailySchedule);
        teacherAttendance.checkOut(LocalDateTime.of(2026, 5, 20, 16, 0));
        Lesson lesson = lesson(subject(classroom, teacher, lessonDate), teacher, lessonDate);
        lesson.updateNote("수업 내용");

        given(dailyScheduleRepository.findByIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(Optional.of(dailySchedule));
        given(dailyTeacherAttendanceRepository.findByDailyScheduleIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(Optional.of(teacherAttendance));
        given(lessonProxyService.getActiveLessonsByClassroomAndDate(classroom.getId(), lessonDate))
            .willReturn(List.of(lesson));
        given(dailyStudentAttendanceRepository.findAllByDailyScheduleIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(List.of());

        DailyScheduleDetailResponse response = dailyScheduleAdminService.correctTeacherAttendance(
            dailySchedule.getId(),
            1L,
            true,
            new UpdateDailyTeacherAttendanceCorrectionRequest(DailyTeacherAttendanceStatus.EXCUSED, null, null)
        );

        assertThat(teacherAttendance.getStatus()).isEqualTo(DailyTeacherAttendanceStatus.EXCUSED);
        assertThat(teacherAttendance.getAttendedAt()).isNull();
        assertThat(teacherAttendance.getCheckedOutAt()).isNull();
        assertThat(dailySchedule.getStatus()).isEqualTo(DailyScheduleStatus.SCHEDULED);
        assertThat(response.status()).isEqualTo(DailyScheduleStatus.SCHEDULED);
        verify(lessonProxyService).updateActiveLessonsStatusByClassroomAndDate(
            classroom.getId(),
            lessonDate,
            LessonStatus.SCHEDULED
        );
    }

    @Test
    void correctTeacherAttendance_completesScheduleWhenAttendanceAndJournalAreCompleted() {
        LocalDate lessonDate = LocalDate.of(2026, 5, 20);
        Classroom classroom = classroom(1L);
        User teacher = teacher(2L, "홍길동");
        DailySchedule dailySchedule = dailySchedule(100L, classroom, teacher, lessonDate);
        DailyTeacherAttendance teacherAttendance = teacherAttendance(dailySchedule);
        Lesson lesson = lesson(subject(classroom, teacher, lessonDate), teacher, lessonDate);
        lesson.updateNote("수업 내용");

        given(dailyScheduleRepository.findByIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(Optional.of(dailySchedule));
        given(dailyTeacherAttendanceRepository.findByDailyScheduleIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(Optional.of(teacherAttendance));
        given(lessonProxyService.getActiveLessonsByClassroomAndDate(classroom.getId(), lessonDate))
            .willReturn(List.of(lesson));
        given(dailyStudentAttendanceRepository.findAllByDailyScheduleIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(List.of());

        DailyScheduleDetailResponse response = dailyScheduleAdminService.correctTeacherAttendance(
            dailySchedule.getId(),
            1L,
            true,
            new UpdateDailyTeacherAttendanceCorrectionRequest(
                DailyTeacherAttendanceStatus.PRESENT,
                LocalDateTime.of(2026, 5, 20, 14, 0),
                LocalDateTime.of(2026, 5, 20, 16, 0)
            )
        );

        assertThat(dailySchedule.getStatus()).isEqualTo(DailyScheduleStatus.COMPLETED);
        assertThat(response.status()).isEqualTo(DailyScheduleStatus.COMPLETED);
        verify(lessonProxyService).updateActiveLessonsStatusByClassroomAndDate(
            classroom.getId(),
            lessonDate,
            LessonStatus.COMPLETED
        );
    }

    @Test
    void correctTeacherAttendance_throwsWhenCheckOutTimeIsBeforeAttendanceTime() {
        LocalDate lessonDate = LocalDate.of(2026, 5, 20);
        Classroom classroom = classroom(1L);
        User teacher = teacher(2L, "홍길동");
        DailySchedule dailySchedule = dailySchedule(100L, classroom, teacher, lessonDate);
        DailyTeacherAttendance teacherAttendance = teacherAttendance(dailySchedule);

        given(dailyScheduleRepository.findByIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(Optional.of(dailySchedule));
        given(dailyTeacherAttendanceRepository.findByDailyScheduleIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(Optional.of(teacherAttendance));

        assertThatThrownBy(() -> dailyScheduleAdminService.correctTeacherAttendance(
            dailySchedule.getId(),
            1L,
            true,
            new UpdateDailyTeacherAttendanceCorrectionRequest(
                DailyTeacherAttendanceStatus.PRESENT,
                LocalDateTime.of(2026, 5, 20, 14, 0),
                LocalDateTime.of(2026, 5, 20, 13, 50)
            )
        )).isInstanceOf(InvalidDailyTeacherCheckOutTimeException.class);
    }

    @Test
    void correctTeacherAttendance_throwsWhenDailyScheduleIsCancelled() {
        LocalDate lessonDate = LocalDate.of(2026, 5, 20);
        Classroom classroom = classroom(1L);
        User teacher = teacher(2L, "홍길동");
        DailySchedule dailySchedule = dailySchedule(100L, classroom, teacher, lessonDate);
        dailySchedule.updateStatus(DailyScheduleStatus.CANCELLED);

        given(dailyScheduleRepository.findByIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(Optional.of(dailySchedule));

        assertThatThrownBy(() -> dailyScheduleAdminService.correctTeacherAttendance(
            dailySchedule.getId(),
            1L,
            true,
            new UpdateDailyTeacherAttendanceCorrectionRequest(
                DailyTeacherAttendanceStatus.PRESENT,
                LocalDateTime.of(2026, 5, 20, 14, 0),
                LocalDateTime.of(2026, 5, 20, 16, 0)
            )
        )).isInstanceOf(InvalidDailyScheduleAttendanceStateException.class);
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
            .name(name)
            .role(RoleType.VOLUNTEER)
            .build();
        ReflectionTestUtils.setField(teacher, "id", id);
        return teacher;
    }

    private DailyTeacherAttendance teacherAttendance(DailySchedule dailySchedule) {
        DailyTeacherAttendance teacherAttendance = new DailyTeacherAttendance(dailySchedule, 120);
        teacherAttendance.updateAttendance(
            DailyTeacherAttendanceStatus.PRESENT,
            LocalDateTime.of(2026, 5, 20, 14, 0),
            null,
            null
        );
        return teacherAttendance;
    }

    private Subject subject(Classroom classroom, User teacher, LocalDate lessonDate) {
        return new Subject(
            classroom,
            teacher,
            "국어",
            lessonDate,
            lessonDate.plusMonths(1),
            java.time.DayOfWeek.WEDNESDAY,
            LocalTime.of(14, 0),
            LocalTime.of(16, 0),
            1,
            LocalDateTime.now(),
            null
        );
    }

    private Lesson lesson(Subject subject, User teacher, LocalDate lessonDate) {
        return new Lesson(subject, teacher, lessonDate, LocalTime.of(14, 0), LocalTime.of(15, 0), 1);
    }
}
