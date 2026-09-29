package geumjeongyahak.domain.lesson.service;

import geumjeongyahak.domain.base.dto.response.AdminPage;
import geumjeongyahak.domain.base.dto.response.AdminSorts;
import geumjeongyahak.domain.lesson.entity.Lesson;
import geumjeongyahak.domain.lesson.enums.LessonStatus;
import geumjeongyahak.domain.lesson.repository.LessonRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
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
    private final Clock clock;

    public AdminPage<AdminLessonRow> getLessons(LessonFilter filter) {
        LocalDate[] range = resolveRange(filter.startDate(), filter.endDate());
        List<AdminLessonRow> rows = lessonRepository.findAllByIsDeletedFalseAndDateBetweenOrderByDateAscPeriodAsc(range[0], range[1])
            .stream()
            .filter(lesson -> filter.status() == null || lesson.getStatus() == filter.status())
            .map(AdminLessonRow::from)
            .toList();

        return AdminPage.from(sortLessons(rows, filter.sort()), filter.page(), filter.size());
    }

    // 수업은 달마다 수백 건씩 쌓인다. 기간을 안 주면 이번 달, 한쪽만 주면 그 날부터(까지) 한 달을 읽는다.
    private LocalDate[] resolveRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null) {
            return new LocalDate[] {startDate, endDate};
        }
        if (startDate != null) {
            return new LocalDate[] {startDate, startDate.plusMonths(1).minusDays(1)};
        }
        if (endDate != null) {
            return new LocalDate[] {endDate.minusMonths(1).plusDays(1), endDate};
        }
        YearMonth thisMonth = YearMonth.now(clock);
        return new LocalDate[] {thisMonth.atDay(1), thisMonth.atEndOfMonth()};
    }

    private List<AdminLessonRow> sortLessons(List<AdminLessonRow> rows, String sort) {
        return AdminSorts.sort(rows, sort, Map.of(
            "id", Comparator.comparing(AdminLessonRow::id, Comparator.nullsLast(Long::compareTo)),
            "date", Comparator.comparing(AdminLessonRow::date, Comparator.nullsLast(LocalDate::compareTo)),
            "period", Comparator.comparing(AdminLessonRow::period, Comparator.nullsLast(Integer::compareTo)),
            "teacherName", Comparator.comparing(AdminLessonRow::teacherName, Comparator.nullsLast(String::compareToIgnoreCase)),
            "subjectName", Comparator.comparing(AdminLessonRow::subjectName, Comparator.nullsLast(String::compareToIgnoreCase)),
            "status", Comparator.comparing(AdminLessonRow::status, Comparator.nullsLast(String::compareToIgnoreCase))
        ), "date,ASC;period,ASC");
    }

    public LessonStatus[] getStatuses() {
        return LessonStatus.values();
    }

    public record LessonFilter(
        LocalDate startDate,
        LocalDate endDate,
        LessonStatus status,
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
        String statusLabel
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
                lesson.getStatus().getDisplayName()
            );
        }
    }
}
