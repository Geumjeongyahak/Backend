package geumjeongyahak.domain.daily_schedule.repository;

import geumjeongyahak.domain.daily_schedule.entity.DailySchedule;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DailyScheduleRepository
    extends JpaRepository<DailySchedule, Long>, JpaSpecificationExecutor<DailySchedule> {

    @Override
    @EntityGraph(attributePaths = {"classroom", "teacher"})
    Page<DailySchedule> findAll(Specification<DailySchedule> spec, Pageable pageable);

    Optional<DailySchedule> findByClassroomIdAndLessonDateAndIsDeletedFalse(Long classroomId, LocalDate lessonDate);

    Optional<DailySchedule> findByClassroomIdAndLessonDate(Long classroomId, LocalDate lessonDate);

    @EntityGraph(attributePaths = {"classroom", "teacher"})
    Optional<DailySchedule> findByTeacherIdAndLessonDateAndIsDeletedFalse(Long teacherId, LocalDate lessonDate);

    @EntityGraph(attributePaths = {"classroom", "teacher"})
    List<DailySchedule> findAllByIsDeletedFalseAndLessonDateBetweenOrderByLessonDateAscIdAsc(
        LocalDate from,
        LocalDate to
    );

    @EntityGraph(attributePaths = {"classroom", "teacher"})
    Optional<DailySchedule> findByIdAndIsDeletedFalse(Long dailyScheduleId);
}
