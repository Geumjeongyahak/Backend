package geumjeongyahak.unit.lesson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import geumjeongyahak.common.config.AppConfig;
import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.base.dto.response.AdminPage;
import geumjeongyahak.domain.lesson.entity.Lesson;
import geumjeongyahak.domain.lesson.enums.LessonStatus;
import geumjeongyahak.domain.lesson.repository.LessonRepository;
import geumjeongyahak.domain.lesson.service.LessonAdminViewService;
import geumjeongyahak.domain.lesson.service.LessonAdminViewService.AdminLessonRow;
import geumjeongyahak.domain.lesson.service.LessonAdminViewService.LessonFilter;
import geumjeongyahak.domain.subject.entity.Subject;
import geumjeongyahak.domain.users.entity.User;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LessonAdminViewServiceTest {

    @Mock
    private LessonRepository lessonRepository;

    // 2026-05-20 12:00 KST
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-20T03:00:00Z"), AppConfig.ZONE_ID);

    private LessonAdminViewService lessonAdminViewService;

    @BeforeEach
    void setUp() {
        lessonAdminViewService = new LessonAdminViewService(lessonRepository, clock);
    }

    @Test
    void getLessons_filtersByDateAndStatus() {
        Lesson matchingLesson = lesson(
            "김교사",
            "국어",
            LocalDate.of(2026, 5, 11),
            1,
            LessonStatus.SCHEDULED
        );
        Lesson completedLesson = lesson(
            "박교사",
            "수학",
            LocalDate.of(2026, 5, 12),
            2,
            LessonStatus.COMPLETED
        );
        given(lessonRepository.findAllByIsDeletedFalseAndDateBetweenOrderByDateAscPeriodAsc(
            LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 12)))
            .willReturn(List.of(matchingLesson, completedLesson));

        AdminPage<AdminLessonRow> page = lessonAdminViewService.getLessons(new LessonFilter(
            LocalDate.of(2026, 5, 10),
            LocalDate.of(2026, 5, 12),
            LessonStatus.SCHEDULED,
            null,
            null,
            null
        ));

        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content()).extracting(AdminLessonRow::teacherName).containsExactly("김교사");
        assertThat(page.content()).extracting(AdminLessonRow::subjectName).containsExactly("국어");
    }

    @Test
    void getLessons_sortsByTeacherName() {
        Lesson secondTeacherLesson = lesson(
            "최교사",
            "수학",
            LocalDate.of(2026, 5, 11),
            1,
            LessonStatus.SCHEDULED
        );
        Lesson firstTeacherLesson = lesson(
            "김교사",
            "국어",
            LocalDate.of(2026, 5, 12),
            1,
            LessonStatus.SCHEDULED
        );
        // 기간을 안 주면 이번 달만 읽는다
        given(lessonRepository.findAllByIsDeletedFalseAndDateBetweenOrderByDateAscPeriodAsc(
            LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)))
            .willReturn(List.of(secondTeacherLesson, firstTeacherLesson));

        AdminPage<AdminLessonRow> page = lessonAdminViewService.getLessons(new LessonFilter(
            null,
            null,
            null,
            null,
            null,
            "teacherName,ASC"
        ));

        assertThat(page.content()).extracting(AdminLessonRow::teacherName).containsExactly("김교사", "최교사");
    }

    @Test
    void getLessons_readsOneMonthFromStartDateWhenEndDateIsMissing() {
        given(lessonRepository.findAllByIsDeletedFalseAndDateBetweenOrderByDateAscPeriodAsc(
            LocalDate.of(2026, 7, 15), LocalDate.of(2026, 8, 14)))
            .willReturn(List.of());

        AdminPage<AdminLessonRow> page = lessonAdminViewService.getLessons(new LessonFilter(
            LocalDate.of(2026, 7, 15), null, null, null, null, null));

        assertThat(page.totalElements()).isZero();
    }

    @Test
    void getLessons_readsOneMonthUntilEndDateWhenStartDateIsMissing() {
        given(lessonRepository.findAllByIsDeletedFalseAndDateBetweenOrderByDateAscPeriodAsc(
            LocalDate.of(2026, 6, 16), LocalDate.of(2026, 7, 15)))
            .willReturn(List.of());

        AdminPage<AdminLessonRow> page = lessonAdminViewService.getLessons(new LessonFilter(
            null, LocalDate.of(2026, 7, 15), null, null, null, null));

        assertThat(page.totalElements()).isZero();
    }

    private Lesson lesson(
        String teacherName,
        String subjectName,
        LocalDate date,
        int period,
        LessonStatus status
    ) {
        User teacher = User.builder()
            .name(teacherName)
            .role(RoleType.VOLUNTEER)
            .build();
        Subject subject = new Subject(
            null,
            teacher,
            subjectName,
            date,
            date.plusMonths(1),
            DayOfWeek.MONDAY,
            LocalTime.of(19, 20),
            LocalTime.of(20, 0),
            period,
            LocalDateTime.now(),
            null
        );
        Lesson lesson = new Lesson(
            subject,
            teacher,
            date,
            LocalTime.of(19, 20),
            LocalTime.of(20, 0),
            period
        );
        lesson.updateStatus(status);
        return lesson;
    }
}
