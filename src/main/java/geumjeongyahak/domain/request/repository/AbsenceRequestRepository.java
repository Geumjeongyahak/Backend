package geumjeongyahak.domain.request.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import geumjeongyahak.domain.request.entity.AbsenceRequest;
import geumjeongyahak.domain.request.enums.RequestStatus;

public interface AbsenceRequestRepository extends JpaRepository<AbsenceRequest, Long> {

    Page<AbsenceRequest> findAll(Pageable pageable);

    Page<AbsenceRequest> findAllByRequestedBy_Id(Long requestedById, Pageable pageable);

    Page<AbsenceRequest> findAllByStatus(RequestStatus status, Pageable pageable);

    Page<AbsenceRequest> findAllByStatusAndRequestedBy_Id(
        RequestStatus status,
        Long requestedById,
        Pageable pageable
    );

    boolean existsByDailySchedule_IdAndRequestedBy_IdAndStatusIn(
        Long dailyScheduleId,
        Long requestedById,
        List<RequestStatus> statuses
    );

    boolean existsByDailySchedule_Id(Long dailyScheduleId);

    boolean existsByDailySchedule_IdIn(List<Long> dailyScheduleIds);

    @Query("""
        select count(absenceRequest) > 0
        from AbsenceRequest absenceRequest
        where exists (
            select 1
            from Lesson lesson
            where lesson.id in :lessonIds
                and lesson.isDeleted = false
                and lesson.subject.classroom.id = absenceRequest.dailySchedule.classroom.id
                and lesson.date = absenceRequest.dailySchedule.lessonDate
        )
        """)
    boolean existsByDailyScheduleMatchingLessonIds(@Param("lessonIds") List<Long> lessonIds);

    List<AbsenceRequest> findAllByStatusInAndExpiresAtBefore(
        Collection<RequestStatus> statuses,
        LocalDateTime expiresAt
    );
}
