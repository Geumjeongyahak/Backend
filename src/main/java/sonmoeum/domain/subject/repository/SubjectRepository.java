package sonmoeum.domain.subject.repository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import sonmoeum.domain.subject.entity.Subject;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    // 기간이 겹치는 과목 중 요일과 교시가 일치하는 과목이 존재하는지 확인하는 메서드
    boolean existsByClassroomIdAndDayOfWeekAndPeriodAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
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

    @Override
    @EntityGraph(attributePaths = {"classroom", "teacher"})
    List<Subject> findAll();
}
