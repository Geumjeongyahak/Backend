package geumjeongyahak.unit.daily_schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.classroom.enums.ClassroomType;
import geumjeongyahak.domain.daily_schedule.entity.DailySchedule;
import geumjeongyahak.domain.daily_schedule.entity.DailyStudentAttendance;
import geumjeongyahak.domain.daily_schedule.entity.DailyTeacherAttendance;
import geumjeongyahak.domain.daily_schedule.enums.DailyScheduleStatus;
import geumjeongyahak.domain.daily_schedule.enums.DailyStudentAttendanceStatus;
import geumjeongyahak.domain.daily_schedule.exception.DailyScheduleForbiddenException;
import geumjeongyahak.domain.daily_schedule.exception.DuplicateDailyStudentAttendanceException;
import geumjeongyahak.domain.daily_schedule.exception.InvalidDailyScheduleAttendanceStateException;
import geumjeongyahak.domain.daily_schedule.exception.InvalidDailyScheduleJournalStateException;
import geumjeongyahak.domain.daily_schedule.exception.InvalidDailySchedulePersonalInfoException;
import geumjeongyahak.domain.daily_schedule.exception.StudentNotInDailyScheduleException;
import geumjeongyahak.domain.daily_schedule.repository.DailyScheduleRepository;
import geumjeongyahak.domain.daily_schedule.repository.DailyStudentAttendanceRepository;
import geumjeongyahak.domain.daily_schedule.repository.DailyTeacherAttendanceRepository;
import geumjeongyahak.domain.daily_schedule.service.DailyScheduleService;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.DailyScheduleListRequest;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.UpdateDailyScheduleJournalRequest;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.UpdateDailyStudentAttendanceItemRequest;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.UpdateDailyStudentAttendancesRequest;
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

        DailyScheduleDetailResponse response = dailyScheduleService.getDailySchedule(
            dailySchedule.getId(),
            teacher.getId(),
            false
        );

        assertThat(response.dailyScheduleId()).isEqualTo(dailySchedule.getId());
        assertThat(response.lessons()).hasSize(1);
        assertThat(response.studentAttendances()).hasSize(1);
        assertThat(response.teacherAttendance().volunteerServiceMinutes()).isEqualTo(120);
    }

    @Test
    void getDailySchedule_hidesSensitiveInfoFromOtherVolunteer() {
        LocalDate lessonDate = LocalDate.of(2026, 5, 20);
        Classroom classroom = classroom(1L);
        User teacher = teacher(2L, "홍길동");
        teacher.setPhoneNumber("010-0000-0000");
        DailySchedule dailySchedule = dailySchedule(100L, classroom, teacher, lessonDate);
        dailySchedule.updateJournalPersonalInfo("900101", true);

        given(dailyScheduleRepository.findByIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(Optional.of(dailySchedule));
        given(lessonProxyService.getActiveLessonsByClassroomAndDate(classroom.getId(), lessonDate))
            .willReturn(List.of());
        given(dailyTeacherAttendanceRepository.findByDailyScheduleIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(Optional.empty());
        given(dailyStudentAttendanceRepository.findAllByDailyScheduleIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(List.of());

        DailyScheduleDetailResponse response = dailyScheduleService.getDailySchedule(
            dailySchedule.getId(),
            99L,
            false
        );

        assertThat(response.teacherPhoneNumber()).isNull();
        assertThat(response.residentRegistrationNumberPrefix()).isNull();
        assertThat(response.personalInfoConsent()).isTrue();
    }

    @Test
    void getDailySchedule_includesSensitiveInfoForTeacher() {
        LocalDate lessonDate = LocalDate.of(2026, 5, 20);
        Classroom classroom = classroom(1L);
        User teacher = teacher(2L, "홍길동");
        teacher.setPhoneNumber("010-0000-0000");
        DailySchedule dailySchedule = dailySchedule(100L, classroom, teacher, lessonDate);
        dailySchedule.updateJournalPersonalInfo("900101", true);

        given(dailyScheduleRepository.findByIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(Optional.of(dailySchedule));
        given(lessonProxyService.getActiveLessonsByClassroomAndDate(classroom.getId(), lessonDate))
            .willReturn(List.of());
        given(dailyTeacherAttendanceRepository.findByDailyScheduleIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(Optional.empty());
        given(dailyStudentAttendanceRepository.findAllByDailyScheduleIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(List.of());

        DailyScheduleDetailResponse response = dailyScheduleService.getDailySchedule(
            dailySchedule.getId(),
            teacher.getId(),
            false
        );

        assertThat(response.teacherPhoneNumber()).isEqualTo("010-0000-0000");
        assertThat(response.residentRegistrationNumberPrefix()).isEqualTo("900101");
    }

    @Test
    void updateJournal_updatesAuthorPersonalInfoAndLessonNotes() {
        LocalDate lessonDate = LocalDate.of(2026, 5, 20);
        Classroom classroom = classroom(1L);
        User teacher = teacher(2L, "홍길동");
        DailySchedule dailySchedule = dailySchedule(100L, classroom, teacher, lessonDate);
        Subject subject = subject(classroom, teacher, lessonDate);
        Lesson firstLesson = lesson(subject, teacher, lessonDate, 1);
        Lesson secondLesson = lesson(subject, teacher, lessonDate, 2);
        ReflectionTestUtils.setField(firstLesson, "id", 11L);
        ReflectionTestUtils.setField(secondLesson, "id", 12L);
        DailyTeacherAttendance teacherAttendance = new DailyTeacherAttendance(dailySchedule, 120);

        given(dailyScheduleRepository.findByIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(Optional.of(dailySchedule));
        given(lessonProxyService.getActiveLessonsByClassroomAndDate(classroom.getId(), lessonDate))
            .willReturn(List.of(firstLesson, secondLesson));
        given(dailyTeacherAttendanceRepository.findByDailyScheduleIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(Optional.of(teacherAttendance));
        given(dailyStudentAttendanceRepository.findAllByDailyScheduleIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(List.of());

        DailyScheduleDetailResponse response = dailyScheduleService.updateJournal(
            dailySchedule.getId(),
            teacher.getId(),
            false,
            new UpdateDailyScheduleJournalRequest(
                true,
                "900101",
                List.of(
                    new UpdateDailyScheduleJournalRequest.LessonJournalRequest(11L, "1교시 수업 내용"),
                    new UpdateDailyScheduleJournalRequest.LessonJournalRequest(12L, "2교시 수업 내용")
                )
            )
        );

        assertThat(dailySchedule.getResidentRegistrationNumberPrefix()).isEqualTo("900101");
        assertThat(dailySchedule.isPersonalInfoConsent()).isTrue();
        assertThat(firstLesson.getNote()).isEqualTo("1교시 수업 내용");
        assertThat(secondLesson.getNote()).isEqualTo("2교시 수업 내용");
        assertThat(response.residentRegistrationNumberPrefix()).isEqualTo("900101");
    }

    @Test
    void updateJournal_throwsWhenVolunteerIsNotTeacher() {
        LocalDate lessonDate = LocalDate.of(2026, 5, 20);
        Classroom classroom = classroom(1L);
        User teacher = teacher(2L, "홍길동");
        DailySchedule dailySchedule = dailySchedule(100L, classroom, teacher, lessonDate);

        given(dailyScheduleRepository.findByIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(Optional.of(dailySchedule));

        assertThatThrownBy(() -> dailyScheduleService.updateJournal(
            dailySchedule.getId(),
            99L,
            false,
            new UpdateDailyScheduleJournalRequest(
                true,
                "900101",
                List.of(new UpdateDailyScheduleJournalRequest.LessonJournalRequest(11L, "수업 내용"))
            )
        )).isInstanceOf(DailyScheduleForbiddenException.class);
    }

    @Test
    void updateJournal_throwsWhenDailyScheduleIsCancelled() {
        LocalDate lessonDate = LocalDate.of(2026, 5, 20);
        Classroom classroom = classroom(1L);
        User teacher = teacher(2L, "홍길동");
        DailySchedule dailySchedule = dailySchedule(100L, classroom, teacher, lessonDate);
        dailySchedule.updateStatus(DailyScheduleStatus.CANCELLED);

        given(dailyScheduleRepository.findByIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(Optional.of(dailySchedule));

        assertThatThrownBy(() -> dailyScheduleService.updateJournal(
            dailySchedule.getId(),
            teacher.getId(),
            false,
            new UpdateDailyScheduleJournalRequest(
                true,
                "900101",
                List.of(new UpdateDailyScheduleJournalRequest.LessonJournalRequest(11L, "수업 내용"))
            )
        )).isInstanceOf(InvalidDailyScheduleJournalStateException.class);
    }

    @Test
    void updateJournal_throwsWhenPersonalInfoConsentAndResidentPrefixDoNotMatch() {
        LocalDate lessonDate = LocalDate.of(2026, 5, 20);
        Classroom classroom = classroom(1L);
        User teacher = teacher(2L, "홍길동");
        DailySchedule dailySchedule = dailySchedule(100L, classroom, teacher, lessonDate);

        given(dailyScheduleRepository.findByIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(Optional.of(dailySchedule));

        assertThatThrownBy(() -> dailyScheduleService.updateJournal(
            dailySchedule.getId(),
            teacher.getId(),
            false,
            new UpdateDailyScheduleJournalRequest(
                false,
                "900101",
                List.of(new UpdateDailyScheduleJournalRequest.LessonJournalRequest(11L, "수업 내용"))
            )
        )).isInstanceOf(InvalidDailySchedulePersonalInfoException.class);
    }

    @Test
    void updateStudentAttendances_updatesStatuses() {
        LocalDate lessonDate = LocalDate.of(2026, 5, 20);
        Classroom classroom = classroom(1L);
        User teacher = teacher(2L, "홍길동");
        DailySchedule dailySchedule = dailySchedule(100L, classroom, teacher, lessonDate);
        Student student = student(10L, classroom);
        DailyStudentAttendance studentAttendance = new DailyStudentAttendance(dailySchedule, student);

        given(dailyScheduleRepository.findByIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(Optional.of(dailySchedule));
        given(dailyStudentAttendanceRepository.findAllByDailyScheduleIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(List.of(studentAttendance));
        given(lessonProxyService.getActiveLessonsByClassroomAndDate(classroom.getId(), lessonDate))
            .willReturn(List.of());
        given(dailyTeacherAttendanceRepository.findByDailyScheduleIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(Optional.empty());

        DailyScheduleDetailResponse response = dailyScheduleService.updateStudentAttendances(
            dailySchedule.getId(),
            teacher.getId(),
            false,
            false,
            new UpdateDailyStudentAttendancesRequest(List.of(
                new UpdateDailyStudentAttendanceItemRequest(student.getId(), DailyStudentAttendanceStatus.PRESENT)
            ))
        );

        assertThat(studentAttendance.getStatus()).isEqualTo(DailyStudentAttendanceStatus.PRESENT);
        assertThat(response.studentAttendances()).hasSize(1);
        assertThat(response.studentAttendances().get(0).status()).isEqualTo(DailyStudentAttendanceStatus.PRESENT);
    }

    @Test
    void updateStudentAttendances_throwsWhenVolunteerIsNotTeacher() {
        LocalDate lessonDate = LocalDate.of(2026, 5, 20);
        Classroom classroom = classroom(1L);
        User teacher = teacher(2L, "홍길동");
        DailySchedule dailySchedule = dailySchedule(100L, classroom, teacher, lessonDate);

        given(dailyScheduleRepository.findByIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(Optional.of(dailySchedule));

        assertThatThrownBy(() -> dailyScheduleService.updateStudentAttendances(
            dailySchedule.getId(),
            99L,
            false,
            false,
            new UpdateDailyStudentAttendancesRequest(List.of(
                new UpdateDailyStudentAttendanceItemRequest(10L, DailyStudentAttendanceStatus.PRESENT)
            ))
        )).isInstanceOf(DailyScheduleForbiddenException.class);
    }

    @Test
    void updateStudentAttendances_throwsWhenDailyScheduleIsCancelled() {
        LocalDate lessonDate = LocalDate.of(2026, 5, 20);
        Classroom classroom = classroom(1L);
        User teacher = teacher(2L, "홍길동");
        DailySchedule dailySchedule = dailySchedule(100L, classroom, teacher, lessonDate);
        dailySchedule.updateStatus(DailyScheduleStatus.CANCELLED);

        given(dailyScheduleRepository.findByIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(Optional.of(dailySchedule));

        assertThatThrownBy(() -> dailyScheduleService.updateStudentAttendances(
            dailySchedule.getId(),
            teacher.getId(),
            false,
            false,
            new UpdateDailyStudentAttendancesRequest(List.of(
                new UpdateDailyStudentAttendanceItemRequest(10L, DailyStudentAttendanceStatus.PRESENT)
            ))
        )).isInstanceOf(InvalidDailyScheduleAttendanceStateException.class);
    }

    @Test
    void updateStudentAttendances_throwsWhenStudentIsNotInDailySchedule() {
        LocalDate lessonDate = LocalDate.of(2026, 5, 20);
        Classroom classroom = classroom(1L);
        User teacher = teacher(2L, "홍길동");
        DailySchedule dailySchedule = dailySchedule(100L, classroom, teacher, lessonDate);

        given(dailyScheduleRepository.findByIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(Optional.of(dailySchedule));
        given(dailyStudentAttendanceRepository.findAllByDailyScheduleIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(List.of());

        assertThatThrownBy(() -> dailyScheduleService.updateStudentAttendances(
            dailySchedule.getId(),
            teacher.getId(),
            false,
            false,
            new UpdateDailyStudentAttendancesRequest(List.of(
                new UpdateDailyStudentAttendanceItemRequest(10L, DailyStudentAttendanceStatus.PRESENT)
            ))
        )).isInstanceOf(StudentNotInDailyScheduleException.class);
    }

    @Test
    void updateStudentAttendances_throwsWhenRequestHasDuplicateStudent() {
        LocalDate lessonDate = LocalDate.of(2026, 5, 20);
        Classroom classroom = classroom(1L);
        User teacher = teacher(2L, "홍길동");
        DailySchedule dailySchedule = dailySchedule(100L, classroom, teacher, lessonDate);
        Student student = student(10L, classroom);
        DailyStudentAttendance studentAttendance = new DailyStudentAttendance(dailySchedule, student);

        given(dailyScheduleRepository.findByIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(Optional.of(dailySchedule));
        given(dailyStudentAttendanceRepository.findAllByDailyScheduleIdAndIsDeletedFalse(dailySchedule.getId()))
            .willReturn(List.of(studentAttendance));

        assertThatThrownBy(() -> dailyScheduleService.updateStudentAttendances(
            dailySchedule.getId(),
            teacher.getId(),
            false,
            false,
            new UpdateDailyStudentAttendancesRequest(List.of(
                new UpdateDailyStudentAttendanceItemRequest(student.getId(), DailyStudentAttendanceStatus.PRESENT),
                new UpdateDailyStudentAttendanceItemRequest(student.getId(), DailyStudentAttendanceStatus.ABSENT)
            ))
        )).isInstanceOf(DuplicateDailyStudentAttendanceException.class);
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
