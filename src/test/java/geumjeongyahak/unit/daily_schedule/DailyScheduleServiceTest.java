package geumjeongyahak.unit.daily_schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static geumjeongyahak.unit.daily_schedule.DailyScheduleFixtures.*;

import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.classroom.enums.ClassroomType;
import geumjeongyahak.domain.classroom.service.ClassroomProxyService;
import geumjeongyahak.domain.daily_schedule.entity.DailySchedule;
import geumjeongyahak.domain.daily_schedule.entity.DailyStudentAttendance;
import geumjeongyahak.domain.daily_schedule.entity.DailyTeacherAttendance;
import geumjeongyahak.domain.daily_schedule.enums.DailyStudentAttendanceStatus;
import geumjeongyahak.domain.daily_schedule.enums.DailyTeacherAttendanceStatus;
import geumjeongyahak.domain.daily_schedule.repository.DailyScheduleRepository;
import geumjeongyahak.domain.daily_schedule.repository.DailyStudentAttendanceRepository;
import geumjeongyahak.domain.daily_schedule.repository.DailyTeacherAttendanceRepository;
import geumjeongyahak.domain.daily_schedule.service.DailyScheduleService;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.StudentAttendanceSheetRequest;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.UpdateDailyTeacherAttendanceRequest;
import geumjeongyahak.domain.daily_schedule.v1.dto.response.StudentAttendanceSheetResponse;
import geumjeongyahak.domain.lesson.entity.Lesson;
import geumjeongyahak.domain.lesson.service.LessonProxyService;
import geumjeongyahak.domain.student.entity.Student;
import geumjeongyahak.domain.student.service.StudentProxyService;
import geumjeongyahak.domain.subject.entity.Subject;
import geumjeongyahak.domain.users.entity.User;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DailyScheduleServiceTest {

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
    private ClassroomProxyService classroomProxyService;

    @InjectMocks
    private DailyScheduleService dailyScheduleService;

    @Test
    void getStudentAttendanceSheet_aggregatesMonthlySchedulesAndStudentAttendances() {
        Classroom classroom = classroom(1L, "해바라기반");
        User teacher = teacher(2L, "홍길동");
        Student firstStudent = student(10L, "김민수", classroom);
        Student secondStudent = student(11L, "박영희", classroom);
        DailySchedule firstSchedule = dailySchedule(100L, classroom, teacher, LocalDate.of(2026, 2, 7));
        DailySchedule secondSchedule = dailySchedule(101L, classroom, teacher, LocalDate.of(2026, 2, 14));
        DailyStudentAttendance firstAttendance = studentAttendance(
            1000L,
            firstSchedule,
            firstStudent,
            DailyStudentAttendanceStatus.PRESENT
        );
        DailyStudentAttendance secondAttendance = studentAttendance(
            1001L,
            firstSchedule,
            secondStudent,
            DailyStudentAttendanceStatus.ABSENT
        );

        given(classroomProxyService.getActiveById(classroom.getId())).willReturn(classroom);
        given(studentProxyService.getActiveStudentsByClassroomId(classroom.getId()))
            .willReturn(List.of(secondStudent, firstStudent));
        given(dailyScheduleRepository.findAllByIsDeletedFalseAndLessonDateBetweenOrderByLessonDateAscIdAsc(
            LocalDate.of(2026, 2, 1),
            LocalDate.of(2026, 2, 28)
        )).willReturn(List.of(firstSchedule, secondSchedule));
        given(dailyStudentAttendanceRepository.findAllByDailySchedule_IdInAndIsDeletedFalse(List.of(100L, 101L)))
            .willReturn(List.of(secondAttendance, firstAttendance));

        StudentAttendanceSheetResponse response = dailyScheduleService.getStudentAttendanceSheet(
            new StudentAttendanceSheetRequest(2026, 2, classroom.getId())
        );

        assertThat(response.year()).isEqualTo(2026);
        assertThat(response.month()).isEqualTo(2);
        assertThat(response.classroomId()).isEqualTo(classroom.getId());
        assertThat(response.classroomName()).isEqualTo("해바라기반");
        assertThat(response.students())
            .extracting("studentId", "studentName")
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(10L, "김민수"),
                org.assertj.core.groups.Tuple.tuple(11L, "박영희")
            );
        assertThat(response.schedules()).hasSize(2);
        assertThat(response.schedules().get(0).dailyScheduleId()).isEqualTo(100L);
        assertThat(response.schedules().get(0).day()).isEqualTo(7);
        assertThat(response.schedules().get(0).dayOfWeek()).isEqualTo("토");
        assertThat(response.schedules().get(0).studentAttendances())
            .extracting("attendanceId", "studentId", "status")
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(1000L, 10L, DailyStudentAttendanceStatus.PRESENT),
                org.assertj.core.groups.Tuple.tuple(1001L, 11L, DailyStudentAttendanceStatus.ABSENT)
            );
        assertThat(response.schedules().get(1).dailyScheduleId()).isEqualTo(101L);
        assertThat(response.schedules().get(1).studentAttendances()).isEmpty();
    }

    @Test
    void getStudentAttendanceSheet_skipsAttendanceQueryWhenMonthlyScheduleIsEmpty() {
        Classroom classroom = classroom(1L, "해바라기반");

        given(classroomProxyService.getActiveById(classroom.getId())).willReturn(classroom);
        given(studentProxyService.getActiveStudentsByClassroomId(classroom.getId())).willReturn(List.of());
        given(dailyScheduleRepository.findAllByIsDeletedFalseAndLessonDateBetweenOrderByLessonDateAscIdAsc(
            LocalDate.of(2026, 2, 1),
            LocalDate.of(2026, 2, 28)
        )).willReturn(List.of());

        StudentAttendanceSheetResponse response = dailyScheduleService.getStudentAttendanceSheet(
            new StudentAttendanceSheetRequest(2026, 2, classroom.getId())
        );

        assertThat(response.students()).isEmpty();
        assertThat(response.schedules()).isEmpty();
        verify(dailyStudentAttendanceRepository, never()).findAllByDailySchedule_IdInAndIsDeletedFalse(any());
    }

    @Test
    void updateAndCheckOutTeacherAttendance_usesConfiguredSeoulClock() {
        Clock seoulClock = Clock.fixed(Instant.parse("2026-07-01T10:15:30Z"), ZoneId.of("Asia/Seoul"));
        DailyScheduleService service = new DailyScheduleService(
            dailyScheduleRepository,
            dailyTeacherAttendanceRepository,
            dailyStudentAttendanceRepository,
            lessonProxyService,
            studentProxyService,
            null,
            null,
            seoulClock
        );
        LocalDate lessonDate = LocalDate.of(2026, 7, 1);
        Classroom classroom = classroom(1L);
        User teacher = teacher(2L, "홍길동");
        DailySchedule dailySchedule = dailySchedule(100L, classroom, teacher, lessonDate);
        DailyTeacherAttendance teacherAttendance = new DailyTeacherAttendance(dailySchedule, 140);
        Lesson lesson = lesson(
            subject(classroom, teacher, lessonDate),
            teacher,
            lessonDate,
            LocalTime.of(19, 20),
            LocalTime.of(20, 0),
            1
        );
        lesson.updateNote("KST 출석 시간 테스트 일지");

        given(dailyScheduleRepository.findByIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(Optional.of(dailySchedule));
        given(dailyTeacherAttendanceRepository.findByDailyScheduleIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(Optional.of(teacherAttendance));
        given(lessonProxyService.getActiveLessonsByClassroomAndDate(classroom.getId(), lessonDate))
            .willReturn(List.of(lesson));
        given(dailyStudentAttendanceRepository.findAllByDailyScheduleIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(List.of());

        service.updateTeacherAttendance(
            dailySchedule.getId(),
            teacher.getId(),
            false,
            true,
            new UpdateDailyTeacherAttendanceRequest(DailyTeacherAttendanceStatus.PRESENT, null, null)
        );

        assertThat(teacherAttendance.getAttendedAt()).isEqualTo(LocalDateTime.of(2026, 7, 1, 19, 15, 30));

        service.checkOutTeacherAttendance(dailySchedule.getId(), teacher.getId(), false, true);

        assertThat(teacherAttendance.getCheckedOutAt()).isEqualTo(LocalDateTime.of(2026, 7, 1, 19, 15, 30));
    }
}
