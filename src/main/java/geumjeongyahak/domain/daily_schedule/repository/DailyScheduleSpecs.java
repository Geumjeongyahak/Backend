package geumjeongyahak.domain.daily_schedule.repository;

import geumjeongyahak.domain.daily_schedule.entity.DailySchedule;
import geumjeongyahak.domain.lesson.entity.Lesson;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.function.Function;
import org.springframework.data.jpa.domain.Specification;

/**
 * 수업일지 목록 조건. 하루 일정의 수업은 분반·날짜로 이어진다(외래키 없음).
 */
public final class DailyScheduleSpecs {

    private static final char LIKE_ESCAPE = '\\';

    private DailyScheduleSpecs() {
    }

    public static Specification<DailySchedule> notDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("isDeleted"));
    }

    public static Specification<DailySchedule> hasTeacherId(Long teacherId) {
        return (root, query, cb) -> cb.equal(root.get("teacher").get("id"), teacherId);
    }

    /** 그 분반·날짜의 삭제 안 된 수업 중 일지가 적힌 것이 하나라도 있다. */
    public static Specification<DailySchedule> hasWrittenJournal() {
        return (root, query, cb) -> cb.exists(scheduleLessons(root, query, cb,
            lesson -> cb.notEqual(cb.trim(lesson.<String>get("note")), "")));
    }

    /** 분반명 · 교사명 · 그 수업의 과목명 · 일지 내용에 키워드가 글자 그대로 들어 있다(대소문자 무시). */
    public static Specification<DailySchedule> containsKeyword(String keyword) {
        String pattern = "%" + escapeLike(keyword.trim().toLowerCase()) + "%";
        return (root, query, cb) -> cb.or(
            like(cb, root.get("classroom").get("name"), pattern),
            like(cb, root.get("teacher").get("name"), pattern),
            cb.exists(scheduleLessons(root, query, cb, lesson -> cb.or(
                like(cb, lesson.get("subject").get("name"), pattern),
                like(cb, lesson.get("note"), pattern)
            )))
        );
    }

    private static Subquery<Integer> scheduleLessons(
        Root<DailySchedule> schedule,
        CriteriaQuery<?> query,
        CriteriaBuilder cb,
        Function<Root<Lesson>, Predicate> condition
    ) {
        Subquery<Integer> sub = query.subquery(Integer.class);
        Root<Lesson> lesson = sub.from(Lesson.class);
        return sub.select(cb.literal(1)).where(
            cb.isFalse(lesson.get("isDeleted")),
            cb.equal(lesson.get("subject").get("classroom").get("id"), schedule.get("classroom").get("id")),
            cb.equal(lesson.get("date"), schedule.get("lessonDate")),
            condition.apply(lesson)
        );
    }

    private static Predicate like(CriteriaBuilder cb, Expression<String> value, String pattern) {
        return cb.like(cb.lower(value), pattern, LIKE_ESCAPE);
    }

    private static String escapeLike(String keyword) {
        return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
