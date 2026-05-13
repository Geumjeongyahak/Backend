package geumjeongyahak.domain.request.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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

    boolean existsByLesson_IdAndRequestedBy_Id(Long lessonId, Long requestedById);

    boolean existsByLesson_IdAndRequestedBy_IdAndStatusIn(
        Long lessonId,
        Long requestedById,
        List<RequestStatus> statuses
    );

    boolean existsByLesson_Id(Long lessonId);

    boolean existsByLesson_IdIn(List<Long> lessonIds);

    List<AbsenceRequest> findAllByStatusInAndExpiresAtBefore(
        Collection<RequestStatus> statuses,
        LocalDateTime expiresAt
    );
}
