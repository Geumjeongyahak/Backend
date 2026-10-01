package geumjeongyahak.unit.daily_schedule;

import static geumjeongyahak.unit.daily_schedule.DailyScheduleFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.daily_schedule.entity.DailySchedule;
import geumjeongyahak.domain.daily_schedule.entity.DailyStudentAttendance;
import geumjeongyahak.domain.daily_schedule.entity.DailyTeacherAttendance;
import geumjeongyahak.domain.daily_schedule.repository.DailyScheduleRepository;
import geumjeongyahak.domain.daily_schedule.repository.DailyStudentAttendanceRepository;
import geumjeongyahak.domain.daily_schedule.repository.DailyTeacherAttendanceRepository;
import geumjeongyahak.domain.daily_schedule.service.DailyScheduleSynchronizer;
import geumjeongyahak.domain.lesson.entity.Lesson;
import geumjeongyahak.domain.lesson.service.LessonProxyService;
import geumjeongyahak.domain.student.entity.Student;
import geumjeongyahak.domain.student.service.StudentProxyService;
import geumjeongyahak.domain.subject.entity.Subject;
import geumjeongyahak.domain.users.entity.User;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DailyScheduleSynchronizerTest {

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
    private DailyScheduleSynchronizer synchronizer;

    @Captor
    private ArgumentCaptor<List<DailyStudentAttendance>> studentAttendancesCaptor;

    @Test
    void synchronize_createsScheduleAndInitialAttendances() {
        LocalDate lessonDate = LocalDate.of(2026, 5, 20);
        Classroom classroom = classroom(1L);
        User teacher = teacher("홍길동");
        Subject subject = subject(classroom, teacher, lessonDate);
        Lesson firstLesson = lesson(subject, teacher, lessonDate, LocalTime.of(14, 0), LocalTime.of(15, 0), 1);
        Lesson secondLesson = lesson(subject, teacher, lessonDate, LocalTime.of(15, 10), LocalTime.of(16, 0), 2);
        Student student = student(10L, classroom);

        given(lessonProxyService.getActiveLessonsByClassroomAndDate(
            classroom.getId(),
            lessonDate
        )).willReturn(List.of(firstLesson, secondLesson));
        given(dailyScheduleRepository.findByClassroomIdAndLessonDate(classroom.getId(), lessonDate))
            .willReturn(Optional.empty());
        given(dailyScheduleRepository.save(any(DailySchedule.class))).willAnswer(invocation -> {
            DailySchedule dailySchedule = invocation.getArgument(0);
            ReflectionTestUtils.setField(dailySchedule, "id", 100L);
            return dailySchedule;
        });
        given(dailyTeacherAttendanceRepository.findByDailyScheduleId(100L)).willReturn(Optional.empty());
        given(dailyTeacherAttendanceRepository.save(any(DailyTeacherAttendance.class)))
            .willAnswer(invocation -> invocation.getArgument(0));
        given(studentProxyService.getActiveStudentsByClassroomId(classroom.getId()))
            .willReturn(List.of(student));
        given(dailyStudentAttendanceRepository.findAllByDailyScheduleId(100L)).willReturn(List.of());

        synchronizer.synchronize(classroom.getId(), lessonDate);

        ArgumentCaptor<DailySchedule> dailyScheduleCaptor = ArgumentCaptor.forClass(DailySchedule.class);
        verify(dailyScheduleRepository).save(dailyScheduleCaptor.capture());
        DailySchedule dailySchedule = dailyScheduleCaptor.getValue();
        assertThat(dailySchedule.getClassroom()).isEqualTo(classroom);
        assertThat(dailySchedule.getTeacher()).isEqualTo(teacher);
        assertThat(dailySchedule.getActivityStartTime()).isEqualTo(LocalTime.of(14, 0));
        assertThat(dailySchedule.getActivityEndTime()).isEqualTo(LocalTime.of(16, 0));

        ArgumentCaptor<DailyTeacherAttendance> teacherAttendanceCaptor =
            ArgumentCaptor.forClass(DailyTeacherAttendance.class);
        verify(dailyTeacherAttendanceRepository).save(teacherAttendanceCaptor.capture());
        assertThat(teacherAttendanceCaptor.getValue().getVolunteerServiceMinutes()).isEqualTo(120);

        verify(dailyStudentAttendanceRepository).saveAll(studentAttendancesCaptor.capture());
        assertThat(studentAttendancesCaptor.getValue())
            .extracting(DailyStudentAttendance::getStudent)
            .containsExactly(student);
    }

    @Test
    void synchronize_readsStudentAttendancesOnceAndCreatesOnlyMissingOnes() {
        LocalDate lessonDate = LocalDate.of(2026, 5, 20);
        Classroom classroom = classroom(1L);
        User teacher = teacher("홍길동");
        Subject subject = subject(classroom, teacher, lessonDate);
        Lesson lesson = lesson(subject, teacher, lessonDate, LocalTime.of(14, 0), LocalTime.of(15, 0), 1);
        Student attending = student(10L, classroom);
        Student newcomer = student(11L, classroom);
        DailySchedule dailySchedule = new DailySchedule(classroom, teacher, lessonDate, LocalTime.of(14, 0), LocalTime.of(15, 0));
        ReflectionTestUtils.setField(dailySchedule, "id", 100L);
        DailyStudentAttendance deletedAttendance = new DailyStudentAttendance(dailySchedule, attending);
        deletedAttendance.softDelete();

        given(lessonProxyService.getActiveLessonsByClassroomAndDate(classroom.getId(), lessonDate))
            .willReturn(List.of(lesson));
        given(dailyScheduleRepository.findByClassroomIdAndLessonDate(classroom.getId(), lessonDate))
            .willReturn(Optional.of(dailySchedule));
        given(dailyTeacherAttendanceRepository.findByDailyScheduleId(100L)).willReturn(Optional.empty());
        given(dailyTeacherAttendanceRepository.save(any(DailyTeacherAttendance.class)))
            .willAnswer(invocation -> invocation.getArgument(0));
        given(studentProxyService.getActiveStudentsByClassroomId(classroom.getId()))
            .willReturn(List.of(attending, newcomer));
        given(dailyStudentAttendanceRepository.findAllByDailyScheduleId(100L))
            .willReturn(List.of(deletedAttendance));

        synchronizer.synchronize(classroom.getId(), lessonDate);

        verify(dailyStudentAttendanceRepository).findAllByDailyScheduleId(100L);
        verify(dailyStudentAttendanceRepository).saveAll(studentAttendancesCaptor.capture());
        assertThat(studentAttendancesCaptor.getValue())
            .extracting(DailyStudentAttendance::getStudent)
            .containsExactly(newcomer);
        assertThat(deletedAttendance.isDeleted()).isFalse();
    }

    @Test
    void synchronize_deletesScheduleAndAttendancesWhenNoActiveLessonRemains() {
        LocalDate lessonDate = LocalDate.of(2026, 5, 20);
        Classroom classroom = classroom(1L);
        User teacher = teacher("홍길동");
        DailySchedule dailySchedule = dailySchedule(100L, classroom, teacher, lessonDate);
        DailyTeacherAttendance teacherAttendance = new DailyTeacherAttendance(dailySchedule, 120);
        DailyStudentAttendance studentAttendance = new DailyStudentAttendance(dailySchedule, student(10L, classroom));

        given(lessonProxyService.getActiveLessonsByClassroomAndDate(classroom.getId(), lessonDate)).willReturn(List.of());
        given(dailyScheduleRepository.findByClassroomIdAndLessonDateAndIsDeletedFalse(classroom.getId(), lessonDate))
            .willReturn(Optional.of(dailySchedule));
        given(dailyTeacherAttendanceRepository.findAllByDailyScheduleIdAndIsDeletedFalse(100L))
            .willReturn(List.of(teacherAttendance));
        given(dailyStudentAttendanceRepository.findAllByDailyScheduleIdAndIsDeletedFalse(100L))
            .willReturn(List.of(studentAttendance));

        synchronizer.synchronize(classroom.getId(), lessonDate);

        assertThat(dailySchedule.isDeleted()).isTrue();
        assertThat(teacherAttendance.isDeleted()).isTrue();
        assertThat(studentAttendance.isDeleted()).isTrue();
    }

    @Test
    void synchronize_doesNothingWhenNoLessonAndNoSchedule() {
        LocalDate lessonDate = LocalDate.of(2026, 5, 20);
        given(lessonProxyService.getActiveLessonsByClassroomAndDate(1L, lessonDate)).willReturn(List.of());
        given(dailyScheduleRepository.findByClassroomIdAndLessonDateAndIsDeletedFalse(1L, lessonDate))
            .willReturn(Optional.empty());

        synchronizer.synchronize(1L, lessonDate);

        verify(dailyScheduleRepository, never()).save(any());
    }
}
