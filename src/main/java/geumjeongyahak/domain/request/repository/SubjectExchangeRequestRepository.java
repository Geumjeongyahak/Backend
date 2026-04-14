package geumjeongyahak.domain.request.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import geumjeongyahak.domain.request.entity.SubjectExchangeRequest;
import geumjeongyahak.domain.request.enums.RequestStatus;

public interface SubjectExchangeRequestRepository extends JpaRepository<SubjectExchangeRequest, Long> {

    List<SubjectExchangeRequest> findAllByOrderByCreatedAtDesc();

    List<SubjectExchangeRequest> findAllByRequestedBy_IdOrderByCreatedAtDesc(Long requestedById);

    List<SubjectExchangeRequest> findAllByStatusOrderByCreatedAtDesc(RequestStatus status);

    List<SubjectExchangeRequest> findAllByStatusAndRequestedBy_IdOrderByCreatedAtDesc(
        RequestStatus status,
        Long requestedById
    );
}
