package geumjeongyahak.domain.lesson.service;

import geumjeongyahak.domain.base.dto.response.AdminPage;
import geumjeongyahak.domain.base.dto.response.AdminSorts;
import geumjeongyahak.domain.lesson.entity.Lesson;
import geumjeongyahak.domain.lesson.enums.LessonStatus;
import geumjeongyahak.domain.lesson.enums.TeacherAttendanceStatus;
import geumjeongyahak.domain.lesson.repository.LessonRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonAdminViewService {

    private final LessonRepository lessonRepository;

    public AdminPage<AdminLessonRow> getLessons(LessonFilter filter) {
        List<AdminLessonRow> rows = lessonRepository.findAllByIsDeletedFalseOrderByDateAscPeriodAsc()
            .stream()
            .filter(lesson -> matchesDateRange(lesson, filter.startDate(), filter.endDate()))
            .filter(lesson -> filter.status() == null || lesson.getStatus() == filter.status())
            .filter(lesson -> filter.teacherAttendance() == null
                || lesson.getTeacherAttendance() == filter.teacherAttendance())
            .map(AdminLessonRow::from)
            .toList();

        return AdminPage.from(sortLessons(rows, filter.sort()), filter.page(), filter.size());
    }

    private boolean matchesDateRange(Lesson lesson, LocalDate startDate, LocalDate endDate) {
        LocalDate lessonDate = lesson.getDate();
        if (startDate != null && lessonDate.isBefore(startDate)) {
            return false;
        }
        return endDate == null || !lessonDate.isAfter(endDate);
    }

    private List<AdminLessonRow> sortLessons(List<AdminLessonRow> rows, String sort) {
        return AdminSorts.sort(rows, sort, Map.of(
            "id", Comparator.comparing(AdminLessonRow::id, Comparator.nullsLast(Long::compareTo)),
            "date", Comparator.comparing(AdminLessonRow::date, Comparator.nullsLast(LocalDate::compareTo)),
            "period", Comparator.comparing(AdminLessonRow::period, Comparator.nullsLast(Integer::compareTo)),
            "teacherName", Comparator.comparing(AdminLessonRow::teacherName, Comparator.nullsLast(String::compareToIgnoreCase)),
            "subjectName", Comparator.comparing(AdminLessonRow::subjectName, Comparator.nullsLast(String::compareToIgnoreCase)),
            "status", Comparator.comparing(AdminLessonRow::status, Comparator.nullsLast(String::compareToIgnoreCase)),
            "teacherAttendance", Comparator.comparing(AdminLessonRow::teacherAttendance, Comparator.nullsLast(String::compareToIgnoreCase))
        ), "date,ASC;period,ASC");
    }

    public LessonStatus[] getStatuses() {
        return LessonStatus.values();
    }

    public TeacherAttendanceStatus[] getTeacherAttendanceStatuses() {
        return TeacherAttendanceStatus.values();
    }

    public record LessonFilter(
        LocalDate startDate,
        LocalDate endDate,
        LessonStatus status,
        TeacherAttendanceStatus teacherAttendance,
        Integer page,
        Integer size,
        String sort
    ) {
    }

    public record AdminLessonRow(
        Long id,
        LocalDate date,
        Integer period,
        LocalTime startTime,
        LocalTime endTime,
        String teacherName,
        String subjectName,
        String status,
        String statusLabel,
        String teacherAttendance,
        String teacherAttendanceLabel
    ) {
        private static AdminLessonRow from(Lesson lesson) {
            return new AdminLessonRow(
                lesson.getId(),
                lesson.getDate(),
                lesson.getPeriod(),
                lesson.getStartTime(),
                lesson.getEndTime(),
                lesson.getTeacher().getName(),
                lesson.getSubject().getName(),
                lesson.getStatus().name(),
                lesson.getStatus().getDisplayName(),
                lesson.getTeacherAttendance().name(),
                lesson.getTeacherAttendance().getDisplayName()
            );
        }
    }
}
