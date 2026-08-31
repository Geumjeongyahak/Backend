package geumjeongyahak.unit.daily_schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.classroom.enums.ClassroomType;
import geumjeongyahak.domain.classroom.service.ClassroomProxyService;
import geumjeongyahak.domain.daily_schedule.entity.DailySchedule;
import geumjeongyahak.domain.daily_schedule.entity.DailyTeacherAttendance;
import geumjeongyahak.domain.daily_schedule.enums.DailyScheduleStatus;
import geumjeongyahak.domain.daily_schedule.enums.DailyTeacherAttendanceStatus;
import geumjeongyahak.domain.daily_schedule.exception.DailyScheduleJournalSheetLinkNotConfiguredException;
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
import geumjeongyahak.domain.daily_schedule.v1.dto.response.DailyScheduleJournalSheetRowResponse;
import geumjeongyahak.domain.lesson.entity.Lesson;
import geumjeongyahak.domain.lesson.enums.LessonStatus;
import geumjeongyahak.domain.lesson.service.LessonProxyService;
import geumjeongyahak.domain.student.service.StudentProxyService;
import geumjeongyahak.domain.subject.entity.Subject;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.service.UserProxyService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
    private ClassroomProxyService classroomProxyService;

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
            classroomProxyService,
            userProxyService,
            Clock.systemDefaultZone()
        );
        dailyScheduleAdminService = new DailyScheduleAdminService(
            dailyScheduleRepository,
            dailyTeacherAttendanceRepository,
            dailyScheduleService,
            lessonProxyService
        );
    }

    @Test
    void getJournalSheetLink_returnsConfiguredUrl() {
        ReflectionTestUtils.setField(
            dailyScheduleAdminService,
            "journalSheetUrl",
            " https://docs.google.com/spreadsheets/d/sheet-id/edit "
        );

        var response = dailyScheduleAdminService.getJournalSheetLink();

        assertThat(response.url()).isEqualTo("https://docs.google.com/spreadsheets/d/sheet-id/edit");
    }

    @Test
    void getJournalSheetLink_throwsWhenUrlIsBlank() {
        ReflectionTestUtils.setField(dailyScheduleAdminService, "journalSheetUrl", "");

        assertThatThrownBy(dailyScheduleAdminService::getJournalSheetLink)
            .isInstanceOf(DailyScheduleJournalSheetLinkNotConfiguredException.class)
            .hasMessage("수업일지 관리 시트 링크가 설정되어 있지 않습니다.");
    }

    @Test
    void getMonthlyJournalSheetData_returnsOnlyWrittenJournalsForRequestedMonth() {
        YearMonth month = YearMonth.of(2026, 7);
        LocalDate firstDay = month.atDay(1);
        LocalDate lastDay = month.atEndOfMonth();
        Classroom classroom = classroom(1L);
        User teacher = teacherWithPersonalInfo(2L, "홍길동", "010-1234-5678", "010101");
        DailySchedule writtenSchedule = dailySchedule(100L, classroom, teacher, firstDay);
        writtenSchedule.updateJournalPersonalInfo("900101", true);
        DailySchedule emptySchedule = dailySchedule(101L, classroom, teacher, lastDay);
        Lesson writtenLesson = lesson(subject(classroom, teacher, firstDay), teacher, firstDay);
        ReflectionTestUtils.setField(writtenLesson, "id", 1353L);
        writtenLesson.updateNote("국어 수업 내용");
        Lesson emptyLesson = lesson(subject(classroom, teacher, lastDay), teacher, lastDay);
        DailyTeacherAttendance teacherAttendance = teacherAttendance(writtenSchedule);

        given(dailyScheduleRepository.findAllByIsDeletedFalseAndLessonDateBetweenOrderByLessonDateAscIdAsc(
            firstDay,
            lastDay
        )).willReturn(List.of(writtenSchedule, emptySchedule));
        given(lessonProxyService.getActiveLessonsByClassroomIdsAndDates(
            Set.of(classroom.getId()),
            Set.of(firstDay, lastDay)
        )).willReturn(List.of(writtenLesson, emptyLesson));
        given(dailyTeacherAttendanceRepository.findAllByDailyScheduleIdInAndIsDeletedFalse(
            List.of(writtenSchedule.getId())
        )).willReturn(List.of(teacherAttendance));

        List<DailyScheduleJournalSheetRowResponse> responses =
            dailyScheduleAdminService.getMonthlyJournalSheetData(month);

        assertThat(responses).hasSize(1);
        DailyScheduleJournalSheetRowResponse response = responses.get(0);
        assertThat(response.dailyScheduleId()).isEqualTo(writtenSchedule.getId());
        assertThat(response.teacherPhoneNumber()).isEqualTo("010-1234-5678");
        assertThat(response.residentRegistrationNumberPrefix()).isEqualTo("900101");
        assertThat(response.personalInfoConsent()).isTrue();
        assertThat(response.teacherAttendanceStatus()).isEqualTo(DailyTeacherAttendanceStatus.PRESENT);
        assertThat(response.lessons()).hasSize(1);
        assertThat(response.lessons().get(0).lessonId()).isEqualTo(1353L);
        assertThat(response.lessons().get(0).period()).isEqualTo(1);
        assertThat(response.lessons().get(0).note()).isEqualTo("국어 수업 내용");
        verify(dailyTeacherAttendanceRepository)
            .findAllByDailyScheduleIdInAndIsDeletedFalse(List.of(writtenSchedule.getId()));
    }

    @Test
    void getMonthlyJournalSheetData_returnsEmptyListWhenMonthHasNoSchedules() {
        YearMonth month = YearMonth.of(2026, 7);
        given(dailyScheduleRepository.findAllByIsDeletedFalseAndLessonDateBetweenOrderByLessonDateAscIdAsc(
            month.atDay(1),
            month.atEndOfMonth()
        )).willReturn(List.of());

        List<DailyScheduleJournalSheetRowResponse> responses =
            dailyScheduleAdminService.getMonthlyJournalSheetData(month);

        assertThat(responses).isEmpty();
        verifyNoInteractions(lessonProxyService, dailyTeacherAttendanceRepository);
    }

    @ParameterizedTest
    @CsvSource({
        "true, 2026-07-15T13:55:00, 2026-07-15T16:05:00",
        "true, 2026-07-15T13:55:00,",
        "true,,",
        "false,,"
    })
    void getMonthlyJournalSheetData_returnsRecordedAttendanceTimes(
        boolean hasAttendanceRecord,
        LocalDateTime attendedAt,
        LocalDateTime checkedOutAt
    ) {
        YearMonth month = YearMonth.of(2026, 7);
        LocalDate lessonDate = month.atDay(15);
        Classroom classroom = classroom(1L);
        User teacher = teacher(2L, "홍길동");
        DailySchedule dailySchedule = dailySchedule(100L, classroom, teacher, lessonDate);
        Lesson lesson = lesson(subject(classroom, teacher, lessonDate), teacher, lessonDate);
        lesson.updateNote("국어 수업 내용");
        DailyTeacherAttendance attendance = new DailyTeacherAttendance(dailySchedule, 120);
        if (attendedAt != null) {
            attendance.correctAttendance(DailyTeacherAttendanceStatus.PRESENT, attendedAt, checkedOutAt);
        }

        given(dailyScheduleRepository.findAllByIsDeletedFalseAndLessonDateBetweenOrderByLessonDateAscIdAsc(
            month.atDay(1), month.atEndOfMonth()
        )).willReturn(List.of(dailySchedule));
        given(lessonProxyService.getActiveLessonsByClassroomIdsAndDates(
            Set.of(classroom.getId()), Set.of(lessonDate)
        )).willReturn(List.of(lesson));
        given(dailyTeacherAttendanceRepository.findAllByDailyScheduleIdInAndIsDeletedFalse(
            List.of(dailySchedule.getId())
        )).willReturn(hasAttendanceRecord ? List.of(attendance) : List.of());

        List<DailyScheduleJournalSheetRowResponse> responses =
            dailyScheduleAdminService.getMonthlyJournalSheetData(month);

        assertThat(responses).hasSize(1);
        DailyScheduleJournalSheetRowResponse response = responses.get(0);
        assertThat(response.attendedAt()).isEqualTo(attendedAt);
        assertThat(response.checkedOutAt()).isEqualTo(checkedOutAt);
        assertThat(response.activityStartTime()).isEqualTo(LocalTime.of(14, 0));
        assertThat(response.activityEndTime()).isEqualTo(LocalTime.of(16, 0));
        verify(dailyTeacherAttendanceRepository)
            .findAllByDailyScheduleIdInAndIsDeletedFalse(List.of(dailySchedule.getId()));
        verifyNoMoreInteractions(dailyTeacherAttendanceRepository);
    }

    @Test
    void getMonthlyJournalSheetData_skipsAttendanceQueryWhenNoJournalIsWritten() {
        YearMonth month = YearMonth.of(2026, 7);
        LocalDate lessonDate = month.atDay(15);
        Classroom classroom = classroom(1L);
        User teacher = teacher(2L, "홍길동");
        DailySchedule dailySchedule = dailySchedule(100L, classroom, teacher, lessonDate);
        Lesson lesson = lesson(subject(classroom, teacher, lessonDate), teacher, lessonDate);

        given(dailyScheduleRepository.findAllByIsDeletedFalseAndLessonDateBetweenOrderByLessonDateAscIdAsc(
            month.atDay(1),
            month.atEndOfMonth()
        )).willReturn(List.of(dailySchedule));
        given(lessonProxyService.getActiveLessonsByClassroomIdsAndDates(
            Set.of(classroom.getId()),
            Set.of(lessonDate)
        )).willReturn(List.of(lesson));

        List<DailyScheduleJournalSheetRowResponse> responses =
            dailyScheduleAdminService.getMonthlyJournalSheetData(month);

        assertThat(responses).isEmpty();
        verifyNoInteractions(dailyTeacherAttendanceRepository);
    }

    @Test
    void getMonthlyJournalSheetData_returnsTeacherResidentNumberWithoutConsent() {
        YearMonth month = YearMonth.of(2026, 7);
        LocalDate lessonDate = month.atDay(15);
        Classroom classroom = classroom(1L);
        User teacher = teacherWithPersonalInfo(2L, "홍길동", "010-1234-5678", "000101");
        DailySchedule dailySchedule = dailySchedule(100L, classroom, teacher, lessonDate);
        Lesson lesson = lesson(subject(classroom, teacher, lessonDate), teacher, lessonDate);
        lesson.updateNote("국어 수업 내용");

        given(dailyScheduleRepository.findAllByIsDeletedFalseAndLessonDateBetweenOrderByLessonDateAscIdAsc(
            month.atDay(1),
            month.atEndOfMonth()
        )).willReturn(List.of(dailySchedule));
        given(lessonProxyService.getActiveLessonsByClassroomIdsAndDates(
            Set.of(classroom.getId()),
            Set.of(lessonDate)
        )).willReturn(List.of(lesson));
        given(dailyTeacherAttendanceRepository.findAllByDailyScheduleIdInAndIsDeletedFalse(
            List.of(dailySchedule.getId())
        )).willReturn(List.of());

        DailyScheduleJournalSheetRowResponse response =
            dailyScheduleAdminService.getMonthlyJournalSheetData(month).get(0);

        assertThat(response.personalInfoConsent()).isFalse();
        assertThat(response.residentRegistrationNumberPrefix()).isEqualTo("000101");
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

    private User teacherWithPersonalInfo(
        Long id,
        String name,
        String phoneNumber,
        String residentRegistrationNumberPrefix
    ) {
        User teacher = User.builder()
            .name(name)
            .phoneNumber(phoneNumber)
            .residentRegistrationNumberPrefix(residentRegistrationNumberPrefix)
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
