package geumjeongyahak.unit.lesson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.base.dto.response.AdminPage;
import geumjeongyahak.domain.lesson.entity.Lesson;
import geumjeongyahak.domain.lesson.enums.LessonStatus;
import geumjeongyahak.domain.lesson.enums.TeacherAttendanceStatus;
import geumjeongyahak.domain.lesson.repository.LessonRepository;
import geumjeongyahak.domain.lesson.service.LessonAdminViewService;
import geumjeongyahak.domain.lesson.service.LessonAdminViewService.AdminLessonRow;
import geumjeongyahak.domain.lesson.service.LessonAdminViewService.LessonFilter;
import geumjeongyahak.domain.subject.entity.Subject;
import geumjeongyahak.domain.users.entity.User;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LessonAdminViewServiceTest {

    @Mock
    private LessonRepository lessonRepository;

    @InjectMocks
    private LessonAdminViewService lessonAdminViewService;

    @Test
    void getLessons_filtersByDateStatusAndTeacherAttendance() {
        Lesson matchingLesson = lesson(
            "김교사",
            "국어",
            LocalDate.of(2026, 5, 11),
            1,
            LessonStatus.SCHEDULED,
            TeacherAttendanceStatus.ABSENT
        );
        Lesson completedLesson = lesson(
            "박교사",
            "수학",
            LocalDate.of(2026, 5, 12),
            2,
            LessonStatus.COMPLETED,
            TeacherAttendanceStatus.PRESENT
        );
        Lesson outsideDateLesson = lesson(
            "이교사",
            "영어",
            LocalDate.of(2026, 5, 18),
            1,
            LessonStatus.SCHEDULED,
            TeacherAttendanceStatus.ABSENT
        );
        given(lessonRepository.findAllByIsDeletedFalseOrderByDateAscPeriodAsc())
            .willReturn(List.of(matchingLesson, completedLesson, outsideDateLesson));

        AdminPage<AdminLessonRow> page = lessonAdminViewService.getLessons(new LessonFilter(
            LocalDate.of(2026, 5, 10),
            LocalDate.of(2026, 5, 12),
            LessonStatus.SCHEDULED,
            TeacherAttendanceStatus.ABSENT,
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
            LessonStatus.SCHEDULED,
            TeacherAttendanceStatus.ABSENT
        );
        Lesson firstTeacherLesson = lesson(
            "김교사",
            "국어",
            LocalDate.of(2026, 5, 12),
            1,
            LessonStatus.SCHEDULED,
            TeacherAttendanceStatus.ABSENT
        );
        given(lessonRepository.findAllByIsDeletedFalseOrderByDateAscPeriodAsc())
            .willReturn(List.of(secondTeacherLesson, firstTeacherLesson));

        AdminPage<AdminLessonRow> page = lessonAdminViewService.getLessons(new LessonFilter(
            null,
            null,
            null,
            null,
            null,
            null,
            "teacherName,ASC"
        ));

        assertThat(page.content()).extracting(AdminLessonRow::teacherName).containsExactly("김교사", "최교사");
    }

    private Lesson lesson(
        String teacherName,
        String subjectName,
        LocalDate date,
        int period,
        LessonStatus status,
        TeacherAttendanceStatus teacherAttendance
    ) {
        User teacher = User.builder()
            .nickname(teacherName)
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
        lesson.updateTeacherAttendance(teacherAttendance);
        return lesson;
    }
}
