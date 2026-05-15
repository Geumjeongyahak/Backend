package geumjeongyahak.unit.daily_schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.classroom.enums.ClassroomType;
import geumjeongyahak.domain.daily_schedule.entity.DailySchedule;
import geumjeongyahak.domain.daily_schedule.entity.DailyStudentAttendance;
import geumjeongyahak.domain.daily_schedule.entity.DailyTeacherAttendance;
import geumjeongyahak.domain.daily_schedule.repository.DailyScheduleRepository;
import geumjeongyahak.domain.daily_schedule.repository.DailyStudentAttendanceRepository;
import geumjeongyahak.domain.daily_schedule.repository.DailyTeacherAttendanceRepository;
import geumjeongyahak.domain.daily_schedule.service.DailyScheduleService;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.DailyScheduleListRequest;
import geumjeongyahak.domain.daily_schedule.v1.dto.response.DailyScheduleDetailResponse;
import geumjeongyahak.domain.daily_schedule.v1.dto.response.DailyScheduleSummaryResponse;
import geumjeongyahak.domain.lesson.entity.Lesson;
import geumjeongyahak.domain.lesson.service.LessonProxyService;
import geumjeongyahak.domain.student.entity.Student;
import geumjeongyahak.domain.subject.entity.Subject;
import geumjeongyahak.domain.users.entity.User;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DailyScheduleServiceReadTest {

    @Mock
    private DailyScheduleRepository dailyScheduleRepository;

    @Mock
    private DailyTeacherAttendanceRepository dailyTeacherAttendanceRepository;

    @Mock
    private DailyStudentAttendanceRepository dailyStudentAttendanceRepository;

    @Mock
    private LessonProxyService lessonProxyService;

    @InjectMocks
    private DailyScheduleService dailyScheduleService;

    @Test
    void getDailySchedules_returnsDailyUnitSummaries() {
        LocalDate lessonDate = LocalDate.of(2026, 5, 20);
        Classroom classroom = classroom(1L);
        User teacher = teacher(2L, "홍길동");
        DailySchedule dailySchedule = dailySchedule(100L, classroom, teacher, lessonDate);
        DailyTeacherAttendance teacherAttendance = new DailyTeacherAttendance(dailySchedule, 120);

        given(dailyScheduleRepository.findAllByIsDeletedFalseAndLessonDateBetweenOrderByLessonDateAscIdAsc(
            lessonDate,
            lessonDate
        )).willReturn(List.of(dailySchedule));
        given(dailyTeacherAttendanceRepository.findByDailyScheduleIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(Optional.of(teacherAttendance));
        given(lessonProxyService.getActiveLessonsByClassroomAndDate(classroom.getId(), lessonDate))
            .willReturn(List.of(lesson(subject(classroom, teacher, lessonDate), teacher, lessonDate, 1)));

        List<DailyScheduleSummaryResponse> responses = dailyScheduleService.getDailySchedules(
            new DailyScheduleListRequest(lessonDate, lessonDate, classroom.getId(), teacher.getId(), null)
        );

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).dailyScheduleId()).isEqualTo(dailySchedule.getId());
        assertThat(responses.get(0).volunteerServiceMinutes()).isEqualTo(120);
        assertThat(responses.get(0).lessonCount()).isEqualTo(1);
    }

    @Test
    void getDailySchedule_returnsLessonsAndAttendances() {
        LocalDate lessonDate = LocalDate.of(2026, 5, 20);
        Classroom classroom = classroom(1L);
        User teacher = teacher(2L, "홍길동");
        DailySchedule dailySchedule = dailySchedule(100L, classroom, teacher, lessonDate);
        Subject subject = subject(classroom, teacher, lessonDate);
        Student student = student(10L, classroom);
        DailyStudentAttendance studentAttendance = new DailyStudentAttendance(dailySchedule, student);
        DailyTeacherAttendance teacherAttendance = new DailyTeacherAttendance(dailySchedule, 120);

        given(dailyScheduleRepository.findByIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(Optional.of(dailySchedule));
        given(lessonProxyService.getActiveLessonsByClassroomAndDate(classroom.getId(), lessonDate))
            .willReturn(List.of(lesson(subject, teacher, lessonDate, 1)));
        given(dailyTeacherAttendanceRepository.findByDailyScheduleIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(Optional.of(teacherAttendance));
        given(dailyStudentAttendanceRepository.findAllByDailyScheduleIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(List.of(studentAttendance));

        DailyScheduleDetailResponse response = dailyScheduleService.getDailySchedule(dailySchedule.getId());

        assertThat(response.dailyScheduleId()).isEqualTo(dailySchedule.getId());
        assertThat(response.lessons()).hasSize(1);
        assertThat(response.studentAttendances()).hasSize(1);
        assertThat(response.teacherAttendance().volunteerServiceMinutes()).isEqualTo(120);
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

    private Subject subject(Classroom classroom, User teacher, LocalDate lessonDate) {
        return new Subject(
            classroom,
            teacher,
            "국어",
            lessonDate,
            lessonDate.plusMonths(1),
            DayOfWeek.WEDNESDAY,
            LocalTime.of(14, 0),
            LocalTime.of(16, 0),
            1,
            LocalDateTime.now(),
            null
        );
    }

    private Lesson lesson(Subject subject, User teacher, LocalDate lessonDate, int period) {
        return new Lesson(subject, teacher, lessonDate, LocalTime.of(14, 0), LocalTime.of(15, 0), period);
    }

    private Student student(Long id, Classroom classroom) {
        Student student = new Student("최양지", null, null, classroom);
        ReflectionTestUtils.setField(student, "id", id);
        return student;
    }
}
