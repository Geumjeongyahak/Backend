package geumjeongyahak.domain.lesson.repository.specification;

import geumjeongyahak.domain.lesson.entity.Lesson;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import org.springframework.data.jpa.domain.Specification;

public class LessonSpecs {

    public static Specification<Lesson> isActive() {
        return (root, query, cb) -> cb.isFalse(root.get("isDeleted"));
    }

    public static Specification<Lesson> taughtBy(Long teacherId) {
        return (root, query, cb) -> cb.equal(root.get("teacher").get("id"), teacherId);
    }

    public static Specification<Lesson> onDates(Collection<LocalDate> dates) {
        return (root, query, cb) -> root.get("date").in(dates);
    }

    /** 반열린 구간 [start, end)끼리 겹친다. 끝과 시작이 맞닿은 수업은 겹치지 않는다. */
    public static Specification<Lesson> overlapsTime(LocalTime startTime, LocalTime endTime) {
        return (root, query, cb) -> cb.and(
            cb.lessThan(root.get("startTime"), endTime),
            cb.greaterThan(root.get("endTime"), startTime)
        );
    }

    public static Specification<Lesson> excludingLesson(Long lessonId) {
        return (root, query, cb) -> cb.notEqual(root.get("id"), lessonId);
    }

    public static Specification<Lesson> excludingSubject(Long subjectId) {
        return (root, query, cb) -> cb.notEqual(root.get("subject").get("id"), subjectId);
    }
}
