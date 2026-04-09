package sonmoeum.domain.request.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import sonmoeum.domain.request.entity.AbsenceRequest;
import sonmoeum.domain.request.enums.RequestStatus;

public interface AbsenceRequestRepository extends JpaRepository<AbsenceRequest, Long> {

    List<AbsenceRequest> findAllByOrderByCreatedAtDesc();

    List<AbsenceRequest> findAllByRequestedBy_IdOrderByCreatedAtDesc(Long requestedById);

    List<AbsenceRequest> findAllByStatusOrderByCreatedAtDesc(RequestStatus status);

    List<AbsenceRequest> findAllByStatusAndRequestedBy_IdOrderByCreatedAtDesc(
        RequestStatus status,
        Long requestedById
    );

    boolean existsByLesson_IdAndRequestedBy_Id(Long lessonId, Long requestedById);
}
