package geumjeongyahak.domain.subject.repository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import geumjeongyahak.domain.subject.entity.Subject;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    // 기간이 겹치는 과목 중 요일과 교시가 일치하는 과목이 존재하는지 확인하는 메서드
    boolean existsByClassroomIdAndDayOfWeekAndPeriodAndStartAtLessThanEqualAndEndAtGreaterThanEqualAndIsActiveTrue(
        Long classroomId,
        DayOfWeek dayOfWeek,
        Integer period,
        LocalDate newEndAt,
        LocalDate newStartAt
    );

    // 해당 과목을 제외하고 기간이 겹치는 과목 중 요일과 교시가 일치하는 과목이 존재하는지 확인하는 메서드
    boolean existsByIdNotAndClassroomIdAndDayOfWeekAndPeriodAndStartAtLessThanEqualAndEndAtGreaterThanEqualAndIsActiveTrue(
        Long subjectId,
        Long classroomId,
        DayOfWeek dayOfWeek,
        Integer period,
        LocalDate newEndAt,
        LocalDate newStartAt
    );

    @Override
    @EntityGraph(attributePaths = {"classroom", "teacher"})
    Optional<Subject> findById(Long subjectId);

    @EntityGraph(attributePaths = {"classroom", "teacher"})
    List<Subject> findByClassroomId(Long classroomId);

    @EntityGraph(attributePaths = {"classroom", "teacher"})
    List<Subject> findAllByClassroomIdAndIsActiveTrueOrderByStartAtAscIdAsc(Long classroomId);

    @EntityGraph(attributePaths = {"classroom", "teacher"})
    List<Subject> findAllByIdIn(Collection<Long> subjectIds);

    @EntityGraph(attributePaths = {"classroom", "teacher"})
    List<Subject> findAllByTeacherIsNullAndIsActiveTrueOrderByStartAtAscIdAsc();

    @EntityGraph(attributePaths = {"classroom", "teacher"})
    List<Subject> findAllByTeacherIdAndIsActiveTrueOrderByStartAtAscIdAsc(Long teacherId);

    @EntityGraph(attributePaths = {"classroom", "teacher"})
    List<Subject> findAllByTeacherIdInAndIsActiveTrueOrderByTeacherIdAscStartAtAscIdAsc(Collection<Long> teacherIds);

    long countByIsActiveTrue();

    boolean existsByClassroomIdAndTeacherId(Long classroomId, Long teacherId);

    boolean existsByClassroomIdAndTeacherIdAndIsActiveTrue(Long classroomId, Long teacherId);

    @Query("""
        select count(s) > 0
        from Subject s
        where s.teacher.id = :teacherId
          and s.isActive = true
          and s.startAt <= :endAt
          and s.endAt >= :startAt
          and (
              s.classroom.id <> :classroomId
              or s.dayOfWeek <> :dayOfWeek
              or s.startAt <> :startAt
              or s.endAt <> :endAt
          )
        """)
    boolean existsOverlappingDifferentScheduleByTeacherId(
        @Param("teacherId") Long teacherId,
        @Param("classroomId") Long classroomId,
        @Param("dayOfWeek") DayOfWeek dayOfWeek,
        @Param("startAt") LocalDate startAt,
        @Param("endAt") LocalDate endAt
    );

    boolean existsByClassroomIdAndIsActiveTrue(Long classroomId);

    boolean existsByTeacherIdAndIsActiveTrue(Long teacherId);

    @Override
    @EntityGraph(attributePaths = {"classroom", "teacher"})
    List<Subject> findAll();

    @EntityGraph(attributePaths = {"classroom", "teacher"})
    List<Subject> findAllByIsActiveTrueOrderByStartAtAscIdAsc();
}
